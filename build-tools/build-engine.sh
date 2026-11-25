#!/usr/bin/env bash
# ==============================================================================
#  NOTIUM BUILD SYSTEM  (Linux / macOS)
#  Builds "src" and creates Notium.apk in the Notium-android-APK folder.
#  Portable: everything needed is downloaded into build-tools/tools
# ==============================================================================
set -u
set -o pipefail

APP_VERSION="1.0.0"
BUILDER="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MAIN="$(dirname "$BUILDER")"
PDIR="$MAIN/src"
TOOLS="$BUILDER/tools"
JDK="$TOOLS/jdk"
SDK="$TOOLS/android-sdk"
KS="$BUILDER/signing/notium.keystore"
LOG="$BUILDER/build.log"
MARK="$TOOLS/.pronto-release"
EST="$TOOLS/.task-estimate"
OUTDIR="$(dirname "$MAIN")/Notium-android-APK"
OUT="$OUTDIR/Notium.apk"
GLOG="$TOOLS/.gradle-run.log"

# ---- user interface
if [ -t 1 ]; then
  TTY=1; R=$'\033[0m'; DIM=$'\033[90m'; BOLD=$'\033[1m'; RED=$'\033[91m'; GRN=$'\033[92m'; YEL=$'\033[93m'; WHT=$'\033[97m'
else
  TTY=0; R=""; DIM=""; BOLD=""; RED=""; GRN=""; YEL=""; WHT=""
fi
T0=$(date +%s)
PHASES=6
W=(3 10 4 15 60 8)
B=(0 3 13 17 32 92)
PHASE_NAMES=("Checking environment" "JDK 17 (Temurin)" "Android command-line tools" "Android SDK 35 and build-tools" "Compiling and optimizing (R8)" "Finalizing")
PHASE=0
PCT=0

elapsed() { local s=$(( $(date +%s) - T0 )); printf '%02d:%02d' $((s/60)) $((s%60)); }
logf() { printf '%s  %-5s  %s\n' "$(elapsed)" "$1" "$2" >> "$LOG"; }

bar() {
  # bar <percent 0-100> <text>
  local p=$1 txt=$2 n=$(( $1 * 40 / 100 )) i filled="" empty=""
  for ((i=0;i<n;i++)); do filled+="█"; done
  for ((i=n;i<40;i++)); do empty+="░"; done
  if [ "$TTY" = 1 ]; then
    printf '\r\033[K  %s%s%s%s%s %3d%%  %s%s%s' "$WHT" "$filled" "$DIM" "$empty" "$R" "$p" "$DIM" "${txt:0:48}" "$R"
  fi
}

say() { # say <tag> <color> <text>
  if [ "$TTY" = 1 ]; then printf '\r\033[K'; fi
  printf '  %s%s%s  %s%-5s%s  %s\n' "$DIM" "$(elapsed)" "$R" "$2" "$1" "$R" "$3"
  logf "$1" "$3"
}
info() { say INFO "$DIM" "$1"; }
ok()   { say OK "$GRN" "$1"; }
warn() { say WARN "$YEL" "$1"; }

phase() {
  PHASE=$1
  PCT=${B[$((PHASE-1))]}
  if [ "$TTY" = 1 ]; then printf '\r\033[K'; fi
  printf '\n  %s%sPhase %d/%d  %s%s\n' "$BOLD" "$WHT" "$PHASE" "$PHASES" "${PHASE_NAMES[$((PHASE-1))]}" "$R"
  logf STEP "Phase $PHASE/$PHASES  ${PHASE_NAMES[$((PHASE-1))]}"
  bar "$PCT" "${PHASE_NAMES[$((PHASE-1))]}"
}
frac() { # frac <0..100 of the phase>
  PCT=$(( ${B[$((PHASE-1))]} + ${W[$((PHASE-1))]} * $1 / 100 ))
}
endphase() { PCT=$(( ${B[$((PHASE-1))]} + ${W[$((PHASE-1))]} )); bar "$PCT" "Phase $PHASE complete"; }

