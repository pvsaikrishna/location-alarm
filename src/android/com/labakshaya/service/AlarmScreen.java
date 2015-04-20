package com.labakshaya.service;

/**
 * Created by saikr on 4/18/2015.
 */
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class AlarmScreen extends Activity {

    public final String TAG = this.getClass().getSimpleName();

    private WakeLock mWakeLock;
    private MediaPlayer mPlayer;
    private NotificationManager notificationManager;

    private int notificationId;

    private static final int WAKELOCK_TIMEOUT = 60 * 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        notificationId = 100;

        RelativeLayout relativeLayout = new RelativeLayout(this);

        TextView alarmScreenTitle = new TextView(this);
        alarmScreenTitle.setText("Nearing destination !!");
        alarmScreenTitle.setTextSize(38);
        relativeLayout.addView(alarmScreenTitle);

        Button dismissButton = new Button(this);
        dismissButton.setText("Dismiss");
        dismissButton.setTextSize(38);
        dismissButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View view) {
                mPlayer.stop();
                notificationManager.cancel(notificationId);
                finish();
            }
        });
        RelativeLayout.LayoutParams buttonParams =
                new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);

        buttonParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        buttonParams.addRule(RelativeLayout.CENTER_VERTICAL);

        relativeLayout.addView(dismissButton, buttonParams);

        //Setup layout
        this.setContentView(relativeLayout);

        notificationManager     = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);


        //Play alarm tone
        mPlayer = new MediaPlayer();
        try {
            Uri defaultRingtoneUri = RingtoneManager.getActualDefaultRingtoneUri(this.getApplicationContext(), RingtoneManager.TYPE_RINGTONE);
            String tone = defaultRingtoneUri.toString();
            if (tone != null && !tone.equals("")) {
                Uri toneUri = Uri.parse(tone);
                raiseNotification();
                if (toneUri != null) {
                    mPlayer.setDataSource(this, toneUri);
                    mPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                    mPlayer.setLooping(true);
                    mPlayer.prepare();
                    mPlayer.start();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Ensure wakelock release
        Runnable releaseWakelock = new Runnable() {

            @Override
            public void run() {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

                if (mWakeLock != null && mWakeLock.isHeld()) {
                    mWakeLock.release();
                }
            }
        };

        new Handler().postDelayed(releaseWakelock, WAKELOCK_TIMEOUT);
    }
    @Override
    public void onBackPressed()
    {
       // super.onBackPressed();
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onResume() {
        super.onResume();

        // Set the window to keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        // Acquire wakelock
        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        if (mWakeLock == null) {
            mWakeLock = pm.newWakeLock((PowerManager.FULL_WAKE_LOCK | PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), TAG);
        }

        if (!mWakeLock.isHeld()) {
            mWakeLock.acquire();
            Log.i(TAG, "Wakelock aquired!!");
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


    private void raiseNotification(){

        Log.d(TAG, "in raiseNotification ");

        // Build a Notification required for running service in foreground.
        Intent main = new Intent(this, AlarmScreen.class);
        main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, main,  PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentTitle("TrainAlarm");
        builder.setContentText("Click here to dismiss the alarm.");
        //builder.setSmallIcon(android.R.drawable.ic_menu_mylocation);
        builder.setSmallIcon(getApplicationContext().getApplicationInfo().icon);
        builder.setContentIntent(pendingIntent);

        Notification notification;
        if (android.os.Build.VERSION.SDK_INT >= 16) {
            notification = buildForegroundNotification(builder);
        } else {
            notification = buildForegroundNotificationCompat(builder);
        }
        notification.flags |= Notification.FLAG_ONGOING_EVENT |  Notification.FLAG_NO_CLEAR;

        notificationManager.notify(notificationId, notification);
        Log.d(TAG, "notification raised  ");
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    private BroadcastReceiver stopAlarmBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "locationPollAlarmReceiver");


        }
    };
}
