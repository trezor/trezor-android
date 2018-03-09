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
import android.widget.RadioGroup;
import android.widget.Spinner;

import com.circlegate.liban.base.Exceptions.NotImplementedException;
import com.satoshilabs.trezor.app.R;
import com.satoshilabs.trezor.app.activity.base.BaseActivity;
import com.satoshilabs.trezor.app.activity.base.BaseSetupActivity;
import com.satoshilabs.trezor.app.common.TrezorTasks.TrezorError;
import com.satoshilabs.trezor.app.common.TrezorTasks.TrezorTaskParam;
import com.satoshilabs.trezor.app.common.TrezorTasks.TrezorTaskResult;
import com.satoshilabs.trezor.app.view.CustomActionBar;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.ButtonAck;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.MessageType;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.RecoveryDevice;

public class TrezorRecoveryInitActivity extends BaseSetupActivity {
    private static final String TASK_RECOVERY_DEVICE = "TASK_RECOVERY_DEVICE";
    private static final String TASK_BUTTON_ACK = "TASK_BUTTON_ACK";


    // Views
    private RadioGroup radioGroupWords;
    private View btnContinue;


    public static Intent createIntent(Context context) {
        return setupIntent(new Intent(context, TrezorRecoveryInitActivity.class), false);
    }


    //
    // LIFECYCLE CALLBACKS
    //


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRootContentLayout(R.layout.trezor_recovery_init_activity);

        this.radioGroupWords = findViewById(R.id.radio_group_words);
        this.btnContinue = (View)findViewById(R.id.btn_continue);

        btnContinue.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                final int wordsCount;
                switch (radioGroupWords.getCheckedRadioButtonId()) {
                    case R.id.rbtn_12: wordsCount = 12; break;
                    case R.id.rbtn_18: wordsCount = 18; break;
                    case R.id.rbtn_24: wordsCount = 24; break;
                    default: return;
                }

                executeTrezorTaskIfCan(TASK_RECOVERY_DEVICE, RecoveryDevice.newBuilder()
                        .setWordCount(wordsCount)
                        .setEnforceWordlist(true)
                        .setLabel(getString(R.string.init_trezor_device_label_default))
                        .build()
                );
                refreshGui();
            }
        });
    }


    //
    // CUSTOM CALLBACKS
    //

    @Override
    public void onTrezorTaskCompletedSucc(String id, TrezorTaskResult res) {
        if (id.equals(TASK_RECOVERY_DEVICE)) {
            if (res.getMsgResult().msgType == MessageType.MessageType_WordRequest) {
                startActivityNextStep(TrezorRecoverySeedActivity.createIntent(this));
            }
            else
                onTrezorError(TrezorError.ERR_UNEXPECTED_RESPONSE);
        }
        else
            throw new NotImplementedException();
    }
}
