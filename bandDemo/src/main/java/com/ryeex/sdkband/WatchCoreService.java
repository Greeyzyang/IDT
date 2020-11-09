package com.ryeex.sdkband;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.ryeex.ble.common.device.DeviceConnectListener;
import com.ryeex.ble.common.model.entity.AppNotification;
import com.ryeex.ble.connector.callback.AsyncBleCallback;
import com.ryeex.ble.connector.error.BleError;
import com.ryeex.ble.connector.handler.BleHandler;
import com.ryeex.sdk.R;
import com.ryeex.sdkband.listener.OnVoiceListener;
import com.ryeex.sdkband.model.PrefsDevice;
import com.ryeex.sdkband.utils.ContactUtil;

public class WatchCoreService extends Service {

    private final String TAG = "WatchCoreService";
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;
    private boolean isIncomeCall;
    private int voidSessionId;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
        initPhoneStateListener();
        WatchManager.getInstance().addDeviceConnectListener(deviceConnectListener);
        WatchManager.getInstance().setOnVoiceListener(onVoiceListener);
        connect();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");
        startForeground(1001, buildRunNotification());
        return super.onStartCommand(intent, flags, startId);
    }


    private DeviceConnectListener deviceConnectListener = new DeviceConnectListener() {
        @Override
        public void onConnecting() {
            Log.i(TAG, "onConnecting");
        }

        @Override
        public void onLoginSuccess() {
            Log.i(TAG, "onLoginSuccess");
            //0: 正常, 1:未授权, 2:网络不可用，云端和app端语音服务不可用时需要通过此接口通知设备，如果app不告诉设备状态，则语音会话开始前会通过checkVoiceReady进行查询
            WatchManager.getInstance().getDevice().sendVoiceStatus(0, null);
        }

        @Override
        public void onDisconnected(BleError error) {
            Log.e(TAG, "onDisconnected " + error);
            connect();
        }

        @Override
        public void onFailure(BleError error) {
            Log.e(TAG, "onFailure " + error);
        }
    };


    private OnVoiceListener onVoiceListener = new OnVoiceListener() {
        @Override
        public void onCheck(AsyncBleCallback<Boolean, BleError> callback) {
            Log.i(TAG, "onVoiceListener onCheck");
            if (callback != null) {
                callback.sendSuccessMessage(true);
            }
        }

        @Override
        public void onStart(int sessionId) {
            Log.i(TAG, "onVoiceListener onStart sessionId=" + sessionId);
            voidSessionId = sessionId;
        }

        @Override
        public void onReceiveBytes(byte[] bytes) {
            Log.i(TAG, "onVoiceListener onReceiveBytes:" + bytes.length);
            //如果云端有截断，则可通过
            //WatchManager.getInstance().getDevice().sendVoiceStop(voidSessionId, null);

        }

        @Override
        public void onReceiveBytesFinish(int sessionId) {
            Log.i(TAG, "onVoiceListener onReceiveBytesFinish sessionId=" + sessionId);

            //模拟云端响应
            BleHandler.getWorkerHandler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    //caption 结果   isContinue是否为连续对话  sessionId会话id
                    WatchManager.getInstance().getDevice().sendVoiceCaption("voice test", false, voidSessionId, null);
                }
            }, 1000);
        }

        @Override
        public void onStop() {
            Log.i(TAG, "onVoiceListener onStop");
        }
    };

    private void connect() {

        if (PrefsDevice.hasDevice()) {
            WatchManager.getInstance().login(new AsyncBleCallback<Void, BleError>() {
                @Override
                public void onSuccess(Void result) {
                    Log.i(TAG, "login onSuccess");
                }

                @Override
                public void onFailure(BleError error) {
                    Log.e(TAG, "login onFailure:" + error);
                }
            });
        }
    }


    private Notification buildRunNotification() {
        NotificationCompat.Builder notificationBuilder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "demo-default";
            NotificationChannel notificationChannel = new NotificationChannel(channelId, TAG, NotificationManager.IMPORTANCE_DEFAULT);
            //不提示声音&震动
            notificationChannel.setSound(null, null);
            notificationChannel.enableLights(false);
            notificationChannel.enableVibration(false);
            //不显示角标
            notificationChannel.setShowBadge(false);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(notificationChannel);
            }
            notificationBuilder = new NotificationCompat.Builder(this, channelId);
        } else {
            notificationBuilder = new NotificationCompat.Builder(this);
        }


        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilder
                .setSmallIcon(R.drawable.idt)
                .setColor(Color.parseColor("#151515"))
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.idt))
                .setContentTitle("IDT")
                .setContentText("demo test")
