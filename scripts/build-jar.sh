#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BUILD_DIR="$ROOT_DIR/build/classes"
JAR_DIR="$ROOT_DIR/release"
JAR_FILE="$JAR_DIR/purchase-fx-service.jar"
rm -rf "$ROOT_DIR/build" "$JAR_DIR"
mkdir -p "$BUILD_DIR" "$JAR_DIR"
find "$ROOT_DIR/src/main/java" -name '*.java' > "$ROOT_DIR/build/main-sources.txt"
javac --release 21 -d "$BUILD_DIR" @"$ROOT_DIR/build/main-sources.txt"
cp -R "$ROOT_DIR/src/main/resources/." "$BUILD_DIR/"
cat > "$ROOT_DIR/build/manifest.mf" <<MANIFEST
Manifest-Version: 1.0
Main-Class: com.example.purchasefx.Application
MANIFEST
jar cfm "$JAR_FILE" "$ROOT_DIR/build/manifest.mf" -C "$BUILD_DIR" .
echo "Built $JAR_FILE"
