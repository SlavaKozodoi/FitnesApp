plugins {
    alias(libs.plugins.android.application)
    // Подключаем плагин Google Services
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.fitnesapp"
    compileSdk = 36

    androidResources {
        noCompress += "tflite"
    }

    defaultConfig {
        applicationId = "com.example.fitnesapp"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    }


    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // ВАЖНОЕ ИЗМЕНЕНИЕ: Включаем Desugaring и ставим Java 1.8
    compileOptions {
        isCoreLibraryDesugaringEnabled = true // <--- Включает поддержку Instant/Duration
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        viewBinding = true
    }

    kotlinOptions {
        jvmTarget = "1.8" // <--- Тоже меняем на 1.8
    }
}

dependencies {
    implementation("com.github.faruktoptas:FancyShowCaseView:1.3.9")
    implementation("com.airbnb.android:lottie:6.4.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.support)

    implementation("androidx.work:work-runtime:2.9.0")
    // ВАЖНОЕ ИЗМЕНЕНИЕ: Библиотека для поддержки времени (Instant, Duration)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    implementation("androidx.health.connect:connect-client:1.1.0-alpha07")

    // Для работы Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
// или 1.8.0
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
// или 1.8.0
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.7.3")

    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("com.mikhaellopez:circularprogressbar:3.1.0")

    // === FIREBASE ===
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-auth") // Версию убрал, её контролирует BOM

    // Google Sign In
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.cardview)
    implementation(libs.legacy.support.v4)
    implementation(libs.recyclerview)
    // implementation(libs.firebase.auth) // Убрал дубль, он уже есть выше через BOM
    implementation(libs.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}