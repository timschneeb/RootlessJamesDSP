import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.google.devtools.ksp") version AndroidConfig.kspVersion
    id("dev.rikka.tools.refine") version AndroidConfig.rikkaRefineVersion
}

android {
    val SUPPORTED_ABIS = setOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
    compileSdk = AndroidConfig.compileSdk

    defaultConfig {
        applicationId = "me.timschneeberger.rootlessjamesdsp"
        minSdk = AndroidConfig.minSdk
        targetSdk = AndroidConfig.targetSdk
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "COMMIT_COUNT", "\"${getCommitCount()}\"")
        buildConfigField("String", "COMMIT_SHA", "\"${getGitSha()}\"")
        buildConfigField("String", "BUILD_TIME", "\"${getBuildTime()}\"")

        externalNativeBuild {
            cmake {
                arguments.addAll(listOf("-DANDROID_ARM_NEON=ON"))
            }
        }

        ndk {
            abiFilters += SUPPORTED_ABIS
        }
    }

    signingConfigs {
        create("release") {
            keyAlias = "rootlessjamesdsp"
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "+${getCommitCount()}"
            manifestPlaceholders["crashlyticsCollectionEnabled"] = "false"
        }
        getByName("release") {
            manifestPlaceholders["crashlyticsCollectionEnabled"] = "true"
            configure<CrashlyticsExtension> {
                nativeSymbolUploadEnabled = true
                mappingFileUploadEnabled = true
            }

            proguardFiles("proguard-android-optimize.txt", "proguard-rules.pro")
            isShrinkResources = true
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
        }
    }

    project.setProperty("archivesBaseName", "RootlessJamesDSP-v${defaultConfig.versionName}")

    splits {
        abi {
            isEnable = true
            reset()
            include(*SUPPORTED_ABIS.toTypedArray())
            isUniversalApk = true
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
        // Disable unused features
        aidl = false
        renderScript = false
        shaders = false
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.18.1"
        }
    }
}

afterEvaluate {
    // I haven't found a way to port this to Gradle KTS yet
    withGroovyBuilder {
        "assembleRelease" {
            "finalizedBy"("uploadCrashlyticsSymbolFileRelease")
        }
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")

    implementation("org.jetbrains.kotlin:kotlin-reflect:1.7.10")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.5.1")
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.5.1")
    implementation("com.google.android.material:material:1.8.0-alpha01")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.2")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.2")
    implementation("androidx.preference:preference-ktx:1.2.0")
    implementation(platform("com.google.firebase:firebase-bom:30.4.1"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ndk")
    implementation("androidx.preference:preference:1.2.0")
    implementation("androidx.navigation:navigation-fragment:2.5.2")

    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("dev.doubledot.doki:library:0.0.1@aar")

    val roomVersion = "2.4.3"
    implementation("androidx.room:room-runtime:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")

    implementation("dev.rikka.shizuku:api:${AndroidConfig.shizukuVersion}")
    implementation("dev.rikka.shizuku:provider:${AndroidConfig.shizukuVersion}")
    implementation("dev.rikka.tools.refine:runtime:${AndroidConfig.rikkaRefineVersion}")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")
    compileOnly(project(":hidden-api-refined"))
    implementation(project(":hidden-api-impl"))
}