package com.satoshilabs.trezor;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;

import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;

public class Trezor {

	public static Trezor getDevice(Context context) {
		UsbManager manager = (UsbManager)context.getSystemService(Context.USB_SERVICE);
		HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
		Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
		while (deviceIterator.hasNext()) {
			UsbDevice device = deviceIterator.next();
			// check if the device is TREZOR
			if (device.getVendorId() != 0x534c || device.getProductId() != 0x0001 || device.getInterfaceCount() < 1) {
				continue;
			}
			// use first interface
			UsbInterface iface = device.getInterface(0);
			// try to find read/write endpoints
			UsbEndpoint epr = null, epw = null;
			for (int i = 0; i < iface.getEndpointCount(); i++) {
				UsbEndpoint ep = iface.getEndpoint(i);
				if (epr == null && ep.getType() == UsbConstants.USB_ENDPOINT_XFER_INT && ep.getDirection() == UsbConstants.USB_DIR_OUT) {
					epr = ep;
					continue;
				}
				if (epw == null && ep.getType() == UsbConstants.USB_ENDPOINT_XFER_INT && ep.getDirection() == UsbConstants.USB_DIR_IN) {
					epw = ep;
					continue;
				}
			}
			// if both endpoints are found, open the device and return the class
			if (epr != null && epw != null) {
				UsbDeviceConnection conn = manager.openDevice(device);
				conn.claimInterface(iface,  true);
				return new Trezor(device, conn, iface, epr, epw);
			}
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
}
