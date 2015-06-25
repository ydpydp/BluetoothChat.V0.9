/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.BluetoothChat;

import java.util.Timer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This is the main Activity that displays the current chat session.
 */
@SuppressLint("NewApi")
public class BluetoothChat extends Activity implements OnClickListener {
	// Debugging
	private static final String TAG = "BluetoothChat";
	private static final boolean D = true;

	// Message types sent from the BluetoothChatService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;
	public static final int MESSAGE_TIME=0;

	// Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;
	public static int revBytes = 0;
	public static boolean isHex = false;

	// Layout Views
	private TextView mTitle, tv_01, tv_02, tv_03;
	/* btn_01, btn_02,menu_tv_03, */
	private Button btn_03, btn_04;

	private TextView menu_tv_01, menu_tv_02, menu_tv_04, menu_tv_05;
	// private ListView mConversationView;
	/*
	 * private EditText mOutEditText; private Button mSendButton; private
	 * EditText mOutEditText2; private Button mSendButton2;
	 */

	// Name of the connected device
	private String mConnectedDeviceName = null;
	// Array adapter for the conversation thread
	private ArrayAdapter<String> mConversationArrayAdapter;
	// String buffer for outgoing messages
	private StringBuffer mOutStringBuffer;
	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;
	// Member object for the chat services
	private BluetoothChatService mChatService = null;
	private String readMessage1 = null;
	private String weight = null;
	private String temperature = null;
	private Timer timer;
	private SlidingMenu mMenu;
	boolean flag = true;
	
	/*boolean  Flag=true;*/
	Intent mIntent = new Intent();
	long time = 0;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (D)
			Log.e(TAG, "+++ ON CREATE +++");

