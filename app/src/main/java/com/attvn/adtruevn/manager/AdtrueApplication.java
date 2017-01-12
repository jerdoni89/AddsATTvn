package com.attvn.adtruevn.manager;

import android.app.Application;
import android.provider.Settings;

import com.attvn.adtruevn.util.Logging;

import io.realm.Realm;

/**
 * Created by app on 12/9/16.
 */

public class AdtrueApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Realm.init(this);
//        FFmpegController.initWith(this);
        String deviceId = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);
        Logging.connectToLogServer(deviceId);
    }
}
