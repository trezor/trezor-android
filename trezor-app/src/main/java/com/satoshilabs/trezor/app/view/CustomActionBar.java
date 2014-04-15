package com.satoshilabs.trezor.app.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.circlegate.liban.base.CommonClasses.Couple;
import com.satoshilabs.trezor.app.R;
import com.satoshilabs.trezor.app.activity.base.BaseActivity;
import com.satoshilabs.trezor.app.common.NavDrawer;

public class CustomActionBar extends Toolbar {
    public CustomActionBar(Context context) {
        this(context, null);
    }

    public CustomActionBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomActionBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    public static CustomActionBar setContentView(BaseActivity activity, int contentLayout, boolean withUpButton) {
        Couple<ViewGroup, CustomActionBar> c = inflateWithActionBar(activity, contentLayout);
        activity.setContentView(c.getFirst());

        if (withUpButton) {
            setNavigationButtonUp(activity);
        }

        return c.getSecond();
    }

    public static Couple<ViewGroup, CustomActionBar> inflateWithActionBar(BaseActivity activity, int contentLayout) {
        ViewGroup root = (ViewGroup) activity.getLayoutInflater().inflate(R.layout.activity_with_action_bar, null);
        CustomActionBar actionBar = (CustomActionBar)root.findViewById(R.id.action_bar);
        activity.setSupportActionBar(actionBar);
        activity.getLayoutInflater().inflate(contentLayout, root);
        return new Couple<>(root, actionBar);
    }

    public static void setNavigationButtonUp(BaseActivity activity) {
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        activity.getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    public void setNavigationButtonNavDrawer(final NavDrawer navDrawer) {
        setNavigationIcon(getResources().getDrawable(R.drawable.ic_navigation_drawer_white));
        setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navDrawer.openDrawer();
            }
        });
    }

//    public void initWithoutUpButton(BaseActivity activity) {
//        activity.setSupportActionBar(this);
//        activity.getSupportActionBar().setDisplayShowHomeEnabled(false);
//    }
//
//    public void initWithUpButton(BaseActivity activity) {
//        activity.setSupportActionBar(this);
//        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        activity.getSupportActionBar().setDisplayShowHomeEnabled(true);
//    }
//
//    public void initWithNavDrawerIcon(BaseActivity activity, final NavDrawer navDrawer) {
//        activity.setSupportActionBar(this);
//        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(false);
//
//        if (navDrawer.getIsPermanent()) {
//            activity.getSupportActionBar().setDisplayShowHomeEnabled(false);
//        }
//        else {
//            activity.getSupportActionBar().setDisplayShowHomeEnabled(true);
//
//            //collapsingToolbarLayout.setTitle(R.);
//            Drawable icNavDrawer = DrawableCompat.wrap(getResources().getDrawable(R.drawable.ic_menu_black_24dp)).mutate();
//            DrawableCompat.setTint(icNavDrawer, Color.WHITE);
//
//            setNavigationIcon(icNavDrawer);
//            setNavigationOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    navDrawer.openDrawer();
//                }
//            });
//        }
//    }
}
