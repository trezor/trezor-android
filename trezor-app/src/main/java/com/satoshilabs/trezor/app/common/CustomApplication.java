package com.satoshilabs.trezor.app.common;

import android.app.Application;

import com.circlegate.liban.utils.LogUtils;

import java.lang.Thread.UncaughtExceptionHandler;

public class CustomApplication extends Application {
    private UncaughtExceptionHandler defaultUeh;

    private UncaughtExceptionHandler myUeh = new UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            LogUtils.e(CustomApplication.class.getSimpleName(), "UncaughtExceptionHandler - uncaught exception", ex);
            defaultUeh.uncaughtException(thread, ex);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        //Fabric.with(this, new Crashlytics());// musi byt pred nastavenim UncaughtExceptionHandleru

        defaultUeh = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(myUeh);

        GlobalContext.init(this);
    }
}
