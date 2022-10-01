# RootlessJamesDSP

A re-implementation of the system-wide JamesDSP audio processing engine for non-rooted Android devices. **Currently work-in-progress.**

This app has several limitations that may be deal-breaking to some people; please read this whole document before using the app. See below for APK downloads.

## Limitations
* Apps blocking internal audio capture remain unprocessed (e.g., Spotify, Google Chrome)
* Apps using some types of HW-accelerated playback currently cause the audio processing service to stop itself (e.g., some Unity games)
* Cannot coexist with (some) other audio effect apps (e.g., Wavelet and other apps that make use of the `DynamicsProcessing` Android API)
* Noticeably increased audio latency 


Apps confirmed working:
* YouTube
* YouTube Music
* Amazon Music
* Deezer
* Poweramp
* Substreamer
* Twitch
* Spotify ReVanced **(Patch required)**
* ...

Unsupported apps include:
* Spotify (patch for Spotify exists)
* Google Chrome
* SoundCloud
* ...

Tested on:
* Samsung Galaxy S20+ (Android 12; OneUI 4.0)
* Stock AOSP emulator (Android 10-13)

## Spotify support patch
You can only use Spotify with this application if you patch the Spotify app.
The setup is very easy:
1. Download and install the [ReVanced manager APK](https://github.com/revanced/revanced-manager/releases/tag/v0.0.28) 
2. Install the unpatched Spotify app
3. Open ReVanced Manager, select Spotify and enable the `disable-capture-restriction` patch.
4. Start the patching process and install the patched APK once it is done.
5. You can now use Spotify with RootlessJamesDSP.

## Downloads
APK files are available for download here: https://github.com/ThePBone/RootlessJamesDSP/releases

