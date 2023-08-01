plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    compileSdk = AndroidConfig.compileSdk

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