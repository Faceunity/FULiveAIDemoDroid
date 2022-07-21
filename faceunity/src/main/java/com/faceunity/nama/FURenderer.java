package com.faceunity.nama;

import android.content.Context;

import com.faceunity.FUConfig;
import com.faceunity.core.callback.OperateCallback;
import com.faceunity.core.enumeration.FUAITypeEnum;
import com.faceunity.core.faceunity.FUAIKit;
import com.faceunity.core.faceunity.FURenderConfig;
import com.faceunity.core.faceunity.FURenderKit;
import com.faceunity.core.faceunity.FURenderManager;
import com.faceunity.core.utils.FULogger;
import com.faceunity.nama.utils.FuDeviceUtils;

import java.io.File;
import java.util.Arrays;

/**
 * 基于 Nama SDK 封装，方便集成，使用步骤：
 * <p>
 * 1. OnFaceUnityControlListener 定义了 UI 上的交互接口
 * 2. FURenderer.Builder 构造器设置相应的参数，
 * 3. SurfaceView 创建和销毁时，分别调用 onSurfaceCreated 和 onSurfaceDestroyed
 * 4. 相机朝向变化和设备方向变化时，分别调用 onCameraChanged 和 onDeviceOrientationChanged
 * 4. 处理图像时调用 onDrawFrame，针对不同数据类型，提供了纹理和 buffer 输入多种方案
 * </p>
 */
public final class FURenderer {
    private static final String TAG = "FURenderer";

    private FURenderer() {
    }

    /**
     * 人脸点位类型，75 点 和 239 点
     */
    public static final FUAITypeEnum FACE_LANDMARKS_75 = FUAITypeEnum.FUAITYPE_FACELANDMARKS75;
    public static final FUAITypeEnum FACE_LANDMARKS_239 = FUAITypeEnum.FUAITYPE_FACELANDMARKS239;

    /**
     * 渲染模式，普通道具使用 renderItemsEx2 接口，controller 使用 renderBundles 接口
     */
    public static final int RENDER_MODE_NORMAL = 1;
    public static final int RENDER_MODE_CONTROLLER = 2;

    public static final FUAITypeEnum FACEPROCESSOR_EXPRESSION_RECOGNIZER = FUAITypeEnum.FUAITYPE_FACEPROCESSOR_EXPRESSION_RECOGNIZER;
    public static final FUAITypeEnum FACEPROCESSOR_EMOTION_RECOGNIZER = FUAITypeEnum.FUAITYPE_FACEPROCESSOR_EMOTION_RECOGNIZER;

    public static final String KEY_AI_TYPE = "aitype";
    public static final String KEY_LANDMARKS_TYPE = "landmarks_type";

    /* 舌头、表情检测，欧拉角结果 */
    private final int[] mTongueDirection = new int[1];
    private final int[] mEmotion = new int[1];
    private final int[] mExpressionType = new int[1];
    private final float[] mRotationEuler = new float[3];


    public volatile static FURenderer INSTANCE;

    /* 特效FURenderKit*/
    private FURenderKit mFURenderKit;
    /* 特效FURenderKit*/
    private FUAIKit mFUAKit;

    /* AI道具*/
    private String BUNDLE_AI_FACE = "model" + File.separator + "ai_face_processor.bundle";
    // 人体
    private String BUNDLE_AI_HUMAN = "model" + File.separator + "ai_human_processor.bundle";
    // 人体
    public static String BUNDLE_AI_HUMAN_GPU = "model" + File.separator + "ai_human_processor_gpu.bundle";
    private String BUNDLE_AI_HAND = "model" + File.separator + "ai_hand_processor.bundle";
    /* AI道具舌头*/
    private String BUNDLE_TONGUE = "graphics" + File.separator + "tongue.bundle";

