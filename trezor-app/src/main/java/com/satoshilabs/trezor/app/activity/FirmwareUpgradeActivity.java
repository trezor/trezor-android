package com.satoshilabs.trezor.app.activity;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.widget.NestedScrollView;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.BulletSpan;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import com.circlegate.liban.base.Exceptions.NotImplementedException;
import com.circlegate.liban.dialog.PromptDialog;
import com.circlegate.liban.task.TaskErrors.BaseError;
import com.circlegate.liban.utils.FragmentUtils;
import com.circlegate.liban.utils.LogUtils;
import com.circlegate.liban.utils.ViewUtils;
import com.google.protobuf.ByteString;
import com.satoshilabs.trezor.app.R;
import com.satoshilabs.trezor.app.activity.base.BaseActivity;
import com.satoshilabs.trezor.app.common.GlobalContext;
import com.satoshilabs.trezor.app.common.GlobalContext.FirmwareVersion;
import com.satoshilabs.trezor.app.common.TrezorTasks.MsgWrp;
import com.satoshilabs.trezor.app.common.TrezorTasks.TrezorError;
import com.satoshilabs.trezor.app.common.TrezorTasks.TrezorTaskResult;
import com.satoshilabs.trezor.app.style.CustomHtml;
import com.satoshilabs.trezor.app.view.CustomActionBar;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.ButtonAck;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.ButtonRequest;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.Failure;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.Features;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.FirmwareErase;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.FirmwareUpload;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.MessageType;
import com.satoshilabs.trezor.lib.protobuf.TrezorType.ButtonRequestType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import eu.inmite.android.lib.dialogs.BaseDialogFragment;

public class FirmwareUpgradeActivity extends BaseActivity {
    private static final String TAG = FirmwareUpgradeActivity.class.getSimpleName();

    private static final String TASK_FIRMWARE_ERASE = "TASK_FIRMWARE_ERASE";
    private static final String TASK_FIRMWARE_UPLOAD = "TASK_FIRMWARE_UPLOAD";

    private static final String DIALOG_PROMPT = FirmwareUpgradeActivity.class.getSimpleName() + ".DIALOG_PROMPT";

    // Views
    private View progressBar;
    private View nestedScrollView;
    private ViewGroup rootContent;
    private TextView txtTitle;
    private View rootDeviceVersion;
    private TextView txtDeviceVersion;
    private TextView txtAvailVersion;
    private TextView txtUpgradeInstructionsTitle;
    private TextView txtUpgradeInstructionsText;
    private TextView txtInitializedTitle;
    private TextView txtInitializedText;
    private View rootButtons;
    private View btnCancel;
    private View btnUpdate;

    // Immutable members
    private GlobalContext gct;
    private Features features;
    private FirmwareVersion availVersion;

    // Saved state
    private DialogFragment dialog;


    public static Intent createIntent(Context context, MsgWrp featuresWrp) {
        return new Intent(context, FirmwareUpgradeActivity.class).putExtra("featuresWrp", featuresWrp);
    }


    //
    // LIFECYCLE CALLBACKS
    //


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // chceme aby zustal displej zapnuty
        super.onCreate(savedInstanceState);

        this.gct = GlobalContext.get();
        this.features = (Features)getIntent().<MsgWrp>getParcelableExtra("featuresWrp").msg;
        this.availVersion = gct.getBundledFirmwareVersion();

        CustomActionBar.setContentView(this, R.layout.firmware_upgrade_activity, !features.getBootloaderMode());

