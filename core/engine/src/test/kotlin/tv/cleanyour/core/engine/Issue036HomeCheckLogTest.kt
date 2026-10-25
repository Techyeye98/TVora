package tv.cleanyour.core.engine

import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import tv.cleanyour.core.model.Component
import tv.cleanyour.core.model.PackageName
import tv.cleanyour.testing.fixtures.Samples
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

// Issues #153 (TCL G07), #339 (TCL BeyondTV), #375 (Hisense hengshan) and #385 (Sony AE2): each log
// says "run finished COMPLETED_WITH_SKIPS, home WRONG_FOCUS" and nothing about what the TV had in
// front during the check, so none of the reports could tell the setup wizard from Settings, the
// chooser or FallbackHome. The engine now writes one line naming it: the TV's own package and
// activity, or "an app you installed" for anything that is not a system app of the TV.
class Issue036HomeCheckLogTest {
    @Test fun issue375TheHomeCheckSaysWhatWasInFrontWhenItRollsBack() = runTest(timeout = TEST_TIMEOUT) {
        val resolver = Component(PackageName("android"), "com.android.internal.app.ResolverActivity")
        for (focus in listOf(Samples.stockLauncher, resolver)) {
            val h = Harness(this)
            val lines = mutableListOf<String>()
            val engine = h.engine(log = { lines += it })
            val plan = h.setupPlan()
            engine.start(plan, h.tv, plan.hash, h.pass())
            advanceTimeBy(20_000); runCurrent()
            assertIs<RunState.AwaitingConfirm>(engine.runState(h.tv.tvId).value)
            h.tv.focus = focus
            advanceTimeBy(2_500); runCurrent(); h.settle(engine)
            assertEquals(HomeOutcome.WRONG_FOCUS, assertIs<RunState.Finished>(engine.runState(h.tv.tvId).value).home)
            assertEquals(listOf("engine: the home check saw ${focus.flat} in front, so the old home screen goes back"), lines, focus.flat)
        }
    }

    // A check that holds writes nothing.
    @Test fun aConfirmedSwitchWritesNoFocusLine() = runTest(timeout = TEST_TIMEOUT) {
        val h = Harness(this)
        val lines = mutableListOf<String>()
        val engine = h.engine(log = { lines += it })
        val plan = h.setupPlan()
        engine.start(plan, h.tv, plan.hash, h.pass())
        advanceTimeBy(20_000); runCurrent()
        assertIs<RunState.AwaitingConfirm>(engine.runState(h.tv.tvId).value)
        engine.confirmHome(h.tv.tvId)
        h.settle(engine)
        assertNull(assertIs<RunState.Finished>(engine.runState(h.tv.tvId).value).home, "no home screen outcome to report")
        assertEquals(emptyList(), lines)
    }
}
