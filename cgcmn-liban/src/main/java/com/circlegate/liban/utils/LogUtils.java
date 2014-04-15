package com.circlegate.liban.utils;

import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

public class LogUtils {
    private static final int MIN_LOG_TO_FILE_COUNT = 400;
    private static final int MAX_LOG_TO_FILE_COUNT = 500;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("[yyyy.MM.dd HH:mm:ss:SSS]");

    private static boolean logcatEnabled;

    private static File logFile = null;
    private static PrintStream logFileStream = null;
    private static final ArrayList<String> logEntries = new ArrayList<>(MAX_LOG_TO_FILE_COUNT); // uchovava posledni logy v pameti - at uz zapisujeme nebo nezapisujeme do log filu

    public static void setLoggingEnabled(boolean logcatEnabled) {
        LogUtils.logcatEnabled = logcatEnabled;
    }

    public static void setupLogFile(File optLogFile) {
        synchronized (logEntries) {
            if (logFile != optLogFile && (logFile == null || optLogFile == null || !logFile.getAbsolutePath().equals(optLogFile.getAbsolutePath()))) {
                closeLogFile();
                logFile = optLogFile;

                if (logFile != null) {
                    openLogFile(true);
                }
            }
        }
    }

    public static StringBuilder copyLogs() {
        synchronized (logEntries) {
            StringBuilder b = new StringBuilder();
            for (String line : logEntries)
                b.append(line).append("\n");
            return b;
        }
    }

    public static void copyLogsToStream(OutputStream outputStream) {
        synchronized (logEntries) {
            if (logFile != null && logFile.exists()) {
                closeLogFile();

                BufferedInputStream origin = null;
                byte buffer[] = new byte[1024 * 8];

                try {
                    origin = new BufferedInputStream(new FileInputStream(logFile), buffer.length);
                    int count;
                    while ((count = origin.read(buffer, 0, buffer.length)) != -1) {
                        outputStream.write(buffer, 0, count);
                    }
                }
                catch (IOException ex) {
                    ex.printStackTrace();
                }
                finally {
                    try {
                        if (origin != null)
                            origin.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                openLogFile(true);
            }
            else {
                // pokud nemame ulozeny logFile, tak zapiseme aspon aktualne ulozene logy v pameti
                if (!logEntries.isEmpty()) {
                    PrintStream printStream = new PrintStream(new BufferedOutputStream(outputStream));

                    for (String line : logEntries) {
                        printStream.println(line);
                    }
                }
            }
        }
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
        StringBuilder s = new StringBuilder(new DateTime().toString(DATE_TIME_FORMATTER));
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
