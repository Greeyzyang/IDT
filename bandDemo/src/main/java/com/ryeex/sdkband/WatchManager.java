package com.ryeex.sdkband;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import com.ryeex.band.adapter.device.BandDevice;
import com.ryeex.band.adapter.device.IBandDeviceRequestListener;
import com.ryeex.ble.common.device.DeviceConnectListener;
import com.ryeex.ble.common.device.DeviceRequestListener;
import com.ryeex.ble.common.device.OnBindListener;
import com.ryeex.ble.common.device.OnSleepAssistDataUploadListener;
import com.ryeex.ble.common.device.OnUnbindListener;
import com.ryeex.ble.common.model.entity.DeviceHeartbeatData;
import com.ryeex.ble.common.model.entity.FindPhoneAlert;
import com.ryeex.ble.common.model.entity.RyeexDeviceBindInfo;
import com.ryeex.ble.common.model.entity.SleepAssistData;
import com.ryeex.ble.common.model.entity.UserConfig;
import com.ryeex.ble.common.model.entity.WeatherInfo;
import com.ryeex.ble.connector.BleEngine;
import com.ryeex.ble.connector.callback.AsyncBleCallback;
import com.ryeex.ble.connector.error.BleError;
import com.ryeex.ble.connector.error.BleErrorCode;
import com.ryeex.ble.connector.error.ServerError;
import com.ryeex.ble.connector.log.BleLogger;
import com.ryeex.sdkband.listener.OnVoiceListener;
import com.ryeex.sdkband.model.PrefsDevice;
import com.ryeex.sdkband.utils.GSON;
import com.ryeex.watch.adapter.device.IWatchDeviceRequestListener;
import com.ryeex.watch.adapter.device.WatchDevice;

import java.util.ArrayList;
import java.util.List;

/**
 * 设备管理
 *
 * @author lijiewen
 * @date on 2020/7/8
 */
public class WatchManager {

    private final String TAG = "WatchManager";

    private static final Object MONITOR = new Object();
    private static WatchManager watchManager;

    private WatchDevice watchDevice;
    private List<DeviceConnectListener> deviceConnectListeners;
    private OnVoiceListener onVoiceListener;


    public static WatchManager getInstance() {
        if (watchManager == null) {
            synchronized (MONITOR) {
                if (watchManager == null) {
                    watchManager = new WatchManager();
                }
            }
        }
        return watchManager;
    }


