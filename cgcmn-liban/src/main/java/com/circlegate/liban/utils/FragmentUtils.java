package com.circlegate.liban.utils;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;

public class FragmentUtils {
    private static final String BUNDLE_SINGLE_ARG_PARCELABLE = FragmentUtils.class.getName() + ".SINGLE_ARG_PACELABLE";

    private static final String NESTED_FRAGMENT_TAG_DELIMITER = "|||";
    private static final String NESTED_FRAGMENT_TAG_DELIMITER_ESCAPED = "\\|\\|\\|";

    public static String getTagNotNull(Fragment f) {
        String ret = f.getTag();
        if (ret == null)
            throw new IllegalArgumentException("Fragment must have its tag assigned!");
        return ret;
    }

    public static String getNestedTagNotNull(Fragment f) {
        String ret = getTagNotNull(f);
        Fragment parent = f.getParentFragment();
        if (parent != null)
            return getNestedTagNotNull(parent) + NESTED_FRAGMENT_TAG_DELIMITER + ret;
        else
            return ret;
    }

    public static Fragment findFragmentByNestedTag(FragmentActivity activity, String nestedFragmentTag) {
        String[] splitted = nestedFragmentTag.split(NESTED_FRAGMENT_TAG_DELIMITER_ESCAPED);
        FragmentManager fm = activity.getSupportFragmentManager();

        for (int i = 0; i < splitted.length; i++) {
            Fragment f = fm.findFragmentByTag(splitted[i]);
            if (f == null || i + 1 == splitted.length)
                return f;
            else
                fm = f.getChildFragmentManager();
        }
        throw new RuntimeException("Not implemented");
    }


    public static <T extends Fragment> T setArgumentParcelable(T fragment, Parcelable p) {
        Bundle b = new Bundle();
        b.putParcelable(BUNDLE_SINGLE_ARG_PARCELABLE, p);
        return setArguments(fragment, b);
    }

    public static <T extends Fragment> T setArguments(T fragment, Bundle b) {
        fragment.setArguments(b);
        return fragment;
    }

    public static <T extends Parcelable> T getArgumentParcelable(Fragment fragment) {
        return fragment.getArguments().getParcelable(BUNDLE_SINGLE_ARG_PARCELABLE);
    }

//    /**
//     * Nasty hack :) V soucasne dobe neni zadny o moc lepsi zpusob, jak z viewPageru dostat aktualni fragment
//     */
//    public static Fragment getViewPagerCurrentFragment(FragmentManager fm, ViewPager viewPager) {
//        return getViewPagerFragmentAt(fm, viewPager, viewPager.getCurrentItem());
//    }
//
//    public static Fragment getViewPagerFragmentAt(FragmentManager fm, ViewPager viewPager, int index) {
//        String tag = "android:switcher:" + viewPager.getId() + ":" + index;
//        Fragment f = fm.findFragmentByTag(tag);
//        //Log.d("getViewPagerCurrentFragment", "" + viewPager.getCurrentItem() + ": " + (f == null ? "null" : (f.getTag() == null ? "tag: null" : f.getTag())));
//        return f;
//
////        int index = viewPager.getCurrentItem();
////        if (index >= 0)
////            return (Fragment)viewPager.getAdapter().instantiateItem(viewPager, viewPager.getCurrentItem());
////        else
////            return null;
//    }

    public static <T extends DialogFragment> T showDialogRemoveOldOne(FragmentManager fm, DialogFragment oldDialog, T newDialog, String fragmentTag) {
        FragmentTransaction ft = fm.beginTransaction();

        if (oldDialog == null) {
            oldDialog = (DialogFragment)fm.findFragmentByTag(fragmentTag);
        }

        if (oldDialog != null) {
            ft.remove(oldDialog);
        }

        newDialog.show(ft, fragmentTag);
        return newDialog;
    }

    public static void removeDialog(FragmentManager fm, DialogFragment dialog, String fragmentTag) {
        if (dialog == null) {
            dialog = (DialogFragment)fm.findFragmentByTag(fragmentTag);
        }

        if (dialog != null) {
            fm.beginTransaction().remove(dialog).commit();
        }
    }
}