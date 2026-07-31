#!/bin/bash

set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repository_dir="$(cd -- "${script_dir}/../.." && pwd)"
source_dir="${ROMRAIDER_SOURCE_DIR:-${repository_dir}}"
app_path="${ROMRAIDER_APP_PATH:-/Applications/RomRaiderHD.app}"
launcher_source="${script_dir}/RomRaiderHD"
info_plist_source="${script_dir}/Info.plist"
app_icon_source="${script_dir}/romraiderhd-app.png"
smoke_test_source="${script_dir}/ThemeSmokeTest.java"
java3d_adapter_source="${script_dir}/Java3dBytecodeAdapter.java"
graph3d_smoke_test_source="${script_dir}/Graph3dSmokeTest.java"
build_runtime=true
launch_app=false
maven_central="https://repo.maven.apache.org/maven2"

usage() {
	printf '%s\n' \
		'Usage: install-macos-app.sh [options]' \
		'' \
		'Build and install a local RomRaiderHD application bundle for Apple Silicon.' \
		'' \
		'Options:' \
		'  --source DIR       RomRaiderHD source tree' \
		'  --app PATH         Destination .app path' \
		'  --skip-build       Package the existing Linux standalone build' \
		'  --launch           Launch the installed application' \
		'  -h, --help         Show this help' \
		'' \
		'Environment:' \
		'  ROMRAIDER_SOURCE_DIR  Default source tree' \
		'  ROMRAIDER_APP_PATH    Default destination .app path' \
		'  ROMRAIDER_JAVA_HOME   ARM64 JDK 17+ used for compilation and checks' \
		'  ROMRAIDER_ANT         Apache Ant executable used for the build'
}

fail() {
	printf 'error: %s\n' "$*" >&2
	exit 1
}

romraider_is_running() {
	/usr/bin/pgrep -f \
		'com[.]romraider[.](theme[.]RomRaiderBootstrap|ECUExec|logger[.]ecu[.]EcuLoggerExec)' \
		>/dev/null 2>&1
}

require_romraider_stopped() {
	if romraider_is_running; then
		fail "RomRaiderHD is running; close it before installation"
	fi
}

download_artifact() {
	local relative_path="$1"
	local expected_sha256="$2"
	local target="$3"
	local temporary_target="${target}.download"
	local actual_sha256

	/usr/bin/curl \
		--fail \
		--location \
		--silent \
		--show-error \
		"${maven_central}/${relative_path}" \
		--output "${temporary_target}"
	actual_sha256="$(/usr/bin/shasum -a 256 "${temporary_target}" |
		/usr/bin/awk '{ print $1 }')"
	[[ "${actual_sha256}" == "${expected_sha256}" ]] ||
		fail "SHA-256 mismatch for ${relative_path}"
	/bin/mv "${temporary_target}" "${target}"
}

while (($#)); do
	case "$1" in
		--source)
			(($# >= 2)) || fail "--source requires a directory"
			source_dir="$2"
			shift 2
			;;
		--app)
			(($# >= 2)) || fail "--app requires a path"
			app_path="$2"
			shift 2
			;;
		--skip-build)
			build_runtime=false
			shift
			;;
		--launch)
			launch_app=true
			shift
			;;
		-h|--help)
			usage
			exit 0
			;;
		*)
			fail "unknown option: $1"
			;;
	esac
done

[[ "$(/usr/bin/uname -s)" == "Darwin" ]] || fail "macOS is required"
[[ "$(/usr/bin/uname -m)" == "arm64" ]] || fail "Apple Silicon is required"
[[ -f "${source_dir}/build.xml" ]] ||
	fail "RomRaiderHD build.xml not found in ${source_dir}"
[[ "${app_path}" == *.app ]] ||
	fail "destination must have an .app suffix"
[[ -f "${launcher_source}" ]] ||
	fail "RomRaiderHD launcher not found at ${launcher_source}"
[[ -f "${info_plist_source}" ]] ||
	fail "Info.plist not found at ${info_plist_source}"
[[ -f "${app_icon_source}" ]] ||
	fail "RomRaiderHD app icon not found at ${app_icon_source}"
