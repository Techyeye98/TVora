package tv.cleanyour.core.engine

import kotlinx.coroutines.test.runTest
import tv.cleanyour.core.model.AdbError
import tv.cleanyour.core.model.EnabledState
import tv.cleanyour.core.model.Op
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Issues #80 (Sony BRAVIA VH2, Android 12) and #364 / #367 (a TCL C06, Android 11), 0.3.3 and 0.3.4.
// Their logs: "run paused CONNECTION_LOST at step 0 (protocol)" 25 and 33 times, each within about
// three seconds of "run running", the connection never dropping (Carry on ran again at once, no
// reconnect). Step 0 was the streamed install of the TV curtain; on #367 a plan without the curtain
// paused the same way at step 8, the launcher's streamed install, and on #80 a plan with no install
// at all completed. The streamed install ends with a protocol error on those TVs; the engine took
// every Transport error for a lost connection, so the run paused as "the TV stopped answering" and
// Carry on met the same install again, for good. Now a protocol error of a streamed install, with
// the session still connected, is that step's failure and its policy (CONTINUE) applies.
class Issue036StreamInstallTest {
    private val refusedOpen = AdbError.Transport.Protocol("the TV refused to open a streamed install")

    @Test fun issue80And367AStreamedInstallTheTvEndsWithAProtocolErrorIsSkippedNotAPause() = runTest(timeout = TEST_TIMEOUT) {
        val h = Harness(this)
        h.tv.installStreamError = refusedOpen
        val engine = h.engine()
        val plan = h.setupPlan(takeover = false)
        val install = plan.steps.single { it.op is Op.InstallStream }.index
        engine.start(plan, h.tv, plan.hash, h.pass())
        val fin = assertIs<RunState.Finished>(h.settle(engine), "a finished run, never a CONNECTION_LOST pause")
        assertEquals(RunStatus.COMPLETED_WITH_SKIPS, fin.status)
        assertTrue(fin.skipped.any { it.index == install })
        assertTrue(h.records(plan).none { it is JournalRecord.Paused })
        val failed = h.records(plan).filterIsInstance<JournalRecord.Failed>().single { it.i == install }
        assertTrue(failed.error.startsWith(DefaultEngine.STREAM_FAILED), failed.error)
        // The clean-up went on: every switch-off of the plan is done.
        for (p in h.debloat) assertEquals(EnabledState.DISABLED_USER, h.tv.packages[p]?.enabled, p.value)
        assertNull(h.tv.installed[h.launcher.pkg])
    }

    // With the switch planned: the launcher never arrived, so the guarded section is never opened.
    // The TV's own home screen stays on and in charge; the run ends with the switch skipped.
    @Test fun issue367TheHomeScreenSwitchIsSkippedWhenItsLauncherCouldNotBeInstalled() = runTest(timeout = TEST_TIMEOUT) {
        val h = Harness(this)
        h.tv.installStreamError = refusedOpen
        val engine = h.engine()
        val plan = h.setupPlan(takeover = true)
        engine.start(plan, h.tv, plan.hash, h.pass())
        val fin = assertIs<RunState.Finished>(h.settle(engine))
        assertEquals(RunStatus.COMPLETED_WITH_SKIPS, fin.status)
        assertEquals(HomeOutcome.VERIFY_FAILED, fin.home)
        assertTrue(h.records(plan).none { it is JournalRecord.Section }, "the section was never armed")
        assertEquals(h.stock, h.tv.home)
        val stock = h.tv.packages[h.stock.pkg]?.enabled
        assertTrue(stock != EnabledState.DISABLED_USER && stock != EnabledState.DISABLED, "the TV's own home screen stays on: $stock")
    }

    // The stream finished and the TV installed the APK before the error: the hash check makes it done.
    @Test fun aStreamedInstallThatLandedBeforeTheErrorIsDone() = runTest(timeout = TEST_TIMEOUT) {
        val h = Harness(this)
        h.tv.installStreamError = refusedOpen
        h.tv.installStreamInstallsFirst = true
        val engine = h.engine()
        val plan = h.setupPlan(takeover = false)
        engine.start(plan, h.tv, plan.hash, h.pass())
        val fin = assertIs<RunState.Finished>(h.settle(engine))
        assertEquals(RunStatus.COMPLETED, fin.status)
        assertEquals(h.apkSha, h.tv.installed[h.launcher.pkg])
    }

    // A connection that really went away is still a lost connection: the run pauses, as before.
    @Test fun aLostConnectionDuringAStreamedInstallStillPauses() = runTest(timeout = TEST_TIMEOUT) {
        val h = Harness(this)
        h.tv.installStreamError = AdbError.Transport.Closed
        val engine = h.engine()
        val plan = h.setupPlan(takeover = false)
        engine.start(plan, h.tv, plan.hash, h.pass())
        val paused = assertIs<RunState.Paused>(h.settle(engine))
        assertEquals(PauseReason.CONNECTION_LOST, paused.reason)
    }
}
