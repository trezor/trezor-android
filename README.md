# trezor-android

[![Build Status](https://travis-ci.org/trezor/trezor-android.svg?branch=master)](https://travis-ci.org/trezor/trezor-android) [![gitter](https://badges.gitter.im/trezor/community.svg)](https://gitter.im/trezor/community) [![](https://jitpack.io/v/trezor/trezor-android.svg)](https://jitpack.io/#trezor/trezor-android)

Trezor Communication Library for Android

**Trezor Manager app is now obsolete!**

## Using the library

You can add the library via jitpack. Add to jitpack as a repository:

```groovy
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
        ...
    }
}
```

Then you can add the dependency like this:

```groovy
dependencies {
    compile 'com.github.trezor.trezor-android:trezor-lib:<current_version>'
    ...
}
```

You can see what to best use for ```<current_version>``` in the jitpack badge on the top.

Please consider using [gradle witness](https://github.com/WhisperSystems/gradle-witness) to improve security.

## Requirements

Trezor has to be connected to Android by OTG cable. The Android device needs to support such connections.
