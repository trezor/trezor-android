package com.satoshilabs.trezor.app.common;

import android.content.Context;
import android.content.res.AssetManager;

import com.circlegate.liban.base.GlobalContextLib;
import com.circlegate.liban.task.TaskCommon.AsyncTaskParam;
import com.circlegate.liban.task.TaskCommon.EmptyTaskListener;
import com.circlegate.liban.task.TaskErrors.TaskException;
import com.circlegate.liban.task.TaskInterfaces.ITask;
import com.circlegate.liban.task.TaskInterfaces.ITaskContext;
import com.circlegate.liban.utils.LogUtils;
import tinyguava.ImmutableList;
import com.satoshilabs.trezor.app.common.TrezorTasks.TrezorTaskParam;
import com.satoshilabs.trezor.app.db.CommonDb;
import com.satoshilabs.trezor.lib.TrezorManager;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.Features;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

public class GlobalContext extends GlobalContextLib {
    private static final String TAG = GlobalContext.class.getSimpleName();

    public static final int RQC_ENTER_PIN = 701;
    public static final int RQC_PICK_HOMESCREEN = 702;
    public static final int RQC_PICK_CUSTOM_IMAGE = 703;
    public static final int RQC_ENTER_PASSPHRASE = 704;
    //public static final int RQC_INIT_TREZOR = 705;

    private static final String FIRMWARE_DIR = "firmware";
    private static final String FIRMWARE_FILE_PREFIX = "trezor-";
    private static final String FIRMWARE_FILE_POSTFIX = ".bin";

    private final TrezorManager trezorManager;

    private CommonDb commonDb;
    private FirmwareVersion firmwareVersion;

    static void init(Context context) {
        GlobalContextLib.init(new GlobalContext(context));
    }

    public static GlobalContext get() {
        return (GlobalContext)GlobalContextLib.get();
    }

    private GlobalContext(Context context) {
        super(context);
        LogUtils.setLoggingEnabled(!getAppIsInProductionMode());
        this.trezorManager = new TrezorManager(context);
    }

    public TrezorManager getTrezorManager() {
        return trezorManager;
    }


    @Override
    public void requestGoogleBackupIfNeeded(String changedDbFileName) {
    }

    @Override
    public String getCurrentLangAbbrev() {
        return Locale.getDefault().getLanguage();
    }

    public synchronized CommonDb getCommonDb() {
        if (this.commonDb == null) {
            LogUtils.d(TAG, "Before creating CommonDb");
            this.commonDb = new CommonDb(this);
            LogUtils.d(TAG, "After creating CommonDb");
        }
        return this.commonDb;
    }

    public synchronized FirmwareVersion getBundledFirmwareVersion() {
        if (this.firmwareVersion == null) {
            LogUtils.d(TAG, "Before loading bundled firmware version");
            BufferedReader changelogReader = null;
            BufferedReader fingerprintReader = null;

            try {
                changelogReader = new BufferedReader(new InputStreamReader(getAndroidContext().getAssets().open(FIRMWARE_DIR + File.separatorChar + "changelog.txt", AssetManager.ACCESS_BUFFER), "UTF-8"));
                ImmutableList.Builder<String> changelog = ImmutableList.builder();
                String line;

                while ((line = changelogReader.readLine()) != null) {
                    changelog.add(line);
                }

                fingerprintReader = new BufferedReader(new InputStreamReader(getAndroidContext().getAssets().open(FIRMWARE_DIR + File.separatorChar + "fingerprint.txt", AssetManager.ACCESS_BUFFER), "UTF-8"));
                StringBuilder fingerprint = new StringBuilder();

                while ((line = fingerprintReader.readLine()) != null) {
                    if (fingerprint.length() > 0)
                        fingerprint.append('\n');
                    fingerprint.append(line);
                }

                String[] files = getAndroidContext().getAssets().list(FIRMWARE_DIR);

                for (String f : files) {
                    if (f.startsWith(FIRMWARE_FILE_PREFIX) && f.endsWith(FIRMWARE_FILE_POSTFIX)) {
                        String[] versionStr = f.substring(FIRMWARE_FILE_PREFIX.length(), f.length() - FIRMWARE_FILE_POSTFIX.length()).split("\\.");
                        if (versionStr.length != 3)
                            throw new RuntimeException("Wrong firmware file name: " + f);
                        this.firmwareVersion = new FirmwareVersion(Integer.parseInt(versionStr[0]), Integer.parseInt(versionStr[1]), Integer.parseInt(versionStr[2]), fingerprint.toString(), changelog.build());
                        break;
                    }
                }
                if (this.firmwareVersion == null)
                    throw new RuntimeException("Firmware file not found!");
            }
            catch (Exception e) {
                LogUtils.e(TAG, "getBundledFirmwareVersion", e);
                this.firmwareVersion = FirmwareVersion.INVALID;
            }
            finally {
                if (changelogReader != null) {
                    try {
                        changelogReader.close();
                    } catch (IOException e) {
                        LogUtils.e(TAG, "getBundledFirmwareVersion (closing changelogReader)", e);
                    }
                }
                if (fingerprintReader != null) {
                    try {
                        fingerprintReader.close();
                    } catch (IOException e) {
                        LogUtils.e(TAG, "getBundledFirmwareVersion (closing fingerprintReader)", e);
                    }
                }
            }
            LogUtils.d(TAG, "After loading bundled firmware version");
        }
        return this.firmwareVersion;
    }

    public synchronized InputStream openStreamBundledFirmware() throws IOException {
        FirmwareVersion f = getBundledFirmwareVersion();
        return getAndroidContext().getAssets().open(FIRMWARE_DIR + File.separatorChar + FIRMWARE_FILE_PREFIX + f.major + "." + f.minor + "." + f.patch + FIRMWARE_FILE_POSTFIX);
    }

    public synchronized void executeDisconnectTrezorTask() {
        // schvalne resime takto globalne pres taskExecutor, protoze kazdopadne nechceme, aby se task (cekajici ve fronte) zrusil napr. pri zavreni aktivity...
        getTaskExecutor().executeTask("TASK_DISCONNECT_TREZOR_GCT",
                new AsyncTaskParam(TrezorTaskParam.SERIAL_EXECUTION_KEY_TREZOR) {
                    @Override
                    public boolean isExecutionInParallelForbidden(ITaskContext context) {
                        return true;
                    }

                    @Override
                    public void execute(ITaskContext context, ITask task) throws TaskException {
                        getTrezorManager().closeDeviceConnection();
                    }
                },
                null,
                false,
                new EmptyTaskListener(),
                null);
    }


    public static class FirmwareVersion {
        public static final FirmwareVersion INVALID = new FirmwareVersion(0, 0, 0, "", ImmutableList.<String>of());

        public final int major;
        public final int minor;
        public final int patch;
        public final String fingerPrint;
        public final ImmutableList<String> changeLog;

        public FirmwareVersion(int major, int minor, int patch, String fingerPrint, ImmutableList<String> changeLog) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
            this.fingerPrint = fingerPrint;
            this.changeLog = changeLog;
        }

        public boolean isNewerThan(Features f) {
            // Odremovat!
            //return true;
            if (this.major != f.getMajorVersion())
                return this.major > f.getMajorVersion();
            else if (this.minor != f.getMinorVersion())
                return this.minor > f.getMinorVersion();
            else
                return this.patch > f.getPatchVersion();
        }

        public boolean isValid() {
            return major != 0 || minor != 0 || patch != 0;
        }
    }
}
