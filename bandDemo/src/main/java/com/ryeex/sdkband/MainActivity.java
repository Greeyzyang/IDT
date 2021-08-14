package com.ryeex.sdkband;

import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.ryeex.ble.connector.BleEngine;
import com.ryeex.ble.connector.handler.BleHandler;
import com.ryeex.sdk.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private String fileDir = BleEngine.getAppContext().getExternalFilesDir(null).getPath() + File.separator + "Update_File";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        BleHandler.getWorkerHandler().post(new Runnable() {
            @Override
            public void run() {
                copyAssets("1.3.0.501");
                copyAssets("501");
            }
        });
    }

    @OnClick({R.id.bnt_brandypb, R.id.bnt_brandyjson, R.id.bnt_saturnjson, R.id.bnt_saturnpb,
            R.id.bnt_wyzepb, R.id.bnt_wyzejson,
    })
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bnt_brandypb:
                startActivity(new Intent(this, BrandyPbDeviceActivity.class));
                break;
            case R.id.bnt_brandyjson:
                startActivity(new Intent(this, BrandyJsonDeviceActivity.class));
                break;
            case R.id.bnt_saturnpb:
                startActivity(new Intent(this, SaturnPbDevicesActivity.class));
                break;
            case R.id.bnt_saturnjson:
                startActivity(new Intent(this, SaturnJsonDevicesActivity.class));
                break;
            case R.id.bnt_wyzepb:
                startActivity(new Intent(this, WyzePbDevicesActivity.class));
                break;
            case R.id.bnt_wyzejson:
                startActivity(new Intent(this, WyzeJsonDevicesActivity.class));
                break;
            default:
        }
    }
    private void copyAssets(String fileName) {
        try {
            File files = new File(fileDir);
            if (!files.exists()) {
                files.mkdirs();
            }
            File file = new File(files, fileName);
            InputStream is = null;
            try {
                AssetManager manager = getAssets();
                if (manager == null) {
                    return;
                }
                is = manager.open(fileName);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (is == null) {
                return;
            }
            FileOutputStream fos = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int byteCount = 0;
            while ((byteCount = is.read(buffer)) != -1) {
                // buffer字节
                fos.write(buffer, 0, byteCount);
            }
            fos.flush();// 刷新缓冲区
            is.close();
            fos.close();
        } catch (Exception e) {
            Log.e("yj", "copy---exception---" + e.toString());
            e.printStackTrace();
        }
    }
}



