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

// The TVs of the 0.3.3 "plan refused" reports, as plain data from what their reports say (nothing
// here came from a TV). All three: Home already answers to com.gathin.tvhome/.MainActivity, the new
// home screen's package, and the only other claimant is FallbackHome; the TV's own home screen is
// switched off. The app's own record of a home switch is empty ("Home switch: not recorded"), so the
// launcher was put there another way (CleanYourTV's tool for computers installs its launcher under
// the same package name, signed with another key). #171: Xiaomi MiTV4I (magnolia, Android 9), Xiaomi
// brand sure set. #187: Xiaomi MIBOX4 (oneday, Android 9), exact profile xiaomi-mi-box-s. #220:
// Polytron POLYTRON4K (songshan, Google TV 11), no profile.
object Issue034 {
    val gathinHome = Component(PackageName("com.gathin.tvhome"), ".MainActivity")
    val fallbackHome = Component(PackageName("com.android.tv.settings"), ".system.FallbackHome")
    val tvlauncher = PackageName("com.google.android.tvlauncher")
    val launcherx = PackageName("com.google.android.apps.tv.launcherx")
    // Not the app's pinned signer (Samples.launcherArtifact signs with zeroHash).
    val otherSigner = Sha256("7".repeat(64))

    private fun system(enabled: EnabledState = EnabledState.ENABLED, label: String? = null) =
        PackageState(enabled, system = true, versionCode = 1, signer = null, persistent = false, tvInput = false, label = label, installedByUs = false)

    // The launcher on the TV: not a system app, a higher version than the pin, signed by `signer`.
    fun launcherState(signer: Sha256?) =
        PackageState(EnabledState.ENABLED, system = false, versionCode = 99, signer = signer, persistent = false, tvInput = false, label = "TV Home", installedByUs = false)

    private val common: List<String> = listOf(
        "android", "com.android.systemui", "com.android.tv.settings", "com.android.vending", "com.android.providers.tv",
        "com.google.android.gms", "com.google.android.gsf", "com.google.android.katniss", "com.google.android.apps.mediashell",
        "com.google.android.feedback", "com.google.android.partnersetup", "com.google.android.play.games",
        "com.google.android.youtube.tv", "com.netflix.ninja",
    )
    private val xiaomi: List<String> = listOf("com.mitv.tvhome.atv", "com.xiaomi.statistic", "com.miui.tv.analytics", "com.xiaomi.mitv.updateservice")

    // #171: "Brand: Xiaomi, Manufacturer: xiaomi, Model: MiTV4I, Device: magnolia, Android: 9 (SDK 28)".
    fun magnoliaFacts(): DeviceFacts = Samples.miTvFacts().copy(
        brand = "Xiaomi", manufacturer = "xiaomi", model = "MiTV4I", device = "magnolia", sdk = 28, release = "9",
        fingerprint = "Xiaomi/magnolia/magnolia:9/PI/3698:user/release-keys", hasGms = true,
    )

    // #187: "Brand: Xiaomi, Manufacturer: Xiaomi, Model: MIBOX4, Device: oneday, Android: 9 (SDK 28)".
    fun onedayFacts(): DeviceFacts = Samples.miTvFacts().copy(
        brand = "Xiaomi", manufacturer = "Xiaomi", model = "MIBOX4", device = "oneday", sdk = 28, release = "9",
        fingerprint = "Xiaomi/oneday/oneday:9/PI/3933:user/release-keys", hasGms = true,
    )

    // #220: "Brand: Polytron, Manufacturer: WLTV, Model: POLYTRON4K, Device: songshan, Android: 11 (SDK 30)".
    fun songshanFacts(): DeviceFacts = Samples.miTvFacts().copy(
        brand = "Polytron", manufacturer = "WLTV", model = "POLYTRON4K", device = "songshan", sdk = 30, release = "11",
        fingerprint = "Polytron/songshan_POLYTRON_4K/songshan:11/RTT2.220118.001/V1:user/release-keys", hasGms = true,
    )

    // The report's HOME lines, with the launcher signed by `signer` (null: the TV did not say).
    fun snapshot(facts: DeviceFacts, stock: PackageName, extra: List<String>, signer: Sha256? = otherSigner,
                 reason: SnapshotReason = SnapshotReason.BASELINE): Snapshot = Samples.snapshot(
        facts = facts, reason = reason,
        packages = (common + extra).associate { PackageName(it) to system() } + mapOf(
            stock to system(EnabledState.DISABLED_USER, "Home"),
            gathinHome.pkg to launcherState(signer),
        ),
        home = HomeResolution(gathinHome, listOf(HomeClaimant(gathinHome, 0), HomeClaimant(fallbackHome, -1000))),
    )

    fun magnoliaSnapshot(signer: Sha256? = otherSigner, reason: SnapshotReason = SnapshotReason.BASELINE) = snapshot(magnoliaFacts(), tvlauncher, xiaomi, signer, reason)
    fun onedaySnapshot(signer: Sha256? = otherSigner, reason: SnapshotReason = SnapshotReason.BASELINE) = snapshot(onedayFacts(), tvlauncher, xiaomi, signer, reason)
    fun songshanSnapshot(signer: Sha256? = otherSigner, reason: SnapshotReason = SnapshotReason.BASELINE) = snapshot(songshanFacts(), launcherx, emptyList(), signer, reason)
}
