/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import org.eclipse.velocitas.version.SemanticVersion
import org.eclipse.velocitas.version.VERSION_FILE_DEFAULT_PATH_KEY
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java-library")
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.dokka)
    ktlint
    publish
}

val versionPath = rootProject.ext[VERSION_FILE_DEFAULT_PATH_KEY] as String
val semanticVersion = SemanticVersion(versionPath)
version = semanticVersion.versionName
group = "org.eclipse.velocitas"

publish {
    artifactName = "vehicle-app-java-sdk"
    mavenPublicationName = "release"
    componentName = "java"
    description = "Velocitas Vehicle App SDK for Java."
}

tasks.register("javadocJar", Jar::class) {
    dependsOn("dokkaHtml")

    val buildDir = layout.buildDirectory.get()
    from("$buildDir/dokka/html")
    archiveClassifier.set("javadoc")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11

    withJavadocJar() // needs to be called after tasks.register("javadocJar")
    withSourcesJar()
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    // required to manipulate the environment vars in tests
    jvmArgs("--add-opens=java.base/java.util=ALL-UNNAMED")
}

dependencies {
    api(libs.kuksa.java.sdk)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.kotest.runner.junit5)
}
