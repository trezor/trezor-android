package com.satoshilabs.trezor.app.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;

import com.circlegate.liban.base.Exceptions.NotImplementedException;
import com.satoshilabs.trezor.app.R;
import com.satoshilabs.trezor.app.activity.base.BaseActivity;
import com.satoshilabs.trezor.app.common.TrezorTasks.TrezorError;
import com.satoshilabs.trezor.app.common.TrezorTasks.TrezorTaskResult;
import com.satoshilabs.trezor.app.view.CustomActionBar;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.MessageType;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.RecoveryDevice;

public class TrezorRecoveryInitActivity extends BaseActivity {
    private static final String TASK_RECOVERY_DEVICE = "TASK_RECOVERY_DEVICE";


    // Views
    private ProgressBar progressBar;
    private View rootContent;
    private TextInputLayout inputLayoutDeviceLabel;
    private EditText editTextDeviceLabel;
    private Spinner spinnerNumberOfWords;
    private CheckBox checkboxPin;
    private CheckBox checkboxPassphrase;
    private View btnContinue;


    public static Intent createIntent(Context context) {
        return new Intent(context, TrezorRecoveryInitActivity.class);
    }


    //
    // LIFECYCLE CALLBACKS
    //


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CustomActionBar.setContentView(this, R.layout.trezor_recovery_init_activity, true);

        this.progressBar = (ProgressBar)findViewById(R.id.progress_bar);
        this.rootContent = findViewById(R.id.root_content);
        this.inputLayoutDeviceLabel = (TextInputLayout)findViewById(R.id.input_layout_device_label);
        this.editTextDeviceLabel = (EditText)findViewById(R.id.edit_text_device_label);
        this.spinnerNumberOfWords = (Spinner)findViewById(R.id.spinner_number_of_words);
        this.checkboxPin = (CheckBox)findViewById(R.id.checkbox_pin);
        this.checkboxPassphrase = (CheckBox)findViewById(R.id.checkbox_passphrase);
        this.btnContinue = (View)findViewById(R.id.btn_continue);

        ArrayAdapter<CharSequence> adapterNumberOfWords = ArrayAdapter.createFromResource(this, R.array.recovery_seed_number_of_words, R.layout.spinner_item);
        adapterNumberOfWords.setDropDownViewResource(R.layout.spinner_item_dropdown);
        spinnerNumberOfWords.setAdapter(adapterNumberOfWords);
        spinnerNumberOfWords.setSelection(2);

        btnContinue.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                final int wordsCount;
                switch (spinnerNumberOfWords.getSelectedItemPosition()) {
                    case 0: wordsCount = 12; break;
                    case 1: wordsCount = 18; break;
                    case 2: wordsCount = 24; break;
                    default: throw new NotImplementedException();
                }

                executeTrezorTaskIfCan(TASK_RECOVERY_DEVICE, RecoveryDevice.newBuilder()
                        .setLabel(editTextDeviceLabel.getText().toString())
                        .setWordCount(wordsCount)
                        .setPinProtection(checkboxPin.isChecked())
                        .setPassphraseProtection(checkboxPassphrase.isChecked())
                        .setEnforceWordlist(true)
                        .build()
                );
                refreshGui();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        refreshGui();
    }

    //
    // CUSTOM CALLBACKS
    //

    @Override
    public void onTrezorTaskCompletedSucc(String id, TrezorTaskResult res) {
        if (id.equals(TASK_RECOVERY_DEVICE)) {
            if (res.getMsgResult().msgType == MessageType.MessageType_WordRequest) {
                startActivity(TrezorRecoverySeedActivity.createIntent(this));
            }
            else
                onTrezorError(TrezorError.ERR_UNEXPECTED_RESPONSE);
        }
        else
            throw new NotImplementedException();
    }


    //
    // PRIVATE
    //

    private void refreshGui() {
        if (getTaskFragment().containsAnyTaskByFragmentTag(null)) {
            progressBar.setVisibility(View.VISIBLE);
            rootContent.setVisibility(View.GONE);
        }
        else {
            progressBar.setVisibility(View.GONE);
            rootContent.setVisibility(View.VISIBLE);
        }
    }
}
