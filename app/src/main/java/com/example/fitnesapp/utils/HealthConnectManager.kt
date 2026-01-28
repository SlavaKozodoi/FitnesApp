package com.example.fitnesapp.utils

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

// Класс для графиков
data class ActivityBucket(
    val startTime: Long,
    val steps: Long,
    val calories: Double
)

// Класс для питания
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

    fun getPermissions(): Set<String> {
        return setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(WeightRecord::class),
            HealthPermission.getReadPermission(OxygenSaturationRecord::class),
            HealthPermission.getReadPermission(NutritionRecord::class) // Важно: разрешение на еду
        )
    }

    fun getPermissionContract() =
        androidx.health.connect.client.PermissionController.createRequestPermissionResultContract()

    // Вспомогательный метод: диапазон "Сегодня"
    private fun getTodayRange(): Pair<Instant, Instant> {
        val now = Instant.now()
        val startOfDay = now.atZone(ZoneId.systemDefault())
            .toLocalDate()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
        return Pair(startOfDay, now)
    }

    // ==========================================
    // === МЕТОДЫ ДЛЯ JAVA (В обход Instant) ===
    // ==========================================

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
        val (start, end) = getTodayRange()
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

    // === НОВЫЙ МЕТОД: Чтение питания ===
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

                    // Если имя не задано, пишем "Meal"
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


    // ==========================================
    // === ВНУТРЕННИЕ МЕТОДЫ (Private) ===
    // ==========================================

    fun readAggregatedData(startTime: Instant, endTime: Instant): ListenableFuture<List<ActivityBucket>> {
        val future = SettableFuture.create<List<ActivityBucket>>()
        scope.launch {
            try {
                val response = healthConnectClient.aggregateGroupByDuration(
                    AggregateGroupByDurationRequest(
                        metrics = setOf(
                            StepsRecord.COUNT_TOTAL,
                            // ИСПОЛЬЗУЕМ TOTAL (ЭТО БАЗА + АКТИВНОСТЬ)
                            TotalCaloriesBurnedRecord.ENERGY_TOTAL
                        ),
                        timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
                        timeRangeSlicer = Duration.ofMinutes(30)
                    )
                )

                val buckets = response.map { bucket ->
                    val startEpoch = bucket.startTime.toEpochMilli()
                    val steps = bucket.result[StepsRecord.COUNT_TOTAL] ?: 0L

                    // Получаем общие калории
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
                if (records.isNotEmpty()) future.set(records.sumOf { it.percentage.value } / records.size) else future.set(0.0)
            } catch (e: Exception) { future.setException(e) }
        }
        return future
    }
}