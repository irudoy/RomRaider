# RomRaiderHD for Windows

Windows is served by two packages. The standalone ZIP unpacks anywhere and
runs on a system Java installation through `run.bat`. The installer places
RomRaiderHD under the user profile together with its own Java runtime, adds
Start Menu and desktop shortcuts, and registers an entry in Apps & features.

## Build the installer

A 64-bit JDK 17 or newer and NSIS 3 are required. Run the build from the
repository root:

```powershell
tools\windows\build-windows-installer.ps1
```

The script runs `ant standalone`, stages the Windows package, links a Java
runtime with `jlink`, and compiles `tools/windows/RomRaiderHD.nsi`. The setup
executable is written next to the standalone ZIP in `build\dist\windows` and
carries the same version, for example `RomRaiderHD-1.1.0-hd.1-setup.exe`.

An existing standalone build can be packaged without recompilation:

```powershell
tools\windows\build-windows-installer.ps1 -SkipBuild
```

`-JavaHome` selects the JDK, `ROMRAIDER_JAVA_HOME` and `JAVA_HOME` serve as
defaults. `-MakeNsis` points at `makensis.exe` when NSIS is not on `PATH` and
not in `Program Files (x86)\NSIS`. `-OutputDir` selects another destination
for the setup executable.

## What the installer produces

The default destination is `%LOCALAPPDATA%\Programs\RomRaiderHD`, so
installation and removal need no administrator rights. Shortcuts start
`runtime\bin\javaw.exe` from the installation directory, which makes the
installed application independent of any system Java. The Start Menu group
holds the ECU Editor, the Logger in normal, full screen and touch screen
modes, the license, the release notes, and the uninstaller.

Reinstallation replaces the payload of the previous installation. Settings,
profiles and definitions live in `%USERPROFILE%\.RomRaider` and are left
untouched by both installation and removal.

The setup executable is unsigned, so SmartScreen warns on first run.

## Standalone package

`run.bat` in the extracted ZIP starts the application from a system Java
installation:

```bat
run.bat
run.bat logger
run.bat logger.fullscreen
run.bat logger.touch
```

It requires a 64-bit Java 17 or newer, taken from `ROMRAIDER_JAVA_HOME`, then
`JAVA_HOME`, then `PATH`, and reports the reason when no suitable runtime is
found. A `settings.xml` placed next to `run.bat` keeps that copy separate from
an installed version.

## Runtime data

Settings and definitions live in `%USERPROFILE%\.RomRaider`. The application
log is written to `%USERPROFILE%\.RomRaider\rr_system.log` and records every
uncaught exception together with its stack trace.

Console output is appended to `%USERPROFILE%\.RomRaider\romraider_sout.log`.
`run.bat` redirects it there. The installer shortcuts start `javaw.exe`, which
has no console, and pass `-Dromraider.consoleLog=true`, so the application
writes that file itself.

The 3D table view runs on JogAmp Java3D 1.7.2 with the JOGL 2.6.0 natives for
`windows-amd64`, both carried in `lib\common`. No separate Java3D or OpenGL
installation is needed.
