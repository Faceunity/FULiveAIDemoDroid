package com.faceunity.fuliveaidemo;

import android.app.Application;

import com.faceunity.FUConfig;
import com.faceunity.nama.FURenderer;
import com.faceunity.nama.utils.FuDeviceUtils;

/**
 * @author Richie on 2020.05.21
 */
public class FUApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FUConfig.DEVICE_LEVEL = FuDeviceUtils.judgeDeviceLevelGPU();
        FURenderer.getInstance().setup(this);
    }
}
