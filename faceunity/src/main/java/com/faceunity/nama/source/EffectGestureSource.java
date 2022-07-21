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
public class EffectGestureSource {
    private static final String ASSETS_DIR = "others/";
    private static final String HUMAN_GESTURE_BUNDLE_PATH = ASSETS_DIR + "human_gesture.bundle"; // 手势识别

    /**
     * 手势特效
     * @return
     */
    public static List<Effect> buildEffectBeans() {
        List<Effect> gestureEffects = new ArrayList<>();
        gestureEffects.add(new Effect(HUMAN_GESTURE_BUNDLE_PATH, "手势识别", Effect.TYPE_GESTURE, Effect.MODULE_CODE_HAND_GESTURE));
        return gestureEffects;
    }

    /**
     * 创建title
     * @return
     */
    public static HashMap<String,Integer> buildTitle(){
        HashMap<String,Integer> hashMap = new HashMap<>();
        hashMap.put("title_tv", R.string.gesture);
        hashMap.put("title_img", R.drawable.demo_sidebar_icon_gesture);
        return hashMap;
    }
}
