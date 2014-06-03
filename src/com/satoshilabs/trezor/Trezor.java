package com.satoshilabs.trezor;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.satoshilabs.trezor.protobuf.TrezorMessage.*;

import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.util.Log;

public class Trezor {

	public static Trezor getDevice(Context context) {
		UsbManager manager = (UsbManager)context.getSystemService(Context.USB_SERVICE);
		HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
		Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

		while (deviceIterator.hasNext()) {
			UsbDevice device = deviceIterator.next();
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
			boolean claimed = conn.claimInterface(iface,  true);
			if (!claimed) {
				Log.e("Trezor.getDevice()", "Could not claim interface");
				continue;
			}
			// all OK - return the class
			return new Trezor(device, conn, iface, epr, epw);
		}
		return null;
	}

//	private UsbDevice device;
	private UsbDeviceConnection conn;
	private String serial;
	private UsbEndpoint epr, epw;

	public Trezor(UsbDevice device, UsbDeviceConnection conn, UsbInterface iface, UsbEndpoint epr, UsbEndpoint epw) {
//		this.device = device;
		this.conn = conn;
		this.epr = epr;
		this.epw = epw;
		this.serial = this.conn.getSerial();
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
		data.put((byte)'#');
		data.put((byte)'#');
		data.put((byte)((msg_id >> 8) & 0xFF));
		data.put((byte)(msg_id & 0xFF));
		data.put((byte)((msg_size >> 24) & 0xFF));
		data.put((byte)((msg_size >> 16) & 0xFF));
		data.put((byte)((msg_size >> 8) & 0xFF));
		data.put((byte)(msg_size & 0xFF));
		data.put(msg.toByteArray());
		while (data.position() % 63 > 0) {
			data.put((byte)0);
		}
		UsbRequest request = new UsbRequest();
		request.initialize(conn, epw);
		int chunks = data.position() / 63;
		Log.i("Trezor.messageWrite()", String.format("Writing %d chunks", chunks));
		data.rewind();
		for (int i = 0; i < chunks; i++) {
			byte[] buffer = new byte[64];
			buffer[0] = (byte)'?';
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
		for (int i = 0; i < data.length; i++) {
			s += String.format(" %02x", data[i]);
		}
		Log.i("Trezor.parseMessageFromBytes()", s);
		try {
			if (type.getNumber() == MessageType.MessageType_Success_VALUE) msg = Success.parseFrom(data);
			if (type.getNumber() == MessageType.MessageType_Failure_VALUE) msg = Failure.parseFrom(data);
			if (type.getNumber() == MessageType.MessageType_Entropy_VALUE) msg = Entropy.parseFrom(data);
			if (type.getNumber() == MessageType.MessageType_PublicKey_VALUE) msg = PublicKey.parseFrom(data);
			if (type.getNumber() == MessageType.MessageType_Features_VALUE) msg = Features.parseFrom(data);
			if (type.getNumber() == MessageType.MessageType_PinMatrixRequest_VALUE) msg = PinMatrixRequest.parseFrom(data);
			if (type.getNumber() == MessageType.MessageType_TxRequest_VALUE) msg = TxRequest.parseFrom(data);
			if (type.getNumber() == MessageType.MessageType_ButtonRequest_VALUE) msg = ButtonRequest.parseFrom(data);
			if (type.getNumber() == MessageType.MessageType_Address_VALUE) msg = Address.parseFrom(data);
			if (type.getNumber() == MessageType.MessageType_EntropyRequest_VALUE) msg = EntropyRequest.parseFrom(data);
			if (type.getNumber() == MessageType.MessageType_MessageSignature_VALUE) msg = MessageSignature.parseFrom(data);
			if (type.getNumber() == MessageType.MessageType_PassphraseRequest_VALUE) msg = PassphraseRequest.parseFrom(data);
			if (type.getNumber() == MessageType.MessageType_TxSize_VALUE) msg = TxSize.parseFrom(data);
			if (type.getNumber() == MessageType.MessageType_WordRequest_VALUE) msg = WordRequest.parseFrom(data);
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
		for (;;) {
			request.queue(buffer, 64);
			conn.requestWait();
			byte[] b = buffer.array();
			Log.i("Trezor.messageRead()", String.format("Read chunk: %d bytes", b.length));
			if (b.length < 9) continue;
			if (b[0] != (byte)'?' || b[1] != (byte)'#' || b[2] != (byte)'#') continue;
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
			if (b[0] != (byte)'?') continue;
			data.put(b, 1, b.length - 1);
		}
		return parseMessageFromBytes(type, Arrays.copyOfRange(data.array(), 0, msg_size));
	}

	public Message send(Message msg) {
		messageWrite(msg);
		return messageRead();
	}

}
