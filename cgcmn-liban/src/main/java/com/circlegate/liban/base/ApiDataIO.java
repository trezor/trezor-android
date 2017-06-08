package com.circlegate.liban.base;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Parcel;
import com.circlegate.liban.base.ApiBase.ApiCreator;
import com.circlegate.liban.base.ApiBase.ApiInstanceCreator;
import com.circlegate.liban.base.ApiBase.IApiObject;
import com.circlegate.liban.base.ApiBase.IApiParcelable;
import com.circlegate.liban.utils.AppUtils;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import tinyguava.ImmutableList;

public class ApiDataIO {
    public interface ApiDataInputOutputBase {

        int FLAG_NONE = 0;
        int FLAG_PORTABLE = 1;

    }

    public interface ApiDataOutput extends ApiDataInputOutputBase {
        boolean write(boolean value);
        void write(int value);
        void write(long value);

        void write(byte[] value);
        void write(String value);
        void write(Bitmap value, int flags);
        void write(IApiObject value, int flags);
        void writeWithName(IApiParcelable value, int flags);
        void write(Collection<? extends IApiObject> value, int flags);
        void writeOpt(IApiObject value, int flags);
        void writeOptWithName(IApiParcelable value, int flags);
    }

    public interface ApiDataInput extends ApiDataInputOutputBase {
        int getDataAppVersionCode(); // version code verze aplikace, ktera puvodne dana data ulozila...

        boolean readBoolean();
        int readInt();
        long readLong();

        byte[] readBytes();
        String readString();
        Bitmap readBitmap();
        <T extends IApiObject> T readObject(ApiCreator<T> creator);
        <T extends IApiParcelable> T readParcelableWithName();
        <T extends IApiObject> ImmutableList<T> readImmutableList(ApiCreator<T> creator);
        <T extends IApiParcelable> ImmutableList<T> readImmutableListWithNames();

        <T extends IApiParcelable> T readOptParcelableWithName();
    }

    public interface ApiDataAppVersionCodeLegacyResolver {
        int resolveAppVersionCodeLegacy(int legacyDataVersion);
    }


    public static abstract class ApiDataOutputBase implements ApiDataOutput {
        private final Map<String, Integer> stringMap = new HashMap<String, Integer>();
        private final Map<Bitmap, Integer> bmpMap = new HashMap<Bitmap, Integer>();

        protected abstract void doWrite(String value);
        protected abstract void doWrite(Bitmap value, int flags);

        @Override
        public final void write(String value) {
            Integer index = stringMap.get(value);
            if (index == null) {
                write(true);
                doWrite(value);
                stringMap.put(value, stringMap.size());
            }
            else {
                write(false);
                write(index);
            }
        }

        @Override
        public final void write(Bitmap value, int flags) {
            Integer index = bmpMap.get(value);
            if (index == null) {
                write(true);
                doWrite(value, flags);
                bmpMap.put(value, bmpMap.size());
            }
            else {
                write(false);
                write(index);
            }
        }


        @Override
        public final void write(IApiObject value, int flags) {
            value.save(this, flags);
        }

        @Override
        public final void writeWithName(IApiParcelable value, int flags) {
            write(value.getClass().getName());
            write(value, flags);
        }

        @Override
        public final void write(Collection<? extends IApiObject> value, int flags) {
            write(value.size());
            for (IApiObject item : value) {
                item.save(this, flags);
            }
        }

        @Override
        public final void writeOpt(IApiObject value, int flags) {
            if (write(value != null))
                write(value, flags);
        }

        @Override
        public final void writeOptWithName(IApiParcelable value, int flags) {
            if (write(value != null))
                writeWithName(value, flags);
        }

    }

    static abstract class ApiDataInputBase implements ApiDataInput {
        private final List<String> stringList = new ArrayList<>();
        private final List<Bitmap> bmpList = new ArrayList<>();

        protected abstract String doReadString();
        protected abstract Bitmap doReadBitmap();

        @Override
        public final String readString() {
            if (readBoolean()) {
                String ret = doReadString();
                stringList.add(ret);
                return ret;
            }
            else {
                int index = readInt();
                return stringList.get(index);
            }
        }

        @Override
        public final Bitmap readBitmap() {
            if (readBoolean()) {
                Bitmap ret = doReadBitmap();
                bmpList.add(ret);
                return ret;
            }
            else {
                int index = readInt();
                return bmpList.get(index);
            }
        }


        @Override
        public final <T extends IApiObject> T readObject(ApiCreator<T> creator) {
            return creator.create(this);
        }

        @Override
        public final <T extends IApiParcelable> T readParcelableWithName() {
            return ApiInstanceCreator.createInstanceReadClassNameFirst(this);
        }

        @Override
        public final <T extends IApiObject> ImmutableList<T> readImmutableList(ApiCreator<T> creator) {
            int length = readInt();
            ImmutableList.Builder<T> b = ImmutableList.builder();
            for (int i = 0; i < length; i++) {
                b.add(creator.create(this));
            }
            return b.build();
        }

        @SuppressWarnings("unchecked")
        @Override
        public final <T extends IApiParcelable> ImmutableList<T> readImmutableListWithNames() {
            ImmutableList.Builder<T> b = ImmutableList.builder();
            int length = readInt();
            for (int i = 0; i < length; i++) {
                b.add((T) readParcelableWithName());
            }
            return b.build();
        }


        @Override
        public final <T extends IApiParcelable> T readOptParcelableWithName() {
            return readBoolean() ? this.<T>readParcelableWithName() : null;
        }
    }


    public static class ApiDataOutputStreamWrp extends ApiDataOutputBase {
        private final DataOutputStream dataOutputStream;

        public ApiDataOutputStreamWrp(DataOutputStream dataOutputStream) {
            this(dataOutputStream, FLAG_NONE);
        }

