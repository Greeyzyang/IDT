package com.ryeex.sdkband;

import android.annotation.SuppressLint;
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

public class BrandyJsonDeviceActivity extends AppCompatActivity {


    @BindView(R.id.tv_connect_status)
    TextView tvConnectStatus;
    @BindView(R.id.tv_result)
    TextView tvResult;
    @BindView(R.id.et_input)
    EditText etInput;
    String inPutStr;
    private final String TAG = "BrandyJsonDeviceActivity";
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
        setContentView(R.layout.brandyactivity_json);
        ButterKnife.bind(this);

        WatchManager.getInstance().addDeviceConnectListener(deviceConnectListener);

    }


    @SuppressLint("LongLogTag")
    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        if (PrefsDevice.hasDevice()) {
            Intent intent = new Intent(this, WatchCoreService.class);
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

        if (WatchManager.getInstance().isLogin()) {
            setDeviceConnectStatus("已连接");
        } else {
            setDeviceConnectStatus("未连接");
        }
    }


    private void setDeviceConnectStatus(String status) {
        if (isActivityAvailable()) {
            tvConnectStatus.setText(WatchManager.getInstance().getDevice().getMac() + "  " + status);
        }
    }


    private boolean isActivityAvailable() {
        return !isFinishing() && !isDestroyed();
    }

    @OnClick({R.id.btn_unbind, R.id.btn_scan, R.id.btn_click, R.id.btn_down_slip, R.id.btn_up_slip, R.id.btn_left_slip, R.id.btn_right_slip, R.id.btn_long_press,})
    public void onClick(View v) {
        setTextResult("");
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
                coordinate_click();
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

    private void setTextResult(String result) {
        tvResult.setText(result);
    }

    private void unbindDevice() {
        WatchManager.getInstance().unbind(new AsyncBleCallback<Void, BleError>() {
            @Override
            public void onSuccess(Void result) {
                Log.i(TAG, "unbindDevice onSuccess:" + GSON.toJSONString(result));
                setDeviceConnectStatus("已解绑");
//                startActivity(new Intent(BrandyJsonDeviceActivity.this, ScanActivity.class));
                Intent intent = new Intent(BrandyJsonDeviceActivity.this, ScanActivity.class);
                intent.putExtra("type", "watch");
                startActivity(intent);
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "unbindDevice onFailure:" + error);
            }
        });
    }


    public void scan() {
        WatchManager.getInstance().getDevice().disconnect(null);
        Intent intent = new Intent(this, ScanActivity.class);
        intent.putExtra("from", "json");
        startActivity(intent);
    }

    public void coordinate_click() {
        if (inPutStr.isEmpty()) {
            Toast.makeText(this, "数据不能为空", Toast.LENGTH_LONG).show();
            return;
        }

        if (TextUtils.isEmpty(inPutStr)) {
            return;
        }
        if (WatchManager.getInstance().getDevice() != null) {
            WatchManager.getInstance().getDevice().sendJsonRequest(inPutStr, new AsyncBleCallback<String, BleError>() {
                //            device.sendJson(inPutStr, new AsyncProtocolCallback<String, BleError>() {
                @Override
                public void onSuccess(String result) {
                    BleLogger.i(TAG, "sendJson onSuccess " + result);
                    setTextResult(GSON.toJSONString(result));

                }

                @Override
                public void onFailure(BleError error) {
                    setTextResult(error.toString());

                }
            });
        }
    }

    public void long_press() {
        if (WatchManager.getInstance().getDevice() != null) {
            WatchManager.getInstance().getDevice().sendJsonRequest(buildJson("touch", "down", "160", "315"), new AsyncBleCallback<String, BleError>() {
                //            device.sendJson(inPutStr, new AsyncProtocolCallback<String, BleError>() {
                @Override
                public void onSuccess(String result) {
                    BleLogger.i(TAG, "sendJson onSuccess " + result);
                    setTextResult(GSON.toJSONString(result));
                }

                @Override
                public void onFailure(BleError error) {
                    setTextResult(error.toString());
                }
            });
        }
        WatchManager.getInstance().getDevice().sendJsonRequest(buildJson("touch", "move", "160", "310"), new AsyncBleCallback<String, BleError>() {
            //            device.sendJson(inPutStr, new AsyncProtocolCallback<String, BleError>() {
            @Override
            public void onSuccess(String result) {
                BleLogger.i(TAG, "sendJson onSuccess " + result);
                setTextResult(GSON.toJSONString(result));

            }

            @Override
            public void onFailure(BleError error) {
                setTextResult(error.toString());
            }
        });
    }
    public void up_slip() {
        if (WatchManager.getInstance().getDevice() != null) {
            WatchManager.getInstance().getDevice().sendJsonRequest(buildJson("touch", "down", "160", "315"), new AsyncBleCallback<String, BleError>() {
                //            device.sendJson(inPutStr, new AsyncProtocolCallback<String, BleError>() {
                @Override
                public void onSuccess(String result) {
                    BleLogger.i(TAG, "sendJson onSuccess " + result);
                    setTextResult(GSON.toJSONString(result));
                }

                @Override
                public void onFailure(BleError error) {
                    setTextResult(error.toString());
                }
            });
        }
        String[] tags = new String[]{"285","150","35"};
        for (String s : tags) {
            WatchManager.getInstance().getDevice().sendJsonRequest(buildJson("touch", "move", "160", s), null);
//            try {
//                Thread.sleep(50);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        }

        WatchManager.getInstance().getDevice().sendJsonRequest(buildJson("touch", "up", "160", "5"), new AsyncBleCallback<String, BleError>() {
            //            device.sendJson(inPutStr, new AsyncProtocolCallback<String, BleError>() {
            @Override
            public void onSuccess(String result) {
                BleLogger.i(TAG, "sendJson onSuccess " + result);
                setTextResult(GSON.toJSONString(result));

            }

            @Override
            public void onFailure(BleError error) {
                setTextResult(error.toString());
            }
        });

    }

    public void down_slip() {
        WatchManager.getInstance().getDevice().sendJsonRequest(buildJson("touch", "down", "160", "5"), new AsyncBleCallback<String, BleError>() {
            //            device.sendJson(inPutStr, new AsyncProtocolCallback<String, BleError>() {
            @Override
            public void onSuccess(String result) {
                BleLogger.i(TAG, "sendJson onSuccess " + result);
                setTextResult(GSON.toJSONString(result));
            }

            @Override
            public void onFailure(BleError error) {
                setTextResult(error.toString());
            }
        });
        String[] tags = new String[]{"35","150","285"};
        for (String s : tags) {
            WatchManager.getInstance().getDevice().sendJsonRequest(buildJson("touch", "move", "160", s), null);
        }

        WatchManager.getInstance().getDevice().sendJsonRequest(buildJson("touch", "up", "160", "315"), new AsyncBleCallback<String, BleError>() {
            //            device.sendJson(inPutStr, new AsyncProtocolCallback<String, BleError>() {
            @Override
            public void onSuccess(String result) {
                BleLogger.i(TAG, "sendJson onSuccess " + result);
                setTextResult(GSON.toJSONString(result));

            }

            @Override
            public void onFailure(BleError error) {
                setTextResult(error.toString());


            }
        });
    }

    public void right_slip() {
        WatchManager.getInstance().getDevice().sendJsonRequest(buildJson("touch", "down", "5", "160"), new AsyncBleCallback<String, BleError>() {
            //            device.sendJson(inPutStr, new AsyncProtocolCallback<String, BleError>() {
            @Override
            public void onSuccess(String result) {
                BleLogger.i(TAG, "sendJson onSuccess " + result);
                setTextResult(GSON.toJSONString(result));
            }

            @Override
            public void onFailure(BleError error) {
                setTextResult(error.toString());
            }
        });
        String[] tags = new String[]{"35","150","285"};
//        String[] tags = new String[]{"25","45","60","75","90","105","120","135","150","165","180"};
        for (String s : tags) {
            WatchManager.getInstance().getDevice().sendJsonRequest(buildJson("touch", "move", s, "160"), null);
        }

        WatchManager.getInstance().getDevice().sendJsonRequest(buildJson("touch", "up", "315", "160"), new AsyncBleCallback<String, BleError>() {
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

    public void left_slip() {
        WatchManager.getInstance().getDevice().sendJsonRequest(buildJson("touch", "down", "315", "160"), new AsyncBleCallback<String, BleError>() {
            //            device.sendJson(inPutStr, new AsyncProtocolCallback<String, BleError>() {
            @Override
            public void onSuccess(String result) {
                BleLogger.i(TAG, "sendJson onSuccess " + result);
                setTextResult(GSON.toJSONString(result));
            }

            @Override
            public void onFailure(BleError error) {
                setTextResult(error.toString());
            }
        });
        String[] tags = new String[]{"285","150","35"};
//        String[] tags = new String[]{"295","285","270","260","240","225","210","195","180","165","150","135", "120", "105","90","75","60","45","25"};
        for (String s : tags) {
            WatchManager.getInstance().getDevice().sendJsonRequest(buildJson("touch", "move", s, "160"), null);
        }

        WatchManager.getInstance().getDevice().sendJsonRequest(buildJson("touch", "up", "5", "160"), new AsyncBleCallback<String, BleError>() {
            //            device.sendJson(inPutStr, new AsyncProtocolCallback<String, BleError>() {
            @Override
            public void onSuccess(String result) {
                BleLogger.i(TAG, "sendJson onSuccess " + result);
                setTextResult(GSON.toJSONString(result));

            }

            @Override
            public void onFailure(BleError error) {
                setTextResult(error.toString());
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
        WatchManager.getInstance().removeDeviceConnectListener(deviceConnectListener);
    }
}