        this.progressBar = (View)findViewById(R.id.progress_bar);
        this.nestedScrollView = (View)findViewById(R.id.nested_scroll_view);
        this.rootContent = (ViewGroup)findViewById(R.id.root_content);
        this.txtTitle = (TextView)findViewById(R.id.txt_title);
        this.rootDeviceVersion = (View)findViewById(R.id.root_device_version);
        this.txtDeviceVersion = (TextView)findViewById(R.id.txt_device_version);
        this.txtAvailVersion = (TextView)findViewById(R.id.txt_avail_version);
        this.txtUpgradeInstructionsTitle = (TextView)findViewById(R.id.txt_upgrade_instructions_title);
        this.txtUpgradeInstructionsText = (TextView)findViewById(R.id.txt_upgrade_instructions_text);
        this.txtInitializedTitle = (TextView)findViewById(R.id.txt_initialized_title);
        this.txtInitializedText = (TextView)findViewById(R.id.txt_initialized_text);
        this.rootButtons = (View)findViewById(R.id.root_buttons);
        this.btnCancel = (View)findViewById(R.id.btn_cancel);
        this.btnUpdate = (View)findViewById(R.id.btn_update);

        this.dialog = (DialogFragment) getSupportFragmentManager().findFragmentByTag(DIALOG_PROMPT);

        if (!features.getBootloaderMode()) {
            txtTitle.setText(R.string.firmware_upgrade_title_outdated);
            rootDeviceVersion.setVisibility(View.VISIBLE);
            txtDeviceVersion.setText(features.getMajorVersion() + "." + features.getMinorVersion() + "." + features.getPatchVersion());
            txtUpgradeInstructionsTitle.setVisibility(View.VISIBLE);
            txtUpgradeInstructionsText.setVisibility(View.VISIBLE);
            txtUpgradeInstructionsText.setMovementMethod(LinkMovementMethod.getInstance());
            rootButtons.setVisibility(View.GONE);
        }
        else {
            txtTitle.setText(R.string.firmware_upgrade_title_upgrade);
            rootDeviceVersion.setVisibility(View.GONE);
            txtUpgradeInstructionsTitle.setVisibility(View.GONE);
            txtUpgradeInstructionsText.setVisibility(View.GONE);
            rootButtons.setVisibility(View.VISIBLE);

            btnCancel.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    showDialogDisconnectTrezor(R.string.main_activity_disconnect_now);
                }
            });
            btnUpdate.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    executeTrezorTaskIfCan(TASK_FIRMWARE_ERASE, FirmwareErase.newBuilder().build());
                    dialog = PromptDialog.show(getSupportFragmentManager(), dialog, DIALOG_PROMPT, DIALOG_PROMPT, "", getString(R.string.firmware_upgrade_progress), false, false, false, null, "", "");
                    refreshProgressVisibility();
                }
            });
        }

        txtAvailVersion.setText(availVersion.major + "." + availVersion.minor + "." + availVersion.patch);

        txtInitializedTitle.setVisibility(features.getInitialized() ? View.VISIBLE : View.GONE);
        txtInitializedText.setVisibility(features.getInitialized() ? View.VISIBLE : View.GONE);
        txtInitializedText.setMovementMethod(LinkMovementMethod.getInstance());

        for (String changelogLine : availVersion.changeLog) {
            TextView txt = (TextView)getLayoutInflater().inflate(R.layout.firmware_upgrade_changelog_line, rootContent, false);
            txt.setText(CustomHtml.fromHtmlWithCustomSpans(changelogLine));
            txt.setMovementMethod(LinkMovementMethod.getInstance());
            rootContent.addView(txt);
        }

