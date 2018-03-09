package com.satoshilabs.trezor.app.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.circlegate.liban.base.Exceptions.NotImplementedException;
import com.satoshilabs.trezor.app.R;
import com.satoshilabs.trezor.app.activity.base.BaseActivity;
import com.satoshilabs.trezor.app.common.TrezorTasks.TrezorError;
import com.satoshilabs.trezor.app.common.TrezorTasks.TrezorTaskResult;
import com.satoshilabs.trezor.app.view.CustomActionBar;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.Cancel;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.Failure;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.MessageType;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.WordAck;

import java.util.HashSet;

public class TrezorRecoverySeedActivity extends BaseActivity {
    private static final String TASK_WORD_ACK = "TASK_WORD_ACK";

    // Views
    private ProgressBar progressBar;
    private View rootContent;
    private AutoCompleteTextView autocomplete;
    private TextView txtEnteredWords;

    // Immutable members
    private HashSet<String> setBip39;


    public static Intent createIntent(Context context) {
        return new Intent(context, TrezorRecoverySeedActivity.class);
    }


    //
    // LIFECYCLE CALLBACKS
    //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // chceme aby zustal displej zapnuty
        super.onCreate(savedInstanceState);
        CustomActionBar.setContentView(this, R.layout.trezor_recovery_seed_activity, true);

        this.progressBar = (ProgressBar)findViewById(R.id.progress_bar);
        this.rootContent = findViewById(R.id.root_content);
        this.autocomplete = (AutoCompleteTextView)findViewById(R.id.autocomplete);
        this.txtEnteredWords = (TextView)findViewById(R.id.txt_entered_words);

        final String[] bip39 = getResources().getStringArray(R.array.bip_39);
        this.setBip39 = new HashSet<>();
        for (String w : bip39) {
            setBip39.add(w);
        }

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.spinner_item_dropdown, bip39);
        autocomplete.setAdapter(adapter);

//        autocomplete.addTextChangedListener(new TextWatcher() {
//            @Override public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
//            @Override public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
//
//            @Override
//            public void afterTextChanged(Editable editable) {
//                btnConfirm.setEnabled(setBip39.contains(editable.toString()));
//            }
//        });

        autocomplete.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if ((keyEvent == null || keyEvent.getAction() == KeyEvent.ACTION_DOWN)
                        && (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL))
                {
                    confirmWordIfCan(autocomplete.getText().toString());
                    return true;
                }
                else
                    return false;
            }
        });
        autocomplete.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String word = adapter.getItem(i);
                autocomplete.setText(word);
                confirmWordIfCan(word);
            }
        });

        //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        refreshGui();
    }


    //
    // CUSTOM CALLBACKS
    //

    @Override
    public void onTrezorTaskCompletedSucc(String id, TrezorTaskResult res) {
        if (id.equals(TASK_WORD_ACK)) {
            if (res.getMsgResult().msgType == MessageType.MessageType_WordRequest) {
                String word = ((WordAck)res.getParam().getMsgParam().msg).getWord();

                if (txtEnteredWords.getText().length() == 0)
                    txtEnteredWords.setText(word);
                else
                    txtEnteredWords.setText(TextUtils.concat(txtEnteredWords.getText(), "\n", word));

                autocomplete.setText("");
                refreshGui();
            }
            else if (res.getMsgResult().msgType == MessageType.MessageType_Success) {
                finish();
                startActivity(MainActivity.createIntent(this));
                setDontDisconnectOnStop();
            }
            else if (res.getMsgResult().msgType == MessageType.MessageType_Failure) {
                showToastTrezorMsgFailure((Failure) res.getMsgResult().msg);
                finish();
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
        }
        else {
            progressBar.setVisibility(View.GONE);
            rootContent.setVisibility(View.VISIBLE);
        }
    }

    private void confirmWordIfCan(String word) {
        if (setBip39.contains(word)) {
            executeTrezorTaskIfCan(TASK_WORD_ACK, WordAck.newBuilder().setWord(word).build());
            refreshGui();
        }
    }
}
