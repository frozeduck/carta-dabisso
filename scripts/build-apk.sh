#!/usr/bin/env bash
set -euo pipefail
project_dir="$(cd "$(dirname "$0")/.." && pwd)"
cd "$project_dir"
mkdir -p /tmp/.android
export ANDROID_USER_HOME=/tmp/.android

# Genera gli asset grafici Paper Mario PNG se non presenti
echo "Generazione e verifica sprite Paper Mario PNG..."
python3 scripts/generate_sprites.py

# Esegui la suite di test nativa Java
echo "Esecuzione test unitari modello nativo..."
javac android/app/src/main/java/eu/sabotino/cartadabisso/Game.java android/app/src/main/java/eu/sabotino/cartadabisso/GameTest.java
java -ea -cp android/app/src/main/java eu.sabotino.cartadabisso.GameTest
rm -f android/app/src/main/java/eu/sabotino/cartadabisso/*.class

# Verifica keystore di debug
if [ ! -f android/app/debug.keystore ]; then
    keytool -genkeypair -keystore android/app/debug.keystore \
        -storepass android -keypass android -alias androiddebugkey \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -dname "CN=Carta d'Abisso Debug,O=Sabotino,C=IT"
fi

# Compilazione APK nativo Android
echo "Compilazione APK nativo Android con Gradle..."
android/gradlew -p android --offline --no-daemon assembleDebug

version="$(grep "versionName" android/app/build.gradle | head -n1 | sed -E "s/.*['\"]([^'\"]+)['\"].*/\1/")"
if [ -z "$version" ]; then
    version="0.3.0"
fi
echo "Versione build: $version"

mkdir -p downloads
apk_target="downloads/carta-dabisso-${version}-debug.apk"
cp android/app/build/outputs/apk/debug/app-debug.apk "$apk_target"

# Verifica integrita APK e assenza vincoli web
python3 scripts/check-apk.py

sha256sum "$apk_target" > "${apk_target}.sha256"
cat "${apk_target}.sha256"
