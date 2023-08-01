// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()  // Google's Maven repository
        gradlePluginPortal()
        maven("https://jitpack.io")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.4.2")
        classpath("com.google.gms:google-services:4.3.15")
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.9.4")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.10")
    }
}

plugins {
    id("com.android.library") version "7.4.2" apply false
    id("org.jetbrains.kotlin.android") version "1.8.10" apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}