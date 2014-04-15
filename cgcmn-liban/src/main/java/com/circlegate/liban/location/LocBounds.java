package com.circlegate.liban.location;

import com.circlegate.liban.base.ApiBase.ApiCreator;
import com.circlegate.liban.base.ApiBase.ApiParcelable;
import com.circlegate.liban.base.ApiDataIO.ApiDataInput;
import com.circlegate.liban.base.ApiDataIO.ApiDataOutput;
import com.circlegate.liban.utils.EqualsUtils;

public class LocBounds extends ApiParcelable {
    private final LocPoint southWest;
    private final LocPoint northEast;

    //private LatLngBounds latLngBounds; // lazy loaded

    public LocBounds(LocPoint southWest, LocPoint northEast) {
        this.southWest = southWest;
        this.northEast = northEast;
    }

//    public LocBounds(LatLngBounds latLngBounds) {
//        this.southWest = new LocPoint(latLngBounds.southwest);
//        this.northEast = new LocPoint(latLngBounds.northeast);
//        this.latLngBounds = latLngBounds;
//    }

    public LocBounds(ApiDataInput d) {
        this.southWest = d.readObject(LocPoint.CREATOR);
        this.northEast = d.readObject(LocPoint.CREATOR);
    }

    @Override
    public void save(ApiDataOutput d, int flags) {
        d.write(this.southWest, flags);
        d.write(this.northEast, flags);
    }

    public LocPoint getSouthWest() {
        return this.southWest;
    }

    public LocPoint getNorthEast() {
        return this.northEast;
    }

//    public LatLngBounds getLatLngBounds() {
//        if (this.latLngBounds == null) {
//            this.latLngBounds = new LatLngBounds(southWest.getLatLng(), northEast.getLatLng());
//        }
//        return this.latLngBounds;
//    }

    @Override
    public int hashCode() {
        int _hash = 17;
        _hash = _hash * 29 + EqualsUtils.hashCodeCheckNull(southWest);
        _hash = _hash * 29 + EqualsUtils.hashCodeCheckNull(northEast);
        return _hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof LocBounds)) {
            return false;
        }

        LocBounds lhs = (LocBounds) o;
        return lhs != null &&
                EqualsUtils.equalsCheckNull(southWest, lhs.southWest) &&
                EqualsUtils.equalsCheckNull(northEast, lhs.northEast);
    }


    public boolean isValid() {
        return southWest.isValid() && northEast.isValid();
    }

    // Pozor! Nepocita se s locbounds pres poly nebo pres 180. polednik
    public boolean isLocPointWithinBounds(LocPoint locPoint) {
        return
                southWest.getLatitudeE6() < locPoint.getLatitudeE6() && locPoint.getLatitudeE6() < northEast.getLatitudeE6()
                        && southWest.getLongitudeE6() < locPoint.getLongitudeE6() && locPoint.getLongitudeE6() < northEast.getLongitudeE6();
    }

    public LocPoint createCenterLocPoint() {
        return new LocPoint((southWest.getLatitudeE6() + northEast.getLatitudeE6()) / 2, (northEast.getLongitudeE6() + northEast.getLongitudeE6()) / 2);
    }



    public static final ApiCreator<LocBounds> CREATOR = new ApiCreator<LocBounds>() {
        public LocBounds create(ApiDataInput d) { return new LocBounds(d); }
        public LocBounds[] newArray(int size) { return new LocBounds[size]; }
    };
}