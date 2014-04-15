package com.circlegate.liban.base;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.text.TextUtils;

import com.circlegate.liban.base.CommonClasses.IGlobalContext;
import com.circlegate.liban.task.TaskCommon.TaskCache;
import com.circlegate.liban.task.TaskExecutor;
import com.circlegate.liban.task.TaskInterfaces.ITaskContext;
import com.circlegate.liban.task.TaskInterfaces.ITaskExecutor;
import com.circlegate.liban.utils.AppUtils;
import com.circlegate.liban.utils.LogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public abstract class GlobalContextLib implements IGlobalContext, ITaskContext {
    private static final String TAG = GlobalContextLib.class.getSimpleName();

    private static GlobalContextLib singleton;

    private final Context androidContext;

    private TaskExecutor taskExecutor;
    private TaskCache taskCache;

    private final ArrayList<String> dbFilesList = new ArrayList<>();

    // jestli je aplikace momentalne nakonfigurovana jako verze pro Google Play
    // - konkretne se kontroluje, jestli je zakazano debugovani a jestli verze aplikace neobsahuje zadne postfixy
    private final boolean appIsInProductionMode;

    protected static void init(GlobalContextLib singleton) {
        if (GlobalContextLib.singleton != null)
            throw new RuntimeException("init called more than one time!");
        GlobalContextLib.singleton = singleton;
    }

    public static GlobalContextLib get() {
        return singleton;
    }


    protected GlobalContextLib(Context context) {
        AppUtils.init(context);
        System.setProperty("org.joda.time.DateTimeZone.Provider", "com.circlegate.liban.base.FastDateTimeZoneProvider");
        this.androidContext = context.getApplicationContext();

        boolean isDebuggable = (0 != (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));
        this.appIsInProductionMode = !isDebuggable && !AppUtils.getAppVersionNameHasAnyPostfix();
    }


    @Override
    public Context getAndroidContext() {
        return androidContext;
    }

    @Override
    public boolean getAppIsInProductionMode() {
        return this.appIsInProductionMode;
    }


    @Override
    public synchronized ITaskExecutor getTaskExecutor() {
        if (this.taskExecutor == null) {
            LogUtils.d(TAG, "Before creating TaskExecutor");
            this.taskExecutor = new TaskExecutor(this);
            LogUtils.d(TAG, "After creating TaskExecutor");
        }
        return this.taskExecutor;
    }

    @Override
    public synchronized TaskCache getTaskCache() {
        if (this.taskCache == null) {
            LogUtils.d(TAG, "Before creating TaskCache");
            this.taskCache = new TaskCache();
            LogUtils.d(TAG, "After creating TaskCache");
        }
        return this.taskCache;
    }

//    @Override
//    public String getCurrentLangAbbrev() {
//        Locale l = getCurrentLocale();
//        final String ret = l.getLanguage();
//        return TextUtils.isEmpty(ret) ? "en" : ret;
//    }

    @Override
    public String getCurrentCountryAbbrev() {
        Locale l = getAndroidContext().getResources().getConfiguration().locale;
        if (l == null) {
            l = Locale.getDefault();
            if (l == null)
                l = Locale.US;
        }
        String ret = l.getCountry();
        return TextUtils.isEmpty(ret) ? "US" : ret;
    }


    protected void addDbFileNameToList(String fileName) {
        if (dbFilesList.contains(fileName))
            throw new RuntimeException("File name already added to dbFilesList: " + fileName);
        dbFilesList.add(fileName);
    }

    public synchronized List<String> getDbFilesList() {
        return dbFilesList;
    }

    public synchronized String writePortableDbFileIfNeeded(String dbFileName) {
        return dbFileName;
    }

    public abstract void requestGoogleBackupIfNeeded(String changedDbFileName);
}
