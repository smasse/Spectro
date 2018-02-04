## README file for software sm Spectro by Serge Masse ##
#### First Published on Gitlab 2016.11.20 ####

#### The app is currently published on Google Play: ####
https://play.google.com/store/apps/details?id=sm.app.spectro&hl=en

[//]: <>(TODO 2017-6-7 Mic button: tries next mic in list of available sound sources; 
device text shows the current mic details at top, then output channel...
)

##### Version 3: 2018-1-2 #####

## Overview ##
sm Spectro is a complete spectrogram app for Android.

Its only dependency is the gitlab project **android-acoustic-lib**, 
from the same author. The library from android-acoustic-lib is an 
.aar file in directory app/libs: **lib-acoustic-release.aar**; more details below.

The executable app does not use other libraries than 
the basic Android ones and the library from the same author.

Copyright 2018 Serge Masse

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

This software is not to be used
for the purpose of killing, harming, harassing, or capturing animals.
The use of this software with captive cetaceans and other large mammals
in captivity is discouraged.

### Identifiers ###

Android Manifest Package: sm.app.spectro 

Android Studio Project: Spectro

Gitlab Project: leafyseadragon/android-spectro-app


#### lib-acoustic-release.aar ####

Library **lib-acoustic** (package **sm.lib.acoustic**) 
contains generic acoustic functions, 
some specific to Android and some generic and not specific to Android. 
It also contains generic utilities.

The compiled file is **lib-acoustic-release.aar**.

Android Studio Project: sm-lib-acoustic

Android Manifest Package = sm.lib.acoustic

GitLab Project: leafyseadragon / android-acoustic-lib

Library **lib-comm** from the above package is not used by the Spectro app.

#### DRAFT TEXT FOR PLAY STORE LISTING ####

See the sounds surrounding you:
- appreciate the visual structural beauty of bird songs
- detect sounds that you cannot hear
- evaluate the sound quality of your immediate environment
- locate anomalous sound sources in your house or at work
- get an idea of the quality of your hearing and may help you decide to get a medical hearing test

This app will also
- play a remote media file (e.g., a wa. file) of killer whale vocalizations, for example, or the pre-recorded vocalizations from your cat or dog
- test the audio capabilities of your Android device and present the results in a text form
- select the best sampling rate supported by your device

Using the built-in feature of your Android device, you can take screen shots of the spectrogram and share them via email or messenger apps.
    The app does not include special features for doing this; just use the normal functions of your device.

External microphones may be compatible with your device, for example, via a USB connector or the RCA audio jack.
    The app does not have special features for external mic's and you're on your own for determining if a microphone is compatible or not.
    Future versions may have special functions for testing external mic's.
    We do not recommend microphones that use Bluetooth at this time due to the inherent limitations of the current Bluetooth technology for audio input.

This app does not record sound.

This app automatically adapts itself to the best sampling rate supported by your device.
Some current Android devices can perform sampling of sounds at 96,000 samples per second, with a relatively narrow latency (maybe 1/2 second);
    this latency is the delay between the actual sounds and the availability of the numeric samples to the application.
    Only Android Marshmallow (i.e., version 6, API level 23), and later versions, do support 96,000 samples per second.
    This app supports 96,000 samples per second. It analyzes sounds 40 times per second. It uses 1024 samples for each analysis (using FFT).


### Publication Process ###

1. Review all "washere", "bug", "prio 1", and "prod" to-do's

2. disable all log flags (in libs and apps) and isDevMode(), ensure all Log uses are in flagged blocks

3. Libs: Compile (assemble) release aar's, then copy aar's to 3 client projects, and test in these projects

4. Review all copyright strings

5. Review all texts shown to user (About, Device, etc.), i18n resources

6. review launch icon(s) and other images

7. for opensourcing, license text at head of all classes; remove old code

8. re-test

9. Copy to backup external drive

10. Update git local and remote 

10. Publish one or more client projects to G Play 
https://developer.android.com/studio/publish/index.html

#### TODO's ####

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

prio 1 or 2? maybe app audio input source = android default, not mic, test bluetooth mic

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

presentation with bluetooth speaker (waterproof)

presentation with laptop showing phone screen (airdroid, teamviewer)

record and post videos of great whistle recording(s) played by spectrogram, send to west coast orca team

cleanup the code for public opensource (all apps and libs)

add license text at top of all classes (all apps and libs)

work on the other 2 apps, Recog and DolphinComm

