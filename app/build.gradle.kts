plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.navigation.safeargs)
}

android {
    namespace = "com.eddy.webcrawler"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.eddy.webcrawler"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.1"

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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
        compose = true // Add this
    }
    // Note: With Kotlin 2.0+, you NO LONGER need composeOptions { kotlinCompilerExtensionVersion }
}

dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Network
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor) // 確保版本與 okhttp 一致
    implementation(libs.jsoup)

    // Image Loading
    implementation("io.coil-kt:coil:2.7.0")

    // Video Playback
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)

    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp("androidx.room:room-compiler:2.6.1")

    // Hilt DI
    implementation(libs.dagger.hilt)
    ksp("com.google.dagger:hilt-compiler:2.51.1")

    // WorkManager + Hilt Worker
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // Navigation
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)

    // ViewPager2
    implementation("androidx.viewpager2:viewpager2:1.1.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Add these lines to satisfy the Compose Compiler
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Specifically, the Runtime is what the error message is asking for:
    implementation("androidx.compose.runtime:runtime")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
}
