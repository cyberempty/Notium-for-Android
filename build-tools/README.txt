BUILD-TOOLS - builds the app contained in "src".
Run Build-Notium-android from the main folder (.bat Windows, .sh Linux, .command macOS).
The first build downloads JDK 17, the Android SDK, Gradle and the libraries into build-tools/tools (about 3 GB, internet needed).
Later builds reuse them and also work without internet while that folder is kept.
The APK is created in Notium-android-APK, next to the Notium-android folder.
On a compilation error, the "e:" lines are shown in the terminal and in build-tools/build.log.
The local signing key is in build-tools/signing: keep it to update the app without uninstalling it.
