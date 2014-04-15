package com.circlegate.liban.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import com.circlegate.liban.R;

public class BoundedRelativeLayout extends RelativeLayout {
    private int mMaxWidth;
    private int mMaxHeight;

    public BoundedRelativeLayout(Context context) {
        this(context, null);
    }

    public BoundedRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.BoundedView);
            mMaxWidth = a.getDimensionPixelSize(R.styleable.BoundedView_android_maxWidth, 0);
            mMaxHeight = a.getDimensionPixelSize(R.styleable.BoundedView_android_maxHeight, 0);
            a.recycle();
        }
        else {
            mMaxWidth = 0;
            mMaxHeight = 0;
        }
    }

    public int getMaximumWidth() {
        return this.mMaxWidth;
    }

    public int getMaximumHeight() {
        return this.mMaxHeight;
    }

    public void setMaximumWidth(int maxWidth) {
        if (this.mMaxWidth != maxWidth) {
            this.mMaxWidth = maxWidth;
            requestLayout();
        }
    }

    public void setMaximumHeigth(int maxHeight) {
        if (this.mMaxHeight != maxHeight) {
            this.mMaxHeight = maxHeight;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Adjust width as necessary
        int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
        if (mMaxWidth > 0 && mMaxWidth < measuredWidth) {
            int measureMode = MeasureSpec.getMode(widthMeasureSpec);
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(mMaxWidth,
                    measureMode);
        }
        // Adjust height as necessary
        int measuredHeight = MeasureSpec.getSize(heightMeasureSpec);
        if (mMaxHeight > 0 && mMaxHeight < measuredHeight) {
            int measureMode = MeasureSpec.getMode(heightMeasureSpec);
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(mMaxHeight,
                    measureMode);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
