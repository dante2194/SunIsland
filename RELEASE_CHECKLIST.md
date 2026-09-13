# Release checklist

1. Change `applicationId` and package namespace from `com.sunisland` to your owned namespace.
2. Replace the placeholder app icon with your final artwork.
3. Add a production signing configuration outside source control (keystore/passwords must never be committed).
4. Build `assembleRelease` with JDK 17+ and Android SDK 36 installed.
5. Run `testDebugUnitTest` and test overlay behavior on Android 14, 15, and 16.
6. Test Xiaomi/HyperOS Autostart + battery/background settings on a physical device.
7. Review the foreground-service declaration and Play Console policy requirements before publishing.
