package com.ryeex.sdkband;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ryeex.band.protocol.callback.AsyncProtocolCallback;
import com.ryeex.band.protocol.device.JsonDevice;
import com.ryeex.ble.common.device.DeviceConnectListener;
import com.ryeex.ble.common.model.entity.DeviceBrightness;
import com.ryeex.ble.connector.error.BleError;
import com.ryeex.ble.connector.log.BleLogger;
import com.ryeex.ble.connector.utils.RandomUtil;
import com.ryeex.sdk.R;
import com.ryeex.sdkband.utils.GSON;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class JsonDeviceActivity extends AppCompatActivity {

    private final String TAG = "DeveloperActivity";
    private List<Integer> idList = new ArrayList<>();
    private JsonDevice device;

    @BindView(R.id.et_input)
    EditText etInput;
    String inPutStr;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_developer);
        ButterKnife.bind(this);

        IntentFilter bleStateChangeFilter = new IntentFilter();
        bleStateChangeFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBleStateChangeReceiver, bleStateChangeFilter);
//        startConnect();
        if(device != null){
            device.disconnect(null);
        }
        Intent intent = new Intent(this, ScanActivity.class);
        intent.putExtra("from", "json");
        startActivityForResult(intent, 100);
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
//                    startConnect();
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


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == 100) {
                if (data != null && data.hasExtra("mac")) {
                    String mac = data.getStringExtra("mac");
                    if (!TextUtils.isEmpty(mac)) {
                        startConnect(mac);
                    }
                }
            }
        }
    }

    private void startConnect(String mac) {
        device = new JsonDevice();
        device.setMac(mac);
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

    @OnClick({R.id.btn_scan, R.id.btn_click, R.id.btn_down_slip, R.id.btn_up_slip, R.id.btn_left_slip, R.id.btn_right_slip,})
    public void onClick(View v) {
        inPutStr = etInput.getText().toString();
        Log.i(TAG, "inPutStr:" + inPutStr);
        switch (v.getId()) {
            case R.id.btn_scan:
                scan(v);
                break;
            case R.id.btn_click:
                coordinate_click(v);
                break;
            case R.id.btn_down_slip:
                down_slip(v);
                break;
            case R.id.btn_left_slip:
                left_slip(v);
                break;
            case R.id.btn_right_slip:
                right_slip(v);
                break;
            case R.id.btn_up_slip:
                up_slip(v);
                break;
            default:
        }
    }

    public void scan(View v) {
        if(device != null){
            device.disconnect(null);
        }
        Intent intent = new Intent(this, ScanActivity.class);
        intent.putExtra("from", "json");
        startActivityForResult(intent, 100);
    }

    public void coordinate_click(View v) {
        if (inPutStr.isEmpty()) {
            Toast.makeText(this, "数据不能为空", Toast.LENGTH_LONG).show();
            return;
        }

        if(TextUtils.isEmpty(inPutStr)){
            return;
        }
        if (device != null) {
            device.sendJson(inPutStr, new AsyncProtocolCallback<String, BleError>() {
//            device.sendJson(inPutStr, new AsyncProtocolCallback<String, BleError>() {
                @Override
                public void onSuccess(String result) {
                    BleLogger.i(TAG, "sendJson onSuccess " + result);

                }

                @Override
                public void onFailure(BleError error) {


                }
            });
        }
    }

    public void up_slip(View v) {
        if (device != null) {
            device.sendJson(buildJson("touch","down","160","320"), new AsyncProtocolCallback<String, BleError>() {
                //            device.sendJson(inPutStr, new AsyncProtocolCallback<String, BleError>() {
                @Override
                public void onSuccess(String result) {
                    BleLogger.i(TAG, "sendJson onSuccess " + result);
                }
                @Override
                public void onFailure(BleError error) {
                }
            });
        }

        if(device == null){
            return;
        }
        String[] tags = new String[]{"280","240","200","160","120","80","40"};
            for (String s : tags) {
                device.sendJson(buildJson("touch", "move", "160", s), null);
            }

        if (device != null) {
            device.sendJson(buildJson("touch","up","160","0"), new AsyncProtocolCallback<String, BleError>() {
                //            device.sendJson(inPutStr, new AsyncProtocolCallback<String, BleError>() {
                @Override
                public void onSuccess(String result) {
                    BleLogger.i(TAG, "sendJson onSuccess " + result);

                }

                @Override
                public void onFailure(BleError error) {


                }
            });
        }

    }

    public void down_slip(View v) {
        if (device != null) {
            device.sendJson(buildJson("touch","down","160","0"), new AsyncProtocolCallback<String, BleError>() {
                //            device.sendJson(inPutStr, new AsyncProtocolCallback<String, BleError>() {
                @Override
                public void onSuccess(String result) {
                    BleLogger.i(TAG, "sendJson onSuccess " + result);
                }
                @Override
                public void onFailure(BleError error) {
                }
            });
        }

        if(device == null){
            return;
        }
        String[] tags = new String[]{"40","80","120","160","200","240","280"};
        for (String s : tags) {
            device.sendJson(buildJson("touch", "move", "160", s), null);
        }

        if (device != null) {
            device.sendJson(buildJson("touch","up","160","320"), new AsyncProtocolCallback<String, BleError>() {
                //            device.sendJson(inPutStr, new AsyncProtocolCallback<String, BleError>() {
                @Override
                public void onSuccess(String result) {
                    BleLogger.i(TAG, "sendJson onSuccess " + result);

                }

                @Override
                public void onFailure(BleError error) {


                }
            });
        }

    }

    public void right_slip(View v) {
        if (device != null) {
            device.sendJson(buildJson("touch","down","160","0"), new AsyncProtocolCallback<String, BleError>() {
                //            device.sendJson(inPutStr, new AsyncProtocolCallback<String, BleError>() {
                @Override
                public void onSuccess(String result) {
                    BleLogger.i(TAG, "sendJson onSuccess " + result);
                }
                @Override
                public void onFailure(BleError error) {
                }
            });
        }

        if(device == null){
            return;
        }
        String[] tags = new String[]{"40","80","120","160","200","240","280"};
        for (String s : tags) {
            device.sendJson(buildJson("touch", "move", "160", s), null);
        }

        if (device != null) {
            device.sendJson(buildJson("touch","up","160","320"), new AsyncProtocolCallback<String, BleError>() {
                //            device.sendJson(inPutStr, new AsyncProtocolCallback<String, BleError>() {
                @Override
                public void onSuccess(String result) {
                    BleLogger.i(TAG, "sendJson onSuccess " + result);

                }

                @Override
                public void onFailure(BleError error) {


                }
            });
        }

    }

    public void left_slip(View v) {
        if (device != null) {
            device.sendJson(buildJson("touch","down","320","160"), new AsyncProtocolCallback<String, BleError>() {
                //            device.sendJson(inPutStr, new AsyncProtocolCallback<String, BleError>() {
                @Override
                public void onSuccess(String result) {
                    BleLogger.i(TAG, "sendJson onSuccess " + result);
                }
                @Override
                public void onFailure(BleError error) {
                }
            });
        }

        if(device == null){
            return;
        }
        String[] tags = new String[]{"280","240","200","160","120","80","40"};
        for (String s : tags) {
            device.sendJson(buildJson("touch", "move", s, "160"), null);
        }

        if (device != null) {
            device.sendJson(buildJson("touch","up","0","160"), new AsyncProtocolCallback<String, BleError>() {
                //            device.sendJson(inPutStr, new AsyncProtocolCallback<String, BleError>() {
                @Override
                public void onSuccess(String result) {
                    BleLogger.i(TAG, "sendJson onSuccess " + result);

                }

                @Override
                public void onFailure(BleError error) {


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

    private String buildJson(String method, Object gesture,  String x, String y)  {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id", getId());
            jsonObject.put("method", method);
            jsonObject.put("gesture", gesture);
            JSONObject posjsonObject = new JSONObject();
            posjsonObject.put("x", x);
            posjsonObject.put("y", y);
            jsonObject.put("pos", posjsonObject);

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
