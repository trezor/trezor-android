package com.satoshilabs.trezor.app.common;

import android.app.Application;
import android.util.Log;
import com.circlegate.liban.utils.LogUtils;
import java.lang.Thread.UncaughtExceptionHandler;
import timber.log.Timber;

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

        Timber.plant(new Timber.Tree() {
            @Override
            protected void log(int priority, String tag, String message, Throwable t) {
                if (t != null) {
                    Log.e(tag, message, t);
                    return;
                }
                switch (priority) {
                    case Log.VERBOSE:
                        LogUtils.d(tag, message);
                        break;
                    case Log.INFO:
                        LogUtils.i(tag, message);
                        break;
                    case Log.DEBUG:
                        LogUtils.d(tag, message);
                        break;
                    case Log.ERROR:
                        LogUtils.e(tag, message);
                        break;
                    case Log.WARN:
                        LogUtils.w(tag, message);
                        break;
                }

            }
        });
        defaultUeh = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(myUeh);

        GlobalContext.init(this);
    }
}
