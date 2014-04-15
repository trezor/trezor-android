package com.circlegate.liban.ws;

import android.net.Uri;
import android.os.SystemClock;
import android.text.TextUtils;

import com.circlegate.liban.R;
import com.circlegate.liban.base.ApiBase.ApiCreator;
import com.circlegate.liban.base.ApiDataIO.ApiDataInput;
import com.circlegate.liban.base.ApiDataIO.ApiDataOutput;
import com.circlegate.liban.base.CommonClasses.Couple;
import com.circlegate.liban.location.LocPoint;
import com.circlegate.liban.task.TaskErrors.BaseError;
import com.circlegate.liban.task.TaskErrors.ITaskError;
import com.circlegate.liban.task.TaskErrors.TaskError;
import com.circlegate.liban.task.TaskErrors.TaskException;
import com.circlegate.liban.task.TaskInterfaces.ITask;
import com.circlegate.liban.task.TaskInterfaces.ITaskContext;
import com.circlegate.liban.utils.EqualsUtils;
import com.circlegate.liban.utils.LogUtils;
import com.circlegate.liban.ws.WsBase.IWsParam;
import com.circlegate.liban.ws.WsBase.IWsResult;
import com.circlegate.liban.ws.WsBase.WsParam;
import com.circlegate.liban.ws.WsBase.WsResult;
import com.google.common.collect.ImmutableList;

import org.joda.time.Duration;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;

import okhttp3.Request.Builder;
import okhttp3.Response;

public class WsGoogleApis {
    public static class GoogleUtils {
        public static Uri.Builder createUriBuilder(ITaskContext context, boolean https, String subPath, String optKey) {
            Uri.Builder uri = new Uri.Builder()
                .scheme(https ? "https" : "http")
                .authority("maps.googleapis.com")
                .path("maps/api/" + subPath + "/json")
                .appendQueryParameter("sensor", "true")
                .appendQueryParameter("language", context.getCurrentLangAbbrev());
            
            if (!TextUtils.isEmpty(optKey))
                uri.appendQueryParameter("key", optKey);
            return uri;
        }
        
        public static String encodeUriLocation(LocPoint l) {
            return l.getLatitudeString() + "," + l.getLongitudeString(); 
        }
        
        public static LocPoint parseLocation(JSONObject location) throws JSONException {
            return new LocPoint(location.getDouble("lat"), location.getDouble("lng"));
        }
        
        public static ImmutableList<LocPoint> decodePolyline(String encoded) {
            ImmutableList.Builder<LocPoint> poly = ImmutableList.builder();
            int index = 0, len = encoded.length();
            int lat = 0, lng = 0;

            while (index < len) {
                int b, shift = 0, result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lat += dlat;

                shift = 0;
                result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lng += dlng;

                LocPoint p = new LocPoint((double)lat / 1E5, (double)lng / 1E5);
                poly.add(p);
            }
            return poly.build();
        }

        public static String encodePolyline(Collection<? extends LocPoint> points) {
            StringBuilder str = new StringBuilder();

            int lastLat = 0;
            int lastLng = 0;
            for (LocPoint point : points) {
                int lat = (point.getLatitudeE6() + 5) / 10;
                int lng = (point.getLongitudeE6() + 5) / 10;

                int diff = lat - lastLat;
                {
                    int shifted = diff << 1;
                    if (diff < 0)
                        shifted = ~shifted;
                    int rem = shifted;
                    while (rem >= 0x20) {
                        str.append((char) ((0x20 | (rem & 0x1f)) + 63));
                        rem >>= 5;
                    }
                    str.append((char) (rem + 63));
                }

                diff = lng - lastLng;
                { // Stejny blok jako ten vyse - jenom kvuli tomu, aby cely kod byl
                  // v jedine funkci
                    int shifted = diff << 1;
                    if (diff < 0)
                        shifted = ~shifted;
                    int rem = shifted;
                    while (rem >= 0x20) {
                        str.append((char) ((0x20 | (rem & 0x1f)) + 63));
                        rem >>= 5;
                    }
                    str.append((char) (rem + 63));
                }

                lastLat = lat;
                lastLng = lng;
            }
            return str.toString();
        }
        
        public static Couple<String, String> generateNameDesc(String formattedAddress) {
            String d = formattedAddress;
            int commaIndex = d.indexOf(',');
            String name = commaIndex < 0 ? d : d.substring(0, commaIndex);
            String desc = commaIndex < 0 ? "" : d.substring(commaIndex + 1).trim();
            
            desc = desc.replace("Česká republika", "CZ");
            desc = desc.replace("Slovensko", "SK");
            desc = desc.replaceFirst("\\d\\d\\d \\d\\d (.*)", "$1");
            
            return new Couple<String, String>(name, desc);
        }
    }
    
