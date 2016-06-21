package com.coolke.blelibtest;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.junkchen.blelib.BleService;
import com.socks.library.KLog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;


/**
 * Created by Administrator on 2016/6/21.
 */
public class ConnectionActivity extends AppCompatActivity {
    private final static String BLUETOOTH_NAME = "name";
    private final static String BLUETOOTH_ADDR = "addr";
    @BindView(R.id.tv_content)
    TextView tvContent;
    @BindView(R.id.scrollView)
    ScrollView scrollView;
    @BindView(R.id.ed_input)
    EditText edInput;

    private String bluetooth_name;
    private String bluetooth_addr;
    private BleService mBleService;

    public final static UUID UUID_NOTIFY =
            UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    public final static UUID UUID_SERVICE =
            UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");

    public BluetoothGattCharacteristic mNotifyCharacteristic;

    public static void startActivity(Context context, String name, String addr) {
        Intent intent = new Intent(context.getApplicationContext(), ConnectionActivity.class);
        intent.putExtra(BLUETOOTH_NAME, name);
        intent.putExtra(BLUETOOTH_ADDR, addr);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);

        Intent intent = getIntent();
        bluetooth_name = intent.getStringExtra(BLUETOOTH_NAME);
        bluetooth_addr = intent.getStringExtra(BLUETOOTH_ADDR);

        getSupportActionBar().setTitle(bluetooth_name + "正在连接中....");
        mBleService = BleService.getInstance();

        mBleService.setOnServicesDiscoveredListener(discoveredListener);
        mBleService.setOnDataAvailableListener(dataAvailableListener);
        //设置连接监听
        mBleService.setOnConnectListener(connectionStateChangeListener);
        //连接Ble
        mBleService.connect(bluetooth_addr);
    }

    public void onclick(View v) {
        if (edInput.getText().toString().length() < 1) {
            Toast.makeText(getApplicationContext(), "请输入内容", Toast.LENGTH_SHORT).show();
        } else {
            try {
                mBleService.writeCharacteristic(mNotifyCharacteristic, (edInput.getText().toString()+"\r\n").getBytes("gbk"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if (tvContent.length() > 500) {
                tvContent.setText("");
            }
            tvContent.append("from me : " + edInput.getText().toString() + "\n");
            scrollView.post(new Runnable() {
                @Override
                public void run() {
                    scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });
        }
    }

    //连接监听
    private BleService.OnConnectionStateChangeListener connectionStateChangeListener = new BleService.OnConnectionStateChangeListener() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                KLog.d("Ble连接已断开");
            } else if (newState == BluetoothProfile.STATE_CONNECTING) {
                KLog.d("Ble正在连接");
            } else if (newState == BluetoothProfile.STATE_CONNECTED) {
                KLog.d("Ble已连接");
            } else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
                KLog.d("Ble正在断开连接");
            }
        }
    };

    ///Ble服务发现监听
    private BleService.OnServicesDiscoveredListener discoveredListener = new BleService.OnServicesDiscoveredListener() {
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            KLog.d();
            if (status == BluetoothGatt.GATT_SUCCESS) {
                KLog.d("onServicesDiscovered received: " + status);
                //查找服务
                findService(gatt);
            } else {
                if (gatt.getDevice().getUuids() == null) {
                    KLog.d("onServicesDiscovered received: " + status);
                }
            }
        }
    };

    //Ble数据回调
    private BleService.OnDataAvailableListener dataAvailableListener = new BleService.OnDataAvailableListener() {
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            KLog.d();
            //处理特性读取返回的数据
            EventBus.getDefault().post(new DataBean(characteristic));
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            KLog.d();
            //处理通知返回的数据
            EventBus.getDefault().post(new DataBean(characteristic));
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            KLog.d();
        }
    };


    public void findService(BluetoothGatt gatt) {
        List<BluetoothGattService> gattServices = gatt.getServices();
        KLog.d("GATT服务总数:" + gattServices.size());
        for (BluetoothGattService gattService : gattServices) {
            KLog.d(gattService.getUuid().toString());
            KLog.d(UUID_SERVICE.toString());
            //判断是否是指定的服务
            if (gattService.getUuid().toString().equalsIgnoreCase(UUID_SERVICE.toString())) {
                //获取参数
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                KLog.d("参数总数:" + gattCharacteristics.size());
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    //判断是否是指定的通知
                    if (gattCharacteristic.getUuid().toString().equalsIgnoreCase(UUID_NOTIFY.toString())) {
                        KLog.d(gattCharacteristic.getUuid().toString());
                        KLog.d(UUID_NOTIFY.toString());
                        //保存配置
                        mNotifyCharacteristic = gattCharacteristic;
                        //订阅配置
                        mBleService.setCharacteristicNotification(gattCharacteristic, true);
                        return;
                    }
                }
            }
        }
    }

    //接受数据
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void handlerDataForService(DataBean dataBean) {
        KLog.d("接受数据");
        String string = null;
        byte[] bytes = dataBean.getCharacteristic().getValue();
        if (bytes != null && bytes.length > 0) {
            try {
                string = new String(bytes, "gbk");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        if (string != null) {
            if (tvContent.length() > 500) {
                tvContent.setText("");
            }
            tvContent.append("from BLE : " + string + "\n");
            scrollView.post(new Runnable() {
                @Override
                public void run() {
                    scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //取消连接
        mBleService.disconnect();
        EventBus.getDefault().unregister(this);
    }
}
