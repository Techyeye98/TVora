package tv.cleanyour.adb.pairing

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.IOException

/** AOSP pairing v1 inside the TLS channel. Password includes the TLS exporter bytes. */
object PairingProtocol {
    const val PEER_SIZE = 8192
    fun exchange(input: InputStream, output: OutputStream, auth: PairingCipher, publicKey: String) {
        val key = publicKey.toByteArray(Charsets.US_ASCII)
        require(key.size < PEER_SIZE - 1 && !publicKey.contains('\n'))
        try {
            val reader = DataInputStream(input)
            val writer = DataOutputStream(output)
            fun send(type: Int, payload: ByteArray) {
                writer.writeByte(1); writer.writeByte(type); writer.writeInt(payload.size)
                writer.write(payload); writer.flush()
            }
            fun read(type: Int, size: Int): ByteArray {
                if (reader.readUnsignedByte() != 1 || reader.readUnsignedByte() != type || reader.readInt() != size)
                    throw IOException("Invalid pairing packet")
                return ByteArray(size).also { reader.readFully(it) }
            }
            send(0, auth.message)
            if (!auth.init(read(0, 32))) throw IOException("Pairing key exchange failed")
            val ours = ByteArray(PEER_SIZE)
            key.copyInto(ours, 1) // peer type 0: ADB RSA public key, NUL terminated
            val encrypted = auth.encrypt(ours) ?: throw IOException("Pairing encryption failed")
            ours.fill(0)
            send(1, encrypted)
            val peer = auth.decrypt(read(1, PEER_SIZE + 16)) ?: throw IOException("Pairing code did not match")
            try {
                // ADB_DEVICE_GUID is type 1. No peer identity is accepted before GCM authentication.
                if (peer.size != PEER_SIZE || peer[0] != 1.toByte() || peer[1] == 0.toByte())
                    throw IOException("Invalid pairing peer")
            } finally { peer.fill(0) }
        } finally { auth.close() }
    }
}
