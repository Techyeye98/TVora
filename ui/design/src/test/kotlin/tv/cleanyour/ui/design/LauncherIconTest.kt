package tv.cleanyour.ui.design

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.res.ResourcesCompat
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LauncherIconTest {
    @Test fun adaptiveIconAndThemedIconRender() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val icon = ResourcesCompat.getDrawable(context.resources, R.mipmap.cyt_ic_launcher, null)!!
        assertTrue(icon is android.graphics.drawable.AdaptiveIconDrawable)
        val adaptive = icon as android.graphics.drawable.AdaptiveIconDrawable
        assertTrue(adaptive.monochrome != null)
        // Render the Android adaptive mask, not a separate approximation of the artwork.
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        icon.setBounds(0, 0, 512, 512)
        icon.draw(Canvas(bitmap))
        bitmap.captureRoboImage("src/test/screenshots/tvora_icon.png")
        for ((name, drawable) in listOf("foreground" to adaptive.foreground, "monochrome" to adaptive.monochrome!!)) {
            val image = Bitmap.createBitmap(432, 432, Bitmap.Config.ARGB_8888)
            drawable.setBounds(0, 0, 432, 432)
            drawable.draw(Canvas(image))
            image.captureRoboImage("src/test/screenshots/tvora_icon_$name.png")
        }
        val small = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
        icon.setBounds(0, 0, 48, 48)
        icon.draw(Canvas(small))
        small.captureRoboImage("src/test/screenshots/tvora_icon_48.png")
    }
}
