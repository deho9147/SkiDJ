package e.dholland.ski_j;

import android.Manifest;
import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

public class ForegroundService extends Service implements LocationListener {
    Context mainContext;
    private Resources resources;
    private Notification notification;
    private AudioManager audioManager;
    private LocationManager locationManager;

    SharedPreferences sharedPreferences;


    private double currentSpeed;

    private int minVolume;
    private int maxVolume;
    private float maxVelocity;
    private float minVelocity;

    private boolean liftBool;
    private boolean liftPrimed;
    private int refreshRate;

    private boolean onLift;
    private double currentAltitude;
    private double pastAltitudeLongTerm;
    private double pastSpeed;
    private double pastAltitudeShortTerm;
    private double speedBuffer;

    private boolean stopUpdates;

    private int volumeChangeSpeed;

    private float speedSum;
    private float avgSpeed;
    private float pastAvgSpeed;

    private int liftCheckCounter;
    private int counterThreshold;


    private int currentVolume;
    private int approachVolume;

    public void onCreate(){
        mainContext = getApplicationContext();
        resources = mainContext.getResources();
        stopUpdates = false;
        onLift = false;
        liftPrimed = false;
        pastAltitudeShortTerm = 0.0;
        pastAltitudeLongTerm = 0.0;
        speedSum = 0;
        pastSpeed = 0.0;
        currentAltitude = 120;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mainContext);
        refreshRate = sharedPreferences.getInt(resources.getString(R.string.refreshRateKey),1);


        liftCheckCounter = 0;
        counterThreshold = Math.round(5/refreshRate);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            startMyOwnForeground();
        }
        else {
            startForeground(1, new Notification());
        }
        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        if (ActivityCompat.checkSelfPermission(mainContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            System.out.println("ServiceFailedPermission");
        } else {
            System.out.println("ServiceAcceptedPermission");
            locationManager = (LocationManager) mainContext.getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, (long) refreshRate *1000, 0, this);
            this.onLocationChanged(null);
        }

    }

    @Nullable

    public int onStartCommand(Intent intent, int Flags, int startID){

        System.out.println((intent.getExtras().getInt(resources.getString(R.string.lowSpeedVolumeKey))));
        if (intent.getExtras().get(resources.getString(R.string.lowSpeedVolumeKey))!= null){
            minVolume = (int) intent.getExtras().get(resources.getString(R.string.lowSpeedVolumeKey));
        }
        if (intent.getExtras().get(resources.getString(R.string.highSpeedVolumeKey)) != null){
            maxVolume = (int) intent.getExtras().get(resources.getString(R.string.highSpeedVolumeKey));
        }
        if (intent.getExtras().get(resources.getString(R.string.highVelKey)) != null){
            maxVelocity = (float) intent.getExtras().get(resources.getString(R.string.highVelKey));
        }
        if (intent.getExtras().get(resources.getString(R.string.lowVelKey)) != null){
            minVelocity = (float) intent.getExtras().get(resources.getString(R.string.lowVelKey));
        }
        if (intent.getExtras().get(resources.getString(R.string.liftCheckKey)) != null){
            liftBool = (boolean) intent.getExtras().get(resources.getString(R.string.liftCheckKey));
        }

        if (intent.getExtras().get(resources.getString(R.string.stopUpdatesKey)) != null) {
            stopUpdates = (boolean) intent.getExtras().get(resources.getString(R.string.stopUpdatesKey));
        }


        if (maxVolume-minVolume != 0){
            speedBuffer = (maxVelocity-minVelocity)/(2*(maxVolume-minVolume));
        }
        else {
            speedBuffer = maxVolume*.1;
        }

        //possible update volume change speed so its smooth
        //volumeChangeSpeed = (int) Math.round(((audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)*.1) - .1));
        volumeChangeSpeed = 100;

        return START_STICKY;
    }

    private void startMyOwnForeground(){
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        String NOTIFICATION_CHANNEL_ID = "SoundSpeedService";
        String channelName = "My Sound Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.snowflake_purple_24dp)
                .setContentTitle("Ski-J is changing the music volume")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setColorized(true)
                .setColor(resources.getColor(R.color.dark_accentColor))
                .setContentIntent(pendingIntent)
                .build();
        startForeground(2, notification);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        locationManager.removeUpdates(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {
        if (!stopUpdates) {
            if (location == null) {
                currentSpeed = -1;
            } else {
                float nCurrentSpeed = location.getSpeed();
                currentSpeed = nCurrentSpeed;

                //maybe move this to its own function. eventually.
                if (liftBool == true) {
                    //less than 3 mph and altitude increasing
                    //Short term check hopefully means getting on the lift will remain low volume rather than being high volume for 5 seconds until the long term check comes in
                    // the low speed zone should help it not being marked true wrongly.
                    if (pastSpeed < 3  && currentAltitude > pastAltitudeShortTerm + refreshRate/4){
                        liftPrimed = true;
                        approachVolume = minVolume;
                    } else if (liftPrimed = true && currentAltitude > pastAltitudeShortTerm + refreshRate/4){
                        //this could be an or on the first one but I think this is more clear
                        liftPrimed = true;
                        approachVolume = minVolume;
                    }else{
                        liftPrimed = false;
                    }
                    pastAltitudeShortTerm = currentAltitude;

                    if (liftCheckCounter >= counterThreshold){
                        avgSpeed = speedSum/counterThreshold;
                        currentAltitude = location.getAltitude();
                        //Extending the time on lift check should help deal with the parabolic path between lift posts.

                        if ((onLift==true)&&(avgSpeed >= (pastAvgSpeed*.8)) && (avgSpeed <= (pastAvgSpeed*1.2))){
                            //.2 buffer zone may be too much and result in false positives.
                            //this should try and capture going down while still on the lift.
                        }
                        else if( currentAltitude > (pastAltitudeLongTerm + 1)){
                            //going up sets on lift to true
                            // this is over 5 seconds though so you most likely wont go up over 5 seconds while skiing.
                            onLift = true;
                        }
                        else{
                            onLift = false;
                        }
                        pastAltitudeLongTerm = currentAltitude;
                        liftCheckCounter = 0;
                    }
                    speedSum = speedSum + nCurrentSpeed;
                    liftCheckCounter ++;
                } else {
                    onLift = false;
                }
                if (onLift == false && liftPrimed == false) {
                    if ((currentSpeed < pastSpeed + speedBuffer) || currentSpeed > pastSpeed - speedBuffer) {
                        if (currentSpeed >= maxVelocity) {
                            approachVolume = maxVolume;
                        } else if (currentSpeed <= minVelocity) {
                            approachVolume = minVolume;
                        } else {
                            approachVolume = (int) Math.round(minVolume + ((maxVolume - minVolume) * (currentSpeed - minVelocity) / (maxVelocity - minVelocity)));
                        }
                        pastSpeed = currentSpeed;
                    }
                }
                else{
                    approachVolume = minVolume;
                }
                int volumeDifference = approachVolume - currentVolume;
                if (volumeDifference < volumeChangeSpeed || volumeDifference > -volumeChangeSpeed) {
                    currentVolume = approachVolume;
                } else if (volumeDifference > volumeChangeSpeed) {
                    currentVolume = currentVolume + volumeChangeSpeed;
                } else {
                    currentVolume = currentVolume - volumeChangeSpeed;
                }
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, approachVolume, 0);
            }
        }
    }


    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}

