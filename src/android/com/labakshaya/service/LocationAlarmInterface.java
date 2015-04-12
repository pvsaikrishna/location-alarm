package com.labakshaya.service;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

public class LocationAlarmInterface extends CordovaPlugin implements LocationListener {
    private static final String TAG = "LocationAlarmInterface";

    public static final String ACTION_START = "start";
    public static final String ACTION_STOP = "stop";
    public static final String ACTION_CONFIGURE = "configure";
    public static final String ACTION_SET_CONFIG = "setConfig";
    public static final String ACTION_LOCATION = "location";

    private Intent updateServiceIntent;

    private Boolean isEnabled = false;

    private String latitude;
    private String longitude;
    private String distanceToAlarm;
    private String isDebugging = "false";
    private String notificationTitle = "Background tracking";
    private String notificationText = "ENABLED";
    private String stopOnTerminate = "false";

    private CallbackContext globalCallbackContext = null;
    private LocationManager locationManager = null;
    private AlarmManager alarmManager;

    private PendingIntent networkProviderTimeOutPI;
    private PendingIntent gpsProviderTimeOutPI;

    private static final String INTERFACE_NETWORK_TIMEOUT = "com.labakshaya.locationalarm.interface.NETWORK_TIMEOUT";
    private static final String INTERFACE_GPS_TIMEOUT = "com.labakshaya.locationalarm.interface.GPS_TIMEOUT";


    private boolean isLocationReturned = false;

