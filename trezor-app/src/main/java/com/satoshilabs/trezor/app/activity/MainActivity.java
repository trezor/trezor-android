package com.satoshilabs.trezor.app.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.circlegate.liban.base.ApiBase.ApiCreator;
import com.circlegate.liban.base.ApiBase.ApiParcelable;
import com.circlegate.liban.base.ApiDataIO.ApiDataInput;
import com.circlegate.liban.base.ApiDataIO.ApiDataOutput;
import com.circlegate.liban.base.CommonClasses.Couple;
import com.circlegate.liban.base.Exceptions.NotImplementedException;
import com.circlegate.liban.dialog.PromptDialog;
import com.circlegate.liban.dialog.PromptDialog.OnPromptDialogDone;
import com.circlegate.liban.task.TaskErrors.BaseError;
import com.circlegate.liban.task.TaskErrors.ITaskError;
import com.circlegate.liban.utils.ActivityUtils;
import com.circlegate.liban.utils.EqualsUtils;
import com.circlegate.liban.utils.LogUtils;
import com.circlegate.liban.utils.ViewUtils;
import com.google.protobuf.ByteString;
import com.satoshilabs.trezor.app.R;
import com.satoshilabs.trezor.app.activity.ChangeHomescreenActivity.ChangeHomescreenActivityResult;
import com.satoshilabs.trezor.app.activity.base.BaseActivity;
import com.satoshilabs.trezor.app.common.GlobalContext;
import com.satoshilabs.trezor.app.common.GlobalContext.FirmwareRelease;
import com.satoshilabs.trezor.app.common.GlobalContext.FirmwareReleases;
import com.satoshilabs.trezor.app.common.GlobalContext.FirmwareVersion;
import com.satoshilabs.trezor.app.common.NavDrawer;
import com.satoshilabs.trezor.app.common.TrezorTasks.MsgWrp;
import com.satoshilabs.trezor.app.common.TrezorTasks.TrezorError;
import com.satoshilabs.trezor.app.common.TrezorTasks.TrezorTaskParam;
import com.satoshilabs.trezor.app.common.TrezorTasks.TrezorTaskResult;
import com.satoshilabs.trezor.app.db.CommonDb.InitializedTrezor;
import com.satoshilabs.trezor.app.db.CommonDb.TrezorListChangedReceiver;
import com.satoshilabs.trezor.app.dialog.CheckConfirmedDialog;
import com.satoshilabs.trezor.app.dialog.CheckConfirmedDialog.OnCheckConfirmedDialogDone;
import com.satoshilabs.trezor.app.dialog.EnterTextDialog;
import com.satoshilabs.trezor.app.dialog.EnterTextDialog.OnEnterTextDialogDone;
import com.satoshilabs.trezor.app.style.CustomHtml;
import com.satoshilabs.trezor.app.utils.CommonUtils;
import com.satoshilabs.trezor.app.view.CustomActionBar;
import com.satoshilabs.trezor.lib.TrezorManager.UsbPermissionReceiver;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.ApplySettings;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.ButtonAck;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.ChangePin;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.Failure;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.Features;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.Initialize;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.MessageType;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.WipeDevice;

public class MainActivity extends BaseActivity implements OnPromptDialogDone, OnEnterTextDialogDone, OnCheckConfirmedDialogDone {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String TASK_TRY_CONNECT_TREZOR = "TASK_TRY_CONNECT_TREZOR";
    private static final String TASK_CHANGE_SETTING_PREFIX = "TASK_CHANGE_SETTING:";

    private static final String POSTFIX_COMMON = "COMMON";
    private static final String POSTFIX_DISCONNECT_TO_APPLY_SETTING = "DISCONNECT_TO_APPLY_SETTING";
    private static final String POSTFIX_DISCONNECT_WIPED = "DISCONNECT_WIPED";

