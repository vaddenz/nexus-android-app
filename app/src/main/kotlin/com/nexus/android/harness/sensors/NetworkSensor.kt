package com.nexus.android.harness.sensors

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.nexus.harness.sensor.Sensor
import com.nexus.harness.sensor.SensorKind
import com.nexus.harness.sensor.SensorPolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Snapshot of the network reachability state. */
data class NetworkReading(
    val online: Boolean,
    val transport: Transport,
    val unmetered: Boolean,
)

enum class Transport { WIFI, CELLULAR, ETHERNET, BLUETOOTH, NONE }

/**
 * Reactive sensor using [ConnectivityManager.NetworkCallback].
 *
 * Lifecycle safety: `awaitClose` unregisters the callback when the collector is
 * cancelled, so a deactivated sensor never leaks the system callback.
 */
@Singleton
class NetworkSensor @Inject constructor(
    @ApplicationContext private val context: Context,
) : Sensor<NetworkReading> {

    override val id = "network"
    override val kind = SensorKind.SYSTEM
    override val policy = SensorPolicy.Reactive

    private val cm: ConnectivityManager
        get() = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override fun observe(): Flow<NetworkReading> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(readingFor(cm.getNetworkCapabilities(network)))
            }
            override fun onLost(network: Network) {
                trySend(NetworkReading(online = false, transport = Transport.NONE, unmetered = false))
            }
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(readingFor(capabilities))
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(request, callback)
        // Seed with current state so consumers don't wait for the first transition.
        trySend(readingFor(cm.getNetworkCapabilities(cm.activeNetwork)))
        awaitClose { cm.unregisterNetworkCallback(callback) }
    }

    private fun readingFor(capabilities: NetworkCapabilities?): NetworkReading {
        if (capabilities == null) {
            return NetworkReading(online = false, transport = Transport.NONE, unmetered = false)
        }
        val transport = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Transport.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> Transport.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> Transport.ETHERNET
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> Transport.BLUETOOTH
            else -> Transport.NONE
        }
        return NetworkReading(
            online = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
            transport = transport,
            unmetered = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED),
        )
    }
}
