package e.dholland.ski_j;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import androidx.core.app.ActivityCompat;


public class MainActivity extends Activity implements LocationListener {

    public static final int REQUEST_LOCATION = 1;

    LocationManager locationManager;
    AudioManager am;

    Button helpButton;
    SeekBar sblow;
    SeekBar sbhigh;
    SeekBarVolume sbv;
    Button startButton;
    EditText minVelocityText;
    EditText maxVelocityText;
    CheckBox liftCheckBox;
    Button optionsButton;

    SharedPreferences sharedPref;
    SharedPreferences.Editor sharedPrefEditor;

    int highVolume;
    int lowVolume;
    float maxVelocity;
    float minVelocity;
    boolean liftBool;
    boolean autoStart;
    int refreshRate;
    int theme;
    int unitPreference;

    int beforeVolume;
    boolean serviceRunning;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        lowVolume = sharedPref.getInt(getString(R.string.lowSpeedVolumeKey),0);
        highVolume = sharedPref.getInt(getString(R.string.highSpeedVolumeKey),13);
        minVelocity = sharedPref.getFloat(getString(R.string.lowVelKey),0);
        maxVelocity = sharedPref.getFloat(getString(R.string.highVelKey),10);
        liftBool = sharedPref.getBoolean(getString(R.string.liftCheckKey), true);
        theme = sharedPref.getInt(getString(R.string.themePreferenceKey),0);
        autoStart = sharedPref.getBoolean(getString(R.string.autoStartKey),false);
        refreshRate = sharedPref.getInt(getString(R.string.refreshRateKey),1);
        unitPreference = sharedPref.getInt(getString(R.string.unitPreferenceKey),0);


        serviceRunning=false;


