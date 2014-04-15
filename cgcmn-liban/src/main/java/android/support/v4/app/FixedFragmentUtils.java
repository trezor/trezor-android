package android.support.v4.app;

import android.os.Bundle;

public abstract class FixedFragmentUtils {
    public static Bundle getFragmentSavedStateField(Fragment fragment) {
        return fragment.mSavedFragmentState;
    }
}
