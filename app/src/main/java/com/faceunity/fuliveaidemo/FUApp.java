package com.faceunity.fuliveaidemo;

import android.app.Application;

import com.faceunity.nama.FURenderer;

/**
 * @author Richie on 2020.05.21
 */
public class FUApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FURenderer.setup(this);
    }

}
