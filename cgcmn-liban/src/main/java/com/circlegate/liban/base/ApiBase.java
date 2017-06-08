package com.circlegate.liban.base;

import android.os.Parcel;
import android.os.Parcelable;
import com.circlegate.liban.base.ApiDataIO.ApiDataInput;
import com.circlegate.liban.base.ApiDataIO.ApiDataOutput;
import com.circlegate.liban.base.ApiDataIO.ApiDataOutputStreamWrp;
import com.circlegate.liban.base.ApiDataIO.ApiParcelInputWrp;
import com.circlegate.liban.base.ApiDataIO.ApiParcelOutputWrp;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class ApiBase {
    public interface IApiObject {
        void save(ApiDataOutput d, int flags);
    }

    public interface IApiParcelable extends IApiObject, Parcelable {
        // tridy implementujici toto rozhrani musi mit clen public static ApiCreator CREATOR;
    }


    public static abstract class ApiObject implements IApiObject {
        public final void save(ApiDataOutput d) {
            save(d, 0);
        }

        @Override
        public abstract void save(ApiDataOutput d, int flags);

        public static byte[] saveToByteArray(IApiParcelable obj) {
            return saveToByteArray(obj, false);
        }

        public static byte[] saveToByteArray(IApiParcelable obj, boolean withName) {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ApiDataOutputStreamWrp stream = new ApiDataOutputStreamWrp(new DataOutputStream(byteStream));
            if (withName)
                stream.writeWithName(obj, 0);
            else
                stream.write(obj, 0);
            return byteStream.toByteArray();
        }

    }

    public static abstract class ApiParcelable extends ApiObject implements IApiParcelable {
        public int describeContents() {
            return baseDescribeContents();
        }

        public final void writeToParcel(Parcel dest, int flags) {
            baseWriteToParcel(this, dest, flags);
        }


        public static int baseDescribeContents() {
            return 0;
        }

        public static void baseWriteToParcel(IApiParcelable parcelable, Parcel dest, int flags) {
            ApiParcelOutputWrp p = new ApiParcelOutputWrp(dest);
            parcelable.save(p, flags);
        }
    }

    public static abstract class ApiCreator<T> implements Parcelable.Creator<T> {
        @Override
        public final T createFromParcel(Parcel source) {
            ApiParcelInputWrp wrp = new ApiParcelInputWrp(source);
            return create(wrp);
        }

        public abstract T create(ApiDataInput d);
    }

    public static class ApiInstanceCreator {
        @SuppressWarnings("rawtypes")
        private static final Map<String, ApiCreator> cache = new HashMap<String, ApiCreator>();
        private static final Map<String, String> classNamesReplacements = new HashMap<>();

        // Kvuli podpore prejmenovani trid resp. jejich presouvani mezi namespacy (napr. kvuli moznosti nacist starsi datovy soubor s parcelables, kdyz nove je trida definovana jinde...)
        public static void addClassNameReplacement(String oldName, String newName) {
            synchronized (cache) {
                classNamesReplacements.put(oldName, newName);
            }
        }

        public static <T extends IApiParcelable> T createInstanceReadClassNameFirst(ApiDataInput d) {
            String className = d.readString();
            return createInstance(className, d);
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        public static <T extends IApiParcelable> T createInstance(String className, ApiDataInput d) {
            synchronized (cache) {
                String newName = classNamesReplacements.get(className);
                if (newName != null)
                    className = newName;

                ApiCreator<T> creator = cache.get(className);
                if (creator == null) {
                    try {
                        Class c = Class.forName(className);
                        Field f = c.getField("CREATOR");
                        creator = (ApiCreator<T>)f.get(null);
                    }
                    catch (ClassNotFoundException e) {
                        throw new RuntimeException("Class not found for: " + className);
                    }
                    catch (NoSuchFieldException e) {
                        throw new RuntimeException("CREATOR field not found for: " + className);
                    }
                    catch (IllegalAccessException e) {
                        throw new RuntimeException("IllegalAccessException when unmarshalling: " + className);
                    }

                    if (creator == null) {
                        throw new RuntimeException("CREATOR field not found for (1): " + className);
                    }
                    cache.put(className, creator);
                }
                return creator.create(d);
            }
        }
    }
}

