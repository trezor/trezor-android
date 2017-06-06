package com.satoshilabs.trezor.app.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.satoshilabs.trezor.app.R;
import com.satoshilabs.trezor.app.activity.base.BaseActivity;
import com.satoshilabs.trezor.app.view.CustomActionBar;

public class XpubActivity extends BaseActivity {
    private static final int QR_CODE_SIZE = 400;


    public static Intent createIntent(Context context, String xpub) {
        return new Intent(context, XpubActivity.class).putExtra("xpub", xpub);
    }


    //
    // LIFECYCLE CALLBACKS
    //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CustomActionBar.setContentView(this, R.layout.xpub_activity, true);

        TextView txtText = (TextView)findViewById(R.id.txt_text);
        EditText editTextXpub = (EditText) findViewById(R.id.edit_text_xpub);
        View btnCopyToClipboard = findViewById(R.id.btn_copy_to_clipboard);
        ImageView imgQrCode = (ImageView) findViewById(R.id.img_qr_code);

        txtText.setMovementMethod(LinkMovementMethod.getInstance());

        final String xpub = getIntent().getStringExtra("xpub");
        editTextXpub.setText(xpub);

        btnCopyToClipboard.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText(getString(R.string.xpub_activity_title), xpub);
                clipboard.setPrimaryClip(clip);
            }
        });

        Bitmap qrCode = encodeAsBitmap(xpub);
        imgQrCode.setImageBitmap(qrCode);
    }


    //
    // PRIVATE
    //

    private Bitmap encodeAsBitmap(String str) {
        BitMatrix result;
        try {
            result = new MultiFormatWriter().encode(str, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE, null);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        int w = result.getWidth();
        int h = result.getHeight();
        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++) {
            int offset = y * w;
            for (int x = 0; x < w; x++) {
                pixels[offset + x] = result.get(x, y) ? Color.BLACK : Color.WHITE;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
        return bitmap;
    }
}