fail() {
  local msg=$1
  if [ "$TTY" = 1 ]; then printf '\r\033[K'; fi
  logf ERROR "$msg"
  local line; line=$(printf '─%.0s' $(seq 1 64))
  printf '\n %s%s%s\n  %sBUILD FAILED%s   (phase %d/%d - %s)\n %s%s%s\n  %s\n' \
    "$DIM" "$line" "$R" "$RED$BOLD" "$R" "$PHASE" "$PHASES" "${PHASE_NAMES[$((PHASE>0?PHASE-1:0))]}" "$DIM" "$line" "$R" "$msg"
  if [ -s "$GLOG" ]; then
    local errs; errs=$(grep -E '^e: ' "$GLOG" | sed -e "s#file://##" | head -12)
    if [ -n "$errs" ]; then printf '\n  Compilation errors:\n'; printf '%s\n' "$errs" | sed 's/^/    /'; fi
    local wrong; wrong=$(awk '/^\* What went wrong:/{f=1;next} /^\* Try:/{f=0} f&&NF' "$GLOG" | head -8)
    if [ -n "$wrong" ]; then printf '\n  Gradle:\n'; printf '%s\n' "$wrong" | sed 's/^/    /'; fi
  fi
  printf '\n  Full log: %s\n\n' "$LOG"
  exit 1
}

need() { command -v "$1" >/dev/null 2>&1; }

fetch() { # fetch <url> <destination> <name>
  info "Download $3"
  local i=0
  while [ $i -lt 3 ]; do
    if need curl; then
      if [ "$TTY" = 1 ]; then printf '\r\033[K'; curl -fL --retry 2 --progress-bar -o "$2" "$1" 2>&1 | tr '\r' '\n' | tail -n 0; curl_rc=${PIPESTATUS[0]}; fi
      if [ "$TTY" = 1 ]; then :; else curl -fsSL --retry 2 -o "$2" "$1"; curl_rc=$?; fi
      [ "${curl_rc:-1}" = 0 ] && return 0
    elif need wget; then
      wget -q -O "$2" "$1" && return 0
    else
      fail "curl or wget is required to download the components."
    fi
    i=$((i+1)); rm -f "$2"; warn "Download failed, retrying ($i/3)"
  done
  fail "Download failed: $3"
}

unzip_to() { # unzip_to <zip> <folder>
  mkdir -p "$2"
  if need unzip; then unzip -q -o "$1" -d "$2"; else (cd "$2" && "$JAVA_HOME/bin/jar" xf "$1"); fi
}

: > "$LOG"
clear_screen() { [ "$TTY" = 1 ] && printf '\033[2J\033[H'; }
clear_screen
printf '\n %s%s NOTIUM  BUILD SYSTEM%s\n %s%s%s\n' "$BOLD" "$WHT" "$R" "$DIM" "$(printf '─%.0s' $(seq 1 64))" "$R"

# ---- Phase 1
phase 1
case "$(uname -s)" in
  Linux)  OS=linux; ADOPT=linux ;;
  Darwin) OS=mac;   ADOPT=mac ;;
  *) fail "Unsupported operating system: $(uname -s)" ;;
esac
case "$(uname -m)" in
  x86_64|amd64) ARCH=x64 ;;
  aarch64|arm64) ARCH=aarch64 ;;
  *) fail "Unsupported architecture: $(uname -m)" ;;
esac
if [ "$OS" = linux ] && [ "$ARCH" != x64 ]; then
  fail "On Linux only x86-64 processors are supported (the Android tools do not exist for Linux ARM)."
fi
need tar || fail "The tar command is required."
[ -f "$PDIR/gradlew" ] || fail "Folder \"src\" is missing or incomplete: $PDIR"
chmod +x "$PDIR/gradlew" 2>/dev/null
mkdir -p "$TOOLS" "$BUILDER/signing"
OFFLINE=0
[ -f "$MARK" ] && OFFLINE=1
info "Project       $PDIR"
info "Components    $TOOLS"
if [ "$OFFLINE" = 1 ]; then ok "Mode          Offline (components already present)"; else info "Mode          Online (first build: downloading components)"; fi
info "System        $OS / $ARCH"
endphase

