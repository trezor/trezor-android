package com.satoshilabs.trezor;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbRequest;
import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.satoshilabs.trezor.protobuf.TrezorMessage;
import com.satoshilabs.trezor.protobuf.TrezorMessage.MessageType;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class TrezorDevice {

    private UsbDevice device;
    private UsbDeviceConnection conn;
    private String serial;
    private UsbEndpoint epr, epw;

    public TrezorDevice(UsbDevice device, UsbDeviceConnection conn, UsbInterface iface, UsbEndpoint epr, UsbEndpoint epw) {
        this.device = device;
        this.conn = conn;
        this.epr = epr;
        this.epw = epw;
        this.serial = this.conn.getSerial();
    }

    @Override
    public String toString() {
        return "TREZOR(path:" + this.device.getDeviceName() + " serial:" + this.serial + ")";
    }

    private String bufferToString(byte[] buffer) {
        String s = "";
        for (byte b : buffer) {
            s += String.format(" %02x", b);
        }
        return s;
    }

    private void messageWrite(Message msg) {
        int msg_size = msg.getSerializedSize();
        String msg_name = msg.getClass().getSimpleName();
        int msg_id = MessageType.valueOf("MessageType_" + msg_name).getNumber();
        Log.i("TrezorDevice.messageWrite()", String.format("Got message: %s (%d bytes)", msg_name, msg_size));
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
        Log.i("TrezorDevice.messageWrite()", String.format("Writing %d chunks", chunks));
        data.rewind();
        for (int i = 0; i < chunks; i++) {
            byte[] buffer = new byte[64];
            buffer[0] = (byte) '?';
            data.get(buffer, 1, 63);
            Log.i("TrezorDevice.messageWrite()", "chunk:" + bufferToString(buffer));
            request.queue(ByteBuffer.wrap(buffer), 64);
            conn.requestWait();
        }
    }

    private Message parseMessageFromBytes(MessageType type, byte[] data) {
        Message msg = null;
        Log.i("TrezorDevice.parseMessageFromBytes()", String.format("Parsing %s (%d bytes):", type, data.length));
        Log.i("TrezorDevice.parseMessageFromBytes()", "data:" + bufferToString(data));
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
            Log.e("TrezorDevice.parseMessageFromBytes()", e.toString());
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
            Log.i("TrezorDevice.messageRead()", String.format("Read chunk: %d bytes", b.length));
            Log.i("TrezorDevice.messageRead()", "chunk:" + bufferToString(b));
            if (b.length < 9) continue;
            if (b[0] != (byte) '?' || b[1] != (byte) '#' || b[2] != (byte) '#') continue;
            type = MessageType.valueOf((b[3] & 0xFF << 8) + b[4] & 0xFF);
            msg_size = (b[5] & 0xFF << 8) + (b[6] & 0xFF << 8) + (b[7] & 0xFF << 8) + b[8] & 0xFF;
            data.put(b, 9, b.length - 9);
            break;
        }
        while (data.position() < msg_size) {
            request.queue(buffer, 64);
            conn.requestWait();
            byte[] b = buffer.array();
            Log.i("TrezorDevice.messageRead()", String.format("Read chunk (cont): %d bytes", b.length));
            Log.i("TrezorDevice.messageRead()", "chunk:" + bufferToString(b));
            if (b[0] != (byte) '?') continue;
            data.put(b, 1, b.length - 1);
        }
        return parseMessageFromBytes(type, Arrays.copyOfRange(data.array(), 0, msg_size));
    }

    public Message send(Message msg) {
        messageWrite(msg);
        return messageRead();
    }

}
