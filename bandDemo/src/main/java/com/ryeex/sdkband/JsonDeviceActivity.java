package com.ryeex.sdkband;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ryeex.ble.common.device.DeviceConnectListener;
import com.ryeex.ble.connector.callback.AsyncBleCallback;
import com.ryeex.ble.connector.error.BleError;
import com.ryeex.ble.connector.log.BleLogger;
import com.ryeex.ble.connector.utils.BleUtil;
import com.ryeex.ble.connector.utils.RandomUtil;
import com.ryeex.sdk.R;
import com.ryeex.sdkband.model.PrefsDevice;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class JsonDeviceActivity extends AppCompatActivity {


    @BindView(R.id.tv_connect_status)
    TextView tvConnectStatus;
    @BindView(R.id.et_input)
    EditText etInput;

    String inPutStr;

    private final String TAG = "DeveloperActivity";
    private List<Integer> idList = new ArrayList<>();


    private DeviceConnectListener deviceConnectListener = new DeviceConnectListener() {
        @Override
        public void onConnecting() {
            setDeviceConnectStatus("正在连接...");
        }

        @Override
        public void onLoginSuccess() {
            setDeviceConnectStatus("已连接");
        }

        @Override
        public void onDisconnected(BleError error) {
            setDeviceConnectStatus("连接断开");
        }

        @Override
        public void onFailure(BleError error) {
            setDeviceConnectStatus("连接失败");
        }
    };


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_developer);
        ButterKnife.bind(this);

        DeviceManager.getInstance().addDeviceConnectListener(deviceConnectListener);

    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        if (PrefsDevice.hasDevice()) {
            Intent intent = new Intent(this, CoreService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        }
        initConnectStatus();
    }

    private void initConnectStatus() {
        if (!BleUtil.isBleEnabled()) {
            setDeviceConnectStatus("蓝牙已关闭");
            return;
        }

        if (DeviceManager.getInstance().isLogin()) {
            setDeviceConnectStatus("已连接");
        } else {
            setDeviceConnectStatus("未连接");
        }
    }


    private void setDeviceConnectStatus(String status) {
        if (isActivityAvailable()) {
            tvConnectStatus.setText(DeviceManager.getInstance().getDevice().getMac() + "  " + status);
        }
    }


    private boolean isActivityAvailable() {
        return !isFinishing() && !isDestroyed();
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
        DeviceManager.getInstance().getDevice().disconnect(null);
        Intent intent = new Intent(this, ScanActivity.class);
        startActivity(intent);
    }

    public void coordinate_click(View v) {
        if (inPutStr.isEmpty()) {
            Toast.makeText(this, "数据不能为空", Toast.LENGTH_LONG).show();
            return;
        }

        if (TextUtils.isEmpty(inPutStr)) {
            return;
        }
        if (DeviceManager.getInstance().getDevice() != null) {
            DeviceManager.getInstance().getDevice().sendJsonRequest(inPutStr, new AsyncBleCallback<String, BleError>() {
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
        if (DeviceManager.getInstance().getDevice() != null) {
            DeviceManager.getInstance().getDevice().sendJsonRequest(buildJson("touch", "down", "160", "320"), new AsyncBleCallback<String, BleError>() {
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

        String[] tags = new String[]{"280", "240", "200", "160", "120", "80", "40"};
        for (String s : tags) {
            DeviceManager.getInstance().getDevice().sendJsonRequest(buildJson("touch", "move", "160", s), null);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        DeviceManager.getInstance().getDevice().sendJsonRequest(buildJson("touch", "up", "160", "0"), new AsyncBleCallback<String, BleError>() {
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

    public void down_slip(View v) {
        DeviceManager.getInstance().getDevice().sendJsonRequest(buildJson("touch", "down", "160", "0"), new AsyncBleCallback<String, BleError>() {
            //            device.sendJson(inPutStr, new AsyncProtocolCallback<String, BleError>() {
            @Override
            public void onSuccess(String result) {
                BleLogger.i(TAG, "sendJson onSuccess " + result);
            }

            @Override
            public void onFailure(BleError error) {
            }
        });
        String[] tags = new String[]{"40", "80", "120", "160", "200", "240", "280"};
        for (String s : tags) {
            DeviceManager.getInstance().getDevice().sendJsonRequest(buildJson("touch", "move", "160", s), null);
        }

        DeviceManager.getInstance().getDevice().sendJsonRequest(buildJson("touch", "up", "160", "320"), new AsyncBleCallback<String, BleError>() {
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

    public void right_slip(View v) {
        DeviceManager.getInstance().getDevice().sendJsonRequest(buildJson("touch", "down", "160", "0"), new AsyncBleCallback<String, BleError>() {
            //            device.sendJson(inPutStr, new AsyncProtocolCallback<String, BleError>() {
            @Override
            public void onSuccess(String result) {
                BleLogger.i(TAG, "sendJson onSuccess " + result);
            }

            @Override
            public void onFailure(BleError error) {
            }
        });
        String[] tags = new String[]{"40", "80", "120", "160", "200", "240", "280"};
        for (String s : tags) {
            DeviceManager.getInstance().getDevice().sendJsonRequest(buildJson("touch", "move", "160", s), null);
        }

        DeviceManager.getInstance().getDevice().sendJsonRequest(buildJson("touch", "up", "160", "320"), new AsyncBleCallback<String, BleError>() {
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

    public void left_slip(View v) {
        DeviceManager.getInstance().getDevice().sendJsonRequest(buildJson("touch", "down", "320", "160"), new AsyncBleCallback<String, BleError>() {
            //            device.sendJson(inPutStr, new AsyncProtocolCallback<String, BleError>() {
            @Override
            public void onSuccess(String result) {
                BleLogger.i(TAG, "sendJson onSuccess " + result);
            }

            @Override
            public void onFailure(BleError error) {
            }
        });
        String[] tags = new String[]{"280", "240", "200", "160", "120", "80", "40"};
        for (String s : tags) {
            DeviceManager.getInstance().getDevice().sendJsonRequest(buildJson("touch", "move", s, "160"), null);
        }

        DeviceManager.getInstance().getDevice().sendJsonRequest(buildJson("touch", "up", "0", "160"), new AsyncBleCallback<String, BleError>() {
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

    private int getId() {
        int id = RandomUtil.randomInt(100000);
        if (!idList.contains(id)) {
            idList.add(id);
            return id;
        } else {
            return getId();
        }
    }

    private String buildJson(String method, Object gesture, String x, String y) {
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
        DeviceManager.getInstance().removeDeviceConnectListener(deviceConnectListener);
    }
}
