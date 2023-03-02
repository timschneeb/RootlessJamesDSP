import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.google.devtools.ksp") version AndroidConfig.kspVersion
    id("dev.rikka.tools.refine") version AndroidConfig.rikkaRefineVersion
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.10"
}

android {
    val SUPPORTED_ABIS = setOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
    compileSdk = AndroidConfig.compileSdk
    project.setProperty("archivesBaseName", "RootlessJamesDSP-v${AndroidConfig.versionName}")

    defaultConfig {
        targetSdk = AndroidConfig.targetSdk
        versionCode = AndroidConfig.versionCode
        versionName = AndroidConfig.versionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "COMMIT_COUNT", "\"${getCommitCount()}\"")
        buildConfigField("String", "COMMIT_SHA", "\"${getGitSha()}\"")
        buildConfigField("String", "BUILD_TIME", "\"${getBuildTime()}\"")
        buildConfigField("boolean", "PREVIEW", "false")

        externalNativeBuild {
            cmake {
                arguments.addAll(listOf("-DANDROID_ARM_NEON=ON"))
            }
        }

        ndk {
            abiFilters += SUPPORTED_ABIS
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-${getCommitCount()}"
            manifestPlaceholders["crashlyticsCollectionEnabled"] = "false"
        }
        getByName("release") {
            manifestPlaceholders += mapOf("crashlyticsCollectionEnabled" to "true")
            configure<CrashlyticsExtension> {
                nativeSymbolUploadEnabled = true
                mappingFileUploadEnabled = false
            }

            //proguardFiles("proguard-android-optimize.txt", "proguard-rules.pro")
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
        }
        create("preview") {
            initWith(getByName("release"))
            buildConfigField("boolean", "PREVIEW", "true")

            val debugType = getByName("debug")
            versionNameSuffix = debugType.versionNameSuffix
            matchingFallbacks.add("release")
        }
    }

    flavorDimensions += "version"
    flavorDimensions += "dependencies"
    productFlavors {
        create("fdroid") {
            dimension = "dependencies"
            buildConfigField("boolean", "FOSS_ONLY", "true")
            android.defaultConfig.externalNativeBuild.cmake.arguments += "-DNO_CRASHLYTICS=1"
        }
        create("full") {
            dimension = "dependencies"
            buildConfigField("boolean", "FOSS_ONLY", "false")
        }

        create("rootless") {
            dimension = "version"

            manifestPlaceholders["label"] = "RootlessJamesDSP"
            applicationId = "me.timschneeberger.rootlessjamesdsp"
            AndroidConfig.minSdk = 29
            minSdk = AndroidConfig.minSdk
            buildConfigField("boolean", "ROOTLESS", "true")
        }
        create("root") {
            dimension = "version"

            manifestPlaceholders["label"] = "JamesDSP"
            project.setProperty("archivesBaseName", "JamesDSP-v${AndroidConfig.versionName}")
            applicationId = "james.dsp"
            AndroidConfig.minSdk = 26
            minSdk = AndroidConfig.minSdk
            buildConfigField("boolean", "ROOTLESS", "false")
        }
    }

    sourceSets {
        // Use different app icon for non-release builds
        getByName("debug").res.srcDirs("src/debug/res")
    }

    // Export multiple CPU architecture split apks
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
        disable += "ObsoleteSdkInt"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "11"
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

// Hooks to upload native symbols to crashlytics automatically
afterEvaluate {
    getTasksByName("bundleRootlessFullRelease", false).firstOrNull()?.finalizedBy("uploadCrashlyticsSymbolFileRootlessFullRelease")
    getTasksByName("bundleRootFullRelease", false).firstOrNull()?.finalizedBy("uploadCrashlyticsSymbolFileRootFullRelease")
    getTasksByName("assembleRootlessFullRelease", false).firstOrNull()?.finalizedBy("uploadCrashlyticsSymbolFileRootlessFullRelease")
    getTasksByName("assembleRootFullRelease", false).firstOrNull()?.finalizedBy("uploadCrashlyticsSymbolFileRootFullRelease")

    getTasksByName("assembleRootlessFullPreview", false).first().finalizedBy("uploadCrashlyticsSymbolFileRootlessFullRelease")
    getTasksByName("assembleRootFullPreview", false).first().finalizedBy("uploadCrashlyticsSymbolFileRootFullRelease")
}

dependencies {
    // Kotlin extensions
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.8.10")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")

    // AndroidX
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.5.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.0-rc01")
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.3")
    implementation("androidx.navigation:navigation-fragment:2.5.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.3")
    implementation("androidx.preference:preference-ktx:1.2.0")
    implementation("androidx.preference:preference:1.2.0")
    implementation("androidx.databinding:databinding-runtime:7.4.1")
    implementation("com.google.android.material:material:1.9.0-alpha02")

    // Dependency injection
    implementation("io.insert-koin:koin-android:3.2.0")
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1")

    // Firebase
    "fullImplementation"(platform("com.google.firebase:firebase-bom:30.4.1"))
    "fullImplementation"("com.google.firebase:firebase-analytics-ktx")
    "fullImplementation"("com.google.firebase:firebase-crashlytics-ktx")
    "fullImplementation"("com.google.firebase:firebase-crashlytics-ndk")

    // Web API client
    implementation("com.google.code.gson:gson:2.10")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")

    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("com.github.bastienpaulfr:Treessence:1.0.0")

    // Archiving
    implementation("org.kamranzafar:jtar:2.3")

    // Room databases
    val roomVersion = "2.5.0"
    implementation("androidx.room:room-runtime:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")

    // Script editor
    implementation(project(":codeview"))

    // Shizuku
    implementation("dev.rikka.shizuku:api:${AndroidConfig.shizukuVersion}")
    implementation("dev.rikka.shizuku:provider:${AndroidConfig.shizukuVersion}")

    // Root APIs
    "rootImplementation"("com.github.topjohnwu.libsu:core:5.0.4")

    // Hidden APIs
    implementation("dev.rikka.tools.refine:runtime:${AndroidConfig.rikkaRefineVersion}")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")
    compileOnly(project(":hidden-api-refined"))
    implementation(project(":hidden-api-impl"))

    // Debug utilities
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.10")

    // Unit tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}