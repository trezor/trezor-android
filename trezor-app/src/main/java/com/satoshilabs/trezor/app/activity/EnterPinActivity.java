package com.satoshilabs.trezor.app.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.circlegate.liban.base.ApiBase.ApiCreator;
import com.circlegate.liban.base.ApiBase.ApiParcelable;
import com.circlegate.liban.base.ApiBase.IApiParcelable;
import com.circlegate.liban.base.ApiDataIO.ApiDataInput;
import com.circlegate.liban.base.ApiDataIO.ApiDataOutput;
import com.circlegate.liban.base.Exceptions.NotImplementedException;
import com.circlegate.liban.task.TaskInterfaces.ITaskResultListener;
import com.circlegate.liban.utils.ActivityUtils;
import tinyguava.ImmutableList;
import com.satoshilabs.trezor.app.R;
import com.satoshilabs.trezor.app.activity.base.BaseActivity;
import com.satoshilabs.trezor.app.common.GlobalContext;
import com.satoshilabs.trezor.app.style.CustomHtml;
import com.satoshilabs.trezor.app.view.CustomActionBar;
import com.satoshilabs.trezor.lib.protobuf.TrezorType.PinMatrixRequestType;

public class EnterPinActivity extends BaseActivity {
    private static final int PIN_MAX_LENGTH = 9;

    // Views
    private TextView txtTitle;
    private TextView txtText;
    private TextView txtPinStars;
    private View btnBackspace;
    private TextView txtPinStrength;
    private View btnCancel;
    private View btnConfirm;
    private ImmutableList<View> buttons;

    // Immutable members
    private GlobalContext gct;
    private EnterPinActivityParam activityParam;
    private final int[] tmpNumCounts = new int[9];



    public static Intent createIntent(Context context, EnterPinActivityParam activityParam) {
        return new Intent(context, EnterPinActivity.class).putExtra("activityParam", activityParam);
    }


