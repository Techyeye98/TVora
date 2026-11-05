package tv.cleanyour.app

import java.io.ByteArrayInputStream
import kotlinx.coroutines.runBlocking
import org.junit.Test
import tv.cleanyour.core.registry.HttpFetcher
import tv.cleanyour.core.registry.HttpResponse
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GitHubUpdateCheckerTest {
    private fun checker(tag: String, code: Int = 200, draft: Boolean = false, prerelease: Boolean = false) =
        GitHubUpdateChecker(object : HttpFetcher {
            override suspend fun get(url: String, etag: String?, rangeFrom: Long?): HttpResponse {
                assertEquals("https://api.github.com/repos/Techyeye98/TVora/releases/latest", url)
                val bytes = """{"tag_name":"$tag","draft":$draft,"prerelease":$prerelease}""".toByteArray()
                return HttpResponse(code, null, ByteArrayInputStream(bytes), bytes.size.toLong())
            }
        })

    @Test fun newerStableVersionIsOffered() = runBlocking {
        assertEquals("0.1.2", checker("v0.1.2").latestNewerThan("0.1.1"))
        assertEquals("0.10.0", checker("v0.10.0").latestNewerThan("0.9.9"))
        assertEquals("0.1.2", checker("v0.1.2").latestNewerThan("0.1.2-preview01"))
    }

    @Test fun equalOrOlderReleaseIsNotOffered() = runBlocking {
        assertEquals("", checker("v0.1.2").latestNewerThan("0.1.2"))
        assertEquals("", checker("v0.1.1").latestNewerThan("0.1.2"))
    }

    @Test fun invalidOrNonPublicVersionsAreRejected() = runBlocking {
        assertNull(checker("v0.1.3-beta").latestNewerThan("0.1.2"))
        assertNull(checker("v0.1.3", draft = true).latestNewerThan("0.1.2"))
        assertNull(checker("v0.1.3", prerelease = true).latestNewerThan("0.1.2"))
        assertNull(checker("v99999999999999.1.3").latestNewerThan("0.1.2"))
    }

    @Test fun httpFailureIsNotReportedAsUpToDate() = runBlocking {
        assertNull(checker("v0.1.3", code = 403).latestNewerThan("0.1.2"))
    }

    @Test fun oversizedResponseIsRejectedEvenWithoutContentLength() = runBlocking {
        var closed = false
        val http = object : HttpFetcher {
            override suspend fun get(url: String, etag: String?, rangeFrom: Long?) = HttpResponse(200, null,
                object : ByteArrayInputStream(ByteArray(256 * 1024 + 1)) {
                    override fun close() { closed = true; super.close() }
                }, null)
        }
        assertNull(GitHubUpdateChecker(http).latestNewerThan("0.1.2"))
        assertEquals(true, closed)
    }
}
