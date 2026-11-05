# TVora release signing

TVora's public APKs use one existing signing identity. Keep that key for updates to existing
installs; do not generate a replacement key during a normal release. Keep a protected offline
backup of the keystore and its credentials, outside Git and release assets.

Expected certificate SHA-256:

`fb2895c0f426aca4b74a393530ef1d04aa337841c616e0554f5f42c8a26dd7c9`

The current identity is the development identity used since 0.1.0. Store distribution and any
key migration need a separate decision; publishing a newly signed APK can break in-place updates.

## Build and verify

1. Run core/flow/app tests, module checks and Android lint. CI also builds the optimized release.
2. Build `:app:assembleFullRelease`. It is unsigned unless local signing is configured.
3. Sign outside the repository with the existing key, or configure `CYT_RELEASE_KEYSTORE`,
   `CYT_RELEASE_KEY_ALIAS`, and the password variables documented in the build signing helper.
4. Run `apksigner verify --print-certs` on the APK. Match the certificate above and check
   `dev.tvora.app`, an increased versionCode and the intended versionName.
5. Publish the optimized APK and SHA256SUMS.txt only after the exact commit passes CI.
6. Download the public APK anonymously and compare its SHA-256 with the local signed APK.

The release page must be a public, stable release so the app can discover it through GitHub's
latest-release endpoint. The dashboard offers a release link; installation stays user-controlled.
No paid server or background push service is needed.

Never upload signing keys, ADB keys, pairing codes or TV backup ZIPs. CI debug artifacts are for
contributors and may use a different key; users should update from the official release APK.
