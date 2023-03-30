package com.faceunity.nama.source;


import com.faceunity.nama.Effect;
import com.faceunity.nama.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * DESC：
 * 人脸道具类
 * Created on 2021/3/28
 */
public class EffectActionSource {
    private static final String ASSETS_DIR = "others/";
    private static final String HUMAN_ACTION_BUNDLE_PATH = ASSETS_DIR + "human_action.bundle"; // 动作识别

    /**
     * 手势特效
     * @return
     */
    public static List<Effect> buildEffectBeans() {
        List<Effect> actionEffects = new ArrayList<>();
        actionEffects.add(new Effect(HUMAN_ACTION_BUNDLE_PATH, "动作识别", Effect.TYPE_ACTION, Effect.MODULE_CODE_ACTION));
        return actionEffects;
    }

    /**
     * 创建title
     * @return
     */
    public static HashMap<String,Integer> buildTitle(){
        HashMap<String,Integer> hashMap = new HashMap<>();
        hashMap.put("title_tv", R.string.action);
        hashMap.put("title_img", R.drawable.demo_sidebar_icon_action);
        return hashMap;
    }
}
