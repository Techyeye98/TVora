package tv.cleanyour.feature.backup

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import tv.cleanyour.core.flows.RestorePhase
import tv.cleanyour.core.flows.RestorePointState
import tv.cleanyour.ui.design.theme.CytTheme

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h780dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RestoreStatusTest {
    @get:Rule val compose = createComposeRule()

    @Test fun incompleteRestoreShowsWarningInsteadOfSuccess() {
        compose.setContent {
            CytTheme(reducedMotion = true) {
                RestoreScreen(RestorePointState(tvName = "Test TV", phase = RestorePhase.Incomplete), {}, {})
            }
        }
        compose.onNodeWithText("Restore did not finish every step.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Recorded changes restored").assertDoesNotExist()
    }
}
