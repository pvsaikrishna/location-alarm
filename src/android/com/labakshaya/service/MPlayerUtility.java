package com.labakshaya.service;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;

/**
 * Created by saikr on 4/18/2015.
 */
public class MPlayerUtility {

    public static MPlayerUtility instance = null;

    private MediaPlayer mPlayer;

    protected  MPlayerUtility()
    {

    }

    public static  MPlayerUtility getInstance(){
        if(instance == null){
            instance = new MPlayerUtility();
        }
        return instance;
    }

    public void startRingtone(Context context, Uri toneUri){
        try {
                if (toneUri != null) {
                    mPlayer.setDataSource(context, toneUri);
                    mPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                    mPlayer.setLooping(true);
                    mPlayer.prepare();
                    mPlayer.start();
                }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void stopRingtone(){

        mPlayer.stop();
    }

}
