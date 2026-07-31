# RomRaiderHD for macOS

RomRaiderHD uses JDK 17, follows the macOS light or dark appearance, renders
Swing text and original RomRaider artwork at Retina resolution, uses native
macOS menus and map title bars, and provides Java3D table rendering on Apple
Silicon.

## Build and install

An ARM64 JDK 17 or newer and Apache Ant are required. Run the installer from
the repository root:

```bash
tools/macos/install-macos-app.sh --launch
```

The default destination is `/Applications/RomRaiderHD.app`. An existing
standalone build can be packaged without recompilation:

```bash
tools/macos/install-macos-app.sh --skip-build --launch
```

Use `--app` to select another destination. `--source` and
`ROMRAIDER_SOURCE_DIR` can select another RomRaiderHD checkout.
`ROMRAIDER_JAVA_HOME` selects the ARM64 JDK 17+ compiler and runtime.

The installer performs a clean Ant standalone build, verifies Java 17
bytecode, replaces the legacy Java3D libraries with JogAmp builds, runs theme
and real-window 3D smoke tests, creates the Retina icon set, and signs the
application bundle locally.

## Appearance

The default `auto` theme follows the macOS appearance at application startup.
The theme can be selected explicitly:

```bash
ROMRAIDER_THEME=dark /Applications/RomRaiderHD.app/Contents/MacOS/RomRaiderHD
ROMRAIDER_THEME=light /Applications/RomRaiderHD.app/Contents/MacOS/RomRaiderHD
```

`ROMRAIDER_THEME` accepts `auto`, `dark`, or `light`.

## Runtime data

Settings and definitions remain in `~/.RomRaider`. Console output is appended
to `~/.RomRaider/romraider_sout.log`, and the application log is written to
`~/.RomRaider/rr_system.log`.

J2534 operation depends on a compatible vendor driver and is not covered by the
macOS checks. Optional Windows-only or vendor-specific Logger plugins can
report unavailable native libraries without preventing the Logger UI from
opening.
