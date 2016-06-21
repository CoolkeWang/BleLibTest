package com.coolke.blelibtest;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Created by Administrator on 2016/6/20.
 */
public class DataBean {
    private BluetoothGattCharacteristic characteristic;


    public DataBean(BluetoothGattCharacteristic characteristic) {
        this.characteristic = characteristic;
    }

    public BluetoothGattCharacteristic getCharacteristic() {
        return characteristic;
    }

    public void setCharacteristic(BluetoothGattCharacteristic characteristic) {
        this.characteristic = characteristic;
    }
}
