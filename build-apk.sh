#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
# Use a locally installed Gradle 8.9 and Android SDK; no secrets or proxy changes.
command -v javac >/dev/null || { echo 'JDK 17+ required (javac missing)' >&2; exit 1; }
command -v gradle >/dev/null || { echo 'Gradle 8.9 required' >&2; exit 1; }
: "${ANDROID_HOME:?Set ANDROID_HOME to Android SDK with platform 35 and build tools 35.0.0}"
gradle --no-daemon assembleDebug lintDebug
printf 'APK: app/build/outputs/apk/debug/app-debug.apk\n'
