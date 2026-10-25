package tv.cleanyour.core.flows

import tv.cleanyour.core.model.Endpoint

interface WirelessPairing {
    fun endpoint(host: String, discoveredPort: Int? = null): Endpoint.Tls?
    suspend fun pair(host: String, pairingPort: Int, connectPort: Int, code: String)
}
