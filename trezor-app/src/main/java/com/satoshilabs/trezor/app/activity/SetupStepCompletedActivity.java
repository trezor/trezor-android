package com.satoshilabs.trezor.app.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.circlegate.liban.base.Exceptions.NotImplementedException;
import com.satoshilabs.trezor.app.R;
import com.satoshilabs.trezor.app.activity.base.BaseSetupActivity;
import com.satoshilabs.trezor.app.common.TrezorTasks.TrezorError;
import com.satoshilabs.trezor.app.common.TrezorTasks.TrezorTaskResult;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.Failure;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.Features;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.GetFeatures;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.MessageType;

public class SetupStepCompletedActivity extends BaseSetupActivity {
    public static final int STEP_CREATE_BACKUP = 0;
    public static final int STEP_NAME_DEVICE = 1;
    public static final int STEP_SETUP_PIN = 2;

    private static final String TASK_GET_FEATURES = "TASK_GET_FEATURES";

    // Views
    private TextView txtText;
    private View btnContinue;

    // Immutable members
    private int step;

    public static Intent createIntent(Context context, boolean isV2, int step) {
        return setupIntent(new Intent(context, SetupStepCompletedActivity.class), isV2)
            .putExtra("step", step);
    }


    //
    // LIFECYCLE CALLBACKS
    //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRootContentLayout(R.layout.setup_step_completed_activity);
        this.step = getIntent().getIntExtra("step", 0);

        this.txtText = (TextView)findViewById(R.id.txt_text);
        this.btnContinue = (View)findViewById(R.id.btn_continue);

        switch (step) {
            case STEP_CREATE_BACKUP:
                setTitle(R.string.create_backup_activity_title);
                txtText.setText(R.string.create_backup_completed);
                break;

            case STEP_NAME_DEVICE:
                setTitle(R.string.name_device_activity_title);
                txtText.setText(R.string.name_device_completed);
                break;

            case STEP_SETUP_PIN:
                setTitle(R.string.setup_pin_activity_title);
                txtText.setText(R.string.setup_pin_completed);
                break;

            default: throw new NotImplementedException();
        }

        this.btnContinue.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                executeTrezorTaskIfCan(TASK_GET_FEATURES, GetFeatures.newBuilder().build());
                refreshGui();
            }
        });
    }


    //
    // CUSTOM CALLBACKS
    //

    @Override
    public void onTrezorTaskCompletedSucc(String id, TrezorTaskResult res) {
        if (id.equals(TASK_GET_FEATURES)) {
            if (res.getMsgResult().msgType == MessageType.MessageType_Features) {
                Features features = (Features)res.getMsgResult().msg;

                if (features.getNeedsBackup())
                    startActivityNextStep(CreateBackupInitActivity.createIntent(this, isV2));
                else if (TextUtils.isEmpty(features.getLabel()) || TextUtils.equals(features.getLabel(), getString(R.string.init_trezor_device_label_default)))
                    startActivityNextStep(NameDeviceInitActivity.createIntent(this, isV2));
                else if (!features.getPinProtection())
                    startActivityNextStep(SetupPinActivity.createIntent(this, isV2, features.getLabel()));
                else {
                    finish();
                    startActivity(MainActivity.createIntent(this));
                    setDontDisconnectOnStop();
                }
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
