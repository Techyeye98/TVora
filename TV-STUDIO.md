# TV Studio Preview

Local development fork of udhaykumarbala/cleanyourtv-app at a8e55aa.
Phone application ID: `dev.tvstudio.preview`. No upstream app updates are offered.
Nothing has been installed on a real phone or TV by this development task.

## Added

- Phone-only ADB pairing: six-digit code, pairing port, connection port. TLS 1.3 through
  Conscrypt, an Android Keystore identity, and the existing TV identity confirmation before cleanup.
  Pairing codes are never put in saved UI state, preferences or diagnostics.
- Personal wallpaper: system image picker, 20 MB input limit, sampled decoding, private JPEG copy
  with metadata stripped, and wallpaper on/off. The image remains on the phone and TV.
- Home app visibility: hide/show without disabling or uninstalling. Hidden apps retain their saved
  position and return to it when shown again.
- Inputs, network and Bluetooth quick-button switches. TV settings remains available.
- Custom launcher 2.7-tvstudio1 (versionCode 9), with a Bluetooth accessory button, signed with the
  local development key. The original upstream-signed TV curtain is retained with its own signer pin.
- Personal settings survive dock edits, theme changes and adding cards.

The existing cleanup planner, protected-package rules, restore point, watchdog and credential gate
remain in use. Software updaters stay on in this preview.

## Limits to test on hardware

- Modern pairing and the Android Keystore/Conscrypt handshake need a real Android phone and a
  pairing-capable TV. This is a preview, not a claim of universal TV support.
- Wireless-debugging connection ports can change after a restart. Manage refreshes the advertised
  port when discoverable; Setup also offers pairing by hand. Keep both devices on a trusted LAN.
- ADB's self-signed server certificates do not provide ordinary HTTPS hostname authentication.
  Pairing binds the PAKE exchange to TLS; later ADB connections authenticate the authorized client.
  AOSP's temporary pairing certificate cannot be used as a pin for the separate adbd connection.
- Bluetooth opens a resolvable accessory screen, falling back to Bluetooth settings or system settings.
  It does not promise headphones have paired or audio has switched.
- Existing manufacturer / Android-version restrictions on launcher switching still apply.
- No arbitrary APK uploader, app uninstaller, profiles or setup sharing added in this iteration.
- Personal photos are private app files; export of the existing recovery scripts does not export them.

## Source and licenses

Original app and launcher: MIT, with their existing notices retained.
Native SPAKE2 uses `com.github.MuntashirAkon.spake2-java:spake2-android:2.2.1` (LGPL-3.0),
from the author's JitPack repository, restricted to that one module. Its license is bundled in
`adb/pairing/notices/spake2-java-LICENSE`. Corresponding source: https://github.com/MuntashirAkon/spake2-java/tree/2.2.1
The pure-Java variant was evaluated and rejected after its authenticated exchange failed; none of
that experimental Java implementation is shipped.
PairingCipher's HKDF/GCM implementation adapts `PairingAuthCtx` from MuntashirAkon/libadb-android
commit c849886ebc6d48e7b46d967e78a6bb65c90c3b74 under its Apache-2.0 option.
The adapter is Kotlin with a platform exchange seam so framing and AEAD can be tested on the JVM.
The Apache license is in `adb/pairing/notices/libadb-Apache-2.0`.
Distribute the corresponding library source and license notices with this preview's source.
Conscrypt is the published `org.conscrypt:conscrypt-android:2.5.3` artifact.

## Implementation decision

The new persisted personalization document is separate from HomeChoice so older dock/theme/card flows
cannot erase custom choices. Look.encode is the common merge point. UI writes go through the existing
planner, credential gate and engine. No raw shell strings were added to feature or app code.
The Android platform module now depends on core:flows for the WirelessPairing port; adb:pairing uses
the already pinned Bouncy Castle dependency. Conscrypt and the native SPAKE2 artifact are explicitly
allowed in ModuleRules. JVM protocol tests use a fake agreed secret; they do not validate native PAKE.
The launcher's source is a locally modified pinned submodule; include that modified source when
sharing a checkout or patch. The existing forge signing/reproducibility scripts target upstream
release keys and are not the build process for this locally signed preview.
