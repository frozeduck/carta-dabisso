#!/usr/bin/env bash
set -euo pipefail
project_dir="$(cd "$(dirname "$0")/.." && pwd)"
cd "$project_dir"
mkdir -p /tmp/.android
export ANDROID_USER_HOME=/tmp/.android
npm test
if [ ! -f android/app/debug.keystore ]; then
    keytool -genkeypair -keystore android/app/debug.keystore \
        -storepass android -keypass android -alias androiddebugkey \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -dname "CN=Carta d'Abisso Debug,O=Sabotino,C=IT"
fi
android/gradlew -p android --offline --no-daemon assembleDebug
mkdir -p downloads
cp android/app/build/outputs/apk/debug/app-debug.apk downloads/carta-dabisso-0.1.0-debug.apk
python3 scripts/check-apk.py
sha256sum downloads/carta-dabisso-0.1.0-debug.apk > downloads/carta-dabisso-0.1.0-debug.apk.sha256
cat downloads/carta-dabisso-0.1.0-debug.apk.sha256
