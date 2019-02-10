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
    private Button btnAddTime;
    private Button btnSubTime;
    private Button btnConfirm;
    private Button btnShots;
    private EditText edtTime;
    private TextView lblShots;
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
        btnAddTime = findViewById(R.id.btnAddTime);
        btnSubTime = findViewById(R.id.btnSubTime);
        btnConfirm = findViewById(R.id.btnConfirm);
        btnShots = findViewById(R.id.btnShots);
        edtTime = findViewById(R.id.edtTime);
        lblShots = findViewById(R.id.lblShots);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            this.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_CODE);
        } else {
            init();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        BleHandler.getInstance(this).killBle();
        btnSearch.setVisibility(View.VISIBLE);
    }

    private void enableUI() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                btnAddTime.setEnabled(true);
                btnSubTime.setEnabled(true);
                btnConfirm.setEnabled(true);
                edtTime.setEnabled(true);
                btnShots.setEnabled(true);
            }
        });
    }

    private void enableUIInErrorState() {
        disableUI();

        handler.post(new Runnable() {
            @Override
            public void run() {
                btnSearch.setVisibility(View.VISIBLE);
            }
        });
    }

    private void disableUI() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                btnSearch.setVisibility(View.GONE);
                btnAddTime.setEnabled(false);
                btnSubTime.setEnabled(false);
                btnConfirm.setEnabled(false);
                edtTime.setEnabled(false);
                btnShots.setEnabled(false);
            }
        });
    }

    private void init() {
        BleHandler.getInstance(this).start(new BleHandler.BleCallback() {
            @Override
            public void onStartedExecution() {
                lblScanning.setText(getString(R.string.scanning));
                disableUI();
            }

            @Override
            public void onServiceReady() {
                lblScanning.setText(getString(R.string.searching_characteristic));
            }

            @Override
            public void onTimeCharacteristicReady(BluetoothGattCharacteristic characteristic) {
                lblScanning.setText(getString(R.string.connected));
                enableUI();
                refreshTimeCharacteristic(characteristic);
            }

            @Override
            public void onDeviceReady() {
                lblScanning.setText(getString(R.string.searching_service));
            }

            @Override
            public void onError() {
                lblScanning.setText(getString(R.string.connection_error));
                enableUIInErrorState();
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
        if (timesListIndex == TIMES_LIST_MAX_INDEX) {
            return;
        }
        timesListIndex++;
        edtTime.setText(times.get(timesListIndex));
    }

    public void decreaseTime(View v) {
        if (timesListIndex == 0) {
            return;
        }
        timesListIndex--;
        edtTime.setText(times.get(timesListIndex));
    }

    public void setTime(View v) {
        String characteristicValue = ValuesUtils.timeToHexString(edtTime.getText().toString());

        BleHandler.getInstance(this).writeTimeCharacteristic(characteristicValue, new BleHandler.TimeCharacteristicCallback() {
            @Override
            public void onChange(BluetoothGattCharacteristic characteristic) {
                refreshTimeCharacteristic(characteristic);
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

    public void readShots(View v) {
        BleHandler.getInstance(this).readShotCharacteristic(new BleHandler.ShotCharacteristicCallback() {
            @Override
            public void onRead(BluetoothGattCharacteristic characteristic) {
                refreshShotCharacteristic(characteristic);
            }

            @Override
            public void onError() {
                Toast.makeText(MainActivity.this, getString(R.string.error_read_shots), Toast.LENGTH_LONG).show();
            }
        });
    }

    public void searchDevices(View v) {
        init();
    }

    private void refreshTimeCharacteristic(BluetoothGattCharacteristic characteristic) {
        String hexString = ValuesUtils.byteArrayToHexString(characteristic.getValue());

        int timesListIndex = ValuesUtils.hexStringToTimeListIndex(hexString);
        this.timesListIndex = timesListIndex;

        handler.post(new Runnable() {
            @Override
            public void run() {
                String timeString = times.get(MainActivity.this.timesListIndex);
                edtTime.setText(timeString);

                Toast.makeText(MainActivity.this, MainActivity.this.getString(R.string.success_set_time) + " " + timeString, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void refreshShotCharacteristic(final BluetoothGattCharacteristic characteristic) {
        String hexString = ValuesUtils.byteArrayToHexString(characteristic.getValue());
        String hexShots = hexString.substring(20);
        int shots = Integer.parseInt(hexShots, 16);
        lblShots.setText(String.valueOf(shots));
    }
}
