// Standalone latency probe APK. It must be a separate package (separate UID) so the rootless
// engine captures and processes its audio like any other media app. See tools/latency/README.md.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "me.timschneeberger.latencyprobe"
    compileSdk = AndroidConfig.compileSdk

    defaultConfig {
        applicationId = "me.timschneeberger.latencyprobe"
        minSdk = 29
        targetSdk = AndroidConfig.targetSdk
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
