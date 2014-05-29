package com.satoshilabs.trezor;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;

import com.google.protobuf.GeneratedMessage;

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
				Log.e("Trezor", "Wrong interface count");
				continue;
			}
			// use first interface
			UsbInterface iface = device.getInterface(0);
			// try to find read/write endpoints
			UsbEndpoint epr = null, epw = null;
			for (int i = 0; i < iface.getEndpointCount(); i++) {
				UsbEndpoint ep = iface.getEndpoint(i);
				if (epr == null && ep.getType() == UsbConstants.USB_ENDPOINT_XFER_INT && ep.getAddress() == 0x01) { // number = 1 ; dir = USB_DIR_OUT
					epr = ep;
					continue;
				}
				if (epw == null && ep.getType() == UsbConstants.USB_ENDPOINT_XFER_INT && ep.getAddress() == 0x81) { // number = 1 ; dir = USB_DIR_IN
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

	private void intrWrite(byte[] data) {
		int len = epw.getMaxPacketSize();
		ByteBuffer buffer = ByteBuffer.allocate(len + 1);
		buffer.put(data);
		UsbRequest request = new UsbRequest();
		request.initialize(conn, epw);
		request.queue(buffer, len);
		conn.requestWait();
	}

	private byte[] intrRead() {
		int len = epr.getMaxPacketSize();
		ByteBuffer buffer = ByteBuffer.allocate(len + 1);
		UsbRequest request = new UsbRequest();
		request.initialize(conn, epr);
		request.queue(buffer, len);
		conn.requestWait();
		byte[] data = new byte[len];
		buffer.get(data);
		return data;
	}

	public GeneratedMessage send(GeneratedMessage msg) {
		// TODO: proper send/receive mechanism intrWrite and intrRead
		return msg;
	}

}
