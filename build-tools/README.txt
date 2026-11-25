BUILD-TOOLS - builds the app contained in "src" (Windows, Linux, macOS).

Run from the main folder: Build-Notium-android.bat (Windows), .sh (Linux), .command (macOS). Nothing else is needed.
First run: internet is required; everything is downloaded into build-tools/tools (JDK, Android SDK, Gradle, libraries: about 3 GB).
Afterwards: if you keep build-tools/tools, builds also work OFFLINE.
If you already have a tools folder from a previous version, copy it into build-tools/.
On a compilation error, the "e:" lines are shown in the terminal and in build-tools/build.log.
The APK is created in Notium-android-APK, next to the main folder.
