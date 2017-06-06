package com.circlegate.liban.dialog;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import com.circlegate.liban.fragment.BaseRetainFragment;


public class DialogsFragment extends BaseRetainFragment {

    protected static final String FRAGMENT_TAG = DialogsFragment.class.getName();
    protected static final String DIALOG_FRAGMENT_TAG = FRAGMENT_TAG + ".Dialog";

    protected DialogFragment dialog;

    public interface IDialogsFragmentActivity {
        DialogsFragment getDialogsFragment();
    }

    public static <T extends FragmentActivity & IDialogsFragmentActivity> DialogsFragment getInstance(T activity) {
        FragmentManager fm = activity.getSupportFragmentManager();
        DialogsFragment f = (DialogsFragment)fm.findFragmentByTag(FRAGMENT_TAG);
        if (f == null) {
            f = new DialogsFragment();
            fm.beginTransaction().add(f, FRAGMENT_TAG).commit();
        }
        return f;
    }

    //
    // Livecycle callbacks
    //

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.dialog = (DialogFragment)getFragmentManager().findFragmentByTag(DIALOG_FRAGMENT_TAG);
    }

}
