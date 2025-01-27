/*
 * Copyright (c) 2023 - 2024 Contributors to the Eclipse Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.eclipse.velocitas.vssprocessor.plugin

import java.io.File
import javax.inject.Inject
import com.android.build.api.dsl.LibraryExtension
import com.android.build.gradle.AppExtension
import com.android.build.gradle.tasks.ExtractAnnotations
import org.eclipse.velocitas.vssprocessor.VssModelGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileType
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import org.gradle.work.ChangeType
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

open class VssProcessorPluginExtension
@Inject
internal constructor(objectFactory: ObjectFactory) {
    /**
     * The default search path is the $rootProject/vss folder. The defined folder will be crawled for all compatible
     * extension types by this plugin.
     */
    val searchPath: Property<String> = objectFactory.property(String::class.java).convention("")
}

private val fileSeparator = File.separator

/**
 * This Plugin searches for compatible VSS files, generates VSS Model classes and copies them into an output folder
 * which is added as a main sourceSet.
 */
class VssProcessorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extensions = project.extensions
        val vssProcessorExtension = extensions.create<VssProcessorPluginExtension>(EXTENSION_NAME)

        // The extension variables are only available after the project has been evaluated
        project.afterEvaluate {
            val vssModelGenerator = VssModelGenerator(project, logger)
            val sourceSetBasePath = vssModelGenerator.sourceSetBasePath

            addGeneratedPathToSourceSets(project, sourceSetBasePath)

            val generateVssModelsTask = project.tasks.register<GenerateVssModelsTask>(GENERATE_TASK_NAME) {
                val defaultVssPath = "${rootDir}${fileSeparator}$VSS_FOLDER_NAME"
                val vssPath = vssProcessorExtension.searchPath.get().ifEmpty { defaultVssPath }
                val vssDir = File(vssPath)
                inputDir.set(vssDir)

                val genOutputDir = File(vssModelGenerator.outputPath)
                outputDir.set(genOutputDir)
            }

            tasks.withType(KotlinCompile::class.java).configureEach {
                dependsOn(generateVssModelsTask.get())
            }
            tasks.withType(JavaCompile::class.java).configureEach {
                dependsOn(generateVssModelsTask.get())
            }
            tasks.withType(ExtractAnnotations::class.java).configureEach {
                dependsOn(generateVssModelsTask.get())
            }
        }
    }

    private fun addGeneratedPathToSourceSets(
        project: Project,
        sourceSetBasePath: String,
    ) {
        val extensions = project.extensions
        val pluginManager = project.pluginManager

        val isAndroidApplication = pluginManager.hasPlugin(PLUGIN_ID_ANDROID_APPLICATION)
        val isAndroidLibrary = pluginManager.hasPlugin(PLUGIN_ID_ANDROID_LIBRARY)
        val isJavaProject = javaPlugins.any { pluginManager.hasPlugin(it) }

        if (isAndroidApplication) {
            val androidExtension = extensions.getByType(AppExtension::class.java)
            val mainSourceSet = androidExtension.sourceSets.named(SOURCESET_MAIN_NAME).get()
            mainSourceSet.java.srcDirs(sourceSetBasePath)
        } else if (isAndroidLibrary) {
            val androidExtension = extensions.getByType(LibraryExtension::class.java)
            val mainSourceSet = androidExtension.sourceSets.named(SOURCESET_MAIN_NAME).get()
            mainSourceSet.java.srcDirs(sourceSetBasePath)
        } else if (isJavaProject) {
            val sourceSets = extensions.getByType(SourceSetContainer::class.java)
            val mainSourceSet = sourceSets.named(SOURCESET_MAIN_NAME).get()
            mainSourceSet.java.srcDirs(sourceSetBasePath)
        } else {
            throw GradleException("Project does not contain any supported plugin")
        }
    }

    companion object {
        private const val EXTENSION_NAME = "vssProcessor"
        private const val VSS_FOLDER_NAME = "vss"
        private const val GENERATE_TASK_NAME = "generateVssModels"
        private const val SOURCESET_MAIN_NAME = "main"
        private const val PLUGIN_ID_ANDROID_APPLICATION = "com.android.application"
        private const val PLUGIN_ID_ANDROID_LIBRARY = "com.android.library"
        private val javaPlugins = arrayOf(
            "java",
            "java-library",
        )
    }
}

/**
 * This task takes an input directory [inputDir] which should contain all available VSS files and an
 * output directory [outputDir] where all files are copied to so the VSSProcessor can work with them.
 */
@CacheableTask
private abstract class GenerateVssModelsTask : DefaultTask() {
    @get:Incremental
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    @get:InputDirectory
    abstract val inputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    private val vssModelGenerator by lazy {
        VssModelGenerator(project, logger)
    }

    @TaskAction
    fun provideFile(inputChanges: InputChanges) {
        inputChanges.getFileChanges(inputDir).forEach { change ->
            if (change.fileType == FileType.DIRECTORY) return@forEach

            val file = change.file
            val extension = file.extension
            if (!validVssExtension.contains(extension)) {
                logger.warn("Found incompatible VSS file: ${file.name} - Consider removing it")
                return@forEach
            }

            val targetFile = outputDir.file(change.normalizedPath).get().asFile
            logger.info("Found VSS file changes for: ${targetFile.name}, change: ${change.changeType}")

            when (change.changeType) {
                ChangeType.ADDED,
                ChangeType.MODIFIED,
                -> {
                    val outputDir = outputDir.asFile.get()
                    outputDir.deleteRecursively()
                    outputDir.mkdirs()

                    val vssFiles = inputDir.asFile.get()
                        .walk()
                        .filter { it.isFile }
                        .filter { validVssExtension.contains(it.extension) }
                        .toSet()
                    if (vssFiles.isEmpty()) {
                        logger.error("No VSS files were found! Is the plugin correctly configured?")
                        return@forEach
                    }

                    vssModelGenerator.generate(vssFiles)
                }

                ChangeType.REMOVED -> outputDir.asFile.get().deleteRecursively()
                else -> logger.warn("Could not determine file change type: ${change.changeType}")
            }
        }
    }

    companion object {
        private val validVssExtension = setOf("yml", "yaml", "json") // keep VssFileExtension aligned
    }
}
