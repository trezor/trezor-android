package com.circlegate.liban.utils;

import java.text.Normalizer;

public class StringUtils {
    public static String removeAccents(String s) {
        s = Normalizer.normalize(s, Normalizer.Form.NFD);
        s = s.replaceAll("[^\\p{ASCII}]", "");
        return s;
    }

    /**
     * Compares two character sequences with API like {@link Comparable#compareTo}.
     *
     * @param me The CharSequence that receives the compareTo call.
     * @param another The other CharSequence.
     * @return See {@link Comparable#compareTo}.
     */
    public static int compareToIgnoreCase(CharSequence me, CharSequence another) {
        // Code adapted from String#compareTo
        int myLen = me.length(), anotherLen = another.length();
        int myPos = 0, anotherPos = 0, result;
        int end = (myLen < anotherLen) ? myLen : anotherLen;

        while (myPos < end) {
            if ((result = Character.toLowerCase(me.charAt(myPos++))
                    - Character.toLowerCase(another.charAt(anotherPos++))) != 0) {
                return result;
            }
        }
        return myLen - anotherLen;
    }
}
