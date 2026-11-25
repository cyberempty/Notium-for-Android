#!/usr/bin/env bash
# macOS (double-click in Finder): builds the app and creates Notium-android-APK/Notium.apk (next to this folder).
cd "$(dirname "$0")" || exit 1
clear
bash ./build-tools/build-engine.sh
code=$?
echo
read -r -n 1 -s -p "Press any key to close..." _
echo
exit $code
