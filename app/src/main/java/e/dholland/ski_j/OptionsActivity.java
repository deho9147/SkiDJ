package e.dholland.ski_j;


import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        themePreference = sharedPref.getInt(getString(R.string.themePreferenceKey),0);
        unitPreference = sharedPref.getInt(getString(R.string.unitPreferenceKey),0);
        refreshRate = sharedPref.getFloat(getString(R.string.refreshRateKey),1);
        isAutoStart = sharedPref.getBoolean(getString(R.string.autoStartKey),true);
        if (themePreference == 0){
            setTheme(R.style.BaseTheme);
        } else if (themePreference == 1){
            setTheme(R.style.Light);
        }
        setContentView(R.layout.activity_options);

        Spinner spinner =(Spinner) findViewById(R.id.unit_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.unitChoices, android.R.layout.simple_spinner_item);
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
                        getApplication().setTheme(R.style.Dark);
                        sharedPrefEditor.commit();
                        recreate();
                    } else {
                        System.out.println("not checked");
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


    }
    public void openMainActivity(){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}

