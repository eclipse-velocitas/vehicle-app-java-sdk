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

package org.eclipse.velocitas.sdk.grpc

import kotlinx.coroutines.flow.Flow
import io.grpc.ChannelCredentials
import io.grpc.Grpc
import io.grpc.InsecureChannelCredentials
import io.grpc.ManagedChannel
import org.eclipse.kuksa.proto.v2.KuksaValV2
import org.eclipse.kuksa.proto.v2.KuksaValV2.ActuateResponse
import org.eclipse.kuksa.proto.v2.KuksaValV2.BatchActuateResponse
import org.eclipse.kuksa.proto.v2.KuksaValV2.GetServerInfoResponse
import org.eclipse.kuksa.proto.v2.KuksaValV2.GetValueResponse
import org.eclipse.kuksa.proto.v2.KuksaValV2.GetValuesResponse
import org.eclipse.kuksa.proto.v2.KuksaValV2.ListMetadataResponse
import org.eclipse.kuksa.proto.v2.KuksaValV2.OpenProviderStreamRequest
import org.eclipse.kuksa.proto.v2.KuksaValV2.OpenProviderStreamResponse
import org.eclipse.kuksa.proto.v2.KuksaValV2.PublishValueResponse
import org.eclipse.kuksa.proto.v2.KuksaValV2.SubscribeResponse
import org.eclipse.kuksa.proto.v2.Types.Datapoint
import org.eclipse.kuksa.proto.v2.Types.SignalID
import org.eclipse.kuksa.proto.v2.VALGrpcKt
import org.eclipse.kuksa.proto.v2.actuateRequest
import org.eclipse.kuksa.proto.v2.batchActuateRequest
import org.eclipse.kuksa.proto.v2.getServerInfoRequest
import org.eclipse.kuksa.proto.v2.getValueRequest
import org.eclipse.kuksa.proto.v2.getValuesRequest
import org.eclipse.kuksa.proto.v2.listMetadataRequest
import org.eclipse.kuksa.proto.v2.publishValueRequest
import org.eclipse.kuksa.proto.v2.subscribeByIdRequest
import org.eclipse.kuksa.proto.v2.subscribeRequest
import org.eclipse.velocitas.sdk.logging.Logger

private const val TAG = "CoroutineBrokerGrpcFacade"

/**
 * CoroutineBrokerGrpcFacade provides synchronous communication using coroutines against the VehicleDataBroker.
 * The current implementation uses the 'kuksa.val.v2' protocol which can be found here:
 * https://github.com/eclipse-kuksa/kuksa-databroker/tree/main/proto/kuksa/val/v2.
 */
