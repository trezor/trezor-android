package com.circlegate.liban.base;

import java.util.Map.Entry;

import android.content.Context;
import android.graphics.Bitmap;

import com.circlegate.liban.base.ApiBase.ApiCreator;
import com.circlegate.liban.base.ApiBase.ApiParcelable;
import com.circlegate.liban.base.ApiDataIO.ApiDataInput;
import com.circlegate.liban.base.ApiDataIO.ApiDataOutput;
import com.circlegate.liban.utils.BitmapUtils;
import com.circlegate.liban.utils.EqualsUtils;

public class CommonClasses {


    public interface IGlobalContext {
        Context getAndroidContext();
        boolean getAppIsInProductionMode();
    }

    public static class Couple<TFirst, TSecond> {
        private final TFirst first;
        private final TSecond second;

        public Couple(TFirst first, TSecond second) {
            this.first = first;
            this.second = second;
        }

        public TFirst getFirst() {
            return this.first;
        }

        public TSecond getSecond() {
            return this.second;
        }
    }

    public static class EntryImpl<TKey, TValue> implements Entry<TKey, TValue> {
        private final TKey key;
        private TValue value;

        public EntryImpl(TKey key, TValue value) {
            this.key = key;
            this.value = value;
        }

        public TKey getKey() {
            return this.key;
        }

        public TValue getValue() {
            return this.value;
        }

        @Override
        public TValue setValue(TValue value) {
            TValue old = this.value;
            this.value = value;
            return old;
        }
    }


    public static class LargeHash extends ApiParcelable {
        private final long md5Upper;
        private final long md5Lower;

        public LargeHash(byte[] bytes) {
            if (bytes.length != 16)
                throw new RuntimeException();

            long md5Lower = 0;
            for (int i = 0; i < 8; i++) {
                md5Lower |= (long)val(bytes[i]) << (i * 8);
            }
            this.md5Lower = md5Lower;

            long md5Upper = 0;
            for (int i = 0; i < 8; i++) {
                md5Upper |= (long)val(bytes[i + 8]) << (i * 8);
            }
            this.md5Upper = md5Upper;
        }

        public LargeHash(String hexString) {
            this(decodeHash(hexString));
        }

        public LargeHash(ApiDataInput d) {
            this.md5Upper = d.readLong();
            this.md5Lower = d.readLong();
        }

        @Override
        public void save(ApiDataOutput d, int flags) {
            d.write(this.md5Upper);
            d.write(this.md5Lower);
        }

        public byte[] getBytes() {
            byte[] ret = new byte[16];

            for (int i = 0; i < 8; i++) {
                ret[i] = (byte)((md5Lower >> (i * 8)) & 0xFF);
            }
            for (int i = 0; i < 8; i++) {
                ret[i + 8] = (byte)((md5Upper >> (i * 8)) & 0xFF);
            }
            return ret;
        }

        @Override
        public int hashCode() {
            int _hash = 17;
            _hash = _hash * 29 + (int) (md5Upper ^ (md5Upper >>> 32));
            _hash = _hash * 29 + (int) (md5Lower ^ (md5Lower >>> 32));
            return _hash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof LargeHash)) {
                return false;
            }

            LargeHash lhs = (LargeHash) o;
            return lhs != null &&
                    md5Upper == lhs.md5Upper &&
                    md5Lower == lhs.md5Lower;
        }

        @Override
        public String toString() {
            byte[] bytes = getBytes();
            StringBuilder res = new StringBuilder(bytes.length * 2);
            String HexAlphabet = "0123456789ABCDEF";

            for (byte B : bytes)
            {
                res.append(HexAlphabet.charAt(val(B) >> 4));
                res.append(HexAlphabet.charAt(val(B) & 0xF));
            }
            return res.toString();
        }

        private static byte[] decodeHash(String hexString) {
            byte[] bytes = new byte[hexString.length() / 2];
            int[] hexValue = new int[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05,
                    0x06, 0x07, 0x08, 0x09, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F };

            for (int x = 0, i = 0; i < hexString.length(); i += 2, x += 1)
            {
                bytes[x] = (byte)(hexValue[Character.toUpperCase(hexString.charAt(i + 0)) - '0'] << 4 |
                        hexValue[Character.toUpperCase(hexString.charAt(i + 1)) - '0']);
            }
            return bytes;
        }

        private static int val(byte b) {
            return (int)b & 0xFF;
        }

        public static final ApiCreator<LargeHash> CREATOR = new ApiCreator<LargeHash>() {
            public LargeHash create(ApiDataInput d) { return new LargeHash(d); }
            public LargeHash[] newArray(int size) { return new LargeHash[size]; }
        };
    }

    public static class CmnIcon extends ApiParcelable {
        private final LargeHash iconId;
        private final Bitmap bitmap;

        public static CmnIcon create(LargeHash iconId, Bitmap bitmap, boolean cachePersist) {
            CmnIcon ret = BitmapUtils.getIcon(iconId, bitmap.getDensity());
            if (ret != null)
                return ret;
            else {
                ret = new CmnIcon(iconId, bitmap);
                BitmapUtils.addIcon(ret, cachePersist);
                return ret;
            }
        }

        public static CmnIcon create(ApiDataInput d) {
            LargeHash iconId = new LargeHash(d);
            Bitmap bitmap = d.readBitmap();
            return create(iconId, bitmap, false);
        }

        private CmnIcon(LargeHash iconId, Bitmap bitmap) {
            this.iconId = iconId;
            this.bitmap = bitmap;
        }

        @Override
        public void save(ApiDataOutput d, int flags) {
            this.iconId.save(d, flags);
            d.write(this.bitmap, flags);
        }

        public LargeHash getIconId() {
            return this.iconId;
        }

        public Bitmap getBitmap() {
            return this.bitmap;
        }

        @Override
        public int hashCode() {
            return iconId.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof CmnIcon)) {
                return false;
            }

            CmnIcon lhs = (CmnIcon) o;
            return lhs != null &&
                    EqualsUtils.equalsCheckNull(iconId, lhs.iconId);
        }

        public static final ApiCreator<CmnIcon> CREATOR = new ApiCreator<CmnIcon>() {
            public CmnIcon create(ApiDataInput d) { return CmnIcon.create(d); }
            public CmnIcon[] newArray(int size) { return new CmnIcon[size]; }
        };
    }




}
