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

package org.eclipse.velocitas.sdk.logging

import io.kotest.core.config.LogLevel
import io.kotest.core.spec.style.BehaviorSpec
import org.eclipse.velocitas.kotest.Unit

private const val TAG = "LoggerTest"

class LoggerTest : BehaviorSpec({
    tags(Unit)

    Logger.loggingStrategy = StringLoggingStrategy

    given("An errorMessage w/o arguments") {
        val errorMessage = "Error: Some Error Occurred - Errorcode: "
        var counter = 0

        `when`("Logging on Verbose Level") {
            counter++
            Logger.verbose(TAG, "$errorMessage ($counter)")

            then("It should log the correct message on the correct level") {
                StringLoggingStrategy.lastLevel = LogLevel.Trace
                StringLoggingStrategy.lastMessage = "$errorMessage ($counter)"
            }
        }

        `when`("Logging on Debug Level") {
            counter++
            Logger.debug(TAG, "$errorMessage ($counter)")

            then("It should log the correct message on the correct level") {
                StringLoggingStrategy.lastLevel = LogLevel.Debug
                StringLoggingStrategy.lastMessage = "$errorMessage ($counter)"
            }
        }

        `when`("Logging on Info Level") {
            counter++
            Logger.info(TAG, "$errorMessage ($counter)")

            then("It should log the correct message on the correct level") {
                StringLoggingStrategy.lastLevel = LogLevel.Info
                StringLoggingStrategy.lastMessage = "$errorMessage ($counter)"
            }
        }

        `when`("Logging on Warn Level") {
            counter++
            Logger.warn(TAG, "$errorMessage ($counter)")

            then("It should log the correct message on the correct level") {
                StringLoggingStrategy.lastLevel = LogLevel.Warn
                StringLoggingStrategy.lastMessage = "$errorMessage ($counter)"
            }
        }

        `when`("Logging on Error Level") {
            counter++

            Logger.error(TAG, "$errorMessage ($counter)")

            then("It should log the correct message on the correct level") {
                StringLoggingStrategy.lastLevel = LogLevel.Error
                StringLoggingStrategy.lastMessage = "$errorMessage ($counter)"
            }
        }
    }

    given("An errorMessage w/ arguments") {
        val errorMessage = "Error: Some Error Occurred"
        var counter = 0

        `when`("Logging on Verbose Level") {
            counter++
            Logger.verbose(TAG, "$errorMessage (%s)", counter)

            then("It should log the correct message on the correct level") {
                StringLoggingStrategy.lastLevel = LogLevel.Trace
                StringLoggingStrategy.lastMessage = "$errorMessage ($counter)"
            }
        }

        `when`("Logging on Debug Level") {
            counter++
            Logger.debug(TAG, "$errorMessage (%s)", counter)

            then("It should log the correct message on the correct level") {
                StringLoggingStrategy.lastLevel = LogLevel.Debug
                StringLoggingStrategy.lastMessage = "$errorMessage ($counter)"
            }
        }

        `when`("Logging on Info Level") {
            counter++
            Logger.info(TAG, "$errorMessage (%s)", counter)

            then("It should log the correct message on the correct level") {
                StringLoggingStrategy.lastLevel = LogLevel.Info
                StringLoggingStrategy.lastMessage = "$errorMessage ($counter)"
            }
        }

        `when`("Logging on Warn Level") {
            counter++
            Logger.warn(TAG, "$errorMessage (%s)", counter)

            then("It should log the correct message on the correct level") {
                StringLoggingStrategy.lastLevel = LogLevel.Warn
                StringLoggingStrategy.lastMessage = "$errorMessage ($counter)"
            }
        }

        `when`("Logging on Error Level") {
            counter++
            Logger.error(TAG, "$errorMessage (%s)", counter)

            then("It should log the correct message on the correct level") {
                StringLoggingStrategy.lastLevel = LogLevel.Error
                StringLoggingStrategy.lastMessage = "$errorMessage ($counter)"
            }
        }
    }
})

object StringLoggingStrategy : LoggingStrategy {
    var lastLevel: LogLevel? = null
    var lastMessage: String? = null
    var lastTag: String? = null

    override fun info(tag: String, message: String) {
        lastLevel = LogLevel.Info
        lastTag = tag
        lastMessage = message
    }

    override fun warn(tag: String, message: String) {
        lastLevel = LogLevel.Warn
        lastTag = tag
        lastMessage = message
    }

    override fun error(tag: String, message: String) {
        lastLevel = LogLevel.Error
        lastTag = tag
        lastMessage = message
    }

    override fun debug(tag: String, message: String) {
        lastLevel = LogLevel.Debug
        lastTag = tag
        lastMessage = message
    }

    override fun verbose(tag: String, message: String) {
        lastLevel = LogLevel.Trace
        lastTag = tag
        lastMessage = message
    }
}
