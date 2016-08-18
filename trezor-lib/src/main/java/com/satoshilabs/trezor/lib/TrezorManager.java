package com.satoshilabs.trezor.lib;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;

import com.circlegate.liban.base.BaseBroadcastReceivers.BaseGlobalReceiver;
import com.circlegate.liban.utils.LogUtils;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.MessageType;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;

/**
 * ATTENTION!
 * All instance methods are synchronized and can possibly lock for a long time (like when Trezor is waiting for user to press a button
 * Methods must be called from a background thread!
 */
public class TrezorManager {
    private static final String TAG = TrezorManager.class.getSimpleName();

    private final Context context;
    private final UsbManager usbManager;

    private UsbDevice deviceWithoutPermission;
    private TrezorDevice device; // lazy loaded

    public TrezorManager(Context context) {
        this.context = context.getApplicationContext();
        this.usbManager = (UsbManager)context.getSystemService(Context.USB_SERVICE);
    }


    public synchronized boolean tryConnectDevice() {
        return tryGetDevice() != null;
    }

    public synchronized void closeDeviceConnection() {
        if (device != null) {
            LogUtils.d(TAG, "closeDeviceConnection: closing");
            device.close();
            device = null;
        }
        else
            LogUtils.d(TAG, "closeDeviceConnection: no device connected now");
    }

    public synchronized boolean hasDeviceWithoutPermission(boolean refreshDevices) {
        if (refreshDevices)
            tryGetDevice();
        return this.device == null && this.deviceWithoutPermission != null;
    }

    public synchronized void requestDevicePermissionIfCan(boolean refreshDevices) {
        if (hasDeviceWithoutPermission(refreshDevices)) {
            usbManager.requestPermission(deviceWithoutPermission, PendingIntent.getBroadcast(context, 0, new Intent(UsbPermissionReceiver.ACTION), 0));
        }
    }

    public synchronized Message sendMessage(Message message) throws TrezorException {
        TrezorDevice device = tryGetDevice();
        if (device == null)
            throw new TrezorException(deviceWithoutPermission != null ? TrezorException.TYPE_NEEDS_PERMISSION : TrezorException.TYPE_NOT_CONNECTED);
        else {
            try {
                return device.sendMessage(message);
            }
            catch (Exception e) {
                LogUtils.e(TAG, "sendMessage: device.sendMessage thrown exception:", e);
                throw new TrezorException(TrezorException.TYPE_COMMUNICATION_ERROR, e);
            }
        }
    }


    public static Message parseMessageFromBytes(MessageType type, byte[] data) throws InvalidProtocolBufferException {
        //LogUtils.i(TAG, String.format("parseMessageFromBytes: Parsing %s (%d bytes):", type, data.length));
        //LogUtils.i(TAG, "parseMessageFromBytes: data:" + bufferToString(data));

        try {
            String className = com.satoshilabs.trezor.lib.protobuf.TrezorMessage.class.getName() + "$" + type.name().replace("MessageType_", "");
            Class cls = Class.forName(className);
            Method method = cls.getDeclaredMethod("parseFrom", byte[].class);
            //noinspection PrimitiveArrayArgumentToVariableArgMethod
            return (Message)method.invoke(null, data); // TODO Doresit cachovani pro rychlejsi spousteni...
        }
        catch (Exception ex) {
            throw new InvalidProtocolBufferException("Exception while calling: parseMessageFromBytes for MessageType: " + type.name());
        }

    }



    //
    // PRIVATE
    //

    static boolean deviceIsTrezor(UsbDevice usbDevice) {
        return usbDevice.getVendorId() == 0x534c
                && usbDevice.getProductId() == 0x0001
                && usbDevice.getInterfaceCount() > 0;
    }

