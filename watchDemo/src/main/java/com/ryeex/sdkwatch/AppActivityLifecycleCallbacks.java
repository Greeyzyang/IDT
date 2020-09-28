package com.ryeex.sdkwatch;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;


/**
 * 监听activity生命周期
 *
 * @author lijiewen
 * @date on 2019-04-23
 */
class AppActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {

    private static final Object LOCK = new Object();

    private static int mStartedActivityNum = 0;

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(Activity activity) {
        synchronized (LOCK) {
            if (mStartedActivityNum == 0) {
                DeviceManager.getInstance().getDevice().setAppForeground(true, null);
            }

            mStartedActivityNum++;
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
        synchronized (LOCK) {
            mStartedActivityNum--;
            if (mStartedActivityNum <= 0) {
                DeviceManager.getInstance().getDevice().setAppForeground(false, null);
            }
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }
}
