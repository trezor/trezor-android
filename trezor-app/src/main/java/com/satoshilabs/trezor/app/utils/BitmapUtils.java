package com.satoshilabs.trezor.app.utils;

import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.util.StateSet;

public class BitmapUtils extends com.circlegate.liban.utils.BitmapUtils {


    public static StateListDrawable getColoredDrawable(Drawable d, int colorNormal, int colorSpec, int specState) {
        StateColoredDrawable ret = new StateColoredDrawable(d, colorNormal, colorSpec, specState, true);

        int width = d.getIntrinsicWidth();
        int height = d.getIntrinsicHeight();
        ret.setBounds(0, 0, width > 0 ? width : 0, height > 0 ? height : 0);

        return ret;
    }

    private static class StateColoredDrawable extends StateListDrawable {
        private final int colorNormal;
        private final int colorSpec;
        private final int specState;

        private boolean isPressed = false;

        public StateColoredDrawable(Drawable drawable, int colorNormal, int colorSpec, int specState, boolean mutateDrawable) {
            if (mutateDrawable)
                drawable.mutate();

            this.colorNormal = colorNormal;
            this.colorSpec = colorSpec;
            this.specState = specState;

            addState(new int[] { specState }, drawable);
            addState(StateSet.WILD_CARD, drawable);

            setColorFilter(colorNormal, Mode.MULTIPLY);
        }

        @Override
        protected boolean onStateChange(int[] states) {
            boolean isSpecState = false;
            for (int state : states) {
                if (state == specState) {
                    isSpecState = true;
                }
            }

            if (this.isPressed != isSpecState) {
                this.isPressed = isSpecState;
                setColorFilter(isSpecState ? colorSpec : colorNormal, Mode.MULTIPLY);
            }

            return super.onStateChange(states);
        }
    }
}
