package com.satoshilabs.trezor.lib;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import com.jcraft.jzlib.Deflater;
import com.jcraft.jzlib.DeflaterOutputStream;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class TrezorImageUtils {
    private static final String TAG = TrezorImageUtils.class.getSimpleName();

    public static byte[] processBitmap(Bitmap bmp) throws IOException {
        Log.d(TAG, "processBitmap 1");
        ByteArrayOutputStream bZData = new ByteArrayOutputStream();
        DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(bZData, new Deflater(9, 10));
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(deflaterOutputStream);
        processRgb(bmp, bufferedOutputStream);
        Log.d(TAG, "processBitmap 2");
        bufferedOutputStream.close();
        deflaterOutputStream.close();

        byte[] zdata = bZData.toByteArray();

        ByteArrayOutputStream bOutData = new ByteArrayOutputStream();
        bOutData.write('T');
        bOutData.write('O');
        bOutData.write('I');
        bOutData.write('f');

        writeTwoBytes(bOutData, bmp.getWidth());
        writeTwoBytes(bOutData, bmp.getHeight());
        writeFourBytes(bOutData, zdata.length - 6);
        bOutData.write(zdata, 2, zdata.length - 6);
        Log.d(TAG, "processBitmap 3");
        return bOutData.toByteArray();
    }


    private static void processRgb(Bitmap bmp, OutputStream outputStream) throws IOException {
        final int w = bmp.getWidth();
        final int h = bmp.getHeight();

        int[] pixels = new int[w * h];
        bmp.getPixels(pixels, 0, w, 0, 0, w, h);

        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                final int color = pixels[(j * w) + i]; // bmp.getPixel(i, j);
                int r = Color.red(color);
                int g = Color.green(color);
                int b = Color.blue(color);

                int c = ((r & 0xF8) << 8) | ((g & 0xFC) << 3) | ((b & 0xF8) >> 3);
                outputStream.write((c >> 8) & 0xFF);
                outputStream.write(c & 0xFF);
            }
        }
    }

    private static void writeTwoBytes(OutputStream outputStream, int i) throws IOException {
        outputStream.write(i & 0xFF);
        outputStream.write((i >> 8) & 0xFF);
    }

    private static void writeFourBytes(OutputStream outputStream, int i) throws IOException {
        outputStream.write(i & 0xFF);
        outputStream.write((i >> 8) & 0xFF);
        outputStream.write((i >> 16) & 0xFF);
        outputStream.write((i >> 24) & 0xFF);
    }
}
