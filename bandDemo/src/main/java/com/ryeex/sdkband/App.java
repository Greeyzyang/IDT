package com.ryeex.sdkband;

import android.util.Log;

import androidx.multidex.MultiDexApplication;

import com.ryeex.ble.connector.BleEngine;
import com.ryeex.ble.connector.log.BleLogCallback;
import com.ryeex.sdkband.model.PrefsEngine;

public class App extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        PrefsEngine.init(this);
        BleEngine.init(this, new BleLogCallback() {
            @Override
            public void verbose(String tag, String msg) {
                Log.v(tag, msg);
            }

            @Override
            public void debug(String tag, String msg) {
                Log.d(tag, msg);
            }

            @Override
            public void info(String tag, String msg) {
                Log.i(tag, msg);
            }

            @Override
            public void warn(String tag, String msg) {
                Log.w(tag, msg);
            }

            @Override
            public void error(String tag, String msg) {
                Log.e(tag, msg);
            }
        });

        registerActivityLifecycleCallbacks(new AppActivityLifecycleCallbacks());

    }


}
