<h1 align="center">
  <img alt="Icon" width="75" src="https://github.com/thepbone/RootlessJamesDSP/blob/master/img/icons/web/icon-192.png?raw=true">
  <br>
  RootlessJamesDSP
  <br>
</h1>
<h4 align="center">System-wide JamesDSP implementation for non-rooted Android devices</h4>
<p align="center">
  <a href="https://play.google.com/store/apps/details?id=me.timschneeberger.rootlessjamesdsp&utm_source=github&pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1">
  	<img alt="Google play release" src="https://img.shields.io/github/v/release/ThePBone/RootlessJamesDSP?label=google%20play">
  </a>
  <a href="https://f-droid.org/packages/me.timschneeberger.rootlessjamesdsp/">
  	<img alt="F-Droid release" src="https://img.shields.io/f-droid/v/me.timschneeberger.rootlessjamesdsp">
  </a>
  <a href="https://github.com/ThePBone/RootlessJamesDSP/blob/master/LICENSE">
      <img alt="License" src="https://img.shields.io/github/license/ThePBone/RootlessJamesDSP">
  </a>
    <a href="https://github.com/ThePBone/RootlessJamesDSP/actions/workflows/build.yml">
      <img alt="GitHub Workflow Status" src="https://img.shields.io/github/actions/workflow/status/thepbone/rootlessjamesdsp/build.yml">
  </a>

</p>
<p align="center">
  <a href="#limitations">Limitations</a> •
  <a href="#spotify-support-patch">Spotify patch</a> •
  <a href="#downloads">Downloads</a> •
  <a href="#credits">Credits</a>
</p>

<p align="center">
  <a href='https://play.google.com/store/apps/details?id=me.timschneeberger.rootlessjamesdsp&utm_source=github&pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'> 
    <img width="300" alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png'/>
  </a>
</p>

<p align="center">
This app uses <a href="https://github.com/james34602/JamesDSPManager">libjamesdsp</a> which is written by <a href="https://github.com/james34602">James Fung (@james34602)</a>.
</p>

<p align="center">
    This app has several limitations that may be deal-breaking to some people; please read this whole document before using the app.</i>
</p>

<p align="center">
   <img alt="Screenshot" width="250" src="img/screenshot1.png">
   <img alt="Screenshot" width="250" src="img/screenshot7.png">
</p>


## Limitations
* Apps blocking internal audio capture remain unprocessed (e.g., Spotify, Google Chrome)
* Cannot coexist with (some) other audio effect apps (e.g., Wavelet and other apps that make use of the `DynamicsProcessing` Android API)
* Increased audio latency 


Apps confirmed working:
* YouTube
* YouTube Music
* Amazon Music
* Deezer
* Poweramp
* Substreamer
* Twitch
* Spotify ReVanced **(Patch required)**
* Apple Music
* Vinyl Music Player
* ...

Unsupported apps include:
* Spotify (patch for Spotify exists)
* Google Chrome
* SoundCloud
* ...

Tested on:
* Samsung Galaxy S20+ (Android 12; OneUI 4.0)
* Stock AOSP emulator (Android 10-13)
* Google Pixel 6 Pro (Android 13)
* Google Pixel 6a

## Spotify support patch
> **Note** This patch is universal and may also work with other apps than Spotify.

You can only use Spotify with this application if you patch the Spotify app.
The setup is very easy:

