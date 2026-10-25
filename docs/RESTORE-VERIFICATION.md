# Restore verification — TVora 0.1.0

The release review covers the recorded-change restore path, planner, execution journal,
rollback/resume, and exported recovery bundle. Automated checks use simulated TVs; they do
not prove recovery on a physical TV or a particular manufacturer's firmware.

## What restore does

- Computes changes from committed operation journals and the earliest recorded prior state.
- Restores package enabled states, including DEFAULT, rather than enabling every disabled app.
- Returns recorded settings and the original Home where the necessary prior state is known.
- Keeps the added launcher by default; optional removal follows restoration of stock Home.
- Removes launcher overlay files that the app created when the records show they were absent.
- Keeps credential confirmation, connection checks and the open-run/resume safeguards.

## Checks

- `PlannerPropertyTest`: randomized apply → undo comparisons against the baseline, including
  protected packages, settings and stock Home.
- `SetupLookTest` and `UndoOrderTest`: earliest prior state, ordering, removal of newly created
  config, and preservation of the Home-switch guard.
- Engine tests: failed steps, rollback, interrupted journals, and resume after simulated crashes.
- Backup tests: bundle checksums, identity, tamper rejection, saved baseline, generated commands,
  and matching shell/PowerShell command quoting.
- `RestoreShellTest` on Linux CI: exported `restore.sh` executes against an on-disk fake TV;
  baseline comparison, wrong-TV refusal, dry run, failed steps and hostile labels are checked.
- Restore flow/UI regressions: confirmation is required; incomplete, rolled-back and abandoned
  runs do not display success or forget launcher metadata. Factory-reset preparation with skipped
  steps is not reported ready. A real factory reset is not performed by this action.

Local Windows checks exclude POSIX interpreter tests; the GitHub workflow runs the complete
backup suite on Linux. Windows recovery scripts are checked for structure and exact command
quoting; the current release review does not claim a new physical Windows-to-TV recovery run.

## Limits

- Only changes recorded by this app can be reconstructed. Earlier manual cleanup or changes by
  another phone/tool require their original backup.
- App data, logins, firmware, user files and arbitrary settings are not a full-device backup.
- Existing launcher config/wallpaper files are not generally rebuilt. Newly created overlay files
  can be removed; an earlier wallpaper or existing file may need its own saved copy.
- Exported ZIPs contain recovery commands and recorded state; they do not include imported photos
  or the phone's private pairing/signing keys.
- Keep the phone's app data and export **Save a copy** before doing substantial changes. A factory
  reset or deleting app data can remove the local records that in-app restore needs.

The 0.1.0 review found and fixed a status-reporting defect: the restore flow formerly treated any
finished run as successful, even when steps were skipped. Success now requires COMPLETED with
no skipped steps; incomplete recovery displays a warning and allows a fresh restore preview.
