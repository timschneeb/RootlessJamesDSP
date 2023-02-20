# Using root

If you want to use this app with the JamesDSP magisk module on a rooted device, follow these instructions:

**Important: I don't actively test root builds; therefore, I won't provide stable APK files. They may be more unstable than the regular rootless version.**

1. Install the JamesDSP magisk module if not already done.
2. Uninstall the original JamesDSP app.
3. Select the latest successful workflow run on this page: https://github.com/ThePBone/RootlessJamesDSP/actions/workflows/build.yml
4. On the next page, scroll down to the artifacts section and download the prebuilt root APK artifact. **You need to be logged in with a GitHub account** to download it, otherwise the link won't be clickable.
5. Install the APK.
6. Make sure to close and restart any music app that is currently active (or simply reboot your device).
7. Done. You should now be able to use the app normally.

   

**This app behaves differently than the original app:**

* Different preset file format (.tar); you can't import old presets
* `/sdcard/Android/data/james.dsp/` is used instead of `/sdcard/JamesDSP` due to scoped storage
* No separate configurations for speaker/bluetooth/usb/wired; there's one active category for all

## Restoring the original app
Uninstall this app, download the original app APK: https://github.com/Magisk-Modules-Repo/ainur_jamesdsp/raw/master/JamesDSPManager.apk, and install it.
