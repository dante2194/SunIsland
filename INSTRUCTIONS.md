# Sun Island — Build & Test Instructions

Everything you need to build, install, and test the latest features
(transparency slider, Apple-notch layout, lock-screen accessibility overlay).

## What's new in this version

1. **Opacity / Transparency slider** — In the *Island* tab, set pill background
   transparency from 20% (nearly transparent) to 100% (solid).
2. **Apple Notch 2021 layout** — New `NOTCH` option in the *Layout* pill choices.
   Renders like the MacBook Pro 2021 camera notch: a black pill with a centered
   camera dot. It is **pinned to the top center of the screen and cannot be
   dragged**.
3. **Accessibility overlay (lock screen)** — Optional way to draw the island
   **above the lock screen** on modern Android (incl. Xiaomi HyperOS 2). Uses a
   minimal accessibility service that reads no screen content. Off by default.

## Requirements

- JDK 17+
- Android Studio (latest stable recommended)
- Android 16 / API 36 SDK platform installed
- A device running Android 8+ (Android 15/16 = HyperOS 2 recommended)

## Build (debug APK)

1. Open the project folder in Android Studio.
2. Wait for the Gradle sync to finish.
3. Run the app on your phone:
   ```
   Run > Run 'app'
   ```
   or build a debug APK:
   ```
   Build > Build APK(s)
   ```
   APK output: `app/build/outputs/apk/debug/app-debug.apk`

## First-run setup

1. Grant **Location** permission when prompted (or use a manual location).
2. Grant **Display over other apps** (Overlay) permission when prompted.
3. On **Xiaomi / HyperOS** only:
   - Settings → Apps → Sun Island → Autostart → **On**
   - Battery saver → restrict rules → allow background running
4. Go to the **Home** tab → **Start Island**. The pill appears.
5. Drag the pill to reposition (except in `NOTCH` layout — it stays pinned).

## Testing the new features

### Transparency
1. Open the **Island** tab.
2. Drag the **Opacity** slider to ~40%.
3. The pill (and preview) becomes see-through while keeping the text readable.

### Notch layout
1. Open the **Island** tab.
2. Tap **Layout → NOTCH**.
3. The island becomes a black notch pill with a centered camera dot at the very
   top of the screen.
4. Verify it **cannot be dragged** — touching it does nothing (events pass
   through to the app behind it).

### Lock-screen overlay (accessibility)
1. Open the **Settings** tab.
2. Enable **Draw over lock screen**.
3. Tap **Open accessibility settings**.
4. Find **Sun Island lock-screen overlay** and turn it **On** (confirm the
   system warning).
5. Back in the app, the status should read **Connected**. The island now uses
   the accessibility window type and should appear **on the lock screen**.
6. Test both **swipe-only** and **PIN/pattern/biometric** lock screens.
7. To turn it off: disable the toggle in the app, or disable the service in
   system accessibility settings. The app falls back to the normal overlay.

## Notes & known behavior

- The accessibility service reads **no screen content** and performs **no
  gestures**; it exists only so Android allows the overlay above the lock
  screen.
- If `Draw over lock screen` is on but the service is **Not connected**, the
  island uses the normal overlay window (no lock-screen drawing) until you
  enable the service in system settings.
- Notch positioning: while `NOTCH` is selected, saved drag coordinates are
  ignored (forced to top center). Switching back to another layout restores
  your last dragged position.
- Secure lock screens may still hide overlays on some ROMs — the accessibility
  method works on most devices incl. HyperOS 2, but test on your specific phone.

## Troubleshooting

| Problem | Fix |
|---|---|
| Island doesn't appear | Grant **Display over other apps**, then press **Start Island** again |
| Not visible on lock screen | Enable the accessibility service (Settings → Accessibility → Sun Island lock-screen overlay) and check the status says **Connected** |
| Island disappears after reboot | Enable **Remember island after reboot** and HyperOS **Autostart** |
| HyperOS kills the app | Allow unrestricted **Battery** usage + Autostart in HyperOS settings |
| Notch moves when dragged | Not possible — `NOTCH` is always pinned and passes touches through |

## Tests

```bash
./gradlew testDebugUnitTest
```

Run this after opening the project in Android Studio (Terminal tab) or from the
project root with a configured SDK.