1. Download and install the [ReVanced manager APK](https://github.com/revanced/revanced-manager/releases) 
2. Install the unpatched Spotify app
3. Open ReVanced Manager, select Spotify and enable the `remove-screen-capture-restriction` patch.
4. Start the patching process and install the patched APK once it is done.
5. You can now use Spotify with RootlessJamesDSP.

### Patching other unsupported apps

The `remove-screen-capture-restriction` patch is universal and can also be used with custom APKs other than Spotify.
The patch cannot remove capture restrictions for apps that use the native AAudio C++ API for playback. 

1. Download and install the [ReVanced manager APK](https://github.com/revanced/revanced-manager/releases) 
2. Open ReVanced Manager, tap on 'Select an application' and press the 'Storage' action button in the bottom-right corner.
3. Select your APK using the file picker.
4. Enable the `remove-screen-capture-restriction` patch.
5. Start the patching process and install the patched APK once it is done. Make sure to uninstall the unpatched app if it is installed, otherwise you will run into a signature conflict during installtion.

> **Warning** If the patched app crashes on startup (or refuses to work properly), it is likely that the app uses signature checks or other protections against tampering. In that case, additional patches that disable these anti-tampering checks would need to be created by hand.

## Differences to other rootless FX apps

Regular rootless audio effect apps on the Play Store all essentially work the same way:
Android has several default audio effects built into its operating system that these apps can use without any special permissions. Here's a list of those: https://developer.android.com/reference/android/media/audiofx/AudioEffect.

Being restricted to these default built-in audio effects is problematic if you want to implement any advanced custom effects such as Viper or JDSP, because Android does not allow apps to access & modify the audio stream directly.

To work around this problem, RootlessJamesDSP uses a bunch of tricks to gain full access to the audio stream of other apps. This is done via Android's internal audio capture.
This allows RootlessJamesDSP to apply its custom audio effects directly without relying on Android's built-in effects.

Unfortunately, these tricks are not 100% reliable and introduce some limitations.
Apps such as Spotify block internal audio capture (they don't want people to record their songs), and because of that, RootlessJamesDSP cannot directly access the audio stream of that app.
This is the reason why a special patch is required to disable this DRM restriction inside Spotify's app. Patches for other apps with these DRM restrictions do not exist, but are possible to do.

## Translations

This application can be translated via Crowdin: https://crowdin.com/project/rootlessjamesdsp

Not all languages are enabled at the moment in Crowdin. To request a new language, please open an issue here on GitHub.

## Downloads

This app is available for free on Google Play: [https://play.google.com/store/apps/details?id=me.timschneeberger.rootlessjamesdsp](https://play.google.com/store/apps/details?id=me.timschneeberger.rootlessjamesdsp&utm_source=github&pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1)

Also available on F-Droid: https://f-droid.org/packages/me.timschneeberger.rootlessjamesdsp/

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
    alt="Get it on F-Droid"
    height="80">](https://f-droid.org/packages/me.timschneeberger.rootlessjamesdsp/)
[<img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png"
    alt="Get it on Google Play"
    height="80">](https://play.google.com/store/apps/details?id=me.timschneeberger.rootlessjamesdsp&utm_source=github&pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1)

## Using Root

This app focuses on a rootless implementation, but it can be made to work with the magisk module too. [See here for details](BUILD_ROOT.md).

All the limitations mentioned above are **not relevant** for the magisk/root version. 

## Credits

* JamesDSP - [James Fung (@james34602)](https://github.com/james34602)
* Theming system & backup system based on Tachiyomi

### Translators

<!-- CROWDIN-CONTRIBUTORS-START -->
<table>
  <tr>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/ThePBone"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15683553/medium/d13428d1e0922bc2069500aef57d1459.png" />
        <br />
        <sub><b>Tim Schneeberger (ThePBone)</b></sub></a>
      <br />
      <sub><b>22396 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/netrunner-exe"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15209210/medium/dabb33b18a6eb0e59cee34e448d81e40.jpg" />
        <br />
        <sub><b>Oleksandr Tkachenko (netrunner-exe)</b></sub></a>
      <br />
      <sub><b>13732 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/hanifz99"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15997687/medium/77003f34134a90b1b9089af86bbef755.png" />
        <br />
        <sub><b>Hanifz99 (hanifz99)</b></sub></a>
      <br />
      <sub><b>4149 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/rex07"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/13820943/medium/5b5499d4f13f168e0eab0499857a831e.jpeg" />
        <br />
        <sub><b>Rex_sa (rex07)</b></sub></a>
      <br />
      <sub><b>3543 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/FrameXX"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/14591682/medium/071f9d859dc36f9281f6f84b9c18c852.png" />
        <br />
        <sub><b>FrameXX</b></sub></a>
      <br />
      <sub><b>3518 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/eevan78"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/12664235/medium/ee2d64bed2ea9a0a1a5ee31e59fa9d7c.jpg" />
        <br />
        <sub><b>Ivan Pesic (eevan78)</b></sub></a>
      <br />
      <sub><b>3470 words</b></sub>
    </td>
  </tr>
  <tr>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/Add000"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15913337/medium/5bb6874d577c3c856b729fdcd2f9137a.jpg" />
        <br />
        <sub><b>Add000</b></sub></a>
      <br />
      <sub><b>3469 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/FlavioPonte"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15994613/medium/6ad9919ecb9cf61c034282b68e8bac17_default.png" />
        <br />
        <sub><b>FlavioPonte</b></sub></a>
      <br />
      <sub><b>3455 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/Gokwu"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15975377/medium/7be6218dc0f81f4f2dc8418ea983bd9e.png" />
        <br />
        <sub><b>Choi Jun Hyeong (Gokwu)</b></sub></a>
      <br />
      <sub><b>3438 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/AeroShark333"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/16224190/medium/e0b34056ea348d30906f48054f716f3c_default.png" />
        <br />
        <sub><b>Abiram Kanagaratnam (AeroShark333)</b></sub></a>
      <br />
      <sub><b>3373 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/fankesyooni"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15676501/medium/6ee6d7e4c63bfb0f90dc5088a5ff0efd.jpg" />
        <br />
        <sub><b>fankesyooni</b></sub></a>
      <br />
      <sub><b>3316 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/vjburic1"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/16358724/medium/2c068e312e7171e555b24f08c4ac9ae2.jpeg" />
        <br />
        <sub><b>Vjekoslav Buric (vjburic1)</b></sub></a>
      <br />
      <sub><b>3237 words</b></sub>
    </td>
  </tr>
  <tr>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/beruanglaut"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15727477/medium/928d69a437d753d783f03c22bf2d2c10.png" />
        <br />
        <sub><b>Beruanglaut (beruanglaut)</b></sub></a>
      <br />
      <sub><b>3168 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/fred199542"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15215886/medium/9a13bdf396f1b87097813de7767f36a4_default.png" />
        <br />
        <sub><b>Federico D. (fred199542)</b></sub></a>
      <br />
      <sub><b>2903 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/ismaeloi1"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15576171/medium/6909c87c219971037460a9110677b64a.png" />
        <br />
        <sub><b>Ismaël GUERET (ismaeloi1)</b></sub></a>
      <br />
      <sub><b>2844 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/hasandgn37"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15507252/medium/4a02e1c8d12aae3330baa229e5f8fb5e.jpeg" />
        <br />
        <sub><b>MajorCanel (hasandgn37)</b></sub></a>
      <br />
      <sub><b>2679 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/marcin.petrusiewicz"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/13535169/medium/29d3f1c6a1a270a85b8fda88e8d1c848.jpeg" />
        <br />
        <sub><b>Marcin Petrusiewicz (marcin.petrusiewicz)</b></sub></a>
      <br />
      <sub><b>2360 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/liziq"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15757161/medium/f3903c160404f095de68760f81609430.jpeg" />
        <br />
        <sub><b>zhiq liu (liziq)</b></sub></a>
      <br />
      <sub><b>1950 words</b></sub>
    </td>
  </tr>
  <tr>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/timli103117"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/16184616/medium/5bb20ab441ea015a44b727baf585c20d.png" />
        <br />
        <sub><b>Tim Li (timli103117)</b></sub></a>
      <br />
      <sub><b>1886 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/TecitoDeMenta"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15859109/medium/09cc6632c3686add5d52d4e7a3dec25a.jpg" />
        <br />
        <sub><b>Alondra Márquez (TecitoDeMenta)</b></sub></a>
      <br />
      <sub><b>1847 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/phannhanh"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/14543576/medium/229892a475f27a927eb4ac8874c1a648.jpg" />
        <br />
        <sub><b>Phan Nhanh (phannhanh)</b></sub></a>
      <br />
      <sub><b>1842 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/MES-INARI"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15690555/medium/d0cc094c5ae8ad9419d7e229d4ed76c0.jpg" />
        <br />
        <sub><b>MES-mitutti (MES-INARI)</b></sub></a>
      <br />
      <sub><b>1750 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/jont4"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15464490/medium/bd7f97dff61f637d007652f9947d8f17.jpeg" />
        <br />
        <sub><b>Jontix (jont4)</b></sub></a>
      <br />
      <sub><b>1731 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/narpatosian"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15572339/medium/887ab0b501163ccf586003a7bca29ee1.jpg" />
        <br />
        <sub><b>narpatosian</b></sub></a>
      <br />
      <sub><b>1469 words</b></sub>
    </td>
  </tr>
  <tr>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/SkyAfterRain_tw"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/16631123/medium/3665e526285f3ed15a2b2f7d68b13cbc.jpeg" />
        <br />
        <sub><b>SkyAfterRain_tw</b></sub></a>
      <br />
      <sub><b>1419 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/dang15082006"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/16282184/medium/bb9dbdbd49c8a5bf049bedc83a0d0cfc.jpeg" />
        <br />
        <sub><b>Đăng Nguyễn (dang15082006)</b></sub></a>
      <br />
      <sub><b>1307 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/SerAX3L"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15755831/medium/4f31c78564ed55fef4b2bf8d96213a55.jpeg" />
        <br />
        <sub><b>Alessandro Belfiore (SerAX3L)</b></sub></a>
      <br />
      <sub><b>1228 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/TheGary"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15713727/medium/4f9ede8b07ace57124001fb6678aeff7_default.png" />
        <br />
        <sub><b>Gary Bonilla (TheGary)</b></sub></a>
      <br />
      <sub><b>1030 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/kyunairi"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15925091/medium/7b1dd408c51242ab8602eb68408987cb_default.png" />
        <br />
        <sub><b>kyunairi</b></sub></a>
      <br />
      <sub><b>888 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/roccovantechno"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15818971/medium/75663306f941c87c2d9088c923aa89ad.jpeg" />
        <br />
        <sub><b>Gyuri Gergely (roccovantechno)</b></sub></a>
      <br />
      <sub><b>714 words</b></sub>
    </td>
  </tr>
  <tr>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/Nlntendq"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/14386422/medium/0c58b2245a59d1596c329dcf24037eb6.png" />
        <br />
        <sub><b>Nlntendq</b></sub></a>
      <br />
      <sub><b>684 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/illegalval"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15973113/medium/6aa896129bb17f69fb69c827274f2131.png" />
        <br />
        <sub><b>eurodyke (illegalval)</b></sub></a>
      <br />
      <sub><b>575 words</b></sub>
    </td>
  </tr>
</table><a href="https://crowdin.com/project/rootlessjamesdsp" target="_blank">Translate in Crowdin 🚀</a>
<!-- CROWDIN-CONTRIBUTORS-END -->
