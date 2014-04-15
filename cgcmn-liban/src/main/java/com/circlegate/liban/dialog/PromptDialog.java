package com.circlegate.liban.dialog;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.circlegate.liban.R;
import com.circlegate.liban.utils.FragmentUtils;

import eu.inmite.android.lib.dialogs.BaseDialogFragment;

public class PromptDialog extends BaseDialogFragment {
    public static final String DEFAULT_FRAGMENT_TAG = PromptDialog.class.getName();

    private static final String BUNDLE_ID = "PromptDialog.id";
    private static final String BUNDLE_TITLE = "PromptDialog.title";
    private static final String BUNDLE_MESSAGE = "PromptDialog.message";
    private static final String BUNDLE_CALLBACK = "PromptDialog.callback";
    private static final String BUNDLE_CANCELABLE = "PromptDialog.cancelable";
    private static final String BUNDLE_SHOW_CANCEL_BTN = "PromptDialog.show_cancel_btn";
    private static final String BUNDLE_BTN_OK_TEXT = "PromptDialog.btn_ok_text";
    private static final String BUNDLE_BTN_CANCEL_TEXT = "PromptDialog.btn_cancel_text";
    private static final String BUNDLE_BUNDLE = "PromptDialog.bundle";

    private Bundle bundle;

    public static PromptDialog show(FragmentManager fm, DialogFragment oldDialog, String fragmentTagOpt, String id, CharSequence title, CharSequence message, boolean callback) {
        return show(fm, oldDialog, fragmentTagOpt, id, title, message, callback, false, false, null);
    }

    public static PromptDialog show(FragmentManager fm, DialogFragment oldDialog, String fragmentTagOpt, String id, CharSequence title, CharSequence message, boolean callback, boolean cancelable, boolean showCancelBtn, Bundle bundle) {
        return show(fm, oldDialog, fragmentTagOpt, id, title, message, callback, cancelable, showCancelBtn, bundle, null, null);
    }

    // btnOkText, btnCancelText - pokud jsou prazdne stringy (ne null!), vubec se nezobrazi
    public static PromptDialog show(FragmentManager fm, DialogFragment oldDialog, String fragmentTagOpt, String id, CharSequence title, CharSequence message, boolean callback, boolean cancelable, boolean showCancelBtn, Bundle bundle, CharSequence btnOkText, CharSequence btnCancelTxt) {
        PromptDialog ret = newInstance(id, title, message, callback, cancelable, showCancelBtn, bundle, btnOkText, btnCancelTxt);
        return FragmentUtils.showDialogRemoveOldOne(fm, oldDialog, ret, fragmentTagOpt == null ? DEFAULT_FRAGMENT_TAG : fragmentTagOpt);
    }

    public static PromptDialog newInstance(String id, CharSequence title, CharSequence message, boolean callback, boolean cancelable, boolean showCancelBtn, Bundle bundle, CharSequence btnOkText, CharSequence btnCancelTxt) {
        Bundle b = new Bundle();
        b.putString(BUNDLE_ID, id);
        b.putCharSequence(BUNDLE_TITLE, title);
        b.putCharSequence(BUNDLE_MESSAGE, message);
        b.putBoolean(BUNDLE_CALLBACK, callback);
        b.putBoolean(BUNDLE_CANCELABLE, cancelable);
        b.putBoolean(BUNDLE_SHOW_CANCEL_BTN, showCancelBtn);
        b.putCharSequence(BUNDLE_BTN_OK_TEXT, btnOkText);
        b.putCharSequence(BUNDLE_BTN_CANCEL_TEXT, btnCancelTxt);
        b.putBundle(BUNDLE_BUNDLE, bundle);

        PromptDialog ret = new PromptDialog();
        ret.setArguments(b);
        ret.setCancelable(cancelable);
        return ret;
    }


    @Override
    protected Builder build(Builder b, Bundle savedInstanceState) {
        Bundle a = getArguments();

        this.bundle = a.getBundle(BUNDLE_BUNDLE);

        final CharSequence title = a.getCharSequence(BUNDLE_TITLE);
        final CharSequence btnOkText = a.getCharSequence(BUNDLE_BTN_OK_TEXT);
        final CharSequence btnCancelText = a.getCharSequence(BUNDLE_BTN_CANCEL_TEXT);

        if (!TextUtils.isEmpty(title)) {
            b.setTitle(title);
        }
        b.setMessage(a.getCharSequence(BUNDLE_MESSAGE));
        if (!"".equals(btnOkText)) {
            b.setPositiveButton(btnOkText != null ? btnOkText : getString(android.R.string.ok), new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onPromptDialogDone(getArguments().getString(BUNDLE_ID), false, bundle);
                    dismiss();
                }
            });
        }

        if (a.getBoolean(BUNDLE_CANCELABLE) && !"".equals(btnCancelText)) {
            b.setNegativeButton(btnCancelText != null ? btnCancelText : getString(android.R.string.cancel), new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onPromptDialogDone(getArguments().getString(BUNDLE_ID), true, bundle);
                    dismiss();
                }
            });
        }
        return b;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null) {
            View v = getDialog().findViewById(R.id.sdl__message);
            if (v instanceof TextView) {
                ((TextView)v).setMovementMethod(LinkMovementMethod.getInstance()); // rozchozeni klikacich linku v message
            }
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        onPromptDialogDone(getArguments().getString(BUNDLE_ID), true, bundle);
    }

    protected void onPromptDialogDone(String id, boolean canceled, Bundle bundle) {
        if (getArguments().getBoolean(BUNDLE_CALLBACK)) {
            if (this.getTargetFragment() != null) {
                ((OnPromptDialogDone)this.getTargetFragment()).onPromptDialogDone(id, canceled, bundle);
            }
            else if (this.getParentFragment() != null) {
                ((OnPromptDialogDone)this.getParentFragment()).onPromptDialogDone(id, canceled, bundle);
            }
            else {
                ((OnPromptDialogDone)this.getActivity()).onPromptDialogDone(id, canceled, bundle);
            }
        }
    }

    public interface OnPromptDialogDone {
        void onPromptDialogDone(String id, boolean canceled, Bundle bundle);
    }
}