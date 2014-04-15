package com.satoshilabs.trezor.app.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.circlegate.liban.utils.AppUtils;
import com.satoshilabs.trezor.app.R;
import com.satoshilabs.trezor.app.activity.base.BaseActivity;
import com.satoshilabs.trezor.app.view.CustomActionBar;

public class AboutActivity extends BaseActivity {

    public static Intent createIntent(Context context) {
        return new Intent(context, AboutActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CustomActionBar.setContentView(this, R.layout.about_activity, true);

        ((TextView)findViewById(R.id.txt_version)).setText(AppUtils.getAppVersionName());
    }
}
