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

Versions have the form `<upstream>-hd.<n>`, for example `1.1.0-hd.3`.
`<upstream>` is the RomRaider release the fork is based on (`version.major`,
`version.minor` and `version.patch` in `version.properties`). `<n>` numbers the
RomRaiderHD releases on that base and restarts at 1 with a new base.

Every push to `master` is released automatically: CI takes the number after
the highest existing `<upstream>-hd.<n>` tag, builds all packages with that
version, tags the commit and publishes a GitHub Release with the commit
subjects since the previous release. Pushes made while a release is building
go into the next release.

Local builds carry `<n>` = 0, for example `1.1.0-hd.0`, unless
`ROMRAIDER_VERSION_FORK` sets another number.

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
