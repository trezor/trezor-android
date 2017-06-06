package com.satoshilabs.trezor.app.common;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.SimpleDrawerListener;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.circlegate.liban.utils.AppUtils;
import com.circlegate.liban.utils.LogUtils;
import com.circlegate.liban.view.ScrimInsetsScrollView;
import com.circlegate.liban.view.ScrimInsetsScrollView.OnInsetsCallback;
import tinyguava.ImmutableList;
import com.satoshilabs.trezor.app.R;
import com.satoshilabs.trezor.app.activity.AboutActivity;
import com.satoshilabs.trezor.app.activity.base.BaseActivity;
import com.satoshilabs.trezor.app.db.CommonDb.InitializedTrezor;
import com.satoshilabs.trezor.app.db.CommonDb.TrezorListChangedReceiver;
import com.satoshilabs.trezor.app.style.CustomHtml;
import com.satoshilabs.trezor.app.utils.BitmapUtils;

public class NavDrawer {
    private static final String TAG = NavDrawer.class.getSimpleName();

    final DrawerLayout drawerLayout;
    final ScrimInsetsScrollView drawerContent;
    final View rootNavDrawerTop;
    final TextView txtNavDrawerAppTitle;
    final RadioGroup radioGroupTrezors;
    final View dividerTrezors;
    final View btnHelp;
    final View btnSupport;
    final View btnAbout;

    final boolean isPermanent;
    final GlobalContext gct;

    // State
    boolean hideDrawerOnStop = false;


    public static NavDrawer setContentView(BaseActivity activity, int contentLayout) {
        return setContentView(activity, activity.getLayoutInflater().inflate(contentLayout, null, false));
    }

    public static NavDrawer setContentView(BaseActivity activity, View contentView) {
        return new NavDrawer(activity, contentView);
    }


