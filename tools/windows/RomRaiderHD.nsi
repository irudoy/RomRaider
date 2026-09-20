; RomRaiderHD Windows installer.
;
; Built by tools/windows/build-windows-installer.ps1, which stages the payload
; and supplies every define below. The payload carries its own Java runtime, so
; the installed application does not depend on a system Java installation.

Unicode true

!include "MUI2.nsh"
!include "FileFunc.nsh"
!insertmacro GetSize

!ifndef APPNAME
  !error "APPNAME is undefined"
!endif
!ifndef APPVERSION
  !error "APPVERSION is undefined"
!endif
!ifndef APPVERSION4
  !error "APPVERSION4 is undefined"
!endif
!ifndef PUBLISHER
  !error "PUBLISHER is undefined"
!endif
!ifndef SOURCEDIR
  !error "SOURCEDIR is undefined"
!endif
!ifndef OUTFILE
  !error "OUTFILE is undefined"
!endif
!ifndef ICONFILE
  !error "ICONFILE is undefined"
!endif
!ifndef LICENSEFILE
  !error "LICENSEFILE is undefined"
!endif

!define JVM_ARGS "-Djava.library.path=lib\windows\64 -Dawt.useSystemAAFontSettings=lcd -Dswing.aatext=true -Dsun.java2d.d3d=true -Xms64M -Xmx512M"
!define JAVAW "$INSTDIR\runtime\bin\javaw.exe"
!define UNINST_KEY "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}"

Name "${APPNAME} ${APPVERSION}"
OutFile "${OUTFILE}"
InstallDir "$LOCALAPPDATA\Programs\${APPNAME}"
InstallDirRegKey HKCU "Software\${APPNAME}" "InstallLocation"
RequestExecutionLevel user
SetCompressor /SOLID lzma
ShowInstDetails show
ShowUninstDetails show

VIProductVersion "${APPVERSION4}"
VIAddVersionKey "ProductName" "${APPNAME}"
VIAddVersionKey "ProductVersion" "${APPVERSION}"
VIAddVersionKey "FileVersion" "${APPVERSION4}"
VIAddVersionKey "FileDescription" "${APPNAME} setup"
VIAddVersionKey "CompanyName" "${PUBLISHER}"
VIAddVersionKey "LegalCopyright" "${PUBLISHER}"

!define MUI_ICON "${ICONFILE}"
!define MUI_UNICON "${ICONFILE}"
!define MUI_ABORTWARNING
!define MUI_FINISHPAGE_RUN
!define MUI_FINISHPAGE_RUN_TEXT "Start ${APPNAME}"
!define MUI_FINISHPAGE_RUN_FUNCTION LaunchApplication

!insertmacro MUI_PAGE_LICENSE "${LICENSEFILE}"
!insertmacro MUI_PAGE_COMPONENTS
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

!insertmacro MUI_LANGUAGE "English"

Function LaunchApplication
    SetOutPath "$INSTDIR"
    Exec '"${JAVAW}" ${JVM_ARGS} -jar "${APPNAME}.jar"'
FunctionEnd