    public static class GoogleError extends TaskError {
        public static final GoogleError ZERO_RESULTS = new GoogleError("ZERO_RESULTS", R.string.err_google_zero_results, true) {
            @Override
            public boolean isOk() {
                return true;
            }
        };
        public static final GoogleError OVER_QUERY_LIMIT = new GoogleError("OVER_QUERY_LIMIT", R.string.err_google_over_query_limit, false);
        public static final GoogleError REQUEST_DENIED = new GoogleError("REQUEST_DENIED", R.string.err_google_request_denied, false);
        public static final GoogleError INVALID_REQUEST = new GoogleError("INVALID_REQUEST", R.string.err_google_invalid_request, true);
        public static final GoogleError MAX_ELEMENTS_EXCEEDED = new GoogleError("MAX_ELEMENTS_EXCEEDED", R.string.err_google_max_elements_exceeded, true);
        public static final GoogleError NOT_FOUND = new GoogleError("NOT_FOUND", R.string.err_google_not_found, true);
        public static final GoogleError MAX_WAYPOINTS_EXCEEDED = new GoogleError("MAX_WAYPOINTS_EXCEEDED", R.string.err_google_max_waypoint_exceeded, true);

        private static volatile HashMap<String, GoogleError> map;

        private final String id;
        private final int msgRid;
        private final boolean cacheable;

        static {
            map = new HashMap<>();
        }
        
        private GoogleError(String id, int msgRid, boolean cacheable) {
            this.id = id;
            this.msgRid = msgRid;
            this.cacheable = cacheable;

            synchronized (GoogleError.class) {
                if (map == null)
                    map = new HashMap<>();
                map.put(id, this);
            }
        }

        @Override
        public void save(ApiDataOutput d, int flags) {
            d.write(this.id);

        }

        public String getId() {
            return this.id;
        }

        public int getMsgRid() {
            return this.msgRid;
        }

        public boolean isCacheable() {
            return cacheable;
        }

        @Override
        public int hashCode() {
            int _hash = 17;
            _hash = _hash * 29 + EqualsUtils.hashCodeCheckNull(id);
            return _hash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof GoogleError)) {
                return false;
            }

