package com.faceunity.nama.factory;

/**
 * DESC：
 * Created on 2021/3/30
 */
public class EffectDataFactory {
    private AvatarDataFactory mAvatarDataFactory;
    private PropDataFactory mPropDataFactory;

    public EffectDataFactory(boolean isFull) {
        mAvatarDataFactory = new AvatarDataFactory(isFull);
        mPropDataFactory = new PropDataFactory(isFull);
    }

    public void bindCurrentRenderer() {
        mPropDataFactory.bindCurrentRenderer();
        mAvatarDataFactory.bindCurrentRenderer();
    }

    public AvatarDataFactory getAvatarDataFactory() {
        return mAvatarDataFactory;
    }

    public PropDataFactory getPropDataFactory() {
        return mPropDataFactory;
    }
}
