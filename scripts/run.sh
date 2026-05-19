#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BUILD_DIR="$ROOT_DIR/build/classes"
mkdir -p "$BUILD_DIR"
find "$ROOT_DIR/src/main/java" -name '*.java' > "$ROOT_DIR/build/main-sources.txt"
javac --release 21 -d "$BUILD_DIR" @"$ROOT_DIR/build/main-sources.txt"
cp -R "$ROOT_DIR/src/main/resources/." "$BUILD_DIR/"
exec java -cp "$BUILD_DIR" com.example.purchasefx.Application