Section "${APPNAME}" SectionApplication
    SectionIn RO

    ; Replace the payload of a previous installation. User settings and
    ; profiles live in %USERPROFILE%\.RomRaider and are never touched.
    RMDir /r "$INSTDIR\runtime"
    RMDir /r "$INSTDIR\lib"
    RMDir /r "$INSTDIR\plugins"
    RMDir /r "$INSTDIR\customize"
    RMDir /r "$INSTDIR\i18n"
    RMDir /r "$INSTDIR\src"

    SetOutPath "$INSTDIR"
    File /r "${SOURCEDIR}\*"

    WriteUninstaller "$INSTDIR\Uninstall.exe"

    CreateDirectory "$SMPROGRAMS\${APPNAME}"
    CreateShortcut "$SMPROGRAMS\${APPNAME}\${APPNAME}.lnk" \
        "${JAVAW}" '${JVM_ARGS} -jar "${APPNAME}.jar"' \
        "$INSTDIR\${APPNAME}.ico" 0 SW_SHOWNORMAL "" "${APPNAME} ECU Editor"
    CreateShortcut "$SMPROGRAMS\${APPNAME}\${APPNAME} Logger.lnk" \
        "${JAVAW}" '${JVM_ARGS} -jar "${APPNAME}.jar" -logger' \
        "$INSTDIR\${APPNAME}.ico" 0 SW_SHOWNORMAL "" "${APPNAME} Logger"
    CreateShortcut "$SMPROGRAMS\${APPNAME}\${APPNAME} Logger Full Screen.lnk" \
        "${JAVAW}" '${JVM_ARGS} -jar "${APPNAME}.jar" -logger.fullscreen' \
        "$INSTDIR\${APPNAME}.ico" 0 SW_SHOWNORMAL "" "${APPNAME} Logger in full screen mode"
    CreateShortcut "$SMPROGRAMS\${APPNAME}\${APPNAME} Logger Touch Screen.lnk" \
        "${JAVAW}" '${JVM_ARGS} -jar "${APPNAME}.jar" -logger.touch' \
        "$INSTDIR\${APPNAME}.ico" 0 SW_SHOWNORMAL "" "${APPNAME} Logger in touch screen mode"
    CreateShortcut "$SMPROGRAMS\${APPNAME}\License.lnk" "$INSTDIR\license.txt"
    CreateShortcut "$SMPROGRAMS\${APPNAME}\Release Notes.lnk" "$INSTDIR\release_notes.txt"
    CreateShortcut "$SMPROGRAMS\${APPNAME}\Uninstall ${APPNAME}.lnk" "$INSTDIR\Uninstall.exe"

    WriteRegStr HKCU "Software\${APPNAME}" "InstallLocation" "$INSTDIR"
    WriteRegStr HKCU "${UNINST_KEY}" "DisplayName" "${APPNAME} ${APPVERSION}"
    WriteRegStr HKCU "${UNINST_KEY}" "DisplayVersion" "${APPVERSION}"
    WriteRegStr HKCU "${UNINST_KEY}" "DisplayIcon" "$INSTDIR\${APPNAME}.ico"
    WriteRegStr HKCU "${UNINST_KEY}" "Publisher" "${PUBLISHER}"
    WriteRegStr HKCU "${UNINST_KEY}" "InstallLocation" "$INSTDIR"
    WriteRegStr HKCU "${UNINST_KEY}" "UninstallString" '"$INSTDIR\Uninstall.exe"'
    WriteRegStr HKCU "${UNINST_KEY}" "QuietUninstallString" '"$INSTDIR\Uninstall.exe" /S'
    WriteRegDWORD HKCU "${UNINST_KEY}" "NoModify" 1
    WriteRegDWORD HKCU "${UNINST_KEY}" "NoRepair" 1
    ${GetSize} "$INSTDIR" "/S=0K" $0 $1 $2
    IntFmt $0 "0x%08X" $0
    WriteRegDWORD HKCU "${UNINST_KEY}" "EstimatedSize" $0
SectionEnd

Section "Desktop shortcut" SectionDesktop
    SetOutPath "$INSTDIR"
    CreateShortcut "$DESKTOP\${APPNAME}.lnk" \
        "${JAVAW}" '${JVM_ARGS} -jar "${APPNAME}.jar"' \
        "$INSTDIR\${APPNAME}.ico" 0 SW_SHOWNORMAL "" "${APPNAME} ECU Editor"
SectionEnd

!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
!insertmacro MUI_DESCRIPTION_TEXT ${SectionApplication} \
    "${APPNAME} with a bundled Java runtime."
!insertmacro MUI_DESCRIPTION_TEXT ${SectionDesktop} \
    "Place a ${APPNAME} shortcut on the desktop."
!insertmacro MUI_FUNCTION_DESCRIPTION_END

Section "Uninstall"
    Delete "$DESKTOP\${APPNAME}.lnk"
    RMDir /r "$SMPROGRAMS\${APPNAME}"

    Delete "$INSTDIR\Uninstall.exe"
    RMDir /r "$INSTDIR\runtime"
    RMDir /r "$INSTDIR\lib"
    RMDir /r "$INSTDIR\plugins"
    RMDir /r "$INSTDIR\customize"
    RMDir /r "$INSTDIR\i18n"
    RMDir /r "$INSTDIR\src"
    Delete "$INSTDIR\${APPNAME}.jar"
    Delete "$INSTDIR\${APPNAME}.ico"
    Delete "$INSTDIR\license.txt"
    Delete "$INSTDIR\release_notes.txt"
    RMDir "$INSTDIR"

    DeleteRegKey HKCU "${UNINST_KEY}"
    DeleteRegKey HKCU "Software\${APPNAME}"
SectionEnd
