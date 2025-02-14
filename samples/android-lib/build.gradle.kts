
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.eclipse.velocitas.vss-processor-plugin") // version <VERSION>
}

vssProcessor {
    searchPath = "vss"
}

android {
    namespace = "com.example.android.lib"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(project(":sdk"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
