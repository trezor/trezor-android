package com.satoshilabs.trezor.app.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.TaskStackBuilder;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.circlegate.liban.base.Exceptions.NotImplementedException;
import com.satoshilabs.trezor.app.R;
import com.satoshilabs.trezor.app.activity.base.BaseSetupActivity;
import com.satoshilabs.trezor.app.common.GlobalContext.FirmwareVersion;
import com.satoshilabs.trezor.app.common.TrezorTasks.MsgWrp;
import com.satoshilabs.trezor.app.common.TrezorTasks.TrezorError;
import com.satoshilabs.trezor.app.common.TrezorTasks.TrezorTaskResult;
import com.satoshilabs.trezor.app.utils.CommonUtils;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.Failure;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.Features;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.MessageType;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.ResetDevice;

public class InitTrezorActivity extends BaseSetupActivity {
    private static final String TASK_RESET_DEVICE = "TASK_RESET_DEVICE";

    // Views
    private View btnCreateWallet;
    private View btnRecoverWallet;
    private Button btnFirmwareUpgradeAvail;

    // Immutable members
    private Features features;


    public static Intent createIntent(Context context, MsgWrp featuresMsg) {
        return setupIntent(new Intent(context, InitTrezorActivity.class).putExtra("featuresMsg", featuresMsg), CommonUtils.isTrezorV2((Features) featuresMsg.msg));
    }


    //
    // LIFECYCLE CALLBACKS
    //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRootContentLayout(R.layout.init_trezor_activity);

        this.btnCreateWallet = (View)findViewById(R.id.btn_create_wallet);
        this.btnRecoverWallet = (View)findViewById(R.id.btn_recover_wallet);
        this.btnFirmwareUpgradeAvail = (Button)findViewById(R.id.btn_notification);

        this.features = (Features) (getIntent().<MsgWrp>getParcelableExtra("featuresMsg").msg);

        btnCreateWallet.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                executeTrezorTaskIfCan(TASK_RESET_DEVICE, ResetDevice.newBuilder()
                        .setStrength(isV2 ? 128 : 256)
                        .setLabel(getString(R.string.init_trezor_device_label_default))
                        .setDisplayRandom(false)
                        .setPinProtection(false)
                        .setSkipBackup(true)
                        .build());

                refreshGui();
            }
        });
        btnRecoverWallet.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isV2)
                    startActivityNextStep(TrezorRecoveryInitV2Activity.createIntent(view.getContext()));
                else
                    startActivityNextStep(TrezorRecoveryInitActivity.createIntent(view.getContext()));
            }
        });

        if (gct.getFirmwareReleases(isV2).getNewest().version.isNewerThan(FirmwareVersion.create(features))) {
            btnFirmwareUpgradeAvail.setVisibility(View.VISIBLE);
            btnFirmwareUpgradeAvail.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(FirmwareUpgradeActivity.createIntent(view.getContext(), new MsgWrp(features)));
                    setDontDisconnectOnStop();
                }
            });
        }
        else
            btnFirmwareUpgradeAvail.setVisibility(View.GONE);
    }


    //
    // CUSTOM CALLBACKS
    //

    @Override
    public void onTrezorTaskCompletedSucc(String id, TrezorTaskResult res) {
        if (id.equals(TASK_RESET_DEVICE)) {
            if (res.getMsgResult().msgType == MessageType.MessageType_Success) {
                finish();

                TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(this);
                taskStackBuilder.addNextIntent(MainActivity.createIntent(this));
                taskStackBuilder.addNextIntent(CreateBackupInitActivity.createIntent(this, CommonUtils.isTrezorV2(features)));
                taskStackBuilder.startActivities();

                setDontDisconnectOnStop();
                //startActivityNextStep(CreateBackupInitActivity.createIntent(this, CommonUtils.isTrezorV2(features)));
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
