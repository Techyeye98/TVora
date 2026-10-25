package tv.cleanyour.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tv.cleanyour.core.flows.StoreRules
import tv.cleanyour.core.model.*
import tv.cleanyour.core.planner.*
import tv.cleanyour.core.registry.*
import tv.cleanyour.testing.fixtures.Samples
import kotlin.test.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class FullStoreTest {
    @Test fun tvoraDoesNotContactTheUpstreamAppUpdateFeed() = runBlocking {
        val http = object : HttpFetcher {
            override suspend fun get(url: String, etag: String?, rangeFrom: Long?): HttpResponse = error("No update request expected")
        }
        assertNull(Store.updateChecker(http).latestNewerThan("0.1.0-preview01"))
        assertNull(Store.releasePage)
        assertFalse(Store.rules.updateCheck)
    }

    @Test fun tvoraKeepsTvSoftwareUpdatesOn() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val graph = AppGraph(context)
        try {
            assertEquals(StoreRules.FULL.copy(updateCheck = false, updatesOff = false), graph.manageDeps.store)
            val ota = PackageName("com.mitv.ota")
            val on = PackageState(EnabledState.ENABLED, true, 1, null, false, false, null, false)
            val snap = Samples.snapshot(packages = Samples.miTvPackages(emptySet()) + (ota to on))
            val result = graph.planner.plan(PlanRequest.SwitchOff(setOf(ota)), snap, graph.facts, emptyList())
            assertIs<PlanResult.Refused>(result)
        } finally { graph.close() }
    }
}
