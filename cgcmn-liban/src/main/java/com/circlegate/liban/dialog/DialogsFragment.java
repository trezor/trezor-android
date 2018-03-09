package com.circlegate.liban.dialog;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.circlegate.liban.R;
import com.circlegate.liban.base.CommonClasses.IGlobalContext;
import com.circlegate.liban.fragment.BaseRetainFragment;
import com.circlegate.liban.task.TaskErrors.ITaskError;
import com.circlegate.liban.task.TaskInterfaces.ITaskContext;
import com.circlegate.liban.task.TaskInterfaces.ITaskResult;
import com.circlegate.liban.utils.FragmentUtils;
import com.circlegate.liban.utils.LogUtils;

import java.util.ArrayList;
import java.util.List;

import eu.inmite.android.lib.dialogs.BaseDialogFragment;


public class DialogsFragment extends BaseRetainFragment {
    private static final String TAG = DialogsFragment.class.getSimpleName();

    protected static final String FRAGMENT_TAG = DialogsFragment.class.getName();
    protected static final String DIALOG_FRAGMENT_TAG = FRAGMENT_TAG + ".Dialog";

    protected DialogFragment dialog;
    private String currCallerFragmentTag;
    private boolean wasShownDialogToFinish = false; // po nastaveni na true uz nepovolim zobrazeni zadneho dalsiho dialogu! - ceka se, dokud uzivatel v danem dialogu neklikne na OK, cimz dojde k ukonceni aktivity

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


    //
    // Dialogs
    //

    public boolean getWasShownDialogToFinish() {
        return this.wasShownDialogToFinish;
    }

    public void showErrorMsg(final ITaskContext context, final ITaskResult res, final boolean finish) {
        LogUtils.e(TAG, "showErrorMsg(1): " + res.getError().getMsg(context));

        if (!canShowNewDialog())
            return;

        if (!isReadyToCommitFragments()) {
            addPendingTask(new Runnable() { public void run() { showErrorMsg(context, res, finish); } });
        }
        else {
            wasShownDialogToFinish = finish;
            DialogFragment newDialog = res.getError() instanceof ITaskErrorWtDialog ?
                    ((ITaskErrorWtDialog)res.getError()).createDialog(context, finish)
                    : ErrorMsgDialog.newInstance(res.getError().getMsg(context), finish);

            this.dialog = FragmentUtils.showDialogRemoveOldOne(getFragmentManager(), this.dialog, newDialog, DIALOG_FRAGMENT_TAG);
        }
    }

    public void showErrorMsgAndExit(final CharSequence msg, final String optionalDetailLog) {
        LogUtils.e(TAG, "showErrorMsgAndExit: " + msg.toString());

        if (!canShowNewDialog())
            return;

        if (!isReadyToCommitFragments()) {
            addPendingTask(new Runnable() { public void run() { showErrorMsgAndExit(msg, optionalDetailLog); } });
        }
        else {
            wasShownDialogToFinish = true;
            DialogFragment newDialog = ErrorMsgDialog.newInstance(msg, true/*optionalDetailLog*/);
            this.dialog = FragmentUtils.showDialogRemoveOldOne(getFragmentManager(), this.dialog, newDialog, DIALOG_FRAGMENT_TAG);
        }
    }

    public void showErrorMsg(final CharSequence msg) {
        showPromptDialog(getResourcesSafe().getString(R.string.error), msg);
    }

    public void showWarningMsg(final CharSequence msg) {
        showPromptDialog(getResourcesSafe().getString(R.string.warning), msg);
    }

    public void showMsgNoTitle(final CharSequence msg) {
        showPromptDialog("", msg);
    }

    public void showPromptDialog(final CharSequence title, final CharSequence msg) {
        LogUtils.e(TAG, "showPromptDialog: " + title + ": " + msg.toString());

        if (!canShowNewDialog())
            return;

        if (!isReadyToCommitFragments()) {
            addPendingTask(new Runnable() { public void run() { showPromptDialog(title, msg); } });
        }
        else {
            this.dialog = PromptDialog.show(getFragmentManager(), this.dialog, DIALOG_FRAGMENT_TAG, null, title, msg, false);
        }
    }

// Radsi zaremuju... kazdopadne chci, aby si volajici explicitne vybral, jestli ma nebo nema byt cancelable
//    public void showProgressDialog(final CharSequence msg) {
//        showProgressDialog(msg, true);
//    }

    public void showProgressDialog(final CharSequence msg, final boolean cancelable) {
        if (!canShowNewDialog())
            return;

        if (!isReadyToCommitFragments()) {
            addPendingTask(new Runnable() { public void run() { showProgressDialog(msg, cancelable); } });
        }
        else {
            this.dialog = ProgressDialog.show(getFragmentManager(), this.dialog, DIALOG_FRAGMENT_TAG, null, msg, cancelable, false, null);
        }
    }

    public void hideProgressDialog() {
        if (!isReadyToCommitFragments()) {
            addPendingTask(new Runnable() { public void run() { hideProgressDialog(); } });
        }
        else {
            FragmentManager fm = getFragmentManager();
            if (this.dialog == null)
                this.dialog = (DialogFragment)fm.findFragmentByTag(DIALOG_FRAGMENT_TAG);
            if (this.dialog instanceof ProgressDialog) {
                fm.beginTransaction().remove(this.dialog).commit();
                this.dialog = null;
            }
        }
    }


    //
    // PRIVATE
    //

    private boolean canShowNewDialog() {
        if (wasShownDialogToFinish) {
            LogUtils.e(TAG, "Can't show new dialog after showing dialog to finish activity!");
            return false;
        }
        else
            return true;
    }


    //
    // INNER CLASSES
    //

    public interface RecognizedTextSetter {
        void onRecognizedText(String text);
    }

    public interface ITaskErrorWtDialog extends ITaskError {
        DialogFragment createDialog(ITaskContext context, boolean finish);
    }

    public static class ErrorMsgDialog extends BaseDialogFragment {
        private boolean finish;

        public static ErrorMsgDialog newInstance(CharSequence msg, boolean finish) {
            Bundle b = new Bundle();
            b.putCharSequence("msg", msg);
            b.putBoolean("finish", finish);
            ErrorMsgDialog ret = new ErrorMsgDialog();
            ret.setArguments(b);
            ret.setCancelable(true);
            return ret;
        }

        @Override
        protected Builder build(Builder b, Bundle savedInstanceState) {
            Bundle args = getArguments();
            final CharSequence errorMsg = args.getCharSequence("msg");
            this.finish = args.getBoolean("finish");

            b.setTitle(getString(R.string.error));
            b.setMessage(errorMsg);
            b.setPositiveButton(android.R.string.ok, new OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                    if (finish && getActivity() != null)
                        getActivity().finish();
                }
            });
            return b;
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            super.onCancel(dialog);
            if (finish && getActivity() != null)
                getActivity().finish();
        }
    }
}
