#!/bin/bash

# RomRaiderHD launcher for the standalone Linux package.
#
# Usage: ./run.sh [editor|logger|logger.fullscreen|logger.touch]
#
# A 64-bit Java 17 or newer runtime is required. It is taken from
# ROMRAIDER_JAVA_HOME, then JAVA_HOME, then PATH.

set -e

cd "$(dirname "$0")"

app_jar="RomRaiderHD.jar"
log_dir="${HOME}/.RomRaider"
log_file="${log_dir}/romraider_sout.log"

fail() {
	printf 'error: %s\n' "$*" >&2
	exit 1
}

app_args=()
case "${1:-editor}" in
	editor) ;;
	logger) app_args=(-logger) ;;
	logger.fullscreen) app_args=(-logger.fullscreen) ;;
	logger.touch) app_args=(-logger.touch) ;;
	*)
		printf 'Usage: %s [editor|logger|logger.fullscreen|logger.touch]\n' \
			"$0" >&2
		exit 1
		;;
esac

[[ -f "${app_jar}" ]] ||
	fail "${app_jar} is missing from $(pwd); run this script from the extracted RomRaiderHD package"

if [[ -n "${ROMRAIDER_JAVA_HOME:-}" ]]; then
	java_bin="${ROMRAIDER_JAVA_HOME}/bin/java"
elif [[ -n "${JAVA_HOME:-}" ]]; then
	java_bin="${JAVA_HOME}/bin/java"
else
	java_bin="$(command -v java || true)"
fi

[[ -n "${java_bin}" && -x "${java_bin}" ]] ||
	fail "no Java runtime found; install a 64-bit Java 17 or newer, or set ROMRAIDER_JAVA_HOME"

java_properties="$("${java_bin}" -XshowSettings:properties -version 2>&1)"
java_spec="$(awk '$1 == "java.specification.version" { print $3; exit }' \
	<<<"${java_properties}")"
java_bits="$(awk '$1 == "sun.arch.data.model" { print $3; exit }' \
	<<<"${java_properties}")"

[[ -n "${java_spec}" ]] ||
	fail "unable to determine the version of ${java_bin}"
java_major="${java_spec%%.*}"
((java_major >= 17)) ||
	fail "Java ${java_spec} at ${java_bin} is too old; RomRaiderHD requires Java 17 or newer"
[[ "${java_bits}" == "64" ]] ||
	fail "Java at ${java_bin} is ${java_bits}-bit; RomRaiderHD requires a 64-bit runtime"

mkdir -p "${log_dir}"

exec "${java_bin}" \
	-Djava.library.path=lib/linux/64 \
	-Dawt.useSystemAAFontSettings=lcd \
	-Dswing.aatext=true \
	-Dsun.java2d.d3d=false \
	-Xms64M \
	-Xmx512M \
	-XX:-UseParallelGC \
	-XX:CompileThreshold=10000 \
	-jar "${app_jar}" \
	"${app_args[@]}" >>"${log_file}" 2>&1
