package com.circlegate.liban.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;

import com.circlegate.liban.utils.FragmentUtils;

public class ProgressDialog extends DialogFragment {
    public static final String DEFAULT_FRAGMENT_TAG = ProgressDialog.class.getName();

    private static final String BUNDLE_ID = "ProgressDialog.id";
    private static final String BUNDLE_MESSAGE = "ProgressDialog.message";
    private static final String BUNDLE_CANCELABLE = "ProgressDialog.cancelable";
    private static final String BUNDLE_CALLBACK = "ProgressDialog.callback";
    private static final String BUNDLE_BUNDLE = "ProgressDialog.bundle";

    private Bundle bundle;

    public static ProgressDialog show(FragmentManager fm, DialogFragment oldDialog, String fragmentTagOpt, String id, CharSequence message, boolean cancelable, boolean callback, Bundle bundle) {
        Bundle b = new Bundle();
        b.putString(BUNDLE_ID, id);
        b.putCharSequence(BUNDLE_MESSAGE, message);
        b.putBoolean(BUNDLE_CANCELABLE, cancelable);
        b.putBoolean(BUNDLE_CALLBACK, callback);
        if (bundle != null)
            b.putBundle(BUNDLE_BUNDLE, bundle);

        ProgressDialog ret = new ProgressDialog();
        ret.setArguments(b);
        return FragmentUtils.showDialogRemoveOldOne(fm, oldDialog, ret, fragmentTagOpt == null ? DEFAULT_FRAGMENT_TAG : fragmentTagOpt);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.bundle = getArguments().getBundle(BUNDLE_BUNDLE);
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        setCancelable(getArguments().getBoolean(BUNDLE_CANCELABLE));
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final android.app.ProgressDialog dialog = new android.app.ProgressDialog(getActivity());
        dialog.setTitle("");
        dialog.setMessage(getArguments().getCharSequence(BUNDLE_MESSAGE));
        //dialog.setIndeterminate(false);
        return dialog;
    }

    public Bundle getBundle() {
        return this.bundle;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        onProgressDialogCancel(getArguments().getString(BUNDLE_ID), getArguments().getBundle(BUNDLE_BUNDLE));
    }

    protected void onProgressDialogCancel(String id, Bundle bundle) {
        if (getArguments().getBoolean(BUNDLE_CALLBACK)) {
            if (this.getTargetFragment() != null) {
                ((OnProgressDialogCancel)this.getTargetFragment()).onProgressDialogCancel(id, bundle);
            }
            else {
                ((OnProgressDialogCancel)this.getActivity()).onProgressDialogCancel(id, bundle);
            }
        }
    }

    public interface OnProgressDialogCancel {
        void onProgressDialogCancel(String id, Bundle bundle);
    }
}
