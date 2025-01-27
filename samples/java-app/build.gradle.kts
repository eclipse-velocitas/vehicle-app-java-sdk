import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("application")
    kotlin("jvm")
    id("org.eclipse.velocitas.vss-processor-plugin") // version <VERSION>
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(project(":sdk"))

    implementation("org.eclipse.kuksa:vss-core:0.3.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
}

vssProcessor {
    searchPath = "$projectDir/vss"
}
