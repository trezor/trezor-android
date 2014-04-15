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
import com.google.common.collect.ImmutableList;

import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiDataIO {
    public interface ApiDataInputOutputBase {
        int UNKNOWN_DATA_VERSION = 0;

        int FLAG_NONE = 0;
        int FLAG_PORTABLE = 1;

        int getCustomFlags();
    }

    public interface ApiDataOutput extends ApiDataInputOutputBase {
        boolean write(boolean value);
        void write(int value);
        void write(long value);
        void write(float value);
        void write(double value);

        void write(byte[] value);
        void write(int[] value);
        void write(String value);
        void write(Bitmap value, int flags);
        void write(DateTime value);
        void write(DateMidnight value);
        void write(Duration value);
        void write(IApiObject value, int flags);
        void writeWithName(IApiParcelable value, int flags);
        void writeBooleans(Collection<Boolean> value);
        void writeIntegers(Collection<Integer> value);
        void writeStrings(Collection<String> value);
        void write(Collection<? extends IApiObject> value, int flags);
        void writeWithNames(Collection<? extends IApiParcelable> value, int flags);

        void writeOpt(byte[] value);
        void writeOpt(int[] value);
        void writeOpt(String value);
        void writeOpt(Bitmap value, int flags);
        void writeOpt(DateTime value);
        void writeOpt(DateMidnight value);
        void writeOpt(Duration value);
        void writeOpt(IApiObject value, int flags);
        void writeOptWithName(IApiParcelable value, int flags);
        void writeOptBooleans(Collection<Boolean> value);
        void writeOptIntegers(Collection<Integer> value);
        void writeOptStrings(Collection<String> value);
        void writeOpt(Collection<? extends IApiObject> value, int flags);
        void writeOptWithNames(Collection<? extends IApiParcelable> value, int flags);
    }

    public interface ApiDataInput extends ApiDataInputOutputBase {
        int getDataAppVersionCode(); // version code verze aplikace, ktera puvodne dana data ulozila...

        boolean readBoolean();
        int readInt();
        long readLong();
        float readFloat();
        double readDouble();

        byte[] readBytes();
        int[] readIntArray();
        String readString();
        Bitmap readBitmap();
        DateTime readDateTime();
        DateMidnight readDateMidnight();
        Duration readDuration();
        <T extends IApiObject> T readObject(ApiCreator<T> creator);
        <T extends IApiParcelable> T readParcelableWithName();
        ImmutableList<Boolean> readBooleans();
        ImmutableList<String> readStrings();
        ImmutableList<Integer> readIntegers();
        <T extends IApiObject> ImmutableList<T> readImmutableList(ApiCreator<T> creator);
        <T extends IApiParcelable> ImmutableList<T> readImmutableListWithNames();

        byte[] readOptBytes();
        int[] readOptIntArray();
        String readOptString();
        Bitmap readOptBitmap();
        DateTime readOptDateTime();
        DateMidnight readOptDateMidnight();
        Duration readOptDuration();
        <T extends IApiObject> T readOptObject(ApiCreator<T> creator);
        <T extends IApiParcelable> T readOptParcelableWithName();
        ImmutableList<Boolean> readOptBooleans();
        ImmutableList<String> readOptStrings();
        ImmutableList<Integer> readOptIntegers();
        <T extends IApiObject> ImmutableList<T> readOptImmutableList(ApiCreator<T> creator);
        <T extends IApiParcelable> ImmutableList<T> readOptImmutableListWithNames();
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
        public final void write(DateTime value) {
            write(value.getMillis());
            write(value.getZone().getID());
        }

        @Override
        public final void write(DateMidnight dateMidnight) {
            write(dateMidnight.getMillis());
            write(dateMidnight.getZone().getID());
        }

        @Override
        public final void write(Duration value) {
            write(value.getMillis());
        }

        @Override
        public final void writeBooleans(Collection<Boolean> value) {
            write(value.size());
            for (Boolean item : value) {
                write(item);
            }
        }

        @Override
        public final void writeIntegers(Collection<Integer> value) {
            write(value.size());
            for (Integer item : value) {
                write(item);
            }
        }

        @Override
        public final void writeStrings(Collection<String> value) {
            write(value.size());
            for (String item : value) {
                write(item);
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
        public final void writeWithNames(Collection<? extends IApiParcelable> value, int flags) {
            write(value.size());
            for (IApiParcelable item : value) {
                writeWithName(item, flags);
            }
        }


        @Override
        public final void writeOpt(int[] value) {
            if (write(value != null))
                write(value);
        }

        @Override
        public final void writeOpt(byte[] value) {
            if (write(value != null))
                write(value);
        }

        @Override
        public final void writeOpt(String value) {
            if (write(value != null))
                write(value);
        }

        @Override
        public final void writeOpt(Bitmap value, int flags) {
            if (write(value != null))
                write(value, flags);
        }

        @Override
        public final void writeOpt(DateTime value) {
            if (write(value != null))
                write(value);
        }

        @Override
        public final void writeOpt(DateMidnight value) {
            if (write(value != null))
                write(value);
        }

        @Override
        public final void writeOpt(Duration value) {
            if (write(value != null))
                write(value);
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

        @Override
        public final void writeOptBooleans(Collection<Boolean> value) {
            if (write(value != null))
                writeBooleans(value);
        }

        @Override
        public final void writeOptIntegers(Collection<Integer> value) {
            if (write(value != null))
                writeIntegers(value);
        }

        @Override
        public final void writeOptStrings(Collection<String> value) {
            if (write(value != null))
                writeStrings(value);
        }

        @Override
        public final void writeOpt(Collection<? extends IApiObject> value, int flags) {
            if (write(value != null))
                write(value, flags);
        }

        @Override
        public final void writeOptWithNames(Collection<? extends IApiParcelable> value, int flags) {
            if (write(value != null))
                writeWithNames(value, flags);
        }
    }

    public static abstract class ApiDataInputBase implements ApiDataInput {
        private final List<String> stringList = new ArrayList<String>();
        private final List<Bitmap> bmpList = new ArrayList<Bitmap>();

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
        public final DateTime readDateTime() {
            long millis = readLong();
            String id = readString();
            return new DateTime(millis, DateTimeZone.forID(id));
        }

        @Override
        public final DateMidnight readDateMidnight() {
            long millis = readLong();
            String id = readString();
            return new DateMidnight(millis, DateTimeZone.forID(id));
        }

        @Override
        public final Duration readDuration() {
            return new Duration(readLong());
        }

        @Override
        public final ImmutableList<Boolean> readBooleans() {
            ImmutableList.Builder<Boolean> b = ImmutableList.builder();
            int length = readInt();
            for (int i = 0; i < length; i++) {
                b.add(readBoolean());
            }
            return b.build();
        }

        @Override
        public final ImmutableList<String> readStrings() {
            ImmutableList.Builder<String> b = ImmutableList.builder();
            int length = readInt();
            for (int i = 0; i < length; i++) {
                b.add(readString());
            }
            return b.build();
        }

        @Override
        public final ImmutableList<Integer> readIntegers() {
            ImmutableList.Builder<Integer> b = ImmutableList.builder();
            int length = readInt();
            for (int i = 0; i < length; i++) {
                b.add(readInt());
            }
            return b.build();
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
        public final byte[] readOptBytes() {
            return readBoolean() ? readBytes() : null;
        }

        @Override
        public final int[] readOptIntArray() {
            return readBoolean() ? readIntArray() : null;
        }

        @Override
        public final String readOptString() {
            return readBoolean() ? readString() : null;
        }

        @Override
        public final Bitmap readOptBitmap() {
            return readBoolean() ? readBitmap() : null;
        }

        @Override
        public final DateTime readOptDateTime() {
            return readBoolean() ? readDateTime() : null;
        }

        @Override
        public final DateMidnight readOptDateMidnight() {
            return readBoolean() ? readDateMidnight() : null;
        }

        @Override
        public final Duration readOptDuration() {
            return readBoolean() ? readDuration() : null;
        }

        @Override
        public final <T extends IApiObject> T readOptObject(ApiCreator<T> creator) {
            return readBoolean() ? readObject(creator) : null;
        }

        @Override
        public final <T extends IApiParcelable> T readOptParcelableWithName() {
            return readBoolean() ? this.<T>readParcelableWithName() : null;
        }

        @Override
        public final ImmutableList<Boolean> readOptBooleans() {
            return readBoolean() ? readBooleans() : null;
        }

        @Override
        public final ImmutableList<String> readOptStrings() {
            return readBoolean() ? readStrings() : null;
        }

        @Override
        public final ImmutableList<Integer> readOptIntegers() {
            return readBoolean() ? readIntegers() : null;
        }

        @Override
        public final <T extends IApiObject> ImmutableList<T> readOptImmutableList(ApiCreator<T> creator) {
            return readBoolean() ? this.<T>readImmutableList(creator) : null;
        }

        @Override
        public final <T extends IApiParcelable> ImmutableList<T> readOptImmutableListWithNames() {
            return readBoolean() ? this.<T>readImmutableListWithNames() : null;
        }
    }


    public static class ApiDataOutputStreamWrp extends ApiDataOutputBase {
        private final int customFlags;
        private final DataOutputStream dataOutputStream;

        public ApiDataOutputStreamWrp(DataOutputStream dataOutputStream) {
            this(dataOutputStream, FLAG_NONE);
        }

        public ApiDataOutputStreamWrp(DataOutputStream dataOutputStream, int customFlags) {
            this.customFlags = customFlags;
            this.dataOutputStream = dataOutputStream;

            int dataAppVersionCode = AppUtils.getAppVersionCode() + ApiDataInputStreamWrp.DATA_VERSION_OFFSET;
            write(dataAppVersionCode);
        }

        public DataOutputStream getDataOutputStream() {
            return dataOutputStream;
        }

        public void close() {
            try {
                dataOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public int getCustomFlags() {
            return customFlags;
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
        public final void write(float value) {
            try {
                dataOutputStream.writeFloat(value);
            } catch (IOException e) {
                throw new RuntimeException();
            }
        }

        @Override
        public final void write(double value) {
            try {
                dataOutputStream.writeDouble(value);
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

        @Override
        public void write(int[] value) {
            try {
                dataOutputStream.writeInt(value.length);
                for (int i = 0; i < value.length; i++)
                    dataOutputStream.writeInt(value[i]);
            }
            catch (IOException ex) {
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

        @Override
        public int getCustomFlags() {
            return customFlags;
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
        public final float readFloat() {
            try {
                return dataInputStream.readFloat();
            } catch (IOException e) {
                throw new RuntimeException();
            }
        }

        @Override
        public final double readDouble() {
            try {
                return dataInputStream.readDouble();
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

        @Override
        public int[] readIntArray() {
            try {
                int[] ret;
                ret = new int[dataInputStream.readInt()];
                for (int i = 0; i < ret.length; i++) {
                    ret[i] = dataInputStream.readInt();
                }
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

        public Parcel getParcel() {
            return parcel;
        }

        @Override
        public int getCustomFlags() {
            return FLAG_NONE;
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
        public void write(float value) {
            parcel.writeFloat(value);
        }

        @Override
        public final void write(double value) {
            parcel.writeDouble(value);
        }

        @Override
        public final void write(byte[] value) {
            parcel.writeByteArray(value);
        }

        @Override
        public void write(int[] value) {
            parcel.writeIntArray(value);
        }

        // POZOR! Zamerne zaremovano ! - zaremovana implementace vytvari novy Parcel a to znamena, ze se nepouziji cache stringu a bitmap
        // - a pak kvuli tomu treba se neobnovovaly nektere aktivity!!
//        @Override
//        public final void writeWithName(IApiParcelable value, int flags) {
//            parcel.writeParcelable(value, flags);
//        }
    }

    public static class ApiParcelInputWrp extends ApiDataInputBase {
        private final Parcel parcel;

        public ApiParcelInputWrp(Parcel parcel) {
            this.parcel = parcel;
        }

        public Parcel getParcel() {
            return parcel;
        }


        @Override
        public int getDataAppVersionCode() {
            return AppUtils.getAppVersionCode();
        }

        @Override
        public int getCustomFlags() {
            return FLAG_NONE;
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
        public final float readFloat() {
            return parcel.readFloat();
        }

        @Override
        public final double readDouble() {
            return parcel.readDouble();
        }

        @Override
        public final byte[] readBytes() {
            return parcel.createByteArray();
        }

        @Override
        public int[] readIntArray() {
            return parcel.createIntArray();
        }

//        @Override
//        public final <T extends IApiParcelable> T readParcelableWithName() {
//            return parcel.readParcelable(getClass().getClassLoader());
//        }
    }
}

