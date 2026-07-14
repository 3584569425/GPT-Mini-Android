#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
export JAVA_HOME="$ROOT_DIR/.tools/jdk/Contents/Home"
export ANDROID_HOME="$ROOT_DIR/.tools/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"

"$ROOT_DIR/.tools/gradle/gradle-8.11.1/bin/gradle" --no-daemon -p "$ROOT_DIR" assembleRelease