		// Set up the window layout
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.main);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.custom_title);

		init();
		// Set up the custom title
		mTitle = (TextView) findViewById(R.id.title_left_text);
		mTitle.setText(R.string.app_name);
		mTitle = (TextView) findViewById(R.id.title_right_text);

		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available",
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}

	}

	public void init() {

		/*
		 * btn_01 = (Button) findViewById(R.id.btn_01); btn_02 = (Button)
		 * findViewById(R.id.btn_02);
		 */
		btn_03 = (Button) findViewById(R.id.btn_03);
		btn_04 = (Button) findViewById(R.id.btn_04);

		/*
		 * btn_01.setOnClickListener(this); btn_02.setOnClickListener(this);
		 */
		btn_03.setOnClickListener(this);
		btn_04.setOnClickListener(this);

		mMenu = (SlidingMenu) findViewById(R.id.id_menu);

		menu_tv_01 = (TextView) findViewById(R.id.menu_tv_01);
		menu_tv_02 = (TextView) findViewById(R.id.menu_tv_02);
		// menu_tv_03 = (TextView) findViewById(R.id.menu_tv_03);
		menu_tv_04 = (TextView) findViewById(R.id.menu_tv_04);
		menu_tv_05 = (TextView) findViewById(R.id.menu_tv_05);
		menu_tv_01.setOnClickListener(this);
		menu_tv_02.setOnClickListener(this);
		// menu_tv_03.setOnClickListener(this);
		menu_tv_04.setOnClickListener(this);
		menu_tv_05.setOnClickListener(this);

		tv_03 = (TextView) findViewById(R.id.tv_03);

	}

	public void toggleMenu(View view) {
		mMenu.toggle();
	}

	@Override
	public void onStart() {
		super.onStart();
		if (D)
			Log.e(TAG, "++ ON START ++");

		// If BT is not on, request that it be enabled.
		// setupChat() will then be called during onActivityResult
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
			// Otherwise, setup the chat session
		} else {
			if (mChatService == null)
				setupChat();
		}
	}

	@Override
	public synchronized void onResume() {
		super.onResume();
		if (D)
			Log.e(TAG, "+ ON RESUME +");

		// Performing this check in onResume() covers the case in which BT was
		// not enabled during onStart(), so we were paused to enable it...
		// onResume() will be called when ACTION_REQUEST_ENABLE activity
		// returns.
		if (mChatService != null) {
			// Only if the state is STATE_NONE, do we know that we haven't
			// started already
			if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
				// Start the Bluetooth chat services
				mChatService.start();
			}
		}
	}

	private void setupChat() {
		Log.d(TAG, "setupChat()");

		// Initialize the array adapter for the conversation thread
		mConversationArrayAdapter = new ArrayAdapter<String>(this,
				R.layout.message);

		/*
		 * mConversationView = (ListView) findViewById(R.id.in);
		 * mConversationView.setAdapter(mConversationArrayAdapter);
		 */
		/*
		 * mConversationView = (ListView) findViewById(R.id.in);
		 * mConversationView.setAdapter(mConversationArrayAdapter);
		 */

		/*
		 * // Initialize the compose field with a listener for the return key
		 * mOutEditText = (EditText) findViewById(R.id.edit_text_out);
		 * mOutEditText.setOnEditorActionListener(mWriteListener);
		 * 
		 * mOutEditText2 = (EditText) findViewById(R.id.edit_text_out2);
		 * mOutEditText2.setOnEditorActionListener(mWriteListener);
		 * 
		 * // Initialize the send button with a listener that for click events
		 * mSendButton = (Button) findViewById(R.id.button_send);
		 * mSendButton.setOnClickListener(new OnClickListener() { public void
		 * onClick(View v) { // Send a message using content of the edit text
		 * widget TextView view = (TextView) findViewById(R.id.edit_text_out);
		 * String message = view.getText().toString(); isHex=false;
		 * sendMessage(message); } });
		 */

		// Initialize the send button with a listener that for click events
		/*
		 * mSendButton2 = (Button) findViewById(R.id.button_send2);
		 * mSendButton2.setText("SendHex"); mSendButton2.setOnClickListener(new
		 * OnClickListener() { public void onClick(View v) { // Send a message
		 * using content of the edit text widget TextView view = (TextView)
		 * findViewById(R.id.edit_text_out2); String message =
		 * view.getText().toString(); isHex=true; try { sendHexMessage(message);
		 * } catch (IOException e) { // TODO Auto-generated catch block
		 * e.printStackTrace(); } } });
		 */

		// Initialize the BluetoothChatService to perform bluetooth connections
		mChatService = new BluetoothChatService(this, mHandler);

		// Initialize the buffer for outgoing messages
		mOutStringBuffer = new StringBuffer("");
	}

	@Override
	public synchronized void onPause() {
		super.onPause();
		if (D)
			Log.e(TAG, "- ON PAUSE -");
	}

	@Override
	public void onStop() {
		super.onStop();
		if (D)
			Log.e(TAG, "-- ON STOP --");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// Stop the Bluetooth chat services
		if (mChatService != null)
			mChatService.stop();
		if (D)
			Log.e(TAG, "--- ON DESTROY ---");
	}

	@SuppressLint("NewApi")
	private void ensureDiscoverable() {
		if (D)
			Log.d(TAG, "ensure discoverable");
		if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(
					BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(discoverableIntent);
		}
	}

	/**
	 * Sends a message.
	 * 
	 * @param message
	 *            A string of text to send.
	 */
	/*
	 * private void sendMessage(String message) { // Check that we're actually
	 * connected before trying anything if (mChatService.getState() !=
	 * BluetoothChatService.STATE_CONNECTED) { Toast.makeText(this,
	 * R.string.not_connected, Toast.LENGTH_SHORT).show(); return; }
	 * 
	 * // Check that there's actually something to send if (message.length() >
	 * 0) { // Get the message bytes and tell the BluetoothChatService to write
	 * byte[] send = message.getBytes(); mChatService.write(send);
	 * 
	 * // Reset out string buffer to zero and clear the edit text field
	 * mOutStringBuffer.setLength(0); mOutEditText.setText(mOutStringBuffer); }
	 * }
	 */

	/*
	 * private void sendHexMessage(String message) throws IOException { // Check
	 * that we're actually connected before trying anything if
	 * (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
	 * Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
	 * return; }
	 * 
	 * // Check that there's actually something to send if (message.length() >
	 * 0) { // Get the message bytes and tell the BluetoothChatService to write
	 * //byte[] send = hexStringToBytes(message); // mChatService.write(send);
	 * int[] send =new int[10]; for(int i=0;i<10;i++){ send[i]=i;}
	 * mChatService.write2(send);
	 * 
	 * // Reset out string buffer to zero and clear the edit text field
	 * mOutStringBuffer.setLength(0); mOutEditText.setText(mOutStringBuffer); }
	 * }
	 */

	/**
	 * Convert hex string to byte[]
	 * 
	 * @param hexString
	 *            the hex string
	 * @return byte[]
	 */
	public static byte[] hexStringToBytes(String hexString) {
		if (hexString == null || hexString.equals("")) {
			return null;
		}
		hexString = hexString.toUpperCase();
		int length = hexString.length() / 2;
		char[] hexChars = hexString.toCharArray();
		byte[] d = new byte[length];
		for (int i = 0; i < length; i++) {
			int pos = i * 2;
			d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
		}
		return d;
	}

	private static int charToByte(char c) {
		// TODO Auto-generated method stub
		return 0;
	}

	// The action listener for the EditText widget, to listen for the return key
	/*
	 * private TextView.OnEditorActionListener mWriteListener = new
	 * TextView.OnEditorActionListener() { public boolean
	 * onEditorAction(TextView view, int actionId, KeyEvent event) { // If the
	 * action is a key-up event on the return key, send the message if (actionId
	 * == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
	 * String message = view.getText().toString(); sendMessage(message); } if(D)
	 * Log.i(TAG, "END onEditorAction"); return true; } };
	 */
	// The Handler that gets information back from the BluetoothChatService
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case  MESSAGE_TIME:
				 int s=(int) time;      
				 int N = s/3600;       
				 s = s%3600;       
				 int K = s/60;       
				 s = s%60;       
				 int M = s;       
				 System.out.println("时间是："+N+"小时 "+K+"分钟 "+M+"秒"); 
				tv_03.setText(String.valueOf(N+":"+K+":"+M));

			case MESSAGE_STATE_CHANGE:
				if (D)
					Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
				switch (msg.arg1) {
				case BluetoothChatService.STATE_CONNECTED:
					mTitle.setText(R.string.title_connected_to);
					mTitle.append(mConnectedDeviceName);
					mConversationArrayAdapter.clear();

					break;
				case BluetoothChatService.STATE_CONNECTING:
					mTitle.setText(R.string.title_connecting);
					break;
				case BluetoothChatService.STATE_LISTEN:
				case BluetoothChatService.STATE_NONE:
					mTitle.setText(R.string.title_not_connected);
					break;
				}
				break;
			case MESSAGE_WRITE:
				byte[] writeBuf = (byte[]) msg.obj;
				String writeMessage = null;
				// construct a string from the buffer
				// String writeMessage = new String(writeBuf);
				if (isHex == true) {
					try {
						writeMessage = getHexString(writeBuf);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					writeMessage = new String(writeBuf);
				}

				mConversationArrayAdapter.add("Me:  " + writeMessage);
				break;
			case MESSAGE_READ:
				byte[] readBuf = (byte[]) msg.obj;
				String readMessage = null;
				if (isHex == true) {
					try {
						readMessage = getHexString(readBuf);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					mConversationArrayAdapter.add(mConnectedDeviceName + ":  "
							+ readMessage);
				} else {
					readMessage = new String(readBuf, 0, msg.arg1);
					readMessage1 = readMessage1 + readMessage;

					// mConversationArrayAdapter.add(readMessage);
					/*
					 * if(readMessage1.length() >15){ int W1=
					 * readMessage1.indexOf("W"); //int T1=
					 * readMessage1.indexOf("T"); String weight=
					 * readMessage1.substring(W1+1,W1+5);
					 * mConversationArrayAdapter.add("weight : " + weight);
					 * String temperature= readMessage1.substring(W1+5,W1+9);
					 * mConversationArrayAdapter.add("temperature : " +
					 * temperature);
					 * mConversationArrayAdapter.add(readMessage1);
					 * readMessage1="";
					 * 
					 * }
					 */
					tv_01 = (TextView) findViewById(R.id.tv_01);
					tv_02 = (TextView) findViewById(R.id.tv_02);

					if (readMessage1.length() > 15 ) {
						int W1 = readMessage1.indexOf("W");
						// int T1= readMessage1.indexOf("T");
						weight = readMessage1.substring(W1 + 1, W1 + 5);

						temperature = readMessage1.substring(W1 + 5, W1 + 9);

						mConversationArrayAdapter.add("temperature : "
								+ temperature);
						mConversationArrayAdapter.add(readMessage1);
						mConversationArrayAdapter.add("weight : " + weight);

						tv_01.setText("weight" + weight);
						tv_02.setText("temperature" + temperature);
						readMessage1 = "";
						/*
						 * if(btn_02.isPressed()){
						 * 
						 * weight = readMessage1.substring(W1 + 1, W1 + 5);
						 * 
						 * temperature = readMessage1.substring(W1 + 5, W1 + 9);
						 * weight=""; temperature="";
						 * mConversationArrayAdapter.add("temperature : " +
						 * temperature);
						 * mConversationArrayAdapter.add(readMessage1);
						 * mConversationArrayAdapter.add("weight : " + weight);
						 * tv_01.setText("weight" + weight);
						 * tv_02.setText("temperature" + temperature);
						 * readMessage1 = ""; }
						 */
                          
					}

				}
				break;
			case MESSAGE_DEVICE_NAME:
				// save the connected device's name
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				Toast.makeText(getApplicationContext(),
						"Connected to " + mConnectedDeviceName,
						Toast.LENGTH_SHORT).show();
				break;
			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
						.show();
				break;
			}
		}
	};

	public static String getHexString(byte[] b) throws Exception {

		String result = "";

		for (int i = 0; i < BluetoothChat.revBytes; i++) {

			result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);

		}

		return result;

	}

	protected String readMessage(byte[] readBuf) {
		// TODO Auto-generated method stub
		return null;
	}

	// 十六进制字节数组转换为ASCII字符串
	public String bytes2HexString(byte[] b) {
		String ret = "";
		for (int i = 0; i < b.length; i++) {
			String hex = Integer.toHexString(b[i] & 0xFF);
			if (hex.length() == 1) {
				hex = '0' + hex;
			}
			ret += hex.toUpperCase();
		}
		return ret;
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (D)
			Log.d(TAG, "onActivityResult " + resultCode);
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				// Get the device MAC address
				String address = data.getExtras().getString(
						DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				// Get the BLuetoothDevice object
				BluetoothDevice device = mBluetoothAdapter
						.getRemoteDevice(address);
				// Attempt to connect to the device
				mChatService.connect(device);
			}
			break;
		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled, so set up a chat session
				setupChat();
			} else {
				// User did not enable Bluetooth or an error occured
				Log.d(TAG, "BT not enabled");
				Toast.makeText(this, R.string.bt_not_enabled_leaving,
						Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.option_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.scan:
			// Launch the DeviceListActivity to see devices and do scan
			Intent serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
			return true;
		case R.id.discoverable:
			// Ensure this device is discoverable by others
			ensureDiscoverable();
			return true;
		}
		return false;
	}

	@Override
	public void onClick(View v) {

		switch (v.getId()) {
		/*
		 * case R.id.btn_01: Toast.makeText(BluetoothChat.this, "button1被点击了！",
		 * 20).show(); /* //自定义对话框；
		 * 
		 * 
		 * Dialog dialog = new Dialog(this);
		 * dialog.setContentView(R.layout.dialog_layout);
		 * 
		 * dialog.setTitle("情景模式设定"); dialog.setOnShowListener(new
		 * OnClickListener() {
		 * 
		 * @Override public void onClick(View v) { // TODO Auto-generated method
		 * stub
		 * 
		 * } });
		 * 
		 * 
		 * 
		 * dialog.getLayoutInflater()。 Window dialogWindow = dialog.getWindow();
		 * WindowManager.LayoutParams lp = dialogWindow.getAttributes();
		 * dialogWindow.setGravity(Gravity.LEFT | Gravity.TOP); lp.x = 100; //
		 * 新位置X坐标 lp.y = 100; // 新位置Y坐标 lp.width = 300; // 宽度 lp.height = 300;
		 * // 高度 lp.alpha = 0.7f; // 透明度 dialogWindow.setAttributes(lp);
		 * dialog.show();
		 * 
		 * 
		 * new AlertDialog.Builder(this)
		 * 
		 * .setTitle("模式设置")
		 * 
		 * .setIcon(R.drawable.icon) .setSingleChoiceItems( new String[] {
		 * "1分钟", "5分钟", "10分钟", "15分钟" },
		 * 
		 * 0, new DialogInterface.OnClickListener() {
		 * 
		 * public void onClick(DialogInterface dialog,
		 * 
		 * int which) {
		 * 
		 * dialog.dismiss();
		 * 
		 * Toast.makeText(BluetoothChat.this,
		 * 
		 * "你选择了: " + which, 5).show();
		 * 
		 * }
		 * 
		 * }).setNegativeButton("取消", null).show();
		 * 
		 * break;
		 */
		/*
		 * case R.id.btn_02: Toast.makeText(BluetoothChat.this, "button2被点击了！",
		 * 20).show();
		 * 
		 * if (mConnectedDeviceName != null) { weight = "0"; temperature = "0";
		 * Integer.parseInt(weight); Integer.parseInt(temperature);
		 * mConversationArrayAdapter.add("temperature : " + temperature);
		 * mConversationArrayAdapter.add(readMessage1);
		 * mConversationArrayAdapter.add("weight : " + weight);
		 * tv_01.setText("weight" + weight); tv_02.setText("temperature" +
		 * temperature); readMessage1 = ""; Toast.makeText(BluetoothChat.this,
		 * "weight" + weight + "temperature" + temperature, 20) .show(); } else
		 * { Toast.makeText(BluetoothChat.this, "请连接设备！", 20).show(); }
		 * 
		 * break;
		 */
		case R.id.btn_03:
			Toast.makeText(BluetoothChat.this, "开始计时！", 20).show();
			IntentFilter filter = new IntentFilter();
			filter.addAction("s.s.s.s");
			BroadcastReceiver receiver = new MyTimeRceiver();
			registerReceiver(receiver, filter);
            if(flag==true){
            	mIntent.setAction("x.x.x.x");
    			mIntent.putExtra("flag", flag);
    			startService(mIntent);
    			flag = false;
            }
			
			break;
		case R.id.btn_04:
			Toast.makeText(BluetoothChat.this, "计时结束！", 20).show();
			if(flag!=true){
				
				mIntent.setAction("x.x.x.x");
				mIntent.putExtra("flag", flag);
				startService(mIntent);
				flag = true;
			}
			break;

		case R.id.menu_tv_01:

			mMenu.toggle();
            
			if (mConnectedDeviceName != null) {
				weight = "0";
				temperature = "0";
				Integer.parseInt(weight);
				Integer.parseInt(temperature);
				mConversationArrayAdapter.add("temperature : " + temperature);
				mConversationArrayAdapter.add(readMessage1);
				mConversationArrayAdapter.add("weight : " + weight);
				tv_01.setText("weight" + weight);
				tv_02.setText("temperature" + temperature);
				readMessage1 = "";
				Toast.makeText(BluetoothChat.this,
						"weight" + weight + "temperature" + temperature, 20)
						.show();
			} else {
				
				Toast.makeText(BluetoothChat.this, "请连接设备！", 20).show();
			}

			break;
		case R.id.menu_tv_02:

			new AlertDialog.Builder(this)

					.setTitle("模式设置")

					.setIcon(R.drawable.icon)
					.setSingleChoiceItems(
							new String[] { "1分钟", "5分钟", "10分钟", "15分钟" },

							0, new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog,

								int which) {

									dialog.dismiss();

									Toast.makeText(BluetoothChat.this,

									"你选择了: " + which, 5).show();

								}

							}).setNegativeButton("取消", null).show();
			break;
		/*
		 * case R.id.menu_tv_03: Intent intent1 = new Intent(BluetoothChat.this,
		 * BrewClockActivity.class); startActivity(intent1);
		 * Toast.makeText(BluetoothChat.this, "菜单计时！", 20).show(); break;
		 */
		case R.id.menu_tv_04:
			Toast.makeText(BluetoothChat.this, "菜单结束！", 20).show();
			finish();
			break;

		case R.id.menu_tv_05:
			Toast.makeText(BluetoothChat.this, "有关APP简介对话框！", 20).show();

			// 自定义对话框
			Dialog dialog = new Dialog(this);
			dialog.setContentView(R.layout.dialog_layout);

			dialog.setTitle("APP详情");
			/*
			 * dialog.setOnShowListener(new OnClickListener() {
			 * 
			 * @Override public void onClick(View v) { // TODO Auto-generated
			 * method stub
			 * 
			 * } });
			 */
			dialog.getLayoutInflater();
			Window dialogWindow = dialog.getWindow();
			WindowManager.LayoutParams lp = dialogWindow.getAttributes();
			dialogWindow.setGravity(Gravity.LEFT | Gravity.TOP);
			lp.x = 100;// 新位置X坐标
			lp.y = 100; // 新位置Y坐标
			lp.width = 300; // 宽度
			lp.height = 300; // 高度
			lp.alpha = 0.7f; // 透明度
			dialogWindow.setAttributes(lp);
			dialog.show();

			break;
		}

	}

	class MyTimeRceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if ("s.s.s.s".equals(intent.getAction())) {
				time = intent.getLongExtra("data", 0);
				
				mHandler.sendEmptyMessage(0);

			}

		}

	}
}