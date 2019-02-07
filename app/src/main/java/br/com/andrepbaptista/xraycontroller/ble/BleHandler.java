package br.com.andrepbaptista.xraycontroller.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Handler;

import java.util.UUID;

import br.com.andrepbaptista.xraycontroller.R;
import br.com.andrepbaptista.xraycontroller.util.ValuesUtils;

public class BleHandler extends BluetoothGattCallback implements BluetoothAdapter.LeScanCallback {

    private static BleHandler bleHandler;
    private Context context;
    private Handler mHandler;
    private Runnable runnable;

    private final BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic characteristic;
    private boolean deviceFound;
    private static final long SCAN_PERIOD = 5000;
    private final UUID SERVICE_UUID;
    private final UUID CHARACTERISTIC_UUID;

    public interface BleCallback {
        void onStartedExecution();

        void onServiceReady();

        void onCharacteristicReady(BluetoothGattCharacteristic characteristic);

        void onDeviceReady();

        void onError();
    }

    public interface CharacteristicCallback {
        void onChange(BluetoothGattCharacteristic characteristic);

        void onError();
    }

    private BleCallback bleCallback;
    private CharacteristicCallback characteristicCallback;

    private BleHandler(Context context) {
        this.context = context;
        SERVICE_UUID = UUID.fromString(context.getString(R.string.service_uuid));
        CHARACTERISTIC_UUID = UUID.fromString(context.getString(R.string.characteristic_uuid));

        final BluetoothManager bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }

    public static BleHandler getInstance(Context context) {
        if (bleHandler == null) {
            bleHandler = new BleHandler(context);
        }

        return bleHandler;
    }

    public void killBle() {
        killScan();

        if (bluetoothGatt != null
                && mBluetoothAdapter != null) {
            bluetoothGatt.close();
            mBluetoothAdapter.disable();
        }
    }

    public void start(BleCallback bleCallback) {
        this.bleCallback = bleCallback;

        if(!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        }

        deviceFound = false;

        scanLeDevice(true);
    }

    public void writeCharacteristic(String value, CharacteristicCallback characteristicCallback) {
        this.characteristicCallback = characteristicCallback;

        if (characteristic != null) {
            characteristic.setValue(ValuesUtils.hexStringToByteArray(value));
            bluetoothGatt.writeCharacteristic(characteristic);
        } else {
            characteristicCallback.onError();
        }
    }

    private void buildStopScanRunnable() {
        runnable = new Runnable() {
            @Override
            public void run() {
                if (!deviceFound) {
                    mBluetoothAdapter.stopLeScan(BleHandler.this);
                    bleCallback.onError();
                }
            }
        };
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            buildStopScanRunnable();
            mHandler = new Handler();
            mHandler.postDelayed(runnable, SCAN_PERIOD);

            bleCallback.onStartedExecution();
            mBluetoothAdapter.startLeScan(this);
        } else {
            mBluetoothAdapter.stopLeScan(this);
        }
    }

    private void killScan() {
        mBluetoothAdapter.stopLeScan(this);

        if (mHandler != null
                && runnable != null) {
            mHandler.removeCallbacks(runnable);
            mHandler = null;
            runnable = null;
        }
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (device.getName() != null && device.getName().equals(context.getString(R.string.ble_name))) {
            deviceFound = true;
            bluetoothGatt = device.connectGatt(context, false, this);
            bleCallback.onDeviceReady();
        }
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        killScan();
        gatt.discoverServices();
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            BluetoothGattService service = gatt.getService(SERVICE_UUID);
            gatt.readCharacteristic(service.getCharacteristic(CHARACTERISTIC_UUID));

            bleCallback.onServiceReady();
        } else {
            bleCallback.onError();
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt,
                                     BluetoothGattCharacteristic characteristic,
                                     int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            BleHandler.this.characteristic = characteristic;

            bleCallback.onCharacteristicReady(characteristic);
        } else {
            bleCallback.onError();
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt,
                                      BluetoothGattCharacteristic characteristic,
                                      int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            characteristicCallback.onChange(BleHandler.this.characteristic);
        } else {
            characteristicCallback.onError();
        }
    }
}
