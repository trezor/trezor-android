package com.satoshilabs.trezor.app.db;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import com.circlegate.liban.base.ApiBase.ApiCreator;
import com.circlegate.liban.base.ApiBase.ApiParcelable;
import com.circlegate.liban.base.ApiDataIO.ApiDataInput;
import com.circlegate.liban.base.ApiDataIO.ApiDataOutput;
import com.circlegate.liban.base.BaseBroadcastReceivers.BaseLocalReceiver;
import com.circlegate.liban.utils.EqualsUtils;
import com.circlegate.liban.utils.FileUtils;
import com.circlegate.liban.utils.FileUtils.FileObjsCallback;
import com.circlegate.liban.utils.FileUtils.FileObjsStaticInfo;
import com.google.protobuf.InvalidProtocolBufferException;
import com.satoshilabs.trezor.app.common.GlobalContext;
import com.satoshilabs.trezor.lib.TrezorManager;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.Features;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.MessageType;
import tinyguava.ImmutableList;

public class CommonDb {

    private final FileObjsStaticInfo info = new FileObjsStaticInfo(getLock(), "CommonDb.obj");
    private final Context context;

    // Persistent data
    private ImmutableList<InitializedTrezor> trezors = ImmutableList.of();
    private String lastSelectedDeviceId = "";

    private final FileObjsCallback fileObjsCallback = new FileObjsCallback() {
        public void setDefaults() {
            trezors = ImmutableList.of();
            lastSelectedDeviceId = "";
        }

        public void readObjects(ApiDataInput d) {
            trezors = d.readImmutableList(InitializedTrezor.CREATOR);
            lastSelectedDeviceId = d.readString();
        }

        public void writeObjects(ApiDataOutput d) {
            d.write(trezors, 0);
            d.write(lastSelectedDeviceId);
        }
    };

    public CommonDb(GlobalContext gct) {
        this.context = gct.getAndroidContext();
        FileUtils.readObjsFromFile(context, info, fileObjsCallback);
    }

    private void flushAsync() {
        //Zatim uplne zaremovano ukladani
        //FileUtils.writeObjsToFileAsync(context, info, fileObjsCallback);
    }


    //
    // GETTERS
    //

    public Object getLock() {
        return this;
    }

    public synchronized ImmutableList<InitializedTrezor> getTrezors() {
        return this.trezors;
    }

    public synchronized String getLastSelectedDeviceId() {
        return this.lastSelectedDeviceId;
    }


    //
    // SETTERS
    //

    public synchronized void addOrUpdateCurrentTrezor(InitializedTrezor trezor) {
        String deviceId = trezor.getFeatures().getDeviceId();
        if (!TextUtils.isEmpty(deviceId)) {
            boolean found = false;
            boolean labelsChanged = false;

            ImmutableList.Builder<InitializedTrezor> newTrezors = ImmutableList.builder();
            for (InitializedTrezor old : this.trezors) {
                if (old.getFeatures().getDeviceId().equals(deviceId)) {
                    found = true;
                    labelsChanged = !EqualsUtils.equalsCheckNull(old.getFeatures().getLabel(), trezor.getFeatures().getLabel());
                    newTrezors.add(trezor);
                }
                else
                    newTrezors.add(old);
            }

            if (!found) {
                newTrezors.add(trezor);
            }

            this.trezors = newTrezors.build();

            final boolean lastSelectedDeviceIdChanged = !EqualsUtils.equalsCheckNull(this.lastSelectedDeviceId, deviceId);
            if (lastSelectedDeviceIdChanged) {
                this.lastSelectedDeviceId = deviceId;
            }

            flushAsync();
            TrezorListChangedReceiver.sendBroadcast(context, !found || labelsChanged, lastSelectedDeviceIdChanged);
        }
    }

