#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

if [[ ! -f "$ROOT_DIR/keystore/release-signing.properties" ]]; then
  echo "警告：未找到 Release 签名配置，无法生成与原 APK 同签名的升级包。" >&2
fi

if [[ -x "$ROOT_DIR/gradlew" ]]; then
  exec "$ROOT_DIR/gradlew" --no-daemon -p "$ROOT_DIR" :app:assembleRelease
fi

BUNDLED_GRADLE="$ROOT_DIR/.tools/gradle/gradle-8.11.1/bin/gradle"
if [[ -x "$BUNDLED_GRADLE" ]]; then
  export JAVA_HOME="$ROOT_DIR/.tools/jdk/Contents/Home"
  export ANDROID_HOME="$ROOT_DIR/.tools/android-sdk"
  export ANDROID_SDK_ROOT="$ANDROID_HOME"
  exec "$BUNDLED_GRADLE" --no-daemon -p "$ROOT_DIR" :app:assembleRelease
fi

if command -v gradle >/dev/null 2>&1; then
  exec gradle --no-daemon -p "$ROOT_DIR" :app:assembleRelease
fi

echo "未找到 Gradle。请安装 Gradle 8.11.1，配置 Gradle Wrapper，或使用 Android Studio 打开项目。" >&2
exit 1
