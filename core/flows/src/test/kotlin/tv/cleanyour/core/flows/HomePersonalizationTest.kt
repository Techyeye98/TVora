package tv.cleanyour.core.flows

import kotlin.test.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Test
import tv.cleanyour.core.model.*
import tv.cleanyour.core.spec.*
import tv.cleanyour.testing.fixtures.Samples
import tv.cleanyour.core.planner.PlanRequest

class HomePersonalizationTest {
    private val netflix = PackageName("com.netflix.ninja")
    private val music = PackageName("com.google.android.youtube.tvmusic")

    @Test fun machineKeepsHiddenAppsInSavedOrderAndRestoresThem() = runTest {
        val h = Harness()
        val tv = h.knownTv().copy(launcher = LauncherState(9, Samples.zeroHash, null, null))
        h.tvs.upsert(tv)
        val machine = HomeScreenMachine(h.manageDeps(encoder = ConfigEncoder.Codec), backgroundScope, tv.id)
        settle()
        machine.dispatch(HomeScreenIntent.SetDock(listOf(netflix, music))); settle()
        machine.dispatch(HomeScreenIntent.SetPersonal(HomePersonalization(hidden = listOf(music)))); settle()
        assertEquals(listOf(netflix, music), HomePrefs(h.documents).load(tv.id)?.dock)
        assertEquals(listOf(netflix), h.planner.requests.filterIsInstance<PlanRequest.ApplyTheme>().last().doc.dockApps)
        machine.dispatch(HomeScreenIntent.SetPersonal(HomePersonalization(hidden = emptyList()))); settle()
        assertEquals(listOf(netflix, music), h.planner.requests.filterIsInstance<PlanRequest.ApplyTheme>().last().doc.dockApps)
    }

    @Test fun preferencesRoundTripWithoutChangingDockOrTheme() = runTest {
        val h = Harness()
        val id = h.knownTv().id
        val home = HomePrefs(h.documents)
        val original = HomeChoice(listOf(netflix, music), "aurora", false)
        home.save(id, original)
        val prefs = PersonalHomePrefs(h.documents)
        val photo = BlobRef(Sha256("a".repeat(64)), 123, "/private/photo.jpg")
        val value = HomePersonalization(listOf(music), false, true, true, true, photo, 45, arrange = false)
        prefs.save(id, value)
        assertEquals(value, prefs.load(id))
        assertEquals(original, home.load(id))
    }

    @Test fun remoteArrangeIsAvailableWithoutChangingTheShelfOrAppOrder() = runTest {
        val h = Harness()
        val dock = List(12) { PackageName("com.example.app$it") }
        val theme = Theme("default", "Default", "", LauncherConfigDoc(rowsBelowDock = 0, dockTitle = true), null)
        for (apps in listOf(dock.take(4), dock.take(8), dock, dock.reversed())) {
            val look = Look.encode(ConfigEncoder.Codec, h.blobs, theme, apps, false)
            assertEquals(apps, look.doc.dockApps)
            assertEquals(0, look.doc.rowsBelowDock)
            assertEquals(true, look.doc.dockTitle)
            assertEquals(true, look.doc.reorderButton)
        }
        val off = Look.encode(ConfigEncoder.Codec, h.blobs, theme, dock, false, HomePersonalization(arrange = false))
        assertEquals(false, off.doc.reorderButton)
        assertEquals(dock, off.doc.dockApps)
        assertEquals(0, off.doc.rowsBelowDock)
    }

    @Test fun hidingOnlyChangesLauncherConfigAndShowingRestoresSavedOrder() = runTest {
        val h = Harness()
        val dock = listOf(netflix, music)
        val theme = Theme("default", "Default", "", LauncherConfigDoc(unknown = mapOf("ambient_glow" to JsonPrimitive(true))), null)
        val personal = HomePersonalization(hidden = listOf(music), bluetooth = true)
        val hidden = Look.encode(ConfigEncoder.Codec, h.blobs, theme, dock, false, personal)
        assertEquals(listOf(netflix), hidden.doc.dockApps)
        assertEquals(listOf(music), hidden.doc.hiddenApps)
        assertEquals(JsonPrimitive(true), hidden.doc.unknown["show_bluetooth_pill"])
        assertEquals(JsonPrimitive(true), hidden.doc.unknown["ambient_glow"])
        val shown = Look.encode(ConfigEncoder.Codec, h.blobs, theme, dock, false, personal.copy(hidden = emptyList()))
        assertEquals(dock, shown.doc.dockApps)
    }

    @Test fun photoOverridesThemeAndCanBeTurnedOffWithoutLosingThePhoto() = runTest {
        val h = Harness()
        val image = BlobRef(Sha256("b".repeat(64)), 20, "/private/photo.jpg")
        val own = HomePersonalization(photo = image, wallpaperEnabled = true, dim = 40)
        val custom = Look.encode(ConfigEncoder.Codec, h.blobs, null, listOf(netflix), false, own)
        assertEquals(image, custom.wallpaper)
        assertEquals(true, custom.doc.wallpaper)
        assertEquals(40, custom.doc.wallpaperDim)
        val off = Look.encode(ConfigEncoder.Codec, h.blobs, null, listOf(netflix), false, own.copy(wallpaperEnabled = false))
        assertNull(off.wallpaper)
        assertEquals(false, off.doc.wallpaper)
        assertFailsWith<IllegalArgumentException> { own.copy(dim = 101) }
    }
}
