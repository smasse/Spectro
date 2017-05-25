## README file for software sm Spectro by Serge Masse ##
#### First Published on Gitlab 2016.11.20 ####
##### Version 2017-3-5 #####

## Overview ##
sm Spectro is a complete spectrogram app for Android.

Its only dependency is the gitlab project android-acoustic-lib, 
from the same author. The library from android-acoustic-lib is an 
.aar file in directory app/libs: lib-acoustic-release.aar.

The executable app does not use other libraries than 
the basic Android ones and the library from the same author.

This software is not to be used for the purpose of killing or harming animals.

Copyright 2017 Serge Masse

License: Apache 2.0

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