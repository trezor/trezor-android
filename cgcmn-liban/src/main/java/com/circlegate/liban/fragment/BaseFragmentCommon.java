package com.circlegate.liban.fragment;

public class BaseFragmentCommon {
    public interface OnBackPressedListener {
        boolean onBackPressed();
    }

    public interface IBaseFragmentActivity {
        void addOnBackPressedListener(OnBackPressedListener l);
        void removeOnBackPressedListener(OnBackPressedListener l);
    }
}
