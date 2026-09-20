@echo off
setlocal enableextensions

:: RomRaiderHD launcher for the standalone Windows package.
::
:: Usage: run.bat [editor|logger|logger.fullscreen|logger.touch]
::
:: A 64-bit Java 17 or newer runtime is required. It is taken from
:: ROMRAIDER_JAVA_HOME, then JAVA_HOME, then PATH. A settings.xml placed next
:: to this script keeps this installation separate from an installed version.

cd /d "%~dp0"

set "APP_JAR=RomRaiderHD.jar"
set "LOG_DIR=%USERPROFILE%\.RomRaider"
set "LOG_FILE=%LOG_DIR%\romraider_sout.log"
set "JVM_ARGS=-Djava.library.path=lib\windows\64 -Dawt.useSystemAAFontSettings=lcd -Dswing.aatext=true -Dsun.java2d.d3d=true -Xms64M -Xmx512M"

set "MODE=%~1"
if not defined MODE set "MODE=editor"

set "APP_ARGS="
if /i "%MODE%"=="editor" (goto :mode_resolved)
if /i "%MODE%"=="logger" (set "APP_ARGS=-logger" & goto :mode_resolved)
if /i "%MODE%"=="logger.fullscreen" (set "APP_ARGS=-logger.fullscreen" & goto :mode_resolved)
if /i "%MODE%"=="logger.touch" (set "APP_ARGS=-logger.touch" & goto :mode_resolved)
echo Unknown mode "%MODE%".
echo Usage: run.bat [editor^|logger^|logger.fullscreen^|logger.touch]
goto :failed

:mode_resolved
if not exist "%APP_JAR%" (
    echo %APP_JAR% is missing from "%CD%".
    echo Run this script from the extracted RomRaiderHD package.
    goto :failed
)

set "JAVA_DIR="
if defined ROMRAIDER_JAVA_HOME set "JAVA_DIR=%ROMRAIDER_JAVA_HOME%"
if not defined JAVA_DIR if defined JAVA_HOME set "JAVA_DIR=%JAVA_HOME%"

if defined JAVA_DIR (
    set "JAVA_EXE=%JAVA_DIR%\bin\java.exe"
    set "JAVAW_EXE=%JAVA_DIR%\bin\javaw.exe"
) else (
    set "JAVA_EXE=java.exe"
    set "JAVAW_EXE=javaw.exe"
)

if defined JAVA_DIR (
    if not exist "%JAVA_EXE%" (
        echo No Java runtime at "%JAVA_DIR%".
        echo Point ROMRAIDER_JAVA_HOME or JAVA_HOME at a 64-bit JDK 17 or newer.
        goto :failed
    )
) else (
    where java.exe >nul 2>&1
    if errorlevel 1 (
        echo No Java runtime on PATH.
        echo Install a 64-bit Java 17 or newer, or set ROMRAIDER_JAVA_HOME.
        goto :failed
    )
)

set "PROBE=%TEMP%\romraiderhd-java-probe.txt"
"%JAVA_EXE%" -XshowSettings:properties -version >"%PROBE%" 2>&1
if errorlevel 1 (
    echo Java at "%JAVA_EXE%" failed to report its version.
    del "%PROBE%" >nul 2>&1
    goto :failed
)

set "JAVA_SPEC="
set "JAVA_BITS="
set "JAVA_ARCH="
for /f "tokens=3" %%v in ('findstr /c:"java.specification.version" "%PROBE%"') do set "JAVA_SPEC=%%v"
for /f "tokens=3" %%b in ('findstr /c:"sun.arch.data.model" "%PROBE%"') do set "JAVA_BITS=%%b"
for /f "tokens=3" %%a in ('findstr /c:"os.arch" "%PROBE%"') do set "JAVA_ARCH=%%a"
del "%PROBE%" >nul 2>&1

if not defined JAVA_SPEC (
    echo Unable to determine the version of Java at "%JAVA_EXE%".
    goto :failed
)

for /f "delims=." %%m in ("%JAVA_SPEC%") do set "JAVA_MAJOR=%%m"
if %JAVA_MAJOR% LSS 17 (
    echo Java %JAVA_SPEC% at "%JAVA_EXE%" is too old.
    echo RomRaiderHD requires Java 17 or newer.
    goto :failed
)

if not "%JAVA_BITS%"=="64" (
    echo Java at "%JAVA_EXE%" is %JAVA_BITS%-bit.
    echo RomRaiderHD requires a 64-bit runtime.
    goto :failed
)

if /i not "%JAVA_ARCH%"=="amd64" (
    echo Java at "%JAVA_EXE%" reports os.arch=%JAVA_ARCH%.
    echo The communication and 3D libraries in lib\windows\64 are x86-64.
    goto :failed
)

if not exist "%LOG_DIR%" mkdir "%LOG_DIR%" >nul 2>&1

start "RomRaiderHD" /NORMAL "%JAVAW_EXE%" %JVM_ARGS% -jar "%APP_JAR%" %APP_ARGS% >>"%LOG_FILE%" 2>&1
echo Started RomRaiderHD in %MODE% mode. Console output goes to "%LOG_FILE%".
endlocal
exit /b 0

:failed
echo.
pause
endlocal
exit /b 1