@Suppress("TooManyFunctions")
class CoroutineBrokerGrpcFacade(
    host: String,
    port: Int,
    channelCredentials: ChannelCredentials = InsecureChannelCredentials.create(),
) : GrpcClient {
    private val channel: ManagedChannel = Grpc.newChannelBuilderForAddress(
        host,
        port,
        channelCredentials,
    ).build()

    private val coroutineStub: VALGrpcKt.VALCoroutineStub = VALGrpcKt.VALCoroutineStub(channel)

    init {
        Logger.info(TAG, "Connecting to gRPC service at $host:$port")
        channel.getState(true)
    }

    /**
     * Gets the latest value of a [signalId].
     *
     * The server might respond with the following GRPC error codes:
     *    NOT_FOUND if the requested signal doesn't exist
     *    PERMISSION_DENIED if access is denied
     */
    suspend fun getValue(signalId: SignalID): GetValueResponse {
        val request = getValueRequest {
            this.signalId = signalId
        }
        return coroutineStub.getValue(request)
    }

    /**
     * Gets the latest values of a set of [signalIds]. The returned list of data points has the same order as the list
     * of the request.
     *
     * The server might respond with the following GRPC error codes:
     *    NOT_FOUND if any of the requested signals doesn't exist.
     *    PERMISSION_DENIED if access is denied for any of the requested signals.
     */
    suspend fun getValues(signalIds: List<SignalID>): GetValuesResponse {
        val request = getValuesRequest {
            this.signalIds.addAll(signalIds)
        }

        return coroutineStub.getValues(request)
    }

    /**
     * Subscribe to a set of signals using i32 id parameters
     * Returns (GRPC error code):
     *    NOT_FOUND if any of the signals are non-existent.
     *    PERMISSION_DENIED if access is denied for any of the signals.
     */
    fun subscribeById(
        signalIds: List<Int>,
    ): Flow<KuksaValV2.SubscribeByIdResponse> {
        val request = subscribeByIdRequest {
            this.signalIds.addAll(signalIds)
        }

        return coroutineStub.subscribeById(request)
    }

    /**
     * Subscribe to a set of signals using string path parameters
     * Returns (GRPC error code):
     *    NOT_FOUND if any of the signals are non-existent.
     *    PERMISSION_DENIED if access is denied for any of the signals.
     *
     * When subscribing the Broker shall immediately return the value for all
     * subscribed entries. If no value is available when subscribing a DataPoint
     * with value None shall be returned.
     */
    fun subscribe(
        signalPaths: List<String>,
    ): Flow<SubscribeResponse> {
        val request = subscribeRequest {
            this.signalPaths.addAll(signalPaths)
        }

        return coroutineStub.subscribe(request)
    }

    /**
     * Actuates a single actuator with the specified [signalId].
     *
     * The server might respond with the following GRPC error codes:
     *    NOT_FOUND if the actuator does not exist.
     *    PERMISSION_DENIED if access is denied for of the actuator.
     *    UNAVAILABLE if there is no provider currently providing the actuator
     *    INVALID_ARGUMENT
     *        - if the data type used in the request does not match the data type of the addressed signal
     *        - if the requested value is not accepted, e.g. if sending an unsupported enum value
     */
    suspend fun actuate(signalId: SignalID): ActuateResponse {
        val request = actuateRequest {
            this.signalId = signalId
        }

        return coroutineStub.actuate(request)
    }

    /**
     * Actuates simultaneously multiple actuators with the specified [signalIds].
     * If any error occurs, the entire operation will be aborted and no single actuator value will be forwarded to the
     * provider.
     *
     * The server might respond with the following GRPC error codes:
     *     NOT_FOUND if any of the actuators are non-existent.
     *     PERMISSION_DENIED if access is denied for any of the actuators.
     *     UNAVAILABLE if there is no provider currently providing an actuator
     *     INVALID_ARGUMENT
     *         - if the data type used in the request does not match the data type of the addressed signal
     *         - if the requested value is not accepted, e.g. if sending an unsupported enum value
     *
     */
    suspend fun batchActuate(signalIds: List<SignalID>): BatchActuateResponse {
        val request = batchActuateRequest {
            signalIds.forEach { signalId ->
                val actuateRequest = actuateRequest {
                    this.signalId = signalId
                }
                this.actuateRequests.add(actuateRequest)
            }
        }

        return coroutineStub.batchActuate(request)
    }

    /**
     * Lists metadata of signals matching the request.
     * If any error occurs, the entire operation will be aborted and no single actuator value will be forwarded to the
     * provider.
     *
     * The server might respond with the following GRPC error codes:
     *     NOT_FOUND if the specified root branch does not exist.
     */
    suspend fun listMetadata(
        root: String,
        filter: String,
    ): ListMetadataResponse {
        val request = listMetadataRequest {
            this.root = root
            this.filter = filter
        }

        return coroutineStub.listMetadata(request)
    }

    /**
     * Publishes a signal value. Used for low frequency signals (e.g. attributes).
     *
     * The server might respond with the following GRPC error codes:
     *     NOT_FOUND if any of the signals are non-existent.
     *     PERMISSION_DENIED
     *         - if access is denied for any of the signals.
     *         - if the signal is already provided by another provider.
     *     INVALID_ARGUMENT
     *        - if the data type used in the request does not match the data type of the addressed signal
     *        - if the published value is not accepted e.g. if sending an unsupported enum value
     */
    suspend fun publishValue(
        signalId: SignalID,
        datapoint: Datapoint,
    ): PublishValueResponse {
        val request = publishValueRequest {
            this.signalId = signalId
            this.dataPoint = datapoint
        }

        return coroutineStub.publishValue(request)
    }

    /**
     *  Open a stream used to provide actuation and/or publishing values using
     *  a streaming interface. Used to provide actuators and to enable high frequency
     *  updates of values.
     *
     *  The open stream is used for request / response type communication between the
     *  provider and server (where the initiator of a request can vary).
     *  Errors are communicated as messages in the stream.
     */
    fun openProviderStream(
        streamRequestFlow: Flow<OpenProviderStreamRequest>,
    ): Flow<OpenProviderStreamResponse> {
        return coroutineStub.openProviderStream(streamRequestFlow)
    }

    /**
     * Gets the server information.
     */
    suspend fun getServerInfo(): GetServerInfoResponse {
        val request = getServerInfoRequest { }

        return coroutineStub.getServerInfo(request)
    }
}
