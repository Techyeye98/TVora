# Workflow: Add an add-on

**When to use this:** a widget or tile that can be its own small app, shown through a module card.

1. Create `addons/<name>/` as its own Gradle project (`settings.gradle` and an `app` module), with its own
   package name.
2. It talks to the launcher only through the module-card contract (`docs/modules.md`, `docs/specs/card.md`).
   Nothing under `launcher/` changes, and `launcher/` never mentions `addons/`. If the idea needs a launcher
   change, it is not an add-on: open an issue.
3. Its `README.md` has a Permissions section listing every permission and why, plus any network access or
   background service. No telemetry without an opt-in, no ads, your own art.
4. Add one row to the table in `addons/README.md` linking `<name>/`, with the TV it was tested on.
5. No APKs in the repo. `tests/test_addons.py` checks all of this; `.github/workflows/addons.yml` builds the
   add-ons that changed with `./gradlew --no-daemon assembleRelease`.

Then run the tests: `python3 -m unittest discover -s tests -v`.
