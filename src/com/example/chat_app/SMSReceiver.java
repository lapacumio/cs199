package com.example.chat_app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

public class SMSReceiver extends BroadcastReceiver {
	
	MainActivity main = null;
	
	void setMainActivityHandler(MainActivity main){
	    this.main=main;
	}
	
	public void onReceive(Context context, Intent intent)
	{
		Log.i("SMSReceiver", "on Receive");
		Bundle bundle=intent.getExtras();
		
		Object[] messages=(Object[])bundle.get("pdus");
		SmsMessage[] sms=new SmsMessage[messages.length];
		
		for(int n=0;n<messages.length;n++){
			sms[n]=SmsMessage.createFromPdu((byte[]) messages[n]);
		}
		
		String message="";
		for(SmsMessage msg:sms){
			message = msg.getMessageBody();
			/* if message is not sent to the app, ignore message */
			if(message.charAt(0)=='_'){
				Log.i("SMSReceiver", "sending to main activity");
				//MainActivity.receiveSMS(message);
				main.receiveSMS(message);
			}
		}
		//if(message.isEmpty() || message.charAt(0)=='_'){ abortBroadcast(); }
	}
}
