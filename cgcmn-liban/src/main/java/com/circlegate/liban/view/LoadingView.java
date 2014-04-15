package com.circlegate.liban.view;

import com.circlegate.liban.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

// Navod na custom styly v tematu: http://stackoverflow.com/questions/4493947/how-to-define-theme-style-item-for-custom-widget

public class LoadingView extends FrameLayout {
    private final ProgressBar progressBar;
    private final TextView text;
    private final ViewGroup innerRoot;

    public LoadingView(Context context) {
        this(context, null);
    }

    public LoadingView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.lib_loadingViewStyle);
    }

    public LoadingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        LayoutInflater.from(context).inflate(R.layout.loading_view, this);

        this.progressBar = (ProgressBar)findViewById(R.id.progress_bar);
        this.text = (TextView)findViewById(R.id.text);
        this.innerRoot = (ViewGroup)findViewById(R.id.inner_root);

        if (attrs != null && !isInEditMode()) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.LoadingView, defStyle, 0);
            CharSequence txt;
            int d;

            if ((txt = a.getText(R.styleable.LoadingView_android_text)) != null)
                text.setText(txt);
            if ((d = a.getDimensionPixelSize(R.styleable.LoadingView_android_textSize, 0)) > 0)
                text.setTextSize(TypedValue.COMPLEX_UNIT_PX, d);
            if (a.hasValue(R.styleable.LoadingView_android_textColor))
                text.setTextColor(a.getColor(R.styleable.LoadingView_android_textColor, 0));

            if (a.hasValue(R.styleable.LoadingView_progressBarMarginTop)) {
                ((MarginLayoutParams)progressBar.getLayoutParams()).topMargin =
                        a.getDimensionPixelOffset(R.styleable.LoadingView_progressBarMarginTop, 0);
            }
            if (a.hasValue(R.styleable.LoadingView_progressBarMarginBottom)) {
                ((MarginLayoutParams)progressBar.getLayoutParams()).bottomMargin =
                        a.getDimensionPixelOffset(R.styleable.LoadingView_progressBarMarginBottom, 0);
            }
            if (a.hasValue(R.styleable.LoadingView_textMarginFromProgressBar)) {
                ((MarginLayoutParams)text.getLayoutParams()).leftMargin =
                        a.getDimensionPixelOffset(R.styleable.LoadingView_textMarginFromProgressBar, 0);
            }
            progressBar.setVisibility(a.getBoolean(R.styleable.LoadingView_progressBarVisible, true) ? VISIBLE : GONE);

            a.recycle();
        }
    }

    public ProgressBar getProgressBar() {
        return this.progressBar;
    }

    public TextView getTextView() {
        return this.text;
    }

    public ViewGroup getInnerRoot() {
        return this.innerRoot;
    }


    public void setProgresBarVisible(boolean visible) {
        this.progressBar.setVisibility(visible ? VISIBLE : GONE);
    }

    public void setText(int resid) {
        this.text.setText(resid);
    }

    public void setText(CharSequence text) {
        this.text.setText(text);
    }
}
