##简介
　　首先非常感谢BleLib的作者为我们提供了如此方便的开源库；这个库大大的简化了我们BLE开发的步骤,操作非常简单
###BleLib中的关键类

 - BleService是单个Ble连接操作的服务类
 - GattAttributes类中包含了蓝牙联盟规定的服务和特征的UUID值
 - MultipleBleService类是可多个蓝牙设备同时连接的服务类
 
##第一步添加BleLib依赖库
```Java
dependencies {
    compile 'com.junkchen.blelib:blelib:1.2.0'
}
```
##第二步绑定BleLib服务
```Java
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
                Toast.makeText(getApplicationContext(), "蓝牙已打开", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), "不支持蓝牙", Toast.LENGTH_SHORT).show();
        }
    }

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
```

##第三步初始化服务
当服务绑定后可进行初始化操作，判断该机是否支持蓝牙，调用如下方法：
```Java
//Ble初始化操作  
mBleService.initialize();
```

##第四步打开蓝牙
如果发现蓝牙没有打开可以调用下面方法打开蓝牙
```Java
//打开或关闭蓝牙
mBleService.enableBluetooth(boolean enable);
```

##第五步扫描ble设备
　　扫描设备提供了两种方法,boolean参数表示是否开始/停止扫描,long 参数表示扫描的时间;默认时间为10秒:

 - mBleService.scanLeDevice(boolean enable)
 - mBleService.scanLeDevice(final boolean enable, long scanPeriod)

###接受扫描的结果
　　接受扫描到的结果也有两种方式可供选择

 - 监听器方式

 ```Java
 //Ble扫描回调
mBleService.setOnLeScanListener(new BleService.OnLeScanListener() {
    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        //每当扫描到一个Ble设备时就会返回，（扫描结果重复的库中已处理）
    }
});
 ```
 - 广播方式
 
 ```Java
 private BroadcastReceiver bleReceiver = new BroadcastReceiver() {
       @Override
       public void onReceive(Context context, Intent intent) {
           if (intent.getAction().equals(BleService.ACTION_BLUETOOTH_DEVICE)) {
               String tmpDevName = intent.getStringExtra("name");
               String tmpDevAddress = intent.getStringExtra("address");
               Log.i(TAG, "name: " + tmpDevName + ", address: " + tmpDevAddress);
           } else if (intent.getAction().equals(BleService.ACTION_SCAN_FINISHED)) {
               //扫描Ble设备停止
           }
       }
   };

##第六步连接Ble服务
```Java
mBleService.connect(String address);//连接Ble  
mBleService.disconnect();//取消连接  
```
接受连接状态的方式也有两种

 - 监听器方式

```Java
//Ble连接回调
mBleService.setOnConnectListener(new BleService.OnConnectListener() {
    @Override
    public void onConnect(BluetoothGatt gatt, int status, int newState) {
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
}); 
```
 - 广播方式

```Java
private BroadcastReceiver bleReceiver = new BroadcastReceiver() {
   @Override
   public void onReceive(Context context, Intent intent) {
       if (intent.getAction().equals(BleService.ACTION_GATT_CONNECTED)) {
           //Ble已连接
       } else if (intent.getAction().equals(BleService.ACTION_GATT_DISCONNECTED)) {
           //Ble连接已断开
       }
   }
};
```

##第七步发现服务
```Java
//Ble服务发现回调
mBleService.setOnServicesDiscoveredListener(new BleService.OnServicesDiscoveredListener() {
    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {

    }
});
```

##第八步：读写Ble特性和接收GATT通知
连接上Ble并获取服务之后就可以对特性进行读写，设置GATT通知:
```Java
mBleService.setCharacteristicNotification();//设置通知  
mBleService.readCharacteristic();//读取数据  
mBleService.writeCharacteristic();//写入数据 
```
###设置读物数据和GATT通知的接受监听器
```Java
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
        //处理描述返回的数据
        KLog.d();
    }
};
```

##总结
常用方法
```Java
mBleService.initialize();//Ble初始化操作  
mBleService.enableBluetooth(boolean enable);//打开或关闭蓝牙  
mBleService.scanLeDevice(boolean enable, long scanPeriod);//启动或停止扫描Ble设备  
mBleService.connect(String address);//连接Ble  
mBleService.disconnect();//取消连接  
mBleService.getSupportedGattServices();//获取服务  
mBleService.setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
boolean enabled);//设置通知  
mBleService.readCharacteristic(BluetoothGattCharacteristic characteristic);//读取数据  
mBleService.writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] value);//写入数据 
```

项目地址:https://github.com/CoolkeWang/BleLibTest<br/>
博客地址:http://blog.csdn.net/q531934288/article/details/51729158
开源库地址:https://github.com/junkchen/BleLib