    private NavDrawer(final BaseActivity activity, View contentView) {
        this.isPermanent = activity.getResources().getBoolean(R.bool.nav_drawer_permanent);
        this.gct = GlobalContext.get();

        activity.setContentView(isPermanent ? R.layout.nav_drawer_permanent : R.layout.nav_drawer_hideable);
        this.drawerLayout = (DrawerLayout) activity.findViewById(isPermanent ? R.id.drawer_layout_permanent : R.id.drawer_layout_hideable);
        this.drawerContent = (ScrimInsetsScrollView)drawerLayout.findViewById(R.id.drawer_content);

        this.rootNavDrawerTop = (View)drawerContent.findViewById(R.id.root_nav_drawer_top);
        this.txtNavDrawerAppTitle = (TextView)drawerContent.findViewById(R.id.txt_nav_drawer_app_title);
        this.radioGroupTrezors = (RadioGroup)drawerContent.findViewById(R.id.radio_group_trezors);
        this.dividerTrezors = (View)drawerContent.findViewById(R.id.divider_trezors);
        this.btnHelp = (View)drawerContent.findViewById(R.id.btn_help);
        this.btnSupport = (View)drawerContent.findViewById(R.id.btn_support);
        this.btnAbout = (View)drawerContent.findViewById(R.id.btn_about);

        if (!isPermanent) {
            drawerLayout.addView(contentView, 0);
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, drawerContent);
        }
        else {
            ((ViewGroup) activity.findViewById(R.id.nav_drawer_screen_content)).addView(contentView);
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN, drawerContent);
            drawerLayout.setScrimColor(Color.TRANSPARENT);
            drawerLayout.setFocusableInTouchMode(false);
        }

        txtNavDrawerAppTitle.setText(CustomHtml.fromHtmlWithCustomSpans(CustomHtml.getTextSizeTag(activity.getString(R.string.app_name), activity.getResources().getDimensionPixelSize(R.dimen.text_subhead_large))
            + "\n" + AppUtils.getAppVersionName()));

        btnHelp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO Doresit help
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://doc.satoshilabs.com/trezor-user/"));
                try {
                    activity.startActivity(intent);
                    hideDrawerOnStop = true;
                }
                catch (Exception ex) {
                    LogUtils.e(TAG, "btnHelp onClick threw Exception:", ex);
                    Toast.makeText(activity, R.string.err_unknown_error, Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnSupport.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO Doresit support
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto","support@trezor.io", null));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, activity.getString(R.string.support_email_subject));
                //emailIntent.putExtra(Intent.EXTRA_TEXT, text);
                try {
                    activity.startActivity(Intent.createChooser(emailIntent, activity.getString(R.string.support_title)));
                }
                catch (Exception ex) {
                    LogUtils.e(TAG, "btnSupport onClick threw Exception:", ex);
                    Toast.makeText(activity, R.string.err_unknown_error, Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnAbout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                hideDrawerOnStop = true;
                activity.startActivity(AboutActivity.createIntent(view.getContext()));
                activity.setDontDisconnectOnStop();
            }
        });


        drawerLayout.setStatusBarBackgroundColor(activity.getResources().getColor(R.color.primary_dark));
        drawerContent.setOnInsetsCallback(new OnInsetsCallback() {
            @Override
            public void onInsetsChanged(Rect insets) {
                if (rootNavDrawerTop.getPaddingTop() != insets.top) {
                    rootNavDrawerTop.setPadding(
                            rootNavDrawerTop.getPaddingLeft(),
                            insets.top,
                            rootNavDrawerTop.getPaddingRight(),
                            rootNavDrawerTop.getPaddingBottom()
                    );
                    rootNavDrawerTop.getLayoutParams().height = activity.getResources().getDimensionPixelSize(R.dimen.nav_drawer_top_height) + insets.top;
                    rootNavDrawerTop.requestLayout();
                }
            }
        });

        activity.setNavDrawerCallbacks(new INavDrawerCallbacks() {
            @Override
            public void onActivityStart() {
                hideDrawerOnStop = false;
            }

            @Override
            public void onActivityStop() {
                if (hideDrawerOnStop && !isPermanent) {
                    drawerLayout.closeDrawers();
                }
            }
        });

//        radioGroupTrezors.setOnCheckedChangeListener(new OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
//
//            }
//        });

        //refreshTrezorsList(); resi si napojena aktivita
    }


    public boolean getIsPermanent() {
        return isPermanent;
    }

    public void openDrawer() {
        if (!isPermanent) {
            drawerLayout.openDrawer(Gravity.LEFT);
        }
    }

    private void refreshTrezorsList() {
        ImmutableList<InitializedTrezor> trezors = gct.getCommonDb().getTrezors();
        if (radioGroupTrezors.getChildCount() > trezors.size()) {
            radioGroupTrezors.removeViews(trezors.size(), radioGroupTrezors.getChildCount() - trezors.size());
        }

        LayoutInflater inflater = LayoutInflater.from(radioGroupTrezors.getContext());
        for (int i = radioGroupTrezors.getChildCount(); i < trezors.size(); i++) {
            RadioButton btn = (RadioButton)inflater.inflate(R.layout.nav_drawer_trezor_radiobutton, radioGroupTrezors, false);
            btn.setCompoundDrawables(createNavDrawerButtonIcon(R.drawable.ic_trezor), null, null, null);
            btn.setOnClickListener(onTrezorClickListener);
            radioGroupTrezors.addView(btn);
        }

        for (int i = 0; i < trezors.size(); i++) {
            InitializedTrezor t = trezors.get(i);
            RadioButton btn = (RadioButton)radioGroupTrezors.getChildAt(i);

            btn.setText(TextUtils.isEmpty(t.getFeatures().getLabel()) ? btn.getContext().getString(R.string.init_trezor_device_label_default) : t.getFeatures().getLabel());
            int id = getTrezorButtonId(t.getFeatures().getDeviceId());
            btn.setId(id);
        }

        refreshCurrentSelectedTrezor(gct.getCommonDb().getLastSelectedDeviceId());

        radioGroupTrezors.setVisibility(trezors.isEmpty() ? View.GONE : View.VISIBLE);
        dividerTrezors.setVisibility(trezors.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void refreshCurrentSelectedTrezor(String currentSelectedTrezorDeviceId) {
        final int id = getTrezorButtonId(currentSelectedTrezorDeviceId);

        for (int i = 0; i < radioGroupTrezors.getChildCount(); i++) {
            RadioButton btn = (RadioButton)radioGroupTrezors.getChildAt(i);
            btn.setChecked(false);
        }
        for (int i = 0; i < radioGroupTrezors.getChildCount(); i++) {
            RadioButton btn = (RadioButton)radioGroupTrezors.getChildAt(i);
            if (btn.getId() == id)
                btn.setChecked(true);
        }
    }

    private Drawable createNavDrawerButtonIcon(int whiteIconBmpRid) {
        Resources res = radioGroupTrezors.getResources();
        return BitmapUtils.getColoredDrawable(res.getDrawable(whiteIconBmpRid),
                res.getColor(R.color.ic_color_darker_light),
                res.getColor(R.color.ic_color_highlight),
                android.R.attr.state_checked );

//        StateListDrawable ret = new StateListDrawable();
//        ret.addState(new int[] { android.R.attr.state_checked }, BitmapUtils.getColoredDrawable(this, whiteIconBmpRid, getResources().getColor(R.color.nav_drawer_checked_item_highlight)));
//        ret.addState(StateSet.WILD_CARD, BitmapUtils.getColoredDrawable(this, whiteIconBmpRid, getResources().getColor(R.color.ic_color_darker_light)));
        //return ret;
    }

    private static int getTrezorButtonId(String deviceId) {
        int ret = deviceId.hashCode();
        return ret == 0 || ret == -1 ? 1 : ret; // 0 a -1 radsi nepouzijeme
    }

    public final TrezorListChangedReceiver trezorListChangedReceiver = new TrezorListChangedReceiver() {
        @Override
        public void onTrezorListChanged(boolean listChanged, boolean lastSelectedDeviceIdChanged) {
            if (listChanged)
                refreshTrezorsList();
            else
                refreshCurrentSelectedTrezor(gct.getCommonDb().getLastSelectedDeviceId());
        }
    };

    private final OnClickListener onTrezorClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            int checkedId = view.getId();

            for (InitializedTrezor t : gct.getCommonDb().getTrezors()) {
                //noinspection ResourceType
                if (getTrezorButtonId(t.getFeatures().getDeviceId()) == checkedId) {
                    gct.getCommonDb().setLastSelectedDeviceId(t.getFeatures().getDeviceId());

                    if (!isPermanent && drawerLayout.isDrawerOpen(Gravity.LEFT)) {
                        radioGroupTrezors.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (!isPermanent)
                                    drawerLayout.closeDrawer(Gravity.LEFT);
                            }
                        }, 300);
                    }
                    break;
                }
            }
        }
    };

    public interface INavDrawerCallbacks {
        void onActivityStart();
        void onActivityStop();
    }
}
