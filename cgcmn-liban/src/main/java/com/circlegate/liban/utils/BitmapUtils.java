package com.circlegate.liban.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.util.Base64;

import com.circlegate.liban.base.CommonClasses.CmnIcon;
import com.circlegate.liban.base.CommonClasses.LargeHash;
import com.circlegate.liban.base.CustomCollections.CacheWeakRef;
import com.circlegate.liban.base.CustomCollections.ICache;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;

public class BitmapUtils {
    private static final ICache<BitmapKey, CmnIcon> cache = new CacheWeakRef<>();
    private static final BitmapKey tempKey = new BitmapKey(null, 0);

    public static Bitmap decodeBitmapFromBase64(String base64) {
        byte[] b = Base64.decode(base64, Base64.DEFAULT);
        Bitmap ret = BitmapFactory.decodeByteArray(b, 0, b.length);
        return ret;
    }

    public static LargeHash createBitmapHash(Bitmap bitmap) {
        try {
            MessageDigest md5Digest = MessageDigest.getInstance("MD5");
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            if (bitmap.compress(CompressFormat.PNG, 100, stream) == false)
                throw new RuntimeException();
            md5Digest.update(stream.toByteArray());
            stream.close();

            byte[] md5 = md5Digest.digest();
            return new LargeHash(md5);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Drawable getDrawableWithIntrisicBounds(Context context, int rid) {
        Drawable ret = context.getResources().getDrawable(rid).mutate();
        ret.setBounds(0, 0, ret.getIntrinsicWidth(), ret.getIntrinsicHeight());
        return ret;
    }


    /**
     * Pokud je true, tak se ikona perzistuje do souboru (pouziti u online dat) - ZATIM NEIMPLEMENTOVANO!
     */
    public static void addIcon(CmnIcon icon, boolean persist) {
        if (persist)
            throw new RuntimeException("Not implemented");

        synchronized (cache) {
            cache.put(new BitmapKey(icon.getIconId(), icon.getBitmap().getDensity()), icon);
        }
    }

    public static CmnIcon getIcon(LargeHash iconId, int targetDensity) {
        synchronized (cache) {
            tempKey.hash = iconId;
            tempKey.targetDensity = targetDensity;
            return cache.get(tempKey);
        }
    }


    // POZOR - je mutable, je nutne s tim pocitat!!!
    private static class BitmapKey {
        public LargeHash hash;
        public int targetDensity;

        public BitmapKey(LargeHash hash, int targetDensity) {
            this.hash = hash;
            this.targetDensity = targetDensity;
        }

        @Override
        public int hashCode() {
            int _hash = 17;
            _hash = _hash * 29 + EqualsUtils.hashCodeCheckNull(hash);
            _hash = _hash * 29 + targetDensity;
            return _hash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof BitmapKey)) {
                return false;
            }

            BitmapKey lhs = (BitmapKey) o;
            return lhs != null &&
                    EqualsUtils.equalsCheckNull(hash, lhs.hash) &&
                    targetDensity == lhs.targetDensity;
        }
    }
}