[[ -f "${smoke_test_source}" ]] ||
	fail "theme smoke test not found at ${smoke_test_source}"
[[ -f "${java3d_adapter_source}" ]] ||
	fail "Java3D bytecode adapter not found at ${java3d_adapter_source}"
[[ -f "${graph3d_smoke_test_source}" ]] ||
	fail "Java3D smoke test not found at ${graph3d_smoke_test_source}"
[[ -x /usr/bin/pgrep ]] || fail "pgrep is required"
require_romraider_stopped

stage_dir="$(/usr/bin/mktemp -d "${TMPDIR:-/tmp}/romraider-macos.XXXXXX")"
previous_app=""
installed=false

cleanup() {
	if [[ "${installed}" != true &&
		-n "${previous_app}" &&
		-d "${previous_app}" &&
		! -e "${app_path}" ]]; then
		/bin/mv "${previous_app}" "${app_path}"
	fi
	/bin/rm -rf "${stage_dir}"
}
trap cleanup EXIT

if [[ -n "${ROMRAIDER_JAVA_HOME:-}" ]]; then
	java_home="${ROMRAIDER_JAVA_HOME}"
else
	java_home="$(/usr/libexec/java_home -v 17+ -a arm64 2>/dev/null || true)"
fi
[[ -x "${java_home}/bin/java" ]] ||
	fail "an ARM64 JDK 17 or newer is required"

java_arch="$("${java_home}/bin/java" -XshowSettings:properties -version 2>&1 |
	/usr/bin/awk '$1 == "os.arch" { print $3; exit }')"
java_version="$("${java_home}/bin/java" -XshowSettings:properties -version 2>&1 |
	/usr/bin/awk '$1 == "java.version" { print $3; exit }')"
java_major="${java_version%%.*}"
if [[ "${java_major}" == "1" ]]; then
	java_remainder="${java_version#1.}"
	java_major="${java_remainder%%.*}"
fi
[[ "${java_arch}" == "aarch64" ]] ||
	fail "JDK architecture is ${java_arch:-unknown}, expected aarch64"
[[ "${java_major}" =~ ^[0-9]+$ ]] ||
	fail "unable to determine JDK version"
((java_major >= 17)) ||
	fail "JDK version is ${java_version:-unknown}, expected 17 or newer"

if [[ -n "${ROMRAIDER_ANT:-}" ]]; then
	ant_bin="${ROMRAIDER_ANT}"
else
	ant_bin="$(command -v ant || true)"
fi
if "${build_runtime}"; then
	[[ -x "${ant_bin}" ]] || fail "Apache Ant is required"
fi
[[ -x /usr/bin/curl ]] || fail "curl is required"
[[ -x /usr/bin/iconutil ]] || fail "macOS iconutil is required"
[[ -x /usr/bin/shasum ]] || fail "shasum is required"
[[ -x /usr/bin/sips ]] || fail "macOS sips is required"

if "${build_runtime}"; then
	printf 'Building RomRaiderHD with %s (%s)...\n' \
		"${java_home}" "${java_arch}"
	build_date="$(LC_ALL=C /bin/date '+%b%d' |
		/usr/bin/tr '[:lower:]' '[:upper:]')"
	(
		cd "${source_dir}"
		JAVA_HOME="${java_home}" \
			"${ant_bin}" \
			-Dmnth.day="${build_date}" \
			standalone
	)
fi

shopt -s nullglob
archives=("${source_dir}"/build/dist/linux/RomRaider*-linux.zip)
shopt -u nullglob
[[ "${#archives[@]}" -eq 1 ]] ||
	fail "expected one Linux standalone archive, found ${#archives[@]}"
archive="${archives[0]}"

version_major="$(/usr/bin/awk -F= \
	'$1 == "version.major" { print $2 }' \
	"${source_dir}/version.properties")"
version_minor="$(/usr/bin/awk -F= \
	'$1 == "version.minor" { print $2 }' \
	"${source_dir}/version.properties")"
version_patch="$(/usr/bin/awk -F= \
	'$1 == "version.patch" { print $2 }' \
	"${source_dir}/version.properties")"
