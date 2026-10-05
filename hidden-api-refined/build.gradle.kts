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
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    namespace = "me.timschneeberger.hiddenapi_refine"
}

dependencies {
    // Kotlin (stdlib is added automatically by built-in Kotlin; kotlin-stdlib-jdk7 is obsolete)
    implementation("androidx.core:core-ktx:1.19.1")

    // Refine
    annotationProcessor("dev.rikka.tools.refine:annotation-processor:${AndroidConfig.rikkaRefineVersion}")
    compileOnly("dev.rikka.tools.refine:annotation:${AndroidConfig.rikkaRefineVersion}")
}
