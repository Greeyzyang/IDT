package com.ryeex.sdkwatch;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ryeex.sdk.R;
import com.ryeex.watch.adapter.device.WatchDevice;
import com.ryeex.ble.common.device.OnBindListener;
import com.ryeex.ble.common.model.entity.RyeexDeviceBindInfo;
import com.ryeex.ble.connector.callback.AsyncBleCallback;
import com.ryeex.ble.connector.error.BleError;
import com.ryeex.ble.connector.scan.ScannedDevice;

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


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_bind);
        ButterKnife.bind(this);
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
        WatchDevice bindingDevice = new WatchDevice();
//        bindingDevice.setDebug(true);
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

            }

            @Override
            public void onBinding() {
                tvStatus.setText("正在绑定");

            }

            @Override
            public void onServerBind(RyeexDeviceBindInfo deviceBindInfo, AsyncBleCallback<Void, BleError> callback) {
                if (callback != null) {
                    callback.sendSuccessMessage(null);
                }
            }

            @Override
            public void onSuccess() {
                tvStatus.setText("绑定成功");
                btnFinish.setEnabled(true);
            }

            @Override
            public void onFailure(BleError error) {
                tvStatus.setText("绑定失败");
            }
        });
    }


    public void finishBind(View view) {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

}
