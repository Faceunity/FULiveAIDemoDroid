package com.faceunity.nama.source;


import com.faceunity.nama.Effect;
import com.faceunity.nama.FURenderer;
import com.faceunity.nama.R;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * DESC：
 * 人脸道具类
 * Created on 2021/3/28
 */
public class EffectFaceSource {
    private static final String ASSETS_DIR = "others/";

    public static final String FACE_LANDMARKS_BUNDLE_PATH = ASSETS_DIR + "landmarks.bundle"; // 人脸特征点
    private static final String FACE_TONGUE_BUNDLE_PATH = ASSETS_DIR + "set_tongue.bundle"; // 舌头检测
    public static final String FACE_EXPRESSION_BUNDLE_PATH = ASSETS_DIR + "aitype.bundle"; // 表情识别
    public static final String FACE_EMOTION_BUNDLE_PATH = ASSETS_DIR + "aitype2.bundle"; // 情绪识别
    public static final String FACE_ARMESH_BUNDLE_PATH = "graphics/" + "armesh.bundle"; // armesh

    /**
     * 人脸特效
     * @return
     */
    public static List<Effect> buildEffectBeans() {
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
        paramMap = new HashMap<>(4);
        paramMap.put(FURenderer.KEY_AI_TYPE, FURenderer.FACEPROCESSOR_EMOTION_RECOGNIZER);
        faceEffects.add(new Effect(FACE_EMOTION_BUNDLE_PATH, "情绪识别", Effect.TYPE_FACE, Effect.MODULE_CODE_FACE_EMOTION, paramMap));

        faceEffects.add(new Effect(FACE_ARMESH_BUNDLE_PATH, "armesh", Effect.TYPE_FACE, Effect.MODULE_CODE_FACE_ARMESH, null));
        return faceEffects;
    }

    /**
     * 创建title
     * @return
     */
    public static HashMap<String,Integer> buildTitle(){
        HashMap<String,Integer> hashMap = new HashMap<>();
        hashMap.put("title_tv", R.string.face);
        hashMap.put("title_img", R.drawable.demo_sidebar_icon_face);
        return hashMap;
    }
}
