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


    @OnClick({R.id.bnt_pb, R.id.bnt_json,})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bnt_pb:
                startActivity(new Intent(this, PbDeviceActivity.class));
                break;
            case R.id.bnt_json:
                startActivity(new Intent(this, JsonDeviceActivity.class));
                break;
            default:
        }
    }


}
