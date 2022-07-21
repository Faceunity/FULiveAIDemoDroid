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
public class EffectBodySource {
    private static final String ASSETS_DIR = "others/";
    private static final String FULL_BODY_LANDMARKS_BUNDLE_PATH = ASSETS_DIR + "bodyLandmarks_dance.bundle"; // 人体关键点 全身
    private static final String HALF_BODY_LANDMARKS_BUNDLE_PATH = ASSETS_DIR + "bodyLandmarks_selife.bundle"; // 人体关键点 半身

    /**
     * 人脸特效
     *
     * @return
     */
    public static List<Effect> buildEffectBeans(boolean containAvatar) {
        List<Effect> humanEffects = new ArrayList<>();
        humanEffects.add(new Effect("", "人体关键点", Effect.TYPE_HUMAN, Effect.MODULE_CODE_HUMAN_LANDMARKS));
        if (containAvatar) {//需要人体骨骼
            humanEffects.add(new Effect("", "人体骨骼", Effect.TYPE_HUMAN_FULL_OR_HALF, Effect.MODULE_CODE_HUMAN_SKELETON));
        }
        return humanEffects;
    }

    /**
     * 人体关键点效果
     *
     * @return
     */
    public static List<Effect> getLandMarkEffects() {
        List<Effect> landMarkEffects = new ArrayList<>();
        landMarkEffects.add(new Effect(FULL_BODY_LANDMARKS_BUNDLE_PATH, "人体关键点 全身", Effect.TYPE_HUMAN, Effect.MODULE_CODE_HUMAN_LANDMARKS_FULL));
        landMarkEffects.add(new Effect(HALF_BODY_LANDMARKS_BUNDLE_PATH, "人体关键点 半身", Effect.TYPE_HUMAN, Effect.MODULE_CODE_HUMAN_LANDMARKS_HALF));
        return landMarkEffects;
    }

    /**
     * 人体骨骼空的Effect
     * 仅仅用于记录
     * @return
     */
    public static List<Effect> getSkeletonEffects() {
        List<Effect> skeletonEffects = new ArrayList<>();
        skeletonEffects.add(new Effect("", "人体骨骼 全身", Effect.TYPE_HUMAN_FULL_OR_HALF, Effect.MODULE_CODE_HUMAN_SKELETON_FULL));
        skeletonEffects.add(new Effect("", "人体骨骼 半身", Effect.TYPE_HUMAN_FULL_OR_HALF, Effect.MODULE_CODE_HUMAN_SKELETON_HALF));
        return skeletonEffects;
    }

    /**
     * 创建title
     *
     * @return
     */
    public static HashMap<String, Integer> buildTitle() {
        HashMap<String, Integer> hashMap = new HashMap<>();
        hashMap.put("title_tv", R.string.body);
        hashMap.put("title_img", R.drawable.demo_sidebar_icon_body);
        return hashMap;
    }
}
