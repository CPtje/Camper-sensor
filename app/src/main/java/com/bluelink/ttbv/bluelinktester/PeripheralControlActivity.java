package com.bluelink.ttbv.bluelinktester;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class PeripheralControlActivity extends Activity {

	public static final String EXTRA_NAME = "name";
	public static final String EXTRA_ID = "id";

	private String mDeviceName;
	private String mDeviceAddress;
	private BleAdapterService mBluetoothLeService;
	private Timer mTimer;
	private boolean mSoundAlarmOnDisconnect = false;
	private int alert_level;
	private int battAvgCount=0;
	private int percentAVG;
	private int defTimeOut;


	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName,
				IBinder service) {
			mBluetoothLeService = ((BleAdapterService.LocalBinder) service)
					.getService();
			mBluetoothLeService.setActivityHandler(mMessageHandler);
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mBluetoothLeService = null;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_peripheral_control);

		defTimeOut = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, -1);
		Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 120000);


		// read intent data
		final Intent intent = getIntent();
		mDeviceName = intent.getStringExtra(EXTRA_NAME);
		mDeviceAddress = intent.getStringExtra(EXTRA_ID);

		// show the device name
		Button mButton=(Button)findViewById(R.id.nameButton);
		mButton.setText(mDeviceName);

		mButton.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				((Button) PeripheralControlActivity.this.findViewById(R.id.programFWVButton)).setVisibility(View.INVISIBLE);
				((TextView) PeripheralControlActivity.this.findViewById(R.id.editFWVText)).setVisibility(View.INVISIBLE);

				((Button) PeripheralControlActivity.this.findViewById(R.id.programSNButton)).setVisibility(View.VISIBLE);
				((TextView) PeripheralControlActivity.this.findViewById(R.id.editText)).setVisibility(View.VISIBLE);
				return true;
			}
		});

		Button FWVButton=(Button)findViewById(R.id.FWVButton);
/*		FWVButton.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				((Button) PeripheralControlActivity.this.findViewById(R.id.programSNButton)).setVisibility(View.INVISIBLE);
				((TextView) PeripheralControlActivity.this.findViewById(R.id.editText)).setVisibility(View.INVISIBLE);

				((Button) PeripheralControlActivity.this.findViewById(R.id.programFWVButton)).setVisibility(View.VISIBLE);
				((TextView) PeripheralControlActivity.this.findViewById(R.id.editFWVText)).setVisibility(View.VISIBLE);
				return true;
			}
		});
		*/

		// hide the coloured rectangle used to show green/amber/red rssi
		// distance
		((LinearLayout) this.findViewById(R.id.rectangle))
				.setVisibility(View.INVISIBLE);
		// set low, mid, high rssi tolerance button colors
		//((Button) this.findViewById(R.id.lowButton)).getBackground()
		//		.setColorFilter(0xFFFF0000, PorterDuff.Mode.MULTIPLY);
		//((Button) this.findViewById(R.id.midButton)).getBackground()
		//		.setColorFilter(0xFF00FF00, PorterDuff.Mode.MULTIPLY);
		//((Button) this.findViewById(R.id.highButton)).getBackground()
		//		.setColorFilter(0xFFFF9000, PorterDuff.Mode.MULTIPLY);

		// disable the noise button
