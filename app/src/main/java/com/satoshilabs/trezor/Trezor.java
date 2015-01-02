package com.satoshilabs.trezor;

import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.satoshilabs.trezor.protobuf.TrezorMessage;
import com.satoshilabs.trezor.protobuf.TrezorMessage.MessageType;
import com.satoshilabs.trezor.protobuf.TrezorType;

import java.nio.ByteBuffer;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

/* Stub for empty TrezorGUICallback */
class _TrezorGUICallback implements TrezorGUICallback {
    public String PinMatrixRequest() {
        return "";
    }

    public String PassphraseRequest() {
        return "";
    }
}

public class Trezor {

    //	private UsbDevice device;
    private UsbDeviceConnection conn;
    private String serial;
    private UsbEndpoint epr, epw;
    private TrezorGUICallback guicall;

    public Trezor(TrezorGUICallback guicall, UsbDevice device, UsbDeviceConnection conn, UsbInterface iface, UsbEndpoint epr, UsbEndpoint epw) {
        this.guicall = guicall;
//		this.device = device;
        this.conn = conn;
        this.epr = epr;
        this.epw = epw;
        this.serial = this.conn.getSerial();
    }

    public static Trezor getDevice(Context context) {
        return getDevice(context, new _TrezorGUICallback());
    }

    public static Trezor getDevice(Context context, TrezorGUICallback guicall) {
        UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();

        for (UsbDevice device : deviceList.values()) {
            // check if the device is TREZOR
            if (device.getVendorId() != 0x534c || device.getProductId() != 0x0001) {
                continue;
            }
            Log.i("Trezor.getDevice()", "TREZOR device found");
            if (device.getInterfaceCount() < 1) {
                Log.e("Trezor.getDevice()", "Wrong interface count");
                continue;
            }
            // use first interface
            UsbInterface iface = device.getInterface(0);
            // try to find read/write endpoints
            UsbEndpoint epr = null, epw = null;
            for (int i = 0; i < iface.getEndpointCount(); i++) {
                UsbEndpoint ep = iface.getEndpoint(i);
                if (epr == null && ep.getType() == UsbConstants.USB_ENDPOINT_XFER_INT && ep.getAddress() == 0x81) { // number = 1 ; dir = USB_DIR_IN
                    epr = ep;
                    continue;
                }
                if (epw == null && ep.getType() == UsbConstants.USB_ENDPOINT_XFER_INT && ep.getAddress() == 0x01) { // number = 1 ; dir = USB_DIR_OUT
                    epw = ep;
                    continue;
                }
            }
            if (epr == null) {
                Log.e("Trezor.getDevice()", "Could not find read endpoint");
                continue;
            }
            if (epw == null) {
                Log.e("Trezor.getDevice()", "Could not find write endpoint");
                continue;
            }
            if (epr.getMaxPacketSize() != 64) {
                Log.e("Trezor.getDevice()", "Wrong packet size for read endpoint");
                continue;
            }
            if (epw.getMaxPacketSize() != 64) {
                Log.e("Trezor.getDevice()", "Wrong packet size for write endpoint");
                continue;
            }
            // try to open the device
            UsbDeviceConnection conn = manager.openDevice(device);
            if (conn == null) {
                Log.e("Trezor.getDevice()", "Could not open connection");
                continue;
            }
            boolean claimed = conn.claimInterface(iface, true);
            if (!claimed) {
                Log.e("Trezor.getDevice()", "Could not claim interface");
                continue;
            }
            // all OK - return the class
            return new Trezor(guicall, device, conn, iface, epr, epw);
        }
        return null;
    }

