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

## Spotify support patch
You can only use Spotify with this application if you patch the Spotify app.
The setup is very easy:

1. Download and install the [ReVanced manager APK](https://github.com/revanced/revanced-manager/releases) 
2. Install the unpatched Spotify app

NOTE: Tested with Spotify version `8.7.48.1062` and `8.7.68.568`

3. Open ReVanced Manager, select Spotify and enable the `disable-capture-restriction` patch.
4. Start the patching process and install the patched APK once it is done.
5. You can now use Spotify with RootlessJamesDSP.

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

## Credits

* JamesDSP - [James Fung (@james34602)](https://github.com/james34602)
* Theming system based on Tachiyomi

### Translators

<!-- CROWDIN-CONTRIBUTORS-START -->
<table>
  <tr>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/ThePBone"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15683553/medium/d13428d1e0922bc2069500aef57d1459.png" />
        <br />
        <sub><b>Tim Schneeberger (ThePBone)</b></sub></a>
      <br />
      <sub><b>4946 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/ianpok17"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15647373/medium/daf979a91f0a64b448cf88a954d45e2b.jpeg" />
        <br />
        <sub><b>Criss Santiesteban (ianpok17)</b></sub></a>
      <br />
      <sub><b>470 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/Tymwitko"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15706765/medium/a2288209d82b78b8e8d959c009382086_default.png" />
        <br />
        <sub><b>Tymwitko</b></sub></a>
      <br />
      <sub><b>210 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/michelequercetti"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15688795/medium/2ca2b8ce17c5319c71579160cd0f7b97.jpeg" />
        <br />
        <sub><b>michele quercetti (michelequercetti)</b></sub></a>
      <br />
      <sub><b>50 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/redwalery17"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15698583/medium/2e78c4e4e8152f0e56b34b67955e96ee.jpeg" />
        <br />
        <sub><b>Валерий Удовенко (redwalery17)</b></sub></a>
      <br />
      <sub><b>2 words</b></sub>
    </td>
  </tr>
</table><a href="https://crowdin.com/project/rootlessjamesdsp" target="_blank">Translate in Crowdin 🚀</a>
<!-- CROWDIN-CONTRIBUTORS-END -->
