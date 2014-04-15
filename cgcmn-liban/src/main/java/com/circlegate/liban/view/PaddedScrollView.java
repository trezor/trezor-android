package com.circlegate.liban.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import com.circlegate.liban.R;
import com.circlegate.liban.base.Exceptions.NotImplementedException;
import com.circlegate.liban.view.Common.OnPaddingChangedListener;

public class PaddedScrollView extends CustomScrollView {
    public static final int PADDED_ALIGN_CENTER = 0;
    public static final int PADDED_ALIGN_LEFT = 1;
    public static final int PADDED_ALIGN_RIGHT = 2;

    private int paddedWidth;
    private int paddedAlign;
    private int origPaddingLeft = Integer.MIN_VALUE;
    private int origPaddingRight = Integer.MIN_VALUE;

    private OnPaddingChangedListener onPaddingChangedListener;

    public PaddedScrollView(Context context) {
        this(context, null);
    }

    public PaddedScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PaddedScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.PaddedView);
            this.paddedWidth = a.getDimensionPixelSize(R.styleable.PaddedView_maxPaddedWidth, 0);
            this.paddedAlign = a.getInt(R.styleable.PaddedView_paddedAlign, PADDED_ALIGN_CENTER);
            a.recycle();
        }
    }

    public int getPaddedWidth() {
        return this.paddedWidth;
    }

    public int getPaddedAlign() {
        return this.paddedAlign;
    }

    public void setPaddedWidth(int paddedWidth) {
        if (this.paddedWidth != paddedWidth) {
            this.paddedWidth = paddedWidth;
            computeAndSetPaddings(false);
        }
    }

    public void setPaddedAlign(int paddedAlign) {
        if (this.paddedAlign != paddedAlign) {
            this.paddedAlign = paddedAlign;
            computeAndSetPaddings(false);
        }
    }


    //
    // CALLBACKS
    //

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        this.origPaddingLeft = left;
        this.origPaddingRight = right;
        computeAndSetPaddings(left, top, right, bottom, false);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        computeAndSetPaddings(oldw <= 0 && oldh <= 0 && (w > 0 || h > 0)); // calback onPaddingChanged chci zavolat i po prvotnim nastaveni velikosti...
    }

    protected void onPaddingChanged(int left, int top, int right, int bottom) {
        if (onPaddingChangedListener != null)
            onPaddingChangedListener.onPaddingChanged(left, top, right, bottom);
    }

    public void setOnPaddingChangedListener(OnPaddingChangedListener l) {
        this.onPaddingChangedListener = l;
    }


    //
    // PRIVATE
    //

    private void computeAndSetPaddings(boolean forceCallback) {
        if (origPaddingLeft == Integer.MIN_VALUE)
            origPaddingLeft = getPaddingLeft();
        if (origPaddingRight == Integer.MIN_VALUE)
            origPaddingRight = getPaddingRight();
        computeAndSetPaddings(origPaddingLeft, getPaddingTop(), origPaddingRight, getPaddingBottom(), forceCallback);
    }

    private void computeAndSetPaddings(int left, int top, int right, int bottom, boolean forceCallback) {
        int w = getWidth();

        if (w > 0 && paddedWidth > 0) {
            switch (paddedAlign) {
                case PADDED_ALIGN_CENTER: {
                    int addedPadding = Math.max(0, (w - paddedWidth) / 2);
                    left += addedPadding;
                    right += addedPadding;
                    break;
                }

                case PADDED_ALIGN_LEFT: {
                    right += Math.max(0, w - paddedWidth);
                    break;
                }

                case PADDED_ALIGN_RIGHT: {
                    left += Math.max(0, w - paddedWidth);
                    break;
                }

                default: throw new NotImplementedException();
            }

            setPaddingIfDifferent(left, top, right, bottom, forceCallback);
        }
        else
            setPaddingIfDifferent(left, top, right, bottom, forceCallback);
    }

    private void setPaddingIfDifferent(int left, int top, int right, int bottom, boolean forceCallback) {
        if (left != getPaddingLeft() ||
                top != getPaddingTop() ||
                right != getPaddingRight() ||
                bottom != getPaddingBottom())
        {
            super.setPadding(left, top, right, bottom);
            forceCallback = true;
            post(new Runnable() {
                @Override
                public void run() {
                    requestLayout();
                }
            });
        }
        if (forceCallback)
            onPaddingChanged(left, top, right, bottom);
    }
}
