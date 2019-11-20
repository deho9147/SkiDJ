package e.dholland.ski_j;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.AudioManager;
import android.preference.PreferenceManager;
import android.widget.SeekBar;
import androidx.appcompat.app.AppCompatActivity;

public class SeekBarVolume extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {
    private Context mainContext;
    private SeekBar sbHigh;
    private SeekBar sbLow;
    private AudioManager am;

    private SharedPreferences sharedPref;
    private SharedPreferences.Editor sharedPrefEditor;
    private Resources resources;

    private int maxVolume;
    private int preAppVolume;
    private int lowSpeedVolume;
    private int highSpeedVolume;
    private int currentVolume;


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
        startService();

    }

    public int getHighSpeedVolume() {
        return highSpeedVolume;
    }

    public int getLowSpeedVolume() {

        return lowSpeedVolume;
    }


    private void startService() {
        Intent serviceIntent = new Intent(mainContext, ForegroundService.class);
        serviceIntent.putExtra(resources.getString(R.string.lowSpeedVolumeKey), lowSpeedVolume);
        serviceIntent.putExtra(resources.getString(R.string.highSpeedVolumeKey), highSpeedVolume);
        serviceIntent.putExtra(resources.getString(R.string.stopUpdatesKey), false);
        mainContext.startService(serviceIntent);
    }
}
