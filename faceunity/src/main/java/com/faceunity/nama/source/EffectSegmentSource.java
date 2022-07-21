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
public class EffectSegmentSource {
    private static final String ASSETS_DIR = "others/";
    private static final String HUMAN_MASK_BUNDLE_PATH = ASSETS_DIR + "human_mask.bundle"; // 人像分割
    private static final String HAIR_MASK_BUNDLE_PATH = ASSETS_DIR + "hair_normal_algorithm.bundle"; // 头发分割
    private static final String HEAD_MASK_BUNDLE_PATH = ASSETS_DIR + "head_mask.bundle"; // 头部分割

    /**
     * 手势特效
     * @return
     */
    public static List<Effect> buildEffectBeans() {
        List<Effect> segEffects = new ArrayList<>();
        segEffects.add(new Effect(HUMAN_MASK_BUNDLE_PATH,  "人像分割", Effect.TYPE_SEGMENTATION, Effect.MODULE_CODE_HUMAN_SEGMENTATION));
        segEffects.add(new Effect(HAIR_MASK_BUNDLE_PATH, "头发分割", Effect.TYPE_SEGMENTATION, Effect.MODULE_CODE_HAIR_SEGMENTATION));
        segEffects.add(new Effect(HEAD_MASK_BUNDLE_PATH, "头部分割", Effect.TYPE_SEGMENTATION, Effect.MODULE_CODE_HEAD_SEGMENTATION));
        return segEffects;
    }

    /**
     * 创建title
     * @return
     */
    public static HashMap<String,Integer> buildTitle(){
        HashMap<String,Integer> hashMap = new HashMap<>();
        hashMap.put("title_tv", R.string.segmentation);
        hashMap.put("title_img", R.drawable.demo_sidebar_icon_segmentation);
        return hashMap;
    }
}
