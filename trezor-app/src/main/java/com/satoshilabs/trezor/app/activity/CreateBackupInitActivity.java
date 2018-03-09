package com.satoshilabs.trezor.app.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.satoshilabs.trezor.app.R;
import com.satoshilabs.trezor.app.activity.base.BaseSetupActivity;

public class CreateBackupInitActivity extends BaseSetupActivity {
    // Views
    private CheckBox checkbox;
    private View btnContinue;

    public static Intent createIntent(Context context, boolean isV2) {
        return setupIntent(new Intent(context, CreateBackupInitActivity.class), isV2);
    }


    //
    // LIFECYCLE CALLBACKS
    //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRootContentLayout(R.layout.create_backup_init_activity);

        this.checkbox = (CheckBox)findViewById(R.id.checkbox);
        this.btnContinue = (View)findViewById(R.id.btn_continue);

        this.checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                btnContinue.setEnabled(isChecked);
            }
        });
        this.btnContinue.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                if (isV2)
                    startActivityNextStep(CreateBackupRecoverySeedV2Activity.createIntent(v.getContext()));
                else
                    startActivityNextStep(CreateBackupRecoverySeedActivity.createIntent(v.getContext()));
            }
        });
    }
}