    private static final String DIALOG_CONFIRM_ACTION = "DIALOG_CONFIRM_ACTION";
    private static final String DIALOG_CHANGE_LABEL = "DIALOG_CHANGE_LABEL";
    private static final String DIALOG_DISABLE_PIN = "DIALOG_DISABLE_PIN";
    private static final String DIALOG_DISABLE_PASSPHRASE= "DIALOG_DISABLE_PASSPHRASE";
    private static final String DIALOG_WIPE = "DIALOG_WIPE";
    private static final String DIALOG_DISCONNECT_TREZOR = "DIALOG_DISCONNECT_TREZOR";
    private static final String DIALOG_FORGET_DEVICE = "DIALOG_FORGET_DEVICE";


    private NavDrawer navDrawer;
    private ProgressBar progressBar;
    private NestedScrollView nestedScrollView;
    private View rootDisconnected;
    private View rootConnected;

    private Button btnLaunchMycelium;
    private Button btnChangeLabel;
    private Button btnChangeHomescreen;
    private TextView txtSecuritySetup;
    private ViewHolderSwitch switchPin;
    private Button btnChangePin;
    private ViewHolderSwitch switchPassphrase;
    private TextView txtDeviceSetup;
    private Button btnFirmwareVersion;
    //private Button btnForgetDevice;
    private Button btnWipeDevice;
    private Button btnNotification;

    private GlobalContext gct;

    // Saved state
    private InitializedTrezor initializedTrezor; // trezor nemusi byt aktualne pripojen
    private String connectedTrezorDeviceId = "";
    private long unlockedTimestamp = Long.MIN_VALUE; // cas v System.elapsedTime, kdy byl trezor odemknut
    private PromptDialog dialogConfirmAction;
    private PromptDialog dialogDisconnectTrezor;


    public static Intent createIntent(Context context) {
        Intent ret = new Intent(context, MainActivity.class);
        ret.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return ret;
    }

    //
    // LIFECYCLE CALLBACKS
    //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Couple<ViewGroup, CustomActionBar> rootWithActionBar = CustomActionBar.inflateWithActionBar(this, R.layout.main_activity);
        this.navDrawer = NavDrawer.setContentView(this, rootWithActionBar.getFirst());
        if (!getResources().getBoolean(R.bool.nav_drawer_permanent))
            rootWithActionBar.getSecond().setNavigationButtonNavDrawer(navDrawer);

        this.progressBar = (ProgressBar)findViewById(R.id.progress_bar);
        this.nestedScrollView = (NestedScrollView)findViewById(R.id.nested_scroll_view);
        this.rootDisconnected = findViewById(R.id.root_disconnected);
        this.rootConnected = findViewById(R.id.root_connected);

        this.btnLaunchMycelium = (Button)findViewById(R.id.btn_launch_mycelium);
        this.btnChangeLabel = (Button)findViewById(R.id.btn_change_label);
        this.btnChangeHomescreen = (Button)findViewById(R.id.btn_change_homescreen);
        this.txtSecuritySetup = (TextView) findViewById(R.id.txt_security_setup);
        this.switchPin = new ViewHolderSwitch(findViewById(R.id.switch_pin));
        this.btnChangePin = (Button)findViewById(R.id.btn_change_pin);
        this.switchPassphrase = new ViewHolderSwitch(findViewById(R.id.switch_passphrase));
        this.txtDeviceSetup = (TextView) findViewById(R.id.txt_device_setup);
        this.btnFirmwareVersion = (Button)findViewById(R.id.btn_firmware_version);
        //this.btnForgetDevice = (Button)findViewById(R.id.btn_forget_device);
        this.btnWipeDevice = (Button)findViewById(R.id.btn_wipe_device);
        this.btnNotification = (Button)findViewById(R.id.btn_notification);

        this.gct = GlobalContext.get();

        this.dialogConfirmAction = (PromptDialog)getSupportFragmentManager().findFragmentByTag(DIALOG_CONFIRM_ACTION);
        this.dialogDisconnectTrezor = (PromptDialog)getSupportFragmentManager().findFragmentByTag(DIALOG_DISCONNECT_TREZOR);

