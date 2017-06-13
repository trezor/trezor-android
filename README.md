# trezor-android

[![Build Status](https://travis-ci.org/trezor/trezor-android.svg?branch=master)](https://travis-ci.org/trezor/trezor-android) [![gitter](https://badges.gitter.im/trezor/community.svg)](https://gitter.im/trezor/community)

TREZOR Management App and Communication Library for Android

## Download options

* [Google Play Store](https://play.google.com/store/apps/details?id=io.trezor.app)
* [TREZOR.io](https://wallet.trezor.io/data/android/latest.apk)
* [F-Droid](https://f-droid.org/repository/browse/?fdid=io.trezor.app)

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
    compile 'com.github.trezor.trezor-android:trezor-lib:v1.0.4'
    ...
}
```

Please consider using [gradle witness](https://github.com/WhisperSystems/gradle-witness) to improve security.

## Requirements

TREZOR has to be connected to Android by OTG cable. The Android device needs to support such connections.
