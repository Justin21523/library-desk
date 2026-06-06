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

echo ">> Building the application image with jpackage"
jpackage \
  --type app-image \
  --name LibraDesk \
  --app-version "${APP_VERSION}" \
  --input "${INPUT}" \
  --main-jar "${MAIN_JAR}" \
  --main-class com.justin.libradesk.Launcher \
  --runtime-image "${RUNTIME}" \
  --java-options "-Dfile.encoding=UTF-8" \
  --dest "${DIST}/image"

echo ">> Done. Launch with: ${DIST}/image/LibraDesk/bin/LibraDesk"
