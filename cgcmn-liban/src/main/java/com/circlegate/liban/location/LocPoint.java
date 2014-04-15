package com.circlegate.liban.location;

import org.json.JSONException;
import org.json.JSONObject;

import android.location.Location;

import com.circlegate.liban.base.ApiBase.ApiCreator;
import com.circlegate.liban.base.ApiBase.ApiParcelable;
import com.circlegate.liban.base.ApiDataIO.ApiDataInput;
import com.circlegate.liban.base.ApiDataIO.ApiDataOutput;

public class LocPoint extends ApiParcelable implements Comparable<LocPoint> {
    // zdroj: http://www.csgnetwork.com/degreelenllavcalc.html
    // jeden stupen sirky v metrech
    public static final int ONE_DEGREE_LATITUDE_IN_METERS = 111229;
    // jeden stupen delky v metrech (pro padesatou rovnobezku)
    public static final int ONE_DEGREE_LONGTITUDE_IN_METERS = 71695;

    private static final int MAX_LAT = 90000000;
    private static final int MAX_LNG = 180000000;

    public static final LocPoint INVALID = new LocPoint(0, 0);

    private final double latitude;
    private final double longitude;
    //private final LatLng latLng;

//    public static LocPoint createFromJson(JSONObject json, String keyLat, String keyLng, boolean optional) throws JSONException {
//        if ((json.isNull(keyLat) || json.isNull(keyLng)) && !optional) {
//            throw new JSONException("Missing lat/lng");
//        }
//
//        LocPoint ret = new LocPoint(json.optDouble(keyLat, 0), json.optDouble(keyLng, 0));
//
//        if (!ret.isValid())
//            if (!optional)
//                throw new JSONException("Invalid lat/lng");
//            else
//                return null;
//        else
//            return ret;
//    }

    public LocPoint(int latitudeE6, int longitudeE6) {
        this((double)latitudeE6 / 1E6, (double)longitudeE6 / 1E6);
    }

    public LocPoint(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        //this(new LatLng(latitude, longitude));
    }

//    public LocPoint(LatLng latLng) {
//        this.latLng = latLng;
//    }

    public LocPoint(Location location) {
        this(location.getLatitude(), location.getLongitude());
    }

    public LocPoint(ApiDataInput d) {
        this.latitude = d.readDouble();
        this.longitude = d.readDouble();
        //this.latLng = new LatLng(lat, lng);
    }

    @Override
    public void save(ApiDataOutput d, int flags) {
        d.write(this.latitude);
        d.write(this.longitude);
    }

    public JSONObject appendToJSON(JSONObject json, String keyLat, String keyLng) {
        try {
            json.put(keyLat, getLatitude());
            json.put(keyLng, getLongitude());
            return json;
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

//    public LatLng getLatLng() {
//        return this.latLng;
//    }

    public int getLatitudeE6() {
        return (int)(this.latitude * 1E6);
    }

    public int getLongitudeE6() {
        return (int)(this.longitude * 1E6);
    }

    public double getLatitude() {
        return this.latitude;
    }

    public double getLongitude() {
        return this.longitude;
    }

    public String getLatitudeString() {
        return getFormattedLatLng(getLatitudeE6());
    }

    public String getLongitudeString() {
        return getFormattedLatLng(getLongitudeE6());
    }

    public String getLatitudeStringDMS() {
        return (latitude >= 0 ? "N" : "S") + decimalToDMS(Math.abs(latitude));
    }

    public String getLongitudeStringDMS() {
        return (longitude >= 0 ? "E" : "W") + decimalToDMS(Math.abs(longitude));
    }
            
    public boolean isValid() {
        return isValid(this);
    }

    @Override
    public int hashCode() {
        int _hash = 17;
        _hash = _hash * 29 + getLatitudeE6();
        _hash = _hash * 29 + getLongitudeE6();
        return _hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof LocPoint)) {
            return false;
        }

        LocPoint lhs = (LocPoint) o;
        return lhs != null &&
                getLatitudeE6() == lhs.getLatitudeE6() &&
                getLongitudeE6() == lhs.getLongitudeE6();
    }

    @Override
    public String toString() {
        return getFormattedLatLng(latitude) + ", " + getFormattedLatLng(longitude);
    }

    @Override
    public int compareTo(LocPoint another) {
        if (getLatitudeE6() != another.getLatitudeE6())
            return getLatitudeE6() < another.getLatitudeE6() ? -1 : 1;
        else if (getLongitudeE6() != another.getLongitudeE6())
            return getLongitudeE6() < another.getLongitudeE6() ? -1 : 1;
        else
            return 0;
    }


    public static boolean isValid(LocPoint locPoint) {
        return Math.round(locPoint.latitude * 1E6) != 0 || Math.round(locPoint.longitude * 1E6) != 0;
    }


    public static int normalizeLat(int lat) {
        return Math.max(-MAX_LAT, Math.min(MAX_LAT, lat));
    }

    public static int normalizeLng(int lng) {
        while (lng > MAX_LNG)
            lng -= 2 * MAX_LNG;
        while (lng < -MAX_LNG)
            lng += 2 * MAX_LNG;
        return lng;
    }

    public static String getFormattedLatLng(double value) {
        return getFormattedLatLng((int)Math.round(value * 1E6));
    }

    public static String getFormattedLatLng(int valueE6) {
        String ret = "" + (Math.abs(valueE6) % 1000000);
        while (ret.length() < 6)
            ret = "0" + ret;
        ret = "" + (valueE6 / 1000000) + "." + ret;
        return ret;
        //return format.format(value);
    }


