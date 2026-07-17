#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
TOOLS_DIR="${GPTMINI_TOOLS_DIR:-$ROOT_DIR/../GPTMini-GeckoView-v1.25.1/.tools}"

export JAVA_HOME="$TOOLS_DIR/jdk/Contents/Home"
export ANDROID_HOME="$TOOLS_DIR/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"

if [[ ! -f "$ROOT_DIR/local.properties" ]]; then
  echo "sdk.dir=$ANDROID_HOME" > "$ROOT_DIR/local.properties"
fi

"$TOOLS_DIR/gradle/gradle-8.10.2/bin/gradle" --no-daemon -p "$ROOT_DIR" assembleRelease
