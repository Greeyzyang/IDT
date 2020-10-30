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
import com.ryeex.sdkband.utils.GSON;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SaturnJsonDevicesActivity extends AppCompatActivity {
    @BindView(R.id.tv_connect_status)
    TextView tvConnectStatus;
    @BindView(R.id.et_input)
    EditText etInput;

    String inPutStr;
    private final int MSG_REBOOT = 100;

    private final String TAG = "SaturnJsonDevicesActivity";
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
        setContentView(R.layout.activity_json);
        ButterKnife.bind(this);

        DeviceManager.getInstance().addDeviceConnectListener(deviceConnectListener);
//        if (!PrefsDevice.hasDevice()) {
//            startActivity(new Intent(this, ScanActivity.class));
//        }
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

    @OnClick({R.id.btn_unbind, R.id.btn_scan, R.id.btn_click, R.id.btn_down_slip, R.id.btn_up_slip, R.id.btn_left_slip, R.id.btn_right_slip,
            R.id.btn_long_press,
    })
    public void onClick(View v) {
        inPutStr = etInput.getText().toString();
        Log.i(TAG, "inPutStr:" + inPutStr);
        switch (v.getId()) {
            case R.id.btn_unbind:
                unbindDevice();
                break;
            case R.id.btn_scan:
                scan();
                break;
            case R.id.btn_click:
                coordinate_clickslip();
                break;
            case R.id.btn_down_slip:
                down_slip();
                break;
            case R.id.btn_left_slip:
                left_slip();
                break;
            case R.id.btn_right_slip:
                right_slip();
                break;
            case R.id.btn_up_slip:
                up_slip();
                break;
            case R.id.btn_long_press:
                long_press();
                break;
            default:
        }
    }


    private void unbindDevice() {
        DeviceManager.getInstance().unbind(new AsyncBleCallback<Void, BleError>() {
            @Override
            public void onSuccess(Void result) {
                Log.i(TAG, "unbindDevice onSuccess:" + GSON.toJSONString(result));
                setDeviceConnectStatus("已解绑");
                startActivity(new Intent(SaturnJsonDevicesActivity.this, ScanActivity.class));
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "unbindDevice onFailure:" + error);
            }
        });
    }


    public void scan() {
        DeviceManager.getInstance().getDevice().disconnect(null);
        Intent intent = new Intent(this, ScanActivity.class);
//        intent.putExtra("from", "json");
        startActivity(intent);
    }

    public void coordinate_clickslip() {
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

    public void long_press() {
        if (DeviceManager.getInstance().getDevice() != null) {
            DeviceManager.getInstance().getDevice().sendJsonRequest(buildJson("tp_move", "160", "160", "160", "160", "3000"), new AsyncBleCallback<String, BleError>() {
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
    public void up_slip() {
        if (DeviceManager.getInstance().getDevice() != null) {
            DeviceManager.getInstance().getDevice().sendJsonRequest(buildJson("tp_move", "160", "315", "160", "5", "500"), new AsyncBleCallback<String, BleError>() {
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

    public void down_slip() {
        if (DeviceManager.getInstance().getDevice() != null) {
            DeviceManager.getInstance().getDevice().sendJsonRequest(buildJson("tp_move", "160", "5", "160", "315", "500"), new AsyncBleCallback<String, BleError>() {
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

    public void right_slip() {
        if (DeviceManager.getInstance().getDevice() != null) {
            DeviceManager.getInstance().getDevice().sendJsonRequest(buildJson("tp_move", "5", "160", "315", "160", "500"), new AsyncBleCallback<String, BleError>() {
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

    public void left_slip() {
        if (DeviceManager.getInstance().getDevice() != null) {
            DeviceManager.getInstance().getDevice().sendJsonRequest(buildJson("tp_move", "315", "160", "5", "160", "500"), new AsyncBleCallback<String, BleError>() {
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

    private String buildJson(String method, String sx, String sy, String ex, String ey, String duration) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("method", method);
            jsonObject.put("sx", sx);
            jsonObject.put("sy", sy);
            jsonObject.put("ex", ex);
            jsonObject.put("ey", ey);
            jsonObject.put("duration", duration);
            jsonObject.put("interval", 50);

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
