# F-Droid status and historical upstream references

TVora is distributed through [GitHub releases](https://github.com/Techyeye98/TVora/releases/latest).
This repository has no `fdroid.yml` workflow, configured F-Droid submission pipeline, or TVora
reproducible-build comparison against F-Droid. The current CI is `.github/workflows/preview.yml`;
it runs TVora tests, builds and lint, including the optimized release build.

The files in this directory are preserved historical material from the upstream
[Clean Your TV app](https://github.com/udhaykumarbala/cleanyourtv-app):

- [MERGE-REQUEST.md](MERGE-REQUEST.md) is an upstream submission draft. Its checklist and CI claims
  do not describe TVora's current submission status.
- [tv.cleanyour.app.yml](tv.cleanyour.app.yml) targets upstream's app ID, repository, release versions
  and signing certificate. It must not be used to build or submit `dev.tvora.app`.
- [Build evidence](../history/reviews/FDROID-S2-build-evidence.md) records an upstream build comparison,
  not a verification of a TVora release.

Any future TVora F-Droid submission needs its own metadata, signing/reproducibility verification
and applicable CI setup. The historical references are not evidence that those steps are complete.
See [TVora release signing](../RELEASING.md) for the current APK release process.