    private WatchManager() {
        watchDevice = new WatchDevice();
        watchDevice.setDebug(true);
        watchDevice.setMac(PrefsDevice.getDeviceMac());
        watchDevice.setToken(PrefsDevice.getDeviceToken());
        deviceConnectListeners = new ArrayList<>();
        setDeviceListener(watchDevice);

        IntentFilter bleStateChangeFilter = new IntentFilter();
        bleStateChangeFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        BleEngine.getAppContext().registerReceiver(mBleStateChangeReceiver, bleStateChangeFilter);
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
                    login(null);
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    Log.d(TAG, " BroadcastReceiver STATE_TURNING_OFF");
                case BluetoothAdapter.STATE_OFF:
                    Log.d(TAG, " BroadcastReceiver STATE_OFF");

                    break;
                default:
            }
        }
    };


    private void setDeviceListener(WatchDevice device) {
        device.addDeviceConnectListener(new DeviceConnectListener() {
            @Override
            public void onConnecting() {
                for (DeviceConnectListener listener : deviceConnectListeners) {
                    if (listener != null) {
                        listener.onConnecting();
                    }
                }
            }

            @Override
            public void onLoginSuccess() {
                for (DeviceConnectListener listener : deviceConnectListeners) {
                    if (listener != null) {
                        listener.onLoginSuccess();
                    }
                }
            }

            @Override
            public void onDisconnected(BleError error) {
                for (DeviceConnectListener listener : deviceConnectListeners) {
                    if (listener != null) {
                        listener.onDisconnected(error);
                    }
                }
            }

            @Override
            public void onFailure(BleError error) {
                for (DeviceConnectListener listener : deviceConnectListeners) {
                    if (listener != null) {
                        listener.onFailure(error);
                    }
                }
            }
        });
        device.setDeviceRequestListener(new IWatchDeviceRequestListener() {

            @Override
            public void onHeartbeat(DeviceHeartbeatData heartbeatData) {
                BleLogger.d(TAG, "onHeartbeat:" + GSON.toJSONString(heartbeatData));

            }

            @Override
            public void onUserConfig(UserConfig userConfig) {
                BleLogger.d(TAG, "onUserConfig:" + GSON.toJSONString(userConfig));
            }

            @Override
            public void onUnbind() {
                BleLogger.d(TAG, "onUnbind");

            }


            @Override
            public void onFinePhone(FindPhoneAlert findPhoneAlert) {
                BleLogger.d(TAG, "onFinePhone");

            }

            @Override
            public void onWeatherRefresh(List<String> cityIds, AsyncBleCallback<WeatherInfo, BleError> responseCallback) {
                BleLogger.d(TAG, "onWeatherRefresh");
                if (responseCallback != null) {
                    responseCallback.sendSuccessMessage(null);
                }
            }

            @Override
            public void onRunStart(long sessionId, AsyncBleCallback<Void, BleError> responseCallback) {
                BleLogger.d(TAG, "onRunStart");
            }

            @Override
            public void onRunUpdate(long sessionId, AsyncBleCallback<Bundle, BleError> responseCallback) {
                BleLogger.d(TAG, "onRunUpdate");
            }

            @Override
            public void onRunPause(long sessionId, AsyncBleCallback<Void, BleError> responseCallback) {
                BleLogger.d(TAG, "onRunPause");
            }

            @Override
            public void onRunResume(long sessionId, AsyncBleCallback<Void, BleError> responseCallback) {
                BleLogger.d(TAG, "onRunResume");
            }

            @Override
            public void onRunStop(long sessionId, AsyncBleCallback<Void, BleError> responseCallback) {
                BleLogger.d(TAG, "onRunStop");
            }

            @Override
            public void checkVoiceReady(AsyncBleCallback<Boolean, BleError> responseCallback) {
                BleLogger.d(TAG, "checkVoiceReady");
                if (onVoiceListener != null) {
                    onVoiceListener.onCheck(responseCallback);
                }
            }

            @Override
            public void onVoiceSessionStart(int sessionId) {
                BleLogger.d(TAG, "onVoiceSessionStart sessionId=" + sessionId);
                if (onVoiceListener != null) {
                    onVoiceListener.onStart(sessionId);
                }
            }

            @Override
            public void onReceiveVoiceBytes(byte[] bytes) {
                BleLogger.d(TAG, "receiveVoiceBytes bytes=" + bytes.length);
                if (onVoiceListener != null) {
                    onVoiceListener.onReceiveBytes(bytes);
                }
            }

            @Override
            public void onReceiveVoiceBytesFinish(int sessionId) {
                if (onVoiceListener != null) {
                    onVoiceListener.onReceiveBytesFinish(sessionId);
                }
            }

            @Override
            public void onVoiceSessionStop() {
                BleLogger.d(TAG, "onVoiceSessionStop");
                if (onVoiceListener != null) {
                    onVoiceListener.onStop();
                }
            }
        });

        device.setSleepAssistDataUploadListener(new OnSleepAssistDataUploadListener() {
            @Override
            public void onUpload(SleepAssistData sleepAssistData, AsyncBleCallback<Boolean, BleError> callback) {
                //TODO 实现云端上传逻辑
                if (callback != null) {
                    callback.sendSuccessMessage(true);
                }
            }

            @Override
            public void onWakeupChange(long l) {

            }
        });
    }

    public WatchDevice getDevice() {
        return watchDevice;
    }

    public void refreshDevice() {

    }


    public void setOnVoiceListener(OnVoiceListener onVoiceListener) {
        this.onVoiceListener = onVoiceListener;
    }


    public void addDeviceConnectListener(DeviceConnectListener listener) {
        if (listener != null && !deviceConnectListeners.contains(listener)) {
            deviceConnectListeners.add(listener);
        }
    }

    public void removeDeviceConnectListener(DeviceConnectListener listener) {
        if (listener != null) {
            deviceConnectListeners.remove(listener);
        }
    }


    public void bind(WatchDevice device, OnBindListener onBindListener) {
        watchDevice = device;
        setDeviceListener(device);
        device.bind(new OnBindListener() {
            @Override
            public void onConnecting() {
                if (onBindListener != null) {
                    onBindListener.onConnecting();
                }
            }

            @Override
            public void onConfirming() {
                if (onBindListener != null) {
                    onBindListener.onConfirming();
                }
            }

            @Override
            public void onBinding() {
                if (onBindListener != null) {
                    onBindListener.onBinding();
                }
            }

            @Override
            public void onServerBind(RyeexDeviceBindInfo deviceBindInfo, AsyncBleCallback<Void, ServerError> callback) {
                //TODO 这里需要替换为实际服务端绑定逻辑，这里demo就直接返回成功
                if (callback != null) {
                    callback.sendSuccessMessage(null);
                }
            }

            @Override
            public void onSuccess() {
//                watchDevice = device;

                PrefsDevice.saveDeviceMac(device.getMac());
                PrefsDevice.saveDeviceToken(device.getToken());

                if (onBindListener != null) {
                    onBindListener.onSuccess();
                }
            }

            @Override
            public void onFailure(BleError error) {
                if (onBindListener != null) {
                    onBindListener.onFailure(error);
                }
            }
        });
    }


    public void unbind(AsyncBleCallback<Void, BleError> callback) {
        WatchDevice device = getDevice();
        if (device != null) {
            device.unbind(new OnUnbindListener() {
                @Override
                public void onServerUnbind(AsyncBleCallback<Void, BleError> callback) {
                    if (callback != null) {
                        callback.sendSuccessMessage(null);
                    }
                }

                @Override
                public void onSuccess() {
                    PrefsDevice.removeDevice();
                    if (callback != null) {
                        callback.sendSuccessMessage(null);
                    }
                }

                @Override
                public void onFailure(BleError error) {
                    if (callback != null) {
                        callback.sendFailureMessage(error);
                    }
                }
            });
        }
    }

    public void login(AsyncBleCallback<Void, BleError> callback) {
        if (isLogin()) {
            return;
        }
        if (watchDevice != null) {
            watchDevice.login(callback);
        }
    }


    public boolean isLogin() {
        return watchDevice != null && watchDevice.isLogin();
    }


}
