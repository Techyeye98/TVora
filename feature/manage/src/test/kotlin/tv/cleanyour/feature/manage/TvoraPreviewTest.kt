package tv.cleanyour.feature.manage

import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import tv.cleanyour.core.flows.*
import tv.cleanyour.core.model.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h2400dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TvoraPreviewTest {
    @get:Rule val compose = createComposeRule()
    @Test fun dashboard() {
        val tv = TvCard(TvId("1234567890abcdef"), "Living room TV", null, true,
            1792843200000L, Level.SIMPLE, ChipState.CONNECTED, null, 7,
            listOf("Netflix", "Prime Video", "SmartTube"), 1792843200000L, DebugState.ON)
        compose.shoot(Shot("tvora_dashboard") { ManageScreen(ManageState(cards = listOf(tv), loading = false), {}) }, Variant.DARK)
    }
    @Test fun dashboardLargeText() {
        val tv = TvCard(TvId("1234567890abcdef"), "Living room TV", null, true,
            1792843200000L, Level.SIMPLE, ChipState.CONNECTED, null, 7, emptyList(), null, DebugState.ON)
        compose.shoot(Shot("tvora_dashboard") { ManageScreen(ManageState(cards = listOf(tv), loading = false), {}) }, Variant.FONT200)
    }
}