    public double getDistanceFrom(LocPoint other) {
        return getDistance(this, other);
    }

    public double getDistanceFrom(double lat, double lng) {
        return getDistance(this.getLatitude(), this.getLongitude(), lat, lng);
    }

    public static double getDistance(LocPoint first, LocPoint second) {
        return getDistance(first.getLatitude(), first.getLongitude(), second.getLatitude(), second.getLongitude());
    }

    /**
     * Vraci vzdalenost v metrech
     */
    public static double getDistance(double nLat1, double nLong1, double nLat2, double nLong2)
    {
        double nTheta = nLong1 - nLong2;
        double nDist = Math.sin(degreesToRadians(nLat1)) * Math.sin(degreesToRadians(nLat2)) +
                Math.cos(degreesToRadians(nLat1)) * Math.cos(degreesToRadians(nLat2)) * Math.cos(degreesToRadians(nTheta));
        nDist = Math.acos(nDist);
        nDist = radiansToDegrees(nDist);
        double nRet = nDist * 60 * 1.1515 * 1.609344 * 1000;

        return Double.isNaN(nRet) ? 0 : nRet;
    }

    public static LocPoint getGpsByBaseGpsAndDistanceAndAzimuth(double latitude, double longitude, double distance, double azimuth) {
        final double rEarth = 6371.01; // Earth's average radius in km
        final double epsilon = 0.000001; // threshold for floating-point equality

        double rlat1 = degreesToRadians(latitude);
        double rlon1 = degreesToRadians(longitude);
        double rbearing = degreesToRadians(azimuth) * -1; // to * -1 jsem tam dal, aby nebyl otoceny vychod/zapad
        double rdistance = distance / (rEarth * 1000); // normalize linear distance to radian angle

        double rlat = Math.asin(Math.sin(rlat1) * Math.cos(rdistance) + Math.cos(rlat1) * Math.sin(rdistance) * Math.cos(rbearing));
        double rlon;

        if (Math.cos(rlat) == 0 || Math.abs(Math.cos(rlat)) < epsilon) // Endpoint a pole
            rlon = rlon1;
        else
            rlon = ((rlon1 - Math.asin(Math.sin(rbearing) * Math.sin(rdistance) / Math.cos(rlat)) + Math.PI) % (2 * Math.PI)) - Math.PI;

        LocPoint ret = new LocPoint(radiansToDegrees(rlat), radiansToDegrees(rlon));
        return ret;
    }

    public static double getDistSquare(double dX1, double dY1, double dX2, double dY2) {
        dX2 -= dX1; dX2 *= ONE_DEGREE_LATITUDE_IN_METERS;
        dY2 -= dY1; dY2 *= ONE_DEGREE_LONGTITUDE_IN_METERS;

        return dX2 * dX2 + dY2 * dY2;
    }

    private static double radiansToDegrees(double dRadians) {
        return (double)(dRadians * (180.0 / Math.PI));
    }

    private static double degreesToRadians(double nDegrees) {
        return (double)(Math.PI * nDegrees / 180.0);
    }

    // Input a double latitude or longitude in the decimal format
    // e.g. 87.728056
    public static String decimalToDMS(double coord) {
        String output, degrees, minutes, seconds;

        // gets the modulus the coordinate divided by one (MOD1).
        // in other words gets all the numbers after the decimal point.
        // e.g. mod = 87.728056 % 1 == 0.728056
        //
        // next get the integer part of the coord. On other words the whole number part.
        // e.g. intPart = 87

        double mod = coord % 1;
        int intPart = (int)coord;

        //set degrees to the value of intPart
        //e.g. degrees = "87"

        degrees = String.valueOf(intPart);

        // next times the MOD1 of degrees by 60 so we can find the integer part for minutes.
        // get the MOD1 of the new coord to find the numbers after the decimal point.
        // e.g. coord = 0.728056 * 60 == 43.68336
        //      mod = 43.68336 % 1 == 0.68336
        //
        // next get the value of the integer part of the coord.
        // e.g. intPart = 43

        coord = mod * 60;
        mod = coord % 1;
        intPart = (int)coord;

        // set minutes to the value of intPart.
        // e.g. minutes = "43"
        minutes = String.valueOf(intPart);

        //do the same again for minutes
        //e.g. coord = 0.68336 * 60 == 41.0016
        //e.g. intPart = 41
        coord = mod * 60;
        intPart = (int)coord;

        // set seconds to the value of intPart.
        // e.g. seconds = "41"
        seconds = String.valueOf(intPart);

        //Standard output of D°M′S″
        output = degrees + "°" + minutes + "'" + seconds + "\"";

        return output;
    }

    /*
     * Conversion DMS to decimal
     *
     * Input: latitude or longitude in the DMS format ( example: N 43° 36' 15.894")
     * Return: latitude or longitude in decimal format
     * hemisphereOUmeridien => {W,E,S,N}
     *
     */
    public static double DMSToDecimal(String hemisphereOUmeridien, double degres, double minutes, double secondes)
    {
        double LatOrLon=0;
        double signe=1.0;

        if((hemisphereOUmeridien=="W")||(hemisphereOUmeridien=="S")) {signe=-1.0;}
        LatOrLon = signe*(Math.floor(degres) + Math.floor(minutes)/60.0 + secondes/3600.0);

        return(LatOrLon);
    }

    
    public static final ApiCreator<LocPoint> CREATOR = new ApiCreator<LocPoint>() {
        public LocPoint create(ApiDataInput d) { return new LocPoint(d); }
        public LocPoint[] newArray(int size) { return new LocPoint[size]; }
    };
}