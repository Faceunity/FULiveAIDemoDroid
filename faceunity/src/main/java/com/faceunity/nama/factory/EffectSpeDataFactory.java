package com.faceunity.nama.factory;


import androidx.annotation.NonNull;

import com.faceunity.core.entity.FUBundleData;
import com.faceunity.core.enumeration.FUAITypeEnum;
import com.faceunity.core.faceunity.FURenderKit;
import com.faceunity.core.model.prop.expression.ExpressionRecognition;
import com.faceunity.nama.Effect;
import com.faceunity.nama.FURenderer;
import com.faceunity.nama.source.EffectBodySource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DESC：效果特殊工厂 -- 单选 人体 全身 半身
 * Created on 2021/3/2
 */
public class EffectSpeDataFactory {

    /*渲染控制器*/
    private final FURenderKit mFURenderKit = FURenderKit.getInstance();
    /*道具列表*/
    private final List<Effect> mEffectBeans;
    /*人体关键点列表*/
    private final List<Effect> mLandMarkEffectBeans;
    /*人体骨骼列表*/
    private final List<Effect> mSkeletonBeans;
    /*当前使用的效果*/
    public Effect currentUseEffect;
    /*当前title的文字和图片 id */
    private HashMap<String, Integer> mTitleMap;

    public EffectSpeDataFactory(boolean containAvatar) {
        mEffectBeans = EffectBodySource.buildEffectBeans(containAvatar);
        mLandMarkEffectBeans = EffectBodySource.getLandMarkEffects();
        mSkeletonBeans = EffectBodySource.getSkeletonEffects();
        mTitleMap = EffectBodySource.buildTitle();
    }

    /**
     * 获取道具队列 + 假的显示在页面上的
     *
     * @return
     */
    @NonNull
    public List<Effect> getEffectBeans() {
        return mEffectBeans;
    }

    /**
     * 获取人体关键点道具队列
     *
     * @return
     */
    @NonNull
    public List<Effect> getLandMarkEffectBeans() {
        return mLandMarkEffectBeans;
    }

    /**
     * 获取人体骨骼道具队列
     *
     * @return
     */
    @NonNull
    public List<Effect> getSkeletonBeans() {
        return mSkeletonBeans;
    }

    /**
     * 替换已选effects
     *
     * @param effect
     */
    public void replaceProp(Effect effect) {
        currentUseEffect = effect;
        addProp(effect);
    }

    /**
     * 添加effects
     *
     * @param effect
     */
    private void addProp(Effect effect) {
//        mFURenderKit.getPropContainer().removeAllProp();
        if (effect == null) {
            return;
        }
        ExpressionRecognition prop = new ExpressionRecognition(new FUBundleData(effect.getFilePath()));
        mFURenderKit.getPropContainer().addProp(prop);
        setParams(effect, prop);
    }

    /**
     * 设置参数
     *
     * @param effect
     * @param prop
     */
    private void setParams(Effect effect, ExpressionRecognition prop) {
        if (Effect.TYPE_FACE == effect.getType()) {
            //需要设置参数
            Map<String, Object> params = effect.getParams();
            if (params != null && params.size() > 0) {
                Set<Map.Entry<String, Object>> entries = params.entrySet();
                for (Map.Entry<String, Object> entry : entries) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (key != null && value != null && value instanceof FUAITypeEnum) {
                        if (key.equals(FURenderer.KEY_LANDMARKS_TYPE)) {
                            prop.setLandmarksType((FUAITypeEnum) value);
                        }

                        if (key.equals(FURenderer.KEY_AI_TYPE)) {
                            prop.setAiType((FUAITypeEnum) value);
                        }
                    }
                }
            }
        }
    }

    /**
     * 绑定当前效果
     */
    public void bindCurrentRenderer() {
        if (currentUseEffect != null) {
            addProp(currentUseEffect);
        }
    }

    /**
     * 获取顶部名称和图片
     * @return
     */
    public HashMap<String, Integer> getTitleMap() {
        return mTitleMap;
    }
}
