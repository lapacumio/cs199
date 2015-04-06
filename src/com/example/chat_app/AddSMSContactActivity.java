package com.example.chat_app;

import java.util.ArrayList;
import java.util.List;

import android.support.v7.app.ActionBarActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class AddSMSContactActivity extends ActionBarActivity {

	private static final boolean DEBUG = true;
	private static final String TAG = "Add SMS contact activity";
	EditText phoneNumET;
	EditText displayET;
	Button addButton;
	
	ArrayList<String> savedPhoneNumbers;
	ArrayList<String> newPhoneNumbers;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_smscontact);
		
		Intent intent = getIntent();
		
		phoneNumET = (EditText) findViewById(R.id.phoneNumET2);
		displayET = (EditText) findViewById(R.id.savedContactsET);
		addButton = (Button) findViewById(R.id.addButton);
		
		newPhoneNumbers = new ArrayList<String>();
		savedPhoneNumbers = intent.getStringArrayListExtra("contacts");
		
		addButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				saveContact();
				phoneNumET.setText("");
			}
		});
		
		for(String num : savedPhoneNumbers){
			displayET.append(num+'\n');
		}
	}
	
	private void saveContact() {
		String phoneNum = phoneNumET.getText().toString();
		//TODO check if input is valid
		newPhoneNumbers.add(phoneNum);
		displayET.append(phoneNum+'\n');
	}
	
	@Override
	public void onBackPressed() {
		sendDataToMain();
		finish();
		if(DEBUG){ Log.i(TAG, "on back pressed"); }
	}
	
	private void sendDataToMain() {
		//send data back to main activity
		
		String string = "";
		for(String s : newPhoneNumbers){
			string = string + '\n' + s;
		}
		
		Intent i = new Intent();
		i.putStringArrayListExtra("newcontacts", newPhoneNumbers);
		setResult(RESULT_OK,i);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