version_build="$(/usr/bin/awk -F= \
	'$1 == "version.buildnumber" { print $2 }' \
	"${source_dir}/version.properties")"
short_version="${version_major}.${version_minor}.${version_patch}"

extract_dir="${stage_dir}/extract"
new_app="${stage_dir}/RomRaiderHD.app"
/bin/mkdir -p \
	"${extract_dir}" \
	"${new_app}/Contents/MacOS" \
	"${new_app}/Contents/Resources"
/usr/bin/ditto -x -k "${archive}" "${extract_dir}"
[[ -f "${extract_dir}/RomRaider/RomRaider.jar" ]] ||
	fail "standalone archive does not contain RomRaider/RomRaider.jar"

/bin/mv \
	"${extract_dir}/RomRaider" \
	"${new_app}/Contents/Resources/RomRaiderHD"
/bin/cp "${info_plist_source}" "${new_app}/Contents/Info.plist"
/bin/cp "${launcher_source}" "${new_app}/Contents/MacOS/RomRaiderHD"
/bin/chmod 755 "${new_app}/Contents/MacOS/RomRaiderHD"

runtime_dir="${new_app}/Contents/Resources/RomRaiderHD"
iconset_dir="${stage_dir}/RomRaiderHD.iconset"
/bin/mkdir -p "${iconset_dir}"
for icon_size in 16 32 128 256 512; do
	retina_size=$((icon_size * 2))
	/usr/bin/sips \
		-z "${icon_size}" "${icon_size}" \
		"${app_icon_source}" \
		--out "${iconset_dir}/icon_${icon_size}x${icon_size}.png" \
		>/dev/null
	/usr/bin/sips \
		-z "${retina_size}" "${retina_size}" \
		"${app_icon_source}" \
		--out "${iconset_dir}/icon_${icon_size}x${icon_size}@2x.png" \
		>/dev/null
done
/usr/bin/iconutil \
	-c icns \
	"${iconset_dir}" \
	-o "${new_app}/Contents/Resources/RomRaiderHD.icns"

java3d_build_dir="${stage_dir}/java3d-build"
java3d_download_dir="${java3d_build_dir}/downloads"
java3d_adapter_classes="${java3d_build_dir}/adapter-classes"
java3d_adapted_dir="${java3d_build_dir}/adapted"
legacy_java3d_dir="${java3d_build_dir}/legacy"
/bin/mkdir -p \
	"${java3d_download_dir}" \
	"${java3d_adapter_classes}" \
	"${java3d_adapted_dir}" \
	"${legacy_java3d_dir}"

download_artifact \
	"org/jogamp/java3d/java3d-core/1.7.2/java3d-core-1.7.2.jar" \
	"894b00e177da90590deec06274ec7410db0c7fe8cfdb75af4013fd4a83c4b95b" \
	"${java3d_download_dir}/java3d-core-1.7.2.jar"
download_artifact \
	"org/jogamp/java3d/java3d-utils/1.7.2/java3d-utils-1.7.2.jar" \
	"194f3ec7a857c883fe4704a56c510fb769cfaec9a2eb48f5942a247eb5d82f03" \
	"${java3d_download_dir}/java3d-utils-1.7.2.jar"
download_artifact \
	"org/jogamp/java3d/vecmath/1.7.2/vecmath-1.7.2.jar" \
	"4bb77e7ec930c6e59be3533ced4597a4c43b5eee2262f9f9bc147cadb2ff7f5b" \
	"${java3d_download_dir}/vecmath-1.7.2.jar"
download_artifact \
	"org/jogamp/gluegen/gluegen-rt/2.6.0/gluegen-rt-2.6.0.jar" \
	"465bbc8d410b872a76b5b901cdb9c2c07905edd5e61a7120dc6a4d007880ec2f" \
	"${java3d_download_dir}/gluegen-rt-2.6.0.jar"
download_artifact \
	"org/jogamp/gluegen/gluegen-rt/2.6.0/gluegen-rt-2.6.0-natives-macosx-universal.jar" \
	"e543d5e81bc8b8f63e5dc365ac47da009f82175b2c6eba16a24ce3b2eb031773" \
	"${java3d_download_dir}/gluegen-rt-2.6.0-natives-macosx-universal.jar"
