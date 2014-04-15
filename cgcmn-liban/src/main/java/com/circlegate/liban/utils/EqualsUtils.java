package com.circlegate.liban.utils;

import java.util.List;

public class EqualsUtils {
    public static int HASHCODE_INVALID = 654123456;
    public static int HASHCODE_INVALID_REPLACEMENT = 654123457;

    public static <T> int hashCodeCheckNull(T o) {
        return o == null ? 0 : o.hashCode();
    }

    public static <T> boolean equalsCheckNull(T lhs, T rhs) {
        return lhs == null ? rhs == null : lhs.equals(rhs);
    }

    public static int itemsHashCode(List<?> items) {
        int result = 17;
        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                result = 31 * result + (items.get(i) == null ? 0 : items.get(i).hashCode());
            }
        }
        return result;
    }

    public static int itemsHashCode(int[] items) {
        int result = 17;
        if (items != null) {
            for (int i = 0; i < items.length; i++) {
                result = 31 * result + items[i];
            }
        }
        return result;
    }


    public static boolean itemsEqual(List<?> lhs, List<?> rhs) {
        if (lhs == rhs)
            return true;
        if (lhs == null || rhs == null)
            return false;
        if (lhs.size() != rhs.size())
            return false;

        for (int i = 0; i < lhs.size(); i++) {
            if (lhs.get(i) == null) {
                if (rhs.get(i) != null)
                    return false;
            }
            else if (!lhs.get(i).equals(rhs.get(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean itemsEqual(int[] lhs, int[] rhs) {
        if (lhs == rhs)
            return true;
        if (lhs == null || rhs == null || lhs.length != rhs.length)
            return false;

        for (int i = 0; i < lhs.length; i++) {
            if (lhs[i] != rhs[i])
                return false;
        }
        return true;
    }

    public static int itemsOfItemsHashCode(List<? extends List<?>> items) {
        int result = 17;
        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                result = 31 * result + itemsHashCode(items.get(i));
            }
        }
        return result;
    }

    public static boolean itemsOfItemsEqual(List<? extends List<?>> lhs, List<? extends List<?>> rhs) {
        if (lhs == rhs)
            return true;
        if (lhs == null || rhs == null)
            return false;
        if (lhs.size() != rhs.size())
            return false;

        for (int i = 0; i < lhs.size(); i++) {
            if (!itemsEqual(lhs.get(i), rhs.get(i))) {
                return false;
            }
        }
        return true;
    }
}

