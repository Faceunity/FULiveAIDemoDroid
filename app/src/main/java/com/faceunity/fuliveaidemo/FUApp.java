package com.faceunity.fuliveaidemo;

import android.app.Application;
import android.os.AsyncTask;
import android.os.Build;

import com.faceunity.fuliveaidemo.util.LogUtils;
import com.faceunity.nama.FURenderer;

/**
 * @author Richie on 2020.05.21
 */
public class FUApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtils.config(this);
        LogUtils.i("************* device info *************\n"
                + LogUtils.retrieveDeviceInfo(this));

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            FURenderer.initFURenderer(FUApp.this);
        } else {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    FURenderer.initFURenderer(FUApp.this);
                }
            });
        }
    }

}