        public ApiDataOutputStreamWrp(DataOutputStream dataOutputStream, int customFlags) {
            this.dataOutputStream = dataOutputStream;

            int dataAppVersionCode = AppUtils.getAppVersionCode() + ApiDataInputStreamWrp.DATA_VERSION_OFFSET;
            write(dataAppVersionCode);
        }

        public void close() {
            try {
                dataOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected final void doWrite(String value) {
            try {
                dataOutputStream.writeUTF(value);
            } catch (IOException e) {
                throw new RuntimeException();
            }
        }

        @Override
        protected void doWrite(Bitmap value, int flags) {
            if (value.compress(CompressFormat.PNG, 100, dataOutputStream) == false)
                throw new RuntimeException();
        }


        @Override
        public final boolean write(boolean value) {
            try {
                dataOutputStream.writeBoolean(value);
                return value;
            } catch (IOException e) {
                throw new RuntimeException();
            }
        }

        @Override
        public final void write(int value) {
            try {
                dataOutputStream.writeInt(value);
            } catch (IOException e) {
                throw new RuntimeException();
            }
        }

        @Override
        public final void write(long value) {
            try {
                dataOutputStream.writeLong(value);
            } catch (IOException e) {
                throw new RuntimeException();
            }
        }

        @Override
        public final void write(byte[] value) {
            try {
                dataOutputStream.writeInt(value.length);
                dataOutputStream.write(value);
            } catch (IOException e) {
                throw new RuntimeException();
            }
        }

    }

    public static class ApiDataInputStreamWrp extends ApiDataInputBase {
        public static final int DATA_VERSION_OFFSET = 100;

        private final DataInputStream dataInputStream;
        private final int customFlags;
        private final int dataAppVersionCode;

        public ApiDataInputStreamWrp(DataInputStream dataInputStream) {
            this.dataInputStream = dataInputStream;
            this.customFlags = FLAG_NONE;

            int dataVersion = readInt();
            if (dataVersion < DATA_VERSION_OFFSET) {
                this.dataAppVersionCode = AppUtils.getAppVersionCode();
            }
            else {
                this.dataAppVersionCode = dataVersion - DATA_VERSION_OFFSET;
            }
        }

        public ApiDataInputStreamWrp(DataInputStream dataInputStream, int customFlags, ApiDataAppVersionCodeLegacyResolver dataAppVersionCodeResolver) {
            this.dataInputStream = dataInputStream;
            this.customFlags = customFlags;

            int dataVersion = readInt();
            if (dataVersion < DATA_VERSION_OFFSET)
                this.dataAppVersionCode = dataAppVersionCodeResolver.resolveAppVersionCodeLegacy(dataVersion);
            else
                this.dataAppVersionCode = dataVersion - DATA_VERSION_OFFSET;
        }

        public void close() {
            try {
                dataInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        @Override
        protected String doReadString() {
            try {
                return dataInputStream.readUTF();
            } catch (IOException e) {
                throw new RuntimeException();
            }
        }

        @Override
        protected Bitmap doReadBitmap() {
            Bitmap ret = BitmapFactory.decodeStream(dataInputStream);
            if (ret == null)
                throw new RuntimeException();
            return ret;
        }

        @Override
        public int getDataAppVersionCode() {
            return this.dataAppVersionCode;
        }

        @Override
        public final boolean readBoolean() {
            try {
                return dataInputStream.readBoolean();
            } catch (IOException e) {
                throw new RuntimeException();
            }
        }

        @Override
        public final int readInt() {
            try {
                return dataInputStream.readInt();
            } catch (IOException e) {
                throw new RuntimeException();
            }
        }

        @Override
        public final long readLong() {
            try {
                return dataInputStream.readLong();
            } catch (IOException e) {
                throw new RuntimeException();
            }
        }


        @Override
        public final byte[] readBytes() {
            try {
                byte[] ret;
                ret = new byte[dataInputStream.readInt()];
                dataInputStream.readFully(ret);
                return ret;
            } catch (IOException e) {
                throw new RuntimeException();
            }
        }
    }


    public static class ApiParcelOutputWrp extends ApiDataOutputBase {
        private final Parcel parcel;

        public ApiParcelOutputWrp(Parcel parcel) {
            this.parcel = parcel;
        }

        @Override
        protected void doWrite(String value) {
            parcel.writeString(value);
        }

        @Override
        protected void doWrite(Bitmap value, int flags) {
            value.writeToParcel(parcel, flags);
        }


        @Override
        public final boolean write(boolean value) {
            parcel.writeByte(value ? (byte)1 : (byte)0);
            return value;
        }

        @Override
        public final void write(int value) {
            parcel.writeInt(value);
        }

        @Override
        public final void write(long value) {
            parcel.writeLong(value);
        }

        @Override
        public final void write(byte[] value) {
            parcel.writeByteArray(value);
        }

    }

    static class ApiParcelInputWrp extends ApiDataInputBase {
        private final Parcel parcel;

        ApiParcelInputWrp(Parcel parcel) {
            this.parcel = parcel;
        }


        @Override
        public int getDataAppVersionCode() {
            return AppUtils.getAppVersionCode();
        }

        @Override
        protected String doReadString() {
            return parcel.readString();
        }

        @Override
        protected Bitmap doReadBitmap() {
            return Bitmap.CREATOR.createFromParcel(parcel);
        }

        @Override
        public final boolean readBoolean() {
            return parcel.readByte() != 0;
        }

        @Override
        public final int readInt() {
            return parcel.readInt();
        }

        @Override
        public final long readLong() {
            return parcel.readLong();
        }


        @Override
        public final byte[] readBytes() {
            return parcel.createByteArray();
        }

    }
}

