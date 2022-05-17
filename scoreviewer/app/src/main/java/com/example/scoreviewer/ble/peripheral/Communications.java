package com.example.scoreviewer.ble.peripheral;

import android.app.AlertDialog;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.ScanCallback;
import android.content.Context;
import android.database.DataSetObserver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.example.scoreviewer.R;
import com.example.scoreviewer.ble.UartPacketManagerBase;
import com.example.scoreviewer.ble.central.BlePeripheral;
import com.example.scoreviewer.ble.central.BlePeripheralUart;
import com.example.scoreviewer.mqtt.MqttManager;
import com.example.scoreviewer.utils.LocalizationManager;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

public class Communications  {//extends BlePeripheralUart {
    // Log
    private final static String TAG = Communications.class.getSimpleName();
    private static BlePeripheralUart mBlePeripheralUart;
    private static UartPeripheralService mUartPeripheralService;
  // Characteristics
    private static UUID kUartTxCharacteristicUUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    private static UUID kUartRxCharacteristicUUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

    private BluetoothGattCharacteristic mTxCharacteristic = new BluetoothGattCharacteristic(kUartTxCharacteristicUUID, BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE, BluetoothGattCharacteristic.PERMISSION_WRITE);
    private BluetoothGattCharacteristic mRxCharacteristic = new BluetoothGattCharacteristic(kUartRxCharacteristicUUID, BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_READ);
    private BluetoothGattDescriptor mRxConfigDescriptor = new BluetoothGattDescriptor(BlePeripheral.kClientCharacteristicConfigUUID, BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);

    protected WeakReference<UartPacketManagerBase.Listener> mWeakListener;
    public static String addT1="A";//on
    public static String subT1="B";//on
    public static String addT2="C";
    public static String subT2="D";//on

    public static String resetAll="E";//on

    public static String requestScoreupdate="F";//on

    public static String set15T1="Y";//on
    public static String set15T2="Z";//on

    private int T1Score = 0;
    private int T2Score = 0;

    private volatile StringBuilder mDataBuffer = new StringBuilder();
    private volatile StringBuilder mTextSpanBuffer = new StringBuilder();

    private boolean isInit = false;

    private static int PACKET_LENGTH = 10;

    public Communications() {}


    public void initCommunications(BlePeripheral mBlePeripheral)
    {
        if(!isInit) {
            mBlePeripheralUart = new BlePeripheralUart(mBlePeripheral);
//        mUartPeripheralService.uartEnable((UartPeripheralService.UartRXListener) mWeakListener);
            mBlePeripheralUart.uartEnable((BlePeripheralUart.UartRxHandler) mWeakListener, null);
            isInit = true;
        }
    }

    public void terminateCommunications()
    {
        if(isInit) {
            if(mBlePeripheralUart.isUartEnabled()) {
                mBlePeripheralUart.uartDisable();
                mBlePeripheralUart.disconnect();
            }
                mBlePeripheralUart = null;
            isInit = false;
        }
    }

    public void sendScoreUpdate(String updateVal){
//        mUartPeripheralService.setCharacteristic(mTxCharacteristic,updateVal.getBytes());
        mBlePeripheralUart.uartSend(updateVal.getBytes(),null);
    }

    public void parsePacket()
    {
        String packetContents = new String(mDataBuffer);
        if(packetContents.length() >= PACKET_LENGTH)
        {
            // Numbering       0123456789
            // Packet contents T1:##T2:##
            if(packetContents.startsWith("T1:") && packetContents.regionMatches(5,"T2:", 0, 3))
            {
                T1Score = Integer.parseInt(mDataBuffer.substring(3,5));
                T2Score = Integer.parseInt(mDataBuffer.substring(8,10));
                mDataBuffer.delete(0, PACKET_LENGTH);
            }
            else
            {

            }
        }
    }
    public int getT1Score()
    {
        return T1Score;
    }

    public int getT2Score()
    {
        return T2Score;
    }

    private int mDataBufferLastSize = 0;
    public synchronized void addText(String text) {
        Log.d(TAG, "addText: " + text);
        mDataBuffer.append(text);

        //Trunkcate to the last T1: header start
        String mDataString = new String(mDataBuffer);
        while(mDataBuffer.length()>PACKET_LENGTH  || (!mDataString.startsWith("T1") && mDataBuffer.length()>2))
        {
            Log.d(TAG, "delChar: " + mDataBuffer.charAt(0));
            mDataBuffer.deleteCharAt(0);
            mDataString = new String(mDataBuffer);
            Log.d(TAG, "newStr: " + mDataString);
        }

            Log.d(TAG, "SetText: " + mDataBuffer);

        parsePacket();
    }


}
