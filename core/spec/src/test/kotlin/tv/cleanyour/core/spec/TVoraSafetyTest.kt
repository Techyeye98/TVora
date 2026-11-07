package tv.cleanyour.core.spec

import kotlin.test.Test
import kotlin.test.assertTrue

class TVoraSafetyTest {
    @Test fun requiredRemoteUpdateAndBootPackagesStayProtected() {
        val expected = mapOf(
            "skyworth-swtv-22ae-fhd" to listOf("com.smartdevice.dailymanage", "com.smartdevice.dailyshortcut", "com.smartdevice.myapp"),
            "kalley-r10g" to listOf("com.google.android.apps.tv.launcherx", "com.google.android.tungsten.setupwraith"),
            "tcl-g03-google-tv" to listOf("com.tcl.xian.StartandroidService"),
            "tcl-ak30a5" to listOf("com.tcl.versionUpdateApp"),
        )
        for ((id, packages) in expected) {
            val kept = ProfileMeta.bundled.profiles.getValue(id).neverDisable
            assertTrue(packages.all { it in kept }, id)
        }
    }
}
