# Contributing to TVora

TVora is an independent preview based on Clean Your TV. Keep the upstream credits and licenses.
See README.md for build commands, current test coverage and hardware limitations.

Use the existing planner, typed operations, credential gate and restore flow for TV writes.
Never add arbitrary shell strings to UI code, log pairing codes, remove protected-package rules,
or commit ADB keys, signing material, local TV snapshots or device addresses.

For UI changes run the relevant Compose tests and capture current screenshots. The older upstream
goldens are reference, not TVora's new baseline. Refresh full baselines on Linux before enabling
whole-project screenshot verification. Document untested hardware behaviour explicitly.

This prepared checkout vendors the modified launcher source. Do not run upstream release/signing
scripts against the original author's project. TVora's own signing and update policy is still to
be established. The preview workflow builds an unsigned-for-distribution debug artifact only.
