# RootlessJamesDSP

A re-implementation of the system-wide JamesDSP audio processing engine for non-rooted Android devices. **Currently work-in-progress.**

This app has several limitations that may be deal-breaking to some people; please read this whole document before using the app.

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
* ...

Unsupported apps include:
* Spotify
* Google Chrome
* SoundCloud
* ...

Tested on:
* Samsung Galaxy S20+ (Android 12; OneUI 4.0)
* Stock AOSP emulator (Android 10-13)

## Downloads
APK files are available for download here: https://github.com/ThePBone/RootlessJamesDSP/releases

