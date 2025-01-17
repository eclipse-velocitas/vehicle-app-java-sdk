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

package org.eclipse.velocitas.vssprocessor.parser.factory

import java.io.File
import org.eclipse.velocitas.vssprocessor.parser.VssParser
import org.eclipse.velocitas.vssprocessor.parser.factory.VssFileExtension.JSON
import org.eclipse.velocitas.vssprocessor.parser.factory.VssFileExtension.YAML
import org.eclipse.velocitas.vssprocessor.parser.json.JsonVssParser
import org.eclipse.velocitas.vssprocessor.parser.yaml.YamlVssParser

internal class VssParserFactory {

    /**
     * @throws IllegalStateException when the specified extension is not supported
     */
    fun create(extension: String): VssParser {
        return when {
            JSON.fileExtensions.contains(extension) -> {
                JsonVssParser()
            }

            YAML.fileExtensions.contains(extension) -> {
                YamlVssParser()
            }

            else -> {
                error("Could not create VssDefinitionParser: File Extension '$extension' not supported")
            }
        }
    }

    /**
     * @throws IllegalStateException when the extension of the specified file is not supported
     */
    fun create(file: File): VssParser {
        val fileName = file.name // with extension
        val fileExtension = fileName.substringAfterLast(".")

        return create(fileExtension)
    }
}
