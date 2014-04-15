package com.circlegate.liban.view;

import android.support.v4.view.MotionEventCompat;
import android.view.MotionEvent;

public class ScrollViewHelper {
    private static final int INVALID_POINTER = -1;

    private final ScrollViewHelperHost host;

    private boolean scrollingEnabled = true;
    private boolean refuseOverscroll = false;

    private boolean isRefusingOverscroll = false;
    private int mActivePointerId = INVALID_POINTER;
    private float mInitMotionY;

    public ScrollViewHelper(ScrollViewHelperHost host) {
        this.host = host;
    }

    //
    // SETTERS
    //

    public void setScrollingEnabled(boolean scrollingEnabled) {
        this.scrollingEnabled = scrollingEnabled;
    }

    public void setRefuseOverscroll(boolean refuseOverscroll) {
        this.refuseOverscroll = refuseOverscroll;
    }

    //
    // CALLBACKS
    //

    public boolean onAnyTouchEvent(MotionEvent ev) {
        if (!scrollingEnabled)
            return false;

        final int action = ev.getAction() & MotionEventCompat.ACTION_MASK;
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            isRefusingOverscroll = false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (refuseOverscroll && host.isScrolledToTop()) {
                    isRefusingOverscroll = true;
                    mActivePointerId = ev.getAction() & MotionEventCompat.ACTION_POINTER_INDEX_MASK;
                    mInitMotionY = MotionEventCompat.getY(ev, mActivePointerId);
                }
                else {
                    isRefusingOverscroll = false;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (isRefusingOverscroll && mActivePointerId != INVALID_POINTER) {
                    final int pointerIndex = getPointerIndex(ev, mActivePointerId);
                    if (pointerIndex >= 0) {
                        final float y = MotionEventCompat.getY(ev, pointerIndex);

                        if (y > mInitMotionY) {
                            return false;
                        }
                    }
                }
                break;
        }
        return true;
    }

    private int getPointerIndex(MotionEvent ev, int id) {
        int activePointerIndex = MotionEventCompat.findPointerIndex(ev, id);
        if (activePointerIndex == -1)
            mActivePointerId = INVALID_POINTER;
        return activePointerIndex;
    }

    public interface ScrollViewHelperHost {
        boolean isScrolledToTop();
    }
}
