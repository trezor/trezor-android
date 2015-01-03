package com.satoshilabs.trezor;

import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.util.HashMap;

public class TrezorEnumerator {

    public static HashMap<String, TrezorDevice> enumerate(Context context) {
        UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        HashMap<String, TrezorDevice> list = new HashMap<String, TrezorDevice>();

        for (UsbDevice device : deviceList.values()) {
            // check if the device is TREZOR
            if (device.getVendorId() != 0x534c || device.getProductId() != 0x0001) {
                continue;
            }
            Log.i("TrezorEnumerator.enumerate()", "TREZOR device found");
            if (device.getInterfaceCount() < 1) {
                Log.e("TrezorEnumerator.enumerate()", "Wrong interface count");
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
                Log.e("TrezorEnumerator.enumerate()", "Could not find read endpoint");
                continue;
            }
            if (epw == null) {
                Log.e("TrezorEnumerator.enumerate()", "Could not find write endpoint");
                continue;
            }
            if (epr.getMaxPacketSize() != 64) {
                Log.e("TrezorEnumerator.enumerate()", "Wrong packet size for read endpoint");
                continue;
            }
            if (epw.getMaxPacketSize() != 64) {
                Log.e("TrezorEnumerator.enumerate()", "Wrong packet size for write endpoint");
                continue;
            }
            // try to open the device
            UsbDeviceConnection conn = manager.openDevice(device);
            if (conn == null) {
                Log.e("TrezorEnumerator.enumerate()", "Could not open connection");
                continue;
            }
            boolean claimed = conn.claimInterface(iface, true);
            if (!claimed) {
                Log.e("TrezorEnumerator.enumerate()", "Could not claim interface");
                continue;
            }
            list.put(device.getDeviceName(), new TrezorDevice(device, conn, iface, epr, epw));
        }
        return list;
    }

}
