package tv.cleanyour.testing.fixtures

import tv.cleanyour.core.model.Component
import tv.cleanyour.core.model.DeviceFacts
import tv.cleanyour.core.model.EnabledState
import tv.cleanyour.core.model.HomeClaimant
import tv.cleanyour.core.model.HomeResolution
import tv.cleanyour.core.model.PackageName
import tv.cleanyour.core.model.PackageState
import tv.cleanyour.core.model.Sha256
import tv.cleanyour.core.model.Snapshot
import tv.cleanyour.core.model.SnapshotReason

// The TV of issue #375 (0.3.4), as plain data from what its problem report says (nothing here came
// from a TV): a Hisense U75, "Model: SmartTV 4K FFM, Device: hengshan, Android: 12 (SDK 31)", Google
// TV, exact profile hisense-u75-hengshan (the wizard quirk). The report was read after the home
// screen switch had been rolled back (WRONG_FOCUS): Home resolves to Google TV's own home screen, the
// setup wizard is back on and claims Home with its RecoveryActivity, and the new home screen the run
// installed is still there. The diagnostics log says "setup wizard and saved Wi-Fi: others_own".
object Issue036 {
    val launcherx = Component(PackageName("com.google.android.apps.tv.launcherx"), ".home.HomeActivity")
    val recovery = Component(PackageName("com.google.android.tungsten.setupwraith"), ".RecoveryActivity")
    val gathinHome = Component(PackageName("com.gathin.tvhome"), ".MainActivity")
    val fallbackHome = Component(PackageName("com.android.tv.settings"), ".system.FallbackHome")
    val wizard: PackageName get() = recovery.pkg

    // The lines of the report's diagnostics log after the rollback, word for word.
    const val REFUSED_LINE = "setup: refused because Google TV setup wizard cannot be switched off: it is the TV's own home screen; only the home screen switch changes it"

    private fun system(enabled: EnabledState = EnabledState.ENABLED, label: String? = null) =
        PackageState(enabled, system = true, versionCode = 1, signer = null, persistent = false, tvInput = false, label = label, installedByUs = false)

    // "Brand: Hisense, Manufacturer: Hisense, Model: SmartTV 4K FFM, Device: hengshan, Android: 12 (SDK 31)".
    fun hengshanFacts(): DeviceFacts = Samples.miTvFacts().copy(
        brand = "Hisense", manufacturer = "Hisense", model = "SmartTV 4K FFM", device = "hengshan", sdk = 31, release = "12",
        fingerprint = "Hisense/hengshan/hengshan:12/STT2.230310.002/V1:user/release-keys", hasGms = true,
    )

    // The set's apps as far as its tested list names them: what the list switches off, what it keeps,
    // the home screens, the wizard and the system parts.
    val hengshanSystem: List<String> = listOf(
        "android", "com.android.systemui", "com.android.tv.settings", "com.android.vending", "com.android.providers.tv",
        "com.google.android.gms", "com.google.android.gsf", "com.google.android.katniss", "com.google.android.apps.mediashell",
        "com.google.android.tv.remote.service", "com.google.android.inputmethod.latin",
        "com.vt.tv.evservice", "com.vt.hishow", "net.vidaatv.hisense.android.tv", "com.vidaa.oemtabcontentprovider",
        "com.vt.storemode", "com.vt.tv.b2b", "com.vt.tv.protector", "com.vt.tv.calmanconnector",
        "android.autoinstalls.config.google.gtvpai", "com.google.android.partnersetup", "com.google.android.feedback",
        "com.android.printspooler", "com.google.android.syncadapters.calendar",
        "com.vt.tv.tvcenter", "com.vt.tv.input", "com.vt.source.external", "com.vt.source.tuner",
        "com.vt.tv.smartsense", "com.vt.tv.smartsense.ui", "com.vt.tv.ai_sense", "com.vt.tv.aiot",
    )

    // `ours`: the new home screen the run installed, with the app's pinned key (Samples.launcherArtifact).
    fun hengshanSnapshot(reason: SnapshotReason = SnapshotReason.MANUAL, ours: Sha256 = Samples.launcherArtifact().signer): Snapshot = Samples.snapshot(
        facts = hengshanFacts(), reason = reason,
        packages = hengshanSystem.associate { PackageName(it) to system() } + mapOf(
            launcherx.pkg to system(label = "Google TV Home"),
            recovery.pkg to system(label = "Setup"),
            gathinHome.pkg to PackageState(EnabledState.ENABLED, system = false, versionCode = 1, signer = ours, persistent = false, tvInput = false,
                label = "Clean Your TV Home", installedByUs = false),
        ),
        home = HomeResolution(launcherx, listOf(HomeClaimant(launcherx, 2), HomeClaimant(recovery, 1), HomeClaimant(gathinHome, 0), HomeClaimant(fallbackHome, -1000))),
    )
}
