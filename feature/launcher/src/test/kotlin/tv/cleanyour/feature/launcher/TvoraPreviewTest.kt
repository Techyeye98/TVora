package tv.cleanyour.feature.launcher

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import tv.cleanyour.core.flows.*
import tv.cleanyour.core.model.PackageName

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h2400dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TvoraPreviewTest {
    @get:Rule val compose = createComposeRule()
    @Test fun homeStudio() {
        val state = HomeScreenState(tvName = "Living room TV", dock = listOf(
            AppChoice(PackageName("com.netflix.ninja"), "Netflix"),
            AppChoice(PackageName("com.amazon.amazonvideo.livingroom"), "Prime Video")), themeId = "default")
        compose.shoot(Shot("tvora_home_studio") { HomeScreen(state, {}, {}, onChoosePhoto = {}) }, Variant.DARK)
    }
    @Test fun remoteArrangeToggleSavesItsChoice() {
        val intents = mutableListOf<HomeScreenIntent>()
        compose.setContent {
            tv.cleanyour.ui.design.theme.CytTheme(reducedMotion = true) {
                HomeScreen(HomeScreenState(), { intents.add(it) }, {})
            }
        }
        compose.onNodeWithText("Quick buttons").performClick()
        compose.onNodeWithText("Arrange apps on TV").performScrollTo().performClick()
        compose.onNodeWithText("Done").performScrollTo().performClick()
        assertEquals(false, (intents.single() as HomeScreenIntent.SetPersonal).value.arrange)
    }
}
