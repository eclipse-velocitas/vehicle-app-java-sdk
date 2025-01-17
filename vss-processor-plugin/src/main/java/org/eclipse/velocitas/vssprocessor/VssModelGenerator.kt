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

package org.eclipse.velocitas.vssprocessor

import java.io.File
import com.squareup.kotlinpoet.FileSpec
import org.eclipse.kuksa.vsscore.model.parentClassName
import org.eclipse.velocitas.vssprocessor.parser.factory.VssParserFactory
import org.eclipse.velocitas.vssprocessor.spec.VssNodeSpecModel
import org.eclipse.velocitas.vssprocessor.spec.VssPath
import org.gradle.api.Project
import org.gradle.api.logging.Logger

/**
 * Generates a [org.eclipse.kuksa.vsscore.model.VssNode] for every entry listed in the input file.
 * These nodes are a usable kotlin data class reflection of the element.
 *
 * @param logger to log output with
 */
class VssModelGenerator(
    val project: Project,
    val logger: Logger,
) {
    private val vssParserFactory = VssParserFactory()

    val sourceSetBasePath = "build" + fileSeparator + "generated" + fileSeparator + "vss" + fileSeparator + "kotlin"
    val outputPath = sourceSetBasePath + fileSeparator + PACKAGE_NAME.replace(".", fileSeparator)

    fun generate(vssFiles: Set<File>) {
        val simpleNodeElements = mutableListOf<VssNodeSpecModel>()

        vssFiles.forEach { definitionFile ->
            logger.info("Parsing models for definition file: ${definitionFile.name}")
            val vssParser = vssParserFactory.create(definitionFile)
            val specModels = vssParser.parseNodes(definitionFile)

            simpleNodeElements.addAll(specModels)
        }

        val vssPathToVssNodeElement = simpleNodeElements
            .distinctBy { it.vssPath }
            .associateBy({ VssPath(it.vssPath) }, { it })

        generateModelFiles(vssPathToVssNodeElement)
    }

    private fun generateModelFiles(vssPathToVssNode: Map<VssPath, VssNodeSpecModel>) {
        val duplicateNodeNames = vssPathToVssNode.keys
            .groupBy { it.leaf }
            .filter { it.value.size > 1 }
            .keys

        logger.info("Ambiguous VssNode - Generate nested classes: $duplicateNodeNames")

        val generatedFilesVssPathToClassName = mutableMapOf<String, String>()
        for ((vssPath, specModel) in vssPathToVssNode) {
            // Every duplicate is produced as a nested class - No separate file should be generated
            if (duplicateNodeNames.contains(vssPath.leaf)) {
                continue
            }

            specModel.logger = logger
            val classSpec = specModel.createClassSpec(
                PACKAGE_NAME,
                vssPathToVssNode.values,
                duplicateNodeNames,
            )

            val className = classSpec.name ?: throw NoSuchFieldException("Class spec $classSpec has no name field!")
            val fileSpecBuilder = FileSpec.builder(PACKAGE_NAME, className)

            val parentImport = buildParentImport(specModel, generatedFilesVssPathToClassName)
            if (parentImport.isNotEmpty()) {
                fileSpecBuilder.addImport(PACKAGE_NAME, parentImport)
            }

            val file = fileSpecBuilder
                .addType(classSpec)
                .build()

            val sourcePath = sourceSetBasePath + fileSeparator + PACKAGE_NAME.replace(".", fileSeparator)
            val sourceDir = project.file(sourcePath)
            sourceDir.mkdirs()
            sourceDir.resolve(file.name + ".kt").writeText(file.toString())

            generatedFilesVssPathToClassName[vssPath.path] = className
        }
    }

    // Uses a map of vssPaths to ClassNames which are validated if it contains a parent of the given specModel.
    // If the actual parent is a sub class (Driver) in another class file (e.g. Vehicle) then this method returns
    // a sub import e.g. "Vehicle.Driver". Otherwise just "Vehicle" is returned.
    private fun buildParentImport(
        specModel: VssNodeSpecModel,
        parentVssPathToClassName: Map<String, String>,
    ): String {
        var availableParentVssPath = specModel.vssPath
        var parentSpecClassName: String? = null

        // Iterate up from the parent until the actual file name = class name was found. This indicates
        // that the actual parent is a sub class in this file.
        while (availableParentVssPath.contains(".")) {
            availableParentVssPath = availableParentVssPath.substringBeforeLast(".")

            parentSpecClassName = parentVssPathToClassName[availableParentVssPath]
            if (parentSpecClassName != null) break
        }

        if (parentSpecClassName == null) {
            logger.info("Could not create import string for: ${specModel.vssPath} - No parent was found")
            return ""
        }

        val parentClassName = specModel.parentClassName

        return if (parentSpecClassName != parentClassName) {
            "$parentSpecClassName.$parentClassName" // Sub class in another file
        } else {
            parentClassName // Main class = File name
        }
    }

    private companion object {
        private val fileSeparator = File.separator
        private const val PACKAGE_NAME = "org.eclipse.velocitas.vss"
    }
}