//                .setCustomContentView(remoteViews)
                .setContentIntent(pendingIntent)
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis())
                .setOngoing(true);

        return notificationBuilder.build();
    }


    /**
     * 初始化来电状态监听器
     */
    private void initPhoneStateListener() {
        Log.i(TAG, "initPhoneStateListener");
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    // 待机，即无电话时，挂断时触发
                    case TelephonyManager.CALL_STATE_IDLE:
                        Log.i(TAG, "state:CALL_STATE_IDLE incomingNumber:" + incomingNumber);
                        //去电时不用通知手环
                        if (!isIncomeCall) {
                            Log.i(TAG, "IDLE isn't incoming call and return");
                            return;
                        }
                        isIncomeCall = false;
                        String contactIdle = ContactUtil.getContactByNumber(WatchCoreService.this, incomingNumber);
                        notifyDevice(TextUtils.isEmpty(incomingNumber) ? " " : incomingNumber, contactIdle, AppNotification.Telephony.Status.DISCONNECTED);
                        break;
                    // 响铃，来电时触发
                    case TelephonyManager.CALL_STATE_RINGING:
                        Log.i(TAG, "state:CALL_STATE_RINGING incomingNumber:" + incomingNumber);
                        isIncomeCall = true;
                        String contactRinging = ContactUtil.getContactByNumber(WatchCoreService.this, incomingNumber);
                        notifyDevice(TextUtils.isEmpty(incomingNumber) ? " " : incomingNumber, contactRinging, AppNotification.Telephony.Status.RINGING_UNANSWERABLE);
                        break;
                    // 摘机，接听或拨出电话时触发
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        Log.i(TAG, "state:CALL_STATE_OFFHOOK incomingNumber:" + incomingNumber);
                        //去电时不用通知手环
                        if (!isIncomeCall) {
                            Log.i(TAG, "OFFHOOK isn't incoming call and return");
                            return;
                        }
                        isIncomeCall = false;
                        String contactOff = ContactUtil.getContactByNumber(WatchCoreService.this, incomingNumber);
                        notifyDevice(TextUtils.isEmpty(incomingNumber) ? " " : incomingNumber, contactOff, AppNotification.Telephony.Status.CONNECTED);
                        break;
                    default:
                }
            }
        };

        // 设置来电监听器
        telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }


    public void notifyDevice(String mIncomingNumber, String notificationText, AppNotification.Telephony.Status status) {
        Log.i(TAG, "notifyDevice " + notificationText + "   number= " + mIncomingNumber);

        if (!WatchManager.getInstance().isLogin()) {
            return;
        }

        //来电
        AppNotification appNotification = new AppNotification();
        appNotification.setType(AppNotification.Type.TELEPHONY);
        AppNotification.Telephony telephony = new AppNotification.Telephony();
        //联系人
        telephony.setContact(notificationText);
        //来电号码
        telephony.setNumber(mIncomingNumber);
        // CONNECTED 接听
        // DISCONNECTED 挂断
        // RINGING_UNANSWERABLE 响铃
        //根据来电状态更改
        telephony.setStatus(status);
        appNotification.setTelephony(telephony);

        WatchManager.getInstance().getDevice().sendNotification(appNotification, new AsyncBleCallback<Void, BleError>() {
            @Override
            public void onSuccess(Void result) {
                Log.i(TAG, "sendNotification onSuccess");
            }

            @Override
            public void onFailure(BleError error) {
                Log.e(TAG, "sendNotification onFailure:" + error);
            }
        });
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
