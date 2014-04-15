package com.satoshilabs.trezor.app.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import com.circlegate.liban.utils.FragmentUtils;
import com.satoshilabs.trezor.app.R;

import eu.inmite.android.lib.dialogs.BaseDialogFragment;

public class CheckConfirmedDialog extends BaseDialogFragment {
    public static final String DEFAULT_FRAGMENT_TAG = CheckConfirmedDialog.class.getName();
    
    private static final String BUNDLE_ID = "CheckConfirmedDialog.id";
    private static final String BUNDLE_TITLE = "CheckConfirmedDialog.title";
    private static final String BUNDLE_MESSAGE = "CheckConfirmedDialog.message";
    private static final String BUNDLE_CHECK_TEXT = "EnterTextDialog.checkText";
    private static final String BUNDLE_CONFIRM_TEXT = "EnterTextDialog.confirmText";
    
    private String id;
    private CheckBox checkBox;
    private Button btnOk;

    public static CheckConfirmedDialog show(FragmentManager fm, DialogFragment oldDialog, String fragmentTagOpt, String id, CharSequence title, CharSequence message, CharSequence checkText, CharSequence confirmText) {
        Bundle b = new Bundle();
        b.putString(BUNDLE_ID, id);
        b.putCharSequence(BUNDLE_TITLE, title);
        b.putCharSequence(BUNDLE_MESSAGE, message);
        b.putCharSequence(BUNDLE_CHECK_TEXT, checkText);
        b.putCharSequence(BUNDLE_CONFIRM_TEXT, confirmText);
        CheckConfirmedDialog ret = new CheckConfirmedDialog();
        ret.setArguments(b);
        ret.setCancelable(true);
        return FragmentUtils.showDialogRemoveOldOne(fm, oldDialog, ret, fragmentTagOpt == null ? DEFAULT_FRAGMENT_TAG : fragmentTagOpt);
    }
    
    @Override
    protected Builder build(Builder b, Bundle savedInstanceState) {
        Bundle a = getArguments();
        this.id = getArguments().getString(BUNDLE_ID);
        
        CharSequence title = a.getCharSequence(BUNDLE_TITLE);
        if (!TextUtils.isEmpty(title))
            b.setTitle(title); 

        CharSequence confirmText = a.getCharSequence(BUNDLE_CONFIRM_TEXT);
        b.setPositiveButton(TextUtils.isEmpty(confirmText) ? getString(android.R.string.ok) : confirmText, new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkBox.isChecked()) {
                    onCheckConfirmedDialogDone(id, false);
                    dismiss();
                }
            }
        });
        b.setNegativeButton(R.string.cancel, new OnClickListener() {
            @Override
            public void onClick(View v) {
                onCheckConfirmedDialogDone(id, true);
                dismiss();
            }
        });
        
        View view = getActivity().getLayoutInflater().inflate(R.layout.check_confirmed_dialog, null);
        TextView text = (TextView)view.findViewById(R.id.text);
        this.checkBox = (CheckBox) view.findViewById(R.id.checkbox);
        
        CharSequence message = getArguments().getCharSequence(BUNDLE_MESSAGE);
        if (TextUtils.isEmpty(message))
            text.setVisibility(View.GONE);
        else {
            text.setVisibility(View.VISIBLE);
            text.setText(message);
            text.setMovementMethod(LinkMovementMethod.getInstance());
        }

        CharSequence checkText = a.getCharSequence(BUNDLE_CHECK_TEXT);
        this.checkBox.setText(TextUtils.isEmpty(checkText) ? getString(R.string.i_understand) : checkText);
        this.checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (btnOk != null)
                    btnOk.setEnabled(b);
            }
        });
        
        b.setView(view);
        return b;
    }

    @Override
    public void onStart() {
        super.onStart();
        
        this.btnOk = (Button)getDialog().findViewById(android.R.id.button1);
        if (btnOk != null) {
            btnOk.setEnabled(checkBox.isChecked());
        }
    }
    
    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        onCheckConfirmedDialogDone(getArguments().getString(BUNDLE_ID), true);
    }
    
    protected void onCheckConfirmedDialogDone(String id, boolean canceled) {
        if (this.getTargetFragment() != null) {
            ((OnCheckConfirmedDialogDone)this.getTargetFragment()).onCheckConfirmedDialogDone(id, canceled);
        }
        else {
            ((OnCheckConfirmedDialogDone)this.getActivity()).onCheckConfirmedDialogDone(id, canceled);
        }
    }
    
    public interface OnCheckConfirmedDialogDone {
        void onCheckConfirmedDialogDone(String id, boolean canceled);
    }
}
