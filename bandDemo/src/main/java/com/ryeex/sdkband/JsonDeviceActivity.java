package com.ryeex.sdkband;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ryeex.band.protocol.callback.AsyncProtocolCallback;
import com.ryeex.band.protocol.device.JsonDevice;
import com.ryeex.ble.common.device.DeviceConnectListener;
import com.ryeex.ble.connector.error.BleError;
import com.ryeex.ble.connector.log.BleLogger;
import com.ryeex.ble.connector.utils.RandomUtil;
import com.ryeex.sdk.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class JsonDeviceActivity extends AppCompatActivity {

    private final String TAG = "DeveloperActivity";
    private List<Integer> idList = new ArrayList<>();
    private JsonDevice device;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_developer);


        IntentFilter bleStateChangeFilter = new IntentFilter();
        bleStateChangeFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBleStateChangeReceiver, bleStateChangeFilter);
        startConnect();
    }


    private BroadcastReceiver mBleStateChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int bleState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
            switch (bleState) {
                case BluetoothAdapter.STATE_TURNING_ON:
                    Log.d(TAG, " BroadcastReceiver STATE_TURNING_ON");
                    //要等STATE_ON才能开始连接
                    break;
                case BluetoothAdapter.STATE_ON:
                    Log.d(TAG, " BroadcastReceiver STATE_ON");
                    startConnect();
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    Log.d(TAG, " BroadcastReceiver STATE_TURNING_OFF");
                    if (device == null) {
                        return;
                    }
                    device.disconnect(null);
                    break;
                case BluetoothAdapter.STATE_OFF:
                    Log.d(TAG, " BroadcastReceiver STATE_OFF");
                    if (device == null) {
                        return;
                    }
                    if (device.isConnected()) {
                        device.disconnect(null);
                    }
                    break;
                default:
            }
        }
    };


    private void startConnect() {
        device = new JsonDevice();
        device.setMac("9C:F6:DD:38:09:1D");
        device.setDeviceConnectListener(new DeviceConnectListener() {
            @Override
            public void onConnecting() {
                BleLogger.i(TAG, "onConnecting");
            }

            @Override
            public void onLoginSuccess() {
                BleLogger.i(TAG, "onConnected");

            }

            @Override
            public void onDisconnected(BleError error) {
                BleLogger.i(TAG, "onDisconnected " + error);

            }

            @Override
            public void onFailure(BleError error) {
                BleLogger.i(TAG, "onFailure " + error);

            }

        });
        device.startConnect(new AsyncProtocolCallback<Void, BleError>() {
            @Override
            public void onSuccess(Void result) {
                BleLogger.i(TAG, "onSuccess ");

            }

            @Override
            public void onFailure(BleError error) {
                BleLogger.e(TAG, "onFailure " + error);

            }
        });
    }


    public void test(View v) {
        if (device != null) {
            device.sendJson(buildJson("get_device_info", "sn"), new AsyncProtocolCallback<String, BleError>() {
                @Override
                public void onSuccess(String result) {
                    BleLogger.i(TAG, "sendJson onSuccess " + result);

                }

                @Override
                public void onFailure(BleError error) {
                    BleLogger.e(TAG, "sendJson onFailure " + error);

                }
            });
        }
    }


    private int getId() {
        int id = RandomUtil.randomInt(100000);
        if (!idList.contains(id)) {
            idList.add(id);
            return id;
        } else {
            return getId();
        }
    }

    private String buildJson(String method, Object param) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id", getId());
            jsonObject.put("method", method);
            jsonObject.put("para", param);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBleStateChangeReceiver);
        if (device == null) {
            return;
        }
        device.disconnect(null);
    }
}
