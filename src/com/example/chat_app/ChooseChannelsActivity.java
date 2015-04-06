package com.example.chat_app;

import android.support.v7.app.ActionBarActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;

public class ChooseChannelsActivity extends ActionBarActivity {

	private static final boolean DEBUG = true;
	private static final String TAG = "Choose channels activity";
	
	private static boolean allowBT;
	private static boolean allowSMS;
	private static boolean allowWiFi;
	
	private CheckBox btCheckBox;
	private CheckBox smsCheckBox;
	private CheckBox wifiCheckBox;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_choose_channels);
		
		//getting initial values of checkbox from main activity
		Intent i = getIntent();
		allowBT = i.getBooleanExtra("bt", false);
		allowSMS = i.getBooleanExtra("sms", false);
		allowWiFi = i.getBooleanExtra("wifi", false);
		
		//initializing checkboxes
		btCheckBox = (CheckBox) findViewById(R.id.bluetoothCheckBox);
		smsCheckBox = (CheckBox) findViewById(R.id.smsCheckBox);
		wifiCheckBox = (CheckBox) findViewById(R.id.wifiCheckBox);
		
		btCheckBox.setChecked(allowBT);
		smsCheckBox.setChecked(allowSMS);
		wifiCheckBox.setChecked(allowWiFi);
	}
	
	public void onCheckboxClicked(View view) {
	    // Is the view now checked?
	    boolean checked = ((CheckBox) view).isChecked();
	    
	    // Check which checkbox was clicked
	    switch(view.getId()) {
	        case R.id.bluetoothCheckBox:
	            if (checked){ allowBT = true; }
	            else{ allowBT = false; }
	            break;
	        case R.id.smsCheckBox:
	            if (checked){ allowSMS = true; }
	            else{ allowSMS = false; }
	            break;
	        case R.id.wifiCheckBox:
	        	if (checked){ allowWiFi = true; }
	        	else{ allowWiFi = false; }
	        	break;
	    }
	}

	@Override
	public void onBackPressed() {
		sendDataToMain();
		finish();
		if(DEBUG){ Log.i(TAG, "on back pressed"); }
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		switch(item.getItemId()){
		case(R.id.home):
			sendDataToMain();
			finish();
			if(DEBUG){ Log.i(TAG, "on up pressed"); }
			return true;
		case(R.id.action_settings):
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void sendDataToMain() {
		//send data back to main activity
		Intent i = new Intent();
		i.putExtra("bt", allowBT);
		i.putExtra("sms", allowSMS);
		i.putExtra("wifi", allowWiFi);
		setResult(RESULT_OK,i);
	}
	
	
}
