package com.ryeex.sdkband;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.ryeex.band.protocol.pb.entity.PBProperty;
import com.ryeex.ble.common.device.DeviceConnectListener;
import com.ryeex.ble.common.device.IResultCallback;
import com.ryeex.ble.common.device.OnDataSyncListener;
import com.ryeex.ble.common.model.entity.AppNotification;
import com.ryeex.ble.common.model.entity.DeviceActivities;
import com.ryeex.ble.common.model.entity.DeviceAlarmClockInfo;
import com.ryeex.ble.common.model.entity.DeviceBrightness;
import com.ryeex.ble.common.model.entity.DeviceInfo;
import com.ryeex.ble.common.model.entity.DeviceProperty;
import com.ryeex.ble.common.model.entity.DeviceRunState;
import com.ryeex.ble.common.model.entity.DoNotDisturbSetting;
import com.ryeex.ble.common.model.entity.FirmwareUpdateInfo;
import com.ryeex.ble.common.model.entity.HeartRateSetting;
import com.ryeex.ble.common.model.entity.RaiseToWakeSetting;
import com.ryeex.ble.common.model.entity.SitRemindSetting;
import com.ryeex.ble.common.model.entity.UserConfig;
import com.ryeex.ble.common.model.entity.WeatherInfo;
import com.ryeex.ble.common.utils.FileUtil;
import com.ryeex.ble.connector.BleEngine;
import com.ryeex.ble.connector.callback.AsyncBleCallback;
import com.ryeex.ble.connector.error.BleError;
import com.ryeex.ble.connector.handler.BleHandler;
import com.ryeex.ble.connector.utils.BleUtil;
import com.ryeex.sdk.R;
import com.ryeex.sdkband.model.PrefsDevice;
import com.ryeex.sdkband.utils.GSON;
import com.ryeex.watch.adapter.model.entity.DeviceDataSet;
import com.ryeex.watch.adapter.model.entity.DeviceLanguage;
import com.ryeex.watch.adapter.model.entity.DeviceSurfaceInfo;
import com.ryeex.watch.adapter.model.entity.DrinkWaterRemindSetting;
import com.ryeex.watch.protocol.pb.entity.PBWeather;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SaturnPbDevicesActivity extends AppCompatActivity {
    private final String TAG = "SaturnPbDevicesActivity";

    @BindView(R.id.tv_connect_status)
    TextView tvConnectStatus;
    @BindView(R.id.tv_result)
    TextView tvResult;
    @BindView(R.id.et_input)
    EditText etInput;
    String inPutStr;

    private final int MSG_REBOOT = 100;
    private List<Integer> idList = new ArrayList<>();
    private String fileDir = BleEngine.getAppContext().getExternalFilesDir(null).getPath() + File.separator + "Update_File";
    private String logDir = BleEngine.getAppContext().getExternalFilesDir(null).getPath() + File.separator + "Device_Log";

    private DeviceConnectListener deviceConnectListener = new DeviceConnectListener() {
        @Override
        public void onConnecting() {
            setDeviceConnectStatus("正在连接...");
        }

        @Override
        public void onLoginSuccess() {
            setDeviceConnectStatus("已连接");
            uiHandler.removeCallbacksAndMessages(null);
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


    private Handler uiHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MSG_REBOOT:
                    WatchManager.getInstance().getDevice().login(new AsyncBleCallback<Void, BleError>() {
                        @Override
                        public void onSuccess(Void result) {
                            removeMessages(MSG_REBOOT);
                        }

                        @Override
                        public void onFailure(BleError error) {
//                            sendEmptyMessageDelayed(MSG_REBOOT, 5000);
                        }
                    });
                    sendEmptyMessageDelayed(MSG_REBOOT, 20000);
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.saturnactivity_pb);
        ButterKnife.bind(this);
//        BleHandler.getWorkerHandler().post(new Runnable() {
//            @Override
//            public void run() {
//                copyAssets("1.3.0.501");
//                copyAssets("501");
//            }
//        });
        WatchManager.getInstance().addDeviceConnectListener(deviceConnectListener); //每次进入页面重新连接设备
//        if (!PrefsDevice.hasDevice()) {
//            startActivity(new Intent(this, ScanActivity.class));
//        }   //禁止进入PB界面时自动进入扫描页面
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.i(TAG, "onNewIntent");

    }

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
            AndPermission.with(this)
                    .runtime()
                    .permission(Permission.READ_PHONE_STATE, Permission.READ_CONTACTS, Permission.READ_CALL_LOG)
                    .onGranted(permissions -> {

                    })
                    .onDenied(permissions -> {

                    })
                    .start();
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


    @OnClick({R.id.tv_unbind, R.id.tv_device_info, R.id.tv_device_property, R.id.tv_device_activity, R.id.tv_device_data,
            R.id.tv_find_device, R.id.tv_reboot_device, R.id.tv_send_notification, R.id.tv_app_list, R.id.tv_set_app_list,
            R.id.tv_getDoNotDisturb, R.id.tv_setDoNotDisturb, R.id.tv_getDeviceRaiseToWake, R.id.tv_setDeviceRaiseToWake,
            R.id.tv_getHeartRateDetect, R.id.tv_setHeartRateDetect, R.id.tv_getDeviceBrightness, R.id.tv_setDeviceBrightness,
            R.id.tv_getHomeVibrateSetting, R.id.tv_setHomeVibrateSetting, R.id.tv_setUnlock, R.id.tv_getUnlock,
            R.id.tv_setUserConfig, R.id.tv_getUserConfig, R.id.tv_setSitRemindSetting, R.id.tv_getSitRemindSetting,
            R.id.tv_setGoalRemindSetting, R.id.tv_getGoalRemindSetting, R.id.tv_setTargetStep, R.id.tv_getTargetStep,
            R.id.tv_setWeatherNotifyStatus, R.id.tv_getWeatherNotifyStatus, R.id.tv_getDeviceRunState, R.id.tv_getDeviceLogFile,
            R.id.tv_getDeviceAlarmClockList, R.id.tv_saveDeviceAlarmClock, R.id.tv_deleteDeviceAlarmClock, R.id.tv_ota,
            R.id.tv_installSurface, R.id.tv_deleteSurface, R.id.tv_bluetooth_control, R.id.tv_getDrinkWaterRemindSetting,
            R.id.tv_setDrinkWaterRemindSetting, R.id.tv_updateWeatherInfo, R.id.tv_setDeviceLanguage
//            R.id.tv_getSurfaceList, R.id.send_json
    })
    public void onClick(View v) {
        setTextResult("");
        inPutStr = etInput.getText().toString();
        Log.i(TAG, "inPutStr:" + inPutStr);
        switch (v.getId()) {
            case R.id.tv_unbind:
                unbindDevice(v);
                break;
            case R.id.tv_device_info:
                getDeviceInfo(v);
                break;
            case R.id.tv_device_property:
                getDeviceProperty(v);
                break;
            case R.id.tv_device_activity:
                getDeviceActivity(v);
                break;
            case R.id.tv_device_data:
                syncDeviceData(v);
                break;
            case R.id.tv_find_device:
                findDevice(v);
                break;
            case R.id.tv_reboot_device:
                rebootDevice(v);
                break;
            case R.id.tv_send_notification:
                sendNotification(v);
                break;
            case R.id.tv_app_list:
                getDeviceAppList(v);
                break;
            case R.id.tv_set_app_list:
                setDeviceAppList(v);
                break;
            case R.id.tv_getDoNotDisturb:
                getDoNotDisturb(v);
                break;
            case R.id.tv_setDoNotDisturb:
                setDoNotDisturb(v);
                break;
            case R.id.tv_getDeviceRaiseToWake:
                getDeviceRaiseToWake(v);
                break;
            case R.id.tv_setDeviceRaiseToWake:
                setDeviceRaiseToWake(v);
                break;
            case R.id.tv_getHeartRateDetect:
                getHeartRateDetect(v);
                break;
            case R.id.tv_setHeartRateDetect:
                setHeartRateDetect(v);
                break;
            case R.id.tv_getDeviceBrightness:
                getDeviceBrightness(v);
                break;
            case R.id.tv_setDeviceBrightness:
                setDeviceBrightness(v);
                break;
            case R.id.tv_getHomeVibrateSetting:
                getHomeVibrateSetting(v);
                break;
            case R.id.tv_setHomeVibrateSetting:
                setHomeVibrateSetting(v);
                break;
            case R.id.tv_setUnlock:
                setUnlockType(v);
                break;
            case R.id.tv_getUnlock:
                getUnlockType(v);
                break;
            case R.id.tv_getDeviceAlarmClockList:
                getDeviceAlarmClockList(v);
                break;
            case R.id.tv_saveDeviceAlarmClock:
                saveDeviceAlarmClock(v);
                break;
            case R.id.tv_deleteDeviceAlarmClock:
                deleteDeviceAlarmClock(v);
                break;
            case R.id.tv_setUserConfig:
                setUserConfig(v);
                break;
            case R.id.tv_getUserConfig:
                getUserConfig(v);
                break;
            case R.id.tv_setSitRemindSetting:
                setSitRemindSetting(v);
                break;
            case R.id.tv_getSitRemindSetting:
                getSitRemindSetting(v);
                break;
            case R.id.tv_setGoalRemindSetting:
                setGoalRemindSetting(v);
                break;
            case R.id.tv_getGoalRemindSetting:
                getGoalRemindSetting(v);
                break;
            case R.id.tv_setTargetStep:
                setTargetStep(v);
                break;
            case R.id.tv_getTargetStep:
                getTargetStep(v);
                break;
            case R.id.tv_setWeatherNotifyStatus:
                setWeatherNotifyStatus(v);
                break;
            case R.id.tv_getWeatherNotifyStatus:
                getWeatherNotifyStatus(v);
                break;
            case R.id.tv_getDeviceRunState:
                getDeviceRunState(v);
                break;
            case R.id.tv_getDeviceLogFile:
                getDeviceLogFile(v);
                break;
            case R.id.tv_ota:
                startOta(v);
                break;
            case R.id.tv_installSurface:
                installSurface(v);
                break;
            case R.id.tv_deleteSurface:
                deleteSurface(v);
                break;
            case R.id.tv_bluetooth_control:
                bluetooth_control(v);
                break;
            case R.id.tv_setDrinkWaterRemindSetting:
                setDrinkWaterRemindSetting(v);
                break;
            case R.id.tv_getDrinkWaterRemindSetting:
                getDrinkWaterRemindSetting(v);
                break;
            case R.id.tv_updateWeatherInfo:
                updateWeatherInfo(v);
                break;
            case R.id.tv_setDeviceLanguage:
                setDeviceLanguage(v);
                break;
            default:
        }
    }


    private void setTextResult(String result) {
        tvResult.setText(result);
    }

    private void unbindDevice(View view) {
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
        WatchManager.getInstance().unbind(new AsyncBleCallback<Void, BleError>() {
            @Override
            public void onSuccess(Void result) {
                Log.i(TAG, "unbindDevice onSuccess:" + GSON.toJSONString(result));
                view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
                setDeviceConnectStatus("已解绑");
//                startActivity(new Intent(SaturnPbDevicesActivity.this, WatchScanActivity.class));
                Intent intent = new Intent(SaturnPbDevicesActivity.this, ScanActivity.class);
                intent.putExtra("type", "watch");
                startActivity(intent);
                BleHandler.getUiHandler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
                    }
                }, 200);
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "unbindDevice onFailure:" + error);
                view.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        });
    }


    private void getDeviceInfo(View view) {
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
        WatchManager.getInstance().getDevice().getDeviceInfo(new AsyncBleCallback<DeviceInfo, BleError>() {
            @Override
            public void onSuccess(DeviceInfo result) {
                Log.i(TAG, "getDeviceInfo onSuccess:" + GSON.toJSONString(result));
                setTextResult(GSON.toJSONString(result));
                view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "getDeviceInfo onFailure:" + error);
                setTextResult(error.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        });
    }


    private void getDeviceProperty(View view) {
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
        WatchManager.getInstance().getDevice().getDeviceProperty(new AsyncBleCallback<DeviceProperty, BleError>() {
            @Override
            public void onSuccess(DeviceProperty result) {
                Log.i(TAG, "getDeviceProperty onSuccess:" + GSON.toJSONString(result));
                setTextResult(GSON.toJSONString(result));
                view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "getDeviceProperty onFailure:" + error);
                setTextResult(error.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        });
    }

    private void getDeviceActivity(View view) {
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
        WatchManager.getInstance().getDevice().getDeviceActivities(new AsyncBleCallback<DeviceActivities, BleError>() {
            @Override
            public void onSuccess(DeviceActivities result) {
                Log.i(TAG, "getDeviceActivity onSuccess:" + GSON.toJSONString(result));
                setTextResult(GSON.toJSONString(result));
                view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "getDeviceActivity onFailure:" + error);
                setTextResult(error.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        });
    }

    private void syncDeviceData(View view) {
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
        WatchManager.getInstance().getDevice().syncDeviceData(new OnDataSyncListener<List<DeviceDataSet>>() {
//        WatchManager.getInstance().getDevice().syncDeviceData(new OnDataSyncListener<List<com.ryeex.watch.adapter.model.entity.DeviceDataSet>>() {
            @Override
            public void onStart(int total) {
                //返回total总包数
                Log.i(TAG, "syncDeviceData onStart:" + total);

            }

            @Override
            public void onProgress(float progress, int currentNum, int total) {
                Log.i(TAG, "syncDeviceData onProgress progress=" + progress + " currentNum=" + currentNum + " total=" + total);
            }

            @Override
            public void onSuccess(List<DeviceDataSet> result, IResultCallback resultCallback) {
                Log.i(TAG, "syncDeviceData onSuccess result:" + GSON.toJSONString(result));
                setTextResult(GSON.toJSONString(result));
                view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
                if (resultCallback != null) {
                    resultCallback.onResult(true);
                }
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "syncDeviceData onFailure:" + error);
                setTextResult(error.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        });
    }


    private void findDevice(View view) {
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
        WatchManager.getInstance().getDevice().findDevice(true, new AsyncBleCallback<Void, BleError>() {
            @Override
            public void onSuccess(Void result) {
                Log.i(TAG, "findDevice onSuccess");
                setTextResult("set success");
                view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "findDevice onFailure:" + error);
                setTextResult(error.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        });
    }

    private void rebootDevice(View view) {
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
        WatchManager.getInstance().getDevice().rebootDevice(new AsyncBleCallback<Void, BleError>() {
            @Override
            public void onSuccess(Void result) {
                Log.i(TAG, "rebootDevice onSuccess");
                setTextResult("set success");
                view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "rebootDevice onFailure:" + error);
                setTextResult(error.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        });
    }

    private void sendNotification(View view) {
        if (inPutStr.isEmpty()) {
            Toast.makeText(this, "数据不能为空", Toast.LENGTH_LONG).show();
            return;
        }
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
//        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
        AppNotification appNotification = GSON.parseObject(inPutStr, AppNotification.class);

//        //app消息
//        String title = "测试title";
//        String text = "测试text";
//        String appKey = NotificationUtil.getAppKeyByPackageName(NotificationConst.PACKAGE_NAME_WX, "");
//        AppNotification appNotification = new AppNotification();
//        appNotification.setType(AppNotification.Type.APP_MESSAGE);
//        AppNotification.AppMessage appMessage = new AppNotification.AppMessage();
//        appMessage.setAppId(appKey);
//
//        if (!TextUtils.isEmpty(title)) {
//            if (title.length() <= 50) {
//                appMessage.setTitle(title);
//            } else {
//                String maxTitle = title.substring(0, 46) + "...";
//                appMessage.setTitle(maxTitle);
//            }
//        }
//        if (!TextUtils.isEmpty(text)) {
//            if (text.length() <= 400) {
//                appMessage.setText(text);
//            } else {
//                String maxText = text.substring(0, 395) + "...";
//                appMessage.setText(maxText);
//            }
//        }
//        appNotification.setAppMessage(appMessage);


//        //来电
//        AppNotification appNotification = new AppNotification();
//        appNotification.setType(AppNotification.Type.TELEPHONY);
//        AppNotification.Telephony telephony = new AppNotification.Telephony();
//        //联系人
//        telephony.setContact("文杰");
//        //来电号码
//        telephony.setNumber("1234567");
//        // CONNECTED 接听
//        // DISCONNECTED 挂断
//        // RINGING_UNANSWERABLE 响铃
//        //根据来电状态更改
//        telephony.setStatus(AppNotification.Telephony.Status.RINGING_UNANSWERABLE);
//        appNotification.setTelephony(telephony);

//        //短信
//        AppNotification appNotification = new AppNotification();
//        appNotification.setType(AppNotification.Type.SMS);
//        AppNotification.Sms sms = new AppNotification.Sms();
//        //联系人
//        sms.setContact("文杰");
//        //短信内容
//        sms.setContent("测试短信");
//        //来信号码
//        sms.setSender("1234567");
//        appNotification.setSms(sms);

//        Log.i(TAG, "doNotDisturbSetting:"+GSON.toJSONString(appNotification));


        WatchManager.getInstance().getDevice().sendNotification(appNotification, new AsyncBleCallback<Void, BleError>() {
            @Override
            public void onSuccess(Void result) {
                Log.i(TAG, "sendNotification onSuccess");
                setTextResult("set success");
                view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "sendNotification onFailure:" + error);
                setTextResult(error.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        });
    }


    List<Integer> deviceAppList;

    private void getDeviceAppList(View view) {
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
        WatchManager.getInstance().getDevice().getDeviceAppList(new AsyncBleCallback<List<Integer>, BleError>() {
            @Override
            public void onSuccess(List<Integer> result) {
                deviceAppList = result;
                Log.i(TAG, "getDeviceAppList onSuccess:" + result.toString());
                setTextResult(result.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "getDeviceAppList onFailure:" + error);
                setTextResult(error.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        });
    }


    private void setDeviceAppList(View view) {
        if (inPutStr.isEmpty()) {
            Toast.makeText(this, "数据不能为空", Toast.LENGTH_LONG).show();
            return;
        }
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
        String[] arr = inPutStr.split(",");
        List<Integer> deviceAppList = new ArrayList<Integer>();
        for (String s : arr) {
            deviceAppList.add(Integer.parseInt(s));
        }
//        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
        if (deviceAppList != null) {
//        if (deviceAppList != null) {
            WatchManager.getInstance().getDevice().setDeviceAppList(deviceAppList, new AsyncBleCallback<Void, BleError>() {
                //            WatchManager.getInstance().getDevice().setDeviceAppList(deviceAppList, new AsyncBleCallback<Void, BleError>() {
                @Override
                public void onSuccess(Void result) {
                    Log.i(TAG, "setDeviceAppList onSuccess");
                    setTextResult("set success");
                    view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
                }

                @Override
                public void onFailure(BleError error) {
                    Log.e(TAG, "setDeviceAppList onFailure:" + error);
                    setTextResult(error.toString());
                    view.setBackgroundColor(getResources().getColor(R.color.colorRed));
                }
            });
        }
    }

    float finishProgress = 0;

    private void startOta(View view) {
        //inPutStr    固件包 资源包 升级类型（1：全资源/0：差分资源）
        if (inPutStr.isEmpty()) {
            Toast.makeText(this, "数据不能为空", Toast.LENGTH_LONG).show();
            return;
        }
        Log.d(TAG, "fileDir------" + fileDir);
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
        Log.i(TAG, "updateFirmware getDevice:" + GSON.toJSONString(DeviceManager.getInstance().getDevice()));
        String [] arr = inPutStr.split(",");
        List<String> list = new ArrayList<>();
        for (String s : arr){
            list.add(s);
        }
        Log.d(TAG, "list------" + list);
        //TODO demo是用assets资源，实际是要从云端下载
        FirmwareUpdateInfo firmwareUpdateInfo = new FirmwareUpdateInfo();
        List<FirmwareUpdateInfo.UpdateItem> items = new ArrayList<>();
        //是否强制升级
        firmwareUpdateInfo.setForce(false);
        firmwareUpdateInfo.setVersion(list.get(0));

        FirmwareUpdateInfo.UpdateItem updateItem = new FirmwareUpdateInfo.UpdateItem();
        //0资源包 1固件包 注意有些版本是两个包有都的，demo这两个版本是没有资源包的
        updateItem.setId(1);
        updateItem.setLocalPath(fileDir + File.separator + list.get(0));
        File files = new File(fileDir + File.separator + list.get(0));
        String md5str = md5ForFile(files);
        Log.d(TAG, "md5str------" + md5str);
        updateItem.setMd5(md5str);
        File file = new File(updateItem.getLocalPath());
        updateItem.setLength((int) file.length());
        items.add(updateItem);

        if (!list.get(1).equals("0")) {
            FirmwareUpdateInfo.UpdateItem updateItem1 = new FirmwareUpdateInfo.UpdateItem();
            updateItem1.setId(0);
            updateItem1.setLocalPath(fileDir + File.separator + list.get(1));
            File files1 = new File(fileDir + File.separator + list.get(1));
            String md5str1 = md5ForFile(files1);
            Log.d(TAG, "md5str1------" + md5str1);
            updateItem1.setMd5(md5str1);
            File file1 = new File(updateItem1.getLocalPath());
            updateItem1.setLength((int) file1.length());
            items.add(updateItem1);
        }

        firmwareUpdateInfo.setUrlList(items);
        if (list.get(2).equals("1")) {
            firmwareUpdateInfo.setResFull(true);
        }
        else if (list.get(2).equals("0")) {
            firmwareUpdateInfo.setResFull(false);
        }
        Log.i(TAG, "updateFirmware firmwareUpdateInfo:" + GSON.toJSONString(firmwareUpdateInfo));

        WatchManager.getInstance().getDevice().updateFirmware(firmwareUpdateInfo, new AsyncBleCallback<Void, BleError>() {
            @Override
            public void onUpdate(Bundle bundle) {
//                if (!WatchManager.getInstance().getDevice().isLogin())
//                    WatchManager.getInstance().getDevice().login();
                float totalLength = bundle.getFloat(BleEngine.KEY_LENGTH);
                float speed = bundle.getFloat(BleEngine.KEY_SPEED);
                int leftSeconds = (int) ((totalLength * (1 - finishProgress / 100)) / speed);

                Log.i(TAG, "updateFirmware onUpdate length=" + totalLength + " speed=" + speed + " time=" + leftSeconds);
            }

            @Override
            public void onProgress(float progress) {
                Log.i(TAG, "updateFirmware onProgress:" + progress);
                finishProgress = progress;
                setTextResult(String.valueOf(progress));
            }

            @Override
            public void onSuccess(Void result) {
                Log.i(TAG, "updateFirmware onSuccess");
                setTextResult("set success");
                view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
                WatchManager.getInstance().getDevice().disconnect(null);
                uiHandler.sendEmptyMessageDelayed(MSG_REBOOT, 20 * DateUtils.SECOND_IN_MILLIS);
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "updateFirmware onFailure:" + error);
                setTextResult(error.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        });
    }


    public static String md5ForFile(File file){
        int buffersize = 1024;
        FileInputStream fis = null;
        DigestInputStream dis = null;

        try {
            //创建MD5转换器和文件流
            MessageDigest messageDigest =MessageDigest.getInstance("MD5");
            fis = new FileInputStream(file);
            dis = new DigestInputStream(fis,messageDigest);

            byte[] buffer = new byte[buffersize];
            //DigestInputStream实际上在流处理文件时就在内部就进行了一定的处理
            while (dis.read(buffer) > 0);

            //通过DigestInputStream对象得到一个最终的MessageDigest对象。
            messageDigest = dis.getMessageDigest();

            // 通过messageDigest拿到结果，也是字节数组，包含16个元素
            byte[] array = messageDigest.digest();
            // 同样，把字节数组转换成字符串
            StringBuilder hex = new StringBuilder(array.length * 2);
            for (byte b : array) {
                if ((b & 0xFF) < 0x10){
                    hex.append("0");
                }
                hex.append(Integer.toHexString(b & 0xFF));
            }
            return hex.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }


    private void getDoNotDisturb(View view) {
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
        WatchManager.getInstance().getDevice().getDeviceDoNotDisturb(new AsyncBleCallback<DoNotDisturbSetting, BleError>() {
            @Override
            public void onSuccess(DoNotDisturbSetting result) {
                Log.i(TAG, "getDoNotDisturb onSuccess:" + result.toString());
                setTextResult(result.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "getDoNotDisturb onFailure:" + error);
                setTextResult(error.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        });
    }


    private void setDoNotDisturb(View view) {
        if (inPutStr.isEmpty()) {
            Toast.makeText(this, "数据不能为空", Toast.LENGTH_LONG).show();
            return;
        }
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
        DoNotDisturbSetting doNotDisturbSetting = GSON.parseObject(inPutStr, DoNotDisturbSetting.class);
//        DoNotDisturbSetting doNotDisturbSetting = new DoNotDisturbSetting();
            /*
            DISABLE = 0; // 关闭
            SMART = 1;   // 智能模式
            TIMING = 2;  // 定时模式  需要设置时间段
            ALWAYS = 3; // 一直开启
            */
//        doNotDisturbSetting.setMode(DoNotDisturbSetting.DndMode.DISABLE);
//        List<DoNotDisturbSetting.DndDuration> dndDurations = new ArrayList<>();
//        //如果是TIMING模式则需要设置时间段
//        DoNotDisturbSetting.DndDuration dndDuration = new DoNotDisturbSetting.DndDuration();
//        //开始与结束时间 hour:0-24 Minute:0-60  可设置多个时间段
//        dndDuration.setStartTimeHour(10);
//        dndDuration.setStartTimeMinute(30);
//        dndDuration.setEndTimeHour(12);
//        dndDuration.setEndTimeMinute(30);
//        dndDurations.add(dndDuration);
//        doNotDisturbSetting.setDurations(dndDurations);

//        Log.i(TAG, "doNotDisturbSetting:"+GSON.toJSONString(doNotDisturbSetting));

        WatchManager.getInstance().getDevice().setDeviceDoNotDisturb(doNotDisturbSetting, new AsyncBleCallback<Void, BleError>() {
            @Override
            public void onSuccess(Void result) {
                Log.i(TAG, "setDoNotDisturb onSuccess");
                setTextResult("set success");
                view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "setDoNotDisturb onFailure:" + error);
                setTextResult(error.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        });
    }


    private void getDeviceRaiseToWake(View view) {
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
        WatchManager.getInstance().getDevice().getDeviceRaiseToWake(new AsyncBleCallback<RaiseToWakeSetting, BleError>() {
            @Override
            public void onSuccess(RaiseToWakeSetting result) {
                Log.i(TAG, "getDeviceRaiseToWake onSuccess");
                setTextResult(result.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "getDeviceRaiseToWake onFailure:" + error);
                setTextResult(error.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        });
    }

    private void setDeviceRaiseToWake(View view) {
        if (inPutStr.isEmpty()) {
            Toast.makeText(this, "数据不能为空", Toast.LENGTH_LONG).show();
            return;
        }
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
        RaiseToWakeSetting raiseToWakeSetting = GSON.parseObject(inPutStr, RaiseToWakeSetting.class);
//        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
//
//        RaiseToWakeSetting raiseToWakeSetting = new RaiseToWakeSetting();
//        raiseToWakeSetting.setEnable(false);
        //开始与结束时间 hour:0-24 Minute:0-60
//        raiseToWakeSetting.setStartTimeHour(10);
//        raiseToWakeSetting.setStartTimeMinute(10);
//        raiseToWakeSetting.setEndTimeHour(12);
//        raiseToWakeSetting.setEndTimeMinute(12);

        WatchManager.getInstance().getDevice().setDeviceRaiseToWake(raiseToWakeSetting, new AsyncBleCallback<Void, BleError>() {
            @Override
            public void onSuccess(Void result) {
                Log.i(TAG, "setDeviceRaiseToWake onSuccess");
                setTextResult("set success");
                view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "setDeviceRaiseToWake onFailure:" + error);
                setTextResult(error.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        });
    }

    private void getHeartRateDetect(View view) {
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
        WatchManager.getInstance().getDevice().getHeartRateDetect(new AsyncBleCallback<HeartRateSetting, BleError>() {
            @Override
            public void onSuccess(HeartRateSetting result) {
                Log.i(TAG, "getHeartRateDetect onSuccess");
                setTextResult(result.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "getHeartRateDetect onFailure:" + error);
                setTextResult(error.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        });
    }

    private void setHeartRateDetect(View view) {
        if (inPutStr.isEmpty()) {
            Toast.makeText(this, "数据不能为空", Toast.LENGTH_LONG).show();
            return;
        }
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
        HeartRateSetting heartRateSetting = GSON.parseObject(inPutStr, HeartRateSetting.class);
//        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
//        HeartRateSetting heartRateSetting = new HeartRateSetting();
//        heartRateSetting.setEnable(false);
        //检测间隔  单位分钟
//        heartRateSetting.setInterval(5);
        WatchManager.getInstance().getDevice().setHeartRateDetect(heartRateSetting, new AsyncBleCallback<Void, BleError>() {
            @Override
            public void onSuccess(Void result) {
                Log.i(TAG, "setHeartRateDetect onSuccess");
                setTextResult("set success");
                view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "setHeartRateDetect onFailure:" + error);
                setTextResult(error.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        });
    }

    private void getDeviceBrightness(View view) {
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
        WatchManager.getInstance().getDevice().getDeviceBrightness(new AsyncBleCallback<DeviceBrightness, BleError>() {
            @Override
            public void onSuccess(DeviceBrightness result) {
                Log.i(TAG, "getDeviceBrightness onSuccess");
                setTextResult(result.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "getDeviceBrightness onFailure:" + error);
                setTextResult(error.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        });
    }

    private void setDeviceBrightness(View view) {
        if (inPutStr.isEmpty()) {
            Toast.makeText(this, "数据不能为空", Toast.LENGTH_LONG).show();
            return;
        }
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
        DeviceBrightness deviceBrightness = GSON.parseObject(inPutStr, DeviceBrightness.class);
//        if (!inPutStr.equalsIgnoreCase("1") && !inPutStr.equalsIgnoreCase("2") && !inPutStr.equalsIgnoreCase("3")) {
//            Toast.makeText(this, "数据格式错", Toast.LENGTH_LONG).show();
//            return;
//        }
//        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
//        DeviceBrightness deviceBrightness;
//        if (inPutStr.equalsIgnoreCase("1")) {
//            deviceBrightness = DeviceBrightness.LOW;
//        } else if (inPutStr.equalsIgnoreCase("2")) {
//            deviceBrightness = DeviceBrightness.MID;
//        } else {
//            deviceBrightness = DeviceBrightness.HIGH;
//        }
        WatchManager.getInstance().getDevice().setDeviceBrightness(deviceBrightness, new AsyncBleCallback<Void, BleError>() {
            @Override
            public void onSuccess(Void result) {
                Log.i(TAG, "setDeviceBrightness onSuccess");
                setTextResult("set success");
                view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "setDeviceBrightness onFailure:" + error);
                setTextResult(error.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        });
    }


    private void getHomeVibrateSetting(View view) {
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
        WatchManager.getInstance().getDevice().getHomeVibrateSetting(new AsyncBleCallback<Boolean, BleError>() {
            @Override
            public void onSuccess(Boolean result) {
                Log.i(TAG, "getHomeVibrateSetting onSuccess");
                setTextResult(result.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "getHomeVibrateSetting onFailure:" + error);
                setTextResult(error.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        });
    }

    private void setHomeVibrateSetting(View view) {
        if (inPutStr.isEmpty()) {
            Toast.makeText(this, "数据不能为空", Toast.LENGTH_LONG).show();
            return;
        }
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
        WatchManager.getInstance().getDevice().setHomeVibrateSetting(Boolean.parseBoolean(inPutStr), new AsyncBleCallback<Void, BleError>() {
            //        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
//        WatchManager.getInstance().getDevice().setHomeVibrateSetting(true, new AsyncBleCallback<Void, BleError>() {
            @Override
            public void onSuccess(Void result) {
                Log.i(TAG, "setHomeVibrateSetting onSuccess");
                setTextResult("set success");
                view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "setHomeVibrateSetting onFailure:" + error);
                setTextResult(error.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        });
    }


    private void setUnlockType(View view) {
        if (inPutStr.isEmpty()) {
            Toast.makeText(this, "数据不能为空", Toast.LENGTH_LONG).show();
            return;
        }
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
        WatchManager.getInstance().getDevice().setUnlockType(Integer.parseInt(inPutStr), new AsyncBleCallback<Void, BleError>() {
            //        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
            //0:禁用解锁, 1:上滑解锁
//        WatchManager.getInstance().getDevice().setUnlockType(1, new AsyncBleCallback<Void, BleError>() {
            @Override
            public void onSuccess(Void result) {
                Log.i(TAG, "setUnlockType onSuccess");
                setTextResult("set success");
                view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "setUnlockType onFailure:" + error);
                setTextResult(error.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        });
    }

    private void getUnlockType(View view) {
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
        WatchManager.getInstance().getDevice().getUnlockType(new AsyncBleCallback<Integer, BleError>() {
            @Override
            public void onSuccess(Integer result) {
                Log.i(TAG, "getUnlockType onSuccess");
                setTextResult(result.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "getUnlockType onFailure:" + error);
                setTextResult(error.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        });
    }

    private void getDeviceAlarmClockList(View view) {
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
        WatchManager.getInstance().getDevice().getDeviceAlarmClockList(new AsyncBleCallback<List<DeviceAlarmClockInfo>, BleError>() {
            @Override
            public void onSuccess(List<DeviceAlarmClockInfo> result) {
                Log.i(TAG, "getDeviceAlarmClockList onSuccess");
                setTextResult(GSON.toJSONString(result));
                view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "getDeviceAlarmClockList onFailure:" + error);
                setTextResult(error.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        });
    }

    private void saveDeviceAlarmClock(View view) {
        if (inPutStr.isEmpty()) {
            Toast.makeText(this, "数据不能为空", Toast.LENGTH_LONG).show();
            return;
        }
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
        DeviceAlarmClockInfo alarmClockInfo = GSON.parseObject(inPutStr, DeviceAlarmClockInfo.class);
        List<DeviceAlarmClockInfo> alarmClockInfolist = new ArrayList<>();
        alarmClockInfolist.add(alarmClockInfo);
//        List<DeviceAlarmClockInfo> alarmClockInfo = new ArrayList<>();
//        DeviceAlarmClockInfo saveDeviceAlarmClock = new DeviceAlarmClockInfo();
//        saveDeviceAlarmClock.setId(1);
//        saveDeviceAlarmClock.setEnable(true);
//        saveDeviceAlarmClock.setHour(12);
//        saveDeviceAlarmClock.setMinute(10);
//        saveDeviceAlarmClock.setTag("go to work");
//        alarmClockInfo.add(saveDeviceAlarmClock);


        WatchManager.getInstance().getDevice().saveDeviceAlarmClock(alarmClockInfolist, new AsyncBleCallback<Void, BleError>() {
            @Override
            public void onSuccess(Void result) {
                Log.i(TAG, "saveDeviceAlarmClock onSuccess");
                setTextResult("set success");
                view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "saveDeviceAlarmClock onFailure:" + error);
                setTextResult(error.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        });
    }

    private void deleteDeviceAlarmClock(View view) {
        if (inPutStr.isEmpty()) {
            Toast.makeText(this, "数据不能为空", Toast.LENGTH_LONG).show();
            return;
        }
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
        WatchManager.getInstance().getDevice().deleteDeviceAlarmClock(Integer.parseInt(inPutStr), new AsyncBleCallback<Void, BleError>() {

            @Override
            public void onSuccess(Void result) {
                Log.i(TAG, "deleteDeviceAlarmClock onSuccess");
                setTextResult("set success");
                view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "deleteDeviceAlarmClock onFailure:" + error);
                setTextResult(error.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        });
    }

    private void setUserConfig(View view) {
        if (inPutStr.isEmpty()) {
            Toast.makeText(this, "数据不能为空", Toast.LENGTH_LONG).show();
            return;
        }
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
//        UserConfig userConfig = new UserConfig();
//        userConfig.setTemperatureType(UserConfig.TemperatureType.C);
//        UserConfig.TimezoneConfig timezoneConfig =new UserConfig.TimezoneConfig();
//        timezoneConfig.setAuto(true);
//        timezoneConfig.setCity("Los AngeLes");
//        timezoneConfig.setOffset(-28800);
//        userConfig.setTimezoneConfig(timezoneConfig);
//        UserConfig.WeatherConfig weatherConfig = new UserConfig.WeatherConfig();
//        weatherConfig.setAuto(true);
//        weatherConfig.setCity("Shenzhen");
//        weatherConfig.setId("1795565");
//        userConfig.setWeatherConfig(weatherConfig);
        UserConfig userConfig = GSON.parseObject(inPutStr, UserConfig.class);
//        Log.i(TAG, "doNotDisturbSetting:"+GSON.toJSONString(userConfig));
        assert userConfig != null;
        WatchManager.getInstance().getDevice().setUserConfig(userConfig, new AsyncBleCallback<Void, BleError>() {

            @Override
            public void onSuccess(Void result) {
                Log.i(TAG, "setUserConfig onSuccess");
                setTextResult("set success");
                view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "setUserConfig onFailure:" + error);
                setTextResult(error.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        });
    }

    private void getUserConfig(View view) {
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
        WatchManager.getInstance().getDevice().getUserConfig(new AsyncBleCallback<UserConfig, BleError>() {
            @Override
            public void onSuccess(UserConfig result) {
                Log.i(TAG, "getUserConfig onSuccess");
                setTextResult(GSON.toJSONString(result));
                view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "getUserConfig onFailure:" + error);
                setTextResult(error.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        });
    }

    private void setSitRemindSetting(View view) {
        if (inPutStr.isEmpty()) {
            Toast.makeText(this, "数据不能为空", Toast.LENGTH_LONG).show();
            return;
        }
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
        SitRemindSetting sitRemindSetting = GSON.parseObject(inPutStr, SitRemindSetting.class);
//        SitRemindSetting sitRemindSetting = new SitRemindSetting();
//        sitRemindSetting.setEndTimeHour(-1);
//        sitRemindSetting.setEndTimeMinute(-1);
//        sitRemindSetting.setStartTimeHour(-1);
//        sitRemindSetting.setStartTimeMinute(-1);
//        sitRemindSetting.isSitRemindEnable();

//        Log.i(TAG, "doNotDisturbSetting:"+GSON.toJSONString(sitRemindSetting));
        assert sitRemindSetting != null;
        WatchManager.getInstance().getDevice().setSitRemindSetting(sitRemindSetting, new AsyncBleCallback<Void, BleError>() {

            @Override
            public void onSuccess(Void result) {
                Log.i(TAG, "setSitRemindSetting onSuccess");
                setTextResult("set success");
                view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "setSitRemindSetting onFailure:" + error);
                setTextResult(error.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        });
    }

    private void getSitRemindSetting(View view) {
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
        WatchManager.getInstance().getDevice().getSitRemindSetting(new AsyncBleCallback<SitRemindSetting, BleError>() {
            @Override
            public void onSuccess(SitRemindSetting result) {
                Log.i(TAG, "getSitRemindSetting onSuccess");
                setTextResult(GSON.toJSONString(result));
                view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "getSitRemindSetting onFailure:" + error);
                setTextResult(error.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        });
    }

    private void updateWeatherInfo(View view) {
//        if (inPutStr.isEmpty()) {
//            Toast.makeText(this, "数据不能为空", Toast.LENGTH_LONG).show();
//            return;
//        }
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
//        WeatherInfo weatherInfo = GSON.parseObject(inPutStr, WeatherInfo.class);
//        WeatherInfo weatherInfo = new WeatherInfo();
//        weatherInfo.setCityName("Shenzhen");
//        weatherInfo.setWeatherDetail();
        String info = "{\"cityName\":\"shenzhen\",\"weatherDetail\":{\"dailyWeather\":{\"cityName\":\"shenzhen\",\"firstDateTime\":1606752000,\"items\":[{\"aqi\":0,\"date\":1606752000,\"maxTemperature\":23,\"minTemperature\":15,\"type\":0},{\"aqi\":0,\"date\":1606838400,\"maxTemperature\":24,\"minTemperature\":16,\"type\":0},{\"aqi\":0,\"date\":1606924800,\"maxTemperature\":24,\"minTemperature\":14,\"type\":0},{\"aqi\":0,\"date\":1607011200,\"maxTemperature\":22,\"minTemperature\":13,\"type\":0},{\"aqi\":0,\"date\":1607097600,\"maxTemperature\":23,\"minTemperature\":15,\"type\":0},{\"aqi\":0,\"date\":1607184000,\"maxTemperature\":25,\"minTemperature\":16,\"type\":0}]},\"sectionWeather\":{\"cityName\":\"shenzhen\",\"interval\":86400,\"items\":[{\"humidity\":55,\"sunriseTime\":0,\"sunsetTime\":0,\"temperature\":20,\"type\":0,\"uv\":1,\"windSpeed\":0}],\"startTime\":1606752000}}}";
        WeatherInfo weatherInfo = GSON.parseObject(info, WeatherInfo.class);
        assert weatherInfo != null;
        WatchManager.getInstance().getDevice().updateWeatherInfo(weatherInfo, new AsyncBleCallback<Void, BleError>() {
            @Override
            public void onSuccess(Void result) {
                Log.i(TAG, "updateWeatherInfo onSuccess");
                setTextResult("set success");
            }
            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "updateWeatherInfo onFailure:" + error);
                setTextResult(error.toString());
            }
        });
//        Log.i(TAG, "doNotDisturbSetting:"+GSON.toJSONString(weatherInfo));

    }

    private void setDeviceLanguage(View view) {
        if (inPutStr.isEmpty()) {
            Toast.makeText(this, "数据不能为空", Toast.LENGTH_LONG).show();
            return;
        }
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
        DeviceLanguage deviceLanguage = GSON.parseObject(inPutStr, DeviceLanguage.class);
        WatchManager.getInstance().getDevice().setDeviceLanguage(deviceLanguage, new AsyncBleCallback<Void, BleError>() {

            @Override
            public void onSuccess(Void result) {
                Log.i(TAG, "setDeviceLanguage onSuccess");
                setTextResult("set success");
                view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "setDeviceLanguage onFailure:" + error);
                setTextResult(error.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        });

    }


    private void setGoalRemindSetting(View view) {
        if (inPutStr.isEmpty()) {
            Toast.makeText(this, "数据不能为空", Toast.LENGTH_LONG).show();
            return;
        }
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));

//        Log.i(TAG, "doNotDisturbSetting:"+GSON.toJSONString(sitRemindSetting));
        WatchManager.getInstance().getDevice().setGoalRemindSetting(Boolean.parseBoolean(inPutStr), new AsyncBleCallback<Void, BleError>() {

            @Override
            public void onSuccess(Void result) {
                Log.i(TAG, "setGoalRemindSetting onSuccess");
                setTextResult("set success");
                view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "setGoalRemindSetting onFailure:" + error);
                setTextResult(error.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        });
    }

    private void getGoalRemindSetting(View view) {
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
        WatchManager.getInstance().getDevice().getGoalRemindSetting(new AsyncBleCallback<Boolean, BleError>() {
            @Override
            public void onSuccess(Boolean result) {
                Log.i(TAG, "getGoalRemindSetting onSuccess");
                setTextResult(GSON.toJSONString(result));
                view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "getGoalRemindSetting onFailure:" + error);
                setTextResult(error.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        });
    }

    private void setTargetStep(View view) {
        if (inPutStr.isEmpty()) {
            Toast.makeText(this, "数据不能为空", Toast.LENGTH_LONG).show();
            return;
        }
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
        WatchManager.getInstance().getDevice().setTargetStep(Integer.parseInt(inPutStr), new AsyncBleCallback<Void, BleError>() {
            @Override
            public void onSuccess(Void result) {
                Log.i(TAG, "setTargetStep onSuccess");
                setTextResult("set success");
                view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "setTargetStep onFailure:" + error);
                setTextResult(error.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        });
    }

    private void getTargetStep(View view) {
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
        WatchManager.getInstance().getDevice().getTargetStep(new AsyncBleCallback<Integer, BleError>() {
            @Override
            public void onSuccess(Integer result) {
                Log.i(TAG, "getTargetStep onSuccess");
                setTextResult(GSON.toJSONString(result));
                view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "getTargetStep onFailure:" + error);
                setTextResult(error.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        });
    }

    private void installSurface(View view) {
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
        DeviceSurfaceInfo deviceSurfaceInfo = new DeviceSurfaceInfo();
        deviceSurfaceInfo.setId(9568);
        deviceSurfaceInfo.setVersion(2);
        deviceSurfaceInfo.setSelected(true);
        DeviceSurfaceInfo.Resource resource = new DeviceSurfaceInfo.Resource();
        resource.setName("watchface_9568");
        resource.setType(DeviceSurfaceInfo.Resource.Type.TAR);
        resource.setBytes(FileUtil.readFromAssets(this, "watchface_9568.tar"));
        List<DeviceSurfaceInfo.Resource> resources = new ArrayList<>();
        resources.add(resource);
        deviceSurfaceInfo.setResources(resources);

        //        DeviceSurfaceInfo deviceSurfaceInfo = GSON.parseObject(inPutStr, DeviceSurfaceInfo.class);
        WatchManager.getInstance().getDevice().installSurface(deviceSurfaceInfo, new AsyncBleCallback<Void, BleError>() {

            @Override
            public void onProgress(float progress) {
                setTextResult((int) (progress * 100) + "%");
            }

            @Override
            public void onSuccess(Void result) {
                Log.i(TAG, "installSurface onSuccess");
                view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "installSurface onFailure:" + error);
                setTextResult(error.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        });
    }

    private void deleteSurface(View view) {
//        if (inPutStr.isEmpty()) {
//            Toast.makeText(this, "数据不能为空", Toast.LENGTH_LONG).show();
//            return;
//        }
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
//        WatchManager.getInstance().getDevice().deleteSurface(Integer.parseInt(inPutStr), new AsyncBleCallback<Void, BleError>() {
        WatchManager.getInstance().getDevice().deleteSurface(9568, new AsyncBleCallback<Void, BleError>() {
            @Override
            public void onSuccess(Void result) {
                Log.i(TAG, "deleteSurface onSuccess");
                setTextResult("set success");
                view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "deleteSurface onFailure:" + error);
                setTextResult(error.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        });
    }

    private void bluetooth_control(View view) {
        if (BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            BluetoothAdapter.getDefaultAdapter().disable();
            setTextResult("off");
        } else {
            BluetoothAdapter.getDefaultAdapter().enable();
            setTextResult("on");
            }
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
    }

    private void setWeatherNotifyStatus(View view) {
        if (inPutStr.isEmpty()) {
            Toast.makeText(this, "数据不能为空", Toast.LENGTH_LONG).show();
            return;
        }
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
        WatchManager.getInstance().getDevice().setWeatherNotifyStatus(Boolean.parseBoolean(inPutStr), new AsyncBleCallback<Void, BleError>() {
            @Override
            public void onSuccess(Void result) {
                Log.i(TAG, "setWeatherNotifyStatus onSuccess");
                setTextResult("set success");
                view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "setWeatherNotifyStatus onFailure:" + error);
                setTextResult(error.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        });
    }

    private void setDrinkWaterRemindSetting(View view) {
        if (inPutStr.isEmpty()) {
            Toast.makeText(this, "数据不能为空", Toast.LENGTH_LONG).show();
            return;
        }
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
        DrinkWaterRemindSetting drinkWaterRemindSetting = GSON.parseObject(inPutStr, DrinkWaterRemindSetting.class);

//        DrinkWaterRemindSetting drinkWaterRemindSetting = new DrinkWaterRemindSetting();
//        drinkWaterRemindSetting.enable = true;
//        drinkWaterRemindSetting.endTimeHour = 23;
//        drinkWaterRemindSetting.endTimeMinute = 0;
//        drinkWaterRemindSetting.forbidEnable = true;
//        drinkWaterRemindSetting.forbidEndTimeHour = 14;
//        drinkWaterRemindSetting.forbidEndTimeMinute = 0;
//        drinkWaterRemindSetting.forbidStartTimeHour = 12;
//        drinkWaterRemindSetting.forbidStartTimeMinute = 0;
//        drinkWaterRemindSetting.interval = 5;
//        drinkWaterRemindSetting.startTimeHour = 7;
//        drinkWaterRemindSetting.startTimeMinute = 0;
//        Log.i(TAG, "doNotDisturbSetting:"+GSON.toJSONString(drinkWaterRemindSetting));
        WatchManager.getInstance().getDevice().setDrinkWaterRemindSetting(drinkWaterRemindSetting, new AsyncBleCallback<Void, BleError>() {

            @Override
            public void onSuccess(Void result) {
                Log.i(TAG, "setDrinkWaterRemindSetting onSuccess");
                setTextResult("set success");
                view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "setDrinkWaterRemindSetting onFailure:" + error);
                setTextResult(error.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        });
    }

    private void getDrinkWaterRemindSetting(View view) {
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
        WatchManager.getInstance().getDevice().getDrinkWaterRemindSetting(new AsyncBleCallback<DrinkWaterRemindSetting, BleError>() {
            @Override
            public void onSuccess(DrinkWaterRemindSetting result) {
                Log.i(TAG, "getDrinkWaterRemindSetting onSuccess");
                setTextResult(GSON.toJSONString(result));
                view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "getDrinkWaterRemindSetting onFailure:" + error);
                setTextResult(error.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        });
    }

    private void getWeatherNotifyStatus(View view) {
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
        WatchManager.getInstance().getDevice().getWeatherNotifyStatus(new AsyncBleCallback<Boolean, BleError>() {
            @Override
            public void onSuccess(Boolean result) {
                Log.i(TAG, "getWeatherNotifyStatus onSuccess");
                setTextResult(GSON.toJSONString(result));
                view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "getWeatherNotifyStatus onFailure:" + error);
                setTextResult(error.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        });
    }

    private void getDeviceRunState(View view) {
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
        //当前主状态, 0:idle(空闲), 1:running(跑步中), 2:ota(固件更新中), 3:se(se操作中), 4:upload_data(同步数据中), 5:repair_res(资源修复中)
        WatchManager.getInstance().getDevice().getDeviceRunState(new AsyncBleCallback<DeviceRunState, BleError>() {
            PBProperty.DeviceSettingBrightNightVal R1000O00000o = new PBProperty.DeviceSettingBrightNightVal();

            @Override
            public void onSuccess(DeviceRunState result) {
                Log.i(TAG, "getDeviceRunState onSuccess");
                setTextResult(GSON.toJSONString(result));
                view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "getDeviceRunState onFailure:" + error);
                setTextResult(error.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        });
    }

    private void getDeviceLogFile(View view) {
//        if (inPutStr.isEmpty()) {
//            Toast.makeText(this, "数据不能为空", Toast.LENGTH_LONG).show();
//            return;
//        }
        view.setBackgroundColor(getResources().getColor(R.color.colorNormal));
        WatchManager.getInstance().getDevice().getDeviceLogFile(logDir, new AsyncBleCallback<String, BleError>() {
            @Override
            public void onSuccess(String result) {
                Log.i(TAG, "getDeviceLogFile onSuccess");
                setTextResult(GSON.toJSONString(result));
                view.setBackgroundColor(getResources().getColor(R.color.colorGreen));
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "getDeviceLogFile onFailure:" + error);
                setTextResult(error.toString());
                view.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        });
    }


    private void setDeviceConnectStatus(String status) {
        if (isActivityAvailable()) {
            tvConnectStatus.setText(WatchManager.getInstance().getDevice().getMac() + "  " + status);
        }
    }


    private boolean isActivityAvailable() {
        return !isFinishing() && !isDestroyed();
    }


//    private void copyAssets(String fileName) {
//        try {
//            File files = new File(fileDir);
//            if (!files.exists()) {
//                files.mkdirs();
//            }
//            File file = new File(files, fileName);
//            InputStream is = null;
//            try {
//                AssetManager manager = getAssets();
//                if (manager == null) {
//                    return;
//                }
//                is = manager.open(fileName);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            if (is == null) {
//                return;
//            }
//            FileOutputStream fos = new FileOutputStream(file);
//            byte[] buffer = new byte[1024];
//            int byteCount = 0;
//            while ((byteCount = is.read(buffer)) != -1) {
//                // buffer字节
//                fos.write(buffer, 0, byteCount);
//            }
//            fos.flush();// 刷新缓冲区
//            is.close();
//            fos.close();
//        } catch (Exception e) {
//            Log.e("yj","copy---exception---"+e.toString());
//            e.printStackTrace();
//        }
//
//    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        WatchManager.getInstance().removeDeviceConnectListener(deviceConnectListener);
    }

}
