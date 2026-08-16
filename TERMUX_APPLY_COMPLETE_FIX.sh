#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
SOURCE_DIR="$(cd "$(dirname "$0")" && pwd)"
TARGET_DIR="${1:-$HOME/PathSathi}"
mkdir -p "$TARGET_DIR"
cp -a "$SOURCE_DIR"/. "$TARGET_DIR"/
cd "$TARGET_DIR"
chmod +x gradlew || true

echo "===== PATH SATHI FINAL SOURCE APPLY ====="
echo "Target: $TARGET_DIR"

echo "===== KOTLIN SOURCE FILES ====="
find app/src/main/java/com/pathsathi/app -type f | sort

echo "===== GIT STATUS BEFORE ====="
git status --short || true

git add app/src/main/java app/src/main/AndroidManifest.xml app/build.gradle.kts gradle.properties README.md .github/workflows/build.yml gradle/wrapper/gradle-wrapper.properties gradlew
if ! git diff --cached --quiet; then
  git commit -m "Fix Path Sathi functionality and GitHub build"
  echo "===== PUSHING TO GITHUB ====="
  git push origin main
else
  echo "No tracked source changes to commit."
  echo "===== PUSHING CURRENT MAIN ====="
  git push origin main
fi

echo
 echo "===== DONE ====="
echo "GitHub Actions should now start automatically."
echo "Open repository -> Actions -> Build Path Sathi APK."
echo "Do not run ./gradlew locally; GitHub Actions uses Gradle 8.2.2 directly."
