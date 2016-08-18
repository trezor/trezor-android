package com.satoshilabs.trezor.app.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.circlegate.liban.base.Exceptions.NotImplementedException;
import com.circlegate.liban.utils.ActivityUtils;
import com.satoshilabs.trezor.app.R;
import com.satoshilabs.trezor.app.activity.EnterPinActivity.EnterPinActivityResult;
import com.satoshilabs.trezor.app.activity.base.BaseActivity;
import com.satoshilabs.trezor.app.common.GlobalContext;
import com.satoshilabs.trezor.app.common.TrezorTasks.MsgWrp;
import com.satoshilabs.trezor.app.common.TrezorTasks.TrezorError;
import com.satoshilabs.trezor.app.common.TrezorTasks.TrezorTaskResult;
import com.satoshilabs.trezor.app.view.CustomActionBar;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.ButtonRequest;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.Failure;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.Features;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.MessageType;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.ResetDevice;
import com.satoshilabs.trezor.lib.protobuf.TrezorType.ButtonRequestType;

public class InitTrezorActivity extends BaseActivity {
    private static final String TASK_RESET_DEVICE = "TASK_RESET_DEVICE";

    // Views
    private View progressBar;
    private View rootContent;
    private EditText editTextDeviceLabel;
    private View btnContinue;
    private View btnTrezorRecovery;
    private Button btnFirmwareUpgradeAvail;

    // Immutable members
    private GlobalContext gct;
    private Features features;



    public static Intent createIntent(Context context, MsgWrp featuresMsg) {
        return new Intent(context, InitTrezorActivity.class).putExtra("featuresMsg", featuresMsg);
    }


    //
    // LIFECYCLE CALLBACKS
    //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CustomActionBar.setContentView(this, R.layout.init_trezor_activity, false);
        this.gct = GlobalContext.get();
        this.features = (Features) getIntent().<MsgWrp>getParcelableExtra("featuresMsg").msg;

        this.progressBar = (View)findViewById(R.id.progress_bar);
        this.rootContent = (View)findViewById(R.id.root_content);
        this.editTextDeviceLabel = (EditText)findViewById(R.id.edit_text_device_label);
        this.btnContinue = (View)findViewById(R.id.btn_continue);
        this.btnTrezorRecovery = (View)findViewById(R.id.btn_trezor_recovery);
        this.btnFirmwareUpgradeAvail = (Button)findViewById(R.id.btn_firmware_upgrade_avail);

        btnContinue.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                String deviceLabel = editTextDeviceLabel.getText().toString();
                if (TextUtils.isEmpty(deviceLabel))
                    deviceLabel = getString(R.string.init_trezor_device_label_default);

                executeTrezorTaskIfCan(TASK_RESET_DEVICE, ResetDevice.newBuilder()
                        .setStrength(256)
                        .setDisplayRandom(false)
                        .setPinProtection(true)
                        .setLabel(deviceLabel)
                        .build());

                refreshGui();
            }
        });
        btnTrezorRecovery.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(TrezorRecoveryInitActivity.createIntent(view.getContext()));
                setDontDisconnectOnStop();
            }
        });
        btnFirmwareUpgradeAvail.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(FirmwareUpgradeActivity.createIntent(view.getContext(), new MsgWrp(features)));
                setDontDisconnectOnStop();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GlobalContext.RQC_ENTER_PIN) {
            if (resultCode == RESULT_OK) {
                EnterPinActivityResult res = ActivityUtils.getResultParcelable(data);
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
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
        if (id.equals(TASK_RESET_DEVICE)) {
            if (res.getMsgResult().msgType == MessageType.MessageType_ButtonRequest && ((ButtonRequest)res.getMsgResult().msg).getCode() == ButtonRequestType.ButtonRequest_ConfirmWord) {
                finish();
                startActivity(RecoverySeedSetupActivity.createIntent(this));
                setDontDisconnectOnStop();
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


    //
    // PRIVATE
    //

    private void refreshGui() {
        if (getTaskFragment().containsAnyTaskByFragmentTag(null)) {
            progressBar.setVisibility(View.VISIBLE);
            rootContent.setVisibility(View.GONE);
            btnFirmwareUpgradeAvail.setVisibility(View.GONE);
        }
        else {
            progressBar.setVisibility(View.GONE);
            rootContent.setVisibility(View.VISIBLE);
            btnFirmwareUpgradeAvail.setVisibility(gct.getBundledFirmwareVersion().isNewerThan(features) ? View.VISIBLE : View.GONE);
        }
    }
}
