package com.ryeex.sdkband;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.ryeex.sdk.R;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
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


}
