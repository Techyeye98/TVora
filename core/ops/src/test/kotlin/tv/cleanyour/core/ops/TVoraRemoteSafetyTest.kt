package tv.cleanyour.core.ops

import tv.cleanyour.core.model.Op
import tv.cleanyour.core.model.PackageName
import kotlin.test.Test
import kotlin.test.assertFailsWith

class TVoraRemoteSafetyTest {
    @Test fun requiredRemoteServicesCannotBeDisabledByAnyList() {
        val renderer = ShellRenderer()
        for (name in listOf("com.smartdevice.global.service", "com.smartdevice.dailyshortcut")) {
            assertFailsWith<RenderRefused>(name) { renderer.render(Op.Disable(PackageName(name))) }
        }
    }
}
