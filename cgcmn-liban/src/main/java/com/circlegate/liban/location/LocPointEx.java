package com.circlegate.liban.location;

import android.location.Location;

import com.circlegate.liban.base.ApiBase.ApiCreator;
import com.circlegate.liban.base.ApiBase.ApiParcelable;
import com.circlegate.liban.base.ApiDataIO.ApiDataInput;
import com.circlegate.liban.base.ApiDataIO.ApiDataOutput;
import com.circlegate.liban.utils.EqualsUtils;

import org.joda.time.DateTime;

public class LocPointEx extends ApiParcelable {
    public static final long INVALID_AGE = 1000000000l;
    public static final float INVALID_ACCURACY = 1000000;

    public static final LocPointEx INVALID = new LocPointEx(LocPoint.INVALID, 0, INVALID_ACCURACY, "");



    private final LocPoint locPoint; // default LocPoint.INVALID
    private final long time; // default 0
    private final float accuracy; // default INVALID_ACCURACY - nemelo by se ale stavat
    private final String provider; // default ""

    public static LocPointEx create(Location optLocation) {
        if (optLocation == null)
            return INVALID;
        else
            return new LocPointEx(
                    new LocPoint(optLocation),
                    optLocation.getTime(),
                    optLocation.hasAccuracy() ? optLocation.getAccuracy() : INVALID_ACCURACY,
                    optLocation.getProvider()
            );
    }

    public LocPointEx(LocPoint locPoint, long time, float accuracy, String provider) {
        this.locPoint = locPoint;
        this.time = time;
        this.accuracy = accuracy;
        this.provider = provider;
    }

    public LocPointEx(ApiDataInput d) {
        this.locPoint = d.readObject(LocPoint.CREATOR);
        this.time = d.readLong();
        this.accuracy = d.readFloat();
        this.provider = d.readString();
    }

    @Override
    public void save(ApiDataOutput d, int flags) {
        d.write(this.locPoint, flags);
        d.write(this.time);
        d.write(this.accuracy);
        d.write(this.provider);
    }

    public LocPoint getLocPoint() {
        return this.locPoint;
    }

    public long getTime() {
        return this.time;
    }

    public float getAccuracy() {
        return this.accuracy;
    }

    public String getProvider() {
        return this.provider;
    }


    @Override
    public int hashCode() {
        int _hash = 17;
        _hash = _hash * 29 + EqualsUtils.hashCodeCheckNull(locPoint);
        _hash = _hash * 29 + (int) (time ^ (time >>> 32));
        _hash = _hash * 29 + Float.floatToIntBits(accuracy);
        _hash = _hash * 29 + EqualsUtils.hashCodeCheckNull(provider);
        return _hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof LocPointEx)) {
            return false;
        }

        LocPointEx lhs = (LocPointEx) o;
        return lhs != null &&
                EqualsUtils.equalsCheckNull(locPoint, lhs.locPoint) &&
                time == lhs.time &&
                accuracy == lhs.accuracy &&
                EqualsUtils.equalsCheckNull(provider, lhs.provider);
    }


    public boolean isValid() {
        return isValid(this);
    }

    public static boolean isValid(LocPointEx locPointEx) {
        return locPointEx.getLocPoint().isValid();
    }

    public String toString() {
        if (isValid())
            return locPoint.toString() + ", time: " + (new DateTime(time).toString("dd/MM/yyyy HH:mm:ss:SSS") + ", accurancy: " + accuracy + ", provider: " + provider);
        else
            return "LocPointEx.INVALID";
    }

    public static final ApiCreator<LocPointEx> CREATOR = new ApiCreator<LocPointEx>() {
        public LocPointEx create(ApiDataInput d) { return new LocPointEx(d); }
        public LocPointEx[] newArray(int size) { return new LocPointEx[size]; }
    };
}
