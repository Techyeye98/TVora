package tv.cleanyour.feature.manage

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import tv.cleanyour.core.flows.AppSettingsIntent
import tv.cleanyour.core.flows.AppSettingsState
import tv.cleanyour.core.flows.ManageIntent
import tv.cleanyour.core.flows.ManageState
import tv.cleanyour.core.flows.UpdateCheck
import tv.cleanyour.ui.design.theme.CytTheme
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h960dp-xhdpi")
class TVoraSupportTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun supportAndContactUseTheExistingSettingsRows() {
        val hits = mutableListOf<AppSettingsIntent>()
        compose.setContent {
            CytTheme(reducedMotion = true) {
                AppSettingsScreen(AppSettingsState(version = "0.1.2", addOns = false), { hits += it }, {}, {})
            }
        }
        compose.onNodeWithText("Made by Namya Mehta").assertExists()
        compose.onNodeWithText("Star the GitHub repository").performClick()
        compose.onNodeWithText("Contact").performClick()
        assertEquals(listOf<AppSettingsIntent>(AppSettingsIntent.SupportRepository, AppSettingsIntent.ContactDeveloper), hits)
        compose.onRoot().captureRoboImage("build/outputs/tvora-settings.png")
    }

    @Test fun updateBannerOpensTheReleasePageAction() {
        val hits = mutableListOf<ManageIntent>()
        compose.setContent {
            CytTheme(reducedMotion = true) {
                ManageScreen(ManageState(addOns = false, update = UpdateCheck.Available("0.1.3")), { hits += it })
            }
        }
        compose.onNodeWithText("0.1.3 is available").assertExists()
        compose.onNodeWithText("Cards and modules").assertDoesNotExist()
        compose.onNodeWithText("Open the release page").performClick()
        assertEquals(listOf<ManageIntent>(ManageIntent.OpenRelease), hits)
        compose.onRoot().captureRoboImage("build/outputs/tvora-update.png")
    }
}
