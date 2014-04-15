package com.circlegate.liban.utils;

import com.circlegate.liban.BuildConfig;

import java.util.List;

public class ComparableUtils {

    public static <T extends Comparable<T>> void assertSorted(List<T> list, boolean onlyIfDebug) {
        if (!onlyIfDebug || BuildConfig.DEBUG) {
            for (int i = 0; i < list.size() - 1; i++) {
                if (list.get(i).compareTo(list.get(i + 1)) > 0)
                    throw new RuntimeException("List is not sorted!");
            }
        }
    }

}
