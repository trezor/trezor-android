package com.satoshilabs.trezor.app.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.circlegate.liban.utils.FragmentUtils;
import com.satoshilabs.trezor.app.R;

import eu.inmite.android.lib.dialogs.BaseDialogFragment;

public class EnterTextDialog extends BaseDialogFragment {
    public static final String DEFAULT_FRAGMENT_TAG = EnterTextDialog.class.getName();
    
    private static final String BUNDLE_ID = "EnterTextDialog.id";
    private static final String BUNDLE_TITLE = "EnterTextDialog.title";
    private static final String BUNDLE_MESSAGE = "EnterTextDialog.message";
    private static final String BUNDLE_HINT = "EnterTextDialog.hint";
    private static final String BUNDLE_DEF_TEXT = "EnterTextDialog.defText";
    
    private String id;
    private EditText editText;
    private Button btnOk;

    public static EnterTextDialog show(FragmentManager fm, DialogFragment oldDialog, String fragmentTagOpt, String id,
                                       CharSequence title, CharSequence message, CharSequence hint, String defText)
    {
        Bundle b = new Bundle();
        b.putString(BUNDLE_ID, id);
        b.putCharSequence(BUNDLE_TITLE, title);
        b.putCharSequence(BUNDLE_MESSAGE, message);
        b.putCharSequence(BUNDLE_HINT, hint);
        b.putString(BUNDLE_DEF_TEXT, defText);
        EnterTextDialog ret = new EnterTextDialog();
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
        
        b.setPositiveButton(R.string.confirm, new OnClickListener() {
            @Override
            public void onClick(View v) {
                onEnterTextDialogDone(id, false, editText.getText().toString());
                dismiss();
            }
        });
        b.setNegativeButton(R.string.cancel, new OnClickListener() {
            @Override
            public void onClick(View v) {
                onEnterTextDialogDone(id, true, null);
                dismiss();
            }
        });
        
        View view = getActivity().getLayoutInflater().inflate(R.layout.enter_text_dialog, null);
        TextView text = (TextView)view.findViewById(R.id.text);
        this.editText = (EditText)view.findViewById(R.id.edit_text);
        
        CharSequence message = getArguments().getCharSequence(BUNDLE_MESSAGE);
        if (TextUtils.isEmpty(message))
            text.setVisibility(View.GONE);
        else {
            text.setVisibility(View.VISIBLE);
            text.setText(message);
        }
        
        this.editText.setHint(getArguments().getCharSequence(BUNDLE_HINT));
        
        b.setView(view);
        
        if (savedInstanceState != null) {
            this.editText.setText(savedInstanceState.getString(BUNDLE_DEF_TEXT));
        }
        else {
            this.editText.setText(getArguments().getString(BUNDLE_DEF_TEXT));
            this.editText.setSelection(0, editText.getText().length());
        }
        
        this.editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (btnOk != null) {
                    btnOk.setEnabled(editText.getText().length() > 0);
                }
            }
            
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void afterTextChanged(Editable s) { }
        });
        return b;
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog ret = super.onCreateDialog(savedInstanceState);
        ret.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return ret;
    }
    
    @Override
    public void onStart() {
        super.onStart();
        
        this.btnOk = (Button)getDialog().findViewById(android.R.id.button1);
        if (btnOk != null) {
            btnOk.setEnabled(editText.getText().length() > 0);
        }
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(BUNDLE_DEF_TEXT, editText.getText().toString());
    }
    
    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        onEnterTextDialogDone(getArguments().getString(BUNDLE_ID), true, null);
    }
    
    protected void onEnterTextDialogDone(String id, boolean canceled, String text) {
        if (this.getTargetFragment() != null) {
            ((OnEnterTextDialogDone)this.getTargetFragment()).onEnterTextDialogDone(id, canceled, text);
        }
        else {
            ((OnEnterTextDialogDone)this.getActivity()).onEnterTextDialogDone(id, canceled, text);
        }
    }
    
    public interface OnEnterTextDialogDone {
        void onEnterTextDialogDone(String id, boolean canceled, String text);
    }
}
