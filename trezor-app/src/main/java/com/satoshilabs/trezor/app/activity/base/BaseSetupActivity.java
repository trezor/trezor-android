package com.satoshilabs.trezor.app.activity.base;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.satoshilabs.trezor.app.R;
import com.satoshilabs.trezor.app.common.GlobalContext;
import com.satoshilabs.trezor.app.view.CustomActionBar;

public abstract class BaseSetupActivity extends BaseActivity {
    // Views
    private View progressBar;

    // Immutable members
    protected GlobalContext gct;
    protected boolean isV2;


    protected static Intent setupIntent(Intent intent, boolean isV2) {
        return intent.putExtra("isV2", isV2);
    }


    //
    // LIFECYCLE CALLBACKS
    //


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CustomActionBar.setContentView(this, R.layout.base_setup_activity, !getNoUpButton());
        this.progressBar = (View)findViewById(R.id.progress_bar);

        this.gct = GlobalContext.get();
        this.isV2 = getIntent().getBooleanExtra("isV2", false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        refreshGui();
    }


    //
    // PROTECTED
    //

    protected void setRootContentLayout(int layoutRid) {
        getLayoutInflater().inflate(layoutRid, (ViewGroup)progressBar.getParent());
    }

    @Override
    protected boolean canHandleButtonReqByDefAction() {
        return true;
    }

    protected void startActivityNextStep(Intent intent) {
        startActivity(intent);
        setDontDisconnectOnStop();
        overridePendingTransition(R.anim.slide_from_right, R.anim.slide_to_left);
    }

    protected final void refreshGui() {
        boolean isProgress = !forceDisplayContent() && getTaskFragment().containsAnyTaskByFragmentTag(null);
        progressBar.setVisibility(isProgress ? View.VISIBLE : View.GONE);

        // nastavuji viditelnost u vsech views v poradi PO progressBaru
        ViewGroup root = (ViewGroup)progressBar.getParent();
        for (int i = root.indexOfChild(progressBar) + 1; i < root.getChildCount(); i++) {
            root.getChildAt(i).setVisibility(isProgress ? View.GONE : View.VISIBLE);
        }

        if (!isProgress)
            refreshVisibleContent();
    }

    protected void refreshVisibleContent() {
    }

    protected boolean forceDisplayContent() {
        return false;
    }

    protected boolean getNoUpButton() {
        return false;
    }
}
