#Requires -Version 5.1

<#
.SYNOPSIS
Builds the RomRaiderHD Windows installer for a persistent installation.

.DESCRIPTION
Stages the Windows standalone package, adds a Java runtime produced with
jlink, and compiles tools/windows/RomRaiderHD.nsi into a setup executable.
The installed application runs from that bundled runtime, so it does not
depend on a system Java installation.

Requirements: a 64-bit JDK 17 or newer and NSIS 3 (makensis).

.EXAMPLE
tools\windows\build-windows-installer.ps1

.EXAMPLE
tools\windows\build-windows-installer.ps1 -SkipBuild
#>

[CmdletBinding()]
param(
    [string] $SourceDir,
    [string] $OutputDir,
    [string] $JavaHome,
    [string] $MakeNsis,
    [switch] $SkipBuild
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Fail([string] $Message) {
    throw $Message
}

function Invoke-Native {
    param(
        [Parameter(Mandatory = $true)] [string] $FilePath,
        [Parameter(Mandatory = $true)] [string] $What,
        [string[]] $Arguments = @()
    )

    & $FilePath @Arguments
    if ($LASTEXITCODE -ne 0) {
        Fail "$What failed with exit code $LASTEXITCODE"
    }
}

function Get-JavaSettings {
    param(
        [Parameter(Mandatory = $true)] [string] $JavaExe
    )

    $outFile = [System.IO.Path]::GetTempFileName()
    $errFile = [System.IO.Path]::GetTempFileName()
    try {
        $probe = Start-Process -FilePath $JavaExe -PassThru -Wait -NoNewWindow `
            -ArgumentList @('-XshowSettings:properties', '-version') `
            -RedirectStandardOutput $outFile -RedirectStandardError $errFile
        if ($probe.ExitCode -ne 0) {
            Fail "$JavaExe exited with code $($probe.ExitCode) while reporting its properties"
        }
        $settings = @(
            @(Get-Content -LiteralPath $errFile) + @(Get-Content -LiteralPath $outFile) |
                Where-Object { $_ -and $_.Trim() }
        )
    } finally {
        Remove-Item -LiteralPath $outFile, $errFile -Force -ErrorAction SilentlyContinue
    }

    if ($settings.Count -eq 0) {
        Fail "$JavaExe printed no properties"
    }
    return $settings
}

function Read-JavaProperty {
    param(
        [Parameter(Mandatory = $true)] [string[]] $Settings,
        [Parameter(Mandatory = $true)] [string] $Name
    )

    $match = $Settings |
        Select-String -Pattern ('^\s*' + [regex]::Escape($Name) + '\s*=\s*(\S+)') |
        Select-Object -First 1
    if (-not $match) {
        Write-Host ($Settings -join [System.Environment]::NewLine)
        Fail "the Java runtime did not report $Name"
    }
    return $match.Matches[0].Groups[1].Value
}

function Remove-Windows10Compatibility {
    param(
        [Parameter(Mandatory = $true)] [string] $Launcher
    )

    # The Intel HD Graphics 2000 and 3000 drivers do not support Windows 10
    # and give a process that declares Windows 10 compatibility only the
    # OpenGL 1.1 software renderer, which Java3D rejects. The Windows 10 entry
    # of the launcher manifest is blanked in place, which keeps the layout of
    # the executable, and the Authenticode signature it invalidates is removed.
    $bytes = [System.IO.File]::ReadAllBytes($Launcher)
    $text = [System.Text.Encoding]::GetEncoding(28591).GetString($bytes)
    $entry = '<supportedOS Id="{8e0f7a12-bfb3-4fe8-b9a5-48fd50a15a9a}"></supportedOS>'
    $index = $text.IndexOf($entry, [System.StringComparison]::Ordinal)
    if ($index -lt 0 -or
            $text.IndexOf($entry, $index + 1, [System.StringComparison]::Ordinal) -ge 0) {
        Fail "$Launcher does not declare Windows 10 compatibility exactly once"
    }
    for ($i = 0; $i -lt $entry.Length; $i++) {
        $bytes[$index + $i] = 0x20
    }

    $peHeader = [System.BitConverter]::ToInt32($bytes, 0x3C)
    $optionalHeader = $peHeader + 24
    if ([System.BitConverter]::ToUInt32($bytes, $peHeader) -ne 0x4550 -or
            [System.BitConverter]::ToUInt16($bytes, $optionalHeader) -ne 0x20B) {
        Fail "$Launcher is not a 64-bit PE executable"
    }
    # IMAGE_DIRECTORY_ENTRY_SECURITY holds the file offset of the signature
    $securityEntry = $optionalHeader + 112 + 4 * 8
    $signatureOffset = [System.BitConverter]::ToUInt32($bytes, $securityEntry)
    $signatureSize = [System.BitConverter]::ToUInt32($bytes, $securityEntry + 4)
    $length = $bytes.Length
    if ($signatureSize -ne 0) {
        if ([long] $signatureOffset + $signatureSize -ne $bytes.Length) {
            Fail "the signature of $Launcher does not end the file"
        }
        $length = [int] $signatureOffset
        [System.Array]::Clear($bytes, $securityEntry, 8)
        [System.Array]::Clear($bytes, $optionalHeader + 64, 4)
    }

    $stream = [System.IO.File]::Open($Launcher, [System.IO.FileMode]::Create)
    try {
        $stream.Write($bytes, 0, $length)
    } finally {
        $stream.Dispose()
    }
}

if ($PSVersionTable.PSVersion.Major -ge 6 -and -not $IsWindows) {
    Fail 'Windows is required'
}

if (-not $SourceDir) {
    $SourceDir = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
}
$SourceDir = (Resolve-Path -LiteralPath $SourceDir).Path
if (-not (Test-Path -LiteralPath (Join-Path $SourceDir 'build.xml'))) {
    Fail "RomRaiderHD build.xml not found in $SourceDir"
}

$nsisScript = Join-Path $PSScriptRoot 'RomRaiderHD.nsi'
if (-not (Test-Path -LiteralPath $nsisScript)) {
    Fail "installer script not found at $nsisScript"
}

if (-not $JavaHome) {
    if ($env:ROMRAIDER_JAVA_HOME) {
        $JavaHome = $env:ROMRAIDER_JAVA_HOME
    } elseif ($env:JAVA_HOME) {
        $JavaHome = $env:JAVA_HOME
    } else {
        Fail 'set ROMRAIDER_JAVA_HOME or JAVA_HOME to a 64-bit JDK 17 or newer'
    }
}
$JavaHome = (Resolve-Path -LiteralPath $JavaHome).Path
$javaExe = Join-Path $JavaHome 'bin\java.exe'
$jlinkExe = Join-Path $JavaHome 'bin\jlink.exe'
foreach ($tool in @($javaExe, $jlinkExe)) {
    if (-not (Test-Path -LiteralPath $tool)) {
        Fail "$tool is missing; a JDK is required, not a JRE"
    }
}

$javaSettings = Get-JavaSettings -JavaExe $javaExe
$javaSpec = Read-JavaProperty -Settings $javaSettings -Name 'java.specification.version'
$javaArch = Read-JavaProperty -Settings $javaSettings -Name 'os.arch'
$javaMajor = [int](($javaSpec -split '\.')[0])
if ($javaMajor -lt 17) {
    Fail "JDK version is $javaSpec, expected 17 or newer"
}
if ($javaArch -ne 'amd64') {
    Fail "JDK architecture is $javaArch, expected amd64"
}

if (-not $MakeNsis) {
    $candidates = New-Object System.Collections.ArrayList
    $onPath = Get-Command 'makensis.exe' -ErrorAction SilentlyContinue
    if ($onPath) { $null = $candidates.Add($onPath.Source) }
    foreach ($root in @(${env:ProgramFiles(x86)}, $env:ProgramFiles)) {
        if ($root) {
            $null = $candidates.Add((Join-Path $root 'NSIS\makensis.exe'))
        }
    }
    $MakeNsis = $candidates |
        Where-Object { $_ -and (Test-Path -LiteralPath $_) } |
        Select-Object -First 1
}
if (-not $MakeNsis -or -not (Test-Path -LiteralPath $MakeNsis)) {
    Fail 'makensis was not found; install NSIS 3 or pass -MakeNsis'
}
$MakeNsis = (Resolve-Path -LiteralPath $MakeNsis).Path

if (-not $SkipBuild) {
    Write-Host "Building the standalone package with $JavaHome..."
    Push-Location -LiteralPath $SourceDir
    try {
        $env:JAVA_HOME = $JavaHome
        Invoke-Native -FilePath 'cmd.exe' -What 'ant standalone' `
            -Arguments @('/c', 'ant', 'standalone')
    } finally {
        Pop-Location
    }
}

$distDir = Join-Path $SourceDir 'build\dist\windows'
$archives = @(Get-ChildItem -LiteralPath $distDir -Filter 'RomRaiderHD*-windows.zip' `
        -ErrorAction SilentlyContinue)
if ($archives.Count -ne 1) {
    Fail "expected one Windows standalone archive in $distDir, found $($archives.Count)"
}
$archive = $archives[0]

if (-not $OutputDir) { $OutputDir = $distDir }
$null = New-Item -ItemType Directory -Force -Path $OutputDir
$OutputDir = (Resolve-Path -LiteralPath $OutputDir).Path
$setupName = $archive.Name -replace '-windows\.zip$', '-setup.exe'
$setupPath = Join-Path $OutputDir $setupName

$versionProperties = @{}
Get-Content -LiteralPath (Join-Path $SourceDir 'version.properties') |
    ForEach-Object {
        if ($_ -match '^\s*(version\.(?:major|minor|patch|fork))\s*=\s*(\d+)\s*$') {
            $versionProperties[$Matches[1]] = $Matches[2]
        }
    }
if ($env:ROMRAIDER_VERSION_FORK) {
    if ($env:ROMRAIDER_VERSION_FORK -notmatch '^\d+$') {
        Fail "ROMRAIDER_VERSION_FORK is not a number: $env:ROMRAIDER_VERSION_FORK"
    }
    $versionProperties['version.fork'] = $env:ROMRAIDER_VERSION_FORK
}
foreach ($key in @('version.major', 'version.minor', 'version.patch', 'version.fork')) {
    if (-not $versionProperties.ContainsKey($key)) {
        Fail "$key is missing from version.properties"
    }
}
$baseVersion = '{0}.{1}.{2}' -f $versionProperties['version.major'],
    $versionProperties['version.minor'], $versionProperties['version.patch']
$appVersion = '{0}-hd.{1}' -f $baseVersion, $versionProperties['version.fork']
$appVersion4 = '{0}.{1}' -f $baseVersion, $versionProperties['version.fork']

$stageDir = Join-Path $env:TEMP ('romraiderhd-installer-' + [guid]::NewGuid().ToString('N'))
$extractDir = Join-Path $stageDir 'extract'
$null = New-Item -ItemType Directory -Force -Path $extractDir

try {
    Write-Host "Staging $($archive.Name)..."
    Expand-Archive -LiteralPath $archive.FullName -DestinationPath $extractDir -Force
    $payloadDir = Join-Path $extractDir 'RomRaiderHD'
    if (-not (Test-Path -LiteralPath (Join-Path $payloadDir 'RomRaiderHD.jar'))) {
        Fail 'standalone archive does not contain RomRaiderHD/RomRaiderHD.jar'
    }
    if (-not (Test-Path -LiteralPath (Join-Path $payloadDir 'lib\windows\64\phidget21.dll'))) {
        Fail 'the standalone archive does not carry the 64-bit native libraries'
    }
    $joglNatives = @(Get-ChildItem -LiteralPath (Join-Path $payloadDir 'lib\common') `
            -Filter 'jogl-all-*-natives-windows-amd64.jar' -ErrorAction SilentlyContinue)
    if ($joglNatives.Count -ne 1) {
        Fail 'the standalone archive does not carry the JOGL natives for windows-amd64'
    }

    # Start Menu shortcuts drive the bundled runtime; run.bat serves the ZIP.
    Remove-Item -LiteralPath (Join-Path $payloadDir 'run.bat') -Force `
        -ErrorAction SilentlyContinue

    $iconSource = Join-Path $SourceDir 'src\main\resources\graphics\romraider-ico.ico'
    if (-not (Test-Path -LiteralPath $iconSource)) {
        Fail "application icon not found at $iconSource"
    }
    Copy-Item -LiteralPath $iconSource `
        -Destination (Join-Path $payloadDir 'RomRaiderHD.ico')

    Write-Host 'Linking the Java runtime...'
    $runtimeDir = Join-Path $payloadDir 'runtime'
    $modules = @(
        'java.se',
        'jdk.charsets',
        'jdk.crypto.ec',
        'jdk.localedata',
        'jdk.unsupported',
        'jdk.zipfs'
    ) -join ','
    $compression = if ($javaMajor -ge 21) { '--compress=zip-6' } else { '--compress=2' }
    Invoke-Native -FilePath $jlinkExe -What 'jlink' -Arguments @(
        '--add-modules', $modules,
        '--strip-debug',
        '--no-header-files',
        '--no-man-pages',
        $compression,
        '--output', $runtimeDir
    )

    $bundledJava = Join-Path $runtimeDir 'bin\java.exe'
    $bundledJavaw = Join-Path $runtimeDir 'bin\javaw.exe'
    foreach ($launcher in @($bundledJava, $bundledJavaw)) {
        if (-not (Test-Path -LiteralPath $launcher)) {
            Fail "jlink produced no $launcher"
        }
        Remove-Windows10Compatibility -Launcher $launcher
    }

    $runtimeSettings = Get-JavaSettings -JavaExe $bundledJava
    $runtimeSpec = Read-JavaProperty -Settings $runtimeSettings `
        -Name 'java.specification.version'
    if ([int](($runtimeSpec -split '\.')[0]) -lt 17) {
        Fail "the bundled runtime reports Java $runtimeSpec"
    }

    Write-Host "Compiling $setupName..."
    if (Test-Path -LiteralPath $setupPath) {
        Remove-Item -LiteralPath $setupPath -Force
    }
    Invoke-Native -FilePath $MakeNsis -What 'makensis' -Arguments @(
        '/V3',
        '/DAPPNAME=RomRaiderHD',
        "/DAPPVERSION=$appVersion",
        "/DAPPVERSION4=$appVersion4",
        '/DPUBLISHER=RomRaider.com',
        "/DSOURCEDIR=$payloadDir",
        "/DOUTFILE=$setupPath",
        "/DICONFILE=$(Join-Path $payloadDir 'RomRaiderHD.ico')",
        "/DLICENSEFILE=$(Join-Path $payloadDir 'license.txt')",
        $nsisScript
    )

    if (-not (Test-Path -LiteralPath $setupPath)) {
        Fail "makensis produced no $setupPath"
    }
    $setupSize = [math]::Round((Get-Item -LiteralPath $setupPath).Length / 1MB, 1)
    Write-Host "Built RomRaiderHD $appVersion"
    Write-Host "Installer: $setupPath ($setupSize MB)"
} finally {
    Remove-Item -LiteralPath $stageDir -Recurse -Force -ErrorAction SilentlyContinue
}
