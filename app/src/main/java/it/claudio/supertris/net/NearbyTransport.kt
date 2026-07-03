package it.claudio.supertris.net

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

sealed interface NearbyEvent {
    data class EndpointFound(val endpointId: String, val name: String) : NearbyEvent
    data class EndpointLost(val endpointId: String) : NearbyEvent
    data class ConnectionInitiated(val endpointId: String, val peerName: String, val authDigits: String) : NearbyEvent
    data class Connected(val endpointId: String) : NearbyEvent
    data class ConnectionFailed(val endpointId: String) : NearbyEvent
    data class Disconnected(val endpointId: String) : NearbyEvent
    data class MessageReceived(val endpointId: String, val message: NetMessage) : NearbyEvent
    data class OperationFailed(val what: String) : NearbyEvent
}

/**
 * Involucro sottile e senza stato di gioco attorno a Nearby Connections:
 * espone gli eventi come [SharedFlow] e i comandi come funzioni semplici.
 */
class NearbyTransport(context: Context) {

    private val client: ConnectionsClient = Nearby.getConnectionsClient(context.applicationContext)

    private val _events = MutableSharedFlow<NearbyEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<NearbyEvent> = _events

    private fun emit(event: NearbyEvent) {
        _events.tryEmit(event)
    }

    private val connectionLifecycle = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            emit(NearbyEvent.ConnectionInitiated(endpointId, info.endpointName, info.authenticationDigits))
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                emit(NearbyEvent.Connected(endpointId))
            } else {
                emit(NearbyEvent.ConnectionFailed(endpointId))
            }
        }

        override fun onDisconnected(endpointId: String) {
            emit(NearbyEvent.Disconnected(endpointId))
        }
    }

    private val discoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            emit(NearbyEvent.EndpointFound(endpointId, info.endpointName))
        }

        override fun onEndpointLost(endpointId: String) {
            emit(NearbyEvent.EndpointLost(endpointId))
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val bytes = payload.asBytes() ?: return
            val msg = NetCodec.decodeOrNull(bytes) ?: return
            emit(NearbyEvent.MessageReceived(endpointId, msg))
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) = Unit
    }

    fun startAdvertising(localName: String) {
        client.startAdvertising(
            localName,
            SERVICE_ID,
            connectionLifecycle,
            AdvertisingOptions.Builder().setStrategy(STRATEGY).build(),
        ).addOnFailureListener { emit(NearbyEvent.OperationFailed("advertising: ${it.message}")) }
    }

    fun startDiscovery() {
        client.startDiscovery(
            SERVICE_ID,
            discoveryCallback,
            DiscoveryOptions.Builder().setStrategy(STRATEGY).build(),
        ).addOnFailureListener { emit(NearbyEvent.OperationFailed("discovery: ${it.message}")) }
    }

    fun requestConnection(localName: String, endpointId: String) {
        client.requestConnection(localName, endpointId, connectionLifecycle)
            .addOnFailureListener { emit(NearbyEvent.ConnectionFailed(endpointId)) }
    }

    fun acceptConnection(endpointId: String) {
        client.acceptConnection(endpointId, payloadCallback)
    }

    fun rejectConnection(endpointId: String) {
        client.rejectConnection(endpointId)
    }

    fun stopAdvertising() {
        client.stopAdvertising()
    }

    fun stopDiscovery() {
        client.stopDiscovery()
    }

    fun send(endpointId: String, msg: NetMessage) {
        client.sendPayload(endpointId, Payload.fromBytes(NetCodec.encode(msg)))
    }

    fun stopAll() {
        client.stopAllEndpoints()
    }

    private companion object {
        // Identifica il "servizio" SuperTris: solo app con lo stesso id si vedono.
        const val SERVICE_ID = "it.claudio.supertris"
        val STRATEGY: Strategy = Strategy.P2P_POINT_TO_POINT
    }
}
