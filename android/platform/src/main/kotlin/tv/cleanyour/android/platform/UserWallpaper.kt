package tv.cleanyour.android.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.nio.ByteBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tv.cleanyour.core.model.BlobRef
import tv.cleanyour.core.model.Sha256

/** Decode untrusted picker content with bounded memory, strip metadata, and retain a private JPEG. */
object UserWallpaper {
    suspend fun import(context: Context, uri: Uri, directory: File): BlobRef = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                require(out.size() + n <= 20 * 1024 * 1024) { "Choose a photo smaller than 20 MB." }
                out.write(buffer, 0, n)
            }
            out.toByteArray()
        } ?: error("The photo could not be opened.")
        // ImageDecoder honors camera orientation before the metadata is stripped from our JPEG.
        val decoded = ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(bytes))) { decoder, info, _ ->
            require(info.size.width > 0 && info.size.height > 0) { "Choose a supported photo." }
            var sample = 1
            while (maxOf(info.size.width, info.size.height) / sample > 2048) sample *= 2
            decoder.setTargetSampleSize(sample)
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        val jpeg = try {
            ByteArrayOutputStream().use { out ->
                check(decoded.compress(Bitmap.CompressFormat.JPEG, 88, out))
                out.toByteArray()
            }
        } finally { decoded.recycle() }
        require(jpeg.size <= 4 * 1024 * 1024) { "Choose a smaller photo." }
        val hash = MessageDigest.getInstance("SHA-256").digest(jpeg).joinToString("") { "%02x".format(it.toInt() and 255) }
        check(directory.isDirectory || directory.mkdirs())
        val file = File(directory, "$hash.jpg")
        if (!file.exists()) {
            val temp = File.createTempFile("photo-", ".tmp", directory)
            try { temp.writeBytes(jpeg); check(temp.renameTo(file)) } finally { temp.delete() }
        }
        BlobRef(Sha256(hash), jpeg.size.toLong(), file.absolutePath)
    }
}
