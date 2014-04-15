package com.circlegate.liban.fragment;

import android.os.Bundle;

public abstract class BaseRetainFragment extends BaseFragment {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }
}
