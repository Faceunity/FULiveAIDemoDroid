package com.faceunity.fuliveaidemo.util;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

/**
 * 可以感知 Activity 生命周期的 Handler，onDestroy 时移除所有消息和回调。
 *
 * @author Richie on 2019.05.13
 */
public final class LifecycleHandler extends Handler implements LifecycleObserver {
    private Lifecycle mLifecycle;

    public LifecycleHandler(final Lifecycle lifecycle) {
        mLifecycle = lifecycle;
        addObserver();
    }

    public LifecycleHandler(final Callback callback, final Lifecycle lifecycle) {
        super(callback);
        mLifecycle = lifecycle;
        addObserver();
    }

    public LifecycleHandler(final Looper looper, final Lifecycle lifecycle) {
        super(looper);
        mLifecycle = lifecycle;
        addObserver();
    }

    public LifecycleHandler(final Looper looper, final Callback callback, final Lifecycle lifecycle) {
        super(looper, callback);
        mLifecycle = lifecycle;
        addObserver();
    }

    private void addObserver() {
        mLifecycle.addObserver(this);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private void onDestroy() {
        removeCallbacksAndMessages(null);
        mLifecycle.removeObserver(this);
    }
}
