<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Glitch OS

Android app built with Kotlin and Jetpack Compose.

## Requirements

- JDK 17 or newer
- Android SDK with Android API 36 / extension level 36.1 and Build Tools 36.0.0
- Android platform tools for installing APKs with `adb`

Open the repository root in Android Studio, or install the SDK components from the command line:

```sh
sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0" "cmake;3.22.1" "ndk;28.2.13676358"
```

Set `ANDROID_HOME` or `ANDROID_SDK_ROOT` to the SDK directory. Android Studio can also create the ignored `local.properties` file automatically.

## Build a debug APK

The checked-in Gradle Wrapper downloads and runs the pinned Gradle version; a system Gradle installation is not needed.

```sh
./gradlew assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Location testing tool

The **Location** tool lets app developers and QA testers select a point on an
OpenStreetMap map or enter coordinates, then explicitly set or stop an Android
mock location. Grant precise location permission and manually select **Glitch OS**
under **Developer options → Select mock location app**. Android exposes mock
locations as mock data; this feature does not hide that status or bypass other
apps' checks.

Map tiles come from the public OpenStreetMap tile service and include visible
attribution. Its tile service is best-effort, not an unlimited or SLA-backed API.
See the [OpenStreetMap tile usage policy](https://operations.osmfoundation.org/policies/tiles/).
The open-source [GPS Locator](https://github.com/0xfnzero/gps-locator) project
was consulted as a design reference; its code is not included in this app.

Install on a connected device:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Run unit tests:

```sh
./gradlew testDebugUnitTest
```

Build and test together:

```sh
./gradlew testDebugUnitTest assembleDebug
```

The debug APK is signed with Android's standard local debug key. The app does not require sign-in, Firebase configuration, or a stored API key. Video generation uses the configured video service or the app's available on-device tools.

## Build a release APK

For a signed release APK, provide a private upload keystore and credentials through environment variables. Do not commit the keystore or passwords:

```sh
export KEYSTORE_PATH=/secure/path/upload-key.jks
export STORE_PASSWORD='...'
export KEY_ALIAS='upload'
export KEY_PASSWORD='...'
./gradlew assembleRelease
```

Without all four values, Gradle builds an **unsigned** release APK that cannot be installed or distributed until it is signed. Release output is under `app/build/outputs/apk/release/`.

## Continuous integration

GitHub Actions runs unit tests and builds both debug and release APKs on pushes, pull requests, and manual dispatch. It uploads separate debug and release artifacts. The release artifact is unsigned unless signing secrets are configured.
