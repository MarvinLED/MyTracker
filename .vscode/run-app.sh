#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR/.."
ADB="/home/user/Android/Sdk/platform-tools/adb"
APP_ID="com.example.prokject2_tracker"
ACTIVITY=".MainActivity"

echo "Waiting for emulator..."
"$ADB" wait-for-device

until [ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do
    sleep 1
done

echo "Emulator ready. Building and installing debug APK..."
"$PROJECT_ROOT/gradlew" -p "$PROJECT_ROOT" installDebug

echo "Launching $APP_ID..."
"$ADB" shell am start -n "$APP_ID/$ACTIVITY"
