package tv.cleanyour.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import tv.cleanyour.core.flows.UpdateChecker
import tv.cleanyour.core.registry.HttpFetcher
import java.io.ByteArrayOutputStream

// Only release metadata is fetched. Installation stays with the owner on GitHub's release page.
internal class GitHubUpdateChecker(private val http: HttpFetcher) : UpdateChecker {
    override suspend fun latestNewerThan(current: String): String? = withContext(Dispatchers.IO) {
        val response = http.get(ENDPOINT, null, null)
        response.body?.use { input ->
            if (response.code != 200 || (response.length ?: 0) > LIMIT) return@withContext null
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (out.size() + count > LIMIT) return@withContext null
                out.write(buffer, 0, count)
            }
            val release = Json.parseToJsonElement(out.toString("UTF-8")).jsonObject
            if (release["draft"]?.jsonPrimitive?.booleanOrNull != false ||
                release["prerelease"]?.jsonPrimitive?.booleanOrNull != false) return@withContext null
            val tag = release["tag_name"]?.jsonPrimitive?.content ?: return@withContext null
            val latest = version(tag) ?: return@withContext null
            val installed = version(current.substringBefore('-').substringBefore('+')) ?: return@withContext null
            val comparison = latest.zip(installed).firstOrNull { (a, b) -> a != b }?.let { (a, b) -> a.compareTo(b) } ?: 0
            if (comparison > 0 || (comparison == 0 && '-' in current)) tag.removePrefix("v") else ""
        }
    }

    private fun version(value: String): List<Int>? {
        val match = Regex("^v?([0-9]+)\\.([0-9]+)\\.([0-9]+)$").matchEntire(value) ?: return null
        return match.groupValues.drop(1).map { it.toIntOrNull() ?: return null }
    }

    companion object {
        const val ENDPOINT = "https://api.github.com/repos/Techyeye98/TVora/releases/latest"
        private const val LIMIT = 256 * 1024
    }
}
