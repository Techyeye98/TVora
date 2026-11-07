package tv.cleanyour.core.flows

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import tv.cleanyour.core.engine.RunStatus
import tv.cleanyour.core.model.*
import tv.cleanyour.core.planner.PlanRequest
import tv.cleanyour.core.spec.*
import tv.cleanyour.core.store.SnapshotCodec
import tv.cleanyour.testing.fixtures.Samples
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class HomeSettingsPreservationTest {
    private val netflix = PackageName("com.netflix.ninja")
    private val music = PackageName("com.google.android.youtube.tvmusic")
    private val photo = BlobRef(Samples.zeroHash, 20, "private:photo")
    private fun current() = LauncherConfigDoc(
        dockApps = listOf(music, netflix), hiddenApps = listOf(PackageName("com.example.hidden")),
        columns = 6, rowsBelowDock = 2, reorderButton = false, moduleCards = false,
        animations = false, reducedMotion = true, greetingName = "Namya", wallpaper = true, wallpaperDim = 65,
        appLabels = listOf("com.netflix.ninja=Films"), settingsKeyKeycodes = listOf(82),
        unknown = mapOf("show_bluetooth_pill" to JsonPrimitive(true), "show_inputs_pill" to JsonPrimitive(false),
            "show_network_pill" to JsonPrimitive(true), "future_widget" to buildJsonObject { put("enabled", true) }))
    private val theme = Theme("aurora", "Aurora", "", LauncherConfigDoc(backgroundColor = "#070A14", wallpaper = true), null)

    private fun assertOtherSettings(before: LauncherConfigDoc, after: LauncherConfigDoc) {
        assertEquals(before.unknown, after.unknown)
        assertEquals(before.reorderButton, after.reorderButton)
        assertEquals(before.animations, after.animations)
        assertEquals(before.reducedMotion, after.reducedMotion)
        assertEquals(before.greetingName, after.greetingName)
        assertEquals(before.appLabels, after.appLabels)
        assertEquals(before.settingsKeyKeycodes, after.settingsKeyKeycodes)
        assertEquals(before.moduleCards, after.moduleCards)
        assertEquals(before.hiddenApps, after.hiddenApps)
    }

    @Test fun changingTheLookPreservesButtonsOrderMotionAndUnknownSettings() = runTest {
        val h = Harness()
        val base = current()
        val changed = Look.encode(ConfigEncoder.Codec, h.blobs, theme, listOf(netflix, music), false,
            current = base, edit = HomeEdit.LOOK)
        assertOtherSettings(base, changed.doc)
        assertEquals(base.dockApps, changed.doc.dockApps)
        assertEquals("#070A14", changed.doc.backgroundColor)
    }

    @Test fun selectingAPhotoChangesOnlyWallpaperSettings() = runTest {
        val h = Harness()
        val base = current()
        val before = HomeSettingsPatch.personal(base, HomePersonalization())
        val changed = Look.encode(ConfigEncoder.Codec, h.blobs, theme, listOf(netflix), false,
            before.copy(photo = photo, wallpaperEnabled = true, dim = 40), base, HomeEdit.PERSONAL, before)
        assertOtherSettings(base, changed.doc)
        assertEquals(base.dockApps, changed.doc.dockApps)
        assertEquals(base.columns, changed.doc.columns)
        assertEquals(base.rowsBelowDock, changed.doc.rowsBelowDock)
        assertEquals(40, changed.doc.wallpaperDim)
        assertEquals(photo, changed.wallpaper)
        assertFalse(changed.preserveWallpaper)
    }

    @Test fun dockAndButtonChangesPreserveTheExistingWallpaperFile() = runTest {
        val h = Harness()
        val base = current()
        val before = HomeSettingsPatch.personal(base, HomePersonalization())
        val dock = Look.encode(ConfigEncoder.Codec, h.blobs, null, listOf(netflix), false, before, base, HomeEdit.DOCK)
        assertEquals(listOf(netflix), dock.doc.dockApps)
        assertEquals(true, dock.doc.wallpaper)
        assertEquals(65, dock.doc.wallpaperDim)
        assertTrue(dock.preserveWallpaper)
        val button = Look.encode(ConfigEncoder.Codec, h.blobs, null, listOf(netflix), false,
            before.copy(bluetooth = false), base, HomeEdit.PERSONAL, before)
        assertEquals(JsonPrimitive(false), button.doc.unknown["show_bluetooth_pill"])
        assertEquals(false, button.doc.reorderButton)
        assertEquals(base.dockApps, button.doc.dockApps)
        assertTrue(button.preserveWallpaper)
    }

    private suspend fun ready(h: Harness, doc: LauncherConfigDoc): Tv {
        val tv = h.knownTv().copy(launcher = LauncherState(9, Samples.zeroHash, null, null))
        h.tvs.upsert(tv)
        h.clock.advance(1)
        val snap = Samples.miTvSnapshot().copy(takenAtMs = h.clock.nowMs(), launcherOverlay = LauncherConfigCodec.encode(doc))
        h.snapshots.save(snap.copy(contentHash = SnapshotCodec.hash(snap)))
        HomePrefs(h.documents).save(tv.id, HomeChoice(listOf(netflix, music), "aurora", false))
        return tv
    }

    @Test fun successiveEditsAndReopeningUseTheLastSuccessfulDocument() = runTest(timeout = 30.seconds) {
        val h = Harness()
        val tv = ready(h, current())
        val deps = h.manageDeps(encoder = ConfigEncoder.Codec)
        val first = HomeScreenMachine(deps, backgroundScope, tv.id)
        settle()
        first.dispatch(HomeScreenIntent.SetPersonal(first.state.value.personal.copy(bluetooth = false))); settle()
        val reopened = HomeScreenMachine(deps, backgroundScope, tv.id)
        settle()
        assertEquals(false, reopened.state.value.personal.bluetooth)
        assertEquals(false, reopened.state.value.personal.arrange)
        reopened.dispatch(HomeScreenIntent.SetPersonal(reopened.state.value.personal.copy(photo = photo, wallpaperEnabled = true))); settle()
        reopened.dispatch(HomeScreenIntent.SetTheme("aurora")); settle()
        val sent = h.planner.requests.filterIsInstance<PlanRequest.ApplyTheme>().last().doc
        assertEquals(JsonPrimitive(false), sent.unknown["show_bluetooth_pill"])
        assertEquals(false, sent.reorderButton)
        assertEquals(current().dockApps, sent.dockApps)
        assertEquals(false, sent.animations)
        assertEquals(current().unknown["future_widget"], sent.unknown["future_widget"])
    }

    @Test fun settingsChangedOnTheTvWinOverAnOlderPhoneSnapshot() = runTest(timeout = 30.seconds) {
        val h = Harness()
        val tv = ready(h, current())
        val m = HomeScreenMachine(h.manageDeps(encoder = ConfigEncoder.Codec), backgroundScope, tv.id)
        settle()
        val live = current().copy(greetingName = "TV edit", reorderButton = true,
            unknown = current().unknown + ("show_bluetooth_pill" to JsonPrimitive(false)))
        h.configTransport.currentConfig = LauncherConfigCodec.encode(live)
        m.dispatch(HomeScreenIntent.SetPersonal(m.state.value.personal.copy(photo = photo))); settle()
        val sent = h.planner.requests.filterIsInstance<PlanRequest.ApplyTheme>().last().doc
        assertEquals("TV edit", sent.greetingName)
        assertEquals(true, sent.reorderButton)
        assertEquals(JsonPrimitive(false), sent.unknown["show_bluetooth_pill"])
    }

    @Test fun anUnreadableDocumentNeverGetsOverwrittenWithDefaults() = runTest(timeout = 30.seconds) {
        val h = Harness()
        val tv = ready(h, current())
        val m = HomeScreenMachine(h.manageDeps(encoder = ConfigEncoder.Codec), backgroundScope, tv.id)
        settle()
        h.configTransport.currentConfig = "not json".encodeToByteArray()
        m.dispatch(HomeScreenIntent.SetTheme("aurora")); settle()
        assertTrue(h.planner.requests.none { it is PlanRequest.ApplyTheme })
        assertNull(m.state.value.busy)
    }

    @Test fun incompleteWritesDoNotAdvanceTheSavedDocument() = runTest(timeout = 30.seconds) {
        val h = Harness()
        val tv = ready(h, current())
        val deps = h.manageDeps(encoder = ConfigEncoder.Codec)
        h.engine.onStart = { p -> finish(p, RunStatus.COMPLETED_WITH_SKIPS) }
        val m = HomeScreenMachine(deps, backgroundScope, tv.id)
        settle()
        m.dispatch(HomeScreenIntent.SetPersonal(m.state.value.personal.copy(bluetooth = false))); settle()
        assertEquals(current(), deps.homePrefs.settings.current(tv.id, h.snapshots.latest(tv.id)))
    }

    @Test fun documentsAreIsolatedByTvAndCanBeClearedAfterRestore() = runTest {
        val h = Harness()
        val settings = HomePrefs(h.documents).settings
        val a = Samples.miTvId()
        val b = TvId("0123456789abcdef")
        settings.remember(a, current(), Samples.NOW_MS)
        settings.remember(b, current().copy(reorderButton = true), Samples.NOW_MS)
        assertEquals(false, settings.current(a, null)?.reorderButton)
        assertEquals(true, settings.current(b, null)?.reorderButton)
        settings.clear(a)
        assertNull(settings.current(a, null))
        assertNotNull(settings.current(b, null))
    }
    @Test fun successfulRestoreDiscardsTheLaterCachedDocument() = runTest(timeout = 30.seconds) {
        val h = Harness()
        val tv = ready(h, current())
        val deps = h.manageDeps(encoder = ConfigEncoder.Codec)
        deps.homePrefs.settings.remember(tv.id, current().copy(reorderButton = true), Samples.NOW_MS)
        val m = RestorePointMachine(deps, backgroundScope, tv.id)
        settle()
        m.dispatch(RestorePointIntent.PreviewUndo); settle()
        m.dispatch(RestorePointIntent.Confirm); settle()
        m.dispatch(RestorePointIntent.PutEverythingBack); settle()
        assertIs<RestorePhase.Finished>(m.state.value.phase)
        assertNull(deps.homePrefs.settings.current(tv.id, null))
    }

    @Test fun aClockChangeDoesNotMakeANewWriteLoseToTheSameOldSnapshot() = runTest {
        val h = Harness()
        val settings = HomePrefs(h.documents).settings
        val observed = Samples.snapshot(takenAtMs = Samples.NOW_MS + 1000)
        settings.remember(observed.tvId, current(), Samples.NOW_MS, observed)
        assertEquals(current(), settings.current(observed.tvId, observed))
        val fresh = observed.copy(launcherOverlay = LauncherConfigCodec.encode(current().copy(reorderButton = true)))
        val checked = fresh.copy(contentHash = SnapshotCodec.hash(fresh))
        assertEquals(true, settings.current(observed.tvId, checked)?.reorderButton)
    }

    @Test fun theChosenLookStillControlsItsOwnWallpaperWithoutResettingButtons() = runTest {
        val h = Harness()
        val base = current()
        val selected = Theme("default", "Default", "", LauncherConfigDoc(wallpaper = false), null)
        val shown = HomeSettingsPatch.personal(base, HomePersonalization())
        val look = Look.encode(ConfigEncoder.Codec, h.blobs, selected, listOf(netflix), false, shown, base, HomeEdit.LOOK)
        assertEquals(false, look.doc.wallpaper)
        assertEquals(base.unknown, look.doc.unknown)
        assertEquals(false, look.doc.reorderButton)
    }

    @Test fun aDisabledPersonalPhotoSurvivesLookChangesAndCanBeEnabledAgain() = runTest {
        val h = Harness()
        val off = HomePersonalization(photo = photo, wallpaperEnabled = false)
        val disabledDoc = current().copy(wallpaper = false)
        val look = Look.encode(ConfigEncoder.Codec, h.blobs, theme, listOf(netflix), false, off, disabledDoc, HomeEdit.LOOK)
        assertTrue(look.preserveWallpaper)
        val enabled = Look.encode(ConfigEncoder.Codec, h.blobs, theme, listOf(netflix), false,
            off.copy(wallpaperEnabled = true), disabledDoc, HomeEdit.PERSONAL, off)
        assertEquals(true, enabled.doc.wallpaper)
        assertEquals(photo, enabled.wallpaper)
        assertFalse(enabled.preserveWallpaper)
    }

    @Test fun aKnownExistingConfigCannotBeReplacedWhenEveryReadIsUnavailable() = runTest(timeout = 30.seconds) {
        val h = Harness()
        val tv = h.knownTv().copy(launcher = LauncherState(9, Samples.zeroHash, Samples.zeroHash, Samples.zeroHash))
        h.tvs.upsert(tv)
        val m = HomeScreenMachine(h.manageDeps(encoder = ConfigEncoder.Codec), backgroundScope, tv.id)
        settle()
        m.dispatch(HomeScreenIntent.SetTheme("aurora")); settle()
        assertTrue(h.planner.requests.none { it is PlanRequest.ApplyTheme })
        assertNull(m.state.value.busy)
    }

}