    private TrezorDevice tryGetDevice() {
        if (device == null) {
            LogUtils.i(TAG, "tryGetDevice: trying to find and open connection to TREZOR");
            HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();

            this.deviceWithoutPermission = null;

            for (UsbDevice usbDevice : deviceList.values()) {
                // check if the device is TREZOR
                if (!deviceIsTrezor(usbDevice))
                    continue;

                LogUtils.i(TAG, "tryGetDevice: TREZOR device found");

//                if (usbDevice.getInterfaceCount() <= 0) {
//                    LogUtils.i(TAG, "wrong number of interfaces!");
//                    continue;
//                }

                if (!usbManager.hasPermission(usbDevice)) {
                    if (this.deviceWithoutPermission == null)
                        this.deviceWithoutPermission = usbDevice;
                    continue;
                }

                // use first interface
                UsbInterface usbInterface = usbDevice.getInterface(0);
                // try to find read/write endpoints
                UsbEndpoint readEndpoint = null, writeEndpoint = null;
                for (int i = 0; i < usbInterface.getEndpointCount(); i++) {
                    UsbEndpoint ep = usbInterface.getEndpoint(i);
                    if (readEndpoint == null && ep.getType() == UsbConstants.USB_ENDPOINT_XFER_INT && ep.getAddress() == 0x81) { // number = 1 ; dir = USB_DIR_IN
                        readEndpoint = ep;
                        continue;
                    }
                    if (writeEndpoint == null && ep.getType() == UsbConstants.USB_ENDPOINT_XFER_INT && (ep.getAddress() == 0x01 || ep.getAddress() == 0x02)) { // number = 1 ; dir = USB_DIR_OUT
                        writeEndpoint = ep;
                        continue;
                    }
                }
                if (readEndpoint == null) {
                    LogUtils.e(TAG, "tryGetDevice: Could not find read endpoint");
                    continue;
                }
                if (writeEndpoint == null) {
                    LogUtils.e(TAG, "tryGetDevice: Could not find write endpoint");
                    continue;
                }
                if (readEndpoint.getMaxPacketSize() != 64) {
                    LogUtils.e(TAG, "tryGetDevice: Wrong packet size for read endpoint");
                    continue;
                }
                if (writeEndpoint.getMaxPacketSize() != 64) {
                    LogUtils.e(TAG, "tryGetDevice: Wrong packet size for write endpoint");
                    continue;
                }

                UsbDeviceConnection conn = usbManager.openDevice(usbDevice);
                if (conn == null) {
                    LogUtils.e(TAG, "tryGetDevice: could not open connection");
                    continue;
                } else {
                    if (!conn.claimInterface(usbInterface, true)) {
                        LogUtils.e(TAG, "tryGetDevice: could not claim interface");
                        continue;
                    } else {
                        this.deviceWithoutPermission = null;
                        this.device = new TrezorDevice(usbDevice.getDeviceName(), conn.getSerial(), conn, usbInterface, readEndpoint, writeEndpoint);
                        break;
                    }
                }
            }
            LogUtils.i(TAG, "tryGetDevice: " + (this.device == null ? "TREZOR device not found or failed to connect" : "TREZOR device successfully connected"));
        } else
            LogUtils.i(TAG, "tryGetDevice: using already connected device");
        return device;
    }

    private static String bytesToHex(ByteString bytes) {
        String hex = "";
        for (int j = 0; j < bytes.size(); j++) {
            hex += String.format("%02x", bytes.byteAt(j) & 0xFF);
        }
        return hex;
    }


    //
    // INNER CLASSES
    //

    private static class TrezorDevice {
        private static final String TAG = TrezorDevice.class.getSimpleName();

        private final String deviceName;
        private final String serial;

        // next fields are only valid until calling close()
        private UsbDeviceConnection usbConnection;
        private UsbInterface usbInterface;
        private UsbEndpoint readEndpoint;
        private UsbEndpoint writeEndpoint;

        public TrezorDevice(String deviceName, String serial, UsbDeviceConnection usbConnection, UsbInterface usbInterface, UsbEndpoint readEndpoint, UsbEndpoint writeEndpoint) {
            this.deviceName = deviceName;
            this.serial = serial;
            this.usbConnection = usbConnection;
            this.usbInterface = usbInterface;
            this.readEndpoint = readEndpoint;
            this.writeEndpoint = writeEndpoint;
        }

        @Override
        public String toString() {
            return "TREZOR(path:" + this.deviceName + " serial:" + this.serial + ")";
        }

        public Message sendMessage(Message msg) throws InvalidProtocolBufferException {
            if (usbConnection == null)
                throw new IllegalStateException(TAG + ": sendMessage: usbConnection already closed, cannot send message");

            messageWrite(msg);
            return messageRead();
        }

        public void close() {
            if (this.usbConnection != null) {
                try {
                    usbConnection.releaseInterface(usbInterface);
                }
                catch (Exception ex) {}
                try {
                    usbConnection.close();
                }
                catch (Exception ex) {}

                usbConnection = null;
                usbInterface = null;
                readEndpoint = null;
                writeEndpoint = null;
            }
        }


        //
        // PRIVATE
        //

        private void messageWrite(Message msg) {
            int msg_size = msg.getSerializedSize();
            String msg_name = msg.getClass().getSimpleName();
            int msg_id = MessageType.valueOf("MessageType_" + msg_name).getNumber();
            LogUtils.i(TAG, String.format("messageWrite: Got message: %s (%d bytes)", msg_name, msg_size));
            ByteBuffer data = ByteBuffer.allocate(msg_size + 1024); // 32768);
            data.put((byte) '#');
            data.put((byte) '#');
            data.put((byte) ((msg_id >> 8) & 0xFF));
            data.put((byte) (msg_id & 0xFF));
            data.put((byte) ((msg_size >> 24) & 0xFF));
            data.put((byte) ((msg_size >> 16) & 0xFF));
            data.put((byte) ((msg_size >> 8) & 0xFF));
            data.put((byte) (msg_size & 0xFF));
            data.put(msg.toByteArray());
            while (data.position() % 63 > 0) {
                data.put((byte) 0);
            }
            UsbRequest request = new UsbRequest();
            request.initialize(usbConnection, writeEndpoint);
            int chunks = data.position() / 63;
            LogUtils.i(TAG, String.format("messageWrite: Writing %d chunks", chunks));
            data.rewind();
            for (int i = 0; i < chunks; i++) {
                byte[] buffer = new byte[64];
                buffer[0] = (byte) '?';
                data.get(buffer, 1, 63);
                //LogUtils.d(TAG, "messageWrite: chunk: " + bufferToString(buffer));
                request.queue(ByteBuffer.wrap(buffer), 64);
                usbConnection.requestWait();
            }
        }

