package com.circlegate.liban.utils;

import android.util.Log;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class LogUtils {
    private static final int MIN_LOG_TO_FILE_COUNT = 400;
    private static final int MAX_LOG_TO_FILE_COUNT = 500;
    private static final DateFormat DATE_FORMATTER = new SimpleDateFormat("[yyyy.MM.dd HH:mm:ss:SSS]", Locale.getDefault());
//    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("[yyyy.MM.dd HH:mm:ss:SSS]");

    private static boolean logcatEnabled;

    private static File logFile = null;
    private static PrintStream logFileStream = null;
    private static final ArrayList<String> logEntries = new ArrayList<>(MAX_LOG_TO_FILE_COUNT); // uchovava posledni logy v pameti - at uz zapisujeme nebo nezapisujeme do log filu

    public static void setLoggingEnabled(boolean logcatEnabled) {
        LogUtils.logcatEnabled = logcatEnabled;
    }

    private static void closeLogFile() {
        if (logFileStream != null) {
            logFileStream.close();
            logFileStream = null;
        }
    }

    private static void openLogFile(boolean append) {
        try {
            logFileStream = new PrintStream(new BufferedOutputStream(new FileOutputStream(logFile, append)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void logToArrayList(String type, String tag, String text, Throwable optTr) {
        StringBuilder s = new StringBuilder(DATE_FORMATTER.format(new Date()));
        s.append(" ");
        s.append(type).append(":").append(tag).append(": ").append(text);

        if (optTr != null) {
            s.append("\n");
            s.append(Log.getStackTraceString(optTr));
        }
        String newLine = s.toString();

        synchronized (logEntries) {
            if (logEntries.size() >= MAX_LOG_TO_FILE_COUNT) {
                logEntries.subList(0,  logEntries.size() - MIN_LOG_TO_FILE_COUNT).clear();

                // pokud mame k dispozici soubor pro logovani, tak jdeme zapisovat
                if (logFile != null) {
                    closeLogFile();
                    openLogFile(false);

                    for (String line : logEntries) {
                        logFileStream.println(line);
                    }
                }
            }
            logEntries.add(newLine);

            if (logFile != null) {
                logFileStream.println(newLine);
                logFileStream.flush();
            }
        }
    }


    /**
     * Wraps {@link android.util.Log#i}.
     *
     * @param tag
     * @param string
     */
    public static void i(String tag, String string) {
        //if (logcatEnabled) {
        Log.i(tag, string);
        //}
        logToArrayList("i", tag, string, null);
    }

    /**
     * Wraps {@link android.util.Log#e}.
     *
     * @param tag
     * @param string
     */
    public static void e(String tag, String string) {
        //if (logcatEnabled) {
        Log.e(tag, string);
        //}
        logToArrayList("e", tag, string, null);
    }

    /**
     * Wraps {@link android.util.Log#e}.
     *
     * @param tag
     * @param string
     */
    public static void e(String tag, String string, Throwable tr) {
        //if (logcatEnabled) {
        Log.e(tag, string, tr);
        //}
        logToArrayList("e", tag, string, tr);
    }

    /**
     * Wraps {@link android.util.Log#d}.
     *
     * @param tag
     * @param string
     */
    public static void d(String tag, String string) {
        if (logcatEnabled) {
            Log.d(tag, string);
        }
        logToArrayList("d", tag, string, null);
    }

    /**
     * Wraps {@link android.util.Log#w}.
     *
     * @param tag
     * @param string
     */
    public static void w(String tag, String string) {
        if (logcatEnabled) {
            Log.w(tag, string);
        }
        logToArrayList("w", tag, string, null);
    }


    /**
     * Wraps {@link android.util.Log#d}.
     *
     * @param tag
     * @param value
     */
    public static void d(String tag, int value) {
        d(tag, String.valueOf(value));
    }
}