    public synchronized void removeTrezor(InitializedTrezor trezor) {
        final String deviceId = trezor.getFeatures().getDeviceId();
        boolean found = false;

        ImmutableList.Builder<InitializedTrezor> newTrezors = ImmutableList.builder();
        for (InitializedTrezor old : this.trezors) {
            if (old.getFeatures().getDeviceId().equals(deviceId))
                found = true;
            else
                newTrezors.add(old);
        }

        if (found) {
            this.trezors = newTrezors.build();

            final boolean lastSelectedDeviceIdChanged = EqualsUtils.equalsCheckNull(lastSelectedDeviceId, deviceId);
            if (lastSelectedDeviceIdChanged) {
                lastSelectedDeviceId = this.trezors.isEmpty() ? "" : this.trezors.get(0).getFeatures().getDeviceId();
            }

            flushAsync();
            TrezorListChangedReceiver.sendBroadcast(context, true, lastSelectedDeviceIdChanged);
        }
    }

    /**
     * Zatim si navenek nechceme trezory pamatovat... pri kazdem odpojeni trezoru vsechny zapomeneme
     */
    public synchronized void removeAllTrezors() {
        if (!trezors.isEmpty()) {
            trezors = ImmutableList.of();

            boolean lastSelectedDeviceIdChanged = false;
            if (!EqualsUtils.equalsCheckNull(lastSelectedDeviceId, "")) {
                lastSelectedDeviceId = "";
                lastSelectedDeviceIdChanged = true;
            }

            flushAsync();
            TrezorListChangedReceiver.sendBroadcast(context, true, lastSelectedDeviceIdChanged);
        }
    }

    public synchronized void setLastSelectedDeviceId(String deviceId) {
        if (!EqualsUtils.equalsCheckNull(this.lastSelectedDeviceId, deviceId)) {
            this.lastSelectedDeviceId = deviceId;
            flushAsync();
            TrezorListChangedReceiver.sendBroadcast(context, false, true);
        }
    }


    //
    // INNER CLASSES
    //

    public static class InitializedTrezor extends ApiParcelable {
        private final Features features;
        //private final PublicKey publicKey;

        public InitializedTrezor(Features features/*, PublicKey publicKey*/) {
            this.features = features;
            //this.publicKey = publicKey;
        }

        public InitializedTrezor(ApiDataInput d) {
            try {
                this.features = (Features) TrezorManager.parseMessageFromBytes(MessageType.MessageType_Features, d.readBytes());
                //this.publicKey = (PublicKey)TrezorManager.parseMessageFromBytes(MessageType.MessageType_PublicKey, d.readBytes());
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void save(ApiDataOutput d, int flags) {
            d.write(features.toByteArray());
            //d.write(publicKey.toByteArray());
        }

        public Features getFeatures() {
            return this.features;
        }

//        public PublicKey getPublicKey() {
//            return this.publicKey;
//        }

        public static final ApiCreator<InitializedTrezor> CREATOR = new ApiCreator<InitializedTrezor>() {
            public InitializedTrezor create(ApiDataInput d) { return new InitializedTrezor(d); }
            public InitializedTrezor[] newArray(int size) { return new InitializedTrezor[size]; }
        };
    }

    public static abstract class TrezorListChangedReceiver extends BaseLocalReceiver {
        private static final String ACTION = TrezorListChangedReceiver.class.getName() + ".ACTION";

        public TrezorListChangedReceiver() {
            super(ACTION);
        }

        public static void sendBroadcast(Context context, boolean listChanged, boolean lastSelectedDeviceIdChanged) {
            Intent intent = new Intent(ACTION)
                    .putExtra("listChanged", listChanged)
                    .putExtra("lastSelectedDeviceIdChanged", lastSelectedDeviceIdChanged);
            sendBroadcast(context, intent);
        }

        @Override
        public void onReceiveRegistered(Context context, Intent intent) {
            onTrezorListChanged(
                    intent.getBooleanExtra("listChanged", false),
                    intent.getBooleanExtra("lastSelectedDeviceIdChanged", false)
            );
        }

        /**
         * @param listChanged - pokud doslo ke zmene v samotnem seznamu (pridani/odebrani), nebo doslo ke zmene labelu nektereho trezoru
         * @param lastSelectedDeviceIdChanged
         */
        public abstract void onTrezorListChanged(boolean listChanged, boolean lastSelectedDeviceIdChanged);
    }
}
