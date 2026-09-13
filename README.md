# Sun Island

A Jetpack Compose Android app that calculates sunrise, solar noon, sunset, and solar midnight for a location and displays a customizable floating countdown pill.

## Highlights

- Jetpack Compose + Material 3 UI
- Android 16 / API 36 target
- Local NOAA-style solar calculations; no server required
- Approximate or precise location through Android's location permission model
- Floating overlay island with drag-to-position behavior
- Custom island width, height, radius, opacity, typography, layout, animation flags, and seconds display
- Sunrise, solar noon, sunset, solar midnight, or next-event selection
- Foreground service for the user-visible overlay
- Xiaomi/HyperOS setup guidance for Autostart and battery restrictions
- DataStore persistence
- Release build configured with R8/resource shrinking
- Unit tests for solar calculations

## Permissions by design

The app does **not** request Accessibility Service or Device Admin. Those permissions are unrelated to a solar countdown and are not used as an anti-kill mechanism.

The app requests only the permissions needed for its stated features: location, notifications, overlay, foreground-service operation, and boot notification/state restoration.

## Build

Open the project in Android Studio with JDK 17+ and an installed Android 16 (API 36) SDK. Sync Gradle, then run:

```bash
./gradlew testDebugUnitTest
./gradlew assembleRelease
```

The release APK is produced under `app/build/outputs/apk/release/`.

## Important platform behavior

Android 15+ and Android 16 place restrictions on starting foreground services from the background. Xiaomi/HyperOS may additionally impose vendor-specific background restrictions. The app intentionally uses supported Android mechanisms and guides the user through Xiaomi settings instead of attempting to bypass those controls.

## Package identity

The placeholder application ID is `com.sunisland`. Change it before publishing if you own another namespace.
