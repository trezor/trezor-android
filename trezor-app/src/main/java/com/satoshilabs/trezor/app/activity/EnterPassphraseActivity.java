package com.satoshilabs.trezor.app.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.circlegate.liban.base.ApiBase.ApiCreator;
import com.circlegate.liban.base.ApiBase.ApiParcelable;
import com.circlegate.liban.base.ApiBase.IApiParcelable;
import com.circlegate.liban.base.ApiDataIO.ApiDataInput;
import com.circlegate.liban.base.ApiDataIO.ApiDataOutput;
import com.circlegate.liban.utils.ActivityUtils;
import com.satoshilabs.trezor.app.R;
import com.satoshilabs.trezor.app.activity.base.BaseActivity;

public class EnterPassphraseActivity extends BaseActivity {

    // Views
    private TextInputLayout inputLayoutPass1;
    private EditText editTextPass1;
    private TextInputLayout inputLayoutPass2;
    private EditText editTextPass2;
    private View btnPassphraseVisibility;
    private Button btnCancel;
    private Button btnConfirm;

    // Immutable members
    private EnterPassphraseActivityParam activityParam;

    // Unsave state
    private boolean passphraseVisible = false;

    public static Intent createIntent(Context context, EnterPassphraseActivityParam activityParam) {
        return new Intent(context, EnterPassphraseActivity.class).putExtra("activityParam", activityParam);
    }


    //
    // LIFECYCLE CALLBACKS
    //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.enter_passphrase_activity);

        this.inputLayoutPass1 = (TextInputLayout)findViewById(R.id.input_layout_pass1);
        this.editTextPass1 = (EditText)findViewById(R.id.edit_text_pass1);
        this.inputLayoutPass2 = (TextInputLayout)findViewById(R.id.input_layout_pass2);
        this.editTextPass2 = (EditText)findViewById(R.id.edit_text_pass2);
        this.btnPassphraseVisibility = (View)findViewById(R.id.btn_passphrase_visibility);
        this.btnCancel = (Button)findViewById(R.id.btn_cancel);
        this.btnConfirm = (Button)findViewById(R.id.btn_confirm);

        this.activityParam = getIntent().getParcelableExtra("activityParam");

        TextWatcher textWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                boolean valid = setupTextInputLayout(inputLayoutPass2, editTextPass2.getText().toString(),
                        TextUtils.equals(editTextPass1.getText(), editTextPass2.getText()), R.string.enter_passphrase_no_match);

                btnConfirm.setEnabled(valid && editTextPass1.getText().length() > 0);
            }
        };

        editTextPass1.addTextChangedListener(textWatcher);
        editTextPass2.addTextChangedListener(textWatcher);

        btnPassphraseVisibility.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                passphraseVisible = !passphraseVisible;
                if (passphraseVisible) {
                    editTextPass1.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    editTextPass2.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                }
                else {
                    editTextPass1.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    editTextPass2.setTransformationMethod(PasswordTransformationMethod.getInstance());
                }
            }
        });
        btnCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        btnConfirm.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                String passphrase = editTextPass1.getText().toString();

                if (passphrase.length() > 0 && passphrase.equals(editTextPass2.getText().toString())) {
                    ActivityUtils.setResultParcelable(EnterPassphraseActivity.this, RESULT_OK, new EnterPassphraseActivityResult(activityParam, passphrase));
                    finish();
                }
            }
        });

        ActivityUtils.setResultParcelable(EnterPassphraseActivity.this, RESULT_CANCELED, new EnterPassphraseActivityResult(activityParam, ""));

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }


    //
    // PRIVATE
    //

    private static boolean setupTextInputLayout(TextInputLayout inputLayout, String enteredText, boolean isValid, int errorRid) {
        if (!isValid) {
            if (enteredText.isEmpty()) {
                inputLayout.setErrorEnabled(false);
                inputLayout.setError(null);
            }
            else {
                inputLayout.setErrorEnabled(true);
                inputLayout.setError(inputLayout.getContext().getString(errorRid));
            }
            return false;
        }
        else {
            inputLayout.setErrorEnabled(false);
            inputLayout.setError(null);
            return true;
        }
    }


    //
    // INNER CLASSES
    //

    public static class EnterPassphraseActivityParam extends ApiParcelable {
        public final String taskId;
        public final IApiParcelable tag; // optional abstract

        public EnterPassphraseActivityParam(String taskId, IApiParcelable tag) {
            this.taskId = taskId;
            this.tag = tag;
        }

        public EnterPassphraseActivityParam(ApiDataInput d) {
            this.taskId = d.readString();
            this.tag = d.readOptParcelableWithName();
        }

        @Override
        public void save(ApiDataOutput d, int flags) {
            d.write(this.taskId);
            d.writeOptWithName(this.tag, flags);
        }

        public static final ApiCreator<EnterPassphraseActivityParam> CREATOR = new ApiCreator<EnterPassphraseActivityParam>() {
            public EnterPassphraseActivityParam create(ApiDataInput d) { return new EnterPassphraseActivityParam(d); }
            public EnterPassphraseActivityParam[] newArray(int size) { return new EnterPassphraseActivityParam[size]; }
        };
    }

    public static class EnterPassphraseActivityResult extends ApiParcelable {
        public final EnterPassphraseActivityParam param;
        public final String passphrase;

        public EnterPassphraseActivityResult(EnterPassphraseActivityParam param, String passphrase) {
            this.param = param;
            this.passphrase = passphrase;
        }

        public EnterPassphraseActivityResult(ApiDataInput d) {
            this.param = d.readObject(EnterPassphraseActivityParam.CREATOR);
            this.passphrase = d.readString();
        }

        @Override
        public void save(ApiDataOutput d, int flags) {
            d.write(this.param, flags);
            d.write(this.passphrase);
        }

        public static final ApiCreator<EnterPassphraseActivityResult> CREATOR = new ApiCreator<EnterPassphraseActivityResult>() {
            public EnterPassphraseActivityResult create(ApiDataInput d) { return new EnterPassphraseActivityResult(d); }
            public EnterPassphraseActivityResult[] newArray(int size) { return new EnterPassphraseActivityResult[size]; }
        };
    }
}
