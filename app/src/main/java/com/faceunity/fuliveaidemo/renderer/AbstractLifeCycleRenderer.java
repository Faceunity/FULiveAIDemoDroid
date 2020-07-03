package com.faceunity.fuliveaidemo.renderer;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

/**
 * @author Richie on 2020.05.25
 */
public abstract class AbstractLifeCycleRenderer implements LifecycleObserver {
    private Lifecycle mLifecycle;

    protected AbstractLifeCycleRenderer(final Lifecycle lifecycle) {
        lifecycle.addObserver(this);
        mLifecycle = lifecycle;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    protected void onResume() {

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    protected void onPause() {

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private void onDestroy() {
        mLifecycle.removeObserver(this);
    }

}
