package com.circlegate.libanloc;

import android.content.Context;
import android.support.v4.content.ContextCompat;

public class LocalUtils {
    public static int checkSelfPermission(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission);
    }
}
