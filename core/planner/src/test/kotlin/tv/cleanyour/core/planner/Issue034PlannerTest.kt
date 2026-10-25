package tv.cleanyour.core.planner

import tv.cleanyour.core.model.Level
import tv.cleanyour.core.model.Op
import tv.cleanyour.core.model.Snapshot
import tv.cleanyour.core.planner.PlannerFixtures.artifact
import tv.cleanyour.core.planner.PlannerFixtures.choices
import tv.cleanyour.core.planner.PlannerFixtures.facts
import tv.cleanyour.core.planner.PlannerFixtures.planner
import tv.cleanyour.core.spec.BundledProfileSource
import tv.cleanyour.core.spec.ProfileResolution
import tv.cleanyour.testing.fixtures.Issue034
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

// 0.3.4: the "plan refused" reports (#171 Xiaomi MiTV4I, #187 Xiaomi Mi Box S, #220 Polytron Google
// TV). Home already answers to com.gathin.tvhome, the new home screen's package name, signed with
// another key. 0.3.3's wizard took that for ours and asked for the install and the switch; the
// planner's D36 rule (a launcher with another key is replaced only while the TV's own home screen is
// in charge) refused the whole plan. The rule stays; the wizard no longer asks (SetupMachineTest),
// and the clean-up alone is a plan that never touches that launcher.
class Issue034PlannerTest {
    private val source = BundledProfileSource()

    private fun reports(): List<Pair<String, Snapshot>> = listOf(
        "#171" to Issue034.magnoliaSnapshot(),
        "#187" to Issue034.onedaySnapshot(),
        "#220" to Issue034.songshanSnapshot(),
    )

    // What 0.3.3's wizard asked for on these TVs: install, switch, and a signer replace.
    private fun asIn033(r: ProfileResolution) =
        PlanRequest.Setup(r, choices(installLauncher = true, takeover = true, replaceSigner = true), artifact(), Level.SIMPLE)

    private fun cleanUpAlone(r: ProfileResolution) =
        PlanRequest.Setup(r, choices(installLauncher = false, takeover = false), artifact(), Level.SIMPLE)

    @Test fun issue171And187And220TheReportsResolveAsTheySay() {
        assertEquals("Xiaomi", assertIs<ProfileResolution.BrandSureSet>(source.resolveNow(Issue034.magnoliaFacts())).brand)
        assertEquals("xiaomi-mi-box-s", assertIs<ProfileResolution.Exact>(source.resolveNow(Issue034.onedayFacts())).profile.id)
        // 0.3.5: the Polytron careful set (one list), with the generic stock launchers of no set.
        val polytron = assertIs<ProfileResolution.BrandSureSet>(source.resolveNow(Issue034.songshanFacts()))
        assertEquals("Polytron", polytron.brand)
        assertEquals(tv.cleanyour.core.spec.KnownLaunchers.generic, polytron.stockLaunchers)
    }

    // The refusal the reports logged, reproduced: the D36 rule, for each of the three shapes.
    @Test fun issue171And187And220TheSwitchOverAnotherKeyIsStillRefused() {
        for ((issue, snap) in reports()) {
            val r = source.resolveNow(snap.facts)
            val refused = assertIs<PlanResult.Refused>(planner().plan(asIn033(r), snap, facts, emptyList()), issue).reasons
            assertTrue(refused.any { "D36" in it }, "$issue: $refused")
            // Without the person's consent to replace it, still refused (never an uninstall).
            val noConsent = PlanRequest.Setup(r, choices(installLauncher = true, takeover = true, replaceSigner = false), artifact(), Level.SIMPLE)
            assertIs<PlanResult.Refused>(planner().plan(noConsent, snap, facts, emptyList()), issue)
        }
    }

    // The clean-up alone: a plan, with no install, no uninstall, no home screen switch, and the
    // launcher in charge never switched off.
    @Test fun issue171And187And220TheCleanUpAloneIsPlanned() {
        for ((issue, snap) in reports()) {
            val r = source.resolveNow(snap.facts)
            val plan = assertIs<PlanResult.Ok>(planner().plan(cleanUpAlone(r), snap, facts, emptyList()), issue).plan
            assertTrue(plan.sections.isEmpty(), issue)
            assertTrue(plan.steps.none { it.op is Op.SetHome || it.op is Op.Install || it.op is Op.InstallStream || it.op is Op.Uninstall && (it.op as Op.Uninstall).pkg == Issue034.gathinHome.pkg }, issue)
            assertTrue(plan.steps.none { it.op == Op.Disable(Issue034.gathinHome.pkg) }, issue)
            assertTrue(!plan.summary.replacesLauncher, issue)
        }
    }

    // Ours with our key already in charge: the takeover is already done, and the plan is fine.
    @Test fun issue171And187And220OursWithOurKeyIsAlreadyDone() {
        for ((issue, snap0) in reports()) {
            val snap = when (issue) {
                "#171" -> Issue034.magnoliaSnapshot(signer = artifact().signer)
                "#187" -> Issue034.onedaySnapshot(signer = artifact().signer)
                else -> Issue034.songshanSnapshot(signer = artifact().signer)
            }
            assertEquals(snap0.home, snap.home)
            val r = source.resolveNow(snap.facts)
            val req = PlanRequest.Setup(r, choices(installLauncher = true, takeover = true, replaceSigner = false), artifact(), Level.SIMPLE)
            val plan = assertIs<PlanResult.Ok>(planner().plan(req, snap, facts, emptyList()), issue).plan
            assertTrue(plan.sections.isEmpty(), "$issue: already ours, no switch")
            assertTrue(plan.summary.homeScreenPossible, issue)
            assertTrue(plan.steps.none { it.op is Op.Uninstall || it.op is Op.SetHome }, issue)
        }
    }
}
