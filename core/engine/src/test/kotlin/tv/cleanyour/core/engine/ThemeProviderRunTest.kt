package tv.cleanyour.core.engine

import kotlinx.coroutines.test.runTest
import tv.cleanyour.core.model.BlobRef
import tv.cleanyour.core.model.EnabledState
import tv.cleanyour.core.model.FileRoute
import tv.cleanyour.core.model.Op
import tv.cleanyour.core.model.PackageState
import tv.cleanyour.core.planner.PlanRequest
import tv.cleanyour.core.spec.LauncherConfigDoc
import tv.cleanyour.testing.fixtures.Recording
import tv.cleanyour.testing.fixtures.Samples
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

// RFC-THEME-1: the look through the launcher's ThemeProvider, run by the engine against a scripted TV.
class ThemeProviderRunTest {
    private val cfg = "{\"columns\":5}".encodeToByteArray()
    private val cfgSha = Harness.sha("{\"columns\":5}")
    private val wall = "jpeg bytes".encodeToByteArray()
    private val wallSha = Harness.sha("jpeg bytes")

    private fun harness(scope: kotlinx.coroutines.test.TestScope, sdk: Int): Harness {
        val tv = ScriptedTv(Samples.snapshot(facts = Samples.miTvFacts().copy(sdk = sdk), packages = Samples.miTvPackages(emptySet())))
        val h = Harness(scope, tv)
        h.tv.blobs[cfgSha] = cfg
        h.tv.blobs[wallSha] = wall
        h.tv.packages[h.launcher.pkg] = PackageState(EnabledState.ENABLED, false, 9, h.signer, false, false, null, true)
        return h
    }

    private fun Harness.theme(wallpaper: Boolean = true) = plan(PlanRequest.ApplyTheme(LauncherConfigDoc(), BlobRef(cfgSha, cfg.size.toLong(), "cfg"),
        if (wallpaper) BlobRef(wallSha, wall.size.toLong(), "wall") else null))

    // Issues #1279, #1649: an Android 11 Xiaomi whose shell may not write Android/data. The sync push the app used
    // so far fails there; through the provider the look arrives and is the launcher's own.
    @Test fun anAndroid11TvThatLocksTheShellOutGetsTheLook() = runTest(timeout = TEST_TIMEOUT) {
        val h = harness(this, 30)
        h.tv.shellLockedOut = true
        val plan = h.theme()
        assertEquals(listOf(FileRoute.THEME_PROVIDER, FileRoute.THEME_PROVIDER), plan.steps.mapNotNull { (it.op as? Op.PushAsset)?.route })
        val engine = h.engine()
        engine.start(plan, h.tv, plan.hash, h.pass())
        val end = assertIs<RunState.Finished>(h.settle(engine))
        assertEquals(RunStatus.COMPLETED, end.status, end.skipped.toString())
        assertTrue(h.tv.files[FileOverlayTransport.CONFIG.absolute]!!.contentEquals(cfg))
        assertTrue(h.tv.files[FileOverlayTransport.WALLPAPER.absolute]!!.contentEquals(wall))
        assertTrue(h.tv.shellOwned.isEmpty(), "both files are the launcher's")
        assertTrue(h.tv.files.keys.none { it.contains("/theme-") }, "the staged copies are gone")
        // The push to the staging file runs before the write that reads it, and the check reads through the provider.
        val log = h.tv.execLog.map { Recording.normalize(it) }
        val push = log.indexOf("push /data/local/tmp/cleanyourtv/theme-config.json")
        val write = log.indexOfFirst { it.contains("content write --uri content://com.gathin.tvhome.theme/config.json") }
        val read = log.indexOfFirst { it.startsWith("content read --uri content://com.gathin.tvhome.theme/config.json") }
        assertTrue(push in 0 until write && write < read, log.toString())
        assertTrue(log.none { it.startsWith("sha256sum /sdcard/") }, "never checked as the shell")
    }

    // The same TV with the sync push of 0.4.6: the step the reports showed ("PushAsset config.json: nonzero_exit(1)").
    @Test fun theSyncPushFailsOnThatTvAsTheReportsShowed() = runTest(timeout = TEST_TIMEOUT) {
        val h = harness(this, 30)
        h.tv.shellLockedOut = true
        val plan = h.opsPlan(Op.PushAsset(BlobRef(cfgSha, cfg.size.toLong(), "cfg"), FileOverlayTransport.CONFIG, cfgSha))
        val engine = h.engine()
        engine.start(plan, h.tv, plan.hash, h.pass())
        assertIs<RunState.Paused>(h.settle(engine))
    }

    // Issue #1290: on Android 14 the shell may write there, but the launcher cannot open the shell's file, so an
    // earlier push was ignored. The provider's write removes the shell's copy first and writes the launcher's own.
    @Test fun anAndroid14TvGetsALookTheLauncherCanRead() = runTest(timeout = TEST_TIMEOUT) {
        val h = harness(this, 34)
        h.tv.launcherRefusesShellFiles = true
        h.tv.files[FileOverlayTransport.CONFIG.absolute] = "old".encodeToByteArray()
        h.tv.shellOwned += FileOverlayTransport.CONFIG.absolute
        val plan = h.theme(wallpaper = false)
        val engine = h.engine()
        engine.start(plan, h.tv, plan.hash, h.pass())
        assertEquals(RunStatus.COMPLETED, assertIs<RunState.Finished>(h.settle(engine)).status)
        assertTrue(h.tv.files[FileOverlayTransport.CONFIG.absolute]!!.contentEquals(cfg))
        assertTrue(FileOverlayTransport.CONFIG.absolute !in h.tv.shellOwned)
        // The old wallpaper is cleared by the launcher too.
        assertTrue(h.tv.execLog.map { Recording.normalize(it) }.any { it == "content delete --uri content://com.gathin.tvhome.theme/wallpaper.jpg" })
    }

    // Where the provider does not answer, the line copies the staged file in as before; the check still reads as the
    // launcher, so a look the launcher cannot see is a failed step with its reason, never a silent pass.
    @Test fun withoutTheProviderTheCopyRunsAndTheCheckTellsTheTruth() = runTest(timeout = TEST_TIMEOUT) {
        val h = harness(this, 33)
        h.tv.themeProvider = false
        val plan = h.theme(wallpaper = false)
        val engine = h.engine()
        engine.start(plan, h.tv, plan.hash, h.pass())
        assertIs<RunState.Paused>(h.settle(engine))
        assertTrue(h.tv.files[FileOverlayTransport.CONFIG.absolute]!!.contentEquals(cfg), "the fallback copied it")
        val failed = h.records(plan).filterIsInstance<JournalRecord.Failed>().single()
        assertTrue(failed.error.startsWith("verify: expected ${cfgSha.hex}"), failed.error)
    }
}
