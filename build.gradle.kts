plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false // Kotlin 2.0+ 必須加入這個
    alias(libs.plugins.google.devtools.ksp) apply false
    alias(libs.plugins.navigation.safeargs) apply false
    alias(libs.plugins.dagger.hilt) apply false
}