/*CP		((Button) PeripheralControlActivity.this.findViewById(R.id.noiseButton))
				.setEnabled(false);
*/
/*CP
		Switch share_switch = (Switch) this.findViewById(R.id.switch1);
		share_switch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView,
										 boolean isChecked) {
				if (mBluetoothLeService != null) {
					mBluetoothLeService.setShare_with_server(isChecked);
					if (!isChecked) {
						showMsg("Switched off sharing proximity data with the Proximity Monitoring Service");
						// write 0,0 to cause Arduino to switch off all LEDs
						if (mBluetoothLeService.writeCharacteristic(
								BleAdapterService.PROXIMITY_MONITORING_SERVICE_UUID,
								BleAdapterService.CLIENT_PROXIMITY_CHARACTERISTIC,
								new byte[]{0, 0})) {
						} else {
							showMsg("Failed to inform Arduino sharing has been disabled");
						}
					}
				}
			}
		});
*/
		// connect to the Bluetooth smart service
		Intent gattServiceIntent = new Intent(this, BleAdapterService.class);
		bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

		if (mBluetoothLeService!=null) showMsg("BLEservice available");

		// connect to device
		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				// Connect to device after 1s = 1000ms
				((Button) PeripheralControlActivity.this.findViewById(R.id.connectButton)).performClick();
			}
		}, 1000);



	}

	@Override
	public void onBackPressed(){
		stopTimer();
		//unbindService(mServiceConnection);
		//mBluetoothLeService = null;
		Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, defTimeOut);
		super.onBackPressed();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		stopTimer();
		//unbindService(mServiceConnection);
		//mBluetoothLeService = null;
		Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, defTimeOut);

	}

	// set low rssi tolerance
	public void onLow(View view) {
		if (mBluetoothLeService.writeCharacteristic(
				BleAdapterService.LOSS_LINK_SERVICE_UUID,
				BleAdapterService.ALERT_LEVEL_CHARACTERISTIC, new byte[] { 0 })) {
			setAlertLevel(0);
			showMsg("alert_level set to 0");
		} else {
			showMsg("Failed to set alert_level set to 0");
		}
	}

	// set mid rssi tolerance
	public void onMid(View view) {
		if (mBluetoothLeService.writeCharacteristic(
				BleAdapterService.LOSS_LINK_SERVICE_UUID,
				BleAdapterService.ALERT_LEVEL_CHARACTERISTIC, new byte[] { 1 })) {
			setAlertLevel(1);
			showMsg("alert_level set to 1");
		} else {
			showMsg("Failed to set alert_level set to 1");
		}
	}

	// set high rssi tolerance
	public void onHigh(View view) {
		if (mBluetoothLeService.writeCharacteristic(
				BleAdapterService.LOSS_LINK_SERVICE_UUID,
				BleAdapterService.ALERT_LEVEL_CHARACTERISTIC, new byte[] { 2 })) {
			setAlertLevel(2);
			showMsg("alert_level set to 2");
		} else {
			showMsg("Failed to set alert_level set to 2");
		}
	}


	public void onNameClick(View view) {

		//((Button) PeripheralControlActivity.this.findViewById(R.id.programSNButton)).setVisibility(View.VISIBLE);
		//((TextView) PeripheralControlActivity.this.findViewById(R.id.editText)).setVisibility(View.VISIBLE);

	}


	public void onProgramSN(View view) {
		//String SN="14/30555";//
		//byte[] SN_Command=SN.getBytes();
		//setContentView(R.layout.activity_peripheral_control);
		TextView SN= (TextView) findViewById(R.id.editText);
		String SN_String = SN.getText().toString();

		showMsg("SN to program: " + SN_String);
		byte[] CommandHeader=hexStringtoByteArray("FF");
		byte[] SN_bytes=SN_String.getBytes();

		byte[] SN_Command= new byte[CommandHeader.length + SN_bytes.length];
		System.arraycopy(CommandHeader,0,SN_Command,0,CommandHeader.length);
		System.arraycopy(SN_bytes,0,SN_Command,CommandHeader.length,SN_bytes.length);

		//byte[] SN_Command=new byte[] {-1, 49, 52, 47, 51, 48, 57, 57, 57}; //{-1, 49, 52, 47, 51, 48, 57, 57, 57}

		//SN_Command=hexStringtoByteArray("FF")+ASCII_SN;//    +SN_String); //{0xF, 0xF, 0x46, 0x31, 0x36, 0x2f, 0x33, 0x30, 0x35, 0x35, 0x35}; "FF31342F3330353535"

		if (mBluetoothLeService.writeCharacteristic(
				BleAdapterService.SPO_SN_SERVICE_UUID, BleAdapterService.SPO_SN_DESCRIPTOR, SN_Command)) {
			showMsg("Programmed SN: " + Arrays.toString(SN_Command));
			this.onBackPressed();
		}
			else {
			showMsg ("Error programming SN");
		}
	}

	public void onProgramFWV(View view) {

		TextView FWV= (TextView) findViewById(R.id.editFWVText);
		String FWV_String = FWV.getText().toString();

		showMsg("FWV to program: " + FWV_String);
		byte[] CommandHeader=hexStringtoByteArray("FF"); //???????????
		byte[] FWV_bytes=FWV_String.getBytes();

		byte[] FWV_Command= new byte[CommandHeader.length + FWV_bytes.length];
		System.arraycopy(CommandHeader,0,FWV_Command,0,CommandHeader.length);
		System.arraycopy(FWV_bytes,0,FWV_Command,CommandHeader.length,FWV_bytes.length);

		//byte[] SN_Command=new byte[] {-1, 49, 52, 47, 51, 48, 57, 57, 57}; //{-1, 49, 52, 47, 51, 48, 57, 57, 57}

		//SN_Command=hexStringtoByteArray("FF")+ASCII_SN;//    +SN_String); //{0xF, 0xF, 0x46, 0x31, 0x36, 0x2f, 0x33, 0x30, 0x35, 0x35, 0x35}; "FF31342F3330353535"

		if (mBluetoothLeService.writeCharacteristic(
				BleAdapterService.SPO_SN_SERVICE_UUID, BleAdapterService.SPO_SN_DESCRIPTOR, FWV_Command)) {
			showMsg("Programmed FW Version: " + Arrays.toString(FWV_Command));
			this.onBackPressed();
		}
		else {
			showMsg ("Error programming FW Version");
		}
	}

	private byte[] hexStringtoByteArray(String s) {
			int len = s.length();
			byte[] data = new byte[len / 2];
			for (int i = 0; i < len; i += 2) {
				data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
						+ Character.digit(s.charAt(i+1), 16));
			}
			return data;
	}


	public void onConnect(View view) {
		//showMsg("onConnect");
		if (mBluetoothLeService != null) {
			if (mBluetoothLeService.connect(mDeviceAddress)) {
				((Button) PeripheralControlActivity.this
						.findViewById(R.id.connectButton)).setEnabled(false);
			} else {
				showMsg("onConnect: failed to connect");
			}
		} else {
			showMsg("onConnect: mBluetoothLeService=null");
		}
	}

	// Service message handler//////////////////
	protected Handler mMessageHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {

		Bundle bundle;

			switch (msg.what) {
			case BleAdapterService.GATT_CONNECTED:
				((Button) PeripheralControlActivity.this
						.findViewById(R.id.connectButton)).setEnabled(false);
				((Button) PeripheralControlActivity.this
						.findViewById(R.id.programSNButton)).setEnabled(true);
				((Button) PeripheralControlActivity.this
						.findViewById(R.id.programFWVButton)).setEnabled(true);
				// we're connected

				mBluetoothLeService.logBondState();

				System.out
						.println("XXXX reading SN");
				/*Read FW Version
				PeripheralControlActivity.this.mBluetoothLeService
						.readCharacteristic(
								BleAdapterService.SPO_SERVICE_UUID,
								BleAdapterService.FW_VERSION_CHARACTERISTIC);

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} */

				/*start reading SpO2 values
				PeripheralControlActivity.this.mBluetoothLeService //CP Lets see if we also can read the SpO module characteristic
						.readCharacteristic(
								BleAdapterService.SPO_SERVICE_UUID,
								BleAdapterService.SPO_CHARACTERISTIC_UUID);
								*/


				break;
			case BleAdapterService.GATT_DISCONNECT:
				((Button) PeripheralControlActivity.this
						.findViewById(R.id.connectButton)).setEnabled(true);
				PeripheralControlActivity.this.stopTimer();
				if (PeripheralControlActivity.this.mSoundAlarmOnDisconnect) {
					PeripheralControlActivity.this.playSound();
					// set to false so when its fluctuating between
					// connected and disconnected it doesnt keep sounding the
					// alarm
					PeripheralControlActivity.this.mSoundAlarmOnDisconnect = false;
				}
				break;
			case BleAdapterService.GATT_SERVICES_DISCOVERED:

				System.out.println("XXXX Services discovered");
				mBluetoothLeService.logBondState();
				// enable the programSN button
				((Button) PeripheralControlActivity.this
						.findViewById(R.id.programSNButton)).setEnabled(true);

				// show the rssi distance colored rectangle
				((LinearLayout) PeripheralControlActivity.this
						.findViewById(R.id.rectangle))
						.setVisibility(View.VISIBLE);
				// start off the rssi reading timer
				PeripheralControlActivity.this.startReadRssiTimer();



				break;
			case BleAdapterService.GATT_CHARACTERISTIC_READ:
				bundle = msg.getData();
				if (bundle.get(BleAdapterService.PARCEL_UUID).toString()
						.equals(BleAdapterService.SPO_CHARACTERISTIC_UUID)) {
					showMsg(" ");
					//showMsg("GATT characteristic read from the SPO...");
					byte[] SPOparcel = (bundle.getByteArray(BleAdapterService.PARCEL_VALUE));
					//byte[] SPOresult= Arrays.copyOfRange(SPOparcel,4,4);
					//String result = new BigInteger(1, SPOparcel).toString(16);
					//int place=(result.indexOf("null"));

					byte index=SPOparcel[0]; //[place+1]
					switch(index) {
						case 0: case 15:case 45:case 60: { // = 4 Hz
							byte status = SPOparcel[1]; //place + 2
							byte MSB = SPOparcel[2]; //place + 3
							byte LSB = SPOparcel[3]; //place + 4
							byte SPO2 = SPOparcel[4]; //place + 5
							if ((status < -124)&&((MSB * 128 + LSB)<500)) {
								((TextView) findViewById(R.id.PRTextView)).setText("Pulserate = " + Integer.toString(MSB * 128 + LSB) + " BPM");
								((TextView) findViewById(R.id.SpO2TextView)).setText("SpO2 = " + Integer.toString(SPO2) + " %");
							} else {
								((TextView) findViewById(R.id.PRTextView)).setText("Pulserate = Invalid");
								((TextView) findViewById(R.id.SpO2TextView)).setText("SpO2 = Invalid");
							}
							break;
						}
						case 30: {
							int BattSample = (SPOparcel[4]*256)+SPOparcel[3]; //place + 4
							//showMsg("Battsample:"+BattSample);
							int Percent=0;

							if (BattSample > 24443)
							{
								Percent = ((BattSample - 24444) / 30) + 13;
							}
							else
							{
								Percent = (BattSample - 22132) / 178;
							}

							if(Percent < 0) Percent = 0;
							if(Percent > 100) Percent = 100;

							if (battAvgCount<4) battAvgCount++;
							if (battAvgCount==1) {
								percentAVG=Percent;
							} else {
								percentAVG=(((3*percentAVG)+Percent)/4);
							}

							/*

							SpO_Battery.Sum -= SpO_Battery.Buffer[SpO_Battery.Pos];
							SpO_Battery.Sum += (char)Percent;

							SpO_Battery.Buffer[SpO_Battery.Pos] = (char)Percent;
							SpO_Battery.Pos += 1;

							if (SpO_Battery.Pos >= SPO_BATTERY_AVERAGE)
							{
								SpO_Battery.Pos = 0;
								SpO_Battery.Init = true;
							}

							if(!SpO_Battery.Init) return (char)Percent;
							*/
							((TextView) findViewById(R.id.BattTextView)).setText("Battery level = " + Integer.toString(percentAVG) + " %");
							break;
						}
						default: break;
					}

					//Handle pulsewave here from SPOparcel[5] to SPOparcel[19], these are 16 bits values



					//String SPOresult=result.substring((place+1), (place+12));
					//showMsg("Index:"+index + "\n");
					//appendMsg("Status:" + status + "\n");
					//appendMsg("Status:"+status + "\n");
					//appendMsg("HR-MSB:"+MSB + "\n");
					//appendMsg("HR-LSB:"+LSB + "\n");
					//appendMsg("SPO2:"+SPO2 + "\n");
					//showMsg("Values after 8000="+ SPOresult+"\n");
					//byte[] b = bundle
					//		.getByteArray(BleAdapterService.PARCEL_VALUE);

					//PeripheralControlActivity.this.setAlertLevel(b[0]);  //CP was PeripheralControlActivity.this.setAlertLevel(b[0]);
				}

				if (bundle.get(BleAdapterService.PARCEL_UUID).toString()
							.equals(BleAdapterService.FW_VERSION_CHARACTERISTIC)) {
						byte[] FWVparcel = (bundle.getByteArray(BleAdapterService.PARCEL_VALUE));

					((TextView) findViewById(R.id.FWVButton)).setText("FW version: " + Integer.toString(FWVparcel[0])+"."+Integer.toString(FWVparcel[1]));

					}

				break;
			case BleAdapterService.NOTIFICATION_SERVICE:
				bundle=msg.getData();
				if (bundle.get(BleAdapterService.PARCEL_UUID).toString()
						.equals(BleAdapterService.SPO_CHARACTERISTIC_UUID)) {
					String text=bundle.getString(BleAdapterService.PARCEL_VALUE);

					showMsg(text);
					//byte[] b = bundle
					//		.getByteArray(BleAdapterService.PARCEL_VALUE);

					//PeripheralControlActivity.this.setAlertLevel(b[0]);  //CP was PeripheralControlActivity.this.setAlertLevel(b[0]);
				}
				break;
			case BleAdapterService.GATT_REMOTE_RSSI:
				bundle = msg.getData();
				int rssi = bundle.getInt(BleAdapterService.PARCEL_RSSI);
				PeripheralControlActivity.this.updateRssi(rssi);
				break;
			case BleAdapterService.MESSAGE:
				bundle = msg.getData();
				String text = bundle.getString(BleAdapterService.PARCEL_TEXT);
				showMsg(text);
			}
		}

	};

	// helper functions ////////////////

	private void setAlertLevel(int level) {
		alert_level = level;
		// update UI
		((TextView) findViewById(R.id.alertTextView)).setText("Alert Level = "
				+ Integer.toString(level));
		// to sound alarm when set to high alert and device disconnects
		mSoundAlarmOnDisconnect = (level == 2) ? true : false;
	}

	private void playSound() {
		MediaPlayer mp = new MediaPlayer();
		AssetFileDescriptor afd = getResources().openRawResourceFd(R.raw.beep);
		try {
			mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(),
					afd.getLength());
			mp.prepare();
			mp.start();
			afd.close();

		} catch (IllegalArgumentException e) {
		} catch (IllegalStateException e) {
		} catch (IOException e) {
		}
	}

	// rssi read timer functions //////////

	private void startReadRssiTimer() {
		mTimer = new Timer();
		mTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				mBluetoothLeService.readRemoteRssi();
			}

		}, 0, 2000);
	}

	private void stopTimer() {
		if (mTimer != null) {
			mTimer.cancel();
			mTimer = null;
		}
	}

	private void updateRssi(int rssi) {
		((TextView) findViewById(R.id.rssiTextView)).setText("RSSI = "
				+ Integer.toString(rssi));
		LinearLayout layout = ((LinearLayout) PeripheralControlActivity.this
				.findViewById(R.id.rectangle));

		byte proximity_band = 3;
		if (rssi < -90) {
			layout.setBackgroundColor(0xFFFF0000);
		} else if (rssi < -80) {
			layout.setBackgroundColor(0xFFFFFF00);
			proximity_band = 2;
		} else {
			layout.setBackgroundColor(0xFF00FF00);
			proximity_band = 1;
		}
		layout.invalidate();
		/*
		if (mBluetoothLeService.isShare_with_server()) {
			showMsg("sharing proximity data with the Proximity Monitoring Service");
			if (mBluetoothLeService.writeCharacteristic(
					BleAdapterService.PROXIMITY_MONITORING_SERVICE_UUID,
					BleAdapterService.CLIENT_PROXIMITY_CHARACTERISTIC,
					new byte[] { proximity_band, (byte) rssi })) {
				showMsg("proximity data shared: proximity_band="
						+ proximity_band + ",rssi=" + rssi);
			} else {
				showMsg("Failed to share proximity data");
			}
		} */
	}

	private void showMsg(String msg) {
		System.out.println("XXXX " + msg);
		((TextView) this.findViewById(R.id.msgTextView)).setText(msg);
	}

	private void appendMsg(String msg) {
		System.out.println("XXXX " + msg);
		((TextView) this.findViewById(R.id.msgTextView)).append(msg);
	}

}