        if (theme == 0){
            setTheme(R.style.LightTheme);
        } else {
            setTheme(R.style.DarkTheme);
        }
        setContentView(R.layout.activity_main);


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
            this.onLocationChanged(null);
        }


        helpButton = (Button)findViewById(R.id.helpButton);
        helpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onShowPopupWindow(v);
            }
        });


        sbhigh = (SeekBar)findViewById(R.id.VolumeBarHigh);
        sblow = (SeekBar)findViewById(R.id.VolumeBarLow);
        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        beforeVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        sbv = new SeekBarVolume(this, sbhigh, sblow, am);


        minVelocityText = (EditText)findViewById(R.id.minSpeed);
        minVelocityText.setText(Integer.toString(Math.round(metersperSecondtoUnit(minVelocity))));
        minVelocityText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId==EditorInfo.IME_ACTION_DONE){
                    sharedPrefEditor = sharedPref.edit();
                    String temp = minVelocityText.getText().toString();
                    minVelocity = unitToMetersPerSecond(Float.parseFloat(temp));
                    sharedPrefEditor.putFloat(getString(R.string.lowVelKey),minVelocity);
                    sharedPrefEditor.commit();
                    if (autoStart||serviceRunning) {
                        startService();
                    }
                }
                return false;
            }
        });


        maxVelocityText = (EditText)findViewById(R.id.maxSpeed);
        maxVelocityText.setText(Integer.toString(Math.round(metersperSecondtoUnit(maxVelocity))));
        maxVelocityText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId==EditorInfo.IME_ACTION_DONE){
                    sharedPrefEditor = sharedPref.edit();
                    String temp = maxVelocityText.getText().toString();
                    System.out.println(temp);
                    System.out.println(Float.parseFloat(temp));
                    maxVelocity = unitToMetersPerSecond(Float.parseFloat(temp));
                    sharedPrefEditor.putFloat(getString(R.string.highVelKey), maxVelocity);
                    sharedPrefEditor.commit();
                    if (autoStart||serviceRunning) {
                        startService();
                    }
                }
                return false;
            }
        });


        liftCheckBox = findViewById(R.id.LiftCheck);
        liftCheckBox.setChecked(liftBool);
        liftCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (liftCheckBox.isChecked()){
                    liftBool = true;
                    if (autoStart) {
                        startService();
                    }
                }
                else {
                    liftBool = false;
                    if (autoStart||serviceRunning) {
                        startService();
                    }
                    sharedPrefEditor = sharedPref.edit();
                    sharedPrefEditor.putBoolean(getString(R.string.liftCheckKey),liftBool);
                    sharedPrefEditor.commit();
                }
            }
        });



        optionsButton = findViewById(R.id.optionsButton);
        optionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startOptionsActivity();
            }
        });


        startButton = (Button) findViewById(R.id.startButton);
        if (autoStart){
            startButton.setVisibility(View.INVISIBLE);
            serviceRunning=true;
            startService();
        }else{
            startButton.setVisibility(View.VISIBLE);
            startButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (startButton.getText().toString()=="Stop"){
                        serviceRunning=false;
                        startButton.setBackgroundResource(R.drawable.start_button_green);
                        stopService();
                        startButton.setText("Start");
                    }
                    else{
                        startButton.setText("Stop");
                        startButton.setBackgroundResource(R.drawable.start_button_red);
                        serviceRunning = true;
                        startService();
                    }
                }
            });
        }
    }

    public void onLocationChanged(Location location) {
        TextView txt = (TextView) this.findViewById(R.id.Speedtxt);
        int outspeed = -1;
        if (location == null) {
            txt.setText("Unable to grab Location Data");
        } else {
            float nCurrentSpeed = location.getSpeed();
            if (unitPreference == 0){
                txt.setText(Integer.toString(Math.round(metersperSecondtoUnit(nCurrentSpeed)))+" Mph");
            }
            else if(unitPreference == 1) {
                txt.setText(Integer.toString(Math.round(metersperSecondtoUnit(nCurrentSpeed)))+" Kmh");
            }
            else{
                txt.setText(Integer.toString(Math.round(nCurrentSpeed))+" m/s");
            }
        }
    }


    public float metersperSecondtoUnit(float mps){
        float outputSpeed;
        if (unitPreference==0){
            //from m/s to mph
            outputSpeed = (float)(mps*2.237);
        }
        else if(unitPreference==1){
            //from KmH to m/s
            outputSpeed = (float)(mps*3.6);
        }
        else{
            //alreadymps
            outputSpeed = mps;
        }
        return outputSpeed;
    }


    public float unitToMetersPerSecond(float unitSpeed){
        System.out.println("in unit to mps");
        System.out.println(unitSpeed);
        float mps;
        if (unitPreference == 0){
            //convert mph to mps
            System.out.println(unitSpeed);
            mps = (float)(unitSpeed/2.237);
            System.out.println(mps);
        }
        else if(unitPreference == 1){
            //convert kmh to mps
            mps = (float)(unitSpeed/3.6);
        }
        else{
            //already mps
            mps = unitSpeed;
        }
        return mps;
    }

    //general location listener items unused
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }


    public void onShowPopupWindow(View view){
        LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup, null);
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        TypedValue temp = new TypedValue();
        getTheme().resolveAttribute(R.attr.background,temp, true );
        popupView.setBackground(getDrawable(R.drawable.simple_border));
        final PopupWindow popupWindow = new PopupWindow(popupView, ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT, true);
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 20);
        popupView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                popupWindow.dismiss();
                return true;
            }
        });
    }


    protected void onDestroy() {
        super.onDestroy();
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPrefEditor = sharedPref.edit();
        sharedPrefEditor.putInt(getString(R.string.highSpeedVolumeKey), sbv.getHighSpeedVolume());
        sharedPrefEditor.putInt(getString(R.string.lowSpeedVolumeKey), sbv.getLowSpeedVolume());
        sharedPrefEditor.putFloat(getString(R.string.highVelKey), maxVelocity);
        sharedPrefEditor.putFloat(getString(R.string.lowVelKey), minVelocity);
        sharedPrefEditor.putBoolean(getString(R.string.liftCheckKey), liftBool);
        sharedPrefEditor.putInt(getString(R.string.refreshRateKey), refreshRate);
        sharedPrefEditor.commit();

        am.setStreamVolume(AudioManager.STREAM_MUSIC, beforeVolume, 0);
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        stopService(serviceIntent);
    }


    public void startService(){
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        serviceIntent.putExtra(getString(R.string.highVelKey), maxVelocity);
        serviceIntent.putExtra(getString(R.string.lowVelKey), minVelocity);
        serviceIntent.putExtra(getString(R.string.lowSpeedVolumeKey), sbv.getLowSpeedVolume());
        serviceIntent.putExtra(getString(R.string.highSpeedVolumeKey), sbv.getHighSpeedVolume());
        serviceIntent.putExtra(getString(R.string.liftCheckKey),liftBool);
        serviceIntent.putExtra(getString(R.string.stopUpdatesKey), false);
        serviceIntent.putExtra(getString(R.string.refreshRateKey), refreshRate);
        startForegroundService(serviceIntent);
    }

    public void stopService(){
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        stopService(serviceIntent);
    }

    public void startOptionsActivity(){
        Intent intent = new Intent(this, OptionsActivity.class);
        startActivity(intent);
    }
}
