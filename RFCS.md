# RFCs (stream up037)

## RFC-UP037-1: the config differential test keeps launcher 2.5's reader

The app bundles launcher 2.7 (v2.7.0, 3e34f40). ConfigDifferentialTest compared the codec with the
upstream Config.java of the pinned submodule, copied in at build time. Launcher 2.7's Config.java
reads 67 new keys and needs Android stubs (android.os.Environment) and resources (R entries for
the new keys, WallpaperMood, ThemeFiles, WallpaperRotation.ORDER_SHUFFLE) the test harness does not
have. Smallest change: the 2.5 reader (v2.5.0, 9071ec8) is vendored in core/spec/src/test/java/
com/gathin/tvhome/pinned with its digests pinned, so the differential test still runs. The read code
of the keys the codec models is unchanged from 2.5 to 2.7 (checked for the bump), so the codec still
agrees with the bundled launcher on them. Open: port the 2.7 reader (stubs and resources), model the
new keys the app sets or shows, and carry the rest without the "the launcher ignores it" warning.
