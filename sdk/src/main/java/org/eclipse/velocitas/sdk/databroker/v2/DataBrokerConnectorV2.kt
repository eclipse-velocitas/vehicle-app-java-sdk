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

package org.eclipse.velocitas.sdk.databroker.v2

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import io.grpc.ConnectivityState
import io.grpc.ManagedChannel
import org.eclipse.kuksa.connectivity.authentication.JsonWebToken
import org.eclipse.kuksa.connectivity.databroker.DataBrokerException
import org.eclipse.kuksa.model.TimeoutConfig
import org.eclipse.velocitas.sdk.logging.Logger

private const val TAG = "DataBrokerConnectorV2"

/**
 * The DataBrokerConnector is used to establish a successful connection to the DataBroker. The communication takes
 * place inside the [managedChannel]. Use the [defaultDispatcher] for the coroutine scope.
 */
class DataBrokerConnectorV2 @JvmOverloads constructor(
    private val managedChannel: ManagedChannel,
    private val jsonWebToken: JsonWebToken? = null,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {

    /**
     * Configuration to be used during connection.
     */
    var timeoutConfig = TimeoutConfig()

    /**
     * Connects to the specified DataBroker.
     *
     * @throws DataBrokerException when connect() is called again while it is trying to connect (fail fast) resp.
     * when no connection to the DataBroker can be established (e.g. after the in #timeoutConfig defined time).
     */
    suspend fun connect(): DataBrokerConnectionV2 {
        val connectivityState = managedChannel.getState(false)
        if (connectivityState != ConnectivityState.IDLE) {
            throw DataBrokerException("Connector is already trying to establish a connection")
        }

        Logger.debug(TAG, "connect() called")

        return withContext(defaultDispatcher) {
            val startTime = System.currentTimeMillis()
            val timeoutMillis = timeoutConfig.timeUnit.toMillis(timeoutConfig.timeout)
            var durationMillis = 0L

            @Suppress("MagicNumber") // self explanatory number
            val delayMillis = 1000L
            var state = managedChannel.getState(true) // is there no other way to connect?
            while (state != ConnectivityState.READY && durationMillis < timeoutMillis - delayMillis) {
                durationMillis = System.currentTimeMillis() - startTime
                state = managedChannel.getState(false)

                delay(delayMillis)
            }

            if (state == ConnectivityState.READY) {
                return@withContext DataBrokerConnectionV2(managedChannel)
                    .apply {
                        jsonWebToken = this@DataBrokerConnectorV2.jsonWebToken
                    }
            } else {
                managedChannel.shutdownNow()
                throw DataBrokerException("timeout")
            }
        }
    }
}
