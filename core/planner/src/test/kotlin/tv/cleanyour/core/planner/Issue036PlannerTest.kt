package tv.cleanyour.core.planner

import tv.cleanyour.core.model.Level
import tv.cleanyour.core.model.Op
import tv.cleanyour.core.planner.PlannerFixtures.artifact
import tv.cleanyour.core.planner.PlannerFixtures.choices
import tv.cleanyour.core.spec.BundledFactsTable
import tv.cleanyour.core.spec.BundledProfileSource
import tv.cleanyour.core.spec.ProfileResolution
import tv.cleanyour.testing.fixtures.FakeConfigTransport
import tv.cleanyour.testing.fixtures.FakeIdGen
import tv.cleanyour.testing.fixtures.Issue036
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

// Issue #375 (0.3.4, a Hisense U75, hengshan, exact profile hisense-u75-hengshan with the wizard
// quirk, Wi-Fi saved by others). The home screen switch rolled back (WRONG_FOCUS), the stop card
// offered "Try with the wizard off", which put the setup wizard into the TV's saved choices, and
// from then on every plan was refused with the reason the log shows, so the person could neither
// set the TV up again nor reach Restore point from Setup. The planner's refusal is right and stays:
// the wizard goes off only inside the switch. This test pins both halves: the refusal the log
// shows, word for word, and that the switch itself already switches the wizard off, so a retry
// "with the wizard off" could never have changed anything. SetupMachine no longer offers it, and
// drops such a saved choice (SetupMachineTest, Issue036SetupTest).
class Issue036PlannerTest {
    private val source = BundledProfileSource()
    private val facts = BundledFactsTable()
    private fun planner() = DefaultPlanner(PlannerFixtures.clock, FakeIdGen(), tv.cleanyour.core.ops.ShellRenderer(), FakeConfigTransport(), "0.3.4")

    private fun setup(r: ProfileResolution, extraOff: Set<tv.cleanyour.core.model.PackageName>) =
        PlanRequest.Setup(r, choices(extraOff = extraOff, installLauncher = true, takeover = true), artifact(), Level.SIMPLE)

    @Test fun issue375TheReportResolvesToTheHengshanList() {
        val r = assertIs<ProfileResolution.Exact>(source.resolveNow(Issue036.hengshanFacts()))
        assertEquals("hisense-u75-hengshan", r.profile.id)
        assertEquals(Issue036.wizard, r.profile.setupWraith)
        assertTrue(tv.cleanyour.core.model.Quirk.HOME_TAKEOVER_SETUPWRAITH in r.profile.quirks)
    }

    // The refusal of the report's log, reproduced from the saved choice "Try with the wizard off" left.
    @Test fun issue375TheWizardAsASavedChoiceIsRefusedAsTheLogSays() {
        val snap = Issue036.hengshanSnapshot()
        val r = source.resolveNow(snap.facts)
        val refused = assertIs<PlanResult.Refused>(planner().plan(setup(r, setOf(Issue036.wizard)), snap, facts, emptyList())).reasons
        assertEquals(listOf(Issue036.REFUSED_LINE.removePrefix("setup: refused because ")), refused)
    }

    // Without that choice the same TV gets its plan, and the switch already holds the wizard's
    // switch-off inside the guarded section (the quirk, Wi-Fi saved by others).
    @Test fun issue375WithoutTheSavedChoiceThePlanIsMadeAndTheSwitchHasTheWizardOff() {
        val snap = Issue036.hengshanSnapshot()
        val r = source.resolveNow(snap.facts)
        val plan = assertIs<PlanResult.Ok>(planner().plan(setup(r, emptySet()), snap, facts, emptyList())).plan
        val section = plan.sections.single()
        val wizardOff = plan.steps.single { it.op == Op.Disable(Issue036.wizard) }.index
        assertTrue(wizardOff in section.from..section.to, "the wizard goes off inside the switch")
        assertTrue(plan.steps.none { it.op == Op.Disable(Issue036.wizard) && it.index !in section.from..section.to })
    }

    // Any other saved choice that is a home screen today is refused the same way; SetupMachine drops
    // those too, with the same rule (SwitchOffRule), so the two never disagree.
    @Test fun issue375AHomeScreenAsASavedChoiceIsRefusedByTheSameRule() {
        val snap = Issue036.hengshanSnapshot()
        val r = assertIs<ProfileResolution.Exact>(source.resolveNow(snap.facts))
        for (pkg in listOf(Issue036.wizard, Issue036.launcherx.pkg)) {
            val why = SwitchOffRule.reason(pkg, snap.packages[pkg], r.profile, snap, r.profile.stockLaunchers)
            assertEquals(DefaultPlanner.REASON_HOME_SCREEN, why, pkg.value)
            assertIs<PlanResult.Refused>(planner().plan(setup(r, setOf(pkg)), snap, facts, emptyList()), pkg.value)
        }
    }
}
