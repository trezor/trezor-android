package com.satoshilabs.trezor.app.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

import com.circlegate.liban.base.ApiBase.ApiCreator;
import com.circlegate.liban.base.ApiBase.ApiParcelable;
import com.circlegate.liban.base.ApiDataIO.ApiDataInput;
import com.circlegate.liban.base.ApiDataIO.ApiDataOutput;
import com.circlegate.liban.base.Exceptions.NotImplementedException;
import com.circlegate.liban.dialog.PromptDialog;
import com.circlegate.liban.task.TaskErrors.BaseError;
import com.circlegate.liban.utils.FragmentUtils;
import com.circlegate.liban.utils.LogUtils;
import com.google.protobuf.ByteString;
import com.satoshilabs.trezor.app.R;
import com.satoshilabs.trezor.app.activity.base.BaseActivity;
import com.satoshilabs.trezor.app.common.Blake2s;
import com.satoshilabs.trezor.app.common.GlobalContext;
import com.satoshilabs.trezor.app.common.GlobalContext.FirmwareRelease;
import com.satoshilabs.trezor.app.common.GlobalContext.FirmwareVersion;
import com.satoshilabs.trezor.app.common.GlobalContext.FirmwareReleases;
import com.satoshilabs.trezor.app.common.TrezorTasks.MsgWrp;
import com.satoshilabs.trezor.app.common.TrezorTasks.TrezorError;
import com.satoshilabs.trezor.app.common.TrezorTasks.TrezorTaskParam;
import com.satoshilabs.trezor.app.common.TrezorTasks.TrezorTaskResult;
import com.satoshilabs.trezor.app.dialog.CheckConfirmedDialog;
import com.satoshilabs.trezor.app.dialog.CheckConfirmedDialog.OnCheckConfirmedDialogDone;
import com.satoshilabs.trezor.app.style.CustomHtml;
import com.satoshilabs.trezor.app.utils.CommonUtils;
import com.satoshilabs.trezor.app.view.CustomActionBar;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.ButtonAck;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.ButtonRequest;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.Failure;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.Features;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.FirmwareErase;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.FirmwareRequest;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.FirmwareUpload;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.MessageType;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.WipeDevice;
import com.satoshilabs.trezor.lib.protobuf.TrezorType.ButtonRequestType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import eu.inmite.android.lib.dialogs.BaseDialogFragment;

public class FirmwareUpgradeActivity extends BaseActivity implements OnCheckConfirmedDialogDone {
    private static final String TAG = FirmwareUpgradeActivity.class.getSimpleName();

    private static final String TASK_FIRMWARE_ERASE = "TASK_FIRMWARE_ERASE";
    private static final String TASK_FIRMWARE_UPLOAD = "TASK_FIRMWARE_UPLOAD";
    private static final String TASK_WIPE_DEVICE = "TASK_WIPE_DEVICE";

    private static final String DIALOG_PROMPT = FirmwareUpgradeActivity.class.getSimpleName() + ".DIALOG_PROMPT";

    // Views
    private View progressBar;
    private View nestedScrollView;
    private View rootWarning;
    private TextView txtWarningDesc;
    private View dividerWarning;
    private ViewGroup rootContent;
    private TextView txtTitle;
    private View rootVersions;
    private View rootDeviceVersion;
    private TextView txtDeviceVersion;
    private TextView txtAvailVersion;
    private TextView txtUpgradeCommonInfo;
    private Button btnChangelog;
    private TextView txtUpgradeInstructionsTitle;
    private TextView txtUpgradeInstructionsText;
    private RadioGroup rootButtons;
    private RadioButton rbtnIHaveRecoverySeedWithMe;
    private RadioButton rbtnEmptyTrezor;
    private Button btnUpdate;
    private View dividerFactoryReset;
    private View rootFactoryReset;
    private Button btnFactoryReset;

    // Immutable members
    private GlobalContext gct;
    private Features features;
    private FirmwareReleases releases;
    private FirmwareRelease newestRelease;
    private boolean isV2;

    // Saved state
    private DialogFragment dialog;