        private Message messageRead() throws InvalidProtocolBufferException {
            ByteBuffer data = null;//ByteBuffer.allocate(32768);
            ByteBuffer buffer = ByteBuffer.allocate(64);
            UsbRequest request = new UsbRequest();
            request.initialize(usbConnection, readEndpoint);
            MessageType type;
            int msg_size;
            int invalidChunksCounter = 0;

            for (; ; ) {
                request.queue(buffer, 64);
                usbConnection.requestWait();
                byte[] b = buffer.array();
                LogUtils.i(TAG, String.format("messageRead: Read chunk: %d bytes", b.length));
                //LogUtils.d(TAG, "messageRead: chunk: " + bufferToString(b));

                if (b.length < 9 || b[0] != (byte) '?' || b[1] != (byte) '#' || b[2] != (byte) '#') {
                    if (invalidChunksCounter++ > 5)
                        throw new InvalidProtocolBufferException("messageRead: too many invalid chunks");
                    continue;
                }
                if (b[0] != (byte) '?' || b[1] != (byte) '#' || b[2] != (byte) '#')
                    continue;

                type = MessageType.valueOf((((int)b[3] & 0xFF) << 8) + ((int)b[4] & 0xFF));
                msg_size = (((int)b[5] & 0xFF) << 24)
                        + (((int)b[6] & 0xFF) << 16)
                        + (((int)b[7] & 0xFF) << 8)
                        + ((int)b[8] & 0xFF);
                data = ByteBuffer.allocate(msg_size + 1024);
                data.put(b, 9, b.length - 9);
                break;
            }

            invalidChunksCounter = 0;

            while (data.position() < msg_size) {
                request.queue(buffer, 64);
                usbConnection.requestWait();
                byte[] b = buffer.array();
                LogUtils.i(TAG, String.format("messageRead: Read chunk (cont): %d bytes", b.length));
                //LogUtils.d(TAG, "messageRead: chunk: " + bufferToString(b));
                if (b[0] != (byte) '?') {
                    if (invalidChunksCounter++ > 5)
                        throw new InvalidProtocolBufferException("messageRead: too many invalid chunks (2)");
                    continue;
                }
                data.put(b, 1, b.length - 1);
            }

            byte[] msgData = Arrays.copyOfRange(data.array(), 0, msg_size);

            LogUtils.i(TAG, String.format("parseMessageFromBytes: Parsing %s (%d bytes):", type, msgData.length));
            //LogUtils.d(TAG, "parseMessageFromBytes: data:" + bufferToString(msgData));
            return parseMessageFromBytes(type, msgData);
        }

        private static String bufferToString(byte[] buffer) {
            String s = "";
            for (byte b : buffer) {
                s += String.format(" %02x", b);
            }
            return s;
        }
    }



    public static abstract class UsbPermissionReceiver extends BaseGlobalReceiver {
        public static final String ACTION = UsbPermissionReceiver.class.getName() + ".ACTION";

        public UsbPermissionReceiver() {
            super(ACTION);
        }

        @Override
        protected final void onReceiveRegistered(Context context, Intent intent) {
            boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
            onUsbPermissionResult(granted);
        }

        public abstract void onUsbPermissionResult(boolean granted);
    }


    public static abstract class TrezorConnectionChangedReceiver extends BaseGlobalReceiver {
        private static final String TAG = TrezorConnectionChangedReceiver.class.getSimpleName();

        private static IntentFilter createIntentFilter() {
            IntentFilter ret = new IntentFilter();
            ret.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
            ret.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
            return ret;
        }

        public TrezorConnectionChangedReceiver() {
            super(createIntentFilter());
        }

        @Override
        protected final void onReceiveRegistered(Context context, Intent intent) {
            LogUtils.d(TAG, "onReceiveRegistered: " + intent.getAction());

            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (device != null && deviceIsTrezor(device)) {
                if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(intent.getAction()))
                    onTrezorConnectionChanged(true);
                else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(intent.getAction())) {
                    onTrezorConnectionChanged(false);
                }
            }
        }

        public abstract void onTrezorConnectionChanged(boolean connected);
    }
}