//        StringBuilder changelog = new StringBuilder();
//        int changelogTextSizeLineBreak = ViewUtils.getPixelsFromDp(this, 8);
//        for (String line : availVersion.changeLog) {
//            if (changelog.length() > 0)
//                changelog.append('\n').append(CustomHtml.getTextSizeTag("\u00A0\n", changelogTextSizeLineBreak));
//            changelog.append(line);
//        }
//
//        txtChangelog.setText(CustomHtml.fromHtmlWithCustomSpans(changelog.toString()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshProgressVisibility();
    }

    @Override
    public void onBackPressed() {
        // v bootloader modu nechceme mit vubec moznost se odsud vybackovat
        if (!features.getBootloaderMode())
            super.onBackPressed();
    }

    //
    // CUSTOM CALLBACKS
    //

    @Override
    public void onTrezorTaskCompletedSucc(String id, TrezorTaskResult res) {
        if (id.equals(TASK_FIRMWARE_ERASE)) {
            if (res.getMsgResult().msgType == MessageType.MessageType_Success) {
                InputStream stream = null;
                try {
                    stream = gct.openStreamBundledFirmware();
                    executeTrezorTaskIfCan(TASK_FIRMWARE_UPLOAD, FirmwareUpload.newBuilder().setPayload(ByteString.readFrom(stream)).build());
                }
                catch (IOException ex) {
                    BaseError.ERR_UNKNOWN_ERROR.showToast(gct);
                    dismissDialogIfAny();
                }
                finally {
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e) {
                            LogUtils.e(TAG, "Exception while closing firmware stream", e);
                        }
                    }
                }
            }
            else if (res.getMsgResult().msgType == MessageType.MessageType_ButtonRequest) {
                executeTrezorTaskIfCan(id, ButtonAck.newBuilder().build());
            }
            else if (res.getMsgResult().msgType == MessageType.MessageType_Failure) {
                showDialogDisconnectTrezor(R.string.main_activity_disconnect_now);
                showToastTrezorMsgFailure((Failure)res.getMsgResult().msg);
            }
            else
                onTrezorError(TrezorError.ERR_UNEXPECTED_RESPONSE);

            refreshProgressVisibility();
        }
        else if (id.equals(TASK_FIRMWARE_UPLOAD)) {
            if (res.getMsgResult().msgType == MessageType.MessageType_Success) {
                showDialogDisconnectTrezor(R.string.firmware_upgrade_success);
            }
            else if (res.getMsgResult().msgType == MessageType.MessageType_ButtonRequest) {
                ButtonRequest req = (ButtonRequest) res.getMsgResult().msg;
                if (req.getCode() == ButtonRequestType.ButtonRequest_FirmwareCheck) {
                    this.dialog = FragmentUtils.showDialogRemoveOldOne(getSupportFragmentManager(), this.dialog, FingerprintDialog.newInstance(availVersion.fingerPrint), DIALOG_PROMPT);
                }
                executeTrezorTaskIfCan(id, ButtonAck.newBuilder().build());
            }
            else if (res.getMsgResult().msgType == MessageType.MessageType_Failure) {
                showDialogDisconnectTrezor(R.string.firmware_upgrade_failed);
                showToastTrezorMsgFailure((Failure)res.getMsgResult().msg);
            }
            else
                onTrezorError(TrezorError.ERR_UNEXPECTED_RESPONSE);

            refreshProgressVisibility();
        }
        else
            throw new NotImplementedException();
    }


    //
    // PRIVATE
    //

    private void refreshProgressVisibility() {
        if (dialog != null || getTaskFragment().containsAnyTaskByFragmentTag(null)) {
            progressBar.setVisibility(View.VISIBLE);
            nestedScrollView.setVisibility(View.GONE);
        }
        else {
            progressBar.setVisibility(View.GONE);
            nestedScrollView.setVisibility(View.VISIBLE);
        }
    }

    private void showDialogDisconnectTrezor(int textRid) {
        this.dialog = PromptDialog.show(getSupportFragmentManager(), dialog, DIALOG_PROMPT, DIALOG_PROMPT, "", getString(textRid), false, false, false, null, "", "");
        refreshProgressVisibility();
    }

    private void dismissDialogIfAny() {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }


    //
    // INNER CLASSES
    //

    public static class FingerprintDialog extends BaseDialogFragment {
        public static FingerprintDialog newInstance(String fingerprint) {
            FingerprintDialog ret = new FingerprintDialog();
            Bundle b = new Bundle();
            b.putString("fingerprint", fingerprint);
            ret.setArguments(b);
            ret.setCancelable(false);
            return ret;
        }

        @Override
        protected Builder build(Builder b, Bundle savedInstanceState) {
            View root = getActivity().getLayoutInflater().inflate(R.layout.firmware_fingerprint_dialog, null);

            ((TextView)root.findViewById(R.id.txt_fingerprint)).setText(getArguments().getString("fingerprint"));

            b.setTitle(R.string.firmware_upgrade_fingerprint_title);
            b.setView(root);

            return b;
        }
    }
}
