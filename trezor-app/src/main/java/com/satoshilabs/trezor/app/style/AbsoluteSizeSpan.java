package com.satoshilabs.trezor.app.style;

public class AbsoluteSizeSpan extends android.text.style.AbsoluteSizeSpan implements IHtmlTagSpan {

    public AbsoluteSizeSpan(int size) {
        super(size);
    }

    public AbsoluteSizeSpan(int size, boolean dip) {
        super(size, dip);
    }
}
