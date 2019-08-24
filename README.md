## README file for software sm Spectro by Serge Masse ##
#### First Published on Gitlab 2016.11.20 ####

#### The app is currently published on Google Play: ####
https://play.google.com/store/apps/details?id=sm.app.spectro&hl=en

Version 4 is the current production version. 
Version 5 is the version in development.

[//]: <>(TODO 2017-6-7 Mic button: tries next mic in list of available sound sources; 
device text shows the current mic details at top, then output channel...
)

## Overview ##
sm Spectro is a complete spectrogram app for Android.

Its only dependency is the gitlab project **android-acoustic-lib**, 
from the same author. The library from android-acoustic-lib is an 
.aar file in directory app/libs: **sm-lib-sound-release.aar**; more details below.

The executable app does not use other libraries than 
the basic Android ones and the library from the same author.

Copyright (c) 2016-2019 Serge Masse

Redistribution and use in source and binary forms, with or without modification, are permitted 
provided that the following conditions are met:
                           
1. Redistributions of source code must retain the above copyright notice, this list of conditions 
and the following disclaimer.
                           
2. Redistributions in binary form must reproduce the above copyright notice, this list of 
conditions and the following disclaimer in the documentation and/or other materials 
provided with the distribution.
                           
3. Neither the name of the copyright holder nor the names of its contributors may be used 
to endorse or promote products derived from this software without specific prior written 
permission.

4. This software, as well as products derived from it, must not be used for the purpose of 
killing, harming, harassing, or capturing animals.
                           
THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, 
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF 
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

The use of this software with captive cetaceans and other large mammals
in captivity is discouraged.

### Identifiers ###

Android Manifest Package: sm.app.spectro 

Android Studio Project: Spectro

Gitlab Project: leafyseadragon/android-spectro-app


#### sm-lib-sound-release.aar ####

Library **sm-lib-acoustic** (package **sm.lib.acoustic**) 
contains generic acoustic functions, 
some specific to Android and some generic and not specific to Android. 
It also contains generic utilities.

The compiled file is **sm-lib-sound-release.aar**.

Android Studio Project: sm-lib-acoustic

Android Manifest Package = sm.lib.acoustic

GitLab Project: leafyseadragon / android-acoustic-lib

Library **sm-lib-comm**, also in the above GitLab project, is not used by the Spectro app.

#### DRAFT TEXT FOR PLAY STORE LISTING ####

See the sounds surrounding you:
- appreciate the visual structural beauty of bird songs
- detect sounds that you cannot hear
- evaluate the sound quality of your immediate environment
- locate anomalous sound sources in your house or at work
- get an idea of the quality of your hearing and may help you decide to get a medical hearing test

This app will also
- play a remote media file (e.g., a wav file) of killer whale vocalizations, for example, or the pre-recorded vocalizations from your cat or dog
- test the audio capabilities of your Android device and present the results in a text form
- select the best sampling rate supported by your device

Using the built-in feature of your Android device, you can take screen shots of the spectrogram and share them via email or messenger apps.
    The app does not include special features for doing this; just use the normal functions of your device.

External microphones may be compatible with your device, for example, via a USB connector or the audio jack.
    The app does not have special features for external mic's and you're on your own for determining if a microphone is compatible or not.
    Future versions may have special functions for testing external mic's.
    We do not recommend microphones that use Bluetooth at this time due to the inherent limitations of the current Bluetooth technology for audio input.

This app does not record sound.

This app automatically adapts itself to the best sampling rate supported by your device.
Some current Android devices can perform sampling of sounds at 96,000 samples per second, with a relatively narrow latency (maybe 1/2 second);
    this latency is the delay between the actual sounds and the availability of the numeric samples to the application.
    Only Android Marshmallow (i.e., version 6, API level 23), and later versions, do support 96,000 samples per second.
    This app supports 96,000 samples per second. It analyzes sounds 40 times per second. It uses 1024 samples for each analysis (using FFT).



#### TODO's Legend/Definitions ####

see all "bug", "washere", "prio 1", "prod"/"production" to-do's

