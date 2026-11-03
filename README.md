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

## Glitch OS AI chat

The creator assistant identifies itself as **Glitch OS AI**, answers common
identity questions directly, and receives app feature context plus recent chat
messages when requesting a provider response. NoTrack may provide chat responses,
but it is a backend service—not the assistant's identity. Video creation is a
separate composer action; video options are available from the settings icon.

## Location testing tool

The **Location** tool opens a full-screen OpenStreetMap picker. Tap the map to
choose a point, use the location button to center on the device's current fix,
then select **Set location** to provide that point as an Android mock location.
If Developer options are not enabled, the setup button opens device information
so you can enable them by tapping **Build number** seven times. Then select
**Glitch OS** under **Developer options → Select mock location app**. Android
requires this manual selection and exposes mock locations as mock data; this
feature does not hide that status or bypass other apps' checks. Grant precise
location permission when Android prompts for it.

## Glitch Tube and recovery

**Glitch Tube** plays videos and music selected from the device. Audio continues
in the background using a playback notification. Downloads accept direct HTTPS
links to media files only, and the user must confirm they own the media or have
permission to download it. Video-platform page extraction (including yt-dlp)
is not included.

**Recover files** scans a raw disk image explicitly selected by the user and
writes PhotoRec matches to a separate output folder. Android does not allow this
app to scan protected internal storage directly. PhotoRec cannot always
distinguish deleted file signatures from files still present in the selected
image; the scan is read-only and does not change the source.

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
