package com.ryeex.sdkband;

import androidx.multidex.MultiDexApplication;

import com.ryeex.ble.connector.BleEngine;
import com.ryeex.ble.connector.log.BleLogCallback;
import com.ryeex.groot.lib.log.Logger;
import com.ryeex.sdkband.model.PrefsEngine;

public class App extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.init(this, true);
        PrefsEngine.init(this);
        BleEngine.init(this, true, new BleLogCallback() {
            @Override
            public void verbose(String tag, String msg) {
                Logger.v(tag, msg);
            }

            @Override
            public void debug(String tag, String msg) {
                Logger.d(tag, msg);
            }

            @Override
            public void info(String tag, String msg) {
                Logger.i(tag, msg);
            }

            @Override
            public void warn(String tag, String msg) {
                Logger.w(tag, msg);
            }

            @Override
            public void error(String tag, String msg) {
                Logger.e(tag, msg);
            }
        });

        registerActivityLifecycleCallbacks(new AppActivityLifecycleCallbacks());

    }


}
