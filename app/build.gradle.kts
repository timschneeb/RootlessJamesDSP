import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.google.devtools.ksp") version AndroidConfig.kspVersion
    id("dev.rikka.tools.refine") version AndroidConfig.rikkaRefineVersion
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.10"
}

android {
    compileSdk = AndroidConfig.compileSdk

    defaultConfig {

        buildConfigField("String", "COMMIT_COUNT", "\"${getCommitCount()}\"")
        buildConfigField("String", "COMMIT_SHA", "\"${getGitSha()}\"")
        buildConfigField("String", "BUILD_TIME", "\"${getBuildTime()}\"")
        buildConfigField("boolean", "PREVIEW", "false")
        buildConfigField("boolean", "FOSS_ONLY", "true")
        buildConfigField("boolean", "ROOTLESS", "false")
        buildConfigField("boolean", "EMBEDDED", "true")
        buildConfigField("String", "APPLICATION_ID", "\"me.timschneeberger.rootlessjamesdsp\"")
        buildConfigField("String", "VERSION_NAME", "\"PLUGIN_MODE\"")
        buildConfigField("int", "VERSION_CODE", "1")

        targetSdk = AndroidConfig.targetSdk

        externalNativeBuild {
            cmake {
                arguments.addAll(listOf("-DANDROID_ARM_NEON=ON"))
            }
        }
    }

    android.defaultConfig.externalNativeBuild.cmake.arguments += "-DNO_CRASHLYTICS=1"

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
            buildConfigField("boolean", "ROOTLESS", "true")
            buildConfigField("boolean", "PLUGIN", "false")
        }
        create("root") {
            dimension = "version"
            buildConfigField("boolean", "ROOTLESS", "false")
            buildConfigField("boolean", "PLUGIN", "false")
        }
        create("plugin") {
            dimension = "version"
            buildConfigField("boolean", "ROOTLESS", "false")
            buildConfigField("boolean", "PLUGIN", "true")
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
            version = "3.27.1"
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
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.3")
    implementation("androidx.preference:preference-ktx:1.2.0")
    implementation("androidx.databinding:databinding-runtime:7.4.2")
    implementation("androidx.work:work-runtime-ktx:2.8.0")
    implementation("androidx.mediarouter:mediarouter:1.3.1")

    // Material
    implementation("com.google.android.material:material:1.9.0-beta01")

    // Dependency injection
    implementation("io.insert-koin:koin-android:3.3.3")
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.0")

    // Firebase
    "fullImplementation"(platform("com.google.firebase:firebase-bom:31.2.3"))
    "fullImplementation"("com.google.firebase:firebase-analytics-ktx")
    "fullImplementation"("com.google.firebase:firebase-crashlytics-ktx")
    "fullImplementation"("com.google.firebase:firebase-crashlytics-ndk")

    // Web API client
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")

    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("com.github.bastienpaulfr:Treessence:1.0.0")

    // IO
    implementation("org.kamranzafar:jtar:2.3")
    implementation("com.squareup.okio:okio:3.3.0")

    // Room databases
    val roomVersion = "2.5.0"
    implementation("androidx.room:room-runtime:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")

    // Script editor
    implementation(project(":jamesdsp:codeview"))

    // Shizuku
    implementation("dev.rikka.shizuku:api:${AndroidConfig.shizukuVersion}")
    implementation("dev.rikka.shizuku:provider:${AndroidConfig.shizukuVersion}")

    // Used for backup file access
    implementation("com.github.tachiyomiorg:unifile:17bec43")

    // Root APIs
    "rootImplementation"("com.github.topjohnwu.libsu:core:5.0.4")

    // Hidden APIs
    implementation("dev.rikka.tools.refine:runtime:${AndroidConfig.rikkaRefineVersion}")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")
    compileOnly(project(":jamesdsp:hidden-api-refined"))
    implementation(project(":jamesdsp:hidden-api-impl"))

    // Debug utilities
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.10")
    debugImplementation("com.plutolib:pluto:2.0.9")
    releaseImplementation("com.plutolib:pluto-no-op:2.0.9")
    debugImplementation("com.plutolib.plugins:bundle-core:2.0.9")
    releaseImplementation("com.plutolib.plugins:bundle-core-no-op:2.0.9")

    // Unit tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}