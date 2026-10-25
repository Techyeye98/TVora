package tv.cleanyour.android.platform

import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tv.cleanyour.core.model.Endpoint

object EndpointProbe {
    suspend fun open(endpoint: Endpoint): Boolean? = withContext(Dispatchers.IO) {
        require(PlainSocketFactory.isLiteralAddress(endpoint.host) && endpoint.port in 1..65535)
        try {
            Socket().use { it.connect(InetSocketAddress(endpoint.host, endpoint.port), 1500) }
            true
        } catch (e: ConnectException) { false
        } catch (e: java.io.IOException) { null }
    }
}
