package com.labakshaya.service;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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


    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) {
        Log.d(TAG, "In execute action:"+action );
        Activity activity = this.cordova.getActivity();
        Boolean result = false;
        updateServiceIntent = new Intent(activity, LocationWatchService.class);

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
                callbackContext.error( e.getMessage());
            }
        } else if (ACTION_SET_CONFIG.equalsIgnoreCase(action)) {
            result = true;
            // TODO reconfigure Service
            callbackContext.success();
        }else if (ACTION_LOCATION.equalsIgnoreCase(action)) {
            globalCallbackContext = callbackContext;

            if(locationManager == null) {
        	Log.d(TAG, "locationManager init" );
                locationManager = (LocationManager) this.cordova.getActivity().getBaseContext()
                        .getSystemService(Context.LOCATION_SERVICE);

        	Log.d(TAG, "locationManager : "+locationManager);
            }

        Log.d(TAG, "In location fetch" );
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
            PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
            pluginResult.setKeepCallback(true);
            globalCallbackContext.sendPluginResult(pluginResult);



        }

        return result;
    }

    /**
     * Override method in CordovaPlugin.
     * Checks to see if it should turn off
     */
    public void onDestroy() {
        Activity activity = this.cordova.getActivity();

        if(isEnabled && stopOnTerminate.equalsIgnoreCase("true")) {
            activity.stopService(updateServiceIntent);
        }
    }

    public void onLocationChanged(Location location) {
        Log.d(TAG, "- onLocationChanged: " + location.getLatitude() + "," + location.getLongitude() + ", accuracy: " + location.getAccuracy());

        JSONObject r = new JSONObject();
        try {
            r.put("altitude", location.getAltitude());
            r.put("latitude", location.getLatitude());
        }catch(Exception e){}

        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, r);
        pluginResult.setKeepCallback(false);
        globalCallbackContext.sendPluginResult(pluginResult);
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
