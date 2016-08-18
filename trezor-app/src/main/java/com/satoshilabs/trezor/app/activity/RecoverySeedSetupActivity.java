package com.satoshilabs.trezor.app.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;

import com.circlegate.liban.base.ApiBase.ApiCreator;
import com.circlegate.liban.base.ApiBase.ApiParcelable;
import com.circlegate.liban.base.ApiDataIO.ApiDataInput;
import com.circlegate.liban.base.ApiDataIO.ApiDataOutput;
import com.circlegate.liban.base.Exceptions.NotImplementedException;
import com.satoshilabs.trezor.app.R;
import com.satoshilabs.trezor.app.activity.base.BaseActivity;
import com.satoshilabs.trezor.app.common.GlobalContext;
import com.satoshilabs.trezor.app.common.TrezorTasks.MsgWrp;
import com.satoshilabs.trezor.app.common.TrezorTasks.TrezorError;
import com.satoshilabs.trezor.app.common.TrezorTasks.TrezorTaskParam;
import com.satoshilabs.trezor.app.common.TrezorTasks.TrezorTaskResult;
import com.satoshilabs.trezor.app.style.CustomHtml;
import com.satoshilabs.trezor.app.view.CustomActionBar;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.ButtonAck;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.ButtonRequest;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.MessageType;
import com.satoshilabs.trezor.lib.protobuf.TrezorType.ButtonRequestType;

public class RecoverySeedSetupActivity extends BaseActivity {
    private static final String TASK_BUTTON_ACK = "TASK_BUTTON_ACK";

    private static final int WORDS_COUNT = 24;

    // Views
    private TextView txtText;
    private TextView txtWordNumAbove;
    private TextView txtWordNum;
    private TextView txtWordNumBelow;

    // Immutable members
    private GlobalContext gct;

    // Saved state
    private int wordInd;


    public static Intent createIntent(Context context) {
        return new Intent(context, RecoverySeedSetupActivity.class);
    }


    //
    // LIFECYCLE CALLBACKS
    //


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // chceme aby zustal displej zapnuty
        super.onCreate(savedInstanceState);
        CustomActionBar.setContentView(this, R.layout.recovery_seed_setup_activity, false);

        this.txtText = (TextView)findViewById(R.id.txt_text);
        this.txtWordNumAbove = (TextView)findViewById(R.id.txt_word_num_above);
        this.txtWordNum = (TextView)findViewById(R.id.txt_word_num);
        this.txtWordNumBelow = (TextView)findViewById(R.id.txt_word_num_below);

        this.gct = GlobalContext.get();

        if (savedInstanceState != null)
            this.wordInd = savedInstanceState.getInt("wordInd");
        else
            this.wordInd = 0;
    }

    @Override
    protected void onStart() {
        super.onStart();
        refreshGui();
        executeButtonAckIfCan();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("wordInd", wordInd);
    }

    @Override
    public void onBackPressed() {
        // nechceme, aby se odsud dalo vybackovat
    }

    //
    // CUSTOM CALLBACKS
    //

    @Override
    public void onTrezorTaskCompletedSucc(String id, TrezorTaskResult res) {
        if (id.equals(TASK_BUTTON_ACK)) {
            ButtonAckTag tag = (ButtonAckTag)res.getParam().getTag();

            if (res.getMsgResult().msgType == MessageType.MessageType_ButtonRequest && ((ButtonRequest)res.getMsgResult().msg).getCode() == ButtonRequestType.ButtonRequest_ConfirmWord) {
                this.wordInd = tag.wordInd + 1;
                refreshGui();
                executeButtonAckIfCan();
            }
            else if (res.getMsgResult().msgType == MessageType.MessageType_Success) {
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


    //
    // PRIVATE
    //

    private void executeButtonAckIfCan() {
        executeTrezorTaskIfCan(TASK_BUTTON_ACK, new TrezorTaskParam(ButtonAck.newBuilder().build(), new ButtonAckTag(wordInd)));
    }

    private void refreshGui() {
        boolean isWritePhase = wordInd < WORDS_COUNT;

        txtText.setText(isWritePhase ? R.string.recovery_seed_setup_text_write : R.string.recovery_seed_setup_text_check);
        txtWordNumAbove.setText(isWritePhase ? R.string.recovery_seed_setup_word_num_above_write : R.string.recovery_seed_setup_word_num_above_check);
        txtWordNumBelow.setText(isWritePhase ? R.string.recovery_seed_setup_word_num_below_write : R.string.recovery_seed_setup_word_num_below_check);

        int wordNum = (wordInd % WORDS_COUNT) + 1;
        int mod10 = wordNum % 10;
        final String wordPostfix;

        if (wordNum > 10  && wordNum < 20)
            wordPostfix = "th";
        else if (mod10 == 1)
            wordPostfix = "st";
        else if (mod10 == 2)
            wordPostfix = "nd";
        else if (mod10 == 3)
            wordPostfix = "rd";
        else
            wordPostfix = "th";

        txtWordNum.setText(CustomHtml.fromHtmlWithCustomSpans(wordNum + CustomHtml.getRelativeTextSizeTag(wordPostfix, 0.5f)));
    }


    //
    // INNER CLASSES
    //

    private static class ButtonAckTag extends ApiParcelable {
        public final int wordInd;

        public ButtonAckTag(int wordInd) {
            this.wordInd = wordInd;
        }

        public ButtonAckTag(ApiDataInput d) {
            this.wordInd = d.readInt();
        }

        @Override
        public void save(ApiDataOutput d, int flags) {
            d.write(this.wordInd);
        }

        public static final ApiCreator<ButtonAckTag> CREATOR = new ApiCreator<ButtonAckTag>() {
            public ButtonAckTag create(ApiDataInput d) { return new ButtonAckTag(d); }
            public ButtonAckTag[] newArray(int size) { return new ButtonAckTag[size]; }
        };
    }
}
