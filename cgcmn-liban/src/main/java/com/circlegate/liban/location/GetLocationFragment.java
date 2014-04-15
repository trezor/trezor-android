package com.circlegate.liban.location;

import android.Manifest.permission;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import com.circlegate.liban.base.GlobalContextLib;
import com.circlegate.liban.fragment.BaseRetainFragment;
import com.circlegate.liban.utils.EqualsUtils;
import com.circlegate.liban.utils.FragmentUtils;
import com.circlegate.liban.utils.LogUtils;
import com.circlegate.libanloc.LocalUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GetLocationFragment extends BaseRetainFragment {
    private static final String FRAGMENT_TAG = GetLocationFragment.class.getName();
    private static final String TAG = GetLocationFragment.class.getSimpleName();

    // flagy
    public static final int PROVIDER_TYPE_NETWORK = 1;
    public static final int PROVIDER_TYPE_GPS = 1 << 1;
    public static final int PROVIDER_TYPE_NETWORK_GPS = PROVIDER_TYPE_NETWORK | PROVIDER_TYPE_GPS;

    public static final int PROVIDER_STATE_WORKING = 0;
    public static final int PROVIDER_STATE_DISABLED = 1;
    public static final int PROVIDER_STATE_OUT_OF_SERVICE = 2;

    public static final int CALLBACK_TYPE_LOC_POINT_EXT_CHANGED = 0;
    public static final int CALLBACK_TYPE_PROVIDER_STATE_CHANGED = 1;
    public static final int CALLBACK_TYPE_TIMEOUT = 2;

    private final List<GetLocationTask> getLocationTasks = new ArrayList<GetLocationTask>();


    public static <T extends FragmentActivity & IGetLocationFragmentActivity> GetLocationFragment getInstance(T activity) {
        FragmentManager fm = activity.getSupportFragmentManager();
        GetLocationFragment f = (GetLocationFragment) fm.findFragmentByTag(FRAGMENT_TAG);
        if (f == null) {
            f = new GetLocationFragment();
            fm.beginTransaction().add(f, FRAGMENT_TAG).commitAllowingStateLoss();
        }
        return f;
    }

    public interface IGetLocationFragmentActivity {
        GetLocationFragment getGetLocationFragment();
    }

    //
    // Livecycle callbacks
    //

    @Override
    public void onStart() {
        super.onStart();
        LogUtils.d(TAG, "onStart");

        for (GetLocationTask t : this.getLocationTasks) {
            t.startListening();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        LogUtils.d(TAG, "onResume");

        for (GetLocationTask t : this.getLocationTasks) {
            t.callCallbacksNowIfCan();
        }
    }

    @Override
    public void onStop() {
        LogUtils.d(TAG, "onStop");

        for (GetLocationTask t : this.getLocationTasks) {
            t.stopListening();
        }
        super.onStop();
    }


    //
    // Get location tasks
    //

    /**
     * maxLocationAgeMillis - pokud je mensi nez Lont.MAX_VALUE, tak se vzdycky bude zapinat zjistovani GPS pres listener!
     */
    public void runGetLocation(Context context, final String id, final Bundle bundle, final String fragmentTag,
                               int providers, long timeout, final long maxAge, final float maxAccuracy, final int minDistanceBetweenPoints, final long recommendedTimeBetweenFixes, boolean forceFireOnStartListening,
                               boolean callbacksEnabled) {
        GetLocationTask task = new GetLocationTask(context, id, bundle, fragmentTag,
                providers, timeout, maxAge, maxAccuracy, minDistanceBetweenPoints, recommendedTimeBetweenFixes, forceFireOnStartListening, callbacksEnabled);
        LogUtils.d(TAG, "runGetLocation: " + task.getLogId());
        this.getLocationTasks.add(task);
        task.startListening();
    }

    public IGetLocationTask getGetLocationTask(String id, String fragmentTag) {
        return getGetLocationTaskPrivate(id, fragmentTag);
    }

    public boolean containsGetLocationTask(String id, String fragmentTag) {
        //LogUtils.d(TAG, "containsGetLocationTask: " + getTaskLogId(id, fragmentTag));
        return getGetLocationTaskPrivate(id, fragmentTag) != null;
    }

    public boolean cancelGetLocationTask(String id, String fragmentTag) {
        //LogUtils.d(TAG, "cancelGetLocationTask: " + getTaskLogId(id, fragmentTag));

        GetLocationTask task = getGetLocationTaskPrivate(id, fragmentTag);
        if (task != null) {
            getLocationTasks.remove(task);
            task.stopListening();
            return true;
        } else
            return false;
    }

    public boolean setGetLocationTaskCallbacksEnabled(String id, String fragmentTag, boolean callbacksEnabled) {
        //LogUtils.d(TAG, "setGetLocationTaskCallbacksEnabled: " + getTaskLogId(id, fragmentTag) + ", callbacksEnabled: " + callbacksEnabled);
        GetLocationTask task = getGetLocationTaskPrivate(id, fragmentTag);

        if (task != null) {
            task.setCallbacksEnabled(callbacksEnabled);
            return true;
        } else
            return false;
    }

    protected void onGetLocationEvent(final GetLocationTask task, final int callbackType) {
        if (!task.getCallbacksEnabled() || !isReadyToCommitFragments()) {
            //LogUtils.d(TAG, "onGetLocationEvent: " + task.getLogId() + ", callbackType: " + callbackType + " - added to pendingCallbacks");
            task.addPendingCallback(callbackType);
        } else {
            LogUtils.d(TAG, "onGetLocationEvent: " + task.getLogId() + ", callbackType: " + callbackType + " - executing");
            OnGetLocationListener listener = task.getFragmentTag() != null ?
                    (OnGetLocationListener) FragmentUtils.findFragmentByNestedTag(getActivity(), task.getFragmentTag()) : (OnGetLocationListener) getActivity();

            if (listener == null) {
                // pro jistotu ukoncim locationlistener
                cancelGetLocationTask(task.getId(), task.getFragmentTag());
            }
            else {
                int nextStep = listener.onGetLocationEvent(task, callbackType);

                switch (nextStep) {
                    case OnGetLocationListener.REMOVE_TASK:
                        cancelGetLocationTask(task.getId(), task.getFragmentTag());
                        break;
                    case OnGetLocationListener.CONTINUE_CALLBACKS_DISABLED:
                        task.setCallbacksEnabled(false);
                        break;
                    case OnGetLocationListener.CONTINUE_CALLBACKS_ENABLED:
                        task.setCallbacksEnabled(true);
                        break;
                    default:
                        throw new RuntimeException("Not implemented");
                }
            }
        }
    }


    //
    // PRIVATE
    //

    public GetLocationTask getGetLocationTaskPrivate(String id, String fragmentTag) {
        for (GetLocationTask r : this.getLocationTasks) {
            if (EqualsUtils.equalsCheckNull(r.getId(), id) && EqualsUtils.equalsCheckNull(r.getFragmentTag(), fragmentTag)) {
                //LogUtils.d(TAG, "getGetLocationTaskPrivate: " + getTaskLogId(id, fragmentTag) + " - found");
                return r;
            }
        }
        //LogUtils.d(TAG, "getGetLocationTaskPrivate: " + getTaskLogId(id, fragmentTag) + " - NOT found");
        return null;
    }


    //
    // STATIC METHODS
    //

    public static String getTaskLogId(String id, String fragmentTag) {
        return id + (fragmentTag != null ? (" (fragmentTag: " + fragmentTag) : "");
    }

    public static LocPoint getLastKnownLocPoint(Context context) {
        return getLastKnownLocPointEx(context).getLocPoint();
    }

    public static LocPointEx getLastKnownLocPointEx(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return getLastKnownLocPointEx(locationManager, PROVIDER_TYPE_NETWORK_GPS, LocPointEx.INVALID_AGE, LocPointEx.INVALID_ACCURACY);
    }

    public static LocPointEx getLastKnownLocPointEx(LocationManager locationManager, int providerType, long maxAge, float maxAccuracy) {
        LocPointEx ret = LocPointEx.INVALID;
        GlobalContextLib gct = GlobalContextLib.get();
        Context context = gct.getAndroidContext();
        final boolean hasFineLocAccess;

        if ((hasFineLocAccess = LocalUtils.checkSelfPermission(context, permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                || LocalUtils.checkSelfPermission(context, permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            if ((providerType & PROVIDER_TYPE_NETWORK) != 0) {
                LocPointEx locPointEx = LocPointEx.create(locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER));
                //LogUtils.d(TAG, "getLastKnownLocPointEx - from network: " + locPointEx.toString());
                if (canReplaceLocPointEx(locPointEx, ret, maxAge, maxAccuracy))
                    ret = locPointEx;
            }

            if ((providerType & PROVIDER_TYPE_GPS) != 0 && hasFineLocAccess) {
                LocPointEx locPointEx = LocPointEx.create(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
                //LogUtils.d(TAG, "getLastKnownLocPointEx - from GPS: " + locPointEx.toString());
                if (canReplaceLocPointEx(locPointEx, ret, maxAge, maxAccuracy))
                    ret = locPointEx;
            }
        }

        //LogUtils.d(TAG, "getLastKnownLocPointEx - returns: " + ret.toString());
        return ret;
    }

    public static boolean canReplaceLocPointEx(LocPointEx locPointEx, LocPointEx currentBestLocPointEx, long maxAge, float maxAccuracy) {
        return isAcceptableLocPointEx(locPointEx, maxAge, maxAccuracy)
                && isBetterLocPointEx(locPointEx, currentBestLocPointEx);
    }

    public static boolean isAcceptableLocPointEx(LocPointEx locPointEx, long maxAge, float maxAccuracy) {
        if (!locPointEx.isValid())
            return false; // nevalidni locPointEx v kazdem pripade neni akceptovatelny

        long age = Math.abs(System.currentTimeMillis() - locPointEx.getTime());
        boolean ret = age <= maxAge
                && locPointEx.getAccuracy() <= maxAccuracy;

        //LogUtils.d(TAG, "isAcceptableLocPointEx (maxAge: " + maxAge + ", maxAccuracy: " + maxAccuracy + " returns " + ret + " for: " + locPointEx.toString());
        return ret;
    }

    /** Determines whether one Location reading is better than the current Location fix
     * @param locPointEx  The new Location that you want to evaluate
     * @param currentBestLocPointEx  The current Location fix, to which you want to compare the new one
     */
    public static boolean isBetterLocPointEx(LocPointEx locPointEx, LocPointEx currentBestLocPointEx) {
        final int TWO_MINUTES = 1000 * 60 * 2;

        if (!currentBestLocPointEx.isValid()) {
            // A new location is always better than no location
            //LogUtils.d(TAG, "isBetterLocPointEx returns true: A new location is always better than no location; new: " + locPointEx.toString() + ", old: " + currentBestLocPointEx.toString());
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = locPointEx.getTime() - currentBestLocPointEx.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            //LogUtils.d(TAG, "isBetterLocPointEx returns true: If it's been more than two minutes since the current location, use the new location; new: " + locPointEx.toString() + ", old: " + currentBestLocPointEx.toString());
            return true;

        } else if (isSignificantlyOlder) {
            // If the new location is more than two minutes older, it must be worse
            //LogUtils.d(TAG, "isBetterLocPointEx returns false: If the new location is more than two minutes older, it must be worse; new: " + locPointEx.toString() + ", old: " + currentBestLocPointEx.toString());
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (locPointEx.getAccuracy() - currentBestLocPointEx.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = EqualsUtils.equalsCheckNull(locPointEx.getProvider(), currentBestLocPointEx.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            //LogUtils.d(TAG, "isBetterLocPointEx returns true: isMoreAccurate; new: " + locPointEx.toString() + ", old: " + currentBestLocPointEx.toString());
            return true;
        } else if (isNewer && !isLessAccurate) {
            //LogUtils.d(TAG, "isBetterLocPointEx returns true: isNewer && !isLessAccurate; new: " + locPointEx.toString() + ", old: " + currentBestLocPointEx.toString());
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            //LogUtils.d(TAG, "isBetterLocPointEx returns true: isNewer && !isSignificantlyLessAccurate && isFromSameProvider; new: " + locPointEx.toString() + ", old: " + currentBestLocPointEx.toString());
            return true;
        }
        return false;
    }


    //
    // INNER CLASSES
    //

    public interface IGetLocationTask {
        // Immutable
        String getId();
        Bundle getBundle();
        String getFragmentTag();
        int getProviders();
        long getTimeout();
        long getMaxAge();
        float getMaxAccuracy();
        int getMinDistanceBetweenPoints();
        long getRecommendedTimeBetweenFixes();

        // Stateful
        LocPointEx getCurrentBestLocPointEx();
        boolean getCallbacksEnabled();
        boolean getIsTimeout();
        boolean getIsStarted();
        int getProviderState();
    }

    public interface OnGetLocationListener {
        int REMOVE_TASK = 0;
        int CONTINUE_CALLBACKS_DISABLED = 1;
        int CONTINUE_CALLBACKS_ENABLED = 2;

        /*
         * Funkce vraci, jednu z hodnot REMOVE_TASK, CONTINUE_CALLBACKS_DISABLED, CONTINUE_CALLBACKS_ENABLED
         */
        int onGetLocationEvent(IGetLocationTask task, int callbackType);
    }

    private class GetLocationTask implements IGetLocationTask {
        private final String id;
        private final Bundle bundle;
        private final String fragmentTag;
        private final int providers;
        private final long timeout;
        private final long maxAge;
        private final float maxAccuracy;
        private final int minDistanceBetweenPoints;
        private final long recommendedTimeBetweenFixes;
        private final boolean forceFireOnStartListening; // Pri zapnuti detekce polohy vzdy dojde k okamzitemu callbacku typu CALLBACK_TYPE_LOC_POINT_EXT_CHANGED, i kdyz by nacachovana poloha nebyla pouzitelna

        // State
        private LocPointEx currentBestLocPointEx = LocPointEx.INVALID;
        private boolean callbacksEnabled;
        private boolean isStarted = false;
        private boolean isTimeout = false;
        private final HashMap<String, Integer> providersStates = new HashMap<String, Integer>();
        private final ArrayList<Integer> pendingCallbackTypes = new ArrayList<>(); // vzdy se uchovava jenom posledni callback daneho typu

        // Utils
        private final Context context;
        private final LocationManager locationManager;
        private final Handler handler = new Handler(Looper.getMainLooper());

        public GetLocationTask(Context context, String id, Bundle bundle, String fragmentTag, int providers, long timeout,
                               long maxAge, float maxAccuracy, int minDistanceBetweenPoints, long recommendedTimeBetweenFixes,
                               boolean forceFireOnStartListening, boolean callbacksEnabled)
        {
            this.id = id;
            this.bundle = bundle;
            this.fragmentTag = fragmentTag;
            this.providers = providers;
            this.timeout = timeout;
            this.maxAge = maxAge;
            this.maxAccuracy = maxAccuracy;
            this.minDistanceBetweenPoints = minDistanceBetweenPoints;
            this.recommendedTimeBetweenFixes = recommendedTimeBetweenFixes;
            this.forceFireOnStartListening = forceFireOnStartListening;

            this.callbacksEnabled = callbacksEnabled;

            this.context = context.getApplicationContext();
            this.locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        }

        public String getId() {
            return this.id;
        }

        public Bundle getBundle() {
            return this.bundle;
        }

        public String getFragmentTag() {
            return this.fragmentTag;
        }

        public int getProviders() {
            return this.providers;
        }

        public long getTimeout() {
            return this.timeout;
        }

        public long getMaxAge() {
            return this.maxAge;
        }

        public float getMaxAccuracy() {
            return this.maxAccuracy;
        }

        public int getMinDistanceBetweenPoints() {
            return this.minDistanceBetweenPoints;
        }

        public long getRecommendedTimeBetweenFixes() {
            return this.recommendedTimeBetweenFixes;
        }

        public LocPointEx getCurrentBestLocPointEx() {
            return this.currentBestLocPointEx;
        }

        public boolean getCallbacksEnabled() {
            return this.callbacksEnabled;
        }

        public boolean getIsTimeout() {
            return this.isTimeout;
        }

        public boolean getIsStarted() {
            return this.isStarted;
        }


        public int getProviderState() {
            int ret = PROVIDER_STATE_DISABLED;
            for (int s : providersStates.values()) {
                switch (s) {
                    case PROVIDER_STATE_WORKING: return PROVIDER_STATE_WORKING; // Pokud alespon jeden provider funguje, vracim rovnou PROVIDER_WORKING
                    case PROVIDER_STATE_DISABLED: break;
                    case PROVIDER_STATE_OUT_OF_SERVICE: ret = PROVIDER_STATE_OUT_OF_SERVICE; break; // OUT OF SERVICE ma prednost pred Disabled
                    default: throw new RuntimeException("Not implemented");
                }
            }
            return ret;
        }

        public String getLogId() {
            return getTaskLogId(id, fragmentTag);
        }



        //
        // SETTERS
        //

        void startListening() {
            if (!isStarted) {
                isStarted = true;
                LogUtils.d(TAG, "startListening: " + getLogId());

                restartTimeoutIfCan();
                clearProviderStates();
                pendingCallbackTypes.clear();

                currentBestLocPointEx = getLastKnownLocPointEx(locationManager, providers, maxAge, maxAccuracy);
                if (currentBestLocPointEx.isValid() || forceFireOnStartListening) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            onGetLocationEvent(GetLocationTask.this, CALLBACK_TYPE_LOC_POINT_EXT_CHANGED);
                        }
                    });
                }

                if ((providers & PROVIDER_TYPE_NETWORK) != 0)
                    startProvider(LocationManager.NETWORK_PROVIDER);
                if ((providers & PROVIDER_TYPE_GPS) != 0)
                    startProvider(LocationManager.GPS_PROVIDER);

                if (getProviderState() != PROVIDER_STATE_WORKING) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            onGetLocationEvent(GetLocationTask.this, CALLBACK_TYPE_PROVIDER_STATE_CHANGED);
                        }
                    });
                }
            }
        }

        void stopListening() {
            if (isStarted
                    && (LocalUtils.checkSelfPermission(context, permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                        || LocalUtils.checkSelfPermission(context, permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            {
                isStarted = false;

                LogUtils.d(TAG, "stopListening: " + getLogId());

                locationManager.removeUpdates(locationListener);

            }
            stopTimeout(); // radsi volam v kazdem pripade...
        }

        void setCallbacksEnabled(boolean callbacksEnabled) {
            if (this.callbacksEnabled != callbacksEnabled) {
                this.callbacksEnabled = callbacksEnabled;

                LogUtils.d(TAG, "setCallbacksEnabled: " + callbacksEnabled + ": " + getLogId());

                stopTimeout();

                if (callbacksEnabled) {
                    callCallbacksNowIfCan();
                    restartTimeoutIfCan();
                }
            }
        }

        void addPendingCallback(int callbackType) {
            //LogUtils.d(TAG, "addPendingCallback: " + callbackType + ": " + getLogId());
            pendingCallbackTypes.remove((Integer)callbackType);
            pendingCallbackTypes.add(callbackType);
        }

        void callCallbacksNowIfCan() {
            if (callbacksEnabled && pendingCallbackTypes.size() > 0) {
                //LogUtils.d(TAG, "callCallbacksNowIfCan: " + getLogId());

                // udelame si lokalni kopii a puvodni callbacky vymazeme jeste pred volanim jednotlivych onGetLocationEvent - ty by mohly planovat dalsi pending callbacky...
                ArrayList<Integer> currPendingCallbackTypes = new ArrayList<>(pendingCallbackTypes);
                pendingCallbackTypes.clear();

                for (Integer callbackType : currPendingCallbackTypes)
                    onGetLocationEvent(this, callbackType);
            }
        }



        //
        // CALLBACKS
        //

        private final LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                LocPointEx locPointEx = LocPointEx.create(location);
                //LogUtils.d(TAG, "onLocationChanged: " + getLogId() + ", " + locPointEx.toString());
                if (canReplaceLocPointEx(locPointEx, currentBestLocPointEx, maxAge, maxAccuracy)) {
                    currentBestLocPointEx = locPointEx;
                    onGetLocationEvent(GetLocationTask.this, CALLBACK_TYPE_LOC_POINT_EXT_CHANGED);
                }
            }

            @Override
            public void onProviderDisabled(String provider) {
                //LogUtils.d(TAG, "onProviderDisabled: " + getLogId() + ", " + provider);
                int oldState = getProviderState();
                providersStates.put(provider, PROVIDER_STATE_DISABLED);
                int newState = getProviderState();
                if (oldState != newState)
                    onGetLocationEvent(GetLocationTask.this, CALLBACK_TYPE_PROVIDER_STATE_CHANGED);
            }

            @Override
            public void onProviderEnabled(String provider) {
                //LogUtils.d(TAG, "onProviderEnabled: " + getLogId() + ", " + provider);
                if (providersStates.get(provider) == PROVIDER_STATE_DISABLED) {
                    int oldState = getProviderState();
                    providersStates.put(provider, PROVIDER_STATE_WORKING);
                    int newState = getProviderState();
                    if (oldState != newState)
                        onGetLocationEvent(GetLocationTask.this, CALLBACK_TYPE_PROVIDER_STATE_CHANGED);
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                //LogUtils.d(TAG, "onStatusChanged: " + getLogId() + ", " + provider + ", status: " + status);
                int oldState = getProviderState();
                switch (status) {
                    case LocationProvider.AVAILABLE: providersStates.put(provider, PROVIDER_STATE_WORKING); break;
                    case LocationProvider.OUT_OF_SERVICE: providersStates.put(provider, PROVIDER_STATE_OUT_OF_SERVICE); break;
                    default: break;
                }
                int newState = getProviderState();
                if (oldState != newState)
                    onGetLocationEvent(GetLocationTask.this, CALLBACK_TYPE_PROVIDER_STATE_CHANGED);
            }
        };

        private final Runnable timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                //LogUtils.d(TAG, "timeoutRunnable: run: " + getLogId());
                isTimeout = true;
                onGetLocationEvent(GetLocationTask.this, CALLBACK_TYPE_TIMEOUT);
            }
        };


        //
        // PRIVATE
        //

        private void startProvider(String providerName) {
            //LogUtils.d(TAG, "startProvider: " + getLogId() + ", " + providerName);
            boolean wasAdded = false;

            if (LocalUtils.checkSelfPermission(context, permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || (LocationManager.NETWORK_PROVIDER.equals(providerName) && LocalUtils.checkSelfPermission(context, permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED))
            {
                if (locationManager.getProvider(providerName) == null) {
                    LogUtils.d("GetLocationFragment", "provider: " + providerName + " does not exist on the device");
                }
                else {
                    try {
                        locationManager.requestLocationUpdates(providerName, recommendedTimeBetweenFixes, minDistanceBetweenPoints, locationListener);
                        wasAdded = true;
                    }
                    catch (Exception ex) {
                        LogUtils.e("GetLocationFragment", "Exc1: " + providerName, ex);
                    }
                }
            }
            else {
                LogUtils.d("GetLocationFragment", "cannot start provider - we don't have permissions");
            }

            if (!wasAdded) {
                providersStates.put(providerName, PROVIDER_STATE_DISABLED);
            }
        }

        private void clearProviderStates() {
            //LogUtils.d(TAG, "clearProviderStates: " + getLogId());
            this.providersStates.clear();
            if ((providers & PROVIDER_TYPE_NETWORK) != 0)
                this.providersStates.put(LocationManager.NETWORK_PROVIDER, PROVIDER_STATE_WORKING); // locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ? PROVIDER_WORKING : PROVIDER_DISABLED);
            if ((providers & PROVIDER_TYPE_GPS) != 0)
                this.providersStates.put(LocationManager.GPS_PROVIDER, PROVIDER_STATE_WORKING); // locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ? PROVIDER_WORKING : PROVIDER_DISABLED);
        }

        private void restartTimeoutIfCan() {
            //LogUtils.d(TAG, "restartTimeoutIfCan: " + getLogId());
            stopTimeout();
            if (timeout > 0 && callbacksEnabled)
                handler.postDelayed(timeoutRunnable, timeout);
        }

        private void stopTimeout() {
            //LogUtils.d(TAG, "stopTimeout: " + getLogId());
            isTimeout = false;
            handler.removeCallbacks(timeoutRunnable);
        }
    }


}
