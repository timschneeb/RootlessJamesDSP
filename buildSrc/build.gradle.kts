plugins {
    `kotlin-dsl`
}

// kotlin-dsl already provides the Gradle API; the old compileOnly KGP 2.1.0 dependency was unused.

repositories {
    mavenCentral()
    google()
}
