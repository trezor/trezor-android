package com.satoshilabs.trezor;

import java.util.HashMap;
import java.util.Iterator;

import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

public class Trezor {

	public static Trezor getDevice(Context context) {
		UsbManager manager = (UsbManager)context.getSystemService(Context.USB_SERVICE);
		HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
		Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
		while (deviceIterator.hasNext()){
			UsbDevice device = deviceIterator.next();
			if (device.getVendorId() != 0x534c || device.getProductId() != 0x0001) {
				continue;
			}
			UsbDeviceConnection conn = manager.openDevice(device);
			UsbInterface iface = device.getInterface(0);
			return new Trezor(conn, iface);
		}
		return null;
	}

	private UsbDeviceConnection conn;
	private String serial;
	private UsbEndpoint epr, epw;

	public Trezor(UsbDeviceConnection conn, UsbInterface iface) {
		this.conn = conn;
		this.serial = conn.getSerial();
		this.epr = null;
		this.epw = null;
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
		if (epr == null) {
			throw new IllegalArgumentException("Could not find read endpoint");
		}
		if (epw == null) {
			throw new IllegalArgumentException("Could not find write endpoint");
		}
	}

	@Override
	public String toString() {
		return "TREZOR(#" + this.serial + ")";
	}
}
