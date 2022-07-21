package com.faceunity.nama.view.listener;

import com.faceunity.nama.Effect;

import java.util.HashSet;

public interface ChoiceEffectOrAvatarListener {

    //效果的选择

    /**
     * 多选效果 为空表示未选择
     *
     * @param effects
     */
    void onChoiceEffects(HashSet<Effect> effects);

    /**
     * 单选效果 选中 或者 取消
     *
     * @param isChoice 是否选中该效果
     * @param effect
     */
    void onEffect(boolean isChoice, Effect effect);

    /**
     * 特殊选择
     * 每一次选中的时候，根据当前选择情况进行记录
     *
     * @param effect
     */
    void onSpeEffect(Effect effect);

    /**
     * 点击确认按钮
     */
    void onSure();
}
