package com.circlegate.liban.base;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.LocalBroadcastManager;

public class BaseBroadcastReceivers {
    public static abstract class BaseBroadcastReceiverCommon extends BroadcastReceiver {
        private final IntentFilter intentFilter;
        private boolean registered = false;

        public BaseBroadcastReceiverCommon(String action) {
            this(new IntentFilter(action));
        }

        public BaseBroadcastReceiverCommon(IntentFilter intentFilter) {
            this.intentFilter = intentFilter;
        }

        public IntentFilter getIntentFilter() {
            return this.intentFilter;
        }

        public boolean isRegistered() {
            return this.registered;
        }

        protected boolean register(Context context) {
            if (!registered) {
                doRegister(context);
                registered = true;
                return true;
            }
            else
                return false;
        }

        public boolean unregister(Context context) {
            if (registered) {
                doUnregister(context);
                registered = false;
                return true;
            }
            else
                return false;
        }

        @Override
        public final void onReceive(Context context, Intent intent) {
            if (registered)
                onReceiveRegistered(context, intent);
        }

        protected abstract void doRegister(Context context);
        protected abstract void doUnregister(Context context);
        protected abstract void onReceiveRegistered(Context context, Intent intent);
    }


    public static abstract class BaseLocalReceiverProt extends BaseBroadcastReceiverCommon {
        public BaseLocalReceiverProt(String action) {
            super(action);
        }

        public BaseLocalReceiverProt(IntentFilter intentFilter) {
            super(intentFilter);
        }

        @Override
        protected void doRegister(Context context) {
            LocalBroadcastManager.getInstance(context).registerReceiver(this, getIntentFilter());
        }

        @Override
        protected void doUnregister(Context context) {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
        }

        public static void sendBroadcast(Context context, Intent intent) {
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }
    }

    public static abstract class BaseLocalReceiver extends BaseLocalReceiverProt {
        public BaseLocalReceiver(String action) {
            super(action);
        }

        public BaseLocalReceiver(IntentFilter intentFilter) {
            super(intentFilter);
        }

        @Override
        public boolean register(Context context) {
            return super.register(context);
        }
    }


    public static abstract class BaseGlobalReceiverProt extends BaseBroadcastReceiverCommon {
        public BaseGlobalReceiverProt(String action) {
            super(action);
        }

        public BaseGlobalReceiverProt(IntentFilter intentFilter) {
            super(intentFilter);
        }

        @Override
        protected void doRegister(Context context) {
            context.registerReceiver(this, getIntentFilter());
        }

        @Override
        protected void doUnregister(Context context) {
            context.unregisterReceiver(this);
        }

        public static void sendBroadcast(Context context, Intent intent) {
            context.sendBroadcast(intent);
        }
    }

    public static abstract class BaseGlobalReceiver extends BaseGlobalReceiverProt {
        public BaseGlobalReceiver(String action) {
            super(action);
        }

        public BaseGlobalReceiver(IntentFilter intentFilter) {
            super(intentFilter);
        }

        @Override
        public boolean register(Context context) {
            return super.register(context);
        }
    }


    public static abstract class OnMinuteChangeReceiver extends BaseGlobalReceiverProt {
        public OnMinuteChangeReceiver() {
            super(Intent.ACTION_TIME_TICK);
        }

        public boolean register(Context context, boolean fireCallbackNow) {
            if (super.register(context)) {
                if (fireCallbackNow)
                    onMinuteChange();
                return true;
            }
            else
                return false;
        }

        @Override
        public final void onReceiveRegistered(Context context, Intent intent) {
            onMinuteChange();
        }

        public abstract void onMinuteChange();
    }

    public static abstract class OnMinuteChangeOrConnectedReceiver extends BaseGlobalReceiverProt {
        public OnMinuteChangeOrConnectedReceiver() {
            super(Intent.ACTION_TIME_TICK);
            getIntentFilter().addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        }

        public boolean register(Context context, boolean fireCallbackNow) {
            if (super.register(context)) {
                if (fireCallbackNow)
                    onMinuteChangeOrConnected(true);
                return true;
            }
            else
                return false;
        }

        @Override
        public final void onReceiveRegistered(Context context, Intent intent) {
            if (intent != null && intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo info = manager.getActiveNetworkInfo();
                if (info != null && info.isConnected()) {
                    onMinuteChangeOrConnected(false);
                }
            }
            else
                onMinuteChangeOrConnected(true);
        }

        public abstract void onMinuteChangeOrConnected(boolean isMinuteChange);
    }
}
