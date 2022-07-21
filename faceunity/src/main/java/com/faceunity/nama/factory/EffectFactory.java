package com.faceunity.nama.factory;

import com.faceunity.core.faceunity.FURenderKit;
import com.faceunity.nama.Effect;
import com.faceunity.nama.source.EffectActionSource;
import com.faceunity.nama.source.EffectGestureSource;
import com.faceunity.nama.source.EffectSegmentSource;
import com.faceunity.nama.view.listener.TypeEnum;

import java.util.HashSet;

/**
 * DESC：总工厂 用于管理 Multi Single spe工厂
 * Created on 2021/3/30
 */
public class EffectFactory {
    /**
     * 保存 实现的效果
     */
    /*生效的效果*/
    private HashSet<Effect> mTakeEffects = new HashSet<>();

    /*当前模式*/
    private TypeEnum mTypeEnum = TypeEnum.EFFECT;

    /*渲染控制器*/
    private final FURenderKit mFURenderKit = FURenderKit.getInstance();
    /*手势工厂*/
    private EffectSingleDataFactory mEffectGestureSingleDataFactory;
    /*分割工厂*/
    private EffectSingleDataFactory mEffectSegmentationSingleDataFactory;
    /*动作工厂*/
    private EffectSingleDataFactory mEffectActionSingleDataFactory;
    /*人脸工厂*/
    private EffectMultiDataFactory mEffectFaceMultiDataFactory;
    /*身体工厂*/
    private EffectSepMultipleFactory mEffectBodySpeDataFactory;

    public EffectFactory(boolean containAvatar) {
        mEffectGestureSingleDataFactory = new EffectSingleDataFactory(EffectGestureSource.buildEffectBeans(), EffectGestureSource.buildTitle());
        mEffectSegmentationSingleDataFactory = new EffectSingleDataFactory(EffectSegmentSource.buildEffectBeans(), EffectSegmentSource.buildTitle());
        mEffectActionSingleDataFactory = new EffectSingleDataFactory(EffectActionSource.buildEffectBeans(), EffectActionSource.buildTitle());
        mEffectFaceMultiDataFactory = new EffectMultiDataFactory();
        mEffectBodySpeDataFactory = new EffectSepMultipleFactory(containAvatar);
    }

    public void bindCurrentRenderer() {
        if (mTypeEnum == TypeEnum.EFFECT) {
            mEffectGestureSingleDataFactory.bindCurrentRenderer();
            mEffectSegmentationSingleDataFactory.bindCurrentRenderer();
            mEffectActionSingleDataFactory.bindCurrentRenderer();
            mEffectFaceMultiDataFactory.bindCurrentRenderer();
        }
        mEffectBodySpeDataFactory.bindCurrentRenderer(mTypeEnum);
    }

    public EffectMultiDataFactory getEffectFaceMultiDataFactory() {
        return mEffectFaceMultiDataFactory;
    }

    public EffectSingleDataFactory getEffectGestureSingleDataFactory() {
        return mEffectGestureSingleDataFactory;
    }

    public EffectSingleDataFactory getEffectSegmentationSingleDataFactory() {
        return mEffectSegmentationSingleDataFactory;
    }

    public EffectSingleDataFactory getEffectActionSingleDataFactory() {
        return mEffectActionSingleDataFactory;
    }

    public EffectSepMultipleFactory getEffectSpeDataFactory() {
        return mEffectBodySpeDataFactory;
    }

    /*当前使用的效果*/
    public HashSet<Effect> currentUseEffects = new HashSet<>();

    public HashSet<Effect> getTakeEffects() {
        return mTakeEffects;
    }

    public void setTypeEnum(TypeEnum typeEnum) {
        this.mTypeEnum = typeEnum;
    }

    /**
     * 替换已选effects
     *
     * @param effects
     */
    public void replaceProps(HashSet<Effect> effects) {
        currentUseEffects.clear();
        if (effects != null)
            currentUseEffects.addAll(effects);
        //将不同的effect效果交给不同的工厂处理

        //1.手势
        Effect gestureEffect = null;
        //2.分割
        Effect segmentationEffect = null;
        //3.动作
        Effect actionEffect = null;
        //4.人脸
        HashSet<Effect> faceEffects = new HashSet<>();
        //5.人体
        Effect bodyEffect = null;

        for (Effect effect : currentUseEffects) {
            if (effect == null) {
                break;
            }
            if (effect.getType() == Effect.TYPE_GESTURE) {
                //手势
                gestureEffect = effect;
            } else if (effect.getType() == Effect.TYPE_SEGMENTATION) {
                //分割
                segmentationEffect = effect;
            } else if (effect.getType() == Effect.TYPE_ACTION) {
                //动作
                actionEffect = effect;
            } else if (effect.getType() == Effect.TYPE_FACE) {
                //人脸
                faceEffects.add(effect);
            } else if (effect.getType() == Effect.TYPE_HUMAN) {
                //人体
                bodyEffect = effect;
            }
        }

        mFURenderKit.getPropContainer().removeAllProp();
        mEffectGestureSingleDataFactory.replaceProp(gestureEffect);
        mEffectSegmentationSingleDataFactory.replaceProp(segmentationEffect);
        mEffectActionSingleDataFactory.replaceProp(actionEffect);
        mEffectFaceMultiDataFactory.replaceProps(faceEffects);
        mEffectBodySpeDataFactory.replaceProp(bodyEffect);
    }

    public void removeAllProps() {
        mFURenderKit.getPropContainer().removeAllProp();
    }
}
