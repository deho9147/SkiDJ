package e.dholland.ski_j;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.AudioManager;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.widget.SeekBar;
import androidx.appcompat.app.AppCompatActivity;

public class SeekBarVolume extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {
    Context mainContext;
    SeekBar sbHigh;
    SeekBar sbLow;
    AudioManager am;

    SharedPreferences sharedPref;
    SharedPreferences.Editor sharedPrefEditor;
    Resources resources;

    int maxVolume;
    int preAppVolume;
    int lowSpeedVolume;
    int highSpeedVolume;
    int currentVolume;


    public SeekBarVolume(Context inputContext, SeekBar sbHighinput, SeekBar sbLowinput, AudioManager aminput) {
        mainContext = inputContext;
        sbHigh = sbHighinput;
        sbLow = sbLowinput;
        am = aminput;

        resources = mainContext.getResources();
        sharedPref = PreferenceManager.getDefaultSharedPreferences(mainContext);
        sharedPrefEditor = sharedPref.edit();

        maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        preAppVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);

        int savedHighVolume = sharedPref.getInt(resources.getString(R.string.highSpeedVolumeKey), 7);
        int savedLowVolume = sharedPref.getInt(resources.getString(R.string.lowSpeedVolumeKey), 0);

        highSpeedVolume = savedHighVolume;
        lowSpeedVolume = savedLowVolume;

        sbHigh.setMax(maxVolume);
        sbHigh.setProgress(savedHighVolume);
        sbHigh.setOnSeekBarChangeListener(this);

        sbLow.setMax(maxVolume);
        sbLow.setProgress(savedLowVolume);
        sbLow.setOnSeekBarChangeListener(this);
    }

    ;

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (seekBar == sbHigh) {
            highSpeedVolume = progress;
            am.setStreamVolume(AudioManager.STREAM_MUSIC, highSpeedVolume, 0);
        } else if (seekBar == sbLow) {
            lowSpeedVolume = progress;
            am.setStreamVolume(AudioManager.STREAM_MUSIC, lowSpeedVolume, 0);
        }

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        Intent serviceIntent = new Intent(mainContext, ForegroundService.class);
        serviceIntent.putExtra(resources.getString(R.string.stopUpdatesKey), true);
        mainContext.startService(serviceIntent);
        currentVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        System.out.println("OnStopTrackingTouch");

        startService();

    }

    public int getHighSpeedVolume() {

        return highSpeedVolume;
    }

    public int getLowSpeedVolume() {
        return lowSpeedVolume;
    }

    public void startService() {
        Intent serviceIntent = new Intent(mainContext, ForegroundService.class);
        System.out.println("AfterServiceIntent");
        serviceIntent.putExtra(resources.getString(R.string.lowSpeedVolumeKey), lowSpeedVolume);
        serviceIntent.putExtra(resources.getString(R.string.highSpeedVolumeKey), highSpeedVolume);
        serviceIntent.putExtra(resources.getString(R.string.stopUpdatesKey), false);
        System.out.println("Added extras");
        mainContext.startService(serviceIntent);
        System.out.println("service Called?");
    }
}
