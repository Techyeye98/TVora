package tv.cleanyour.adb.pairing

import java.io.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.*
import org.junit.Test

class PairingProtocolTest {
    // This test double supplies an agreed secret. These tests exercise the production HKDF/GCM
    // and wire validation; native SPAKE2 + Android Keystore/TLS still require Android hardware.
    private fun cipher(password: ByteArray) = PairingCipher(object : SpakeExchange {
        private val secret = java.security.MessageDigest.getInstance("SHA-512").digest(password)
        override val message = ByteArray(32) { 7 }
        override fun sharedSecret(peer: ByteArray): ByteArray { require(peer.size == 32); return secret.copyOf() }
        override fun close() { secret.fill(0) }
    })
    @Test(timeout = 15000) fun authenticatedExchangeSucceeds() { exchange(false) }
    @Test(timeout = 15000) fun wrongCodeCannotAuthenticatePeer() { exchange(true) }

    private fun exchange(wrongCode: Boolean) {
        val clientIn = PipedInputStream(65536)
        val serverOut = PipedOutputStream(clientIn)
        val serverIn = PipedInputStream(65536)
        val clientOut = PipedOutputStream(serverIn)
        val pool = Executors.newSingleThreadExecutor()
        val password = "123456 tls-exporter".toByteArray()
        val bob = cipher(password.copyOf())
        val peer = pool.submit {
            try {
                val input = DataInputStream(serverIn)
                val output = DataOutputStream(serverOut)
                fun read(type: Int, size: Int): ByteArray {
                    assertEquals(1, input.readUnsignedByte()); assertEquals(type, input.readUnsignedByte())
                    assertEquals(size, input.readInt())
                    return ByteArray(size).also(input::readFully)
                }
                fun send(type: Int, bytes: ByteArray) {
                    output.writeByte(1); output.writeByte(type); output.writeInt(bytes.size)
                    output.write(bytes); output.flush()
                }
                val alice = read(0, 32)
                send(0, bob.message)
                assertTrue(bob.init(alice))
                val ours = bob.decrypt(read(1, PairingProtocol.PEER_SIZE + 16))
                if (!wrongCode) { assertNotNull(ours); assertEquals(0, ours[0].toInt()) }
                else assertNull(ours)
                val guid = ByteArray(PairingProtocol.PEER_SIZE).apply { this[0] = 1; "test-tv".toByteArray().copyInto(this, 1) }
                send(1, bob.encrypt(guid)!!)
            } finally { bob.close(); serverOut.close() }
        }
        val secret = if (wrongCode) "000000 tls-exporter".toByteArray() else password.copyOf()
        try {
            val result = runCatching { PairingProtocol.exchange(clientIn, clientOut, cipher(secret), "test-key") }
            peer.get(10, TimeUnit.SECONDS)
            if (wrongCode) assertIs<IOException>(result.exceptionOrNull()) else result.getOrThrow()
        } finally {
            clientIn.close(); clientOut.close(); serverIn.close(); serverOut.close(); pool.shutdownNow()
        }
    }

    @Test fun oversizedPacketIsRejectedBeforeAllocation() {
        val invalid = byteArrayOf(1, 0, 0x7f, -1, -1, -1)
        assertFailsWith<IOException> {
            PairingProtocol.exchange(ByteArrayInputStream(invalid), ByteArrayOutputStream(), cipher("secret".toByteArray()), "key")
        }
    }
}
