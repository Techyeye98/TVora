package tv.cleanyour.core.planner

import tv.cleanyour.core.model.BlobRef
import tv.cleanyour.core.model.FileRoute
import tv.cleanyour.core.model.Op
import tv.cleanyour.core.model.Prior
import tv.cleanyour.core.model.RemoteDir
import tv.cleanyour.core.model.Sha256
import tv.cleanyour.core.model.Snapshot
import tv.cleanyour.core.ops.FailurePolicy
import tv.cleanyour.core.planner.PlannerFixtures.artifact
import tv.cleanyour.core.planner.PlannerFixtures.change
import tv.cleanyour.core.planner.PlannerFixtures.installedLauncher
import tv.cleanyour.core.planner.PlannerFixtures.plan
import tv.cleanyour.core.planner.PlannerFixtures.setup
import tv.cleanyour.core.planner.PlannerFixtures.snapshot
import tv.cleanyour.core.spec.LauncherConfigDoc
import tv.cleanyour.core.store.SnapshotCodec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// RFC-THEME-1 (issues #1279, #1649, #1094, #1290): the look goes through the launcher's ThemeProvider on Android 10
// and later when the launcher on the TV, or the one the plan installs, has it (versionCode 6 and up, or not read);
// else the sync push of every earlier release.
class ThemeRouteTest {
    private val config = BlobRef(Sha256("c".repeat(64)), 100, "blobs/config")
    private val wallpaper = BlobRef(Sha256("d".repeat(64)), 2_000, "blobs/wallpaper")
    private fun planner() = PlannerFixtures.planner(OverlayTransport())
    private fun sdk(s: Snapshot, sdk: Int): Snapshot = s.copy(facts = s.facts.copy(sdk = sdk)).let { it.copy(contentHash = SnapshotCodec.hash(it)) }
    private fun withLauncher(versionCode: Long, sdk: Int) = sdk(snapshot(extra = mapOf(installedLauncher(versionCode = versionCode))), sdk)
    private fun routes(p: Plan) = p.steps.filter { it.group == DefaultPlanner.GROUP_THEME }.mapNotNull { s ->
        when (val op = s.op) { is Op.PushAsset -> op.route; is Op.RemoveFile -> op.route; else -> null } }

    @Test fun setupSendsTheLookThroughTheProviderOfTheLauncherItInstalls() {
        val req = setup(artifact = artifact().copy(versionCode = 9)).copy(config = config, wallpaper = wallpaper)
        val p = plan(req, snapshot = sdk(snapshot(), 30), planner = planner())
        assertEquals(listOf(FileRoute.THEME_PROVIDER, FileRoute.THEME_PROVIDER), routes(p))
        val look = p.steps.filter { it.group == DefaultPlanner.GROUP_THEME }
        // The staging folder is made, not the launcher's (an Android 11 Xiaomi refuses that mkdir); the policy stays CONTINUE.
        assertEquals(Op.Mkdir(RemoteDir.TMP), look[1].op)
        assertTrue(look.all { it.onFailure == FailurePolicy.CONTINUE })
        assertEquals(listOf(Prior(present = false), Prior(present = false)), look.filter { it.op is Op.PushAsset }.map { it.prior })
    }

    @Test fun androidNineAndAnOldLauncherKeepTheSyncPush() {
        val fresh = setup(artifact = artifact().copy(versionCode = 9)).copy(config = config, wallpaper = null)
        assertEquals(listOf(FileRoute.SYNC, FileRoute.SYNC), routes(plan(fresh, snapshot = sdk(snapshot(), 28), planner = planner())))
        // The launcher this plan installs predates the provider (the fixture's versionCode 1).
        assertEquals(listOf(FileRoute.SYNC, FileRoute.SYNC), routes(plan(setup().copy(config = config), snapshot = sdk(snapshot(), 30), planner = planner())))
    }

    @Test fun applyThemeFollowsTheLauncherOnTheTv() {
        fun route(versionCode: Long, sdk: Int) = routes(plan(PlanRequest.ApplyTheme(LauncherConfigDoc(), config, wallpaper), snapshot = withLauncher(versionCode, sdk), planner = planner()))
        assertEquals(listOf(FileRoute.THEME_PROVIDER, FileRoute.THEME_PROVIDER), route(9, 30))
        assertEquals(listOf(FileRoute.THEME_PROVIDER, FileRoute.THEME_PROVIDER), route(6, 34))
        assertEquals(listOf(FileRoute.THEME_PROVIDER, FileRoute.THEME_PROVIDER), route(0, 31))   // not read: the bundled launcher
        assertEquals(listOf(FileRoute.SYNC, FileRoute.SYNC), route(5, 30))   // launcher 2.4 has no provider
        assertEquals(listOf(FileRoute.SYNC, FileRoute.SYNC), route(9, 28))   // Android 9 has no content write
        // ApplyTheme still stops on a refused send (Manage shows it), through either route.
        val p = plan(PlanRequest.ApplyTheme(LauncherConfigDoc(), config, null), snapshot = withLauncher(9, 30), planner = planner())
        assertTrue(p.steps.filter { it.op is Op.PushAsset }.all { it.onFailure == FailurePolicy.STOP_AND_ROLLBACK })
    }

    @Test fun undoClearsALookTheProviderWroteThroughTheProvider() {
        val s = withLauncher(9, 30)
        val path = OverlayTransport.CONFIG
        for (route in FileRoute.entries) {
            val changes = listOf(change(1, Op.PushAsset(config, path, config.sha256, route), Prior(present = false)))
            val p = plan(PlanRequest.UndoEverything(changes, removeLauncher = false), s, changes)
            assertEquals(listOf(Op.RemoveFile(path, route)), p.steps.map { it.op }.filterIsInstance<Op.RemoveFile>(), route.name)
        }
    }
}
