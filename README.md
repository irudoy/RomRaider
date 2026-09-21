# RomRaiderHD

Fork of [RomRaider](https://github.com/RomRaider/RomRaider) with macOS
support, Logger plugins and a Java 17 runtime. Based on RomRaider 1.1.0.

Packages: [Releases](https://github.com/irudoy/RomRaider/releases).

## Differences from upstream

- macOS support: Apple Silicon application with native menu bar, Retina
  rendering, light and dark theme.
- Logger plugins: UDP external data source for values sent by other programs
  ([docs](docs/UDP_External_Data_Source.md)); per-user plugin files in
  `~/.RomRaider/plugins`.
- Windows installer with its own Java runtime.
- Java 17 or newer, 64-bit only.
- 3D table view on JogAmp Java3D 1.7.2 and JOGL 2.6.0.
- Sharp icons on scaled displays.
- Logger starts without a Logger definition; Overlay Log works on table axes.
- Editor fixes in table navigation, ROM loading and closing, user level saving
  and the table filter. The "Table click behavior" setting is removed.
- Start mode as a launcher argument:
  `run.sh [editor|logger|logger.fullscreen|logger.touch]`.
- Toolbars cannot be detached.

## Versioning

RomRaiderHD versions have the form `<upstream>-hd.<n>`, for example
`1.1.0-hd.1`:

- `<upstream>` is the RomRaider release the fork is based on
  (`version.major`, `version.minor` and `version.patch` in
  `version.properties`).
- `<n>` is `version.fork`, the number of the RomRaiderHD release on that base.
  It restarts at 1 when the fork moves to a new upstream release, so
  `1.1.0-hd.3` is followed by `1.2.0-hd.1`.

The version appears in the window titles and About dialogs followed by the
build date, in the package names (`RomRaiderHD-1.1.0-hd.1-windows.zip`,
`RomRaiderHD-1.1.0-hd.1-setup.exe`), in the Windows installer and its Apps &
features entry, and in the macOS application bundle, where
`CFBundleShortVersionString` holds the upstream part and `CFBundleVersion` the
fork number. `version.buildnumber` keeps the build number of the upstream base
release.

A release is made from a commit that sets the version in `version.properties`
and adds a `## <version> - <date>` section to [CHANGELOG.md](CHANGELOG.md).
Pushing a tag equal to the version starts the release:

    git tag 1.1.0-hd.1
    git push origin 1.1.0-hd.1

The build workflow checks that the tag matches `version.properties` and that
`CHANGELOG.md` has a section for it, builds all packages and publishes the
GitHub Release `RomRaiderHD 1.1.0-hd.1` with that section as its notes.
Upstream tags such as `1.1.0-NOV26-2025` have a different form and do not
start a release.

Every push to `master` publishes the prerelease `master-<commit>` with the same
set of packages. A build between two releases reports the version recorded in
`version.properties` at its commit; the build date in the About dialog and the
commit in the prerelease name identify it.

## RomRaider

RomRaider is a free, open source tuning suite created for viewing, logging and
tuning of modern Subaru Engine Control Units. The intuitive tuning interface
and powerful datalogger are modelled to be familiar to experienced professional
tuners while providing all the power of expensive commercial products, without
license fees.

See [docs/Building_RomRaider.txt](docs/Building_RomRaider.txt) for building
from source.

See the following links for further information:

 - http://www.romraider.com/
 - http://www.romraider.com/forum/
 - https://github.com/RomRaider/RomRaider
