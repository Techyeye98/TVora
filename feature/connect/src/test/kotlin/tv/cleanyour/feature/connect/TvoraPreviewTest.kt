package tv.cleanyour.feature.connect

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import tv.cleanyour.core.flows.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h780dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TvoraPreviewTest {
    @get:Rule val compose = createComposeRule()
    @Test fun welcome() {
        compose.setContent { Rendered(Shot("tvora_welcome") {
            WelcomeScreen(SetupState.Welcome(emptyList(), true, true), SetupExtras(), NoSetupActions, {})
        }, Variant.DARK) }
        compose.waitForIdle()
        compose.onNodeWithTag(SHOT_TAG).captureRoboImage("src/test/screenshots/tvora_welcome_dark.png")
    }
}
