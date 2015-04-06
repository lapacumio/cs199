package com.example.chat_app;

import java.util.ArrayList;
import java.util.List;
import android.support.v7.app.ActionBarActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity {

	// Debugging
	private static final boolean DEBUG = true;
	private static final String TAG = "Chat App";
	
	// Intent request codes
	private static final int CHOOSE_CHANNEL_REQUEST = 100;
	private static final int ADD_SMS_CONTACT_REQUEST = 200;
	private static final int CONNECT_TO_NETWORK_REQUEST = 300;
	private static final int START_CONVERSATION_REQUEST = 400;
	private static final int CONNECT_BT_DEVICE_REQUEST = 500;
	private static final int ENABLE_BT_REQUEST = 6;
	private static final int INPUT_USERNAME_REQUEST = 7;
	
    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    	
	// Layout views
	private TextView titleTV;
	private EditText receiverIdET;
    private ListView outputView;
    private EditText inputET;
	private Button sendButton;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
    private static ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;
	
	private static int numOfPieces;
	private static ArrayList<String> receivedMessage;
	
	private static boolean allowBT;
	private static boolean allowSMS;
	private static boolean allowWiFi;
	
	private static ArrayList<String> smsContacts;
	private static String myId;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		if(DEBUG){ Log.i(TAG, "--ON CREATE--"); }
		
		// start activity to get username
		Intent i = new Intent(getApplicationContext(), InputUsernameActivity.class);
		startActivityForResult(i, INPUT_USERNAME_REQUEST);
		
		allowBT = true;
		allowSMS = true;
		allowWiFi = true;
		
		smsContacts = new ArrayList<String>();
		//TODO update smsContacts with saved data
		
		titleTV = (TextView) findViewById(R.id.convoTitleTV);
		outputView = (ListView) findViewById(R.id.outputListView);
		receiverIdET = (EditText) findViewById(R.id.receiverIdET);
        inputET = (EditText) findViewById(R.id.inputET);
		sendButton = (Button) findViewById(R.id.sendButton);
		sendButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				// construct message fragments
				String message = inputET.getText().toString();
				if(message.length()<=0){ return; }
				List<String> fragments = splitMessage(message);
				
				for(String fragment : fragments){
					sendMessage(fragment);
				}
				
				mConversationArrayAdapter.add(myId + ": " + message);
			}
		});
		mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
		outputView.setAdapter(mConversationArrayAdapter);
		
		// Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            allowBT = false;
        }
        
        // setup SMS broadcast receiver
        SMSReceiver BR_smsreceiver = null;
        BR_smsreceiver = new SMSReceiver();
        BR_smsreceiver.setMainActivityHandler(this);
        IntentFilter fltr_smsreceived = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        fltr_smsreceived.setPriority(999);
        registerReceiver(BR_smsreceiver,fltr_smsreceived);  
	}

	@Override
    public void onStart() {
        super.onStart();
        if(DEBUG) Log.e(TAG, "--ON START--");
        
        if(allowBT){
	        // If BT is not on, request that it be enabled.
	        // setupChat() will then be called during onActivityResult
	        if (!mBluetoothAdapter.isEnabled()) {
	            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	            startActivityForResult(enableIntent, ENABLE_BT_REQUEST);
	        // Otherwise, setup the chat session
	        } else {
	            if (mChatService == null) setupChat();
	        }
        }
    }
    
    @Override
    public synchronized void onResume() {
        super.onResume();
        if(DEBUG) Log.e(TAG, "--ON RESUME--");
        
        if(allowBT){
	        // Performing this check in onResume() covers the case in which BT was
	        // not enabled during onStart(), so we were paused to enable it...
	        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
	        if (mChatService != null) {
	            // Only if the state is STATE_NONE, do we know that we haven't started already
	            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
	              // Start the Bluetooth chat services
	              mChatService.start();
	            }
	        }
    	}
    }
    
    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the array adapter for the conversation thread
        //mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        //mConversationView = (ListView) findViewById(R.id.in);
        //mConversationView.setAdapter(mConversationArrayAdapter);
        //outputView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        //mOutEditText = (EditText) findViewById(R.id.edit_text_out);
        //mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        //mSendButton = (Button) findViewById(R.id.button_send);
        //mSendButton.setOnClickListener(new OnClickListener() {
        //    public void onClick(View v) {
        //        // Send a message using content of the edit text widget
        //        TextView view = (TextView) findViewById(R.id.edit_text_out);
        //        String message = view.getText().toString();
        //        sendMessage(message);
        //    }
        //});

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }
    
    @Override
    public synchronized void onPause() {
        super.onPause();
        if(DEBUG) Log.e(TAG, "--ON PAUSE--");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(DEBUG) Log.e(TAG, "--ON STOP--");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(DEBUG) Log.e(TAG, "--ON DESTROY--");
        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
    }
    
    private void ensureDiscoverable() {
        if(DEBUG) Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }
    
	private void sendMessage(String fragment) {
		
		/* check if there is an allowed channel */
		if(!allowSMS && allowBT && allowWiFi){ return; }
		
		/* sending via SMS */
		if(allowSMS){
			
			if(DEBUG){ Log.i(TAG, "SENT VIA SMS " + System.currentTimeMillis() ); }
	        
			try {
				for(String phoneNum : smsContacts){
					SmsManager smsManager = SmsManager.getDefault();
					smsManager.sendTextMessage(phoneNum, null, fragment, null, null);
					Toast.makeText(getApplicationContext(), "Your sms has successfully sent!",
							Toast.LENGTH_LONG).show();
				}
			} catch (Exception ex) {
				Toast.makeText(getApplicationContext(),"Your sms has failed...",
						Toast.LENGTH_LONG).show();
				ex.printStackTrace();
			}
		}
		
		/* sending via BT */
		if(allowBT){
			// Check that we're actually connected before trying anything
	        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
	            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
	            return;
	        }
	        
            if(DEBUG){ Log.i(TAG, "SENT VIA BLUETOOTH " + System.currentTimeMillis() ); }
	        
            byte[] send = fragment.getBytes();
            mChatService.write(send);
            
	        /*for(String piece : pieces){
	            // Get the message bytes and tell the BluetoothChatService to write
	            byte[] send = piece.getBytes();
	            mChatService.write(send);
	        }*/
		}
		
		/* sending via WiFi */
		if(allowWiFi){
			if(DEBUG){ Log.i(TAG, "SENT VIA WIFI " + System.currentTimeMillis() ); }
	        
			// TODO
		}
				
        // Reset out string buffer to zero and clear the edit text field
        mOutStringBuffer.setLength(0);
        inputET.setText(mOutStringBuffer);
        //inputET.setText("");
	}
	
    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(DEBUG) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothChatService.STATE_CONNECTED:
                	titleTV.setText("Connected to");
                    titleTV.append(mConnectedDeviceName);
                    mConversationArrayAdapter.clear();
                    break;
                case BluetoothChatService.STATE_CONNECTING:
                	titleTV.setText("Connecting");
                    break;
                case BluetoothChatService.STATE_LISTEN:
                case BluetoothChatService.STATE_NONE:
                	Toast.makeText(getApplicationContext(), "State:  none",
                            Toast.LENGTH_SHORT).show();
                	titleTV.setText("Not connected");
                    break;
                }
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                //mConversationArrayAdapter.add("Me:  " + writeMessage);
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                if (readMessage.length() > 0) {
                	processReceivedMessage(readMessage);
                    //mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
                }
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
            	//if (!msg.getData().getString(TOAST).contains("Unable to connect device")) {
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();            		
            	//}
                break;
            }
        }
    };
	
	public void receiveSMS(String msg){
    	
		processReceivedMessage(msg);
		
    }
	
	public void processReceivedMessage(String msg){
		String[] parts = msg.split("_");
		String senderid = parts[1];
		String receiverid = parts[2];
		int fragmentNum = Integer.parseInt(parts[3]);
		int flag = Integer.parseInt(parts[4]);
		String body = parts[5]; // TODO improve this. Get substring or something
		
		if(!receiverid.equals(myId) && !senderid.equals(myId)){
			// forward message
			sendMessage(msg);
			Toast.makeText(this, "forwarded", Toast.LENGTH_SHORT).show();
		}
		else{
        	if(receivedMessage==null){
	    		receivedMessage = new ArrayList<String>();
	    	}
	    	receivedMessage.add(fragmentNum, body);
	    	
			if(flag==1){numOfPieces = fragmentNum+1; }
	    	
	    	//if message is complete. Print out
	    	if(numOfPieces>0 && receivedMessage.size()==numOfPieces){
	    		String fullMessage = senderid + ": ";
	    		// TODO check if this prints in proper order
	    		for(String s : receivedMessage){
	    			fullMessage = fullMessage.concat(s);
	    		}
	    		mConversationArrayAdapter.add(fullMessage);
	        	receivedMessage =  null;
	        	numOfPieces = 0;
	        	
	        	if(DEBUG) Log.i(TAG, "RECEIVED " + System.currentTimeMillis());
            }
		}
	
	}
	
	public List<String> splitMessage(String text) {
		//TODO define receiver Id. or do it somewhere else
		String receiverId = receiverIdET.getText().toString();
		if(receiverId.length()<=0){
			// TODO no receiver selected. Do something abt this.
			// an option is that this means everyone receives it.
			// if not, tell user to input receiver.
		}
		
		String header = "_" + myId + "_" + receiverId + "_";
		int max_text_length = 160 - header.length() - 5;
		int flag = 0;
		
		List<String> strings = new ArrayList<String>();
		int numOfPieces = text.length()/max_text_length;
		if(text.length()%max_text_length >0){ numOfPieces++; }
		for(int i=0; i<numOfPieces; i++){
			if(i+1==numOfPieces){ flag=1; }
			String body = text.substring(i*max_text_length, Math.min((i+1)*max_text_length,text.length()));
		    strings.add(header+i+"_"+flag+"_"+body);
		}
		return strings;
	}

	private void startAddSMSContactActivity() {
		Intent i = new Intent(getApplicationContext(), AddSMSContactActivity.class);
		i.putStringArrayListExtra("contacts", smsContacts);
		startActivityForResult(i, ADD_SMS_CONTACT_REQUEST);
		if(DEBUG){ Log.i(TAG, "AddSMSContactActivity started"); }
	}

	private void startChooseChannelsActivity() {
		Intent i = new Intent(getApplicationContext(), ChooseChannelsActivity.class);
		i.putExtra("bt", allowBT);
		i.putExtra("sms", allowSMS);
		i.putExtra("wifi", allowWiFi);
		startActivityForResult(i, CHOOSE_CHANNEL_REQUEST);
		if(DEBUG){ Log.i(TAG, "ChooseChannelsActivity started"); }
	}
	
	// Function to read the result from newly created activity
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        switch(requestCode){
        case(CHOOSE_CHANNEL_REQUEST):
        	if(resultCode == RESULT_OK){
	    		allowBT = intent.getBooleanExtra("bt", false);
	    		allowSMS = intent.getBooleanExtra("sms", false);
	    		allowWiFi = intent.getBooleanExtra("wifi", false);
	    		if(DEBUG){ Log.i(TAG, "ChooseChannels Activity RESULT_OK"); }
    		}
	        else if (resultCode==RESULT_CANCELED){
	        	if(DEBUG){ Log.i(TAG, "ChooseChannelsActivity RESULT_CANCELLED"); }
    		}
	        break;
        case(ADD_SMS_CONTACT_REQUEST):
        	if(resultCode == RESULT_OK){
        		smsContacts.addAll(intent.getStringArrayListExtra("newcontacts"));
        		if(DEBUG){ Log.i(TAG, "AddSMSContact Activity RESULT_OK"); }
        	}
        	else if (resultCode==RESULT_CANCELED){
	        	if(DEBUG){ Log.i(TAG, "AddSMSContactActivity RESULT_CANCELLED"); }
    		}
        	break;
        case(CONNECT_TO_NETWORK_REQUEST):
        	//TODO
        	break;
        case(START_CONVERSATION_REQUEST):
        	//TODO
        	break;
    	case(CONNECT_BT_DEVICE_REQUEST):
	        // When DeviceListActivity returns with a device to connect
	        if (resultCode == Activity.RESULT_OK) {
	            // Get the device MAC address
	            String address = intent.getExtras()
	                                 .getString(BTDeviceListActivity.EXTRA_DEVICE_ADDRESS);
	            // Get the BLuetoothDevice object
	            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
	            // Attempt to connect to the device
	            mChatService.connect(device);
	        }
	        break;
	    case(ENABLE_BT_REQUEST):
	        // When the request to enable Bluetooth returns
	        if (resultCode == Activity.RESULT_OK) {
	            // Bluetooth is now enabled, so set up a chat session
	            setupChat();
	        } else {
	            // User did not enable Bluetooth or an error occured
	            Log.d(TAG, "BT not enabled");
	            Toast.makeText(this, "BT not enabled", Toast.LENGTH_SHORT).show();
	            allowBT = false;
	        }
	        break;
        case(INPUT_USERNAME_REQUEST):
        	if(resultCode == RESULT_OK){
        		myId = intent.getStringExtra("username");
            	if(DEBUG){ Log.i(TAG, "InputUsernameActivity RESULT_OK"); }
        	}
        	else if (resultCode==RESULT_CANCELED){
	        	if(DEBUG){ Log.i(TAG, "InputUsernameActivity RESULT_CANCELLED"); }
    		}
        	break;
        }
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		switch(id){
		case(R.id.action_settings):
			return true;
		case(R.id.edit_allowed_channels):
			startChooseChannelsActivity();
			return true;
		case(R.id.add_SMS_contact):
			startAddSMSContactActivity();
			return true;
		case(R.id.connect_to_network):
			// TODO
			Toast.makeText(getApplicationContext(), R.string.feature_unavailable,
					Toast.LENGTH_SHORT).show();
			return true;
		case(R.id.start_conversation):
			//TODO
			Toast.makeText(getApplicationContext(), R.string.feature_unavailable,
					Toast.LENGTH_SHORT).show();
			return true;
        case R.id.scan_for_bt_devices:
            // Launch the DeviceListActivity to see devices and do scan
            Intent serverIntent = new Intent(this, BTDeviceListActivity.class);
            startActivityForResult(serverIntent, CONNECT_BT_DEVICE_REQUEST);
            return true;
        case R.id.ensure_bt_discoverable:
            // Ensure this device is discoverable by others
            ensureDiscoverable();
            return true;
        }
		return false;
	}
}
