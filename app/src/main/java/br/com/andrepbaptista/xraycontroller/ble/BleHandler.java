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

    private BluetoothDevice device;
    private final BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic timeCharacteristic;
    private boolean deviceFound;
    private static final long SCAN_PERIOD = 5000;
    private final UUID SERVICE_UUID;
    private final UUID TIME_CHARACTERISTIC_UUID;
    private final UUID SHOTS_CHARACTERISTIC_UUID;

    public interface BleCallback {
        void onStartedExecution();

        void onServiceReady();

        void onTimeCharacteristicReady(BluetoothGattCharacteristic characteristic);

        void onDeviceReady();

        void onError();
    }

    public interface TimeCharacteristicCallback {
        void onChange(BluetoothGattCharacteristic characteristic);

        void onError();
    }

    public interface ShotCharacteristicCallback {
        void onRead(BluetoothGattCharacteristic characteristic);

        void onError();
    }

    private BleCallback bleCallback;
    private TimeCharacteristicCallback timeCharacteristicCallback;
    private ShotCharacteristicCallback shotCharacteristicCallback;

    private BleHandler(Context context) {
        this.context = context;
        SERVICE_UUID = UUID.fromString(context.getString(R.string.service_uuid));
        TIME_CHARACTERISTIC_UUID = UUID.fromString(context.getString(R.string.time_characteristic_uuid));
        SHOTS_CHARACTERISTIC_UUID = UUID.fromString(context.getString(R.string.shots_characteristic_uuid));

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

        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
        }
    }

    public void start(BleCallback bleCallback) {
        this.bleCallback = bleCallback;

        if(!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        }

        if(device != null) {
            bleCallback.onStartedExecution();
            bluetoothGatt = device.connectGatt(context, false, this);
            bluetoothGatt.discoverServices();
            return;
        }

        deviceFound = false;

        scanLeDevice(true);
    }

    public void writeTimeCharacteristic(String value, TimeCharacteristicCallback timeCharacteristicCallback) {
        this.timeCharacteristicCallback = timeCharacteristicCallback;

        if (timeCharacteristic != null) {
            timeCharacteristic.setValue(ValuesUtils.hexStringToByteArray(value));
            bluetoothGatt.writeCharacteristic(timeCharacteristic);
        } else {
            timeCharacteristicCallback.onError();
        }
    }

    public void readShotCharacteristic(ShotCharacteristicCallback shotCharacteristicCallback) {
        BluetoothGattService service = bluetoothGatt.getService(SERVICE_UUID);
        if(service != null) {
            this.shotCharacteristicCallback = shotCharacteristicCallback;
            bluetoothGatt.readCharacteristic(service.getCharacteristic(SHOTS_CHARACTERISTIC_UUID));
        } else {
            this.shotCharacteristicCallback.onError();
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
        mBluetoothAdapter.cancelDiscovery();

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
            this.device = device;
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
            gatt.readCharacteristic(gatt.getService(SERVICE_UUID).getCharacteristic(TIME_CHARACTERISTIC_UUID));

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

            if(characteristic.getUuid().equals(TIME_CHARACTERISTIC_UUID)) {
                timeCharacteristic = characteristic;
                bleCallback.onTimeCharacteristicReady(characteristic);
            } else if(characteristic.getUuid().equals(SHOTS_CHARACTERISTIC_UUID)) {
                shotCharacteristicCallback.onRead(characteristic);
            }

        } else {
            bleCallback.onError();
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt,
                                      BluetoothGattCharacteristic characteristic,
                                      int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            timeCharacteristicCallback.onChange(BleHandler.this.timeCharacteristic);
        } else {
            timeCharacteristicCallback.onError();
        }
    }
}