    public static FURenderer getInstance() {
        if (INSTANCE == null) {
            synchronized (FURenderer.class) {
                if (INSTANCE == null) {
                    INSTANCE = new FURenderer();
                    INSTANCE.mFURenderKit = FURenderKit.getInstance();
                    INSTANCE.mFUAKit = FUAIKit.getInstance();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 替换方法
     * 1.初始化faceunity
     * 2.加载道具
     * 3.加载
     *
     * @param context
     */
    public void setup(Context context) {
        FURenderManager.setKitDebug(FULogger.LogLevel.TRACE);
        FURenderManager.setCoreDebug(FULogger.LogLevel.ERROR);
        FURenderManager.registerFURender(context, authpack.A(), new OperateCallback() {
            @Override
            public void onSuccess(int i, String s) {
                if (i == FURenderConfig.OPERATE_SUCCESS_AUTH) {
                    mFUAKit.loadAIProcessor(BUNDLE_AI_FACE, FUAITypeEnum.FUAITYPE_FACEPROCESSOR);//人脸识别
                    if (FUConfig.DEVICE_LEVEL  > FuDeviceUtils.DEVICE_LEVEL_MID) {
                        mFUAKit.loadAIProcessor(BUNDLE_AI_HUMAN_GPU, FUAITypeEnum.FUAITYPE_HUMAN_PROCESSOR);//人体识别
                    } else {
                        mFUAKit.loadAIProcessor(BUNDLE_AI_HUMAN, FUAITypeEnum.FUAITYPE_HUMAN_PROCESSOR);//人体识别
                    }
                    mFUAKit.loadAIProcessor(BUNDLE_AI_HAND, FUAITypeEnum.FUAITYPE_HANDGESTURE);//手势识别
                    mFUAKit.loadAIProcessor(BUNDLE_TONGUE, FUAITypeEnum.FUAITYPE_TONGUETRACKING);//舌头
                }
            }

            @Override
            public void onFail(int i, String s) {
            }
        });
    }

    /**
     * 获取 Nama SDK 版本号，例如 7_1_0_phy_5cadfa8_f92de5b
     *
     * @return version
     */
    public String getVersion() {
        return mFURenderKit.getVersion();
    }


    //----------------------------------------------------------------------------------------------

    /**
     * 手势识别类型
     */
    public static final class FuAiGestureType {
        public static final int FUAIGESTURE_UNKNOWN = 0;
        public static final int FUAIGESTURE_THUMB = 1;
        public static final int FUAIGESTURE_KORHEART = 2;
        public static final int FUAIGESTURE_SIX = 3;
        public static final int FUAIGESTURE_FIST = 4;
        public static final int FUAIGESTURE_PALM = 5;
        public static final int FUAIGESTURE_ONE = 6;
        public static final int FUAIGESTURE_TWO = 7;
        public static final int FUAIGESTURE_OK = 8;
        public static final int FUAIGESTURE_ROCK = 9;
        public static final int FUAIGESTURE_CROSS = 10;
        public static final int FUAIGESTURE_HOLD = 11;
        public static final int FUAIGESTURE_GREET = 12;
        public static final int FUAIGESTURE_PHOTO = 13;
        public static final int FUAIGESTURE_HEART = 14;
        public static final int FUAIGESTURE_MERGE = 15;
        public static final int FUAIGESTURE_EIGHT = 16;
        public static final int FUAIGESTURE_HALFFIST = 17;
        public static final int FUAIGESTURE_GUN = 18;
    }

    /**
     * 舌头检测类型
     */
    public static final class FuAiTongueType {
        public static final int FUAITONGUE_UNKNOWN = 0;
        public static final int FUAITONGUE_UP = 1 << 1;
        public static final int FUAITONGUE_DOWN = 1 << 2;
        public static final int FUAITONGUE_LEFT = 1 << 3;
        public static final int FUAITONGUE_RIGHT = 1 << 4;
        public static final int FUAITONGUE_LEFT_UP = 1 << 5;
        public static final int FUAITONGUE_LEFT_DOWN = 1 << 6;
        public static final int FUAITONGUE_RIGHT_UP = 1 << 7;
        public static final int FUAITONGUE_RIGHT_DOWN = 1 << 8;
        public static final int FUAITONGUE_FORWARD = 1 << 9;
        public static final int FUAITONGUE_NO_STRETCH = 1 << 10;
    }

    /**
     * 表情识别类型
     */
    public static final class FuAiFaceExpressionType {
        public static final int FACE_EXPRESSION_UNKONW = 0;
        /**
         * 抬眉毛
         */
        public static final int FACE_EXPRESSION_BROW_UP = 1 << 1;
        /**
         * 皱眉
         */
        public static final int FACE_EXPRESSION_BROW_FROWN = 1 << 2;
        /**
         * 闭左眼
         */
        public static final int FACE_EXPRESSION_LEFT_EYE_CLOSE = 1 << 3;
        /**
         * 闭右眼
         */
        public static final int FACE_EXPRESSION_RIGHT_EYE_CLOSE = 1 << 4;
        /**
         * 睁大眼睛
         */
        public static final int FACE_EXPRESSION_EYE_WIDE = 1 << 5;
        /**
         * 抬左嘴角
         */
        public static final int FACE_EXPRESSION_MOUTH_SMILE_LEFT = 1 << 6;
        /**
         * 抬右嘴角
         */
        public static final int FACE_EXPRESSION_MOUTH_SMILE_RIGHT = 1 << 7;
        /**
         * 嘴巴 欧
         */
        public static final int FACE_EXPRESSION_MOUTH_FUNNEL = 1 << 8;
        /**
         * 嘴巴 啊
         */
        public static final int FACE_EXPRESSION_MOUTH_OPEN = 1 << 9;
        /**
         * 嘟嘴
         */
        public static final int FACE_EXPRESSION_MOUTH_PUCKER = 1 << 10;
        /**
         * 抿嘴
         */
        public static final int FACE_EXPRESSION_MOUTH_ROLL = 1 << 11;
        /**
         * 鼓脸
         */
        public static final int FACE_EXPRESSION_MOUTH_PUFF = 1 << 12;
        /**
         * 微笑
         */
        public static final int FACE_EXPRESSION_MOUTH_SMILE = 1 << 13;
        /**
         * 撇嘴
         */
        public static final int FACE_EXPRESSION_MOUTH_FROWN = 1 << 14;
        /**
         * 左转头
         */
        public static final int FACE_EXPRESSION_HEAD_LEFT = 1 << 15;
        /**
         * 右转头
         */
        public static final int FACE_EXPRESSION_HEAD_RIGHT = 1 << 16;
        /**
         * 点头
         */
        public static final int FACE_EXPRESSION_HEAD_NOD = 1 << 17;
    }

    /**
     * 情绪识别类型
     */
    public static final class FuAiEmotionType {
        public static final int FUAIEMOTION_UNKNOWN = 0;
        /**
         * 开心
         */
        public static final int FUAIEMOTION_HAPPY = 1 << 1;
        /**
         * 悲伤
         */
        public static final int FUAIEMOTION_SAD = 1 << 2;
        /**
         * 生气
         */
        public static final int FUAIEMOTION_ANGRY = 1 << 3;
        /**
         * 惊讶
         */
        public static final int FUAIEMOTION_SURPRISE = 1 << 4;
        /**
         * 恐惧
         */
        public static final int FUAIEMOTION_FEAR = 1 << 5;
        /**
         * 厌恶
         */
        public static final int FUAIEMOTION_DISGUST = 1 << 6;
        /**
         * 平静
         */
        public static final int FUAIEMOTION_NEUTRAL = 1 << 7;
        /**
         * 困惑
         */
        public static final int FUAIEMOTION_CONFUSE = 1 << 8;
    }

    /**
     * 检测人体手势类型
     *
     * @return
     */
    public int detectHumanGesture() {
        if (mFUAKit.handProcessorGetNumResults() > 0) {
            return mFUAKit.handDetectorGetResultGestureType(0);
        }
        return -1;
    }

    /**
     * 获取手指数量
     * @return
     */
    public int handProcessorGetNumResults(){
        return mFUAKit.handProcessorGetNumResults();
    }

    /**
     * 检测人体动作类型
     *
     * @return
     */
    public int detectHumanAction() {
        if (mFUAKit.humanProcessorGetNumResults() > 0) {
            return mFUAKit.humanProcessorGetResultActionType(0);
        }
        return -1;
    }

    /**
     * 检测舌头方向
     *
     * @return
     */
    public int detectFaceTongue() {
        if (mFUAKit.isTracking() > 0) {
            int[] tongueDirection = mTongueDirection;
            mFUAKit.getFaceInfo(0, "tongue_direction", tongueDirection);
            return tongueDirection[0];
        }
        return -1;
    }

    /**
     * 检测情绪
     */
    public void detectFaceEmotion(int[] result) {
        Arrays.fill(result, 0);
        if (mFUAKit.isTracking() > 0) {
            int[] emotion = mEmotion;
            mFUAKit.getFaceInfo(0, "emotion", emotion);
            int em = emotion[0];
            if ((em & FuAiEmotionType.FUAIEMOTION_HAPPY) != 0) {
                result[0] = FuAiEmotionType.FUAIEMOTION_HAPPY;
            } else if ((em & FuAiEmotionType.FUAIEMOTION_SAD) != 0) {
                result[0] = FuAiEmotionType.FUAIEMOTION_SAD;
            } else if ((em & FuAiEmotionType.FUAIEMOTION_ANGRY) != 0) {
                result[0] = FuAiEmotionType.FUAIEMOTION_ANGRY;
            } else if ((em & FuAiEmotionType.FUAIEMOTION_SURPRISE) != 0) {
                result[0] = FuAiEmotionType.FUAIEMOTION_SURPRISE;
            } else if ((em & FuAiEmotionType.FUAIEMOTION_FEAR) != 0) {
                result[0] = FuAiEmotionType.FUAIEMOTION_FEAR;
            } else if ((em & FuAiEmotionType.FUAIEMOTION_DISGUST) != 0) {
                result[0] = FuAiEmotionType.FUAIEMOTION_DISGUST;
            } else if ((em & FuAiEmotionType.FUAIEMOTION_NEUTRAL) != 0) {
                result[0] = FuAiEmotionType.FUAIEMOTION_NEUTRAL;
            }
            if ((em & FuAiEmotionType.FUAIEMOTION_CONFUSE) != 0) {
                result[1] = 1;
            }
//            LogUtils.debug(TAG, "emotion %s, result %s ", Arrays.toString(emotion), Arrays.toString(result));
        }
    }

    /**
     * 表情识别
     *
     * @param result
     * @return
     */
    public void detectFaceExpression(int[] result) {
        if (result == null || result.length < 4) {
            return;
        }
        Arrays.fill(result, 0);
        if (mFUAKit.isTracking() > 0) {
            int[] expressionType = mExpressionType;
            mFUAKit.getFaceInfo(0, "expression_type", expressionType);
            int et = expressionType[0];
            if (et > 0) {
                int index = 0;
                if ((et & FuAiFaceExpressionType.FACE_EXPRESSION_BROW_UP) != 0) {
                    result[index++] = FuAiFaceExpressionType.FACE_EXPRESSION_BROW_UP;
                }
                if ((et & FuAiFaceExpressionType.FACE_EXPRESSION_BROW_FROWN) != 0) {
                    result[index++] = FuAiFaceExpressionType.FACE_EXPRESSION_BROW_FROWN;
                }
                if ((et & FuAiFaceExpressionType.FACE_EXPRESSION_LEFT_EYE_CLOSE) != 0) {
                    result[index++] = FuAiFaceExpressionType.FACE_EXPRESSION_LEFT_EYE_CLOSE;
                }
                if ((et & FuAiFaceExpressionType.FACE_EXPRESSION_RIGHT_EYE_CLOSE) != 0) {
                    result[index++] = FuAiFaceExpressionType.FACE_EXPRESSION_RIGHT_EYE_CLOSE;
                }
                if ((et & FuAiFaceExpressionType.FACE_EXPRESSION_EYE_WIDE) != 0) {
                    result[index++] = FuAiFaceExpressionType.FACE_EXPRESSION_EYE_WIDE;
                }
                if ((et & FuAiFaceExpressionType.FACE_EXPRESSION_MOUTH_SMILE_LEFT) != 0) {
                    result[index++] = FuAiFaceExpressionType.FACE_EXPRESSION_MOUTH_SMILE_LEFT;
                }
                if ((et & FuAiFaceExpressionType.FACE_EXPRESSION_MOUTH_SMILE_RIGHT) != 0) {
                    result[index++] = FuAiFaceExpressionType.FACE_EXPRESSION_MOUTH_SMILE_RIGHT;
                }
                if ((et & FuAiFaceExpressionType.FACE_EXPRESSION_MOUTH_SMILE_RIGHT) != 0) {
                    result[index++] = FuAiFaceExpressionType.FACE_EXPRESSION_MOUTH_SMILE_RIGHT;
                }
                if ((et & FuAiFaceExpressionType.FACE_EXPRESSION_MOUTH_FUNNEL) != 0) {
                    result[index++] = FuAiFaceExpressionType.FACE_EXPRESSION_MOUTH_FUNNEL;
                }
                if ((et & FuAiFaceExpressionType.FACE_EXPRESSION_MOUTH_OPEN) != 0) {
                    result[index++] = FuAiFaceExpressionType.FACE_EXPRESSION_MOUTH_OPEN;
                }
                if ((et & FuAiFaceExpressionType.FACE_EXPRESSION_MOUTH_PUCKER) != 0) {
                    result[index++] = FuAiFaceExpressionType.FACE_EXPRESSION_MOUTH_PUCKER;
                }
                if ((et & FuAiFaceExpressionType.FACE_EXPRESSION_MOUTH_ROLL) != 0) {
                    result[index++] = FuAiFaceExpressionType.FACE_EXPRESSION_MOUTH_ROLL;
                }
                if ((et & FuAiFaceExpressionType.FACE_EXPRESSION_MOUTH_PUFF) != 0) {
                    result[index++] = FuAiFaceExpressionType.FACE_EXPRESSION_MOUTH_PUFF;
                }
                if ((et & FuAiFaceExpressionType.FACE_EXPRESSION_MOUTH_SMILE) != 0) {
                    result[index++] = FuAiFaceExpressionType.FACE_EXPRESSION_MOUTH_SMILE;
                }
                if ((et & FuAiFaceExpressionType.FACE_EXPRESSION_MOUTH_FROWN) != 0) {
                    result[index++] = FuAiFaceExpressionType.FACE_EXPRESSION_MOUTH_FROWN;
                }
                if ((et & FuAiFaceExpressionType.FACE_EXPRESSION_HEAD_LEFT) != 0) {
                    result[index++] = FuAiFaceExpressionType.FACE_EXPRESSION_HEAD_LEFT;
                }
                if ((et & FuAiFaceExpressionType.FACE_EXPRESSION_HEAD_RIGHT) != 0) {
                    result[index++] = FuAiFaceExpressionType.FACE_EXPRESSION_HEAD_RIGHT;
                }
                if ((et & FuAiFaceExpressionType.FACE_EXPRESSION_HEAD_NOD) != 0) {
                    result[index] = FuAiFaceExpressionType.FACE_EXPRESSION_HEAD_NOD;
                }
            }
        }
    }

    /**
     * 人脸旋转欧拉角，顺序是 pitch yaw roll
     *
     * @return
     */
    public float[] getFaceRotationEuler() {
        float[] rotationEuler = mRotationEuler;
        if (mFUAKit.isTracking() > 0) {
            mFUAKit.getFaceInfo(0, "rotation_euler", rotationEuler);
            for (int i = 0; i < rotationEuler.length; i++) {
                rotationEuler[i] = (float) (rotationEuler[i] * 180 / Math.PI);
            }
        } else {
            Arrays.fill(rotationEuler, 0F);
        }
        return rotationEuler;
    }

    /**
     * 识别到的人脸数
     *
     * @return
     */
    public int queryFaceTrackStatus() {
        return mFUAKit.isTracking();
    }

    /**
     * 识别到的人体数
     *
     * @return
     */
    public int queryHumanTrackStatus() {
        return mFUAKit.humanProcessorGetNumResults();
    }

    //----------------------------------------------------------------------------------------------

    //------------------------------FPS 渲染时长回调相关定义------------------------------------

    private static final int NANO_IN_ONE_MILLI_SECOND = 1_000_000;
    private static final int NANO_IN_ONE_SECOND = 1_000_000_000;
    private static final int FRAME_COUNT = 10;
    private boolean mIsRunBenchmark = true;
    private int mCurrentFrameCount;
    private long mLastFrameTimestamp;
    private long mSumRenderTime;

    public void setCallStartTime(long mCallStartTime) {
        if (!mIsRunBenchmark) {
            return;
        }
        this.mCallStartTime = mCallStartTime;
    }

    private long mCallStartTime;

    public interface OnDebugListener {
        /**
         * 统计每 10 帧的平均值
         *
         * @param fps         FPS
         * @param renderTime  渲染时间
         * @param elapsedTime 帧间耗时
         */
        void onFpsChanged(double fps, double renderTime, double elapsedTime);
    }

    public void benchmarkFPS(OnDebugListener mOnDebugListener) {
        if (!mIsRunBenchmark) {
            return;
        }

        mSumRenderTime += System.nanoTime() - mCallStartTime;

        if (++mCurrentFrameCount == FRAME_COUNT) {
            long tmp = System.nanoTime();
            double elapsedTime = (double) (tmp - mLastFrameTimestamp) / FRAME_COUNT;
            double fps = (double) NANO_IN_ONE_SECOND / elapsedTime;
            double renderTime = (double) mSumRenderTime / FRAME_COUNT / NANO_IN_ONE_MILLI_SECOND;
            mLastFrameTimestamp = tmp;
            mSumRenderTime = 0;
            mCurrentFrameCount = 0;

            if (mOnDebugListener != null) {
                mOnDebugListener.onFpsChanged(fps, renderTime, elapsedTime / NANO_IN_ONE_MILLI_SECOND);
            }
        }
    }
}
