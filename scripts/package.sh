#!/usr/bin/env bash
#
# Builds a self-contained native application image for LibraDesk using jlink
# (a slimmed JDK runtime) + jpackage. JavaFX and the other dependencies run from
# the classpath, so the jlink runtime only needs JDK modules.
#
# Requires: JDK 21 (jlink + jpackage on PATH), Maven, and a desktop OS toolchain.
# Output: target/jpackage/image/LibraDesk (run target/jpackage/image/LibraDesk/bin/LibraDesk).
#
set -euo pipefail
cd "$(dirname "$0")/.."

APP_VERSION="0.1.0"
MAIN_JAR="libradesk-${APP_VERSION}.jar"
DIST="target/jpackage"
INPUT="${DIST}/input"
RUNTIME="${DIST}/runtime"

echo ">> Building application jar and collecting dependencies"
mvn -q clean package -DskipTests
rm -rf "${DIST}"
mkdir -p "${INPUT}"
cp "target/${MAIN_JAR}" "${INPUT}/"
mvn -q dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory="${INPUT}"

echo ">> Creating a slim runtime with jlink"
jlink \
  --add-modules java.base,java.desktop,java.logging,java.naming,java.sql,java.management,java.xml,java.scripting,jdk.unsupported,jdk.crypto.ec \
  --strip-debug --no-header-files --no-man-pages --compress=zip-6 \
  --output "${RUNTIME}"

VENDOR="LibraDesk"
ICON_OPT=()
if [[ -f "scripts/libradesk.png" ]]; then
  ICON_OPT=(--icon "scripts/libradesk.png")
fi

echo ">> Building the application image with jpackage"
jpackage \
  --type app-image \
  --name LibraDesk \
  --app-version "${APP_VERSION}" \
  --vendor "${VENDOR}" \
  "${ICON_OPT[@]}" \
  --input "${INPUT}" \
  --main-jar "${MAIN_JAR}" \
  --main-class com.justin.libradesk.Launcher \
  --runtime-image "${RUNTIME}" \
  --java-options "-Dfile.encoding=UTF-8" \
  --dest "${DIST}/image"
echo ">> App image: ${DIST}/image/LibraDesk (run bin/LibraDesk)"

# Optional native installer. A .deb needs dpkg-deb + fakeroot; .rpm needs rpmbuild.
if command -v dpkg-deb >/dev/null 2>&1 && command -v fakeroot >/dev/null 2>&1; then
  echo ">> Building .deb installer"
  jpackage \
    --type deb \
    --name LibraDesk \
    --app-version "${APP_VERSION}" \
    --vendor "${VENDOR}" \
    "${ICON_OPT[@]}" \
    --input "${INPUT}" \
    --main-jar "${MAIN_JAR}" \
    --main-class com.justin.libradesk.Launcher \
    --runtime-image "${RUNTIME}" \
    --java-options "-Dfile.encoding=UTF-8" \
    --dest "${DIST}/installer"
  echo ">> Installer written to ${DIST}/installer"
else
  echo ">> Skipping .deb installer (dpkg-deb/fakeroot not found). The app image above is ready to run."
fi