download_artifact \
	"org/jogamp/jogl/jogl-all/2.6.0/jogl-all-2.6.0.jar" \
	"34c919bc6073c2d9e73cbe7558c4e9de6b5c58146f3658e2bcc5f23ec3fccc9f" \
	"${java3d_download_dir}/jogl-all-2.6.0.jar"
download_artifact \
	"org/jogamp/jogl/jogl-all/2.6.0/jogl-all-2.6.0-natives-macosx-universal.jar" \
	"81db6a2b50f3803d4307a8bd7570a4f78c41ae0d44361208d0885a5cdf289dfa" \
	"${java3d_download_dir}/jogl-all-2.6.0-natives-macosx-universal.jar"
download_artifact \
	"org/jogamp/joal/joal/2.6.0/joal-2.6.0.jar" \
	"ac50705ca328ddb7664882fce582306b88722f936b81642f8c384fd707de57ce" \
	"${java3d_download_dir}/joal-2.6.0.jar"
download_artifact \
	"org/jogamp/joal/joal/2.6.0/joal-2.6.0-natives-macosx-universal.jar" \
	"3cdaefb36713d1fc71740fa9ce07cb3713c582fa7c5ac80c527e439f031ae853" \
	"${java3d_download_dir}/joal-2.6.0-natives-macosx-universal.jar"

"${java_home}/bin/javac" \
	--release 17 \
	-d "${java3d_adapter_classes}" \
	"${java3d_adapter_source}"
"${java_home}/bin/java" \
	-classpath "${java3d_adapter_classes}" \
	com.romraider.build.Java3dBytecodeAdapter \
	"${runtime_dir}/lib/common/Graph3d.jar" \
	"${java3d_adapted_dir}/Graph3d.jar"

for legacy_java3d in Graph3d.jar j3dcore.jar j3dutils.jar vecmath.jar; do
	[[ -f "${runtime_dir}/lib/common/${legacy_java3d}" ]] ||
		fail "legacy Java3D library ${legacy_java3d} is missing"
	/bin/mv \
		"${runtime_dir}/lib/common/${legacy_java3d}" \
		"${legacy_java3d_dir}/${legacy_java3d}"
done
/bin/cp \
	"${java3d_adapted_dir}/Graph3d.jar" \
	"${java3d_download_dir}/java3d-core-1.7.2.jar" \
	"${java3d_download_dir}/java3d-utils-1.7.2.jar" \
	"${java3d_download_dir}/vecmath-1.7.2.jar" \
	"${java3d_download_dir}/gluegen-rt-2.6.0.jar" \
	"${java3d_download_dir}/gluegen-rt-2.6.0-natives-macosx-universal.jar" \
	"${java3d_download_dir}/jogl-all-2.6.0.jar" \
	"${java3d_download_dir}/jogl-all-2.6.0-natives-macosx-universal.jar" \
	"${java3d_download_dir}/joal-2.6.0.jar" \
	"${java3d_download_dir}/joal-2.6.0-natives-macosx-universal.jar" \
	"${runtime_dir}/lib/common/"

smoke_classes="${stage_dir}/smoke-classes"
/bin/mkdir -p "${smoke_classes}"
"${java_home}/bin/javac" \
	--release 17 \
	-classpath "${runtime_dir}/RomRaider.jar:${runtime_dir}/lib/common/*" \
	-d "${smoke_classes}" \
	"${smoke_test_source}" \
	"${graph3d_smoke_test_source}"

for theme_class in \
	com.romraider.theme.DarkNimbusLookAndFeel \
	com.romraider.theme.HiDpiIconScaler \
	com.romraider.theme.MacNativeMenuBar \
	com.romraider.theme.ThemePalette \
	com.romraider.theme.MacHiDpiBootstrap \
	com.romraider.theme.RomRaiderBootstrap; do
	"${java_home}/bin/javap" \
		-classpath "${runtime_dir}/RomRaider.jar:${runtime_dir}/lib/common/*" \
		"${theme_class}" >/dev/null
