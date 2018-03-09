package com.satoshilabs.trezor.app.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;

import com.circlegate.liban.base.Exceptions.NotImplementedException;
import com.circlegate.liban.task.TaskInterfaces.ITask;
import com.satoshilabs.trezor.app.R;
import com.satoshilabs.trezor.app.activity.base.BaseSetupActivity;
import com.satoshilabs.trezor.app.common.TrezorTasks.TrezorError;
import com.satoshilabs.trezor.app.common.TrezorTasks.TrezorTaskParam;
import com.satoshilabs.trezor.app.common.TrezorTasks.TrezorTaskResult;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.BackupDevice;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.ButtonAck;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.MessageType;

public class CreateBackupRecoverySeedV2Activity extends BaseSetupActivity {
    private static final String TASK_BACKUP_DEVICE = "TASK_BACKUP_DEVICE";

    public static Intent createIntent(Context context) {
        return setupIntent(new Intent(context, CreateBackupRecoverySeedV2Activity.class), true);
    }

    //
    // LIFECYCLE CALLBACKS
    //


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // chceme aby zustal displej zapnuty
        super.onCreate(savedInstanceState);
        setRootContentLayout(R.layout.create_backup_recovery_seed_v2_activity);

        executeTrezorTaskIfCan(TASK_BACKUP_DEVICE, BackupDevice.newBuilder().build());
    }

    @Override
    public void onBackPressed() {
        // nechceme, aby se odsud dalo vybackovat
    }


    //
    // CUSTOM CALLBACKS
    //


    @Override
    protected boolean canHandleButtonReqByDefAction() {
        return false;
    }

    @Override
    protected boolean getNoUpButton() {
        return true;
    }

    @Override
    public void onTrezorTaskCompletedSucc(String id, TrezorTaskResult res) {
        if (id.equals(TASK_BACKUP_DEVICE)) {
            if (res.getMsgResult().msgType == MessageType.MessageType_ButtonRequest) {
                executeTrezorTaskIfCan(TASK_BACKUP_DEVICE, ButtonAck.newBuilder().build());
                refreshGui();
            }
            else if (res.getMsgResult().msgType == MessageType.MessageType_Success) {
                finish();
                startActivityNextStep(SetupStepCompletedActivity.createIntent(this, isV2, SetupStepCompletedActivity.STEP_CREATE_BACKUP));
            }
            else {
                onTrezorError(TrezorError.ERR_UNEXPECTED_RESPONSE);
            }
        }
        else
            throw new NotImplementedException();
    }

    @Override
    protected boolean forceDisplayContent() {
        ITask task = getTaskFragment().getTask(TASK_BACKUP_DEVICE, null);
        return task != null && task.getParam() instanceof TrezorTaskParam && ((TrezorTaskParam)task.getParam()).getMsgParam().msgType == MessageType.MessageType_ButtonAck;
    }
}
