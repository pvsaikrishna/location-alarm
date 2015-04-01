package com.labakshaya.service;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

/**
 * Created by saikr on 3/30/2015.
 */
public class LocationWatchService extends Service implements LocationListener {

    private static final String TAG = "LocationWatchService";

    private static final String LOCATION_POLL_ACTION = "com.labakshaya.locationalarm.LOCATION_POLL_ACTION";
    private static final long DEFAULT_TIMEOUT = 5 * 1000 * 60;    // 5 minutes.

    private PowerManager.WakeLock wakeLock;
    private Location lastLocation;
    private long lastUpdateTime = 0l;


    private Double latitude;
    private Double longitude;
    private String distanceToAlarm;
    private Integer locationTimeout = 30;
    private Boolean isDebugging;
    private String notificationTitle = "Background checking";
    private String notificationText = "ENABLED";
    private Boolean stopOnTerminate = true;

    private LocationManager locationManager;
    private AlarmManager alarmManager;
    private ConnectivityManager connectivityManager;
    private NotificationManager notificationManager;
    public static TelephonyManager telephonyManager = null;
    private ToneGenerator toneGenerator;

    private Criteria criteria;


    private PendingIntent locationPollAlarmPI;

    private int locationProviderCounter = 0;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "OnCreate");

        locationManager         = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        alarmManager            = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        toneGenerator           = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
        connectivityManager     = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        notificationManager     = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);
        telephonyManager        = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        locationPollAlarmPI   = PendingIntent.getBroadcast(this, 0, new Intent(LOCATION_POLL_ACTION), 0);
        registerReceiver(locationPollAlarmReceiver, new IntentFilter(LOCATION_POLL_ACTION));

        PowerManager pm         = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

        wakeLock.acquire();


        // Location criteria
        criteria = new Criteria();
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setSpeedRequired(true);
        criteria.setCostAllowed(true);


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Received start id " + startId + ": " + intent);

        latitude = Double.parseDouble( intent.getStringExtra("latitude"));
        longitude = Double.parseDouble( intent.getStringExtra("longitude"));
        distanceToAlarm = intent.getStringExtra("distanceToAlarm");

        isDebugging = Boolean.parseBoolean(intent.getStringExtra("isDebugging"));
        notificationTitle = intent.getStringExtra("notificationTitle");
        notificationText = intent.getStringExtra("notificationText");
        stopOnTerminate = Boolean.parseBoolean(intent.getStringExtra("stopOnTerminate"));

        raiseNotification(notificationTitle, notificationTitle, startId);

        fetchLocationsFromVariousSources();

        //We want this service to continue running until it is explicitly stopped
        return START_REDELIVER_INTENT;
    }

    private void raiseNotification(String notificationTitle, String notificationText, int notificationId){
        // Build a Notification required for running service in foreground.
        Intent main = new Intent(this, LocationAlarmInterface.class);
        main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, main,  PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentTitle(notificationTitle);
        builder.setContentText(notificationText);
        builder.setSmallIcon(android.R.drawable.ic_menu_mylocation);
        builder.setContentIntent(pendingIntent);
        Notification notification;
        if (android.os.Build.VERSION.SDK_INT >= 16) {
            notification = buildForegroundNotification(builder);
        } else {
            notification = buildForegroundNotificationCompat(builder);
        }
        notification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_FOREGROUND_SERVICE | Notification.FLAG_NO_CLEAR;
        startForeground(notificationId, notification);

    }

    private void fetchLocationsFromVariousSources(){

        this.locationProviderCounter = 0;

        locationManager.removeUpdates(this);

        criteria.setAccuracy(Criteria.ACCURACY_FINE);
      //  criteria.setHorizontalAccuracy(translateDesiredAccuracy(desiredAccuracy));
        criteria.setPowerRequirement(Criteria.POWER_HIGH);

        List<String> matchingProviders = locationManager.getAllProviders();
        for (String provider: matchingProviders) {
            if (provider != LocationManager.PASSIVE_PROVIDER) {
                this.locationProviderCounter++;
                locationManager.requestLocationUpdates(provider, 0, 0, this);
            }
        }

    }


    @TargetApi(16)
    private Notification buildForegroundNotification(Notification.Builder builder) {
        return builder.build();
    }

    @SuppressWarnings("deprecation")
    @TargetApi(15)
    private Notification buildForegroundNotificationCompat(Notification.Builder builder) {
        return builder.getNotification();
    }

    @Override
    public boolean stopService(Intent intent) {
        Log.i(TAG, "- Received stop: " + intent);
        cleanUp();
        if (isDebugging) {
            Toast.makeText(this, "Background location tracking stopped", Toast.LENGTH_SHORT).show();
        }
        return super.stopService(intent);
    }

    public void resetStationaryAlarm(long timeOut) {
        alarmManager.cancel(locationPollAlarmPI);
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + timeOut, locationPollAlarmPI); // Millisec * Second * Minute
    }

    private void cleanUp() {

    }

    public void onLocationChanged(Location location) {
        Log.d(TAG, "- onLocationChanged: " + location.getLatitude() + "," + location.getLongitude() + ", accuracy: " + location.getAccuracy());


        //compute the time interval for polling next location

        raiseNotification("LocationChanged", location.getLatitude() + "," + location.getLongitude() + ", accuracy: " + location.getAccuracy(), 1);

        resetStationaryAlarm(DEFAULT_TIMEOUT);
    }

        @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        Log.i(TAG, "OnBind" + intent);
        return null;
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

    /**
     * Broadcast receiver which polls locations using available providers when triggered
     */
    private BroadcastReceiver locationPollAlarmReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "locationPollAlarmReceiver");

            fetchLocationsFromVariousSources();
        }
    };

}