        btnLaunchMycelium.setText(R.string.main_activity_launch_mycelium);
        btnChangeLabel.setText(R.string.main_activity_change_label);
        btnChangeHomescreen.setText(R.string.main_activity_change_homescreen);
        txtSecuritySetup.setText(R.string.main_activity_security_setup);
        switchPin.text.setText(R.string.main_activity_pin);
        btnChangePin.setText(R.string.main_activity_change_pin);
        setupTwoLinesText(switchPassphrase.text, R.string.main_activity_passphrase_title, R.string.main_activity_passphrase_text);
        txtDeviceSetup.setText(R.string.main_activity_device_setup);
        btnFirmwareVersion.setText(R.string.main_activity_firmware_version);
        //setupTwoLinesText(btnForgetDevice, R.string.main_activity_forget_device_title, R.string.main_activity_forget_device_text);
        setupTwoLinesText(btnWipeDevice, R.string.main_activity_wipe_device_title, R.string.main_activity_wipe_device_text);

        btnLaunchMycelium.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    String myceliumUri = "com.mycelium.wallet";
                    Intent launchIntent = getPackageManager().getLaunchIntentForPackage(myceliumUri);
                    if (launchIntent != null) {
                        startActivity(launchIntent);
                    }
                    else {
                        try {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + myceliumUri)));
                        } catch (android.content.ActivityNotFoundException anfe) {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + myceliumUri)));
                        }
                    }
                }
                catch (Exception ex) {
                    LogUtils.e(TAG, "btnLaunchMycelium onClick thrown exception", ex);
                    BaseError.ERR_UNKNOWN_ERROR.showToast(gct);
                }
//                if (initializedTrezor != null) {
//                    startActivity(XpubActivity.createIntent(view.getContext(), initializedTrezor.getPublicKey().getXpub()));
//                }
            }
        });
        btnChangeLabel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (initializedTrezor != null && initializedTrezor.getFeatures().getDeviceId().equals(connectedTrezorDeviceId)) {
                    EnterTextDialog.show(getSupportFragmentManager(), null, DIALOG_CHANGE_LABEL, DIALOG_CHANGE_LABEL,
                            getString(R.string.main_activity_change_label),
                            getString(R.string.main_activity_change_label_prompt),
                            getString(R.string.init_trezor_device_label_hint),
                            "");
                }
            }
        });
        btnChangeHomescreen.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (initializedTrezor != null && initializedTrezor.getFeatures().getDeviceId().equals(connectedTrezorDeviceId)) {
                    startActivityForResult(ChangeHomescreenActivity.createIntent(view.getContext(), initializedTrezor.getFeatures().getLabel(), TextUtils.equals(initializedTrezor.getFeatures().getModel(), "T")), GlobalContext.RQC_PICK_HOMESCREEN);
                    setDontDisconnectOnStop();
                }
            }
        });
        switchPin.button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (initializedTrezor != null && initializedTrezor.getFeatures().getDeviceId().equals(connectedTrezorDeviceId)) {
                    if (initializedTrezor.getFeatures().getPinProtection()) {
                        CheckConfirmedDialog.show(getSupportFragmentManager(), null, DIALOG_DISABLE_PIN, DIALOG_DISABLE_PIN,
                                getString(R.string.warning),
                                getString(R.string.main_activity_disable_pin_prompt),
                                getString(R.string.i_understand),
                                getString(R.string.main_activity_disable_pin));
                    }
                    else {
                        executeTrezorTaskIfCan(TASK_CHANGE_SETTING_PREFIX + POSTFIX_COMMON, ChangePin.newBuilder().build());
                        refreshGui();
                    }
                }
            }
        });
        btnChangePin.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (initializedTrezor != null && initializedTrezor.getFeatures().getDeviceId().equals(connectedTrezorDeviceId)) {
                    executeTrezorTaskIfCan(TASK_CHANGE_SETTING_PREFIX + POSTFIX_COMMON, ChangePin.newBuilder().build());
                }
            }
        });
        switchPassphrase.button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (initializedTrezor != null && initializedTrezor.getFeatures().getDeviceId().equals(connectedTrezorDeviceId)) {
                    if (initializedTrezor.getFeatures().getPassphraseProtection()) {
                        CheckConfirmedDialog.show(getSupportFragmentManager(), null, DIALOG_DISABLE_PASSPHRASE, DIALOG_DISABLE_PASSPHRASE,
                                getString(R.string.warning),
                                getString(R.string.main_activity_disable_passphrase_prompt),
                                getString(R.string.i_understand),
                                getString(R.string.main_activity_disable_passphrase));
                    }
                    else {
                        executeTrezorTaskIfCan(TASK_CHANGE_SETTING_PREFIX + POSTFIX_COMMON, ApplySettings.newBuilder().setUsePassphrase(true).build());
                        refreshGui();
                    }
                }
            }
        });
