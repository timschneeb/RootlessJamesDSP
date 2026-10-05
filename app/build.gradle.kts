import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension

plugins {
    id("com.android.application")
    // AGP 9: Kotlin is built in, so org.jetbrains.kotlin.android is no longer applied.
    // kotlin-kapt was removed: it had no kapt() dependencies here and is incompatible with built-in Kotlin.
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.google.devtools.ksp")
    id("dev.rikka.tools.refine")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {

    val SUPPORTED_ABIS = setOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
    compileSdk = AndroidConfig.compileSdk
    // Gradle 9 removed the "archivesBaseName" convention property -> base extension
    project.base.archivesName.set("RootlessJamesDSP-v${AndroidConfig.versionName}")

    signingConfigs {
        // Only used by the "fyt" flavor (standard Android platform key, password "android").
        // All other variants keep the original behavior (default debug keystore).
        create("fyt") {
            storeFile = file("src/fyt/androidkeystore.jks")
            storePassword = "android"
            keyAlias = "android"
            keyPassword = "android"
        }
    }

    defaultConfig {
        targetSdk = AndroidConfig.targetSdk
        versionCode = AndroidConfig.versionCode
        versionName = AndroidConfig.versionName

        manifestPlaceholders["label"] = "RootlessJamesDSP"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "COMMIT_COUNT", "\"${getCommitCount()}\"")
        buildConfigField("String", "COMMIT_SHA", "\"${getGitSha()}\"")
        buildConfigField("String", "BUILD_TIME", "\"${getBuildTime()}\"")
        buildConfigField("boolean", "PREVIEW", "false")
        buildConfigField("boolean", "PLUGIN", "false")

        externalNativeBuild {
            cmake {
                arguments.addAll(listOf("-DANDROID_ARM_NEON=ON"))
                cFlags.add("-std=gnu11 -Wno-incompatible-pointer-types -Wno-implicit-int -Wno-implicit-function-declaration")
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
            // Keep the IDE's default build variant as in the original repo (pluginFdroidDebug)
            isDefault = true
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
            buildConfigField("boolean", "PLUGIN", "false")
        }
        create("root") {
            dimension = "version"

            manifestPlaceholders["label"] = "JamesDSP"
            project.base.archivesName.set("JamesDSP-v${AndroidConfig.versionName}-${AndroidConfig.versionCode}")
            applicationId = "james.dsp"
            AndroidConfig.minSdk = 26
            minSdk = AndroidConfig.minSdk
            buildConfigField("boolean", "ROOTLESS", "false")
            buildConfigField("boolean", "PLUGIN", "false")
        }
        create("fyt") {
            dimension = "version"

            // Root build signed with the system key (sharedUserId=android.uid.system, see src/fyt)
            manifestPlaceholders["label"] = "JamesDSP"
            project.base.archivesName.set("JamesDSP-v${AndroidConfig.versionName}-${AndroidConfig.versionCode}")
            applicationId = "james.dsp"
            AndroidConfig.minSdk = 26
            minSdk = AndroidConfig.minSdk
            buildConfigField("boolean", "ROOTLESS", "false")
            buildConfigField("boolean", "PLUGIN", "false")
            signingConfig = signingConfigs.getByName("fyt")
        }
        create("plugin") {
            dimension = "version"
            isDefault = true

            AndroidConfig.minSdk = 26
            minSdk = AndroidConfig.minSdk
            buildConfigField("boolean", "ROOTLESS", "false")
            buildConfigField("boolean", "PLUGIN", "true")
        }
    }

    // The fyt flavor is a root build: reuse the root flavor sources
    sourceSets.getByName("fyt") {
        kotlin.directories.add("src/root/java")
    }

    // The old sourceSets block only re-added src/debug/res, which is already the default
    // resource directory of the "debug" source set, so it was removed.

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // kotlinOptions {} is an error with built-in Kotlin; Kotlin's jvmTarget now
    // defaults to compileOptions.targetCompatibility (17), so nothing else is needed.

    buildFeatures {
        viewBinding = true
        // Replaces android.defaults.buildfeatures.buildconfig=true (removed in AGP 9)
        buildConfig = true
        // Disable unused features
        aidl = false
        renderScript = false
        shaders = false
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    namespace = "me.timschneeberger.rootlessjamesdsp"
}

// Hooks to upload native symbols to crashlytics automatically
afterEvaluate {
    getTasksByName("bundleRootlessFullRelease", false).firstOrNull()?.finalizedBy("uploadCrashlyticsSymbolFileRootlessFullRelease")
    getTasksByName("bundleRootFullRelease", false).firstOrNull()?.finalizedBy("uploadCrashlyticsSymbolFileRootFullRelease")
    getTasksByName("assembleRootlessFullRelease", false).firstOrNull()?.finalizedBy("uploadCrashlyticsSymbolFileRootlessFullRelease")
    getTasksByName("assembleRootFullRelease", false).firstOrNull()?.finalizedBy("uploadCrashlyticsSymbolFileRootFullRelease")

    getTasksByName("assembleRootlessFullPreview", false).firstOrNull()?.finalizedBy("uploadCrashlyticsSymbolFileRootlessFullRelease")
    getTasksByName("assembleRootFullPreview", false).firstOrNull()?.finalizedBy("uploadCrashlyticsSymbolFileRootFullRelease")
}

// The buildType-level signingConfig overrides the flavor one, so force the system key for every fyt variant
androidComponents {
    onVariants(selector().all()) { variant ->
        if (variant.productFlavors.any { it.second == "fyt" }) {
            variant.signingConfig.setConfig(android.signingConfigs.getByName("fyt"))
        }
    }
}

dependencies {
    // Kotlin extensions
    implementation("org.jetbrains.kotlin:kotlin-reflect:${AndroidConfig.kotlinVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    // AndroidX
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.11.0")
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.2")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.10.2")
    implementation("androidx.navigation:navigation-ui-ktx:2.10.2")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.databinding:databinding-runtime:${AndroidConfig.agpVersion}")
    implementation("androidx.work:work-runtime-ktx:2.12.0")
    implementation("androidx.mediarouter:mediarouter:1.8.1")

    // Material
    implementation("com.google.android.material:material:1.14.0")

    // Dependency injection
    implementation("io.insert-koin:koin-android:4.2.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.11.0")

    // Firebase - BoM 34+ has no -ktx modules anymore, the Kotlin APIs live in the main artifacts
    "fullImplementation"(platform("com.google.firebase:firebase-bom:34.19.0"))
    "fullImplementation"("com.google.firebase:firebase-analytics")
    "fullImplementation"("com.google.firebase:firebase-crashlytics")
    "fullImplementation"("com.google.firebase:firebase-crashlytics-ndk")

    // Web API client
    implementation("com.google.code.gson:gson:2.14.0")
    // 2.12.0 is the newest Retrofit on OkHttp 3 (Retrofit 3.x moves to OkHttp 4)
    implementation("com.squareup.retrofit2:retrofit:2.12.0")
    implementation("com.squareup.retrofit2:converter-gson:2.12.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.12.0")

    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")
    // Kept at 1.0.0: 1.1.x renamed the package fr.bipi.tressence -> fr.bipi.treessence
    implementation("com.github.bastienpaulfr:Treessence:1.0.0")

    // IO
    implementation("org.kamranzafar:jtar:2.3")
    implementation("com.squareup.okio:okio:3.16.2")

    // Room databases
    val roomVersion = "2.8.5"
    implementation("androidx.room:room-runtime:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")

    // Script editor
    implementation(project(":codeview"))

    // Shizuku
    implementation("dev.rikka.shizuku:api:${AndroidConfig.shizukuVersion}")
    implementation("dev.rikka.shizuku:provider:${AndroidConfig.shizukuVersion}")

    // Used for backup file access
    implementation("com.github.tachiyomiorg:unifile:17bec43")

    // Root APIs
    "rootImplementation"("com.github.topjohnwu.libsu:core:6.0.0")
    "fytImplementation"("com.github.topjohnwu.libsu:core:6.0.0")

    // Hidden APIs
    implementation("dev.rikka.tools.refine:runtime:${AndroidConfig.rikkaRefineVersion}")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:6.1")
    compileOnly(project(":hidden-api-refined"))
    implementation(project(":hidden-api-impl"))

    // Debug utilities
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")
    // Pluto kept at 2.0.9: newer releases reorganised the network-interceptor artifacts
    debugImplementation("com.plutolib:pluto:2.0.9")
    "previewImplementation"("com.plutolib:pluto-no-op:2.0.9")
    releaseImplementation("com.plutolib:pluto-no-op:2.0.9")
    debugImplementation("com.plutolib.plugins:bundle-core:2.0.9")
    "previewImplementation"("com.plutolib.plugins:bundle-core-no-op:2.0.9")
    releaseImplementation("com.plutolib.plugins:bundle-core-no-op:2.0.9")

    // Unit tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}
