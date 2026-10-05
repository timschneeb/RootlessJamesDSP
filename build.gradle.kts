// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // AGP 9 has built-in Kotlin and brings its own (older) KGP/KSP.
        // Putting newer versions on the root buildscript classpath is the documented way to upgrade them.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${AndroidConfig.kotlinVersion}")
        classpath("com.google.devtools.ksp:symbol-processing-gradle-plugin:${AndroidConfig.kspVersion}")
    }
}

plugins {
    id("com.android.application") version AndroidConfig.agpVersion apply false
    id("com.android.library") version AndroidConfig.agpVersion apply false
    id("org.jetbrains.kotlin.plugin.serialization") version AndroidConfig.kotlinVersion apply false
    id("com.google.gms.google-services") version "4.5.0" apply false
    id("com.google.firebase.crashlytics") version "3.0.8" apply false
    id("dev.rikka.tools.refine") version AndroidConfig.rikkaRefineVersion apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
