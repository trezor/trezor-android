package com.satoshilabs.trezor.app.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.circlegate.liban.base.Exceptions.NotImplementedException;
import com.satoshilabs.trezor.app.R;
import com.satoshilabs.trezor.app.activity.base.BaseSetupActivity;
import com.satoshilabs.trezor.app.common.TrezorTasks.TrezorError;
import com.satoshilabs.trezor.app.common.TrezorTasks.TrezorTaskResult;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.ChangePin;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.Failure;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.MessageType;

public class SetupPinActivity extends BaseSetupActivity {
    private static final String TASK_CHANGE_PIN = "TASK_CHANGE_PIN";

    // Views
    private TextView txtTitle;
    private View btnContinue;

    public static Intent createIntent(Context context, boolean isV2, String deviceLabel) {
        return setupIntent(new Intent(context, SetupPinActivity.class), isV2)
            .putExtra("deviceLabel", deviceLabel);
    }


    //
    // LIFECYCLE CALLBACKS
    //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRootContentLayout(R.layout.setup_pin_activity);

        this.txtTitle = (TextView) findViewById(R.id.txt_title);
        this.btnContinue = (View)findViewById(R.id.btn_continue);

        this.txtTitle.setText(getString(R.string.setup_pin_title).replace("^d^", getIntent().getStringExtra("deviceLabel")));

        this.btnContinue.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                executeTrezorTaskIfCan(TASK_CHANGE_PIN, ChangePin.newBuilder().setRemove(false).build());
                refreshGui();
            }
        });
    }


    //
    // CUSTOM CALLBACKS
    //

    @Override
    public void onTrezorTaskCompletedSucc(String id, TrezorTaskResult res) {
        if (id.equals(TASK_CHANGE_PIN)) {
            if (res.getMsgResult().msgType == MessageType.MessageType_Success) {
                startActivityNextStep(SetupStepCompletedActivity.createIntent(this, isV2, SetupStepCompletedActivity.STEP_SETUP_PIN));
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
