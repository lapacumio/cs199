package com.example.chat_app;

import android.support.v7.app.ActionBarActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class InputUsernameActivity extends ActionBarActivity {

	EditText usernameTV;
	Button saveBtn;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_input_username);
		
		usernameTV = (EditText) findViewById(R.id.usernameET);
		saveBtn = (Button) findViewById(R.id.saveUsernameButton);
		saveBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				save();
			}
		});
	}
	
	private void save() {
		String username = usernameTV.getText().toString();
		if(username.length()<=0){ return; }

		Intent i = new Intent();
		i.putExtra("username", username);
		setResult(RESULT_OK,i);
		
		finish();
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