//        btnForgetDevice.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                PromptDialog.show(getSupportFragmentManager(), null, DIALOG_FORGET_DEVICE, DIALOG_FORGET_DEVICE, getString(R.string.main_activity_forget_device_title), getString(R.string.main_activity_forget_device_prompt),
//                        true, true, true, null, getString(R.string.main_activity_forget_device_title), getString(R.string.cancel));
//            }
//        });
        btnWipeDevice.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (initializedTrezor != null && initializedTrezor.getFeatures().getDeviceId().equals(connectedTrezorDeviceId)) {
                    CheckConfirmedDialog.show(getSupportFragmentManager(), null, DIALOG_WIPE, DIALOG_WIPE,
                            getString(R.string.warning),
                            CustomHtml.fromHtmlWithCustomSpans(CustomHtml.getFontColorTag(getString(R.string.main_activity_wipe_device_prompt_text), getResources().getColor(R.color.text_problem))),
                            getString(R.string.i_understand),
                            getString(R.string.main_activity_wipe_device_title));
                }
            }
        });

        this.initializedTrezor = null;
        this.connectedTrezorDeviceId = savedInstanceState != null ? savedInstanceState.getString("connectedTrezorDeviceId") : "";
        this.unlockedTimestamp = savedInstanceState != null ? savedInstanceState.getLong("unlockedTimestamp") : Long.MIN_VALUE;
        String lastSelectedDeviceId = gct.getCommonDb().getLastSelectedDeviceId();

        for (InitializedTrezor t : gct.getCommonDb().getTrezors()) {
            if (EqualsUtils.equalsCheckNull(t.getFeatures().getDeviceId(), lastSelectedDeviceId)) {
                this.initializedTrezor = t;
                break;
            }
        }

        //if (TextUtils.isEmpty(connectedTrezorDeviceId))
        //    executeTryConnectTrezor(false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GlobalContext.RQC_PICK_HOMESCREEN) {
            if (resultCode == RESULT_OK) {
                ChangeHomescreenActivityResult res = ActivityUtils.getResultParcelable(data);
                executeTrezorTaskIfCan(TASK_CHANGE_SETTING_PREFIX + POSTFIX_COMMON, ApplySettings.newBuilder().setHomescreen(ByteString.copyFrom(res.homescreen)).build());
            }
        }
        else
            super.onActivityResult(requestCode, resultCode, data);
    }

    // pozor - musi byt onResume, kvuli typu aktivity singleTask v manifestu (onResume je vzdy volan po onNewIntent...)
    @Override
    protected void onResume() {
        super.onResume();
        usbPermissionReceiver.register(this);
        navDrawer.trezorListChangedReceiver.register(this);
        navDrawer.trezorListChangedReceiver.onTrezorListChanged(true, true);
        trezorListChangedReceiver.register(this);

        executeTryConnectTrezor(false);
        //if (!TextUtils.isEmpty(connectedTrezorDeviceId))
        //    executeTrezorTaskIfCan(TASK_TRY_CONNECT_TREZOR, GetFeatures.newBuilder().build());
        refreshGui();
    }

    @Override
    protected void onPause() {
        super.onPause();
        usbPermissionReceiver.unregister(this);
        navDrawer.trezorListChangedReceiver.unregister(this);
        trezorListChangedReceiver.register(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("connectedTrezorDeviceId", connectedTrezorDeviceId);
        outState.putLong("unlockedTimestamp", unlockedTimestamp);
    }


    //
    // CUSTOM CALLBACKS
    //


    @Override
    public void beforeTrezorTaskCompleted(String id, TrezorTaskResult res) {
        super.beforeTrezorTaskCompleted(id, res);

        if (dialogConfirmAction != null) {
            dialogConfirmAction.dismiss();
            dialogConfirmAction = null;
        }
    }

    @Override
    public void onTrezorTaskCompletedSucc(String id, TrezorTaskResult res) {
        if (id.equals(TASK_TRY_CONNECT_TREZOR)) {
            if (res.getMsgResult().msgType == MessageType.MessageType_Features) {
                Features features = (Features)res.getMsgResult().msg;
                TryConnectTag tag = (TryConnectTag)res.getParam().getTag();

                if (features.getBootloaderMode()) {
                    finish();
                    startActivity(FirmwareUpgradeActivity.createIntent(this, res.getMsgResult()));
                    if (!tag.canAnimateNextScreen)
                        overridePendingTransition(0, 0);
                    setDontDisconnectOnStop();
                }
                else if (gct.getFirmwareReleases(CommonUtils.isTrezorV2(features)).isUpdateRequired(FirmwareVersion.create(features))) {
                    finish();
                    startActivity(FirmwareUpgradeActivity.createIntent(this, res.getMsgResult()));
                    if (!tag.canAnimateNextScreen)
                        overridePendingTransition(0, 0);
                    setDontDisconnectOnStop();
                }

                else if (!features.getInitialized()) {
                    finish();
                    startActivity(InitTrezorActivity.createIntent(this, res.getMsgResult()));
                    if (!tag.canAnimateNextScreen)
                        overridePendingTransition(0, 0);
                    setDontDisconnectOnStop();
                }
                else
                    setupConnectedTrezor(features);
//                    if (this.initializedTrezor != null && (EqualsUtils.equalsCheckNull(this.initializedTrezor.getFeatures().getDeviceId(), features.getDeviceId()))) {
//                        setupConnectedTrezor(features, initializedTrezor.getPublicKey());
//                    }
//                    else {
//                        GetPublicKey msg = GetPublicKey.newBuilder()
//                                .addAddressN((int)2147483692L)
//                                .addAddressN((int)2147483648L)
//                                .addAddressN((int)2147483648L)
//                                .build();
//
//                        executeTrezorTaskIfCan(id, new TrezorTaskParam(msg, new MsgWrp(features)));
//                        refreshGui();
//                    }

            }
            //else if (res.getMsgResult().msgType == MessageType.MessageType_PublicKey) {
            //    setupConnectedTrezor((Features) ((MsgWrp)res.getParam().getTag()).msg, (PublicKey)res.getMsgResult().msg);
            //}
            else if (res.getMsgResult().msgType == MessageType.MessageType_Failure){
                showToastTrezorMsgFailure((Failure)res.getMsgResult().msg);
                showDialogDisconnectTrezor("", getString(R.string.main_activity_disconnect_now));
                refreshGui();
            }
            else
                onTrezorError(TrezorError.ERR_UNEXPECTED_RESPONSE);
        }
        else if (id.startsWith(TASK_CHANGE_SETTING_PREFIX)) {
            if (res.getMsgResult().msgType == MessageType.MessageType_Success) {
                if (id.endsWith(POSTFIX_DISCONNECT_TO_APPLY_SETTING))
                    showDialogDisconnectTrezor("", getString(R.string.main_activity_disconnect_to_apply_setting));
                else if (id.endsWith(POSTFIX_DISCONNECT_WIPED)) {
                    showDialogDisconnectTrezor("", getString(R.string.main_activity_disconnect_now));
                    if (initializedTrezor != null) {
                        gct.getCommonDb().removeTrezor(initializedTrezor);
                    }
                }
                else {
                    executeTryConnectTrezor(true);
                    refreshGui();
                }
            }
            else if (res.getMsgResult().msgType == MessageType.MessageType_ButtonRequest) {
                executeButtonAckAndShowDialog("", getString(R.string.confirm_action_on_trezor), id);
                refreshGui();
            }
            else if (res.getMsgResult().msgType == MessageType.MessageType_Failure) {
                showToastTrezorMsgFailure((Failure)res.getMsgResult().msg);
                refreshGui();
            }
            else
                onTrezorError(TrezorError.ERR_UNEXPECTED_RESPONSE);
        }
        else
            throw new NotImplementedException();
    }

    @Override
    public void onTrezorConnected() {
        super.onTrezorConnected();
        executeTryConnectTrezor(true);
        refreshGui();
    }

    @Override
    public void onTrezorError(ITaskError error) {
        // schvalne nevolam super - komplet vyridim zde

        if (dialogDisconnectTrezor != null) {
            dialogDisconnectTrezor.dismiss();
            dialogDisconnectTrezor = null;
        }

        this.connectedTrezorDeviceId = "";

        if (TrezorError.ERR_NEEDS_PERMISSION.equals(error)) {
            gct.getTrezorManager().requestDevicePermissionIfCan(false);
        }
        else {
            gct.getCommonDb().removeAllTrezors();
            gct.executeDisconnectTrezorTask();

            if (!TrezorError.ERR_NOT_CONNECTED.equals(error))
                error.showToast(gct);
        }

        refreshGui();
    }

    @Override
    public void onPromptDialogDone(String id, boolean canceled, Bundle bundle) {
        if (id.equals(DIALOG_FORGET_DEVICE)) {
            if (!canceled && initializedTrezor != null) {
                if (EqualsUtils.equalsCheckNull(initializedTrezor.getFeatures().getDeviceId(), connectedTrezorDeviceId))
                    showDialogDisconnectTrezor("", getString(R.string.main_activity_disconnect_now));

                gct.getCommonDb().removeTrezor(initializedTrezor);
            }
        }
        else
            throw new NotImplementedException();
    }

    @Override
    public void onEnterTextDialogDone(String id, boolean canceled, String text) {
        if (id.equals(DIALOG_CHANGE_LABEL)) {
            if (!canceled) {
                executeTrezorTaskIfCan(TASK_CHANGE_SETTING_PREFIX + POSTFIX_COMMON, ApplySettings.newBuilder().setLabel(text).build());
                refreshGui();
            }
        }
        else
            throw new NotImplementedException();
    }

    @Override
    public void onCheckConfirmedDialogDone(String id, boolean canceled) {
        if (id.equals(DIALOG_DISABLE_PIN)) {
            if (!canceled) {
                executeTrezorTaskIfCan(TASK_CHANGE_SETTING_PREFIX + POSTFIX_COMMON, ChangePin.newBuilder().setRemove(true).build());
                refreshGui();
            }
        }
        else if (id.equals(DIALOG_DISABLE_PASSPHRASE)) {
            if (!canceled) {
                executeTrezorTaskIfCan(TASK_CHANGE_SETTING_PREFIX + POSTFIX_COMMON, ApplySettings.newBuilder().setUsePassphrase(false).build());
                refreshGui();
            }
        }
        else if (id.equals(DIALOG_WIPE)) {
            if (!canceled) {
                executeTrezorTaskIfCan(TASK_CHANGE_SETTING_PREFIX + POSTFIX_DISCONNECT_WIPED, WipeDevice.newBuilder().build());
                refreshGui();
            }
        }
        else
            throw new NotImplementedException();
    }


    //
    // PRIVATE
    //

    private void setupTwoLinesText(TextView text, int ridTitle, int ritText) {
        text.setText(CustomHtml.fromHtmlWithCustomSpans(getString(ridTitle)
                + CustomHtml.getTextSizeTag("\n" + String.valueOf(CustomHtml.HARD_SPACE) + "\n", ViewUtils.getPixelsFromDp(this, 3))
                + CustomHtml.getTextSizeTag(CustomHtml.getFontColorTag(getString(ritText), getResources().getColor(R.color.text_primary2)), getResources().getDimensionPixelSize(R.dimen.text_body_1))));
    }

    private void setupConnectedTrezor(Features features/*, PublicKey publicKey*/) {
        this.initializedTrezor = new InitializedTrezor(features);
        this.connectedTrezorDeviceId = features.getDeviceId();
        gct.getCommonDb().addOrUpdateCurrentTrezor(initializedTrezor);
        refreshGui();
    }

    private void refreshGui() {
        if (this.dialogDisconnectTrezor != null
                || getTaskFragment().containsAnyTaskByFragmentTag(null))
        {
            progressBar.setVisibility(View.VISIBLE);
            nestedScrollView.setVisibility(View.GONE);
            btnNotification.setVisibility(View.GONE);
        }
        else {
            progressBar.setVisibility(View.GONE);
            nestedScrollView.setVisibility(View.VISIBLE);

            if (initializedTrezor == null) {
                btnNotification.setVisibility(View.GONE);
                rootDisconnected.setVisibility(View.VISIBLE);
                rootConnected.setVisibility(View.GONE);
                getSupportActionBar().setTitle(R.string.app_name);
            }
            else {
                final Features f = initializedTrezor.getFeatures();

                if (f.getNeedsBackup()) {
                    btnNotification.setVisibility(View.VISIBLE);
                    btnNotification.setText(R.string.main_activity_notif_not_backed_up);
                    btnNotification.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivity(CreateBackupInitActivity.createIntent(v.getContext(), CommonUtils.isTrezorV2(f)));
                            setDontDisconnectOnStop();
                        }
                    });
                }
                else if (gct.getFirmwareReleases(CommonUtils.isTrezorV2(f)).getNewest().version.isNewerThan(FirmwareVersion.create(f))) {
                    btnNotification.setVisibility(View.VISIBLE);
                    btnNotification.setText(R.string.firmware_upgrade_avail);
                    btnNotification.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivity(FirmwareUpgradeActivity.createIntent(v.getContext(), new MsgWrp(f)));
                            setDontDisconnectOnStop();
                        }
                    });
                }
                else if (TextUtils.isEmpty(f.getLabel()) || TextUtils.equals(f.getLabel(), getString(R.string.init_trezor_device_label_default))) {
                    btnNotification.setVisibility(View.VISIBLE);
                    btnNotification.setText(R.string.main_activity_notif_not_named);
                    btnNotification.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivity(NameDeviceInitActivity.createIntent(v.getContext(), CommonUtils.isTrezorV2(f)));
                            setDontDisconnectOnStop();
                        }
                    });
                }
                else if (!f.getPinProtection()) {
                    btnNotification.setVisibility(View.VISIBLE);
                    btnNotification.setText(R.string.main_activity_notif_no_pin);
                    btnNotification.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivity(SetupPinActivity.createIntent(v.getContext(), CommonUtils.isTrezorV2(f), f.getLabel()));
                            setDontDisconnectOnStop();
                        }
                    });
                }
                else
                    btnNotification.setVisibility(View.GONE);

                rootDisconnected.setVisibility(View.GONE);
                rootConnected.setVisibility(View.VISIBLE);

                getSupportActionBar().setTitle(f.getLabel());
                switchPin.switchCompat.setChecked(f.getPinProtection());
                switchPassphrase.switchCompat.setChecked(f.getPassphraseProtection());
                btnFirmwareVersion.setText(getString(R.string.main_activity_firmware_version).replace("^d^", f.getMajorVersion() + "." + f.getMinorVersion() + "." + f.getPatchVersion()));

                boolean trezorConnected = initializedTrezor != null && EqualsUtils.equalsCheckNull(initializedTrezor.getFeatures().getDeviceId(), connectedTrezorDeviceId);

                btnChangeLabel.setEnabled(trezorConnected);
                btnChangeHomescreen.setEnabled(trezorConnected);
                switchPin.setEnabled(trezorConnected);
                btnChangePin.setEnabled(trezorConnected && f.getPinProtection());
                switchPassphrase.setEnabled(trezorConnected);
                btnFirmwareVersion.setEnabled(trezorConnected);
                btnWipeDevice.setEnabled(trezorConnected);
            }
        }
    }

    private void executeTryConnectTrezor(boolean canAnimateNextScreen) {
        executeTrezorTaskIfCan(TASK_TRY_CONNECT_TREZOR, new TrezorTaskParam(Initialize.newBuilder().build(), new TryConnectTag(canAnimateNextScreen)));
    }

    private void executeButtonAckAndShowDialog(CharSequence dialogTitle, CharSequence dialogText, String taskId) {
        this.dialogConfirmAction = PromptDialog.show(getSupportFragmentManager(), dialogConfirmAction, DIALOG_CONFIRM_ACTION, DIALOG_CONFIRM_ACTION, dialogTitle, dialogText, false, false, false, null, "", "");
        executeTrezorTaskIfCan(taskId, ButtonAck.newBuilder().build());
    }

    private void showDialogDisconnectTrezor(CharSequence dialogTitle, CharSequence dialogText) {
        this.dialogDisconnectTrezor = PromptDialog.show(getSupportFragmentManager(), dialogDisconnectTrezor, DIALOG_DISCONNECT_TREZOR, DIALOG_DISCONNECT_TREZOR, dialogTitle, dialogText, false, false, false, null, "", "");
    }


    private final UsbPermissionReceiver usbPermissionReceiver = new UsbPermissionReceiver() {
        @Override
        public void onUsbPermissionResult(boolean granted) {
            LogUtils.d(TAG, "usbPermissionReceiver: onUsbPermissionResult: granted = " + granted);

            if (granted)
                executeTryConnectTrezor(true);
        }
    };

    private final TrezorListChangedReceiver trezorListChangedReceiver = new TrezorListChangedReceiver() {
        @Override
        public void onTrezorListChanged(boolean listChanged, boolean lastSelectedDeviceIdChanged) {

            if (lastSelectedDeviceIdChanged) {
                String lastDeviceId = gct.getCommonDb().getLastSelectedDeviceId();

                if (!EqualsUtils.equalsCheckNull(lastDeviceId, initializedTrezor != null ? initializedTrezor.getFeatures().getLabel() : "")) {
                    initializedTrezor = null;
                    for (InitializedTrezor t : gct.getCommonDb().getTrezors()) {
                        if (EqualsUtils.equalsCheckNull(t.getFeatures().getDeviceId(), lastDeviceId)) {
                            initializedTrezor = t;
                            break;
                        }
                    }

                    refreshGui();
                }
            }
        }
    };


    //
    // INNER CLASSES
    //

    private static class TryConnectTag extends ApiParcelable {
        public final boolean canAnimateNextScreen;

        public TryConnectTag(boolean canAnimateNextScreen) {
            this.canAnimateNextScreen = canAnimateNextScreen;
        }

        public TryConnectTag(ApiDataInput d) {
            this.canAnimateNextScreen = d.readBoolean();
        }

        @Override
        public void save(ApiDataOutput d, int flags) {
            d.write(this.canAnimateNextScreen);
        }

        public static final ApiCreator<TryConnectTag> CREATOR = new ApiCreator<TryConnectTag>() {
            public TryConnectTag create(ApiDataInput d) { return new TryConnectTag(d); }
            public TryConnectTag[] newArray(int size) { return new TryConnectTag[size]; }
        };
    }

    private static class ViewHolderSwitch {
        public final View button;
        public final TextView text;
        public final SwitchCompat switchCompat;

        public ViewHolderSwitch(View button) {
            this.button = button;

            this.text = (TextView)button.findViewById(R.id.text);
            this.switchCompat = (SwitchCompat)button.findViewById(R.id.switch_compat);
        }

        public void setEnabled(boolean enabled) {
            if (this.button.isEnabled() != enabled) {
                this.button.setEnabled(enabled);
                this.text.setEnabled(enabled);
                this.switchCompat.setEnabled(enabled);
            }
        }
    }
}
