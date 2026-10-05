plugins {
    id("com.android.library")
    id("dev.rikka.tools.refine")
}

android {
    compileSdk = AndroidConfig.compileSdk

    defaultConfig {
        minSdk = AndroidConfig.minSdk
        lint.targetSdk = AndroidConfig.targetSdk
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // AGP 9 fails on keep files that don't exist - this used to point at a non-existent local file
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    namespace = "me.timschneeberger.hiddenapi_impl"
}

dependencies {
    implementation("dev.rikka.shizuku:api:${AndroidConfig.shizukuVersion}")
    compileOnly(project(":hidden-api-stubs"))
    compileOnly(project(":hidden-api-refined"))
}
