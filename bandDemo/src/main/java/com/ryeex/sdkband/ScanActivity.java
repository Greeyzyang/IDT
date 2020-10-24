package com.ryeex.sdkband;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemClickListener;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.ryeex.ble.connector.scan.BleScanner;
import com.ryeex.ble.connector.scan.ScannedDevice;
import com.ryeex.sdk.R;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ScanActivity extends AppCompatActivity {

    private final String TAG = "ScanActivity";
    @BindView(R.id.ryv_main)
    RecyclerView ryvMain;

    private String from;
    private DeviceScanAdapter deviceScanAdapter;


    private List<ScannedDevice> scannedDeviceList = new ArrayList<>();
    private List<ScannedDevice> scannedShowDeviceList = new ArrayList<>();


    private final int MSG_REFRESH_UI = 1;

    Handler mUiHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case MSG_REFRESH_UI:
                    scannedShowDeviceList.clear();
                    scannedShowDeviceList.addAll(scannedDeviceList);
                    if (!scannedShowDeviceList.isEmpty()) {
                        Collections.sort(scannedShowDeviceList, new Comparator<ScannedDevice>() {
                            @Override
                            public int compare(ScannedDevice o1, ScannedDevice o2) {
                                return o2.getRssi() - o1.getRssi();
                            }
                        });
                        deviceScanAdapter.notifyDataSetChanged();
                    }
                    sendEmptyMessageDelayed(MSG_REFRESH_UI, 3000);
                    break;
                default:
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        ButterKnife.bind(this);

        if (getIntent().hasExtra("from")) {
            from = getIntent().getStringExtra("from");
        }
        ryvMain.setLayoutManager(new LinearLayoutManager(this));
        deviceScanAdapter = new DeviceScanAdapter(scannedShowDeviceList);
        ryvMain.setAdapter(deviceScanAdapter);
        ryvMain.addItemDecoration(new HorizontalDividerItemDecoration.Builder(this)
                .color(getResources().getColor(R.color.colorGray))
                .sizeResId(R.dimen.divider)
                .build());
        deviceScanAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                if (position >= 0 && position < scannedShowDeviceList.size()) {
                    bindDevice(scannedShowDeviceList.get(position));
                }
            }
        });

        BleScanner.getInstance().addDeviceScanCallback(new BleScanner.OnDeviceScanCallback() {
            @Override
            public void onFind(ScannedDevice scannedDevice) {
                Iterator<ScannedDevice> iterator = scannedDeviceList.iterator();
                while (iterator.hasNext()) {
                    ScannedDevice scannedDeviceTemp = iterator.next();
                    if (scannedDeviceTemp.getMac().equals(scannedDevice.getMac())) {
                        iterator.remove();
                        break;
                    }
                }

                if (!scannedDeviceList.contains(scannedDevice)) {
                    scannedDeviceList.add(scannedDevice);
                }

                if (scannedShowDeviceList.isEmpty()) {
                    mUiHandler.sendEmptyMessage(MSG_REFRESH_UI);
                }
            }
        });

        startScan();
    }


    private void startScan() {

        AndPermission.with(this)
                .runtime()
                .permission(Permission.ACCESS_COARSE_LOCATION, Permission.ACCESS_FINE_LOCATION)
                .onGranted(permissions -> {
                    BleScanner.getInstance().start();
                })
                .onDenied(permissions -> {

                })
                .start();
    }


    private void bindDevice(ScannedDevice scannedDevice) {
        BleScanner.getInstance().stopScan();
        Intent intent = new Intent(this, DeviceBindActivity.class);
        intent.putExtra("scannedDevice", scannedDevice);
        intent.putExtra("from", from);
        startActivity(intent);
    }


    public class DeviceScanAdapter extends BaseQuickAdapter<ScannedDevice, BaseViewHolder> {


        DeviceScanAdapter(List<ScannedDevice> scannedDeviceList) {
            super(R.layout.device_item_scan, scannedDeviceList);
        }

        @Override
        protected void convert(final BaseViewHolder helper, ScannedDevice scannedDevice) {
            String mac = scannedDevice.getMac();
            /*if (mac.length() > 8) {
                int macLength = mac.length();
                mac = mac.substring(macLength - 5, macLength - 3) + mac.substring(macLength - 2, macLength);
            }*/
            helper.setText(R.id.tv_name, scannedDevice.getName())
                    .setText(R.id.tv_mac, mac);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BleScanner.getInstance().stopScan();
    }
}
