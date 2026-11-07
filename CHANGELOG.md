# TVora changelog

## 0.1.4 (unreleased)

- Reconnect to saved TVs at changed addresses/subnets using their trusted key and TV identity; keep the dashboard connection only while resumed.
- Bound stalled ADB stream opens and discard silent connections so interrupted operations can pause and reconnect safely.
- Deliver and verify themes through the launcher's ThemeProvider on compatible Android versions; retain the existing route on Android 9.
- Offer launchable system apps in the dock list and exclude launcher placeholders and background-only services.
- Protect required Skyworth remote services, TCL update helpers and Kalley home/setup packages.
- Preserve current visuals, motion, TV launcher assets and signing identity.

## 0.1.3 (unreleased)

- Use Android 9-compatible APIs for ADB public keys and restore ZIP timestamps.
- Install the launcher explicitly for TV user 0 on the streamed install route.
- Read the current TV state for both Undo everything's preview and its execution.
- Preserve the existing UI, launcher customizations, restore-to-original-Home fix and signing identity.

## 0.1.2 (2026-11-05)

- Remove Cards and modules entry points from setup and the dashboard.
- Show a new-version card using TVora's GitHub releases; open the release page to update.
- Add Star the GitHub repository, Made by Namya Mehta and Telegram contact in Settings.
- Clarify release signing, privacy, security contact and TVora's own version history.
- Keep the existing glass interface, animations and TV cleanup/restore behavior.

## 0.1.1 (2026-11-02)

- Open the original TV Home automatically after all restore/theme cleanup finishes.
- Verify cleanup, restore and reboot against the MiTV-AXSO0 baseline; owner-confirmed use on two TVs.

## 0.1.0 (2026-10-26)

- First TVora release: app ordering and visibility, personal wallpaper, Inputs/Wi-Fi/Bluetooth shortcuts.
- Arrange apps with the TV remote; preserve dock spacing when adding rows.
- Preserve the original glass UI and motion; add the TVora glass TV icon.
- Save/export recovery records and report incomplete restores correctly.

See [upstream history](docs/UPSTREAM-CHANGELOG.md) for inherited Clean Your TV versions.
