package com.circlegate.liban.view;

public class Common {
    public interface OnScrollChangedListener {
        void onScrollChanged(int l, int t, int oldl, int oldt);
    }
    
    public interface OnSizeChangedListener {
        void onSizeChanged(int w, int h, int oldw, int oldh);
    }

    public interface OnPaddingChangedListener {
        void onPaddingChanged(int left, int top, int right, int bottom);
    }
}
