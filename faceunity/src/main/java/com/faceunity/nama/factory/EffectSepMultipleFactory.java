package com.faceunity.nama.factory;

import com.faceunity.nama.Effect;
import com.faceunity.nama.view.listener.TypeEnum;

import java.util.List;

/**
 * DESC：
 * Created on 2021/3/30
 * 控制 效果 + avatar
 */
public class EffectSepMultipleFactory {
    private AvatarDataFactory mAvatarDataFactory;
    private EffectSpeDataFactory mEffectSpeDataFactory;

    public EffectSepMultipleFactory(boolean isFull) {
        mAvatarDataFactory = new AvatarDataFactory(isFull);
        mEffectSpeDataFactory = new EffectSpeDataFactory(isFull);
    }

    public void bindCurrentRenderer(TypeEnum typeEnum) {
        if (typeEnum == TypeEnum.AVATAR) {
            mAvatarDataFactory.bindCurrentRenderer();
        } else {
            mEffectSpeDataFactory.bindCurrentRenderer();
        }
    }

    public AvatarDataFactory getAvatarDataFactory() {
        return mAvatarDataFactory;
    }

    public EffectSpeDataFactory getEffectSpeDataFactory() {
        return mEffectSpeDataFactory;
    }

    public List<Effect> getEffectBeans() {
        return mEffectSpeDataFactory.getEffectBeans();
    }

    public void replaceProp(Effect effect) {
        mEffectSpeDataFactory.replaceProp(effect);
    }
}
