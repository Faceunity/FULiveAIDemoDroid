package com.faceunity.nama.source;


import com.faceunity.nama.Effect;
import com.faceunity.nama.FURenderer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * DESC：道具数据构造
 * Created on 2021/3/28
 */
public class EffectSource {
    private static final String ASSETS_DIR = "others/";

    public static final String FACE_LANDMARKS_BUNDLE_PATH = ASSETS_DIR + "landmarks.bundle"; // 人脸特征点
    private static final String FACE_TONGUE_BUNDLE_PATH = ASSETS_DIR + "set_tongue.bundle"; // 舌头检测
    public static final String FACE_EXPRESSION_BUNDLE_PATH = ASSETS_DIR + "aitype.bundle"; // 表情识别
    public static final String FACE_EMOTION_BUNDLE_PATH = ASSETS_DIR + "aitype2.bundle"; // 情绪识别
    private static final String HUMAN_MASK_BUNDLE_PATH = ASSETS_DIR + "human_mask.bundle"; // 人像分割
    private static final String HUMAN_ACTION_BUNDLE_PATH = ASSETS_DIR + "human_action.bundle"; // 动作识别
    private static final String HUMAN_GESTURE_BUNDLE_PATH = ASSETS_DIR + "human_gesture.bundle"; // 手势识别
    private static final String HAIR_MASK_BUNDLE_PATH = ASSETS_DIR + "hair_normal_algorithm.bundle"; // 头发分割
    private static final String HEAD_MASK_BUNDLE_PATH = ASSETS_DIR + "head_mask.bundle"; // 头部分割
    private static final String FULL_BODY_LANDMARKS_BUNDLE_PATH = ASSETS_DIR + "bodyLandmarks_dance.bundle"; // 人体关键点 全身
    private static final String HALF_BODY_LANDMARKS_BUNDLE_PATH = ASSETS_DIR + "bodyLandmarks_selife.bundle"; // 人体关键点 半身
    private static Map<Integer, List<Effect>> mEffectMap;

    /**
     * @return
     */
    public static Map<Integer, List<Effect>> buildPropBeans(boolean containHumanAvatar) {
        /*
        * 存储元素的方式
        * key 元素类型
            public static final int TYPE_FACE = 1; //脸部
            public static final int TYPE_HUMAN = 2; //人体
            public static final int TYPE_GESTURE = 3; //手势
            public static final int TYPE_SEGMENTATION = 4; //分割
            public static final int TYPE_ACTION = 5; //动作
        * value 元素值 effect数组
        * */
        Map<Integer, List<Effect>> effectMap = new HashMap<>(16);
        //Effect.TYPE_FACE
        List<Effect> faceEffects = new ArrayList<>();
        Map<String, Object> paramMap = new HashMap<>(4);
        paramMap.put(FURenderer.KEY_LANDMARKS_TYPE, FURenderer.FACE_LANDMARKS_239);
        Effect mFaceLandmarksEffect = new Effect(FACE_LANDMARKS_BUNDLE_PATH, "人脸特征点", Effect.TYPE_FACE, Effect.MODULE_CODE_FACE_LANDMARKS, paramMap);
        paramMap = new HashMap<>(4);
        paramMap.put(FURenderer.KEY_AI_TYPE, FURenderer.FACEPROCESSOR_EXPRESSION_RECOGNIZER);
        Effect mAiTypeEffect = new Effect(FACE_EXPRESSION_BUNDLE_PATH, "表情识别", Effect.TYPE_FACE, Effect.MODULE_CODE_FACE_EXPRESSION, paramMap);
        faceEffects.add(mFaceLandmarksEffect);
        faceEffects.add(new Effect(FACE_TONGUE_BUNDLE_PATH,  "舌头检测", Effect.TYPE_FACE, Effect.MODULE_CODE_FACE_TONGUE));
        faceEffects.add(mAiTypeEffect);
        Map<String, Object> paramMap2 = new HashMap<>(4);
        paramMap2.put(FURenderer.KEY_AI_TYPE, FURenderer.FACEPROCESSOR_EMOTION_RECOGNIZER);
        faceEffects.add(new Effect(FACE_EMOTION_BUNDLE_PATH, "情绪识别", Effect.TYPE_FACE, Effect.MODULE_CODE_FACE_EMOTION, paramMap2));
        effectMap.put(Effect.TYPE_FACE, Collections.unmodifiableList(faceEffects));

        //TYPE_HUMAN
        List<Effect> humanEffects = new ArrayList<>();
        humanEffects.add(new Effect("", "人体关键点", Effect.TYPE_HUMAN, Effect.MODULE_CODE_HUMAN_LANDMARKS));
        if (containHumanAvatar) {//需要人体骨骼
            humanEffects.add(new Effect("", "人体骨骼", Effect.TYPE_HUMAN, Effect.MODULE_CODE_HUMAN_SKELETON));
        }

        //TYPE_HUMAN 子项目 全身 半身
        List<Effect> humanEffects_ = new ArrayList<>();
        humanEffects_.add(new Effect(HALF_BODY_LANDMARKS_BUNDLE_PATH, Effect.MODULE_CODE_HUMAN_LANDMARKS));
        humanEffects_.add(new Effect(FULL_BODY_LANDMARKS_BUNDLE_PATH, Effect.MODULE_CODE_HUMAN_LANDMARKS));

        effectMap.put(Effect.TYPE_HUMAN, Collections.unmodifiableList(humanEffects));
        effectMap.put(Effect.TYPE_HUMAN_FULL_OR_HALF, Collections.unmodifiableList(humanEffects_));

        //TYPE_GESTURE
        List<Effect> gestureEffects = new ArrayList<>();
        gestureEffects.add(new Effect(HUMAN_GESTURE_BUNDLE_PATH, "手势识别", Effect.TYPE_GESTURE, Effect.MODULE_CODE_HAND_GESTURE));
        effectMap.put(Effect.TYPE_GESTURE, Collections.unmodifiableList(gestureEffects));

        //TYPE_SEGMENTATION
        List<Effect> segEffects = new ArrayList<>();
        segEffects.add(new Effect(HUMAN_MASK_BUNDLE_PATH,  "人像分割", Effect.TYPE_SEGMENTATION, Effect.MODULE_CODE_HUMAN_SEGMENTATION));
        segEffects.add(new Effect(HAIR_MASK_BUNDLE_PATH, "头发分割", Effect.TYPE_SEGMENTATION, Effect.MODULE_CODE_HAIR_SEGMENTATION));
        segEffects.add(new Effect(HEAD_MASK_BUNDLE_PATH, "头部分割", Effect.TYPE_SEGMENTATION, Effect.MODULE_CODE_HEAD_SEGMENTATION));
        effectMap.put(Effect.TYPE_SEGMENTATION, Collections.unmodifiableList(segEffects));

        //动作
        List<Effect> actionEffects = new ArrayList<>();
        actionEffects.add(new Effect(HUMAN_ACTION_BUNDLE_PATH, "动作识别", Effect.TYPE_ACTION, Effect.MODULE_CODE_ACTION));
        effectMap.put(Effect.TYPE_ACTION, Collections.unmodifiableList(actionEffects));
        mEffectMap = Collections.unmodifiableMap(effectMap);
        return mEffectMap;
    }
}
