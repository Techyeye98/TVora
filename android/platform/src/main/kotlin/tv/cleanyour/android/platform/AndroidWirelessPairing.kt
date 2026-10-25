package tv.cleanyour.android.platform

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.net.InetSocketAddress
import java.net.Socket
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPublicKey
import javax.net.ssl.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.conscrypt.Conscrypt
import tv.cleanyour.adb.pairing.PairingProtocol
import tv.cleanyour.adb.pairing.PairingCipher
import tv.cleanyour.adb.pairing.SpakeExchange
import io.github.muntashirakon.crypto.spake2.Spake2Context
import io.github.muntashirakon.crypto.spake2.Spake2Role
import tv.cleanyour.adb.wire.AdbPublicKeyCodec
import tv.cleanyour.adb.wire.AdbSocket
import tv.cleanyour.adb.wire.TlsUpgrader
import tv.cleanyour.core.flows.WirelessPairing
import tv.cleanyour.core.model.Endpoint
import tv.cleanyour.core.model.Sha256

/** Separate non-exportable TLS identity; the existing legacy ADB signing key stays untouched. */
class AndroidWirelessPairing(context: Context) : WirelessPairing, TlsUpgrader {
    private val prefs = context.getSharedPreferences("wireless-adb", Context.MODE_PRIVATE)
    private val alias = "tvstudio.adb.tls.v1"

    override fun endpoint(host: String, discoveredPort: Int?): Endpoint.Tls? {
        if (!PlainSocketFactory.isLiteralAddress(host) || !prefs.getBoolean("paired:$host", false)) return null
        val port = discoveredPort ?: prefs.getInt("port:$host", 0)
        return if (port in 1..65535) Endpoint.Tls(host, port, null) else null
    }

    override suspend fun pair(host: String, pairingPort: Int, connectPort: Int, code: String) = withContext(Dispatchers.IO) {
        require(PlainSocketFactory.isLiteralAddress(host))
        require(pairingPort in 1..65535 && connectPort in 1..65535 && code.matches(Regex("[0-9]{6}")))
        val ctx = sslContext()
        Socket().use { raw ->
            raw.connect(InetSocketAddress(host, pairingPort), 10_000)
            raw.soTimeout = 20_000
            (ctx.socketFactory.createSocket(raw, host, pairingPort, true) as SSLSocket).use { ssl ->
                ssl.enabledProtocols = arrayOf("TLSv1.3")
                ssl.soTimeout = 20_000
                ssl.startHandshake()
                val exporter = Conscrypt.exportKeyingMaterial(ssl, "adb-label\u0000", null, 64)
                    ?: error("TLS exporter unavailable")
                val codeBytes = code.toByteArray(Charsets.US_ASCII)
                val password = codeBytes + exporter
                codeBytes.fill(0); exporter.fill(0)
                val cert = identity().getCertificate(alias) as X509Certificate
                val cipher = try { nativeCipher(password) } finally { password.fill(0) }
                PairingProtocol.exchange(ssl.inputStream, ssl.outputStream, cipher,
                    AdbPublicKeyCodec.line(cert.publicKey as RSAPublicKey))
                // Pairing authorizes our client key on the TV. The pairing server uses a temporary
                // certificate, not adbd's connection certificate (AOSP pairing_server_new_no_cert).
                check(prefs.edit().putBoolean("paired:$host", true).putInt("port:$host", connectPort).commit())
            }
        }
    }

    override suspend fun upgrade(socket: AdbSocket, pin: Sha256?): AdbSocket = withContext(Dispatchers.IO) {
        val raw = (socket as? TcpAdbSocket)?.socket ?: error("Unsupported TLS socket")
        val host = raw.inetAddress.hostAddress ?: error("Missing TV address")
        if (!prefs.getBoolean("paired:$host", false)) throw CertificateException("Pair this TV first")
        // The wire protocol supplies no authenticated server certificate pin. Do not silently
        // ignore a pin if a future caller starts supplying one.
        require(pin == null) { "Server pin verification is not available" }
        val ssl = sslContext().socketFactory.createSocket(raw, host, raw.port, true) as SSLSocket
        try {
            ssl.enabledProtocols = arrayOf("TLSv1.3")
            ssl.soTimeout = 15_000
            ssl.startHandshake()
            ssl.soTimeout = 0
            TcpAdbSocket(ssl)
        } catch (e: Exception) { ssl.close(); throw e }
    }

    @Synchronized private fun identity(): KeyStore {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!store.containsAlias(alias)) {
            KeyPairGenerator.getInstance("RSA", "AndroidKeyStore").apply {
                initialize(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
                    .setKeySize(2048).setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA384, KeyProperties.DIGEST_SHA512)
                    .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1, KeyProperties.SIGNATURE_PADDING_RSA_PSS).build())
            }.generateKeyPair()
        }
        return store
    }

    private fun nativeCipher(password: ByteArray): PairingCipher {
        val native = try {
            Spake2Context(Spake2Role.Alice, "adb pair client\u0000".toByteArray(Charsets.UTF_8),
                "adb pair server\u0000".toByteArray(Charsets.UTF_8))
        } catch (e: LinkageError) { throw java.io.IOException("Pairing is unavailable on this phone", e) }
        val firstMessage = try { native.generateMessage(password) } catch (e: Exception) { native.destroy(); throw e }
        return PairingCipher(object : SpakeExchange {
            override val message: ByteArray = firstMessage
            override fun sharedSecret(peer: ByteArray): ByteArray = native.processMessage(peer)
            override fun close() { native.destroy() }
        })
    }

    @android.annotation.SuppressLint("CustomX509TrustManager")
    private fun sslContext(): SSLContext {
        val keys = identity()
        val cert = keys.getCertificate(alias) as X509Certificate
        val key = keys.getKey(alias, null) as PrivateKey
        val manager = object : X509ExtendedKeyManager() {
            override fun getClientAliases(type: String?, issuers: Array<out java.security.Principal>?) = arrayOf(alias)
            override fun chooseClientAlias(types: Array<out String>?, issuers: Array<out java.security.Principal>?, socket: Socket?) = alias
            override fun chooseEngineClientAlias(types: Array<out String>?, issuers: Array<out java.security.Principal>?, engine: SSLEngine?) = alias
            override fun getServerAliases(type: String?, issuers: Array<out java.security.Principal>?) = null
            override fun chooseServerAlias(type: String?, issuers: Array<out java.security.Principal>?, socket: Socket?) = null
            override fun getCertificateChain(name: String?) = if (name == alias) arrayOf(cert) else null
            override fun getPrivateKey(name: String?) = if (name == alias) key else null
        }
        val trust = object : X509TrustManager {
            override fun getAcceptedIssuers() = emptyArray<X509Certificate>()
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) { throw CertificateException("Not a server") }
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                val leaf = chain?.firstOrNull() ?: throw CertificateException("Missing TV certificate")
                // ADB uses self-signed device certificates and authenticates the client key.
                // Pairing authenticates its TLS channel using PAKE + exporter; Setup retains the
                // existing explicit TV-identity check before any cleanup can run.
                leaf.checkValidity()
            }
        }
        return SSLContext.getInstance("TLSv1.3", Conscrypt.newProvider()).apply {
            init(arrayOf(manager), arrayOf(trust), null)
        }
    }


    suspend fun reset() = withContext(Dispatchers.IO) {
        val keys = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        keys.deleteEntry(alias)
        check(prefs.edit().clear().commit())
    }
}
