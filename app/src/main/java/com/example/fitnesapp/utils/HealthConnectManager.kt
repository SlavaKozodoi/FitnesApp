package com.example.fitnesapp.utils

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

// === Data Classes ===

data class SpeedPoint(
    val time: Long,
    val speedKmh: Double
)

// 2. Обновляем основную модель (добавили дистанцию и массив скоростей)
data class WorkoutSessionData(
    val type: String,
    val startTime: Long,
    val durationMinutes: Long,
    val calories: Long,
    val distanceKm: Double,          // НОВОЕ: Общая дистанция
    val speedData: List<SpeedPoint>  // НОВОЕ: Точки для графика
)

data class SleepSessionData(
    val startTime: Long,
    val endTime: Long,
    val durationMinutes: Long,
    val deepSleepMin: Long = 0,
    val lightSleepMin: Long = 0,
    val remSleepMin: Long = 0,
    val awakeMin: Long = 0
)

data class OxygenData(
    val time: Long,
    val percentage: Double
)

data class HeartRateData(
    val time: Long,
    val bpm: Long
)

data class ActivityBucket(
    val startTime: Long,
    val steps: Long,
    val calories: Double
)

data class MealData(
    val time: Long,
    val name: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)

class HealthConnectManager(private val context: Context) {

    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }
    private val scope = CoroutineScope(Dispatchers.IO)

    fun isAvailable(): Boolean = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    // === ИСПРАВЛЕННЫЙ МЕТОД ДЛЯ ТРЕНИРОВОК ===
    fun readWorkoutSessions(): ListenableFuture<List<WorkoutSessionData>> {
        val (start, end) = getTodayRange()
        val future = SettableFuture.create<List<WorkoutSessionData>>()

        scope.launch {
            try {
                val response = healthConnectClient.readRecords(
                    ReadRecordsRequest(
                        recordType = ExerciseSessionRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(start, end)
                    )
                )

                val workouts = ArrayList<WorkoutSessionData>()

                for (record in response.records) {
                    val duration = java.time.Duration.between(record.startTime, record.endTime).toSeconds()

                    // АГРЕГАЦИЯ: Запрашиваем калории И ДИСТАНЦИЮ
                    val aggregationResult = healthConnectClient.aggregate(
                        AggregateRequest(
                            metrics = setOf(
                                TotalCaloriesBurnedRecord.ENERGY_TOTAL,
                                DistanceRecord.DISTANCE_TOTAL // Добавили дистанцию!
                            ),
                            timeRangeFilter = TimeRangeFilter.between(record.startTime, record.endTime)
                        )
                    )

                    val energy = aggregationResult[TotalCaloriesBurnedRecord.ENERGY_TOTAL]
                    val cals = energy?.inKilocalories?.toLong() ?: 0L

                    val dist = aggregationResult[DistanceRecord.DISTANCE_TOTAL]
                    val distanceKm = dist?.inKilometers ?: 0.0

                    // ЧТЕНИЕ ГРАФИКА: Запрашиваем записи скорости (SpeedRecord) для этой тренировки
                    val speedPoints = ArrayList<SpeedPoint>()
                    val speedResponse = healthConnectClient.readRecords(
                        ReadRecordsRequest(
                            recordType = SpeedRecord::class,
                            timeRangeFilter = TimeRangeFilter.between(record.startTime, record.endTime)
                        )
                    )

                    // Проходим по всем записям скорости и вытаскиваем сэмплы (точки)
                    for (speedRecord in speedResponse.records) {
                        for (sample in speedRecord.samples) {
                            // Переводим м/с (стандарт Health Connect) в км/ч
                            val speedKmh = sample.speed.inMetersPerSecond * 3.6
                            speedPoints.add(SpeedPoint(sample.time.toEpochMilli(), speedKmh))
                        }
                    }

                    val typeName = getExerciseName(record.exerciseType)

                    workouts.add(
                        WorkoutSessionData(
                            type = typeName,
                            startTime = record.startTime.toEpochMilli(),
                            durationMinutes = duration,
                            calories = cals,
                            distanceKm = distanceKm,     // Передаем дистанцию
                            speedData = speedPoints      // Передаем точки графика
                        )
                    )
                }

                future.set(workouts)
            } catch (e: Exception) {
                future.setException(e)
            }
        }
        return future
    }
    private fun getExerciseName(type: Int): String {
        return when (type) {
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> "Running"
            ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "Walking"
            ExerciseSessionRecord.EXERCISE_TYPE_GYMNASTICS -> "Gymnastics"
            ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING -> "Weightlifting"
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> "Cycling"
            ExerciseSessionRecord.EXERCISE_TYPE_YOGA -> "Yoga"
            ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL,
            ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> "Swimming"
            ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING -> "HIIT"
            else -> "Workout"
        }
    }

    fun readSleepSessions(): ListenableFuture<List<SleepSessionData>> {
        // ИСПРАВЛЕНИЕ: Окно поиска с 12:00 вчерашнего дня до текущего момента.
        // Это захватит весь ночной сон (например с 21:30 до 08:15) без обрывов на полуночи.
        val now = Instant.now()
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -1) // Вчерашний день
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 12) // 12:00 дня
        calendar.set(java.util.Calendar.MINUTE, 0)

        val start = calendar.time.toInstant()
        val end = now

        val future = SettableFuture.create<List<SleepSessionData>>()

        scope.launch {
            try {
                val response = healthConnectClient.readRecords(
                    ReadRecordsRequest(
                        recordType = SleepSessionRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(start, end)
                    )
                )

                // УБРАН ФИЛЬТР. Теперь мы просто берем всё, что попало в это окно (всю ночь)
                val sessions = response.records.map { record ->
                    val totalDuration = java.time.Duration.between(record.startTime, record.endTime).toMinutes()

                    var deep = 0L
                    var light = 0L
                    var rem = 0L
                    var awake = 0L

                    // Суммируем фазы (если они есть от браслета)
                    for (stage in record.stages) {
                        val stageDuration = java.time.Duration.between(stage.startTime, stage.endTime).toMinutes()
                        when (stage.stage) {
                            SleepSessionRecord.STAGE_TYPE_DEEP -> deep += stageDuration
                            SleepSessionRecord.STAGE_TYPE_LIGHT -> light += stageDuration
                            SleepSessionRecord.STAGE_TYPE_REM -> rem += stageDuration
                            SleepSessionRecord.STAGE_TYPE_AWAKE,
                            SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED -> awake += stageDuration
                        }
                    }

                    SleepSessionData(
                        startTime = record.startTime.toEpochMilli(),
                        endTime = record.endTime.toEpochMilli(),
                        durationMinutes = totalDuration,
                        deepSleepMin = deep,
                        lightSleepMin = light,
                        remSleepMin = rem,
                        awakeMin = awake
                    )
                }
                future.set(sessions)
            } catch (e: Exception) {
                future.setException(e)
            }
        }
        return future
    }    fun readOxygenHistory(): ListenableFuture<List<OxygenData>> {
        val (start, end) = getTodayRange()
        val future = SettableFuture.create<List<OxygenData>>()

        scope.launch {
            try {
                val response = healthConnectClient.readRecords(
                    ReadRecordsRequest(
                        recordType = OxygenSaturationRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(start, end)
                    )
                )

                val oxygenList = response.records.map { record ->
                    OxygenData(
                        time = record.time.toEpochMilli(),
                        percentage = record.percentage.value
                    )
                }
                future.set(oxygenList)
            } catch (e: Exception) {
                future.setException(e)
            }
        }
        return future
    }

    fun readHeartRateHistory(): ListenableFuture<List<HeartRateData>> {
        val (start, end) = getTodayRange()
        val future = SettableFuture.create<List<HeartRateData>>()

        scope.launch {
            try {
                val response = healthConnectClient.readRecords(
                    ReadRecordsRequest(
                        recordType = HeartRateRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(start, end)
                    )
                )

                val heartRates = response.records.flatMap { record ->
                    record.samples.map { sample ->
                        HeartRateData(
                            time = sample.time.toEpochMilli(),
                            bpm = sample.beatsPerMinute
                        )
                    }
                }
                future.set(heartRates)
            } catch (e: Exception) {
                future.setException(e)
            }
        }
        return future
    }

    fun readMealsForToday(): ListenableFuture<List<MealData>> {
        val (start, end) = getTodayRange()
        val future = SettableFuture.create<List<MealData>>()

        scope.launch {
            try {
                val response = healthConnectClient.readRecords(
                    ReadRecordsRequest(
                        recordType = NutritionRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(start, end)
                    )
                )

                val meals = response.records.map { record ->
                    val calories = record.energy?.inKilocalories ?: 0.0
                    val protein = record.protein?.inGrams ?: 0.0
                    val carbs = record.totalCarbohydrate?.inGrams ?: 0.0
                    val fat = record.totalFat?.inGrams ?: 0.0
                    val name = if (!record.name.isNullOrEmpty()) record.name!! else "Meal"

                    MealData(
                        time = record.startTime.toEpochMilli(),
                        name = name,
                        calories = calories,
                        protein = protein,
                        carbs = carbs,
                        fat = fat
                    )
                }
                future.set(meals)
            } catch (e: Exception) {
                future.setException(e)
            }
        }
        return future
    }

    fun readAggregatedData(startTime: Instant, endTime: Instant): ListenableFuture<List<ActivityBucket>> {
        val future = SettableFuture.create<List<ActivityBucket>>()
        scope.launch {
            try {
                val response = healthConnectClient.aggregateGroupByDuration(
                    AggregateGroupByDurationRequest(
                        metrics = setOf(
                            StepsRecord.COUNT_TOTAL,
                            TotalCaloriesBurnedRecord.ENERGY_TOTAL
                        ),
                        timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
                        timeRangeSlicer = Duration.ofMinutes(30)
                    )
                )

                val buckets = response.map { bucket ->
                    val startEpoch = bucket.startTime.toEpochMilli()
                    val steps = bucket.result[StepsRecord.COUNT_TOTAL] ?: 0L
                    val cals = bucket.result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0

                    ActivityBucket(startEpoch, steps, cals)
                }
                future.set(buckets)
            } catch (e: Exception) {
                future.setException(e)
            }
        }
        return future
    }

    // === Простые методы чтения ===

    fun readStepsForToday(): ListenableFuture<Long> {
        val (start, end) = getTodayRange()
        return readSteps(start, end)
    }

    fun readCaloriesForToday(): ListenableFuture<Double> {
        val (start, end) = getTodayRange()
        return readCalories(start, end)
    }

    fun readHeartRateForToday(): ListenableFuture<Long> {
        val (start, end) = getTodayRange()
        return readAvgHeartRate(start, end)
    }

    fun readSleepForToday(): ListenableFuture<Long> {
        // Берем данные с 12:00 вчерашнего дня, чтобы не обрезать сон на 00:00
        val now = Instant.now()
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 12)
        calendar.set(java.util.Calendar.MINUTE, 0)

        val start = calendar.time.toInstant()
        val end = now

        return readSleepDuration(start, end)
    }

    fun readWeightForToday(): ListenableFuture<Double> {
        val (start, end) = getTodayRange()
        return readLatestWeight(start, end)
    }

    fun readOxygenForToday(): ListenableFuture<Double> {
        val (start, end) = getTodayRange()
        return readAvgOxygen(start, end)
    }

    fun readHistoryForToday(): ListenableFuture<List<ActivityBucket>> {
        val (start, end) = getTodayRange()
        return readAggregatedData(start, end)
    }

    // === Приватные вспомогательные методы ===

    private fun readSteps(startTime: Instant, endTime: Instant): ListenableFuture<Long> {
        val future = SettableFuture.create<Long>()
        scope.launch {
            try {
                val response = healthConnectClient.aggregate(
                    AggregateRequest(metrics = setOf(StepsRecord.COUNT_TOTAL), timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
                )
                future.set(response[StepsRecord.COUNT_TOTAL] ?: 0L)
            } catch (e: Exception) { future.setException(e) }
        }
        return future
    }

    private fun readCalories(startTime: Instant, endTime: Instant): ListenableFuture<Double> {
        val future = SettableFuture.create<Double>()
        scope.launch {
            try {
                val response = healthConnectClient.aggregate(
                    AggregateRequest(metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL), timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
                )
                future.set(response[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0)
            } catch (e: Exception) { future.setException(e) }
        }
        return future
    }

    private fun readAvgHeartRate(startTime: Instant, endTime: Instant): ListenableFuture<Long> {
        val future = SettableFuture.create<Long>()
        scope.launch {
            try {
                val response = healthConnectClient.aggregate(
                    AggregateRequest(metrics = setOf(HeartRateRecord.BPM_AVG), timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
                )
                future.set(response[HeartRateRecord.BPM_AVG] ?: 0L)
            } catch (e: Exception) { future.setException(e) }
        }
        return future
    }

    private fun readSleepDuration(startTime: Instant, endTime: Instant): ListenableFuture<Long> {
        val future = SettableFuture.create<Long>()
        scope.launch {
            try {
                val response = healthConnectClient.aggregate(
                    AggregateRequest(metrics = setOf(SleepSessionRecord.SLEEP_DURATION_TOTAL), timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
                )
                future.set(response[SleepSessionRecord.SLEEP_DURATION_TOTAL]?.toMinutes() ?: 0L)
            } catch (e: Exception) { future.setException(e) }
        }
        return future
    }

    private fun readLatestWeight(startTime: Instant, endTime: Instant): ListenableFuture<Double> {
        val future = SettableFuture.create<Double>()
        scope.launch {
            try {
                val response = healthConnectClient.readRecords(
                    ReadRecordsRequest(recordType = WeightRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime), ascendingOrder = false, pageSize = 1)
                )
                if (response.records.isNotEmpty()) future.set(response.records[0].weight.inKilograms) else future.set(0.0)
            } catch (e: Exception) { future.setException(e) }
        }
        return future
    }

    private fun readAvgOxygen(startTime: Instant, endTime: Instant): ListenableFuture<Double> {
        val future = SettableFuture.create<Double>()
        scope.launch {
            try {
                val response = healthConnectClient.readRecords(
                    ReadRecordsRequest(recordType = OxygenSaturationRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
                )
                val records = response.records
                if (records.isNotEmpty()) future.set(records.sumOf { it.percentage.value } / records.size.toDouble()) else future.set(0.0)
            } catch (e: Exception) { future.setException(e) }
        }
        return future
    }

    fun getPermissions(): Set<String> {
        return setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(WeightRecord::class),
            HealthPermission.getReadPermission(OxygenSaturationRecord::class),
            HealthPermission.getReadPermission(NutritionRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class)
        )
    }

    fun getPermissionContract() =
        androidx.health.connect.client.PermissionController.createRequestPermissionResultContract()

    private fun getTodayRange(): Pair<Instant, Instant> {
        val now = Instant.now()
        val startOfDay = now.atZone(ZoneId.systemDefault())
            .toLocalDate()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
        return Pair(startOfDay, now)
    }
}