done

bytecode_signature="${stage_dir}/bytecode.txt"
"${java_home}/bin/javap" \
	-verbose \
	-classpath "${runtime_dir}/RomRaider.jar:${runtime_dir}/lib/common/*" \
	com.romraider.Settings >"${bytecode_signature}"
/usr/bin/grep -q \
	"major version: 61" \
	"${bytecode_signature}" ||
	fail "RomRaiderHD classes are not Java 17 bytecode"

graph3d_signature="${stage_dir}/graph3d-signature.txt"
"${java_home}/bin/javap" \
	-private \
	-classpath "${runtime_dir}/RomRaider.jar:${runtime_dir}/lib/common/*" \
	com.ecm.graphics.Graph3dJPanel >"${graph3d_signature}"
/usr/bin/grep -q \
	"org.jogamp.java3d.Canvas3D canvas3d" \
	"${graph3d_signature}" ||
	fail "Graph3d.jar was not adapted to JogAmp Java3D"

(
	cd "${runtime_dir}"
	"${java_home}/bin/java" \
		-Djava.awt.headless=true \
		-classpath "RomRaider.jar:lib/common/*:${smoke_classes}" \
		ThemeSmokeTest
)
(
	cd "${runtime_dir}"
	"${java_home}/bin/java" \
		--add-opens=java.desktop/sun.awt=ALL-UNNAMED \
		-Dapple.awt.application.appearance=NSAppearanceNameDarkAqua \
		-Dapple.laf.useScreenMenuBar=true \
		-Dcom.apple.macos.useScreenMenuBar=true \
		-Djava.awt.headless=false \
		-Dsun.java2d.opengl=false \
		-classpath "RomRaider.jar:lib/common/*:${smoke_classes}" \
		com.romraider.build.Graph3dSmokeTest
)

/usr/libexec/PlistBuddy \
	-c "Set :CFBundleShortVersionString ${short_version}" \
	"${new_app}/Contents/Info.plist"
/usr/libexec/PlistBuddy \
	-c "Set :CFBundleVersion ${version_build}" \
	"${new_app}/Contents/Info.plist"
/usr/bin/plutil -lint "${new_app}/Contents/Info.plist" >/dev/null
[[ "$(/usr/libexec/PlistBuddy \
	-c 'Print :NSHighResolutionCapable' \
	"${new_app}/Contents/Info.plist")" == true ]] ||
	fail "NSHighResolutionCapable must be true"
[[ "$(/usr/libexec/PlistBuddy \
	-c 'Print :NSRequiresAquaSystemAppearance' \
	"${new_app}/Contents/Info.plist")" == false ]] ||
	fail "NSRequiresAquaSystemAppearance must be false"
[[ "$(/usr/libexec/PlistBuddy \
	-c 'Print :CFBundleName' \
	"${new_app}/Contents/Info.plist")" == RomRaiderHD ]] ||
	fail "CFBundleName must be RomRaiderHD"
[[ "$(/usr/libexec/PlistBuddy \
	-c 'Print :CFBundleExecutable' \
	"${new_app}/Contents/Info.plist")" == RomRaiderHD ]] ||
	fail "CFBundleExecutable must be RomRaiderHD"
[[ -f "${new_app}/Contents/Resources/RomRaiderHD.icns" ]] ||
	fail "RomRaiderHD app icon is missing"
/usr/bin/codesign --force --deep --sign - "${new_app}" >/dev/null
/usr/bin/codesign --verify --deep --strict "${new_app}"

require_romraider_stopped
install_parent="$(/usr/bin/dirname "${app_path}")"
/bin/mkdir -p "${install_parent}"
if [[ -e "${app_path}" ]]; then
	previous_app="${stage_dir}/previous.app"
	/bin/mv "${app_path}" "${previous_app}"
fi
/bin/mv "${new_app}" "${app_path}"
installed=true

printf 'Installed RomRaiderHD %s build %s at %s\n' \
	"${short_version}" "${version_build}" "${app_path}"

if "${launch_app}"; then
	/usr/bin/open "${app_path}"
	printf 'Launched %s\n' "${app_path}"
fi
