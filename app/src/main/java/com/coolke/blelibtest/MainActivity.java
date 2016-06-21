package com.coolke.blelibtest;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.junkchen.blelib.BleService;
import com.socks.library.KLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.lv)
    ListView lv;
    private SimpleAdapter simpleAdapter;
    private ArrayList<Map<String, String>> deviceList = new ArrayList<>();

    private final static String BLUETOOTH_NAME = "name";
    private final static String BLUETOOTH_TYPE = "type";
    private final static String BLUETOOTH_ADDR = "addr";
    private final static String BLUETOOTH_UUIDS = "uuids";

    private BleService mBleService;
    private boolean mIsBind;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        doBindService();

        //初始化listview
        simpleAdapter = new SimpleAdapter(getApplicationContext(),
                deviceList, R.layout.item,
                new String[]{BLUETOOTH_NAME, BLUETOOTH_TYPE, BLUETOOTH_ADDR, BLUETOOTH_UUIDS},
                new int[]{R.id.tv_name, R.id.tv_type, R.id.tv_addr, R.id.tv_uuid});

        lv.setAdapter(simpleAdapter);
        lv.setOnItemClickListener(lvOnItemClickListener);
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mIsBind = true;
            mBleService = ((BleService.LocalBinder) service).getService();
            //Ble初始化操作
            if (mBleService.initialize()) {
                //打开蓝牙
                if (mBleService.enableBluetooth(true)) {
                    //Ble扫描回调
                    mBleService.setOnLeScanListener(leScanListener);
                    mBleService.scanLeDevice(true);
                    Toast.makeText(getApplicationContext(), "蓝牙已打开", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getApplicationContext(), "不支持蓝牙", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBleService = null;
            mIsBind = false;
        }
    };

    //绑定服务
    private void doBindService() {
        Intent serviceIntent = new Intent(this, BleService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    //解绑服务
    private void doUnBindService() {
        if (mIsBind) {
            unbindService(serviceConnection);
            mBleService = null;
            mIsBind = false;
        }
    }

    //ListView元素点击
    private AdapterView.OnItemClickListener lvOnItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            KLog.d(position);

            String name = deviceList.get(position).get(BLUETOOTH_NAME);
            String addr = deviceList.get(position).get(BLUETOOTH_ADDR);

            name = name.substring(name.indexOf(":")+1);
            addr = addr.substring(addr.indexOf(":")+1);
            //启动连接Activity,并停止扫描
            ConnectionActivity.startActivity(MainActivity.this,name,addr);
            mBleService.scanLeDevice(false);
        }
    };

    private BleService.OnLeScanListener leScanListener = new BleService.OnLeScanListener() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            //每当扫描到一个Ble设备时就会返回，（扫描结果重复的库中已处理）
            KLog.d(device.getName());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    int i;
                    for (i = 0; i < deviceList.size(); i++) {
                        if (-1 != deviceList.get(i).get(BLUETOOTH_ADDR).indexOf(device.getAddress())) {
                            break;
                        }
                    }
                    if (i >= deviceList.size()) {
                        Map<String, String> item = new HashMap<String, String>();
                        //获取设备名
                        item.put(BLUETOOTH_NAME, "设备名:" + device.getName());
                        //获取设备类型
                        switch (device.getType()) {
                            case BluetoothDevice.DEVICE_TYPE_CLASSIC:
                                item.put(BLUETOOTH_TYPE, "设备类型:" + "BR/EDR devices");
                                break;
                            case BluetoothDevice.DEVICE_TYPE_DUAL:
                                item.put(BLUETOOTH_TYPE, "设备类型:" + "Dual Mode - BR/EDR/LE");
                                break;
                            case BluetoothDevice.DEVICE_TYPE_LE:
                                item.put(BLUETOOTH_TYPE, "设备类型:" + "Low Energy - LE-only");
                                break;
                            case BluetoothDevice.DEVICE_TYPE_UNKNOWN:
                                item.put(BLUETOOTH_TYPE, "设备类型:" + "Unknown");
                                break;
                        }
                        //获取设备地址
                        item.put(BLUETOOTH_ADDR, "设备地址:" + device.getAddress());
                        //获取设备UUID
                        StringBuffer stringBuffer = new StringBuffer();
                        ParcelUuid[] uuids = device.getUuids();
                        if (uuids != null) {
                            for (ParcelUuid uuid : uuids) {
                                stringBuffer.append(uuid.toString() + "\n");
                            }
                        }
                        item.put(BLUETOOTH_UUIDS, "UUID:" + stringBuffer.toString());
                        //更新设备列表
                        deviceList.add(item);
                        simpleAdapter.notifyDataSetChanged();
                    }
                }
            });
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.btn_search:
                //启动扫描Ble设备
                mBleService.scanLeDevice(true);
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
