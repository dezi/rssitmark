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

    .../app/src/main/cpp
    
and

    .../app/src/main/jniLibs
    
to the corresponding location in Your project.

## Java Interface

Copy the java interface from

    .../app/src/main/java/de/kappa_mm/sitmark/sitmarkaudiobeaconbridge
    
to the corresponding location in Your project.

## Build Integration

Make sure, the contents of

    .../app/CMakeLists.txt

is integrated with Your local CMakeLists.txt if You have already native parts in Your app, or the complete file is copied to the corresponding location.

# Sample Activity

A sample activity class is contained in

    .../app/src/main/java/de/kappa_mm/sitmark/sitmarkaudiobeacondemo

You can simple pull and compile the whole project to see, how it works.

