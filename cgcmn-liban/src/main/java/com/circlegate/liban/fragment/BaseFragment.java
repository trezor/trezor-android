package com.circlegate.liban.fragment;

import java.util.ArrayList;
import java.util.List;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.circlegate.liban.base.GlobalContextLib;
import com.circlegate.liban.fragment.BaseFragmentCommon.IBaseFragmentActivity;
import com.circlegate.liban.fragment.BaseFragmentCommon.OnBackPressedListener;

public class BaseFragment extends Fragment implements OnBackPressedListener {
    private final List<Runnable> pendingTasks = new ArrayList<Runnable>();
    private boolean readyToCommitFragments = false;

    private final List<Runnable> delayedTasks = new ArrayList<Runnable>();
    private boolean delayTasks = false;

    private boolean isInitialOnResume = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.isInitialOnResume = true;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getActivity() instanceof IBaseFragmentActivity) {
            ((IBaseFragmentActivity)getActivity()).addOnBackPressedListener(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        this.readyToCommitFragments = true;

        for (Runnable r : this.pendingTasks) {
            r.run();
        }
        this.pendingTasks.clear();
    }

    @Override
    public void onPause() {
        this.readyToCommitFragments = false;
        this.isInitialOnResume = false;
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (getActivity() instanceof IBaseFragmentActivity) {
            ((IBaseFragmentActivity)getActivity()).removeOnBackPressedListener(this);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        this.readyToCommitFragments = false;
        super.onSaveInstanceState(outState);
    }


    public Resources getResourcesSafe() {
        if (getActivity() != null)
            return getActivity().getResources();
        else
            return GlobalContextLib.get().getAndroidContext().getResources();
    }


    public void addPendingTask(Runnable task) {
        this.pendingTasks.add(task);
    }

    public boolean isReadyToCommitFragments() {
        return readyToCommitFragments;
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }
}
