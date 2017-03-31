# About

This repository contains all files to integrate the radioScreen SonoBeacons and Fraunhofer stream embedded watermark recognition into Your Android app.

## Prerequisites

Make sure, in Your Android Studio, CMake and NDK SDK-Tools are installed. This can be found under:

    Preferences => Appearance & Behaviour => System Settings => Android SDK => SDK Tools (Tab)
    
## Permissions

The app needs to record audio from the microphone. Please add the following tag to Your AndroidManifest.xml:

    <uses-permission android:name="android.permission.RECORD_AUDIO"/>

## Libraries

The Fraunhofer detection is provided as a native library. Please copy the contents of

    ../app/src/main/cpp
and
    ../app/src/main/jniLibs
to the corresponding location in Your project.