    private long networkTimeout = 60 * 1000; //60secs
    private long gpsTimeout = 60 * 1000; //60secs

    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) {
        Log.d(TAG, "In execute action:" + action);
        Activity activity = this.cordova.getActivity();
        Boolean result = false;
        updateServiceIntent = new Intent(activity, LocationWatchService.class);
        this.globalCallbackContext = callbackContext;
        if (ACTION_START.equalsIgnoreCase(action) && !isEnabled) {
            result = true;
            if (latitude == null || longitude == null || distanceToAlarm == null) {
                callbackContext.error("Call configure before calling start");
            } else {
                callbackContext.success();
                //[latitude, longitude, distanceToAlarm, debug, notificationTitle, notificationText, stopOnTerminate]

                updateServiceIntent.putExtra("latitude", latitude);
                updateServiceIntent.putExtra("longitude", longitude);
                updateServiceIntent.putExtra("distanceToAlarm", distanceToAlarm);
                updateServiceIntent.putExtra("isDebugging", isDebugging);
                updateServiceIntent.putExtra("notificationTitle", notificationTitle);
                updateServiceIntent.putExtra("notificationText", notificationText);
                updateServiceIntent.putExtra("stopOnTerminate", stopOnTerminate);

                activity.startService(updateServiceIntent);
                isEnabled = true;
            }
        } else if (ACTION_STOP.equalsIgnoreCase(action)) {
            isEnabled = false;
            result = true;
            activity.stopService(updateServiceIntent);
            callbackContext.success();
        } else if (ACTION_CONFIGURE.equalsIgnoreCase(action)) {
            result = true;
            try {

                this.latitude = data.getString(0);
                this.longitude = data.getString(1);
                this.distanceToAlarm = data.getString(2);
                this.isDebugging = data.getString(3);
                this.notificationTitle = data.getString(4);
                this.notificationText = data.getString(5);
                this.stopOnTerminate = data.getString(6);

            } catch (Exception e) {
                callbackContext.error(e.getMessage());
            }
        } else if (ACTION_SET_CONFIG.equalsIgnoreCase(action)) {
            result = true;
            // TODO reconfigure Service
            callbackContext.success();
        } else if (ACTION_LOCATION.equalsIgnoreCase(action)) {
            result = true;

            isLocationReturned = false;

            Context context = this.cordova.getActivity().getBaseContext();

            if (locationManager == null) {
                Log.d(TAG, "locationManager init");
                locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

                Log.d(TAG, "locationManager : " + locationManager);
            }
            if (alarmManager == null) {
                alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            }

            if (networkProviderTimeOutPI == null) {
                networkProviderTimeOutPI = PendingIntent.getBroadcast(context, 0, new Intent(INTERFACE_NETWORK_TIMEOUT), 0);
                context.registerReceiver(networkTimeOutReceiver, new IntentFilter(INTERFACE_NETWORK_TIMEOUT));

            }
            if (gpsProviderTimeOutPI == null) {
                gpsProviderTimeOutPI = PendingIntent.getBroadcast(context, 0, new Intent(INTERFACE_GPS_TIMEOUT), 0);
                context.registerReceiver(gpsTimeOutReceiver, new IntentFilter(INTERFACE_GPS_TIMEOUT));
            }


            Log.d(TAG, "In location fetch");
            JSONObject r = new JSONObject();

            fetchLocationUsingNetwork();



            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, r);
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
            Log.d(TAG, "callbackcontext :  " + callbackContext.getCallbackId() + " - " + callbackContext);
        }

        return result;
    }

    private void fetchLocationUsingNetwork() {

        Log.i(TAG, "fetching location using network");

        locationManager.removeUpdates(this);

        alarmManager.cancel(networkProviderTimeOutPI);
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + networkTimeout, networkProviderTimeOutPI); // Millisec * Second * Minute


       locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
    }

    private void fetchLocationUsingGPS() {

        Log.i(TAG, "fetching location using gps");


        locationManager.removeUpdates(this);

        alarmManager.cancel(gpsProviderTimeOutPI);
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + gpsTimeout, gpsProviderTimeOutPI); // Millisec * Second * Minute

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
    }

    private BroadcastReceiver networkTimeOutReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "networkTimeOutReceiver");

            if (!isLocationReturned) {
                //network couldn't fetch location so use gps.
                fetchLocationUsingGPS();
            }
        }
    };

    private BroadcastReceiver gpsTimeOutReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "gpsTimeOutReceiver");
            if (!isLocationReturned) {
                //even gps couldn't fetch the location
                //throw error

                JSONObject r = new JSONObject();
                try {
                    r.put("error", "Unable to fetch location. Timed out, try again.");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, r);
                pluginResult.setKeepCallback(false);
                Log.d(TAG, "callbackcontext3 :  " + globalCallbackContext.getCallbackId() + " - " + globalCallbackContext);
                globalCallbackContext.sendPluginResult(pluginResult);

            }

        }
    };

    /**
     * Override method in CordovaPlugin.
     * Checks to see if it should turn off
     */
    public void onDestroy() {
        Activity activity = this.cordova.getActivity();

        if (isEnabled && stopOnTerminate.equalsIgnoreCase("true")) {
            activity.stopService(updateServiceIntent);
        }
    }

    public void onLocationChanged(Location location) {
        Log.d(TAG, "- onLocationChanged: " + location.getLatitude() + "," + location.getLongitude() + ", accuracy: " + location.getAccuracy()+" , fix : "+location.getProvider());


        JSONObject r = new JSONObject();
        try {
            r.put("latitude", location.getLatitude());
            r.put("longitude", location.getLongitude());
        } catch (Exception e) {
        }


        if (this.globalCallbackContext != null) {
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, r);
            pluginResult.setKeepCallback(false);
            Log.d(TAG, "callbackcontext2 :  " + this.globalCallbackContext.getCallbackId() + " - " + this.globalCallbackContext);
            this.globalCallbackContext.sendPluginResult(pluginResult);
            isLocationReturned = true;
            this.globalCallbackContext = null;
        }

        locationManager.removeUpdates(this);

        //compute the time interval for polling next location

    }

    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub
        Log.d(TAG, "- onProviderDisabled: " + provider);
    }

    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub
        Log.d(TAG, "- onProviderEnabled: " + provider);
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub
        Log.d(TAG, "- onStatusChanged: " + provider + ", status: " + status);
    }
}