            GoogleError lhs = (GoogleError) o;
            return lhs != null &&
                    EqualsUtils.equalsCheckNull(id, lhs.id);
        }


        @Override
        public CharSequence getMsg(ITaskContext context) {
            return msgRid == 0 ? "" : context.getAndroidContext().getString(msgRid);
        }

        @Override
        public String getGoogleAnalyticsId() {
            return "GoogleError:" + id;
        }

        public static ITaskError getErrorById(String id) {
            if (id.equals("OK"))
                return BaseError.ERR_OK;
            else {
                GoogleError ret = getGoogleErrorById(id);
                if (ret != null)
                    return ret;
                else
                    return BaseError.ERR_UNKNOWN_ERROR;
            }
        }

        private static GoogleError getGoogleErrorById(String id) {
            synchronized (GoogleError.class) {
                if (map == null)
                    return null;
                return map.get(id);
            }
        }
        
        public static final ApiCreator<GoogleError> CREATOR = new ApiCreator<GoogleError>() {
            public GoogleError create(ApiDataInput d) { return getGoogleErrorById(d.readString()); }
            public GoogleError[] newArray(int size) { return new GoogleError[size]; }
        };
    }
    

    public interface IGoogleParam extends IWsParam {
    }

    public interface IGoogleResult extends IWsResult {
    }
    
    
    public static abstract class GoogleParam extends WsParam implements IGoogleParam {
        //private static final String TAG = GoogleParam.class.getSimpleName();
        private static volatile long lastExecutionTime;

        @Override
        public String getSerialExecutionKey(ITaskContext context) {
            return "WS_GOOGLE";
        }

        @Override
        public int getRetries(ITaskContext context, ITask task) {
            return 2;
        }

        @Override
        protected Builder createRequest(ITaskContext context, ITask task) {
            return WsUtils.createRequestAcceptingJsonResponse(getUri(context, task));
        }

        @Override
        protected IWsResult createResult(ITaskContext context, ITask task, Response acceptableResponse) throws TaskException, IOException {
            try {
                JSONObject json = WsUtils.readResponseJson(acceptableResponse);
                return createResult(context, task, json);
            }
            catch (JSONException ex) {
                return createErrorResult(context, task, BaseError.ERR_CONNECTION_ERROR_UNEXPECTED_RES);
            }
        }

        @Override
        public IWsResult createResultUncached(ITaskContext context, ITask task) throws TaskException {
            final long minTimeBetween = 500;
            final long minDelayBefore = getMinDelayBefore();
            final long currentTime = SystemClock.elapsedRealtime();
            final long lastExecutionTime;
            
            synchronized (GoogleParam.class) {
                lastExecutionTime = GoogleParam.lastExecutionTime;
            }
            
            if (lastExecutionTime + minTimeBetween > currentTime || minDelayBefore > 0) {
                long sleepTime = Math.min(minTimeBetween, (lastExecutionTime + minTimeBetween) - currentTime);
                sleepTime = Math.max(sleepTime, minDelayBefore);
                log("Sleeping for: " + sleepTime + " ms");
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) { }
            }
            
            if (task.isCanceled()) {
                log("Task was canceled before execution");
                return null;
            }
            
            log("Executing");
            IWsResult ret = super.createResultUncached(context, task);
            if (ret == null)
                log("Finished, ret == null");
            else if (ret.isValidResult())
                log("Finished, valid result");
            else
                log("Finished, " + ret.getError().getMsg(context));
            
            synchronized (GoogleParam.class) {
                GoogleParam.lastExecutionTime = SystemClock.elapsedRealtime();
            }
            return ret;
        }

        public long getMinDelayBefore() {
            return 0;
        }
        
        private void log(String msg) {
            LogUtils.d(getClass().getSimpleName(), this.toString() + " - " + msg);
        }


        public abstract String getUri(ITaskContext context, ITask task);
        public abstract IGoogleResult createResult(ITaskContext context, ITask task, JSONObject json) throws JSONException;
    }
    
    public static abstract class GoogleResult<TGoogleParam extends IGoogleParam> extends WsResult<TGoogleParam> implements IGoogleResult {
        public GoogleResult(TGoogleParam param, ITaskError error) {
            super(param, error);
        }
        
        public GoogleResult(TGoogleParam param, JSONObject json) throws JSONException {
            super(param, GoogleError.getErrorById(json.getString("status")));
        }

        @Override
        public boolean isCacheableResult() {
            if (isValidResult())
                return true;
            else
                return
                    (getError() instanceof GoogleError) ?
                    ((GoogleError)getError()).isCacheable() : super.isCacheableResult();
        }
    }

    
    public static class GooglePlaceAutocompleteParam extends GoogleParam {
        public static final int TYPES_ANY = 0;
        public static final int TYPES_GEOCODE = 1;
        public static final int TYPES_ESTABLISHMENT = 2;
        public static final int TYPES_REGIONS = 3;
        public static final int TYPES_CITIES = 4;
        
        private final String input;
        private final String key;
        private final LocPoint location; // default LocPoint.INVALID
        private final int radius; // default -1
        private final int types; // default TYPES_ANY
        private final String components; // optional
        
        public GooglePlaceAutocompleteParam(String input, String key, LocPoint location, int radius, int types, String components) {
            this.input = input;
            this.key = key;
            this.location = location;
            this.radius = radius;
            this.types = types;
            this.components = components;
        }

        public String getInput() {
            return this.input;
        }
        
        public String getKey() {
            return this.key;
        }
        
        public LocPoint getLocation() {
            return this.location;
        }
        
        public int getRadius() {
            return this.radius;
        }
        
        public int getTypes() {
            return this.types;
        }
        
        public String getComponents() {
            return this.components;
        }

        @Override
        public int hashCode() {
            int _hash = 17;
            _hash = _hash * 29 + EqualsUtils.hashCodeCheckNull(input);
            _hash = _hash * 29 + EqualsUtils.hashCodeCheckNull(key);
            _hash = _hash * 29 + EqualsUtils.hashCodeCheckNull(location);
            _hash = _hash * 29 + radius;
            _hash = _hash * 29 + types;
            _hash = _hash * 29 + EqualsUtils.hashCodeCheckNull(components);
            return _hash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            
            if (!(o instanceof GooglePlaceAutocompleteParam)) {
                return false;
            }
            
            GooglePlaceAutocompleteParam lhs = (GooglePlaceAutocompleteParam) o;
            return lhs != null &&
                EqualsUtils.equalsCheckNull(input, lhs.input) &&
                EqualsUtils.equalsCheckNull(key, lhs.key) &&
                EqualsUtils.equalsCheckNull(location, lhs.location) &&
                radius == lhs.radius &&
                types == lhs.types &&
                EqualsUtils.equalsCheckNull(components, lhs.components);
        }
        
        @Override
        public long getMinDelayBefore() {
            return 800;
        }
        
        @Override
        public String getUri(ITaskContext context, ITask task) {
            Uri.Builder uri = GoogleUtils.createUriBuilder(context, true, "place/autocomplete", key);
            uri.appendQueryParameter("input", input);

            if (!location.equals(LocPoint.INVALID))
                uri.appendQueryParameter("location", GoogleUtils.encodeUriLocation(location));
            if (radius >= 0)
                uri.appendQueryParameter("radius", Integer.toString(radius));

            switch (types) {
            case TYPES_ANY: break;
            case TYPES_GEOCODE: uri.appendQueryParameter("types", "geocode"); break;
            case TYPES_ESTABLISHMENT: uri.appendQueryParameter("types", "establishment"); break;
            case TYPES_REGIONS: uri.appendQueryParameter("types", "(regions)"); break;
            case TYPES_CITIES: uri.appendQueryParameter("types", "(cities)"); break;
            default: throw new RuntimeException("Not implemented");
            }

            if (!TextUtils.isEmpty(components))
                uri.appendQueryParameter("components", components);

            return uri.build().toString();
        }


        @Override
        public GooglePlaceAutocompleteResult createResult(ITaskContext context, ITask task, JSONObject json) throws JSONException {
            return new GooglePlaceAutocompleteResult(this, json);
        }

        @Override
        public GooglePlaceAutocompleteResult createErrorResult(ITaskContext context, ITask task, ITaskError error) {
            return new GooglePlaceAutocompleteResult(this, error, null);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + ": " + input;
        }
    }
    
    public static class GooglePlaceAutocompleteResult extends GoogleResult<GooglePlaceAutocompleteParam> {
        private final ImmutableList<GooglePrediction> predictions;
        
        public GooglePlaceAutocompleteResult(GooglePlaceAutocompleteParam param, ITaskError error, ImmutableList<GooglePrediction> predictions) {
            super(param, error);
            this.predictions = predictions;
        }
        
        public GooglePlaceAutocompleteResult(GooglePlaceAutocompleteParam param, JSONObject json) throws JSONException {
            super(param, json);

            if (isValidResult()) {
                ImmutableList.Builder<GooglePrediction> predictions = new ImmutableList.Builder<GooglePrediction>();
                JSONArray jPredictions = json.getJSONArray("predictions");
                for (int i = 0; i < jPredictions.length(); i++) 
                    predictions.add(new GooglePrediction(jPredictions.getJSONObject(i)));
                this.predictions = predictions.build();
            }
            else {
                this.predictions = null;
            }
        }

        public ImmutableList<GooglePrediction> getPredictions() {
            return this.predictions;
        }
    }
    
    
    public static class GooglePlaceDetailParam extends GoogleParam {
        private final String key;
        private final String placeId;
        
        public GooglePlaceDetailParam(String key, String placeId) {
            this.key = key;
            this.placeId = placeId;
        }

        public String getKey() {
            return this.key;
        }
        
        public String getPlaceId() {
            return this.placeId;
        }

        @Override
        public int hashCode() {
            int _hash = 17;
            _hash = _hash * 29 + EqualsUtils.hashCodeCheckNull(key);
            _hash = _hash * 29 + EqualsUtils.hashCodeCheckNull(placeId);
            return _hash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            
            if (!(o instanceof GooglePlaceDetailParam)) {
                return false;
            }
            
            GooglePlaceDetailParam lhs = (GooglePlaceDetailParam) o;
            return lhs != null &&
                EqualsUtils.equalsCheckNull(key, lhs.key) &&
                EqualsUtils.equalsCheckNull(placeId, lhs.placeId);
        }
        
        @Override
        public String getUri(ITaskContext context, ITask task) {
            Uri.Builder uri = GoogleUtils.createUriBuilder(context, true, "place/details", key)
                .appendQueryParameter("placeid", placeId);
            return uri.build().toString();
        }

        @Override
        public GooglePlaceDetailResult createResult(ITaskContext context, ITask task, JSONObject json) throws JSONException {
            return new GooglePlaceDetailResult(this, json);
        }

        @Override
        public GooglePlaceDetailResult createErrorResult(ITaskContext context, ITask task, ITaskError error) {
            return new GooglePlaceDetailResult(this, error, null);
        }
    }
    
    public static class GooglePlaceDetailResult extends GoogleResult<GooglePlaceDetailParam> {
        private final LocPoint location;
        
        public GooglePlaceDetailResult(GooglePlaceDetailParam param, ITaskError error, LocPoint location) {
            super(param, error);
            this.location = location;
        }
        
        public GooglePlaceDetailResult(GooglePlaceDetailParam param, JSONObject json) throws JSONException {
            super(param, json);

            if (isValidResult()) {
                JSONObject result = json.getJSONObject("result");
                this.location = GoogleUtils.parseLocation(result.getJSONObject("geometry").getJSONObject("location"));
            }
            else {
                this.location = null;
            }
        }

        public LocPoint getLocation() {
            return this.location;
        }
    }
    
    
    public static class GoogleDirectionsParam extends GoogleParam {
        public static final int MODE_DRIVING = 0;
        public static final int MODE_WALKING = 1;
        public static final int MODE_BICYCLING = 2;
        public static final int MODE_TRANSIT = 3;
        
        public static final int UNITS_METRIC = 0;
        public static final int UNITS_IMPERIAL = 1;
        
        private final GoogleLocationOrAddress origin;
        private final GoogleLocationOrAddress destination;
        private final int mode; // default MODE_DRIVING
        private final int units; // default UNITS_METRIC
        
        public GoogleDirectionsParam(GoogleLocationOrAddress origin, GoogleLocationOrAddress destination, int mode, int units) {
            this.origin = origin;
            this.destination = destination;
            this.mode = mode;
            this.units = units;
        }

        public GoogleLocationOrAddress getOrigin() {
            return this.origin;
        }
        
        public GoogleLocationOrAddress getDestination() {
            return this.destination;
        }
        
        public int getMode() {
            return this.mode;
        }
        
        public int getUnits() {
            return this.units;
        }

        @Override
        public int hashCode() {
            int _hash = 17;
            _hash = _hash * 29 + EqualsUtils.hashCodeCheckNull(origin);
            _hash = _hash * 29 + EqualsUtils.hashCodeCheckNull(destination);
            _hash = _hash * 29 + mode;
            _hash = _hash * 29 + units;
            return _hash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            
            if (!(o instanceof GoogleDirectionsParam)) {
                return false;
            }
            
            GoogleDirectionsParam lhs = (GoogleDirectionsParam) o;
            return lhs != null &&
                EqualsUtils.equalsCheckNull(origin, lhs.origin) &&
                EqualsUtils.equalsCheckNull(destination, lhs.destination) &&
                mode == lhs.mode &&
                units == lhs.units;
        }
        
        @Override
        public String getUri(ITaskContext context, ITask task) {
            Uri.Builder uri = GoogleUtils.createUriBuilder(context, false, "directions", null)
                .appendQueryParameter("origin", origin.getUriArgValue())
                .appendQueryParameter("destination", destination.getUriArgValue());
            
            switch (mode) {
            case MODE_DRIVING: break;
            case MODE_WALKING: uri.appendQueryParameter("mode", "walking"); break;
            case MODE_BICYCLING: uri.appendQueryParameter("mode", "bicycling"); break;
            case MODE_TRANSIT: uri.appendQueryParameter("mode", "transit"); break;
            default: throw new RuntimeException("Not implemented");
            }
            
            switch (units) {
            case UNITS_METRIC: break;
            case UNITS_IMPERIAL: uri.appendQueryParameter("units", "imperial"); break;
            default: throw new RuntimeException("Not implemented");
            }
            
            return uri.build().toString();
        }

        @Override
        public GoogleDirectionsResult createResult(ITaskContext context, ITask task, JSONObject json) throws JSONException {
            return new GoogleDirectionsResult(this, json);
        }

        @Override
        public GoogleDirectionsResult createErrorResult(ITaskContext context, ITask task, ITaskError error) {
            return new GoogleDirectionsResult(this, error, null, null, null);
        }
    }
    
    public static class GoogleDirectionsResult extends GoogleResult<GoogleDirectionsParam> {
        private final GoogleDuration duration;
        private final GoogleDistance distance;
        private final ImmutableList<LocPoint> polyline;
        
        public GoogleDirectionsResult(GoogleDirectionsParam param, ITaskError error, GoogleDuration duration, GoogleDistance distance, ImmutableList<LocPoint> polyline) {
            super(param, error);
            this.duration = duration;
            this.distance = distance;
            this.polyline = polyline;
        }
        
        public GoogleDirectionsResult(GoogleDirectionsParam param, JSONObject json) throws JSONException {
            super(param, json);

            if (isValidResult()) {
                JSONObject route = json.getJSONArray("routes").getJSONObject(0);
                JSONObject leg = route.getJSONArray("legs").getJSONObject(0);
                                
                this.duration = new GoogleDuration(leg.getJSONObject("duration"));
                this.distance = new GoogleDistance(leg.getJSONObject("distance"));
                this.polyline = GoogleUtils.decodePolyline(route.getJSONObject("overview_polyline").getString("points"));
            }
            else {
                this.duration = null;
                this.distance = null;
                this.polyline = null;
            }
        }

        public GoogleDuration getDuration() {
            return this.duration;
        }
        
        public GoogleDistance getDistance() {
            return this.distance;
        }
        
        public ImmutableList<LocPoint> getPolyline() {
            return this.polyline;
        }
    }
    
    
    public static class GoogleDistanceMatrixParam extends GoogleParam {
        public static final int MAX_RESULTS = 50; // 100 je moc dlouha url, navic problem se zadanym Odkud i Kam jako adresa...

        public static final int MODE_DRIVING = 0;
        public static final int MODE_WALKING = 1;
        public static final int MODE_BICYCLING = 2;
                
        public static final int UNITS_METRIC = 0;
        public static final int UNITS_IMPERIAL = 1;
        
        private final ImmutableList<GoogleLocationOrAddress> origins;
        private final ImmutableList<GoogleLocationOrAddress> destinations;
        private final int mode; // default MODE_DRIVING
        private final int units; // default UNITS_METRIC
        
        public GoogleDistanceMatrixParam(ImmutableList<GoogleLocationOrAddress> origins, ImmutableList<GoogleLocationOrAddress> destinations, int mode, int units) {
            this.origins = origins;
            this.destinations = destinations;
            this.mode = mode;
            this.units = units;
        }

        public ImmutableList<GoogleLocationOrAddress> getOrigins() {
            return this.origins;
        }
        
        public ImmutableList<GoogleLocationOrAddress> getDestinations() {
            return this.destinations;
        }
        
        public int getMode() {
            return this.mode;
        }
        
        public int getUnits() {
            return this.units;
        }

        private int _hash = EqualsUtils.HASHCODE_INVALID;

        @Override
        public int hashCode() {
            if (_hash == EqualsUtils.HASHCODE_INVALID) {
                int _hash = 17;
                _hash = _hash * 29 + EqualsUtils.itemsHashCode(origins);
                _hash = _hash * 29 + EqualsUtils.itemsHashCode(destinations);
                _hash = _hash * 29 + mode;
                _hash = _hash * 29 + units;
                this._hash = _hash == EqualsUtils.HASHCODE_INVALID ? EqualsUtils.HASHCODE_INVALID_REPLACEMENT : _hash;
            }
            return this._hash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            
            if (!(o instanceof GoogleDistanceMatrixParam)) {
                return false;
            }
            
            GoogleDistanceMatrixParam lhs = (GoogleDistanceMatrixParam) o;
            return lhs != null &&
                EqualsUtils.itemsEqual(origins, lhs.origins) &&
                EqualsUtils.itemsEqual(destinations, lhs.destinations) &&
                mode == lhs.mode &&
                units == lhs.units;
        }
        
        @Override
        public String getUri(ITaskContext context, ITask task) {
            Uri.Builder uri = GoogleUtils.createUriBuilder(context, false, "distancematrix", null); 
            
            StringBuilder origins = new StringBuilder();
            for (GoogleLocationOrAddress g : this.origins) {
                if (origins.length() > 0)
                    origins.append('|');
                origins.append(g.getUriArgValue());
            }
            uri.appendQueryParameter("origins", origins.toString());
            
            StringBuilder destinations = new StringBuilder();
            for (GoogleLocationOrAddress g : this.destinations) {
                if (destinations.length() > 0)
                    destinations.append('|');
                destinations.append(g.getUriArgValue());
            }
            uri.appendQueryParameter("destinations", destinations.toString());
            
            switch (mode) {
            case MODE_DRIVING: break;
            case MODE_WALKING: uri.appendQueryParameter("mode", "walking"); break;
            case MODE_BICYCLING: uri.appendQueryParameter("mode", "bicycling"); break;
            default: throw new RuntimeException("Not implemented");
            }
            
            switch (units) {
            case UNITS_METRIC: break;
            case UNITS_IMPERIAL: uri.appendQueryParameter("units", "imperial"); break;
            default: throw new RuntimeException("Not implemented");
            }
            
            return uri.build().toString();
        }

        @Override
        public GoogleDistanceMatrixResult createResult(ITaskContext context, ITask task, JSONObject json) throws JSONException {
            return new GoogleDistanceMatrixResult(this, json);
        }

        @Override
        public GoogleDistanceMatrixResult createErrorResult(ITaskContext context, ITask task, ITaskError error) {
            return new GoogleDistanceMatrixResult(this, error, null, null, null);
        }
    }
    
    public static class GoogleDistanceMatrixResult extends GoogleResult<GoogleDistanceMatrixParam> {
        private final ImmutableList<String> originAddresses;
        private final ImmutableList<String> destinationAddresses;
        private final ImmutableList<GoogleDistanceMatrixRow> rows;
        
        public GoogleDistanceMatrixResult(GoogleDistanceMatrixParam param, ITaskError error, ImmutableList<String> originAddresses, ImmutableList<String> destinationAddresses, ImmutableList<GoogleDistanceMatrixRow> rows) {
            super(param, error);
            this.originAddresses = originAddresses;
            this.destinationAddresses = destinationAddresses;
            this.rows = rows;
        }
        
        public GoogleDistanceMatrixResult(GoogleDistanceMatrixParam param, JSONObject json) throws JSONException {
            super(param, json);

            if (isValidResult()) {
                ImmutableList.Builder<String> originAddresses = new ImmutableList.Builder<String>();
                JSONArray jOriginAddresses = json.getJSONArray("origin_addresses");
                for (int i = 0; i < jOriginAddresses.length(); i++) 
                    originAddresses.add(jOriginAddresses.getString(i));
                this.originAddresses = originAddresses.build();
                
                ImmutableList.Builder<String> destinationAddresses = new ImmutableList.Builder<String>();
                JSONArray jDestinationAddresses = json.getJSONArray("destination_addresses");
                for (int i = 0; i < jDestinationAddresses.length(); i++) 
                    destinationAddresses.add(jDestinationAddresses.getString(i));
                this.destinationAddresses = destinationAddresses.build();
                
                ImmutableList.Builder<GoogleDistanceMatrixRow> rows = new ImmutableList.Builder<GoogleDistanceMatrixRow>();
                JSONArray jRows = json.getJSONArray("rows");
                for (int i = 0; i < jRows.length(); i++) 
                    rows.add(new GoogleDistanceMatrixRow(jRows.getJSONObject(i)));
                this.rows = rows.build();
            }
            else {
                this.originAddresses = null;
                this.destinationAddresses = null;
                this.rows = null;
            }
        }

        public ImmutableList<String> getOriginAddresses() {
            return this.originAddresses;
        }
        
        public ImmutableList<String> getDestinationAddresses() {
            return this.destinationAddresses;
        }
        
        public ImmutableList<GoogleDistanceMatrixRow> getRows() {
            return this.rows;
        }
    }
    
    
    public static class GoogleGeocodeParam extends GoogleParam {
        private final GoogleLocationOrAddress place;
	
    	public GoogleGeocodeParam(GoogleLocationOrAddress place) {
    		this.place = place;
    	}

    	public GoogleLocationOrAddress getPlace() {
    		return this.place;
    	}
    
    	@Override
    	public int hashCode() {
    		int _hash = 17;
    		_hash = _hash * 29 + EqualsUtils.hashCodeCheckNull(place);
    		return _hash;
    	}
    
    	@Override
    	public boolean equals(Object o) {
    		if (this == o) {
    			return true;
    		}
    		
    		if (!(o instanceof GoogleGeocodeParam)) {
    			return false;
    		}
    		
    		GoogleGeocodeParam lhs = (GoogleGeocodeParam) o;
    		return lhs != null &&
    			EqualsUtils.equalsCheckNull(place, lhs.place);
    	}

        @Override
        public String getUri(ITaskContext context, ITask task) {
            Uri.Builder uri = GoogleUtils.createUriBuilder(context, false, "geocode", null)
                .appendQueryParameter(place.getUriArgKey(), place.getUriArgValue());
            return uri.build().toString();
        }

        @Override
        public GoogleGeocodeResult createResult(ITaskContext context, ITask task, JSONObject json) throws JSONException {
            return new GoogleGeocodeResult(this, json);
        }

        @Override
        public GoogleGeocodeResult createErrorResult(ITaskContext context, ITask task, ITaskError error) {
            return new GoogleGeocodeResult(this, error, null, null);
        }
    }
    
    public static class GoogleGeocodeResult extends GoogleResult<GoogleGeocodeParam> {
        private final String formattedAddress;
        private final LocPoint location;
        
        public GoogleGeocodeResult(GoogleGeocodeParam param, ITaskError error, String formattedAddress, LocPoint location) {
            super(param, error);
            this.formattedAddress = formattedAddress;
            this.location = location;
        }
        
        public GoogleGeocodeResult(GoogleGeocodeParam param, JSONObject json) throws JSONException {
            super(param, json);

            if (isValidResult()) {
                JSONObject result = json.getJSONArray("results").getJSONObject(0);
                this.formattedAddress = result.getString("formatted_address");
                this.location = GoogleUtils.parseLocation(result.getJSONObject("geometry").getJSONObject("location"));
            }
            else {
                this.formattedAddress = null;
                this.location = null;
            }
        }

        public String getFormattedAddress() {
            return this.formattedAddress;
        }
        
        public LocPoint getLocation() {
            return this.location;
        }
    }
    
    
    public static class GooglePrediction {
        private final String description;
        private final String id;
        private final String placeId;
        private final ImmutableList<GoogleTerm> terms;
        private final ImmutableList<GoogleMatchedSubstring> matchedSubstrings;
        
        public GooglePrediction(String description, String id, String placeId, ImmutableList<GoogleTerm> terms, ImmutableList<GoogleMatchedSubstring> matchedSubstrings) {
            this.description = description;
            this.id = id;
            this.placeId = placeId;
            this.terms = terms;
            this.matchedSubstrings = matchedSubstrings;
        }
        
        public GooglePrediction(JSONObject json) throws JSONException {
            this.description = json.getString("description");
            this.id = json.getString("id");
            this.placeId = json.getString("place_id");
            
            ImmutableList.Builder<GoogleTerm> terms = new ImmutableList.Builder<GoogleTerm>();
            JSONArray jTerms = json.getJSONArray("terms");
            for (int i = 0; i < jTerms.length(); i++) 
                terms.add(new GoogleTerm(jTerms.getJSONObject(i)));
            this.terms = terms.build();
            
            ImmutableList.Builder<GoogleMatchedSubstring> matchedSubstrings = new ImmutableList.Builder<GoogleMatchedSubstring>();
            JSONArray jMatchedSubstrings = json.getJSONArray("matched_substrings");
            for (int i = 0; i < jMatchedSubstrings.length(); i++) 
                matchedSubstrings.add(new GoogleMatchedSubstring(jMatchedSubstrings.getJSONObject(i)));
            this.matchedSubstrings = matchedSubstrings.build();
        }

        public String getDescription() {
            return this.description;
        }
        
        public String getId() {
            return this.id;
        }
        
        public String getPlaceId() {
            return this.placeId;
        }
        
        public ImmutableList<GoogleTerm> getTerms() {
            return this.terms;
        }
        
        public ImmutableList<GoogleMatchedSubstring> getMatchedSubstrings() {
            return this.matchedSubstrings;
        }
    }
    
    public static class GoogleTerm {
        private final String value;
        private final int offset;
        
        public GoogleTerm(String value, int offset) {
            this.value = value;
            this.offset = offset;
        }
        
        public GoogleTerm(JSONObject json) throws JSONException {
            this.value = json.getString("value");
            this.offset = json.getInt("offset");
        }

        public String getValue() {
            return this.value;
        }
        
        public int getOffset() {
            return this.offset;
        }
    }
    
    public static class GoogleMatchedSubstring {
        private final int offset;
        private final int length;
        
        public GoogleMatchedSubstring(int offset, int length) {
            this.offset = offset;
            this.length = length;
        }
        
        public GoogleMatchedSubstring(JSONObject json) throws JSONException {
            this.offset = json.getInt("offset");
            this.length = json.getInt("length");
        }

        public int getOffset() {
            return this.offset;
        }
        
        public int getLength() {
            return this.length;
        }
    }
    
    
    public static class GoogleLocationOrAddress {
        // Vzdy musi byt zadana bud location nebo address
        private final LocPoint latlng; // default LocPoint.INVALID
    	private final String address; // optional
    	
    	public GoogleLocationOrAddress(LocPoint latlng, String address) {
    		this.latlng = latlng;
    		this.address = address;
    	}

    	public LocPoint getLatlng() {
    		return this.latlng;
    	}
    	
    	public String getAddress() {
    		return this.address;
    	}
    
    	@Override
    	public int hashCode() {
    		int _hash = 17;
    		_hash = _hash * 29 + EqualsUtils.hashCodeCheckNull(latlng);
    		_hash = _hash * 29 + EqualsUtils.hashCodeCheckNull(address);
    		return _hash;
    	}
    
    	@Override
    	public boolean equals(Object o) {
    		if (this == o) {
    			return true;
    		}
    		
    		if (!(o instanceof GoogleLocationOrAddress)) {
    			return false;
    		}
    		
    		GoogleLocationOrAddress lhs = (GoogleLocationOrAddress) o;
    		return lhs != null &&
    			EqualsUtils.equalsCheckNull(latlng, lhs.latlng) &&
    			EqualsUtils.equalsCheckNull(address, lhs.address);
    	}
        
        public String getUriArgKey() {
            if (!latlng.equals(LocPoint.INVALID))
                return "latlng";
            else
                return "address";
        }
        
        public String getUriArgValue() {
            if (!latlng.equals(LocPoint.INVALID))
                return GoogleUtils.encodeUriLocation(latlng);
            else
                return address;
        }
    }
    
    public static class GoogleDistanceMatrixRow {
        private final ImmutableList<GoogleDistanceMatrixElement> elements;
        
        public GoogleDistanceMatrixRow(ImmutableList<GoogleDistanceMatrixElement> elements) {
            this.elements = elements;
        }
        
        public GoogleDistanceMatrixRow(JSONObject json) throws JSONException {
            
            ImmutableList.Builder<GoogleDistanceMatrixElement> elements = new ImmutableList.Builder<GoogleDistanceMatrixElement>();
            JSONArray jElements = json.getJSONArray("elements");
            for (int i = 0; i < jElements.length(); i++) 
                elements.add(new GoogleDistanceMatrixElement(jElements.getJSONObject(i)));
            this.elements = elements.build();
        }

        public ImmutableList<GoogleDistanceMatrixElement> getElements() {
            return this.elements;
        }
    }
    
    public static class GoogleDistanceMatrixElement {
        public static final int STATUS_OK = 0;
        public static final int STATUS_UNKNOWN_ERROR = 1;
        public static final int STATUS_NOT_FOUND = 2;
        public static final int STATUS_ZERO_RESULTS = 3;
        
        private final int status;
        private final GoogleDuration duration; // optional
        private final GoogleDistance distance; // optional
        
        public GoogleDistanceMatrixElement(int status, GoogleDuration duration, GoogleDistance distance) {
    		this.status = status;
    		this.duration = duration;
    		this.distance = distance;
    	}
    	
        // UPRAVENO !!!
    	public GoogleDistanceMatrixElement(JSONObject json) throws JSONException {
    	    String s = json.getString("status");    	    
    	    if (s.equals("OK"))
    	        this.status = STATUS_OK;
    	    else if (s.equals("NOT_FOUND"))
    	        this.status = STATUS_NOT_FOUND;
    	    else if (s.equals("ZERO_RESULTS"))
                this.status = STATUS_ZERO_RESULTS;
    	    else
    	        this.status = STATUS_UNKNOWN_ERROR;
    		
    		if (status == STATUS_OK) {
    		    this.duration = new GoogleDuration(json.getJSONObject("duration"));
    		    this.distance = new GoogleDistance(json.getJSONObject("distance"));
    		}
    		else {
    		    this.duration = null;
    		    this.distance = null;
    		}
    	}
    
    	public int getStatus() {
    		return this.status;
    	}
    	
    	public GoogleDuration getDuration() {
    		return this.duration;
    	}
    	
    	public GoogleDistance getDistance() {
    		return this.distance;
    	}
    }
    
    public static class GoogleDuration {
        private final Duration value;
        private final String text;
        
        public GoogleDuration(Duration value, String text) {
            this.value = value;
            this.text = text;
        }
        
        public GoogleDuration(JSONObject json) throws JSONException {
            this.value = Duration.standardSeconds(json.getInt("value"));
            this.text = json.getString("text");
        }

        public Duration getValue() {
            return this.value;
        }
        
        public String getText() {
            return this.text;
        }
    }
    
    public static class GoogleDistance {
        private final int value;
        private final String text;
        
        public GoogleDistance(int value, String text) {
            this.value = value;
            this.text = text;
        }
        
        public GoogleDistance(JSONObject json) throws JSONException {
            this.value = json.getInt("value");
            this.text = json.getString("text");
        }

        public int getValue() {
            return this.value;
        }
        
        public String getText() {
            return this.text;
        }
    }  
}