    private static String bytesToHex(byte[] bytes) {
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    @Override
    public String toString() {
        return "TREZOR(#" + this.serial + ")";
    }

    private void messageWrite(Message msg) {
        int msg_size = msg.getSerializedSize();
        String msg_name = msg.getClass().getSimpleName();
        int msg_id = MessageType.valueOf("MessageType_" + msg_name).getNumber();
        Log.i("Trezor.messageWrite()", String.format("Got message: %s (%d bytes)", msg_name, msg_size));
        ByteBuffer data = ByteBuffer.allocate(32768);
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
        request.initialize(conn, epw);
        int chunks = data.position() / 63;
        Log.i("Trezor.messageWrite()", String.format("Writing %d chunks", chunks));
        data.rewind();
        for (int i = 0; i < chunks; i++) {
            byte[] buffer = new byte[64];
            buffer[0] = (byte) '?';
            data.get(buffer, 1, 63);
            String s = "chunk:";
            for (int j = 0; j < 64; j++) {
                s += String.format(" %02x", buffer[j]);
            }
            Log.i("Trezor.messageWrite()", s);
            request.queue(ByteBuffer.wrap(buffer), 64);
            conn.requestWait();
        }
    }

    private Message parseMessageFromBytes(MessageType type, byte[] data) {
        Message msg = null;
        Log.i("Trezor.parseMessageFromBytes()", String.format("Parsing %s (%d bytes):", type, data.length));
        String s = "data:";
        for (byte b : data) {
            s += String.format(" %02x", b);
        }
        Log.i("Trezor.parseMessageFromBytes()", s);
        try {
            if (type.getNumber() == MessageType.MessageType_Success_VALUE)
                msg = TrezorMessage.Success.parseFrom(data);
            if (type.getNumber() == MessageType.MessageType_Failure_VALUE)
                msg = TrezorMessage.Failure.parseFrom(data);
            if (type.getNumber() == MessageType.MessageType_Entropy_VALUE)
                msg = TrezorMessage.Entropy.parseFrom(data);
            if (type.getNumber() == MessageType.MessageType_PublicKey_VALUE)
                msg = TrezorMessage.PublicKey.parseFrom(data);
            if (type.getNumber() == MessageType.MessageType_Features_VALUE)
                msg = TrezorMessage.Features.parseFrom(data);
            if (type.getNumber() == MessageType.MessageType_PinMatrixRequest_VALUE)
                msg = TrezorMessage.PinMatrixRequest.parseFrom(data);
            if (type.getNumber() == MessageType.MessageType_TxRequest_VALUE)
                msg = TrezorMessage.TxRequest.parseFrom(data);
            if (type.getNumber() == MessageType.MessageType_ButtonRequest_VALUE)
                msg = TrezorMessage.ButtonRequest.parseFrom(data);
            if (type.getNumber() == MessageType.MessageType_Address_VALUE)
                msg = TrezorMessage.Address.parseFrom(data);
            if (type.getNumber() == MessageType.MessageType_EntropyRequest_VALUE)
                msg = TrezorMessage.EntropyRequest.parseFrom(data);
            if (type.getNumber() == MessageType.MessageType_MessageSignature_VALUE)
                msg = TrezorMessage.MessageSignature.parseFrom(data);
            if (type.getNumber() == MessageType.MessageType_PassphraseRequest_VALUE)
                msg = TrezorMessage.PassphraseRequest.parseFrom(data);
            if (type.getNumber() == MessageType.MessageType_TxSize_VALUE)
                msg = TrezorMessage.TxSize.parseFrom(data);
            if (type.getNumber() == MessageType.MessageType_WordRequest_VALUE)
                msg = TrezorMessage.WordRequest.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            Log.e("Trezor.parseMessageFromBytes()", e.toString());
            return null;
        }
        return msg;
    }

    private Message messageRead() {
        ByteBuffer data = ByteBuffer.allocate(32768);
        ByteBuffer buffer = ByteBuffer.allocate(64);
        UsbRequest request = new UsbRequest();
        request.initialize(conn, epr);
        MessageType type;
        int msg_size;
        for (; ; ) {
            request.queue(buffer, 64);
            conn.requestWait();
            byte[] b = buffer.array();
            Log.i("Trezor.messageRead()", String.format("Read chunk: %d bytes", b.length));
            if (b.length < 9) continue;
            if (b[0] != (byte) '?' || b[1] != (byte) '#' || b[2] != (byte) '#') continue;
            type = MessageType.valueOf((b[3] << 8) + b[4]);
            msg_size = (b[5] << 8) + (b[6] << 8) + (b[7] << 8) + b[8];
            data.put(b, 9, b.length - 9);
            break;
        }
        while (data.position() < msg_size) {
            request.queue(buffer, 64);
            conn.requestWait();
            byte[] b = buffer.array();
            Log.i("Trezor.messageRead()", String.format("Read chunk (cont): %d bytes", b.length));
            if (b[0] != (byte) '?') continue;
            data.put(b, 1, b.length - 1);
        }
        return parseMessageFromBytes(type, Arrays.copyOfRange(data.array(), 0, msg_size));
    }

    public Message send(Message msg) {
        messageWrite(msg);
        return messageRead();
    }

    private String _get(Message resp) {
        switch (resp.getClass().getSimpleName()) {
            case "Success": {
                TrezorMessage.Success r = (TrezorMessage.Success) resp;
                if (r.hasMessage()) return r.getMessage();
                return "";
            }
            case "Failure":
                throw new IllegalStateException();
        /* User can catch ButtonRequest to Cancel by not calling _get */
            case "ButtonRequest":
                return _get(this.send(TrezorMessage.ButtonAck.newBuilder().build()));
            case "PinMatrixRequest":
                return _get(this.send(
                        TrezorMessage.PinMatrixAck.newBuilder().
                                setPin(this.guicall.PinMatrixRequest()).
                                build()));
            case "PassphraseRequest":
                return _get(this.send(
                        TrezorMessage.PassphraseAck.newBuilder().
                                setPassphrase(Normalizer.normalize(this.guicall.PassphraseRequest(), Normalizer.Form.NFKD)).
                                build()));
            case "PublicKey": {
                TrezorMessage.PublicKey r = (TrezorMessage.PublicKey) resp;
                if (!r.hasNode()) throw new IllegalArgumentException();
                TrezorType.HDNodeType N = r.getNode();
                String NodeStr = ((N.hasDepth()) ? N.getDepth() : "") + "%" +
                        ((N.hasFingerprint()) ? N.getFingerprint() : "") + "%" +
                        ((N.hasChildNum()) ? N.getChildNum() : "") + "%" +
                        ((N.hasChainCode()) ? bytesToHex(N.getChainCode().toByteArray()) : "") + "%" +
                        ((N.hasPrivateKey()) ? bytesToHex(N.getPrivateKey().toByteArray()) : "") + "%" +
                        ((N.hasPublicKey()) ? bytesToHex(N.getPublicKey().toByteArray()) : "") + "%" +
                        "";
                if (r.hasXpub())
                    return NodeStr + ":!:" + r.getXpub() + ":!:" +
                            bytesToHex(r.getXpubBytes().toByteArray());
                return NodeStr;
            }
        }
//		throw new IllegalArgumentException();
        return resp.getClass().getSimpleName();
    }

    public String MessagePing() {
        return _get(this.send(TrezorMessage.Ping.newBuilder().build()));
    }

    public String MessagePing(String msg) {
        return _get(this.send(
                TrezorMessage.Ping.newBuilder().
                        setMessage(msg).
                        build()));
    }

    public String MessagePing(String msg, Boolean ButtonProtection) {
        return _get(this.send(
                TrezorMessage.Ping.newBuilder().
                        setMessage(msg).
                        setButtonProtection(ButtonProtection).
                        build()));
    }

    public String MessageGetPublicKey() {
        return _get(this.send(
                TrezorMessage.GetPublicKey.newBuilder().
                        clearAddressN().
                        addAddressN(1).
                        build()));
    }

    public String MessageGetPublicKey(Integer[] addrn) {
        return _get(this.send(
                TrezorMessage.GetPublicKey.newBuilder().
                        clearAddressN().
                        addAllAddressN(Arrays.asList(addrn)).
                        build()));
    }

}
