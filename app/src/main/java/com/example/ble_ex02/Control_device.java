package com.example.ble_ex02;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothHidDeviceAppQosSettings;
import android.bluetooth.BluetoothHidDeviceAppSdpSettings;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.util.Range;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.example.ble_ex02.hid.descriptor_object;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;

import static java.lang.Integer.parseInt;

public class Control_device extends Activity {
    /**
     * ----------------------- GATT 프로파일 -----------------------
     **/
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private TextView mConnectionState;
    private TextView mDataField;
    private boolean mConnected = false;
    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BLEServuce mBluetoothLeService;
    private String LIST_NAME = "NAME";
    private String LIST_UUID = "UUID";
    /**------------------------------------------------------------  **/


    /**
     * ----------------------- HID 프로파일 ------------------------
     **/
    private BluetoothAdapter proxyadapter =
            BluetoothAdapter.getDefaultAdapter();
    private BluetoothHidDevice hidDevice;

    private BluetoothHidDeviceAppSdpSettings sdp;
    private BluetoothHidDeviceAppQosSettings qos;
    private BluetoothDevice btdevice;

    private ArrayList<BluetoothDevice> deviceList = new ArrayList<>();
    Spinner list_proxy;


    /**
     * ------------------------------------------------------------
     **/


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);


        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        mGattServicesList.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                                        int groupPosition, int childPosition, long id) {
                if (mGattServicesList != null) {
                    final BluetoothGattCharacteristic characteristic =
                            mGattCharacteristics.get(groupPosition).get(childPosition);
                    final int charaProp = characteristic.getProperties();
                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                        if (mNotifyCharacteristic != null) {
                            mBluetoothLeService.setCharacteristicNotification(
                                    mNotifyCharacteristic, false);
                            mNotifyCharacteristic = null;
                        }
                        mBluetoothLeService.readCharacteristic(characteristic);
                    }
                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                        mNotifyCharacteristic = characteristic;
                        mBluetoothLeService.setCharacteristicNotification(
                                characteristic, true);
                    }
                    return true;
                }

                return false;
            }
        });
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataField = (TextView) findViewById(R.id.data_value);

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattintent = new Intent(this, BLEServuce.class);
        bindService(gattintent, mServiceConnection, BIND_AUTO_CREATE);
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBluetoothLeService = ((BLEServuce.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                finish();
            }
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBluetoothLeService = null;
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
        }
        ProxyList();
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BLEServuce.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState("연결됨");
                invalidateOptionsMenu();
            } else if (BLEServuce.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState("연결끊김");
                invalidateOptionsMenu();
                clearUI();
            } else if (BLEServuce.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                updateConnectionState("연결됨(서비스호출)");
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BLEServuce.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BLEServuce.EXTRA_DATA));
                Log.d("fff", " ddddd " + BLEServuce.EXTRA_DATA);
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLEServuce.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BLEServuce.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BLEServuce.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BLEServuce.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void updateConnectionState(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(text);
            }
        });
    }

    private void clearUI() {
        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        mDataField.setText("No Data");
    }

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        //UUID uuid1 = UUID.fromString("d618d000-6000-1000-8000-000000000000");


        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
        int i = 0;

        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();


            currentServiceData.put(LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);


            if (i == 3) {
                gattServiceData.add(currentServiceData);
                ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                        new ArrayList<HashMap<String, String>>();
                List<BluetoothGattCharacteristic> gattCharacteristics =
                        gattService.getCharacteristics();
                ArrayList<BluetoothGattCharacteristic> charas =
                        new ArrayList<BluetoothGattCharacteristic>();

                // Loops through available Characteristics.
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    for (int z = 0; z < 2; z++) {
                        charas.add(gattCharacteristic);
                        HashMap<String, String> currentCharaData = new HashMap<String, String>();
                        uuid = gattCharacteristic.getUuid().toString();
                        currentCharaData.put(
                                LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                        currentCharaData.put(LIST_UUID, uuid);
                        gattCharacteristicGroupData.add(currentCharaData);

                    }
                }
                mGattCharacteristics.add(charas);
                gattCharacteristicData.add(gattCharacteristicGroupData);

            }
            i++;
        }
        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[]{LIST_NAME, LIST_UUID},
                new int[]{android.R.id.text1, android.R.id.text2},
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[]{LIST_NAME, LIST_UUID},
                new int[]{android.R.id.text1, android.R.id.text2}
        );
        mGattServicesList.setAdapter(gattServiceAdapter);


    }

    private void displayData(String data) {
        ConnectedTask2 connectedTask2 = new ConnectedTask2(data);
        if (data != null) {
            mDataField.setText(data);
            Log.d("데이터값 ", data);
            connectedTask2.execute();

        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * ------------------------------ Hid 세팅 ------------------------------
     **/
    private void Proxy() {
        proxyadapter.getProfileProxy(this,
                new BluetoothProfile.ServiceListener() {
                    @RequiresApi(api = Build.VERSION_CODES.P)
                    @Override
                    public void onServiceConnected(int profile, BluetoothProfile proxy) {
                        if (profile == BluetoothProfile.HID_DEVICE) {
                            hidDevice = (BluetoothHidDevice) proxy;
                            sdp = new BluetoothHidDeviceAppSdpSettings(
                                    "BTHid",
                                    "BTMouse",
                                    "Android",
                                    BluetoothHidDevice.SUBCLASS1_COMBO,
                                    descriptor_object.descriptor
                            );
                            qos = new BluetoothHidDeviceAppQosSettings(1, 0, 0, 0, 300, -1);
                            hidDevice.registerApp(
                                    sdp,
                                    null,
                                    qos,
                                    Executors.newSingleThreadExecutor(),
                                    new BluetoothHidDevice.Callback() {
                                        @Override
                                        public void onGetReport(BluetoothDevice device, byte type, byte id, int bufferSize) {
                                            super.onGetReport(device, type, id, bufferSize);
                                        }

                                        public void onConnectionStateChanged(BluetoothDevice device, final int state) {
                                            if (device.equals(btdevice)) {
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        TextView status = findViewById(R.id.proxy_state);
                                                        if (state == BluetoothProfile.STATE_DISCONNECTED) {
                                                            status.setText(R.string.status_disconnected);
                                                            Log.d("에림이", "사쿠라야??");
                                                            btdevice = null;
                                                        } else if (state == BluetoothProfile.STATE_CONNECTING) {
                                                            status.setText(R.string.status_connecting);
                                                        } else if (state == BluetoothProfile.STATE_CONNECTED) {
                                                            status.setText(R.string.status_connected);
                                                        } else if (state == BluetoothProfile.STATE_DISCONNECTING) {
                                                            status.setText(R.string.status_disconnecting);
                                                        }
                                                    }
                                                });
                                            }
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onServiceDisconnected(int profile) {
                        if (profile == BluetoothProfile.HID_DEVICE) {
                            Log.d("Bluetoot", "Proxy Lost Hid");
                        }

                    }
                }, BluetoothProfile.HID_DEVICE);

    }

    private void ProxyList() {
        Proxy();
        Set<BluetoothDevice> parieddevice = proxyadapter.getBondedDevices();
        List<String> names = new ArrayList<>();
        names.add("HID연결없음");
        deviceList.add(null);
        for (BluetoothDevice btdevice : parieddevice) {
            names.add(btdevice.getName());
            deviceList.add(btdevice);
        }
        list_proxy = findViewById(R.id.proxy_list);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        list_proxy.setAdapter(adapter);

        list_proxy.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @RequiresApi(api = Build.VERSION_CODES.P)
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice device = deviceList.get(position);
                Proxy_Connect proxy_connect = new Proxy_Connect(device);
                proxy_connect.execute();

                Log.d("Proxy", "연결됨?");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

    }

    private class Proxy_Connect extends AsyncTask<Void, Void, Boolean> {
        @RequiresApi(api = Build.VERSION_CODES.P)
        public Proxy_Connect(BluetoothDevice device) {
            for (BluetoothDevice device1 : hidDevice.getDevicesMatchingConnectionStates(
                    new int[]{
                            BluetoothProfile.STATE_CONNECTING,
                            BluetoothProfile.STATE_CONNECTED
                    }
            ))
                if (device == null) {
                    hidDevice.disconnect(device1);
                }
            if (device != null) {
                btdevice = device;
                hidDevice.connect(device);
            }
            Log.d("proxy", "device있음?" + device);
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            return null;
        }
    }

    private class ConnectedTask2 extends AsyncTask<Void, String, Boolean> {
        String getMessage;

        int i, x, y = 0;
        byte state = 0;

        ConnectedTask2(String string) {
            getMessage = string;


        }

        @RequiresApi(api = Build.VERSION_CODES.P)
        @Override
        protected Boolean doInBackground(Void... voids) {
            int readBufferPosition = 0;
            if (true) {
                try {
                    if (getMessage !=null) {

                        Log.i("좌표값 : ",getMessage);
                        Log.d("rkqt", "확인 : " + getMessage.substring(0, 7));
                        String beforeX = getMessage.substring(0, 3).trim();
                        String beforeY = getMessage.substring(4, 7).trim();
                        try {
                            x = parseInt(beforeX);
                            y = parseInt(beforeY);
                            for (BluetoothDevice btDev : hidDevice.getConnectedDevices()) {
                                hidDevice.sendReport(
                                        btDev,
                                        0,
                                        new byte[]{
                                                state,
                                                (byte) x,
                                                (byte) -y,
                                        }
                                );
                            }
                        } catch (Exception e) {

                        }
                    }

                } catch (Exception e) {
                    return false;
                }
            }
            return null;
        }
    }


}
