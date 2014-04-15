package com.circlegate.liban.utils;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;

public class AppUtils {
    private static int versionCode = Integer.MIN_VALUE;
    private static String versionName = null;
    private static int  targetSdkVersion = Integer.MIN_VALUE;

    public static void init(Context context) {
        try {
            String packageName = context.getPackageName();
            versionCode = context.getPackageManager().getPackageInfo(packageName, 0).versionCode;
            versionName = context.getPackageManager().getPackageInfo(packageName, 0).versionName;
            targetSdkVersion = context.getPackageManager().getApplicationInfo(packageName, 0).targetSdkVersion;
        }
        catch (NameNotFoundException e) {
            versionCode = -1;
            versionName = null;
            targetSdkVersion = -1;
        }
    }

    public static String getApplicationId(Context context) {
        String ret = context.getApplicationContext().getPackageName();
        return ret;
    }

    public static int getAppVersionCode() {
        if (versionCode == Integer.MIN_VALUE)
            throw new IllegalStateException("Must call AppUtils.init before calling AppUtils.getAppVersionCode()!");
        return versionCode;
    }

    public static String getAppVersionName() {
        if (versionName == null)
            throw new IllegalStateException("Must call AppUtils.init before calling AppUtils.getAppVersionName()!");
        return versionName;
    }

    public static boolean getAppVersionNameHasAnyPostfix() {
        String v = getAppVersionName();
        return v.indexOf(' ') > 0 || v.indexOf('-') > 0;
    }

    /**
     * Funkce vrati verzi aplikace bez postfixu typu beta1, RC3 "-debug" apod. - tzn. jen napr. 2.0.0
     * @param context
     * @return
     */
    public static String getAppVersionNameWithoutPostfix(Context context) {
        try {
            String ret = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;

            int ind = ret.indexOf(' ');
            if (ind >= 0)
                ret = ret.substring(0, ind);

            ind = ret.indexOf('-');
            if (ind >= 0)
                ret = ret.substring(0, ind);

            return ret;
        }
        catch (NameNotFoundException e) {
            return "err";
        }
    }

    public static int getTargetSdkVersion() {
        if (targetSdkVersion == Integer.MIN_VALUE)
            throw new IllegalStateException("Must call AppUtils.init before calling AppUtils.getTargetSdkVersion()!");
        return targetSdkVersion;
    }

    public static String getModelName() {
        try {
            String manufacturer = Build.MANUFACTURER;
            String model = Build.MODEL;
            if (model.startsWith(manufacturer)) {
                return capitalize(model);
            } else {
                return capitalize(manufacturer) + " " + model;
            }
        }
        catch (Exception ex) {
            return "err";
        }
    }


    private static String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }
}
