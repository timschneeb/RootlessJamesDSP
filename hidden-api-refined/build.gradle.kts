plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("dev.rikka.tools.refine") version AndroidConfig.rikkaRefineVersion
}

android {
    compileSdk = AndroidConfig.compileSdk

    defaultConfig {
        minSdk = AndroidConfig.minSdk
        targetSdk = AndroidConfig.targetSdk
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles("proguard-android-optimize.txt", "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    namespace = "me.timschneeberger.hiddenapi_refine"
}

dependencies {
    // Kotlin
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:${AndroidConfig.kotlinVersion}")

    // Refine
    annotationProcessor("dev.rikka.tools.refine:annotation-processor:${AndroidConfig.rikkaRefineVersion}")
    compileOnly("dev.rikka.tools.refine:annotation:${AndroidConfig.rikkaRefineVersion}")
}