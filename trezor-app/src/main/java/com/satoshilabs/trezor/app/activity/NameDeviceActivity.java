package com.satoshilabs.trezor.app.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

import com.circlegate.liban.base.Exceptions.NotImplementedException;
import com.satoshilabs.trezor.app.R;
import com.satoshilabs.trezor.app.activity.base.BaseSetupActivity;
import com.satoshilabs.trezor.app.common.TrezorTasks.TrezorError;
import com.satoshilabs.trezor.app.common.TrezorTasks.TrezorTaskResult;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.ApplySettings;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.Failure;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.MessageType;

public class NameDeviceActivity extends BaseSetupActivity {
    private static final String TASK_APPLY_SETTINGS = "TASK_APPLY_SETTINGS";

    // Views
    private EditText editTextDeviceLabel;
    private View btnContinue;

    public static Intent createIntent(Context context, boolean isV2) {
        return setupIntent(new Intent(context, NameDeviceActivity.class), isV2);
    }


    //
    // LIFECYCLE CALLBACKS
    //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRootContentLayout(R.layout.name_device_activity);

        this.editTextDeviceLabel = (EditText) findViewById(R.id.edit_text_device_label);
        this.btnContinue = (View)findViewById(R.id.btn_continue);

        this.editTextDeviceLabel.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                btnContinue.setEnabled(editTextDeviceLabel.getText().length() > 0);
            }
        });

        this.btnContinue.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                executeTrezorTaskIfCan(TASK_APPLY_SETTINGS, ApplySettings.newBuilder().setLabel(editTextDeviceLabel.getText().toString()).build());
                refreshGui();
            }
        });
    }


    //
    // CUSTOM CALLBACKS
    //

    @Override
    public void onTrezorTaskCompletedSucc(String id, TrezorTaskResult res) {
        if (id.equals(TASK_APPLY_SETTINGS)) {
            if (res.getMsgResult().msgType == MessageType.MessageType_Success) {
                startActivityNextStep(SetupStepCompletedActivity.createIntent(this, isV2, SetupStepCompletedActivity.STEP_NAME_DEVICE));
            }
            else if (res.getMsgResult().msgType == MessageType.MessageType_Failure) {
                showToastTrezorMsgFailure((Failure)res.getMsgResult().msg);
                refreshGui();
            }
            else {
                onTrezorError(TrezorError.ERR_UNEXPECTED_RESPONSE);
            }
        }
        else
            throw new NotImplementedException();
    }
}
