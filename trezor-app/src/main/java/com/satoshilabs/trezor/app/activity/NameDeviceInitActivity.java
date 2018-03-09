package com.satoshilabs.trezor.app.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

import com.satoshilabs.trezor.app.R;
import com.satoshilabs.trezor.app.activity.base.BaseSetupActivity;

public class NameDeviceInitActivity extends BaseSetupActivity {
    // Views
    private View btnContinue;

    public static Intent createIntent(Context context, boolean isV2) {
        return setupIntent(new Intent(context, NameDeviceInitActivity.class), isV2);
    }


    //
    // LIFECYCLE CALLBACKS
    //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRootContentLayout(R.layout.name_device_init_activity);

        this.btnContinue = (View)findViewById(R.id.btn_continue);

        this.btnContinue.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityNextStep(NameDeviceActivity.createIntent(v.getContext(), isV2));
            }
        });
    }
}
