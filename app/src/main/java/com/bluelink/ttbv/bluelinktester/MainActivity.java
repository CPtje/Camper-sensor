package com.bluelink.ttbv.bluelinktester;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter=null;
    private boolean mScanning=false;
    private Handler mHandler=new Handler();
    private ListAdapter mLeDeviceListAdapter;
    private static final long SCAN_TIMEOUT=5000;

    static class ViewHolder{
        public TextView text;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       // requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TextView messageBox=(TextView) findViewById(R.id.messageBox);
        messageBox.setText("Click the Bluetooth button to start scanning.\n");

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.scanButton);
        fab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorPrimary)));
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Scanning for Bluetooth devices...", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                scanLeDevice(!mScanning);
            }
        });

        // Initializes Bluetooth adapter.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE Not Supported",
                    Toast.LENGTH_SHORT).show();
            finish();
        }
        final BluetoothManager bluetoothManager=(BluetoothManager)
                getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter=bluetoothManager.getAdapter();

        mLeDeviceListAdapter = new ListAdapter();

        ListView listView = (ListView) this.findViewById(R.id.deviceList);
        listView.setAdapter(mLeDeviceListAdapter);


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                if (mScanning) {
                    setScanState(false);
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }

                BluetoothDevice device = mLeDeviceListAdapter
                        .getDevice(position);
                Intent intent = new Intent(MainActivity.this,
                        PeripheralControlActivity.class);
                intent.putExtra(PeripheralControlActivity.EXTRA_NAME,
                        device.getName());
                intent.putExtra(PeripheralControlActivity.EXTRA_ID,
                        device.getAddress());//
                startActivity(intent);

            }
        });

    }

    public void onWindowFocusChanged(boolean hasFocus) {

        super.onWindowFocusChanged(hasFocus);

        if(hasFocus) {
            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.scanButton);
            fab.performClick();

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setScanState(boolean value) {
        mScanning = value;
        // setProgressBarIndeterminateVisibility(value);
        // ((FloatingActionButton) this.findViewById(R.id.scanButton)).set    //Some action on the scanButton
       }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // scan for SCAN)TIMEOUT
             mHandler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    setScanState(false);

                     //       TextView messageBox = (TextView) findViewById(R.id.messageBox);
                     //       messageBox.append("Scanning stopped...\n");

                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    }
                }
                ,SCAN_TIMEOUT);

        /*    runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView messageBox = (TextView) findViewById(R.id.messageBox);
                    messageBox.setText("Start scanning...\n");
                }
            });
*/
                setScanState(true);

                mLeDeviceListAdapter.clear();
                mLeDeviceListAdapter.notifyDataSetChanged();
                // pass in the link loss service id uuid to filter out devices that don't support it
                //UUID[] uuids = new UUID[1];
                //uuids[0]=UUID.fromString("00001803-0000-1000-8000-00805f9b34fb"); // or maybe:  "68BC39C8-601A-4C9D-90BA-C83FED6E0F7D"

            //TextView messageBox = (TextView) findViewById(R.id.messageBox);
            //messageBox.append(uuids+"\n");
            mBluetoothAdapter.startLeScan(mLeScanCallback); //was: (uuids, mLeScanCallback)
            }

            else

                {
                    setScanState(false);
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }

        public void onScan(View view) {
        // check Bluetooth is available and on
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView messageBox = (TextView) findViewById(R.id.messageBox);
                    messageBox.setText("Bluetooth is NOT enabled...\n");
                }
            });
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            return;
        }
        scanLeDevice(!mScanning);
    }

    /*
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());
            BluetoothDevice btDevice = result.getDevice();
           // connectToDevice(btDevice);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.i("ScanResult - Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }
    };

*/

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi,
                             byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLeDeviceListAdapter.addDevice(device);
                    mLeDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }
    };


    //adapter
    private class ListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;

        public ListAdapter() {
            super();
            mLeDevices=new ArrayList<BluetoothDevice>();
        }

        public void addDevice(BluetoothDevice device) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public  BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = MainActivity.this.getLayoutInflater().inflate(R.layout.list_row,null);
                viewHolder = new ViewHolder();
                viewHolder.text = (TextView) view.findViewById(R.id.textView);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }
            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            final String deviceAddress=device.getAddress();
            if (deviceName != null && deviceName.length()>0) {
                viewHolder.text.setText("\n"+deviceName+"\n"+ deviceAddress +"\n");

            }
            else {
                viewHolder.text.setText("\n"+"No device name."+"\n"+ deviceAddress +"\n");
            }
            return view;
            }
        }


    }

