package com.circlegate.liban.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;

import android.content.Context;
import android.os.Environment;

import com.circlegate.liban.base.ApiDataIO.ApiDataAppVersionCodeLegacyResolver;
import com.circlegate.liban.base.ApiDataIO.ApiDataInput;
import com.circlegate.liban.base.ApiDataIO.ApiDataInputOutputBase;
import com.circlegate.liban.base.ApiDataIO.ApiDataInputStreamWrp;
import com.circlegate.liban.base.ApiDataIO.ApiDataOutput;
import com.circlegate.liban.base.ApiDataIO.ApiDataOutputStreamWrp;
import com.circlegate.liban.base.GlobalContextLib;

public class FileUtils {
    private static final String TAG = FileUtils.class.getSimpleName();

    private static final String TMP_FILE_POSTFIX = "~tmp";

    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /* Checks if external storage is available to at least read */
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }


    public static boolean readObjsFromFile(Context context, FileObjsStaticInfo info, ReadObjsCallback callback) {
        synchronized (info.getLock()) {
            String fileName = info.getFileName();
            ApiDataInputStreamWrp stream = null;
            try {
                if (!context.getFileStreamPath(fileName).exists()) {
                    fileName += TMP_FILE_POSTFIX; // Pokud dany soubor neexistuje, tak se podivame, jestli neexistuje soubor s priponou ~tmp a pripadne jej zkusime nacist

                    if (!context.getFileStreamPath(fileName).exists()) {
                        callback.setDefaults();
                        return false;
                    }
                }

                stream = new ApiDataInputStreamWrp(new DataInputStream(new BufferedInputStream(context.openFileInput(fileName))), info.getCustomFlags(), info);
                if (!info.canReadFile1(stream.getDataAppVersionCode())) {
                    callback.setDefaults();
                    stream.close();
                    stream = null;
                    context.deleteFile(fileName);
                    return false;
                }
                else {
                    callback.readObjects(stream);
                    return true;
                }
            }
            catch (Exception e) {
                LogUtils.e(TAG, "Exception while reading " + fileName, e);
                callback.setDefaults();
                return false;
            }
            finally {
                if (stream != null)
                    stream.close();
            }
        }
    }

    public static boolean writeObjsToFile(Context context, FileObjsStaticInfo info, WriteObjsCallback callback) {
        final boolean ret;
        synchronized (info.getLock()) {
            ApiDataOutputStreamWrp stream = null;
            String fileNameTmp = info.getFileName() + TMP_FILE_POSTFIX;
            try {
                stream = new ApiDataOutputStreamWrp(new DataOutputStream(new BufferedOutputStream(
                        context.openFileOutput(fileNameTmp, Context.MODE_PRIVATE))));
                callback.writeObjects(stream);
            }
            catch (Exception e) {
                LogUtils.e(TAG, "Exception while writing " + info.getFileName(), e);
                return false;
            }
            finally {
                if (stream != null)
                    stream.close();
            }

            File tmpFile = context.getFileStreamPath(fileNameTmp);
            File origFile = context.getFileStreamPath(info.getFileName());

            if (origFile.exists())
                origFile.delete();
            ret = tmpFile.renameTo(origFile);
        }
        GlobalContextLib.get().requestGoogleBackupIfNeeded(info.getFileName());
        return ret;
    }

    public static void writeObjsToFileAsync(final Context context, final FileObjsStaticInfo info, final WriteObjsCallback callback) {
        Thread t = new Thread(new Runnable() {
            public void run() {
                writeObjsToFile(context, info, callback);
            }
        });
        t.start();
    }

    public static class FileObjsStaticInfo implements ApiDataAppVersionCodeLegacyResolver {
        private final Object lock;
        private final String fileName;
        private final int minReadAppVersionCode;
        private final int customFlags;

        public FileObjsStaticInfo(Object lock, String fileName) {
            this(lock, fileName, Integer.MIN_VALUE);
        }

        public FileObjsStaticInfo(Object lock, String fileName, int minReadAppVersionCode) {
            this(lock, fileName, minReadAppVersionCode, ApiDataInputOutputBase.FLAG_NONE);
        }

        public FileObjsStaticInfo(Object lock, String fileName, int minReadAppVersionCode, int customFlags) {
            this.lock = lock;
            this.fileName = fileName;
            this.minReadAppVersionCode = minReadAppVersionCode;
            this.customFlags = customFlags;
        }

        public Object getLock() {
            return this.lock;
        }

        public String getFileName() {
            return this.fileName;
        }

        public int getCustomFlags() {
            return customFlags;
        }

        public boolean canReadFile1(int fileAppVersionCode) {
            return fileAppVersionCode >= minReadAppVersionCode;
        }

        @Override
        public int resolveAppVersionCodeLegacy(int legacyDataVersion) {
            return 0;
        }

        public FileObjsStaticInfo createPortableInfoForWriting() {
            return new FileObjsStaticInfo(getLock(), getFileName() + ".port", minReadAppVersionCode, customFlags | ApiDataInputOutputBase.FLAG_PORTABLE);
        }
    }


    public interface ReadObjsCallback {
        void readObjects(ApiDataInput d);
        void setDefaults();
    }

    public interface WriteObjsCallback {
        void writeObjects(ApiDataOutput d);
    }

    public interface FileObjsCallback extends WriteObjsCallback, ReadObjsCallback {

    }
}
