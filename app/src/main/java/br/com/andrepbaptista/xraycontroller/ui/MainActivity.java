package br.com.andrepbaptista.xraycontroller.ui;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import br.com.andrepbaptista.xraycontroller.R;
import br.com.andrepbaptista.xraycontroller.ble.BleHandler;
import br.com.andrepbaptista.xraycontroller.util.ValuesUtils;

public class MainActivity extends Activity {

    private TextView lblScanning;
    private Button btnSearch;
    private EditText edtTime;
    private Handler handler = new Handler();

    private final int PERMISSION_REQUEST_CODE = 1;

    private List<String> times;
    private int timesListIndex = 0;
    private final int TIMES_LIST_MAX_INDEX = 14;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        times = ValuesUtils.getTimesList();

        lblScanning = findViewById(R.id.lblScanning);
        btnSearch = findViewById(R.id.btnSearch);
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
        BleHandler.getInstance(this).killBle();
    }

    private void init() {
        BleHandler.getInstance(this).start(new BleHandler.BleCallback() {
            @Override
            public void onStartedExecution() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        lblScanning.setText(getString(R.string.scanning));
                        btnSearch.setEnabled(false);
                    }
                });
            }

            @Override
            public void onServiceReady() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        lblScanning.setText(getString(R.string.searching_characteristic));
                    }
                });
            }

            @Override
            public void onCharacteristicReady(BluetoothGattCharacteristic characteristic) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        lblScanning.setText(getString(R.string.connected));
                    }
                });
                refreshCharacteristic(characteristic);
            }

            @Override
            public void onDeviceReady() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        lblScanning.setText(getString(R.string.searching_service));
                    }
                });
            }

            @Override
            public void onError() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        lblScanning.setText(getString(R.string.connection_error));
                        btnSearch.setEnabled(true);
                    }
                });
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            init();
        }
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
        String characteristicValue = ValuesUtils.timeToHexString(edtTime.getText().toString());

        BleHandler.getInstance(this).writeCharacteristic(characteristicValue, new BleHandler.CharacteristicCallback() {
            @Override
            public void onChange(BluetoothGattCharacteristic characteristic) {
                refreshCharacteristic(characteristic);
            }

            @Override
            public void onError() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, getString(R.string.error_set_time), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    public void searchDevices(View v) {
        init();
    }

    private void refreshCharacteristic(BluetoothGattCharacteristic characteristic) {
        String hexString = ValuesUtils.byteArrayToHexString(characteristic.getValue());

        int timesListIndex = ValuesUtils.hexStringToTimeListIndex(hexString);
        this.timesListIndex = timesListIndex;

        handler.post(new Runnable() {
            @Override
            public void run() {
                String timeString = times.get(MainActivity.this.timesListIndex);

                edtTime.setEnabled(true);
                edtTime.setText(timeString);

                Toast.makeText(MainActivity.this, MainActivity.this.getString(R.string.success_set_time) + " " + timeString, Toast.LENGTH_LONG).show();
            }
        });
    }
}
