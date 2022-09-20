// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()  // Google's Maven repository
        gradlePluginPortal()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.3.0")
        classpath("com.google.gms:google-services:4.3.14")
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.9.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${AndroidConfig.kotlinVersion}")
    }
}

plugins {
    id("com.android.application") version "7.3.0" apply false
    id("com.android.library") version "7.3.0" apply false
    id("org.jetbrains.kotlin.android") version "1.7.10" apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}