package tv.cleanyour.adb.pairing

import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.modes.GCMBlockCipher
import org.bouncycastle.crypto.params.AEADParameters
import org.bouncycastle.crypto.params.HKDFParameters
import org.bouncycastle.crypto.params.KeyParameter
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Platform supplies the tested native SPAKE2 implementation; the wire module owns AEAD framing. */
interface SpakeExchange : AutoCloseable {
    val message: ByteArray
    fun sharedSecret(peer: ByteArray): ByteArray
}

// Adapted from libadb-android PairingAuthCtx, Apache-2.0 option. See notices/libadb-Apache-2.0.
class PairingCipher(private val exchange: SpakeExchange) : AutoCloseable {
    val message: ByteArray get() = exchange.message
    private val key = ByteArray(16)
    private var initialized = false
    private var closed = false
    private var sendIv = 0L
    private var receiveIv = 0L

    fun init(peer: ByteArray): Boolean {
        check(!closed && !initialized)
        val secret = exchange.sharedSecret(peer)
        try {
            HKDFBytesGenerator(SHA256Digest()).apply {
                init(HKDFParameters(secret, null, "adb pairing_auth aes-128-gcm key".toByteArray(Charsets.UTF_8)))
                generateBytes(key, 0, key.size)
            }
            initialized = true
            return true
        } finally { secret.fill(0) }
    }

    fun encrypt(bytes: ByteArray): ByteArray? = crypt(true, bytes, sendIv++)
    fun decrypt(bytes: ByteArray): ByteArray? = crypt(false, bytes, receiveIv++)

    private fun crypt(encrypt: Boolean, bytes: ByteArray, counter: Long): ByteArray? {
        check(initialized && !closed)
        val iv = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN).putLong(counter).array()
        val cipher = GCMBlockCipher.newInstance(AESEngine.newInstance())
        cipher.init(encrypt, AEADParameters(KeyParameter(key), 128, iv))
        val out = ByteArray(cipher.getOutputSize(bytes.size))
        return try {
            val count = cipher.processBytes(bytes, 0, bytes.size, out, 0)
            val total = count + cipher.doFinal(out, count)
            out.copyOf(total)
        } catch (e: org.bouncycastle.crypto.InvalidCipherTextException) { out.fill(0); null }
    }

    override fun close() { closed = true; key.fill(0); exchange.close() }
}
