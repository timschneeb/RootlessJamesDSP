plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.10")

    implementation(gradleApi())
}

repositories {
    mavenCentral()
    google()
}
