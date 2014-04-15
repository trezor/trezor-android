package com.circlegate.liban.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.TextView;

public class ViewUtils {
    private static float baseScale = 1;

    public static void setBaseScale(float baseScale) {
        ViewUtils.baseScale = baseScale;
    }

    public static int getScaledDensityDpi(Context context) {
        return (int)((context.getResources().getDisplayMetrics().densityDpi * baseScale) + 0.5f);
    }

    public static int getPixelsFromDp(Context context, float dp) {
        return getPixelsFromDp(context, dp, baseScale);
    }

    public static int getPixelsFromDp(Context context, float dp, float baseScale) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) ((dp * scale * baseScale) + 0.5f);
    }

    public static float getPixelsFromSp(Context context, float sp) {
        final float scale = context.getResources().getDisplayMetrics().scaledDensity;
        return sp * scale * baseScale;
    }

    public static void setTextOrHide(TextView textView, CharSequence text) {
        if (!TextUtils.isEmpty(text)) {
            textView.setVisibility(TextView.VISIBLE);
            textView.setText(text);
        }
        else
            textView.setVisibility(TextView.GONE);
    }

    @SuppressWarnings("deprecation")
    public static void setBackgroundDrawableKeepPadding(View view, Drawable drawable) {
        int left = view.getPaddingLeft();
        int top = view.getPaddingTop();
        int right = view.getPaddingRight();
        int bottom = view.getPaddingBottom();

        view.setBackgroundDrawable(drawable);
        view.setPadding(left, top, right, bottom);
    }

    public static void setBackgroundResourceKeepPadding(View view, int resid) {
        int left = view.getPaddingLeft();
        int top = view.getPaddingTop();
        int right = view.getPaddingRight();
        int bottom = view.getPaddingBottom();

        view.setBackgroundResource(resid);
        view.setPadding(left, top, right, bottom);
    }

    public static int resolveSizeAndState(int size, int measureSpec, int childMeasuredState) {
        int result = size;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize =  MeasureSpec.getSize(measureSpec);
        switch (specMode) {
            case MeasureSpec.UNSPECIFIED:
                result = size;
                break;
            case MeasureSpec.AT_MOST:
                if (specSize < size) {
                    result = specSize | View.MEASURED_STATE_TOO_SMALL;
                } else {
                    result = size;
                }
                break;
            case MeasureSpec.EXACTLY:
                result = specSize;
                break;
        }
        return result | (childMeasuredState & View.MEASURED_STATE_MASK);
    }

    public static void addOnGlobalLayoutCalledOnce(final View view, final OnGlobalLayoutListener l) {
        view.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                ViewTreeObserver vto = view.getViewTreeObserver();
                if (vto != null && vto.isAlive()) {
                    if (VERSION.SDK_INT < VERSION_CODES.JELLY_BEAN)
                        vto.removeGlobalOnLayoutListener(this);
                    else
                        vto.removeOnGlobalLayoutListener(this);
                }
                l.onGlobalLayout();
            }
        });
    }

    public static void addOnTextChangedAndLayout(final TextView view, final OnTextChangedAndLayoutListener listener) {
        view.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (view.getLineCount() <= 0) {
                    ViewUtils.addOnGlobalLayoutCalledOnce(view, new OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            listener.onTextChangedAndLayout(view);
                        }
                    });
                } else
                    listener.onTextChangedAndLayout(view);
            }
        });
    }



    public interface OnTextChangedAndLayoutListener {
        void onTextChangedAndLayout(TextView view);
    }
}