# ---- Phase 2: JDK
phase 2
jhome() { if [ -x "$JDK/Contents/Home/bin/java" ]; then echo "$JDK/Contents/Home"; else echo "$JDK"; fi; }
if [ ! -x "$(jhome)/bin/java" ]; then
  rm -rf "$JDK" "$TOOLS/jdk.tar.gz"
  fetch "https://api.adoptium.net/v3/binary/latest/17/ga/$ADOPT/$ARCH/jdk/hotspot/normal/eclipse" "$TOOLS/jdk.tar.gz" "Eclipse Temurin JDK 17 ($ARCH)"
  frac 60; bar "$PCT" "Installing JDK"
  mkdir -p "$JDK"
  tar -xzf "$TOOLS/jdk.tar.gz" -C "$JDK" --strip-components=1 || fail "JDK extraction failed"
  rm -f "$TOOLS/jdk.tar.gz"
  [ -x "$(jhome)/bin/java" ] || fail "Invalid JDK after extraction"
  ok "JDK 17 installed"
else
  ok "JDK 17 already present"
fi
export JAVA_HOME="$(jhome)"
export PATH="$JAVA_HOME/bin:$PATH"
endphase

# ---- Phase 3: cmdline-tools
phase 3
SDKMANAGER="$SDK/cmdline-tools/latest/bin/sdkmanager"
if [ ! -x "$SDKMANAGER" ]; then
  rm -rf "$SDK/cmdline-tools" "$TOOLS/cmd_tmp"
  fetch "https://dl.google.com/android/repository/commandlinetools-${OS}-11076708_latest.zip" "$TOOLS/cmdline-tools.zip" "Android command-line tools 11076708"
  unzip_to "$TOOLS/cmdline-tools.zip" "$TOOLS/cmd_tmp"
  mkdir -p "$SDK/cmdline-tools"
  mv "$TOOLS/cmd_tmp/cmdline-tools" "$SDK/cmdline-tools/latest" || fail "Command-line tools extraction failed"
  chmod +x "$SDK"/cmdline-tools/latest/bin/* 2>/dev/null
  rm -rf "$TOOLS/cmd_tmp" "$TOOLS/cmdline-tools.zip"
  ok "Command-line tools installed"
else
  ok "Command-line tools already present"
fi
export ANDROID_HOME="$SDK" ANDROID_SDK_ROOT="$SDK"
endphase

# ---- Phase 4: SDK
phase 4
if [ ! -d "$SDK/platforms/android-35" ] || [ ! -d "$SDK/build-tools/34.0.0" ]; then
  info "Accepting Android SDK licenses"
  mkdir -p "$SDK/licenses"
  printf '\n24333f8a63b6825ea9c5514f83c2829b004d1fee\n84831b9409646a918e30573bab4c9c91346d8abd\nd56f5187479451eabf01fb78af6dfcb131a6481e\n' > "$SDK/licenses/android-sdk-license"
  printf '\n84831b9409646a918e30573bab4c9c91346d8abd\n504667f4c0de7af1a06de9f4b1727b84351f2910\n' > "$SDK/licenses/android-sdk-preview-license"
  info "Installing platforms;android-35 and build-tools;34.0.0"
  frac 20; bar "$PCT" "Download Android SDK"
  "$SDKMANAGER" --sdk_root="$SDK" "platforms;android-35" "build-tools;34.0.0" >> "$LOG" 2>&1 </dev/null
  if [ ! -d "$SDK/platforms/android-35" ] || [ ! -d "$SDK/build-tools/34.0.0" ]; then
    fail "SDK components not installed: license not accepted or incomplete download. Try again."
  fi
  ok "Android SDK installed"
else
  ok "Android SDK already present"
fi
printf 'sdk.dir=%s\n' "$SDK" > "$PDIR/local.properties"
endphase

# ---- local signing
if [ ! -f "$KS" ]; then
  info "Creating the local signing key (one time only)"
  keytool -genkeypair -keystore "$KS" -storetype PKCS12 -alias notium -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass notium-local -keypass notium-local -dname "CN=Notium, OU=Local, O=Notium, C=IT" >> "$LOG" 2>&1
  [ -f "$KS" ] || fail "Signing key creation failed"
fi
export NOTIUM_KEYSTORE="$KS" NOTIUM_STORE_PASSWORD="notium-local" NOTIUM_KEY_ALIAS="notium" NOTIUM_KEY_PASSWORD="notium-local"

# ---- Phase 5: Gradle
phase 5
export GRADLE_USER_HOME="$TOOLS/gradle-home"
ESTIMATE=60
if [ -f "$EST" ]; then v=$(tr -dc '0-9' < "$EST"); [ -n "$v" ] && [ "$v" -gt 20 ] && ESTIMATE=$v; fi

run_gradle() { # run_gradle [--offline]
  : > "$GLOG"
  local n=0
  (cd "$PDIR" && ./gradlew assembleRelease --parallel --build-cache --console=plain "$@" 2>&1) | tee -a "$GLOG" | while IFS= read -r l; do
    printf '%s\n' "$l" >> "$LOG"
    case "$l" in
      "> Task "*)
        n=$((n+1))
        f=$(( n * 100 / ESTIMATE )); [ "$f" -gt 97 ] && f=97
        p=$(( ${B[4]} + ${W[4]} * f / 100 ))
        t="${l#> Task }"; t="${t%% *}"
        bar "$p" "Gradle  ${t}  ($n tasks)"
        ;;
    esac
  done
  return "${PIPESTATUS[0]}"
}

if [ "$OFFLINE" = 1 ]; then info "Gradle in offline mode"; else info "Gradle: downloading Gradle and libraries on first run"; fi
info "Starting release build (optimized with R8)"
if [ "$OFFLINE" = 1 ]; then run_gradle --offline; code=$?; else run_gradle; code=$?; fi
if [ "$code" -ne 0 ] && [ "$OFFLINE" = 1 ] && ! grep -q '^e: ' "$GLOG"; then
  warn "Components missing from the local cache: retrying with network access"
  run_gradle; code=$?
fi
[ "$code" -eq 0 ] || fail "Build failed (code $code)"
TASKS=$(grep -c '^> Task ' "$GLOG")
WARNS=$(grep -c '^w: ' "$GLOG")
ok "Build finished  ($TASKS tasks, $WARNS compiler warnings)"
echo ok > "$MARK"
[ "$TASKS" -gt 20 ] && echo "$TASKS" > "$EST"
endphase

# ---- Phase 6
phase 6
APK=$(find "$PDIR/app/build/outputs/apk/release" -name '*.apk' ! -name '*unaligned*' 2>/dev/null | head -n 1)
[ -n "$APK" ] || fail "APK not found in the Gradle output folder"
mkdir -p "$OUTDIR" && cp -f "$APK" "$OUT" || fail "Copying the APK failed: $OUT"
if need sha256sum; then SHA=$(sha256sum "$OUT" | cut -d' ' -f1); else SHA=$(shasum -a 256 "$OUT" | cut -d' ' -f1); fi
SIZE=$(wc -c < "$OUT" | tr -d ' ')
info "Stopping the Gradle daemon and removing temporary files"
(cd "$PDIR" && ./gradlew --stop >/dev/null 2>&1)
rm -rf "$PDIR/app/build" "$PDIR/build" "$PDIR/.gradle" "$PDIR/.kotlin" "$PDIR/local.properties" "$GLOG"
PCT=100; bar 100 "Done"
ok "Build completed successfully"

LINE=$(printf '─%.0s' $(seq 1 64))
printf '\n %s%s%s\n  %sBUILD COMPLETE%s\n %s%s%s\n' "$DIM" "$LINE" "$R" "$GRN$BOLD" "$R" "$DIM" "$LINE" "$R"
printf '  Output       %s\n' "$OUT"
printf '  Size         %s MB\n' "$(awk -v s="$SIZE" 'BEGIN{printf "%.1f", s/1048576}')"
printf '  SHA-256      %s\n' "$SHA"
printf '  Version      %s\n' "$APP_VERSION"
printf '  Duration     %s\n' "$(elapsed)"
printf '  Signing      local key in build-tools/signing\n'
printf ' %s%s%s\n\n' "$DIM" "$LINE" "$R"
printf '  Copy Notium.apk to your phone and open it. If a version signed with a different key is already installed,\n'
printf '  uninstall it first (Android does not allow overwriting an app signed with a different key).\n\n'
exit 0
