package com.faceunity.nama.factory;


import androidx.annotation.NonNull;

import com.faceunity.core.entity.FUBundleData;
import com.faceunity.core.enumeration.FUAITypeEnum;
import com.faceunity.core.faceunity.FURenderKit;
import com.faceunity.core.model.prop.Prop;
import com.faceunity.core.model.prop.expression.ExpressionRecognition;
import com.faceunity.nama.Effect;
import com.faceunity.nama.FURenderer;
import com.faceunity.nama.source.EffectSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DESC：道具业务工厂
 * Created on 2021/3/2
 */
public class PropDataFactory {

    /*渲染控制器*/
    private final FURenderKit mFURenderKit = FURenderKit.getInstance();
    /*道具列表*/
    private final Map<Integer, List<Effect>> effectBeans;
    /*当前使用的效果*/
    public HashMap<Effect, Prop> currentUseEffects;

    public PropDataFactory(boolean containHumanAvatar) {
        effectBeans = EffectSource.buildPropBeans(containHumanAvatar);
        currentUseEffects = new HashMap<>();
    }

    /**
     * 获取道具队列
     *
     * @return
     */
    @NonNull
    public Map<Integer, List<Effect>> getEffectBeans() {
        return effectBeans;
    }

    /**
     * 加入道具
     *
     * @param bean
     */
    public void addProp(Effect bean) {
        ExpressionRecognition prop = new ExpressionRecognition(new FUBundleData(bean.getFilePath()));
        currentUseEffects.put(bean, prop);
        mFURenderKit.getPropContainer().addProp(prop);
        setParams(bean, prop);
    }

    /**
     * 移除道具
     *
     * @param bean
     */
    public void removeProp(Effect bean) {
        Prop prop = currentUseEffects.remove(bean);
        if (prop != null)
            mFURenderKit.getPropContainer().removeProp(prop);
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

    public void bindCurrentRenderer() {
        if (currentUseEffects != null && !currentUseEffects.isEmpty()) {
            Set<Map.Entry<Effect, Prop>> entries = currentUseEffects.entrySet();
            for (Map.Entry<Effect,Prop> entry : entries) {
                addProp(entry.getKey());
            }
        }
    }
}
