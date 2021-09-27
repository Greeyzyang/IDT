package com.ryeex.sdkband;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ryeex.band.adapter.device.BandDevice;
import com.ryeex.ble.common.device.OnBindListener;
import com.ryeex.ble.common.model.entity.RyeexDeviceBindInfo;
import com.ryeex.ble.connector.callback.AsyncBleCallback;
import com.ryeex.ble.connector.error.BleError;
import com.ryeex.ble.connector.error.ServerError;
import com.ryeex.ble.connector.scan.ScannedDevice;
import com.ryeex.sdk.R;
import com.ryeex.watch.adapter.device.WatchDevice;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * 设备绑定
 *
 * @author lijiewen
 * @date on 2019-05-15
 */
public class DeviceBindActivity extends AppCompatActivity {
    @BindView(R.id.tv_status)
    TextView tvStatus;
    @BindView(R.id.btn_finish)
    Button btnFinish;

//    rivate String from;p

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_bind);
        ButterKnife.bind(this);
//        if (getIntent().hasExtra("from")) {
//            from = getIntent().getStringExtra("from");
//        }
        if (getIntent().hasExtra("scannedDevice")) {
            ScannedDevice scannedDevice = getIntent().getParcelableExtra("scannedDevice");
            if (scannedDevice == null) {
                finish();
                return;
            }
            //一进来自动开始绑定
            startBind(scannedDevice);
        }
    }


    /**
     * 开始绑定
     *
     * @param scannedDevice
     */
    private void startBind(ScannedDevice scannedDevice) {
        BandDevice bindingDevice = new BandDevice();
        bindingDevice.setPid(scannedDevice.getRyeexProductId());
        bindingDevice.setMac(scannedDevice.getMac());

        DeviceManager.getInstance().bind(bindingDevice, new OnBindListener() {
            @Override
            public void onConnecting() {
                tvStatus.setText("正在连接");

            }

            @Override
            public void onConfirming() {
                tvStatus.setText("请在设备上点击确认");
                btnFinish.setEnabled(true);
            }

            @Override
            public void onBinding() {
                tvStatus.setText("正在绑定");

            }

            @Override
            public void onServerBind(RyeexDeviceBindInfo deviceBindInfo, AsyncBleCallback<Void, ServerError> callback) {
                if (callback != null) {
                    callback.sendSuccessMessage(null);
                }
            }

            @Override
            public void onSuccess() {
                tvStatus.setText("绑定成功");
//                btnFinish.setEnabled(true);
            }

            @Override
            public void onFailure(BleError error) {
                tvStatus.setText("绑定失败");
            }
        });
    }

//    private void startWatch(ScannedDevice scannedDevice) {
//        WatchDevice bindingDevice = new WatchDevice();
//        bindingDevice.setPid(scannedDevice.getRyeexProductId());
//        bindingDevice.setMac(scannedDevice.getMac());
//
//        WatchManager.getInstance().bind(bindingDevice, new OnBindListener() {
//            @Override
//            public void onConnecting() {
//                tvStatus.setText("正在连接");
//
//            }
//
//            @Override
//            public void onConfirming() {
//                tvStatus.setText("请在设备上点击确认");
//
//            }
//
//            @Override
//            public void onBinding() {
//                tvStatus.setText("正在绑定");
//
//            }
//
//            @Override
//            public void onServerBind(RyeexDeviceBindInfo deviceBindInfo, AsyncBleCallback<Void, BleError> callback) {
//                if (callback != null) {
//                    callback.sendSuccessMessage(null);
//                }
//            }
//
//            @Override
//            public void onSuccess() {
//                tvStatus.setText("绑定成功");
//                btnFinish.setEnabled(true);
//            }
//
//            @Override
//            public void onFailure(BleError error) {
//                tvStatus.setText("绑定失败");
//            }
//        });
//    }


    public void finishBind(View view) {
        Intent intent;
//        if (TextUtils.equals("json", from)) {
//            intent = new Intent(this, PbDeviceActivity.class);
//        } else {
//            intent = new Intent(this, JsonDeviceActivity.class);
//        }
        intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

}
