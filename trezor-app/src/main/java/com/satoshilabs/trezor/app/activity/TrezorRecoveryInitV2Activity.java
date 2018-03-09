package com.satoshilabs.trezor.app.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;

import com.circlegate.liban.base.Exceptions.NotImplementedException;
import com.satoshilabs.trezor.app.R;
import com.satoshilabs.trezor.app.activity.base.BaseSetupActivity;
import com.satoshilabs.trezor.app.common.TrezorTasks.TrezorError;
import com.satoshilabs.trezor.app.common.TrezorTasks.TrezorTaskParam;
import com.satoshilabs.trezor.app.common.TrezorTasks.TrezorTaskResult;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.ButtonAck;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.MessageType;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.RecoveryDevice;

public class TrezorRecoveryInitV2Activity extends BaseSetupActivity {
    private static final String TASK_RECOVERY_DEVICE = "TASK_RECOVERY_DEVICE";


    // Views
    private View btnContinue;


    public static Intent createIntent(Context context) {
        return setupIntent(new Intent(context, TrezorRecoveryInitV2Activity.class), true);
    }


    //
    // LIFECYCLE CALLBACKS
    //


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // chceme aby zustal displej zapnuty
        super.onCreate(savedInstanceState);
        setRootContentLayout(R.layout.trezor_recovery_init_v2_activity);

        this.btnContinue = (View)findViewById(R.id.btn_continue);

        btnContinue.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                executeTrezorTaskIfCan(TASK_RECOVERY_DEVICE, RecoveryDevice.newBuilder()
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
            if (res.getMsgResult().msgType == MessageType.MessageType_Success) {
                finish();
                startActivity(MainActivity.createIntent(this));
                setDontDisconnectOnStop();
            }
            else {
                onTrezorError(TrezorError.ERR_UNEXPECTED_RESPONSE);
            }
        }
        else
            throw new NotImplementedException();
    }
}
