package com.circlegate.liban.view;

import com.circlegate.liban.view.Common.OnScrollChangedListener;
import com.circlegate.liban.view.Common.OnSizeChangedListener;
import com.circlegate.liban.view.ScrollViewHelper.ScrollViewHelperHost;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

public class CustomScrollView extends ScrollView implements ScrollViewHelperHost {
    private final ScrollViewHelper helper;

    private OnScrollChangedListener onScrollChangedListener;
    private OnSizeChangedListener onSizeChangedListener;

    public CustomScrollView(Context context) {
        this(context, null);
    }

    public CustomScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.helper = new ScrollViewHelper(this);
    }


    //
    // GETTERS
    //

    @Override
    public boolean isScrolledToTop() {
        return getScrollY() == 0;
    }

    //
    // SETTERS
    //

    public void setScrollingEnabled(boolean scrollingEnabled) {
        this.helper.setScrollingEnabled(scrollingEnabled);
    }

    public void setRefuseOverscroll(boolean refuseOverscroll) {
        this.helper.setRefuseOverscroll(refuseOverscroll);
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!helper.onAnyTouchEvent(ev))
            return false;
        else
            return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!helper.onAnyTouchEvent(ev))
            return false;
        else
            return super.onTouchEvent(ev);
    }


    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (this.onScrollChangedListener != null) {
            this.onScrollChangedListener.onScrollChanged(l, t, oldl, oldt);
        }
    }

    public void setOnScrollChangedListener(OnScrollChangedListener l) {
        this.onScrollChangedListener = l;
    }


    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (this.onSizeChangedListener != null) {
            this.onSizeChangedListener.onSizeChanged(w, h, oldw, oldh);
        }
    }

    public void setOnSizeChangedListener(OnSizeChangedListener l) {
        this.onSizeChangedListener = l;
    }
}
