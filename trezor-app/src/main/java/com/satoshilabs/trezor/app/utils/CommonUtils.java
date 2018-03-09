package com.satoshilabs.trezor.app.utils;

import android.text.TextUtils;

import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.Features;

public class CommonUtils {
    public static boolean isTrezorV2(Features features) {
        return TextUtils.equals(features.getModel(), "T");
    }
}
