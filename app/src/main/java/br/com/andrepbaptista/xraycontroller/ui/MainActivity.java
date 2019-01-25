package br.com.andrepbaptista.xraycontroller.ui;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.UUID;

import br.com.andrepbaptista.xraycontroller.R;
import br.com.andrepbaptista.xraycontroller.app.XRayApplication;
import br.com.andrepbaptista.xraycontroller.util.ValuesUtils;

public class MainActivity extends Activity {

    TextView lblScanning;
    Button btnSearch;
    EditText edtTime;

    public static final String ERROR_SET_TIME = "Aguarde o dispositivo ser conectado.";
    public static final String SUCCESS_SET_TIME = "Tempo modificado com sucesso para ";
    public static final String CONNECTION_ERROR = "A comunicação com o dispositivo falhou.";
    private final String SCANNING = "Procurando dispositivo...";
    private final String SEARCHING_SERVICE = "Dispositivo encontrado! Buscando serviço...";
    private final String SEARCHING_CHARACTERISTIC = "Serviço encontrado! Lendo característica...";
    private final String CONNECTED = "Tudo Pronto!";
    private boolean deviceFound = false;

    private static final long SCAN_PERIOD = 10000;
    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler = new Handler();

    private final int PERMISSION_REQUEST_CODE = 1;

    private BluetoothGatt bluetoothGatt;
    BluetoothGattCharacteristic characteristic;
    private final UUID SERVICE_UUID = UUID.fromString("b8d2d47e-9f5b-43e0-a2ef-a243dc862164");
    private final UUID CHARACTERISTIC_UUID = UUID.fromString("c0a00c1f-cb50-4a7a-a70f-a78b73fa1205");

    private List<String> times;
    private int timesListIndex = 0;
    private final int TIMES_LIST_MAX_INDEX = 13;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        times = ValuesUtils.getTimesList();

        lblScanning = findViewById(R.id.lblScanning);
        btnSearch = findViewById(R.id.btnSearch);
        btnSearch.setVisibility(View.GONE);
        edtTime = findViewById(R.id.edtTime);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            this.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_CODE);
        } else {
            init();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothGatt == null) {
            return;
        }
        bluetoothGatt.close();
        bluetoothGatt = null;

    }

    private void init() {
        Application app = getApplication();
        mBluetoothAdapter = ((XRayApplication) app).getmBluetoothAdapter();

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }

        scanLeDevice(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            init();
        }
    }

    public void searchDevices(View view) {
        scanLeDevice(true);
    }

    public void increaseTime(View v) {
        if(timesListIndex == TIMES_LIST_MAX_INDEX) {
            return;
        }
        timesListIndex ++;
        edtTime.setText(times.get(timesListIndex));
    }

    public void decreaseTime(View v) {
        if(timesListIndex == 0) {
            return;
        }
        timesListIndex --;
        edtTime.setText(times.get(timesListIndex));
    }

    public void setTime(View v) {
        if(characteristic == null) {
            Toast.makeText(this, ERROR_SET_TIME, Toast.LENGTH_LONG).show();
            return;
        }

        String timeString = edtTime.getText().toString();
        String characteristicValue = ValuesUtils.timeToHexString(timeString);

        characteristic.setValue(ValuesUtils.hexStringToByteArray(characteristicValue));
        if(bluetoothGatt.writeCharacteristic(characteristic)) {
            Toast.makeText(this, SUCCESS_SET_TIME + timeString + "s", Toast.LENGTH_LONG).show();
        }
    }

    private void scanLeDevice(final boolean enable) {

        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!deviceFound) {
                        lblScanning.setText(CONNECTION_ERROR);
                        btnSearch.setVisibility(View.VISIBLE);
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    }
                }
            }, SCAN_PERIOD);

            lblScanning.setText(SCANNING);
            btnSearch.setVisibility(View.GONE);
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            btnSearch.setVisibility(View.VISIBLE);
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    deviceFound = true;
                    lblScanning.setText(SEARCHING_SERVICE);
                    bluetoothGatt = device.connectGatt(MainActivity.this, false, mGattCallback);
                }
            };

    private final BluetoothGattCallback mGattCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    gatt.discoverServices();
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        lblScanning.setText(SEARCHING_CHARACTERISTIC);

                        BluetoothGattService service = gatt.getService(SERVICE_UUID);
                        gatt.readCharacteristic(service.getCharacteristic(CHARACTERISTIC_UUID));
                    } else {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                lblScanning.setText(CONNECTION_ERROR);
                                btnSearch.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                }

                @Override
                public void onCharacteristicRead(BluetoothGatt gatt,
                                                 BluetoothGattCharacteristic characteristic,
                                                 int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        MainActivity.this.characteristic = characteristic;
                        lblScanning.setText(CONNECTED);

                        String hexString = ValuesUtils.byteArrayToHexString(characteristic.getValue());
                        int timesListIndex = ValuesUtils.hexStringToTimeListIndex(hexString);
                        MainActivity.this.timesListIndex = timesListIndex;

                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                edtTime.setEnabled(true);
                                edtTime.setText(times.get(MainActivity.this.timesListIndex));
                            }
                        });
                    } else {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                lblScanning.setText(CONNECTION_ERROR);
                                btnSearch.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                }
            };
}
