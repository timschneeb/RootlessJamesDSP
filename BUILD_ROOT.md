# Using root

If you want to use this app with the JamesDSP magisk module on a rooted device, follow these instructions:

**Important: I don't actively test root builds. They may be more unstable than the regular rootless version.**

1. Install the JamesDSP magisk module if not already done.
2. Uninstall the original JamesDSP app.
3. Download the latest APK from this release server: https://nightly.timschneeberger.me/rootlessjamesdsp-rootfull/
6. Install the APK.
7. Make sure to close and restart any music app that is currently active (or simply reboot your device).
8. Done. You should now be able to use the app normally.

Starting with version 1.3.2, the root build includes a self-updater. You can directly update the app without visiting this site again.

**This app behaves differently than the original app:**

* No separate configurations for speaker/bluetooth/usb/wired for now; there's one active category for all (I'm going to add device profiles later)
* Different preset file format (.tar); you can't import old presets
* `/sdcard/Android/data/james.dsp/` is used instead of `/sdcard/JamesDSP` due to scoped storage. Files in scoped storage are deleted when the app is uninstalled. You can use the new auto-backup feature if you'd like to keep a separate backup of all your IRS, DDC, Liveprog, and preset files.

**Updating: **

* Just install the update over the old version. The root builds include a self-updater, which will notify you when an update is available. You can manually trigger an update check by navigating to 'Settings > About > Check for updates'.

## Restoring the original app
Uninstall this app, download the original app APK: https://github.com/Magisk-Modules-Repo/ainur_jamesdsp/raw/master/JamesDSPManager.apk, and install it. A reboot may be necessary.
