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
      <sub><b>21131 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/netrunner-exe"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15209210/medium/dabb33b18a6eb0e59cee34e448d81e40.jpg" />
        <br />
        <sub><b>Oleksandr Tkachenko (netrunner-exe)</b></sub></a>
      <br />
      <sub><b>6209 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/FrameXX"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/14591682/medium/071f9d859dc36f9281f6f84b9c18c852.png" />
        <br />
        <sub><b>FrameXX</b></sub></a>
      <br />
      <sub><b>2950 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/fankesyooni"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15676501/medium/6ee6d7e4c63bfb0f90dc5088a5ff0efd.jpg" />
        <br />
        <sub><b>fankesyooni</b></sub></a>
      <br />
      <sub><b>2840 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/beruanglaut"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15727477/medium/928d69a437d753d783f03c22bf2d2c10.png" />
        <br />
        <sub><b>Beruanglaut (beruanglaut)</b></sub></a>
      <br />
      <sub><b>2544 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/TheGary"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15713727/medium/4f9ede8b07ace57124001fb6678aeff7_default.png" />
        <br />
        <sub><b>Gary Bonilla (TheGary)</b></sub></a>
      <br />
      <sub><b>1030 words</b></sub>
    </td>
  </tr>
  <tr>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/pizzawithdirt"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15711961/medium/e6c27e5ff36a68db03f9b786007b9cbd.png" />
        <br />
        <sub><b>Ali Yuruk (pizzawithdirt)</b></sub></a>
      <br />
      <sub><b>606 words</b></sub>
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
      <a href="https://crowdin.com/profile/Jamil.M.Gomez"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/13442100/medium/70d15cc33101a9739868321b10543f18.png" />
        <br />
        <sub><b>Jamil M. Gomez (Jamil.M.Gomez)</b></sub></a>
      <br />
      <sub><b>131 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/KnoyanMitsu"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15717417/medium/448273d6a14af20ef27c48850d69fc43.jpeg" />
        <br />
        <sub><b>Knoyan Mitsu (KnoyanMitsu)</b></sub></a>
      <br />
      <sub><b>61 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/dev_trace"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15729737/medium/f515d9ef1eeb393759e7180bc700afc2_default.png" />
        <br />
        <sub><b>dev_trace</b></sub></a>
      <br />
      <sub><b>61 words</b></sub>
    </td>
  </tr>
  <tr>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/jont4"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15464490/medium/bd7f97dff61f637d007652f9947d8f17.jpeg" />
        <br />
        <sub><b>Jontix (jont4)</b></sub></a>
      <br />
      <sub><b>52 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/trmatii"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15725233/medium/a324828f5904f9be718a3e6de262a48d.jpeg" />
        <br />
        <sub><b>Matias Cortes (trmatii)</b></sub></a>
      <br />
      <sub><b>51 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/michelequercetti"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15688795/medium/2ca2b8ce17c5319c71579160cd0f7b97.jpeg" />
        <br />
        <sub><b>michele quercetti (michelequercetti)</b></sub></a>
      <br />
      <sub><b>50 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/nattramnar"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15722771/medium/13729d04abd3d7cfb680bfb1cef79a33_default.png" />
        <br />
        <sub><b>nattramnar</b></sub></a>
      <br />
      <sub><b>28 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/Rosacco"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15722829/medium/45cf6551b57a2ae855c714389ef43f3c_default.png" />
        <br />
        <sub><b>Rosacco</b></sub></a>
      <br />
      <sub><b>21 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/O2C14"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15716303/medium/b2e76f82c99dd39c24ec0c3e36c0fdc9.png" />
        <br />
        <sub><b>陈里 (O2C14)</b></sub></a>
      <br />
      <sub><b>19 words</b></sub>
    </td>
  </tr>
  <tr>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/vbisoi"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15726357/medium/e7a928f5536f12bdd3a985b116c65d0b_default.png" />
        <br />
        <sub><b>vbisoi</b></sub></a>
      <br />
      <sub><b>15 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/redwalery17"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15698583/medium/2e78c4e4e8152f0e56b34b67955e96ee.jpeg" />
        <br />
        <sub><b>Валерий Удовенко (redwalery17)</b></sub></a>
      <br />
      <sub><b>8 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/mrbin233"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15721371/medium/fe13b9238375d895e197025c6c24e4d4.jpeg" />
        <br />
        <sub><b>mrbin233</b></sub></a>
      <br />
      <sub><b>3 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/nnamphong0709"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15714167/medium/3a78fdaf3d50a166ee41f3644ef523c8.jpeg" />
        <br />
        <sub><b>Namphong Nguyen (nnamphong0709)</b></sub></a>
      <br />
      <sub><b>2 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/funtos666"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15720235/medium/f5cc8364c68501da39f0b3606fb2743a.jpeg" />
        <br />
        <sub><b>Рома Romario (funtos666)</b></sub></a>
      <br />
      <sub><b>2 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/mekyson"><img alt="logo" style="width: 64px" src="https://crowdin-static.downloads.crowdin.com/avatar/15715699/medium/3445884a2b6cf411305a730e29cb9d72.jpeg" />
        <br />
        <sub><b>Mekyson Makys (mekyson)</b></sub></a>
      <br />
      <sub><b>1 words</b></sub>
    </td>
  </tr>
</table><a href="https://crowdin.com/project/rootlessjamesdsp" target="_blank">Translate in Crowdin 🚀</a>
<!-- CROWDIN-CONTRIBUTORS-END -->
