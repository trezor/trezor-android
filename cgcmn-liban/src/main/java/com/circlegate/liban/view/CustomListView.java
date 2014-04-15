package com.circlegate.liban.view;

import com.circlegate.liban.view.Common.OnScrollChangedListener;
import com.circlegate.liban.view.Common.OnSizeChangedListener;
import com.circlegate.liban.view.ScrollViewHelper.ScrollViewHelperHost;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListView;

public class CustomListView extends ListView implements ScrollViewHelperHost {
    private final ScrollViewHelper helper;

    private OnScrollChangedListener onScrollChangedListener;
    private OnSizeChangedListener onSizeChangedListener;

    public CustomListView(Context context) {
        this(context, null);
    }

    public CustomListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.helper = new ScrollViewHelper(this);
    }

    //
    // GETTERS
    //

    @Override
    public boolean isScrolledToTop() {
        return getFirstVisiblePosition() == 0 &&
                getFirstVisiblePositionOffset() == 0;
    }

    public int getFirstVisiblePositionOffset() {
        return getVisiblePositionOffset(0);
    }

    public int getVisiblePositionOffset(int visiblePositionIndex) {
        View v = getChildAt(visiblePositionIndex);
        return (v == null) ? 0 : v.getTop();
    }

    /**
     * Vraci position prvniho viditelneho elementu v listView, ktery je "dostatecne" videt (bud alespon z pulky, nebo je z nej videt alespon 144dp)
     */
    public int getFirstVisiblePositionShownEnough() {
        final int ind = getFirstVisiblePosition();
        final View v = getChildCount() > 0 ? getChildAt(0) : null;

        if (ind >= 0 && v != null) {
            int top = v.getTop();
            int height = v.getHeight();

            if (height > 0 && getLastVisiblePosition() > ind) {
                float f = (float)top / (float)height;
                float density = getResources().getDisplayMetrics().density;

                if (f > -0.5f || (height + top > (density * 144)))
                    return ind;
                else
                    return ind + 1;
            }
            else
                return ind;
        }
        else
            return ind;
    }

    public boolean isVisibleItemShowingAtLeastHalfHeight(int visiblePositionIndex) {
        View v = getChildAt(visiblePositionIndex);
        return v != null && v.getHeight() > 0 && ((float)v.getTop() / (float)v.getHeight()) > -0.5f;
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
    public void onScrollChanged(int l, int t, int oldl, int oldt) {
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
