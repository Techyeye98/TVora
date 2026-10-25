package tv.cleanyour.core.flows

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import tv.cleanyour.core.engine.HomeOutcome
import tv.cleanyour.core.engine.JournalRecord
import tv.cleanyour.core.engine.RunState
import tv.cleanyour.core.engine.RunStatus
import tv.cleanyour.core.model.Op
import tv.cleanyour.core.model.Prior
import tv.cleanyour.testing.fixtures.Samples
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

// The diagnostics lines the 0.3.4 reports were missing.
// - #338 (Xiaomi MiTV-MOOQ0, croods, Android 10): three tries of the home screen switch, each "run
//   rolling back" then "home VERIFY_FAILED", and nothing to say which step of the switch failed.
// - #367 (TCL C06): "run cd2", the R8 name of the in doubt state.
@OptIn(ExperimentalCoroutinesApi::class)
class Issue036DiagnosticsTest {
    private suspend fun kotlinx.coroutines.test.TestScope.toFound(h: Harness, m: SetupMachine) {
        h.discovery.emit(tv.cleanyour.core.discovery.DiscoveryEvent.Found(miTvFound))
        settle()
        m.dispatch(SetupIntent.Start)
        settle()
        h.discovery.open5555[HOST] = true
        settle(2_100)
        settle(700)
    }

    @Test fun issue338TheLogNamesTheStepTheHomeScreenSwitchStoppedAt() = runTest(timeout = 30.seconds) {
        val h = Harness()
        h.sessions.allowPromptFor[HOST] = "AB:CD:7F:2A"
        h.setupPlan = h.plan(listOf(Op.Disable(adPkg)), takeover = true)
        val setHome = h.setupPlan.steps.single { it.op is Op.SetHome }
        h.engine.onStart = { p ->
            // What the engine journals when a step of the switch fails: its Begin and Failed, then step R.
            h.journal.append(JournalRecord.Begin(p.id, setHome.index, setHome.op, emptyList(), Prior(), Samples.NOW_MS))
            h.journal.append(JournalRecord.Failed(p.id, setHome.index, 255, "exit 255", "Error: unknown command 'set-home-activity'"))
            h.journal.append(JournalRecord.Rollback(p.id, p.sections.single().from))
            finish(p, RunStatus.COMPLETED_WITH_SKIPS, HomeOutcome.VERIFY_FAILED)
        }
        val m = h.setup(backgroundScope)
        toFound(h, m)
        m.dispatch(SetupIntent.MakeItClean); settle()
        val log = h.logLines.joinToString("\n")
        assertTrue(h.logLines.any { it == "setup: home switch stopped at SetHome ${Launcher.home.flat}: exit 255: Error: unknown command 'set-home-activity'" }, log)
    }

    // A failure outside the switch (a switch-off skipped earlier) is never named as the switch's.
    @Test fun aSkippedSwitchOffIsNotTheHomeScreenSwitchsFailure() = runTest {
        val h = Harness()
        val plan = h.plan(listOf(Op.Disable(adPkg)), takeover = true)
        h.journal.append(JournalRecord.Begin(plan.id, 0, plan.steps[0].op, emptyList(), Prior(), Samples.NOW_MS))
        h.journal.append(JournalRecord.Failed(plan.id, 0, 1, "exit 1", "refused"))
        assertNull(h.ledger.homeSwitchFailure(plan))
        val verify = plan.steps.single { it.op is Op.VerifyHome }
        h.journal.append(JournalRecord.Begin(plan.id, verify.index, verify.op, emptyList(), null, Samples.NOW_MS))
        h.journal.append(JournalRecord.Failed(plan.id, verify.index, null, "verify: expected the new home screen", ""))
        assertEquals(verify.op, h.ledger.homeSwitchFailure(plan)?.op)
    }

    @Test fun issue367TheInDoubtStateIsLoggedInWords() = runTest(timeout = 30.seconds) {
        val h = Harness()
        h.sessions.allowPromptFor[HOST] = "AB:CD:7F:2A"
        h.engine.onStart = { p -> emit(p.tvId, RunState.InDoubt(p, 0)) }
        val m = h.setup(backgroundScope)
        toFound(h, m)
        m.dispatch(SetupIntent.MakeItClean); settle()
        assertTrue(h.logLines.any { it == "setup: run in doubt" }, h.logLines.joinToString("\n"))
    }
}
