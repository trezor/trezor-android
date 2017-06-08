package com.satoshilabs.trezor.app.activity.base;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.Toast;

import com.circlegate.liban.base.BaseBroadcastReceivers.BaseLocalReceiver;
import com.circlegate.liban.dialog.DialogsFragment;
import com.circlegate.liban.dialog.DialogsFragment.IDialogsFragmentActivity;
import com.circlegate.liban.fragment.BaseFragmentCommon.IBaseFragmentActivity;
import com.circlegate.liban.fragment.BaseFragmentCommon.OnBackPressedListener;
import com.circlegate.liban.task.TaskErrors.ITaskError;
import com.circlegate.liban.task.TaskFragment;
import com.circlegate.liban.task.TaskFragment.ITaskFragmentActivity;
import com.circlegate.liban.task.TaskInterfaces.ITaskResult;
import com.circlegate.liban.task.TaskInterfaces.ITaskResultListener;
import com.circlegate.liban.utils.ActivityUtils;
import com.circlegate.liban.utils.EqualsUtils;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.satoshilabs.trezor.app.R;
import com.satoshilabs.trezor.app.activity.EnterPassphraseActivity;
import com.satoshilabs.trezor.app.activity.EnterPassphraseActivity.EnterPassphraseActivityParam;
import com.satoshilabs.trezor.app.activity.EnterPassphraseActivity.EnterPassphraseActivityResult;
import com.satoshilabs.trezor.app.activity.EnterPinActivity;
import com.satoshilabs.trezor.app.activity.EnterPinActivity.EnterPinActivityParam;
import com.satoshilabs.trezor.app.activity.EnterPinActivity.EnterPinActivityResult;
import com.satoshilabs.trezor.app.activity.MainActivity;
import com.satoshilabs.trezor.app.common.GlobalContext;
import com.satoshilabs.trezor.app.common.NavDrawer.INavDrawerCallbacks;
import com.satoshilabs.trezor.app.common.TrezorTasks.TrezorError;
import com.satoshilabs.trezor.app.common.TrezorTasks.TrezorTaskParam;
import com.satoshilabs.trezor.app.common.TrezorTasks.TrezorTaskResult;
import com.satoshilabs.trezor.app.view.CustomActionBar;
import com.satoshilabs.trezor.lib.TrezorManager.TrezorConnectionChangedReceiver;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.Cancel;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.EntropyAck;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.Failure;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.PassphraseAck;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.PinMatrixAck;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.PinMatrixRequest;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseActivity extends AppCompatActivity implements IBaseFragmentActivity, ITaskFragmentActivity, IDialogsFragmentActivity, OnRequestPermissionsResultCallback, ITaskResultListener {
    private GlobalContext gct;
    private TaskFragment taskFragment;
	private DialogsFragment dialogsFragment;
    private final ArrayList<OnBackPressedListener> onBackPressedListeners = new ArrayList<>();
    private final List<Runnable> pendingTasks = new ArrayList<Runnable>();
    private boolean readyToCommitFragments = false;
    private INavDrawerCallbacks navDrawerCallbacks;

    // pokud je false a pokud nedochazi pouze ke zmene konfigurace (nejcasteji kvuli otoceni displeje), pri kazdem volani onStop dojde k odpojeni trezoru
    // aktivita muze nastavit na true, aby pri prechodu z jedne aktivity do druhe nedochazelo ke zbytecnemu odpojovani
    // V kazdem pripade pri kazdem volani onStop (a onCreate) je nastaveno zpet na false
    private boolean dontDisconnectOnStop = false;


    //
    // LIFECYCLE CALLBACKS
    //

	@Override
	protected void onCreate(Bundle savedInstanceState) {
	    this.gct = GlobalContext.get();
        super.onCreate(savedInstanceState);
        onBackPressedListeners.clear(); // pro jistotu...
        readyToCommitFragments = false;
        trezorConnectionChangedReceiver.register(this);
        dontDisconnectOnStop = false;
	}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GlobalContext.RQC_ENTER_PIN) {
            EnterPinActivityResult res = ActivityUtils.getResultParcelable(data);
            executeTrezorTaskIfCan(
                    res.param.taskId,
                    new TrezorTaskParam(resultCode == RESULT_OK ? PinMatrixAck.newBuilder().setPin(res.pinEncoded).build() : Cancel.newBuilder().build(), res.param.tag));
        }
        else if (requestCode == GlobalContext.RQC_ENTER_PASSPHRASE) {
            EnterPassphraseActivityResult res = ActivityUtils.getResultParcelable(data);
            executeTrezorTaskIfCan(
                    res.param.taskId,
                    new TrezorTaskParam(resultCode == RESULT_OK ? PassphraseAck.newBuilder().setPassphrase(res.passphrase).build() : Cancel.newBuilder().build(), res.param.tag));
        }
        else
            super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (this.navDrawerCallbacks != null)
            this.navDrawerCallbacks.onActivityStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.readyToCommitFragments = true;

        ArrayList<Runnable> pendingTasksCopy = new ArrayList<Runnable>(this.pendingTasks);
        this.pendingTasks.clear(); // pro jistotu vymazu uz tady a misto toho prochazim pendingTasksCopy

        for (Runnable r : pendingTasksCopy)
            r.run();
    }

    @Override
    protected void onPause() {
        this.readyToCommitFragments = false;
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (this.navDrawerCallbacks != null)
            this.navDrawerCallbacks.onActivityStop();

        if (!dontDisconnectOnStop && !isChangingConfigurations()) {
            gct.executeDisconnectTrezorTask();
        }
        dontDisconnectOnStop = false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        this.readyToCommitFragments = false;
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        onBackPressedListeners.clear(); // pro jistotu...
        trezorConnectionChangedReceiver.unregister(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        else
            return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, final @NonNull String[] permissions, final @NonNull int[] grantResults) {
        if (isReadyToCommitFragments()) {
            OnRequestPermissionsResultReceiver.sendBroadcast(this, requestCode, permissions, grantResults);
        }
        else {
            addPendingTask(new Runnable() {
                @Override
                public void run() {
                    OnRequestPermissionsResultReceiver.sendBroadcast(BaseActivity.this, requestCode, permissions, grantResults);
                }
            });
        }
    }


    //
    // GETTERS
    //

	@Override
	public TaskFragment getTaskFragment() {
		if (taskFragment == null) {
			this.taskFragment = TaskFragment.getInstance(this);
		}
		return taskFragment;
	}

    @Override
	public DialogsFragment getDialogsFragment() {
		if (dialogsFragment == null) {
			this.dialogsFragment = DialogsFragment.getInstance(this);
		}
		return dialogsFragment;
	}

    @Override
    public void onBackPressed() {
        // projdeme seznam od konce...
        for (int i = onBackPressedListeners.size() - 1; i >= 0; --i) {
            if (onBackPressedListeners.get(i).onBackPressed()) {
                return; // koncime...
            }
        }
        if (!onBackPressedAfterListeners())
            super.onBackPressed();
    }

    public boolean onBackPressedAfterListeners() {
        return false;
    }

    @Override
    public void addOnBackPressedListener(OnBackPressedListener l) {
        onBackPressedListeners.remove(l); // pro jistotu... navic chceme, aby listener byl v seznamu posledni
        onBackPressedListeners.add(l);
    }

    @Override
    public void removeOnBackPressedListener(OnBackPressedListener l) {
        onBackPressedListeners.remove(l);
    }


    public void addPendingTask(Runnable task) {
        this.pendingTasks.add(task);
    }

    public boolean isReadyToCommitFragments() {
        return readyToCommitFragments;
    }

    public void setNavDrawerCallbacks(INavDrawerCallbacks navDrawerCallbacks) {
        this.navDrawerCallbacks = navDrawerCallbacks;
    }


    public void setDontDisconnectOnStop() {
        this.dontDisconnectOnStop = true;
    }

    public void executeTrezorTaskIfCan(String taskId, Message msgParam) {
        executeTrezorTaskIfCan(taskId, new TrezorTaskParam(msgParam));
    }

    public void executeTrezorTaskIfCan(String taskId, TrezorTaskParam param) {
        if (!getTaskFragment().containsAnyTaskByFragmentTag(null)) {
            getTaskFragment().executeTask(taskId, param, null, false, null);
        }
    }

    @Override
    public final void onTaskCompleted(String id, ITaskResult result, Bundle bundle) {
        if (result instanceof TrezorTaskResult) {
            TrezorTaskResult res = (TrezorTaskResult)result;
            beforeTrezorTaskCompleted(id, res);

            if (res.isValidResult()) {
                switch (res.getMsgResult().msgType) {
                    case MessageType_PinMatrixRequest:
                        startActivityForResult(EnterPinActivity.createIntent(this, new EnterPinActivityParam(id, res.getParam().getTag(), ((PinMatrixRequest) res.getMsgResult().msg).getType())),
                                GlobalContext.RQC_ENTER_PIN);
                        setDontDisconnectOnStop();
                        break;

                    case MessageType_EntropyRequest: {
                        SecureRandom random = new SecureRandom();
                        byte[] entropy = new byte[32];
                        random.nextBytes(entropy);
                        EntropyAck param = EntropyAck.newBuilder().setEntropy(ByteString.copyFrom(entropy)).build();
                        executeTrezorTaskIfCan(id, new TrezorTaskParam(param, res.getParam().getTag()));
                        break;
                    }

                    case MessageType_PassphraseRequest: {
                        startActivityForResult(EnterPassphraseActivity.createIntent(this, new EnterPassphraseActivityParam(id, res.getParam().getTag())),
                                GlobalContext.RQC_ENTER_PASSPHRASE);
                        setDontDisconnectOnStop();
                        break;
                    }

                    default:
                        onTrezorTaskCompletedSucc(id, res);
                }
            }
            else {
                onTrezorError(res.getError());
            }
        }
        else
            onOtherTaskCompleted(id, result, bundle);
    }

    public void onOtherTaskCompleted(String id, ITaskResult result, Bundle bundle) {
    }

    public void beforeTrezorTaskCompleted(String id, TrezorTaskResult res) {
    }

    public void onTrezorTaskCompletedSucc(String id, TrezorTaskResult res) {
    }

    public void onTrezorConnected() {
    }

    public void onTrezorError(ITaskError error) {
        error.showToast(gct);

        gct.getCommonDb().removeAllTrezors();
        gct.executeDisconnectTrezorTask();
        finish();
        startActivity(MainActivity.createIntent(BaseActivity.this));
        setDontDisconnectOnStop();

        if (BaseActivity.this instanceof MainActivity) {
            overridePendingTransition(0, 0);
        }
    }

    public void showToastTrezorMsgFailure(Failure failure) {
        final String s;

        if (failure != null) {
            if (failure.hasMessage())
                s = failure.getMessage();
            else if (failure.hasCode())
                s = failure.getCode().name();
            else
                s = getString(R.string.trezor_err_unexpected_response);
        }
        else
            s = getString(R.string.trezor_err_unexpected_response);

        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    private final TrezorConnectionChangedReceiver trezorConnectionChangedReceiver = new TrezorConnectionChangedReceiver() {
        @Override
        public void onTrezorConnectionChanged(boolean connected) {
            pendingTasks.remove(pendingTaskTrezorConnected);
            pendingTasks.remove(pendingTaskTrezorDisconnected);

            Runnable task = connected ? pendingTaskTrezorConnected : pendingTaskTrezorDisconnected;

            if (isReadyToCommitFragments())
                task.run();
            else
                addPendingTask(task);
        }
    };

    private final Runnable pendingTaskTrezorConnected = new Runnable() {
        @Override
        public void run() {
            onTrezorConnected();
        }
    };

    private final Runnable pendingTaskTrezorDisconnected = new Runnable() {
        @Override
        public void run() {
            onTrezorError(TrezorError.ERR_NOT_CONNECTED);
        }
    };


    //
    // INNER CLASSES
    //


    /**
     * POZOR Je potreba zajistit, aby byl receiver zaregistrovan dostatecne brzo - bud v onStart, nebo alespon v onResume pred volanim super.onResume!
     */
    public static abstract class OnRequestPermissionsResultReceiver extends BaseLocalReceiver {
        public static final int RQC_COMMON = 0;
        public static final int RQC_ACCESS_FINE_LOC = 100;

        private static final String ACTION = OnRequestPermissionsResultReceiver.class.getName() + ".ACTION";

        public OnRequestPermissionsResultReceiver() {
            super(ACTION);
        }

        public static void sendBroadcast(Context context, int requestCode, String[] permissions, int[] grantResults) {
            Intent intent = new Intent(ACTION)
                    .putExtra("requestCode", requestCode)
                    .putExtra("permissions", permissions)
                    .putExtra("grantResults", grantResults);
            sendBroadcast(context, intent);
        }

        @Override
        public void onReceiveRegistered(Context context, Intent intent) {
            onRequestPermissionsResultReceived(
                    context,
                    intent.getIntExtra("requestCode", 0),
                    intent.getStringArrayExtra("permissions"),
                    intent.getIntArrayExtra("grantResults")
            );
        }

        public abstract void onRequestPermissionsResultReceived(Context context, int requestCode, String[] permissions, int[] grantResults);

        public static boolean isPermissionGranted(String permissionToFind, String[] permissions, int[] grantResults) {
            int ind = tryFindPermissionInd(permissionToFind, permissions);
            return ind >= 0 && grantResults[ind] == PackageManager.PERMISSION_GRANTED;
        }

        public static int tryFindPermissionInd(String permissionToFind, String[] permissions) {
            for (int i = 0; i < permissions.length; i++) {
                if (EqualsUtils.equalsCheckNull(permissionToFind, permissions[i])) {
                    return i;
                }
            }
            return -1;
        }
    }
}