    //
    // LIFECYCLE CALLBACKS
    //


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.enter_pin_activity);

        //final CustomActionBar actionBar = (CustomActionBar)findViewById(R.id.action_bar);
        //actionBar.initWithoutUpButton(this);

        this.txtTitle = (TextView)findViewById(R.id.txt_title);
        this.txtText = (TextView)findViewById(R.id.txt_text);
        this.txtPinStars = (TextView)findViewById(R.id.txt_pin_stars);
        this.btnBackspace = (View)findViewById(R.id.btn_backspace);
        this.txtPinStrength = (TextView)findViewById(R.id.txt_pin_strength);
        this.btnCancel = (View)findViewById(R.id.btn_cancel);
        this.btnConfirm = (View)findViewById(R.id.btn_confirm);

        this.gct = GlobalContext.get();
        this.activityParam = getIntent().getParcelableExtra("activityParam");

        ImmutableList.Builder<View> bButtons = ImmutableList.builder();
        addPinButton(bButtons, R.id.button1, 1);
        addPinButton(bButtons, R.id.button2, 2);
        addPinButton(bButtons, R.id.button3, 3);
        addPinButton(bButtons, R.id.button4, 4);
        addPinButton(bButtons, R.id.button5, 5);
        addPinButton(bButtons, R.id.button6, 6);
        addPinButton(bButtons, R.id.button7, 7);
        addPinButton(bButtons, R.id.button8, 8);
        addPinButton(bButtons, R.id.button9, 9);
        this.buttons = bButtons.build();

        btnBackspace.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                String s = txtPinStars.getText().toString();
                if (s.length() > 0) {
                    txtPinStars.setText(s.substring(0, s.length() - 1));
                }
            }
        });

        txtPinStrength.setVisibility(activityParam.pinMatrixRequestType != PinMatrixRequestType.PinMatrixRequestType_Current ? View.VISIBLE : View.GONE);

        txtPinStars.addTextChangedListener(new TextWatcher() {

            @Override public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
            @Override public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() == 0) {
                    btnConfirm.setEnabled(false);
                    txtPinStrength.setText(String.valueOf(CustomHtml.HARD_SPACE));
                }
                else {
                    btnConfirm.setEnabled(true);

                    for (int i = 0; i < tmpNumCounts.length; i++)
                        tmpNumCounts[i] = 0;

                    for (int i = 0; i < editable.length(); i++) {
                        int ind = (int) editable.charAt(i) - 49;
                        tmpNumCounts[ind]++;
                    }

                    int diffDigits = 0;
                    for (int i = 0; i < tmpNumCounts.length; i++) {
                        if (tmpNumCounts[i] > 0)
                            diffDigits++;
                    }

                    if (diffDigits < 4)
                        txtPinStrength.setText(CustomHtml.fromHtmlWithCustomSpans(CustomHtml.getFontColorTag(getString(R.string.enter_pin_strength_weak), getResources().getColor(R.color.text_problem))));
                    else if (diffDigits < 6)
                        txtPinStrength.setText(R.string.enter_pin_strength_fine);
                    else if (diffDigits < 8)
                        txtPinStrength.setText(R.string.enter_pin_strength_strong);
                    else
                        txtPinStrength.setText(R.string.enter_pin_strength_ultimate);
                }

                boolean pinBtnsEnabled = editable.length() < PIN_MAX_LENGTH;
                for (View button : buttons)
                    button.setEnabled(pinBtnsEnabled);
            }
        });

        btnCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        btnConfirm.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                String pin = txtPinStars.getText().toString();
                if (pin.length() > 0) {
                    ActivityUtils.setResultParcelable(EnterPinActivity.this, RESULT_OK, new EnterPinActivityResult(activityParam, pin));
                    finish();
                }
            }
        });

        switch (activityParam.pinMatrixRequestType) {
            case PinMatrixRequestType_Current:
                txtTitle.setText(R.string.enter_pin_current_prompt);
                txtText.setText(R.string.enter_pin_text);
                break;

            case PinMatrixRequestType_NewFirst:
                txtTitle.setText(R.string.enter_pin_new_prompt);
                txtText.setText(R.string.enter_pin_text);
                break;

            case PinMatrixRequestType_NewSecond:
                txtTitle.setText(R.string.enter_pin_repeat_prompt);
                txtText.setText(R.string.enter_pin_text_repeat);
                break;

            default: throw new NotImplementedException();
        }

        if (savedInstanceState == null) {
            txtPinStars.setText("");
        }

        ActivityUtils.setResultParcelable(this, RESULT_CANCELED, new EnterPinActivityResult(activityParam, ""));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("tmp", true);
    }


    //
    // PRIVATE
    //

    void addPinButton(ImmutableList.Builder<View> bButtons, int rid, final int number) {
        View button = findViewById(rid);
        button.setOnClickListener(new OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View view) {
                if (txtPinStars.getText().length() < PIN_MAX_LENGTH) {
                    txtPinStars.setText(txtPinStars.getText().toString() + number);
                }
            }
        });
        bButtons.add(button);
    }


    //
    // INNER CLASSES
    //

    public static class EnterPinActivityParam extends ApiParcelable {
        public final String taskId;
        public final IApiParcelable tag; // optional abstract
        public final PinMatrixRequestType pinMatrixRequestType;

        public EnterPinActivityParam(String taskId, IApiParcelable tag, PinMatrixRequestType pinMatrixRequestType) {
            this.taskId = taskId;
            this.tag = tag;
            this.pinMatrixRequestType = pinMatrixRequestType;
        }

        public EnterPinActivityParam(ApiDataInput d) {
            this.taskId = d.readString();
            this.tag = d.readOptParcelableWithName();
            this.pinMatrixRequestType = PinMatrixRequestType.valueOf(d.readString());
        }

        @Override
        public void save(ApiDataOutput d, int flags) {
            d.write(this.taskId);
            d.writeOptWithName(this.tag, flags);
            d.write(this.pinMatrixRequestType.name());
        }

        public static final ApiCreator<EnterPinActivityParam> CREATOR = new ApiCreator<EnterPinActivityParam>() {
            public EnterPinActivityParam create(ApiDataInput d) { return new EnterPinActivityParam(d); }
            public EnterPinActivityParam[] newArray(int size) { return new EnterPinActivityParam[size]; }
        };
    }

    public static class EnterPinActivityResult extends ApiParcelable {
        public final EnterPinActivityParam param;
        public final String pinEncoded;

        public EnterPinActivityResult(EnterPinActivityParam param, String pinEncoded) {
            this.param = param;
            this.pinEncoded = pinEncoded;
        }

        public EnterPinActivityResult(ApiDataInput d) {
            this.param = d.readObject(EnterPinActivityParam.CREATOR);
            this.pinEncoded = d.readString();
        }

        @Override
        public void save(ApiDataOutput d, int flags) {
            d.write(this.param, flags);
            d.write(this.pinEncoded);
        }

        public static final ApiCreator<EnterPinActivityResult> CREATOR = new ApiCreator<EnterPinActivityResult>() {
            public EnterPinActivityResult create(ApiDataInput d) { return new EnterPinActivityResult(d); }
            public EnterPinActivityResult[] newArray(int size) { return new EnterPinActivityResult[size]; }
        };
    }
}
