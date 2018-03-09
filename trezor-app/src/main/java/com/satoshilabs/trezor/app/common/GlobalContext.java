package com.satoshilabs.trezor.app.common;

import android.content.Context;
import android.content.res.AssetManager;
import android.support.annotation.NonNull;

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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

public class GlobalContext extends GlobalContextLib {
    private static final String TAG = GlobalContext.class.getSimpleName();

    public static final int RQC_ENTER_PIN = 701;
    public static final int RQC_PICK_HOMESCREEN = 702;
    public static final int RQC_PICK_CUSTOM_IMAGE = 703;
    public static final int RQC_ENTER_PASSPHRASE = 704;
    //public static final int RQC_INIT_TREZOR = 705;

    private static final String FIRMWARE_DIR_V1 = "firmware";
    private static final String FIRMWARE_DIR_V2 = "firmware-v2";
    private static final String FIRMWARE_FILE_PREFIX = "trezor-";
    private static final String FIRMWARE_FILE_POSTFIX = ".bin";

    private final TrezorManager trezorManager;

    private CommonDb commonDb;
    private FirmwareReleases firmwareReleases;

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


    public synchronized FirmwareReleases getFirmwareReleases(boolean isV2) {
        if (this.firmwareReleases == null || this.firmwareReleases.isV2 != isV2) {
            this.firmwareReleases = null;
            LogUtils.d(TAG, "Before loading releases.json, isV2: " + isV2);
            BufferedReader releasesReader = null;
            final String firmwareDir = isV2 ? FIRMWARE_DIR_V2 : FIRMWARE_DIR_V1;

            try {
                releasesReader = new BufferedReader(new InputStreamReader(getAndroidContext().getAssets().open(firmwareDir + File.separatorChar + "releases.json", AssetManager.ACCESS_BUFFER), "UTF-8"));
                StringBuilder bRelease = new StringBuilder();

                String line;
                while ((line = releasesReader.readLine()) != null) {
                    bRelease.append(line).append('\n');
                }

                JSONArray jReleases = new JSONArray(bRelease.toString());
                ArrayList<FirmwareRelease> versions = new ArrayList<>();

                for (int i = 0; i < jReleases.length(); i++) {
                    JSONObject jRelease = jReleases.getJSONObject(i);
                    JSONArray jVersion = jRelease.getJSONArray("version");

                    versions.add(new FirmwareRelease(
                            jRelease.getBoolean("required"),
                            new FirmwareVersion(jVersion.getInt(0), jVersion.getInt(1), jVersion.getInt(2)),
                            jRelease.optString("fingerprint", ""),
                            jRelease.getString("changelog")));
                }

                Collections.sort(versions, Collections.reverseOrder());
                this.firmwareReleases = new FirmwareReleases(isV2, ImmutableList.copyOf(versions));
            }
            catch (Exception e) {
                LogUtils.e(TAG, "getBundledFirmwareVersion", e);
                this.firmwareReleases = FirmwareReleases.createInvalid(isV2);
            }
            finally {
                if (releasesReader != null) {
                    try {
                        releasesReader.close();
                    } catch (IOException e) {
                        LogUtils.e(TAG, "getBundledFirmwareVersion (closing releaseReader)", e);
                    }
                }
            }
            LogUtils.d(TAG, "After loading bundled firmware version");
        }
        return this.firmwareReleases;
    }

    public synchronized InputStream openStreamBundledFirmware(boolean isV2) throws IOException {
        FirmwareRelease f = getFirmwareReleases(isV2).getNewest();
        return getAndroidContext().getAssets().open((isV2 ? FIRMWARE_DIR_V2 : FIRMWARE_DIR_V1) + File.separatorChar + FIRMWARE_FILE_PREFIX + f.version.major + "." + f.version.minor + "." + f.version.patch + FIRMWARE_FILE_POSTFIX);
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


    public static class FirmwareReleases {
        public static final FirmwareReleases createInvalid(boolean isV2) {
            return new FirmwareReleases(isV2, ImmutableList.<FirmwareRelease>of());
        }

        public final boolean isV2;
        public final ImmutableList<FirmwareRelease> releasesDesc; // sorted from the newest to oldest

        // upraveno: versionsDesc
        public FirmwareReleases(boolean isV2, ImmutableList<FirmwareRelease> releasesDesc) {
            this.isV2 = isV2;
            this.releasesDesc = releasesDesc;
        }

        public FirmwareRelease getNewest() {
            return releasesDesc.size() > 0 ? releasesDesc.get(0) : null;
        }

        public boolean isUpdateRequired(FirmwareVersion deviceVersion) {
            if (!deviceVersion.isValid())
                return false;

            for (FirmwareRelease r : releasesDesc) {
                if (!r.version.isNewerThan(deviceVersion))
                    return false;

                if (r.required)
                    return true;
            }
            return false;
        }
    }

    public static class FirmwareRelease implements Comparable<FirmwareRelease> {
        public final boolean required;
        public final FirmwareVersion version;
        public final String fingerprint;
        public final String changelog;

        public FirmwareRelease(boolean required, FirmwareVersion version, String fingerprint, String changelog) {
            this.required = required;
            this.version = version;
            this.fingerprint = fingerprint;
            this.changelog = changelog;
        }

        @Override
        public int compareTo(@NonNull FirmwareRelease v) {
            return version.compareTo(v.version);
        }

        @Override
        public String toString() {
            return version.toString();
        }
    }

    public static class FirmwareVersion implements Comparable<FirmwareVersion> {
        public static final FirmwareVersion INVALID = new FirmwareVersion(0, 0, 0);

        public final int major;
        public final int minor;
        public final int patch;

        public static FirmwareVersion create(Features features) {
            if (features.getBootloaderMode()) {
                return (features.hasFwMajor() && features.hasFwMinor() && features.hasFwPatch() && features.getFwMajor() > 0) ?
                        new FirmwareVersion(features.getFwMajor(), features.getFwMinor(), features.getFwPatch()) : FirmwareVersion.INVALID;
            }
            else
                return new FirmwareVersion(features.getMajorVersion(), features.getMinorVersion(), features.getPatchVersion());
        }

        public FirmwareVersion(int major, int minor, int patch) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
        }

        public boolean isValid() {
            return major != 0 || minor != 0 || patch != 0;
        }

        public boolean isNewerThan(FirmwareVersion v) {
            return compareTo(v) > 0;
        }

        @Override
        public int compareTo(@NonNull FirmwareVersion v) {
            if (this.major != v.major)
                return this.major < v.major ? -1 : 1;
            else if (this.minor != v.minor)
                return this.minor < v.minor ? -1 : 1;
            else if (this.patch != v.patch)
                return this.patch < v.patch ? -1 : 1;
            else
                return 0;
        }

        @Override
        public String toString() {
            return major + "." + minor + "." + patch;
        }
    }
}
