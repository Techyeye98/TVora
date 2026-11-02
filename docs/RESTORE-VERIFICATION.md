# Restore verification — TVora 0.1.1

The release review covers the recorded-change restore path, planner, execution journal,
rollback/resume, and exported recovery bundle. Automated checks use simulated TVs. A physical MiTV-AXSO0
(Android 9) cleanup, restore and reboot test on 2 November 2026 also passed: all 112 original
packages matched their baseline enabled/installed/hidden/suspended states, no packages remained
disabled, and the original Google Home persisted after reboot. The added launcher was kept as an
ordinary app. This device test does not establish compatibility with every TV.

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

## 0.1.1: finish on the restored Home

The physical test exposed a handoff bug: the original Home became default, but removing the
new launcher's config restarted that launcher and left it visible. The undo plan now opens the
recorded original Home as its final operation, after file cleanup and accessibility restoration.
This also works when the original Home is already default, including a repeated restore. Unknown
original Homes are not launched. A failed final launch is recorded as an incomplete restore.
Regression tests cover kept/removed launchers, cleanup ordering and the already-restored case.
