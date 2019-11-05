package e.dholland.ski_j;


import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class OptionsActivity extends AppCompatActivity {
    SharedPreferences sharedPref;
    int themePreference;
    int unitPreference;
    float refreshRate;
    boolean isAutoStart;
    EditText refreshRateText;
    Button backButton;
    CheckBox autoStartBox;
    Button helpButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        themePreference = sharedPref.getInt(getString(R.string.themePreferenceKey),0);
        unitPreference = sharedPref.getInt(getString(R.string.unitPreferenceKey),0);
        refreshRate = sharedPref.getFloat(getString(R.string.refreshRateKey),1);
        isAutoStart = sharedPref.getBoolean(getString(R.string.autoStartKey),true);
        System.out.println(themePreference);
        if (themePreference == 0){
            setTheme(R.style.LightTheme);
        } else if (themePreference == 1){
            setTheme(R.style.DarkTheme);
        }
        setContentView(R.layout.activity_options);

        Spinner spinner =(Spinner) findViewById(R.id.unit_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.unitChoices, R.layout.spinner_layout);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(unitPreference);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            SharedPreferences.Editor sharedPrefEditor = sharedPref.edit();
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                System.out.println(position);
                sharedPrefEditor.putInt(getString(R.string.unitPreferenceKey),position);
                sharedPrefEditor.commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        //resources = getApplicationContext().getResources();

        Switch themeSwitch = (Switch) findViewById(R.id.themeSwitch);
        if (themePreference == 0){
            themeSwitch.setChecked(false);
        } else if (themePreference == 1){
            themeSwitch.setChecked(true);
        }
        themeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.isPressed()) {
                    SharedPreferences.Editor sharedPrefEditor = sharedPref.edit();
                    if (isChecked) {
                        System.out.println("isChecked");
                        sharedPrefEditor.putInt(getString(R.string.themePreferenceKey),1);
                        sharedPrefEditor.commit();
                        recreate();
                    } else {
                        System.out.println("lighttheme");
                        sharedPrefEditor.putInt(getString(R.string.themePreferenceKey),0);
                        sharedPrefEditor.commit();
                        recreate();

                    }
                }
            }
        });

        refreshRateText = (EditText)findViewById(R.id.refreshRateNumber);
        refreshRateText.setText(Float.toString(refreshRate));
        refreshRateText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId== EditorInfo.IME_ACTION_DONE){
                    SharedPreferences.Editor sharedPrefEditor = sharedPref.edit();
                    String temp = refreshRateText.getText().toString();
                    refreshRate = Float.parseFloat(temp);
                    sharedPrefEditor.putFloat(getString(R.string.refreshRateKey), refreshRate);
                    sharedPrefEditor.commit();
                }
                return false;
            }
        });
        backButton = (Button) findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMainActivity();
            }
        });
        autoStartBox = (CheckBox) findViewById(R.id.automaticStartCheckBox);
        if (isAutoStart){
            autoStartBox.setChecked(true);
        }
        else {
            autoStartBox.setChecked(false);
        }
        autoStartBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor sharedPrefEditor = sharedPref.edit();
                if (autoStartBox.isChecked()){
                    sharedPrefEditor.putBoolean(getString(R.string.autoStartKey),true);
                }
                else{
                    sharedPrefEditor.putBoolean(getString(R.string.autoStartKey),false);
                }
                sharedPrefEditor.commit();
            }
        });
        helpButton = (Button) findViewById(R.id.helpButtonOptions);
        helpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onShowPopupWindow(view);
            }
        });


    }
    public void openMainActivity(){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
    public void onDestroy() {
        super.onDestroy();
        stopService();
    }
    public void stopService(){
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        stopService(serviceIntent);
    }
    public void onShowPopupWindow(View view){
        LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_options, null);
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        TypedValue temp = new TypedValue();
        getTheme().resolveAttribute(R.attr.background,temp, true );
        int height = size.y;
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
}

