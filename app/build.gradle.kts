plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "br.com.wasstudio"
    compileSdk = 35
    defaultConfig {
        applicationId = "br.com.wasstudio"
        minSdk = 26
        targetSdk = 28
        versionCode = 1
        versionName = "0.1"
    }
}
