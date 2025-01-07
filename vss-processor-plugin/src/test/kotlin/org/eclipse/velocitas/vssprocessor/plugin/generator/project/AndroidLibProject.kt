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

package org.eclipse.velocitas.vssprocessor.plugin.generator.project

import kotlin.io.path.appendText
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

open class AndroidLibProject(name: String) : GradleProject(name) {
    final override val projectDir = rootProjectDir.resolve(name).createDirectories()

    private val buildFile = projectDir.resolve("build.gradle.kts")
    private val mainDir = projectDir.resolve("src/main").createDirectories()
    private val androidManifestFile = mainDir.resolve("AndroidManifest.xml")

    override fun generate(appendix: String) {
        androidManifestFile.writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest>
            </manifest>
            """.trimIndent(),
        )

        buildFile.writeText(
            """
            plugins {
                id("com.android.library")
                id("org.eclipse.velocitas.vss-processor-plugin")
            }

            android {
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }

                namespace = "org.eclipse.kuksa.vssProcessorPluginTest"
                compileSdk = $COMPILE_SDK

                defaultConfig {
                    minSdk = $MIN_SDK
                }
            }

            dependencies {
            }

            """.trimIndent(),
        )

        buildFile.appendText("\n$appendix")
    }

    companion object {
        private const val COMPILE_SDK = 34
        private const val MIN_SDK = 27
    }
}