    public static Intent createIntent(Context context, MsgWrp featuresWrp) {
        return new Intent(context, FirmwareUpgradeActivity.class)
                .putExtra("featuresWrp", featuresWrp);
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
        this.isV2 = CommonUtils.isTrezorV2(features);
        this.releases = gct.getFirmwareReleases(isV2);
        this.newestRelease = releases.getNewest();
        final FirmwareVersion deviceFirmVersion = FirmwareVersion.create(features);
        final boolean isUpdateRequired = releases.isUpdateRequired(deviceFirmVersion);

        CustomActionBar.setContentView(this, R.layout.firmware_upgrade_activity, !features.getBootloaderMode() && !isUpdateRequired);

        this.progressBar = (View)findViewById(R.id.progress_bar);
        this.nestedScrollView = (View)findViewById(R.id.nested_scroll_view);
        this.rootWarning = (View)findViewById(R.id.root_warning);
        this.txtWarningDesc = (TextView)findViewById(R.id.txt_warning_desc);
        this.dividerWarning = (View)findViewById(R.id.divider_warning);
        this.rootContent = (ViewGroup)findViewById(R.id.root_content);
        this.txtTitle = (TextView)findViewById(R.id.txt_title);
        this.rootVersions = (View)findViewById(R.id.root_versions);
        this.rootDeviceVersion = (View)findViewById(R.id.root_device_version);
        this.txtDeviceVersion = (TextView)findViewById(R.id.txt_device_version);
        this.txtAvailVersion = (TextView)findViewById(R.id.txt_avail_version);
        this.txtUpgradeCommonInfo = (TextView)findViewById(R.id.txt_upgrade_common_info);
        this.btnChangelog = (Button)findViewById(R.id.btn_changelog);
        this.txtUpgradeInstructionsTitle = (TextView)findViewById(R.id.txt_upgrade_instructions_title);
        this.txtUpgradeInstructionsText = (TextView)findViewById(R.id.txt_upgrade_instructions_text);
        this.rootButtons = (RadioGroup)findViewById(R.id.root_buttons);
        this.rbtnIHaveRecoverySeedWithMe = (RadioButton)findViewById(R.id.rbtn_i_have_recovery_seed_with_me);
        this.rbtnEmptyTrezor = (RadioButton)findViewById(R.id.rbtn_empty_trezor);
        this.btnUpdate = (Button)findViewById(R.id.btn_update);
        this.dividerFactoryReset = (View)findViewById(R.id.divider_factory_reset);
        this.rootFactoryReset = (View)findViewById(R.id.root_factory_reset);
        this.btnFactoryReset = (Button)findViewById(R.id.btn_factory_reset);

        this.dialog = (DialogFragment) getSupportFragmentManager().findFragmentByTag(DIALOG_PROMPT);

        txtUpgradeCommonInfo.setMovementMethod(LinkMovementMethod.getInstance());

        btnChangelog.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                FirmwareVersion deviceFirmVersion = FirmwareVersion.create(features);
                StringBuilder b = new StringBuilder();

                for (FirmwareRelease r : releases.releasesDesc) {
                    if (!r.version.isNewerThan(deviceFirmVersion))
                        break;

                    if (b.length() > 0)
                        b.append("\n\n");
                    b.append(r.version.toString()).append("\n");
                    b.append(r.changelog);
                }

                // at least one changelog to show...
                if (b.length() == 0 && releases.releasesDesc.size() > 0) {
                    b.append(releases.getNewest().version.toString()).append("\n");
                    b.append(releases.getNewest().changelog);
                }


                // schvalne nepouzivam promennou dialog - nechci zobrazovat progressbar pri zobrazovani tohoto dialogu...
                getDialogsFragment().showPromptDialog(getString(R.string.firmware_upgrade_changelog), b.toString());
            }
        });

        if (!features.getBootloaderMode()) {
            setTitle(R.string.firmware_upgrade_activity_title_update);

            if (features.getInitialized()) {
                show(rootWarning, dividerWarning);
                show(txtWarningDesc, R.string.firmware_upgrade_warning_initialized_desc);
            }
            else
                hide(rootWarning, dividerWarning);

            show(txtTitle, R.string.firmware_upgrade_title_outdated);
            show(rootVersions, rootDeviceVersion);
            show(txtDeviceVersion, features.getMajorVersion() + "." + features.getMinorVersion() + "." + features.getPatchVersion());
            show(txtAvailVersion, newestRelease.toString());
            show(txtUpgradeCommonInfo, isUpdateRequired ? R.string.firmware_upgrade_common_info_update_required : R.string.firmware_upgrade_common_info_update);
            show(btnChangelog);
            show(txtUpgradeInstructionsTitle);
            show(txtUpgradeInstructionsText, isV2 ? R.string.firmware_upgrade_instructions_text_v2 : R.string.firmware_upgrade_instructions_text);
            hide(rootButtons, dividerFactoryReset, rootFactoryReset);
        }
        else {
            if (features.hasFirmwarePresent() && !features.getFirmwarePresent()) {
                setTitle(R.string.firmware_upgrade_activity_title_clean_install);
                hide(rootWarning, dividerWarning);
                show(txtTitle, R.string.firmware_upgrade_title_clean_install);
                hide(rootVersions);
                show(txtUpgradeCommonInfo, R.string.firmware_upgrade_common_info_clean_install);
                hide(btnChangelog, txtUpgradeInstructionsTitle, txtUpgradeInstructionsText);
                show(rootButtons);
                hide(rbtnIHaveRecoverySeedWithMe, rbtnEmptyTrezor);
                rbtnEmptyTrezor.setChecked(true);
                show(btnUpdate, R.string.firmware_upgrade_install_firmware);
                btnUpdate.setEnabled(true);
                hide(dividerFactoryReset, rootFactoryReset);
            }
            else {
                setTitle(R.string.firmware_upgrade_activity_title_update);

                final boolean needsUpdates = newestRelease.version.isNewerThan(deviceFirmVersion);

                if (needsUpdates) {
                    show(rootWarning, dividerWarning);
                    show(txtWarningDesc, R.string.firmware_upgrade_warning_bootloader_desc);
                    show(txtTitle, R.string.firmware_upgrade_title_outdated);
                }
                else {
                    hide(rootWarning, dividerWarning);
                    show(txtTitle, R.string.firmware_upgrade_title_up_to_date);
                }

                if (features.hasFwMajor() && features.hasFwMinor() && features.hasFwPatch() && features.getFwMajor() > 0) {
                    show(txtTitle, needsUpdates ? R.string.firmware_upgrade_title_outdated : R.string.firmware_upgrade_title_up_to_date);
                    show(rootDeviceVersion);
                    show(txtDeviceVersion, features.getFwMajor() + "." + features.getFwMinor() + "." + features.getFwPatch());
                }
                else {
                    show(txtTitle, R.string.firmware_upgrade_title_firmware_update);
                    hide(rootDeviceVersion);
                }

                show(txtAvailVersion, newestRelease.toString());
                show(txtUpgradeCommonInfo, isUpdateRequired ? R.string.firmware_upgrade_common_info_update_required : R.string.firmware_upgrade_common_info_update);
                show(btnChangelog);
                hide(txtUpgradeInstructionsTitle, txtUpgradeInstructionsText);

                if (needsUpdates) {
                    show(rootButtons, rbtnIHaveRecoverySeedWithMe, rbtnEmptyTrezor);
                    show(btnUpdate, R.string.firmware_upgrade_update_my_device);
                }
                else
                    hide(rootButtons);

                if (isV2)
                    show(rootFactoryReset);
                else
                    hide(rootFactoryReset);
            }

            btnUpdate.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!getTaskFragment().containsAnyTaskByFragmentTag(null)) {
                        InputStream stream = null;
                        byte[] firmwareBytes;
                        try {
                            stream = gct.openStreamBundledFirmware(CommonUtils.isTrezorV2(features));
                            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

                            int nRead;
                            byte[] data = new byte[16384];

                            while ((nRead = stream.read(data, 0, data.length)) != -1) {
                                buffer.write(data, 0, nRead);
                            }
                            buffer.flush();
                            firmwareBytes = buffer.toByteArray();
                        }
                        catch (IOException ex) {
                            BaseError.ERR_UNKNOWN_ERROR.showToast(gct);
                            dismissDialogIfAny();
                            return;
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

                        executeTrezorTaskIfCan(TASK_FIRMWARE_ERASE, new TrezorTaskParam(FirmwareErase.newBuilder().setLength(firmwareBytes.length).build(), new BytesWrp(firmwareBytes)));
                        dialog = PromptDialog.show(getSupportFragmentManager(), dialog, DIALOG_PROMPT, DIALOG_PROMPT, "", getString(R.string.firmware_upgrade_update_progress), false, false, false, null, "", "");
                        refreshProgressVisibility();
                    }
                }
            });

            btnFactoryReset.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!getTaskFragment().containsAnyTaskByFragmentTag(null)) {
                        dialog = CheckConfirmedDialog.show(getSupportFragmentManager(), dialog, DIALOG_PROMPT, DIALOG_PROMPT,
                                getString(R.string.firmware_upgrade_factory_reset_prompt_title),
                                CustomHtml.fromHtmlWithCustomSpans(CustomHtml.getFontColorTag(getString(R.string.firmware_upgrade_factory_reset_prompt_text), getResources().getColor(R.color.text_problem))),
                                getString(R.string.i_understand),
                                getString(R.string.firmware_upgrade_factory_reset_device));
                    }
                }
            });

            rootButtons.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    btnUpdate.setEnabled(checkedId != 0);
                }
            });
        }

        //txtAvailVersion.setText(availVersion.major + "." + availVersion.minor + "." + availVersion.patch);