prio 1 = urgent, possibly to be resolved before publishing current version

prio 2 = maybe for next version

no prio or prio 3: lowest priority, or no priority

washere is for easily finding the place where was working last in current issue in different locations
should not be used as a priority level

prod is for settings to be changed for production

#### for next version: prio 2 & 3 ####

2017-6-7 "Next Mic" button: tries next mic in list of available sound sources; 
device text shows the current mic details at top.

new gui to allow user to test different mic and speaker config: bluetooth, usb, ...

test with bluetooth mic and speakers, for altamer underwater tablet

2017-12-27 
Previous url: url play button ==> play/pause, previous, {cancel/clear?}
if the file is http.s, then it is saved and shown when using "Previous"
on normal app start, no url displayed, 
if http.s file from pref, then show "Previous", 
otherwise show disabled "PLay" url button;
Selecting "Previous" will play the http.s file from pref
(content files are not played again)

done: if the file is a content one (e.g. a download), 
then it is not saved and it is not shown when the app is restarted

done: invalid url file text not saved

prio 2 new preferences:
- for mic, offer to user all choices supported by the device and give the app one of the options
    future:
- for channel, mono or stereo, same as above
- for float pcm encoding, same as above, float/int for input, float/int for output, depending on version of android
- for sampling rate, offer to user the native rate and some other rates supported by device, and let user pick one
      and let user decide for same encoding for input and output

2017-6-3 prio 2 next version: Make the status bar translucent

```Window window = activity.getWindow();
window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
window.setStatusBarColor(ContextCompat.getColor(activity, R.color.example_color));

android:statusBarColor = @android:color/transparent

    <!-- Make the status bar translucent -->
    <style name="AppTheme" parent="AppTheme.Base">
        <item name="android:windowTranslucentStatus">true</item>
    </style>
```

prio 1 or 2? maybe [audio input source = android default, not mic, test bluetooth mic]

prio 1 or 2? list of sources/recordings of ceta sounds; to add in about text

prio 1 or 2? activity with text containing URLs of ceta sounds

do in prominence in play store text:

>Great to analyse live sounds and recorded sound files.
This app does not record sound but will play most types of recorded files and will display the histogram of the sound as it is played.
To do that, one way is to go to the sound file using the Chrome browser and to share the sound to this app.
To share the sound file, you open the pop-up on the sound file link and you select the share option, which has an icon that looks like three linked dots.
When the choices of ways to share comes up, you select the icon for this app.
You can also use other sound apps that can share recorded files with this app, often in a similar fashion to using Chrome for sharing.

 
prio 3: keep list of urls to play, give them names, delete, move up/down, export list (share)

prio 3: weakness: when the app loses focus after a url Pause, when it comes back, the button is Resume 
but the url/intent restarts from beginning; fix: keep the position in preferences and seek to it at restart

prio 3: try the new exoplayer

for html strings: 
```<string name="my_text">
  <![CDATA[
    <b>Autor:</b> Mr Nice Guy<br>
    <b>Contact:</b> myemail@g.com<br>
    <i>Copyright © 2011-2012 isc </i>
  ]]>
</string>
tv.setText(Html.fromHtml(getString(R.string.my_text)));
```

#### 2018 todo's ####

[presentation with bluetooth speaker (waterproof)]

[presentation with laptop showing phone screen (airdroid, teamviewer)]

[record and post videos of great whistle recording(s) played by spectrogram, send to west coast orca team]

[cleanup the code for public opensource (all apps and libs)]

done: add license text at top of all classes (all apps and libs)

work on the other 2 apps, Recog and DolphinComm

### samsung SGH-I467M SDK 19 test results 2018-2-10 ###

todo www.samsung.com/kies

fixed: no output for < v21

[buttons too dark]

done: move device basic at top of text

done: add in about: icon is made using a screenshot of a recording by www.aguasonic.com of a very animated
discussion (vocalisations) by a group of dolphins, probably Tursiops, with often some 5 individuals
whistling at the same time.

#### 2018-4-17 ####

[not starting correctly]

