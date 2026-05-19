#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BUILD_DIR="$ROOT_DIR/build/classes"
rm -rf "$ROOT_DIR/build"
mkdir -p "$BUILD_DIR"
find "$ROOT_DIR/src/main/java" "$ROOT_DIR/src/test/functional/java" -name '*.java' > "$ROOT_DIR/build/functional-sources.txt"
javac --release 21 -d "$BUILD_DIR" @"$ROOT_DIR/build/functional-sources.txt"
cp -R "$ROOT_DIR/src/main/resources/." "$BUILD_DIR/"
java -ea -cp "$BUILD_DIR" com.example.purchasefx.PurchaseApplicationFunctionalTest