//        txtInitializedTitle.setVisibility(features.getInitialized() ? View.VISIBLE : View.GONE);
//        txtInitializedText.setVisibility(features.getInitialized() ? View.VISIBLE : View.GONE);
//        txtInitializedText.setMovementMethod(LinkMovementMethod.getInstance());

//        for (String changelogLine : availVersion.changeLog) {
//            TextView txt = (TextView)getLayoutInflater().inflate(R.layout.firmware_upgrade_changelog_line, rootContent, false);
//            txt.setText(CustomHtml.fromHtmlWithCustomSpans(changelogLine));
//            txt.setMovementMethod(LinkMovementMethod.getInstance());
//            rootContent.addView(txt);
//        }

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
        // vetev, ktera se deje jak pri firmwareErase, tak firmwareUpload...
        if (res.isValidResult() && res.getMsgResult().msgType == MessageType.MessageType_FirmwareRequest) {
            BytesWrp firmwareBytesWrp = (BytesWrp)res.getParam().getTag();
            FirmwareRequest r = (FirmwareRequest)res.getMsgResult().msg;

            int offset = r.getOffset() < 0 ? 0 : r.getOffset();
            int length = r.getLength() <= 0 ? firmwareBytesWrp.bytes.length : r.getLength();

            Blake2s blake2s = new Blake2s(null, null);
            blake2s.update(firmwareBytesWrp.bytes, (long)offset, (long)length);

            executeTrezorTaskIfCan(TASK_FIRMWARE_UPLOAD, new TrezorTaskParam(FirmwareUpload.newBuilder()
                    .setPayload(ByteString.copyFrom(firmwareBytesWrp.bytes, offset, length))
                    .setHash(ByteString.copyFrom(blake2s.digest())).build(), firmwareBytesWrp));
            refreshProgressVisibility();
        }

        else if (id.equals(TASK_FIRMWARE_ERASE)) {
            if (res.getMsgResult().msgType == MessageType.MessageType_Success) {
                BytesWrp firmwareBytesWrp = (BytesWrp)res.getParam().getTag();

                Blake2s blake2s = new Blake2s(null, null);
                blake2s.update(firmwareBytesWrp.bytes);

                executeTrezorTaskIfCan(TASK_FIRMWARE_UPLOAD, FirmwareUpload.newBuilder()
                        .setPayload(ByteString.copyFrom(firmwareBytesWrp.bytes))
                        .setHash(ByteString.copyFrom(blake2s.digest())).build());
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
                showDialogDisconnectTrezor((features.hasFirmwarePresent() && !features.getFirmwarePresent()) ? R.string.firmware_upgrade_clean_install_success : R.string.firmware_upgrade_update_success);
            }
            else if (res.getMsgResult().msgType == MessageType.MessageType_ButtonRequest) {
                ButtonRequest req = (ButtonRequest) res.getMsgResult().msg;
                if (req.getCode() == ButtonRequestType.ButtonRequest_FirmwareCheck) {
                    int lineLen = newestRelease.fingerprint.length() / 4;
                    int lineStart = 0;
                    StringBuilder b = new StringBuilder();
                    for (int i = 0; i < 4; i++) {
                        if (i < 3) {
                            b.append(newestRelease.fingerprint.substring(lineStart, lineStart + lineLen)).append("\n");
                            lineStart += lineLen;
                        }
                        else
                            b.append(newestRelease.fingerprint.substring(lineStart));
                    }

                    this.dialog = FragmentUtils.showDialogRemoveOldOne(getSupportFragmentManager(), this.dialog, FingerprintDialog.newInstance(b.toString()), DIALOG_PROMPT);
                }
                executeTrezorTaskIfCan(id, ButtonAck.newBuilder().build());
            }
            else if (res.getMsgResult().msgType == MessageType.MessageType_Failure) {
                showDialogDisconnectTrezor((features.hasFirmwarePresent() && !features.getFirmwarePresent()) ? R.string.firmware_upgrade_clean_install_failed : R.string.firmware_upgrade_update_failed);
                showToastTrezorMsgFailure((Failure)res.getMsgResult().msg);
            }
            else
                onTrezorError(TrezorError.ERR_UNEXPECTED_RESPONSE);

            refreshProgressVisibility();
        }
        else if (id.equals(TASK_WIPE_DEVICE)) {
            if (res.getMsgResult().msgType == MessageType.MessageType_Success) {
                showDialogDisconnectTrezor(R.string.firmware_upgrade_factory_reset_success);
            }
            else if (res.getMsgResult().msgType == MessageType.MessageType_ButtonRequest) {
                this.dialog = PromptDialog.show(getSupportFragmentManager(), dialog, DIALOG_PROMPT, DIALOG_PROMPT, "", getString(R.string.confirm_action_on_trezor),
                        false, false, false, null, "", "");
                executeTrezorTaskIfCan(id, ButtonAck.newBuilder().build());
            }
            else if (res.getMsgResult().msgType == MessageType.MessageType_Failure) {
                showDialogDisconnectTrezor(R.string.firmware_upgrade_factory_reset_failed);
                showToastTrezorMsgFailure((Failure)res.getMsgResult().msg);
            }
            else
                onTrezorError(TrezorError.ERR_UNEXPECTED_RESPONSE);

            refreshProgressVisibility();
        }
        else
            throw new NotImplementedException();
    }

    @Override
    public void onCheckConfirmedDialogDone(String id, boolean canceled) {
        if (id.equals(DIALOG_PROMPT)) {
            dialog = null;
            if (!canceled) {
                executeTrezorTaskIfCan(TASK_WIPE_DEVICE, WipeDevice.newBuilder().build());
                dialog = PromptDialog.show(getSupportFragmentManager(), dialog, DIALOG_PROMPT, DIALOG_PROMPT, "", getString(R.string.firmware_upgrade_factory_reset_progress), false, false, false, null, "", "");
                refreshProgressVisibility();
            }
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


    private void show(View v) {
        v.setVisibility(View.VISIBLE);
    }

    private void show(View... views) {
        for (View v : views)
            show(v);
    }

    private void show(TextView v, int textRid) {
        show(v);
        v.setText(textRid);
    }

    private void show(TextView v, CharSequence text) {
        show(v);
        v.setText(text);
    }

    private void hide(View v) {
        v.setVisibility(View.GONE);
    }

    private void hide(View... views) {
        for (View v : views)
            hide(v);
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

    private static class BytesWrp extends ApiParcelable {
        public final byte[] bytes;

        public BytesWrp(byte[] bytes) {
            this.bytes = bytes;
        }

        public BytesWrp(ApiDataInput d) {
            this.bytes = d.readBytes();
        }

        @Override
        public void save(ApiDataOutput d, int flags) {
            d.write(this.bytes);
        }

        public static final ApiCreator<BytesWrp> CREATOR = new ApiCreator<BytesWrp>() {
            public BytesWrp create(ApiDataInput d) { return new BytesWrp(d); }
            public BytesWrp[] newArray(int size) { return new BytesWrp[size]; }
        };
    }
}
