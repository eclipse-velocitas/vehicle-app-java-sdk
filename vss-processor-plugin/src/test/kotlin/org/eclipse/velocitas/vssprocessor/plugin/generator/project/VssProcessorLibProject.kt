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

import java.io.File
import java.nio.file.Path
import kotlin.io.path.createDirectories

class VssProcessorLibProject(name: String) : AndroidLibProject(name) {
    val vssDir = rootProjectDir.resolve(VSS_DIR_NAME).createDirectories()
    val vssDir2 = rootProjectDir.resolve("${VSS_DIR_NAME}_2").createDirectories()

    fun copyVssFiles(directory: Path, fileName: String): File {
        val certificateUrl = VssProcessorLibProject::class.java.classLoader?.getResource(fileName)!!
        val certificateFile = File(certificateUrl.toURI())

        val targetLocation = directory.resolve(certificateFile.name).toFile()
        return certificateFile.copyTo(targetLocation, true)
    }

    companion object {
        const val VSS_DIR_NAME = "vss"
    }
}
