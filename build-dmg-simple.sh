#!/bin/bash

# SDS Incident Tracker - DMG Packaging Script
# Non-modular approach for JavaFX

set -e

echo "=== Building SDS Incident Tracker DMG ==="
echo ""

# Step 1: Clean and build
echo "[1/2] Building project..."
mvn clean package

# Step 2: Create DMG with all dependencies on classpath (non-modular)
echo "[2/2] Creating DMG..."
jpackage \
  --input target \
  --name "SDS Incident Tracker" \
  --main-jar incident-tracker-1.0-SNAPSHOT.jar \
  --main-class com.sdsweather.Launcher \
  --type dmg \
  --dest target \
  --app-version 1.0 \
  --vendor "SDS Weather" \
  --copyright "Copyright 2026 SDS Weather" \
  --mac-package-identifier com.sdsweather.incidenttracker \
  --java-options '-Dprism.order=sw' \
  --java-options '-Dprism.verbose=true'

echo ""
echo "✅ SUCCESS! DMG created at:"
echo "   target/SDS Incident Tracker-1.0.dmg"
echo ""
echo "📦 To install:"
echo "   1. Double-click the DMG"
echo "   2. Drag 'SDS Incident Tracker' to Applications"
echo "   3. If macOS blocks it:"
echo "      System Settings → Privacy & Security → 'Open Anyway'"
echo ""