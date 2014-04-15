package com.circlegate.liban.base;

import org.joda.time.DateTimeZone;
import org.joda.time.tz.Provider;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

public class FastDateTimeZoneProvider implements Provider {
    private static final String TAG = FastDateTimeZoneProvider.class.getSimpleName();
    
    public static final Set<String> AVAILABLE_IDS = new HashSet<String>();

    static {
        AVAILABLE_IDS.addAll(Arrays.asList(TimeZone.getAvailableIDs()));
    }

    public DateTimeZone getZone(String id) {
        if (id == null) {
            //LogUtils.d(TAG, "getZone, id == null, returning: UTC");
            return DateTimeZone.UTC;
        }

        // hack
        if (id.startsWith("+") || id.startsWith("-"))
            id = "GMT" + id;

        TimeZone tz = TimeZone.getTimeZone(id);

        return getJodaDateTimeZone(tz);
    }

    public Set<String> getAvailableIDs() {
        return AVAILABLE_IDS;
    }

    // neresim synchronizaci - je treba volat z hlavniho threadu!
    public static void refreshCurrentTimeZone() {
        TimeZone tz = TimeZone.getDefault();
        DateTimeZone jodaZone = getJodaDateTimeZone(tz);
        DateTimeZone.setDefault(jodaZone);
    }

    private static DateTimeZone getJodaDateTimeZone(TimeZone tz) {
        if (tz == null) {
            //LogUtils.d(TAG, "getZone, id == " + id + ", tz == null, returning: UTC");
            return DateTimeZone.UTC;
        }

        int rawOffset = tz.getRawOffset();

        if (tz.inDaylightTime(new Date())) {
            rawOffset += tz.getDSTSavings();
        }

        DateTimeZone ret = DateTimeZone.forOffsetMillis(rawOffset);
        //LogUtils.d(TAG, "getZone, id == " + id + ", returning: " + ret.toString());
        return ret;
    }
}
