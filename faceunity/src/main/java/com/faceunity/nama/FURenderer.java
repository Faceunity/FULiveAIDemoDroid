package com.faceunity.nama;

import android.content.Context;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.faceunity.wrapper.faceunity;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
public class FURenderer {
    private static final String TAG = "FURenderer";
    /**
     * 算法模型和图形道具文件夹
     */
    private static final String ASSETS_DIR_MODEL = "model/";
    private static final String ASSETS_DIR_GRAPHICS = "graphics/";
    private static final String ASSETS_DIR_PTA = "pta/";
    /**
     * 输入的 texture 类型，OES 或 2D
     */
    public static final int INPUT_EXTERNAL_OES_TEXTURE = faceunity.FU_ADM_FLAG_EXTERNAL_OES_TEXTURE;
    public static final int INPUT_2D_TEXTURE = 0;
    /**
     * 输入的 buffer 格式，NV21、I420 或 RGBA
     */
    public static final int INPUT_FORMAT_NV21 = faceunity.FU_FORMAT_NV21_BUFFER;
    public static final int INPUT_FORMAT_I420 = faceunity.FU_FORMAT_I420_BUFFER;
    public static final int INPUT_FORMAT_RGBA = faceunity.FU_FORMAT_RGBA_BUFFER;

    /**
     * 算法检测类型
     */
    public static final int TRACK_TYPE_FACE = faceunity.FUAITYPE_FACEPROCESSOR;
    public static final int TRACK_TYPE_HUMAN = faceunity.FUAITYPE_HUMAN_PROCESSOR;
    public static final int TRACK_TYPE_GESTURE = faceunity.FUAITYPE_HANDGESTURE;

    /**
     * 渲染模式，普通道具使用 renderItemsEx2 接口，controller 使用 renderBundles 接口
     */
    public static final int RENDER_MODE_NORMAL = 1;
    public static final int RENDER_MODE_CONTROLLER = 2;
    /**
     * 人体跟踪模式，全身或者半身
     */
    public static final int HUMAN_TRACK_SCENE_FULL = 1;
    public static final int HUMAN_TRACK_SCENE_HALF = 0;

    /* 人体骨骼动作识别道具 */
    private static final String[] GESTURE_BIND_BUNDLES = {"anim_idle.bundle", "anim_eight.bundle", "anim_fist.bundle", "anim_greet.bundle"
            , "anim_gun.bundle", "anim_heart.bundle", "anim_hold.bundle", "anim_korheart.bundle", "anim_merge.bundle",
            "anim_ok.bundle", "anim_one.bundle", "anim_palm.bundle", "anim_rock.bundle", "anim_six.bundle",
            "anim_thumb.bundle", "anim_two.bundle"};

    /* 句柄数组长度 2 */
    private static final int ITEMS_ARRAY_COUNT = 1;
    /* 存放贴纸句柄的数组 */
    private int[] mItemsArray;
    private final Context mContext;
    /* IO 线程 Handler */
    private Handler mFuItemHandler;
    /* 递增的帧 ID */
    private int mFrameId = 0;
    /* 贴纸道具 */
    private Set<Effect> mEffectList = new HashSet<>();
    /* 最大识别的人体数 */
    private int mMaxHumans = 1;
    /* 最大识别的人脸数 */
    private int mMaxFaces = 1;
    /* 是否手动创建 EGLContext，默认不创建 */
    private boolean mIsCreateEGLContext = false;
    /* 输入图像的纹理类型，默认 2D */
    private int mInputTextureType = INPUT_2D_TEXTURE;
    /* 输入图像的 buffer 类型，此项一般不用改 */
    private int mInputImageFormat = 0;
    /* 输入图像的方向，默认前置相机 270 */
    private int mInputImageOrientation = 270;
    /* 设备方向，默认竖屏 */
    private int mDeviceOrientation = 90;
    /* 人脸识别方向，默认 1，通过 createRotationMode 方法获得 */
    private int mRotationMode = faceunity.FU_ROTATION_MODE_90;
    /* 相机前后方向，默认前置相机  */
    private int mCameraType = Camera.CameraInfo.CAMERA_FACING_FRONT;
    /* 事件队列 */
    private final ArrayList<Runnable> mEventQueue = new ArrayList<>(16);
    /* 事件队列操作锁 */
    private final Object mLock = new Object();
    /* GL 线程 ID */
    private long mGlThreadId;
    /* 是否已经全局初始化，确保只初始化一次 */
    private static boolean sIsInited;

    private int mRenderMode = RENDER_MODE_NORMAL;
    private int[] mControllerBoundItems;
    private int mHumanTrackScene = HUMAN_TRACK_SCENE_FULL;
    private faceunity.RotatedImage mRotatedImage = new faceunity.RotatedImage();

    /**
     * 初始化系统环境，加载底层数据，并进行网络鉴权。
     * 应用使用期间只需要初始化一次，无需释放数据。
     * 不需要 GL 环境，但必须在SDK其他接口前调用，否则会引起应用崩溃。
     *
     * @param context
     */
    public static void initFURenderer(Context context) {
        if (sIsInited) {
            return;
        }
        // {trace:0, debug:1, info:2, warn:3, error:4, critical:4, off:6}
        int logLevel = 6;
        faceunity.fuSetLogLevel(logLevel);
        Log.i(TAG, "initFURenderer setLogLevel: " + logLevel);

        // 初始化高通 DSP
        String path = context.getApplicationInfo().nativeLibraryDir;
        faceunity.fuHexagonInitWithPath(path);

        // 获取 Nama SDK 版本信息
        Log.e(TAG, "fu sdk version " + faceunity.fuGetVersion());
        // v3 不再使用，第一个参数传空字节数组即可
        int isSetup = faceunity.fuSetup(new byte[0], authpack.A());
        Log.d(TAG, "fuSetup. isSetup: " + (isSetup == 0 ? "no" : "yes"));
        sIsInited = isLibInit();
        Log.d(TAG, "initFURenderer finish. isLibraryInit: " + (sIsInited ? "yes" : "no"));

        loadAiModel(context, ASSETS_DIR_MODEL + "ai_face_processor.bundle", faceunity.FUAITYPE_FACEPROCESSOR);
        loadAiModel(context, ASSETS_DIR_MODEL + "ai_human_processor.bundle", faceunity.FUAITYPE_HUMAN_PROCESSOR);
        loadAiModel(context, ASSETS_DIR_MODEL + "ai_gesture.bundle", faceunity.FUAITYPE_HANDGESTURE);
    }

    /**
     * 释放鉴权数据占用的内存。如需再次使用，需要调用 fuSetup
     */
    public static void destroyLibData() {
        releaseAiModel(faceunity.FUAITYPE_FACEPROCESSOR);
        releaseAiModel(faceunity.FUAITYPE_HUMAN_PROCESSOR);
        releaseAiModel(faceunity.FUAITYPE_HANDGESTURE);
        if (sIsInited) {
            faceunity.fuDestroyLibData();
            sIsInited = isLibInit();
            Log.d(TAG, "destroyLibData. isLibraryInit: " + (sIsInited ? "yes" : "no"));
        }
    }

    /**
     * SDK 是否初始化
     *
     * @return
     */
    public static boolean isLibInit() {
        return faceunity.fuIsLibraryInit() == 1;
    }

    /**
     * 加载 AI 模型资源，一般在 onSurfaceCreated 方法调用，不需要 EGL Context，耗时操作，可以异步执行
     *
     * @param context
     * @param bundlePath ai_model.bundle
     * @param type       faceunity.FUAITYPE_XXX
     */
    private static void loadAiModel(Context context, String bundlePath, int type) {
        byte[] buffer = readFile(context, bundlePath);
        if (buffer != null) {
            int isLoaded = faceunity.fuLoadAIModelFromPackage(buffer, type);
            Log.d(TAG, "loadAiModel. type: " + type + ", isLoaded: " + (isLoaded == 1 ? "yes" : "no"));
        }
    }

    /**
     * 释放 AI 模型资源，一般在 onSurfaceDestroyed 方法调用，不需要 EGL Context，对应 loadAiModel 方法
     *
     * @param type
     */
    private static void releaseAiModel(int type) {
        if (faceunity.fuIsAIModelLoaded(type) == 1) {
            int isReleased = faceunity.fuReleaseAIModel(type);
            Log.d(TAG, "releaseAiModel. type: " + type + ", isReleased: " + (isReleased == 1 ? "yes" : "no"));
        }
    }

    /**
     * 加载 bundle 道具，不需要 EGL Context，耗时操作，可以异步执行
     *
     * @param bundlePath bundle 文件路径
     * @return 道具句柄，大于 0 表示加载成功
     */
    private static int loadItem(Context context, String bundlePath) {
        int handle = 0;
        if (!TextUtils.isEmpty(bundlePath)) {
            byte[] buffer = readFile(context, bundlePath);
            if (buffer != null) {
                handle = faceunity.fuCreateItemFromPackage(buffer);
            }
        }
        Log.d(TAG, "loadItem. bundlePath: " + bundlePath + ", itemHandle: " + handle);
        return handle;
    }

    /**
     * 从 assets 文件夹或者本地磁盘读文件，一般在 IO 线程调用
     *
     * @param context
     * @param path
     * @return
     */
    private static byte[] readFile(Context context, String path) {
        InputStream is = null;
        try {
            is = context.getAssets().open(path);
        } catch (IOException e1) {
            Log.w(TAG, "readFile: e1", e1);
            // open assets failed, then try sdcard
            try {
                is = new FileInputStream(path);
            } catch (IOException e2) {
                Log.w(TAG, "readFile: e2", e2);
            }
        }
        if (is != null) {
            try {
                byte[] buffer = new byte[is.available()];
                int length = is.read(buffer);
                Log.v(TAG, "readFile. path: " + path + ", length: " + length + " Byte");
                is.close();
                return buffer;
            } catch (IOException e3) {
                Log.e(TAG, "readFile: e3", e3);
            }
        }
        return null;
    }

    /**
     * 获取相机方向
     *
     * @param cameraFacing
     * @return
     */
    public static int getCameraOrientation(int cameraFacing) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        int cameraId = -1;
        int numCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numCameras; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == cameraFacing) {
                cameraId = i;
                break;
            }
        }
        if (cameraId < 0) {
            // no front camera, regard it as back camera
            return 90;
        } else {
            return info.orientation;
        }
    }

    /**
     * 获取 Nama SDK 完整版本号，例如 6.7.0_tf_phy-f1e36a93-b9e3359-b5f220d
     *
     * @return full version
     */
    public static String getFuVersion() {
        return faceunity.fuGetVersion();
    }

    private FURenderer(Context context) {
        mContext = context;
    }

    /**
     * 创建及初始化 SDK 相关资源，必须在 GL 线程调用。如果没有 GL 环境，请把 mIsCreateEGLContext 设置为 true。
     */
    public void onSurfaceCreated() {
        Log.e(TAG, "onSurfaceCreated");
        mGlThreadId = Thread.currentThread().getId();
        HandlerThread handlerThread = new HandlerThread("FUItemHandlerThread");
        handlerThread.start();
        mFuItemHandler = new FUItemHandler(handlerThread.getLooper());
        mFrameId = 0;
        mItemsArray = new int[ITEMS_ARRAY_COUNT];
        synchronized (mLock) {
            mEventQueue.clear();
        }
        /*
         * 创建OpenGL环境，适用于没有 OpenGL 环境时。
         * 如果调用了fuCreateEGLContext，销毁时需要调用fuReleaseEGLContext
         */
        if (mIsCreateEGLContext) {
            faceunity.fuCreateEGLContext();
        }
        mRotationMode = createRotationMode();
        // 设置人脸识别的方向，能够提高首次识别速度
        faceunity.fuSetDefaultRotationMode(mRotationMode);
        // 设置同时识别的人体数量
        faceunity.fuHumanProcessorSetMaxHumans(mMaxHumans);
        // 设置同时识别的人脸数量
        faceunity.fuSetMaxFaces(mMaxFaces);
        // 异步加载图形道具
        if (mRenderMode == RENDER_MODE_CONTROLLER) {
            loadController(null);
        } else {
            for (Effect effect : mEffectList) {
                mFuItemHandler.sendMessage(Message.obtain(mFuItemHandler, FUItemHandler.MSG_WHAT_LOAD_BUNDLE, effect));
            }
        }
    }

    public void loadController(final Runnable callback) {
        mFuItemHandler.post(new Runnable() {
            @Override
            public void run() {
                int[] defaultItems = new int[3];
                final int controllerItem = loadItem(mContext, ASSETS_DIR_GRAPHICS + "controller.bundle");
                if (controllerItem <= 0) {
                    return;
                }
                final int controllerConfigItem = loadItem(mContext, ASSETS_DIR_PTA + "controller_config.bundle");
                defaultItems[0] = controllerConfigItem;
                if (controllerConfigItem > 0) {
                    queueEvent(new Runnable() {
                        @Override
                        public void run() {
                            faceunity.fuBindItems(controllerItem, new int[]{controllerConfigItem});
                            Log.d(TAG, "run: controller bind config");
                        }
                    });
                }
                final int bgItem = loadItem(mContext, ASSETS_DIR_PTA + "default_bg.bundle");
                defaultItems[1] = bgItem;
                if (bgItem > 0) {
                    queueEvent(new Runnable() {
                        @Override
                        public void run() {
                            faceunity.fuBindItems(controllerItem, new int[]{bgItem});
                            Log.d(TAG, "run: controller bind default bg");
                        }
                    });
                }
                final int fakeManItem = loadItem(mContext, ASSETS_DIR_PTA + "fakeman.bundle");
                defaultItems[2] = fakeManItem;
                if (fakeManItem > 0) {
                    queueEvent(new Runnable() {
                        @Override
                        public void run() {
                            faceunity.fuBindItems(controllerItem, new int[]{fakeManItem});
                            Log.d(TAG, "run: controller bind fake man");
                        }
                    });
                }
                final int fxaaItem = loadItem(mContext, ASSETS_DIR_GRAPHICS + "fxaa.bundle");
                int[] gestureItems = new int[GESTURE_BIND_BUNDLES.length];
                for (int i = 0; i < GESTURE_BIND_BUNDLES.length; i++) {
                    int item = loadItem(mContext, ASSETS_DIR_PTA + "gesture/" + GESTURE_BIND_BUNDLES[i]);
                    gestureItems[i] = item;
                }
                final int[] validGestureItems = validateItems(gestureItems);
                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        faceunity.fuBindItems(controllerItem, validGestureItems);
                        Log.d(TAG, "run: controller bind gesture");
                    }
                });
                int[] validDefaultItems = validateItems(defaultItems);
                int[] controllerBoundItems = new int[validDefaultItems.length + validGestureItems.length];
                System.arraycopy(validDefaultItems, 0, controllerBoundItems, 0, validDefaultItems.length);
                System.arraycopy(validGestureItems, 0, controllerBoundItems, validDefaultItems.length, validGestureItems.length);
                mControllerBoundItems = controllerBoundItems;
                Log.d(TAG, "run: controller all bind item " + Arrays.toString(controllerBoundItems));
                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        for (int item : mItemsArray) {
                            if (item > 0) {
                                faceunity.fuDestroyItem(item);
                            }
                        }
                        Arrays.fill(mItemsArray, 0);
                        // 关闭CNN面部追踪
                        faceunity.fuItemSetParam(controllerItem, "close_face_capture", 1.0);
                        // 关闭 DDE
                        faceunity.fuItemSetParam(controllerItem, "is_close_dde", 1.0);
                        // 开启身体追踪
                        faceunity.fuItemSetParam(controllerItem, "use_human_processor", 1.0);
                        // 进入身体追踪模式
                        faceunity.fuItemSetParam(controllerItem, "enter_human_pose_track_mode", 1.0);
                        int[] items = new int[2];
                        items[0] = controllerItem;
                        items[1] = fxaaItem;
                        mItemsArray = items;
                        setHumanTrackScene(mHumanTrackScene);
                        mRotationMode = faceunity.fuGetCurrentRotationMode();
                        faceunity.fuSetDefaultRotationMode(0);
                        mRenderMode = RENDER_MODE_CONTROLLER;
                        resetTrackStatus();
                        if (callback != null) {
                            callback.run();
                        }
                    }
                });
            }
        });
    }

    public void destroyController(final Runnable callback) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                destroyControllerRelated();
                for (int item : mItemsArray) {
                    if (item > 0) {
                        faceunity.fuDestroyItem(item);
                    }
                }
                Arrays.fill(mItemsArray, 0);
                faceunity.fuSetDefaultRotationMode(mRotationMode);
                mRenderMode = RENDER_MODE_NORMAL;
                resetTrackStatus();
                if (callback != null) {
                    callback.run();
                }
            }
        });
    }

    public void setHumanTrackScene(final int humanTrackScene) {
        Log.d(TAG, "setHumanTrackScene() called with: humanTrackScene = [" + humanTrackScene + "]");
        mHumanTrackScene = humanTrackScene;
        queueEvent(new Runnable() {
            @Override
            public void run() {
                int item = mItemsArray[0];
                if (item > 0) {
                    boolean isFullScene = humanTrackScene == HUMAN_TRACK_SCENE_FULL;
                    if (isFullScene) {
                        faceunity.fuItemSetParam(item, "target_position", new double[]{0.0, 53.14, -537.94});
                        faceunity.fuItemSetParam(item, "target_angle", 0.0);
                        faceunity.fuItemSetParam(item, "reset_all", 3.0);
                    } else {
                        faceunity.fuItemSetParam(item, "target_position", new double[]{0.0, 11.76, -183.89});
                        faceunity.fuItemSetParam(item, "target_angle", 0);
                        faceunity.fuItemSetParam(item, "reset_all", 6);
                    }
                    faceunity.fuItemSetParam(item, "human_3d_track_set_scene", humanTrackScene);
                }
            }
        });
    }

    private int[] validateItems(int[] input) {
        int[] output = new int[input.length];
        int count = 0;
        for (int i : input) {
            if (i > 0) {
                output[count++] = i;
            }
        }
        return Arrays.copyOfRange(output, 0, count);
    }

    private void rotateImage(byte[] img, int width, int height) {
        boolean isFrontCam = mCameraType == Camera.CameraInfo.CAMERA_FACING_FRONT;
        //旋转角度
        int rotateMode = isFrontCam ? faceunity.FU_ROTATION_MODE_270 : faceunity.FU_ROTATION_MODE_90;
        //是否x镜像
        int flipX = isFrontCam ? 1 : 0;
        //是否y镜像
        int flipY = 0;
        faceunity.fuRotateImage(mRotatedImage, img, faceunity.FU_FORMAT_NV21_BUFFER, width, height, rotateMode, flipX, flipY);
        faceunity.fuSetInputCameraMatrix(flipX, flipY, rotateMode);
    }

    public int drawFrame(byte[] img, int tex, int w, int h) {
        if (img == null || tex <= 0 || w <= 0 || h <= 0) {
            Log.e(TAG, "onDrawFrame data is invalid");
            return 0;
        }
        prepareDrawFrame();
        if (mIsRunBenchmark) {
            mCallStartTime = System.nanoTime();
        }
        int fuTex;
        boolean renderNormal = mRenderMode == RENDER_MODE_NORMAL;
        if (renderNormal) {
            int flags = createFlags();
            flags ^= mInputTextureType;
            fuTex = faceunity.fuRenderToNV21Image(img, w, h, mFrameId++, mItemsArray, flags);
        } else {
            rotateImage(img, w, h);
            fuTex = faceunity.fuRenderBundlesWithCamera(mRotatedImage.mData, tex, faceunity.FU_ADM_FLAG_EXTERNAL_OES_TEXTURE,
                    mRotatedImage.mWidth, mRotatedImage.mHeight, mFrameId++, mItemsArray);
        }
        if (mIsRunBenchmark) {
            mSumRenderTime += System.nanoTime() - mCallStartTime;
        }
        return fuTex;
    }

    public static class FuAiGestureType {
        public static final int FUAIGESTURE_NO_HAND = -1;
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
     * 人体手势查询与匹配
     *
     * @return
     */
    public int matchHumanGesture() {
        if (faceunity.fuHandDetectorGetResultNumHands() > 0) {
            return faceunity.fuHandDetectorGetResultGestureType(0);
        }
        return FuAiGestureType.FUAIGESTURE_NO_HAND;
    }

    /**
     * 人体动作检测
     *
     * @param img
     * @param width
     * @param height
     */
    public void trackHumanAction(byte[] img, int width, int height, int format) {
        track(img, width, height, faceunity.FUAITYPE_HUMAN_PROCESSOR_2D_DANCE, format);
    }

    /**
     * 人体手势检测
     *
     * @param img
     * @param width
     * @param height
     */
    public void trackHumanGesture(byte[] img, int width, int height, int format) {
        track(img, width, height, faceunity.FUAITYPE_HANDGESTURE, format);
    }

    /**
     * 人体、人脸、手势检测
     *
     * @param img
     * @param width
     * @param height
     * @param aiType
     * @param format
     */
    public void track(byte[] img, int width, int height, int aiType, int format) {
        faceunity.fuSetTrackFaceAIType(aiType);
        faceunity.fuTrackFace(img, width, height, format);
        callTrackStatus();
    }

    /**
     * 人体动作查询与匹配
     *
     * @return
     */
    public int matchHumanAction() {
        if (faceunity.fuHumanProcessorGetNumResults() > 0) {
            return faceunity.fuHumanProcessorGetResultActionType(0);
        }
        return -1;
    }

    /**
     * 单 texture 输入接口，必须在具有 GL 环境的线程调用
     *
     * @param tex
     * @param w
     * @param h
     * @return
     */
    public int onDrawFrameSingleInput(int tex, int w, int h) {
        if (tex <= 0 || w <= 0 || h <= 0) {
            Log.e(TAG, "onDrawFrame data is invalid");
            return 0;
        }
        prepareDrawFrame();
        int flags = createFlags();
        if (mIsRunBenchmark) {
            mCallStartTime = System.nanoTime();
        }
        int fuTex = faceunity.fuRenderToTexture(tex, w, h, mFrameId++, mItemsArray, flags);
        if (mIsRunBenchmark) {
            mSumRenderTime += System.nanoTime() - mCallStartTime;
        }
        return fuTex;
    }

    /**
     * 单 texture 输入接口，支持数据回写，必须在具有 GL 环境的线程调用
     *
     * @param tex
     * @param w
     * @param h
     * @param readBackImg    数据回写到的 buffer
     * @param readBackW
     * @param readBackH
     * @param readBackFormat buffer 格式: nv21, i420, rgba
     * @return
     */
    public int onDrawFrameSingleInput(int tex, int w, int h, byte[] readBackImg, int readBackW, int readBackH, int readBackFormat) {
        if (tex <= 0 || w <= 0 || h <= 0 || readBackImg == null || readBackW <= 0 || readBackH <= 0) {
            Log.e(TAG, "onDrawFrame data is invalid");
            return 0;
        }
        prepareDrawFrame();
        int flags = createFlags();
        if (mIsRunBenchmark) {
            mCallStartTime = System.nanoTime();
        }
        switch (readBackFormat) {
            case INPUT_FORMAT_I420:
                flags |= faceunity.FU_ADM_FLAG_I420_TEXTURE;
                break;
            case INPUT_FORMAT_RGBA:
                flags |= faceunity.FU_ADM_FLAG_RGBA_BUFFER;
                break;
            case INPUT_FORMAT_NV21:
            default:
                flags |= faceunity.FU_ADM_FLAG_NV21_TEXTURE;
                break;
        }

        int fuTex = faceunity.fuRenderToTexture(tex, w, h, mFrameId++, mItemsArray, flags, readBackImg, readBackW, readBackH);
        if (mIsRunBenchmark) {
            mSumRenderTime += System.nanoTime() - mCallStartTime;
        }
        return fuTex;
    }

    /**
     * 单 buffer 输入接口，必须在具有 GL 环境的线程调用
     *
     * @param img
     * @param w
     * @param h
     * @param format buffer 格式: nv21, i420, rgba
     * @return
     */
    public int onDrawFrameSingleInput(byte[] img, int w, int h, int format) {
        if (img == null || w <= 0 || h <= 0) {
            Log.e(TAG, "onDrawFrame data is invalid");
            return 0;
        }
        prepareDrawFrame();
        int flags = createFlags();
        flags ^= mInputTextureType;
        if (mIsRunBenchmark) {
            mCallStartTime = System.nanoTime();
        }
        int fuTex;
        switch (format) {
            case INPUT_FORMAT_I420:
                fuTex = faceunity.fuRenderToI420Image(img, w, h, mFrameId++, mItemsArray, flags);
                break;
            case INPUT_FORMAT_RGBA:
                fuTex = faceunity.fuRenderToRgbaImage(img, w, h, mFrameId++, mItemsArray, flags);
                break;
            case INPUT_FORMAT_NV21:
            default:
                fuTex = faceunity.fuRenderToNV21Image(img, w, h, mFrameId++, mItemsArray, flags);
                break;
        }
        if (mIsRunBenchmark) {
            mSumRenderTime += System.nanoTime() - mCallStartTime;
        }
        return fuTex;
    }

    /**
     * 单 buffer 输入接口，支持数据回写，必须在具有 GL 环境的线程调用
     *
     * @param img
     * @param w
     * @param h
     * @param readBackImg 数据回写到的 buffer
     * @param readBackW
     * @param readBackH
     * @param format      buffer 格式: nv21, i420, rgba
     * @return
     */
    public int onDrawFrameSingleInput(byte[] img, int w, int h, byte[] readBackImg, int readBackW, int readBackH, int format) {
        if (img == null || w <= 0 || h <= 0 || readBackImg == null || readBackW <= 0 || readBackH <= 0) {
            Log.e(TAG, "onDrawFrame data is invalid");
            return 0;
        }
        prepareDrawFrame();
        int flags = createFlags();
        flags ^= mInputTextureType;
        if (mIsRunBenchmark) {
            mCallStartTime = System.nanoTime();
        }
        int fuTex;
        switch (format) {
            case INPUT_FORMAT_I420:
                fuTex = faceunity.fuRenderToI420Image(img, w, h, mFrameId++, mItemsArray, flags,
                        readBackW, readBackH, readBackImg);
                break;
            case INPUT_FORMAT_RGBA:
                fuTex = faceunity.fuRenderToRgbaImage(img, w, h, mFrameId++, mItemsArray, flags,
                        readBackW, readBackH, readBackImg);
                break;
            case INPUT_FORMAT_NV21:
            default:
                fuTex = faceunity.fuRenderToNV21Image(img, w, h, mFrameId++, mItemsArray, flags,
                        readBackW, readBackH, readBackImg);
                break;
        }
        if (mIsRunBenchmark) {
            mSumRenderTime += System.nanoTime() - mCallStartTime;
        }
        return fuTex;
    }

    /**
     * 双输入接口，输入 buffer 和 texture，必须在具有 GL 环境的线程调用
     * 由于省去数据拷贝，性能相对最优，优先推荐使用。
     *
     * @param img NV21 数据
     * @param tex 纹理 ID
     * @param w
     * @param h
     * @return
     */
    public int onDrawFrameDualInput(byte[] img, int tex, int w, int h) {
        if (img == null || tex <= 0 || w <= 0 || h <= 0) {
            Log.e(TAG, "onDrawFrame data is invalid");
            return 0;
        }
        prepareDrawFrame();
        int flags = createFlags();
        if (mIsRunBenchmark) {
            mCallStartTime = System.nanoTime();
        }
        int fuTex = faceunity.fuDualInputToTexture(img, tex, flags, w, h, mFrameId++, mItemsArray);
        if (mIsRunBenchmark) {
            mSumRenderTime += System.nanoTime() - mCallStartTime;
        }
        return fuTex;
    }

    /**
     * 双输入接口，输入 buffer 和 texture，支持数据回写到 buffer，必须在具有 GL 环境的线程调用
     *
     * @param img         NV21数据
     * @param tex         纹理 ID
     * @param w
     * @param h
     * @param readBackImg 数据回写到的 buffer
     * @param readBackW
     * @param readBackH
     * @return
     */
    public int onDrawFrameDualInput(byte[] img, int tex, int w, int h, byte[] readBackImg, int readBackW, int readBackH) {
        if (img == null || tex <= 0 || w <= 0 || h <= 0 || readBackImg == null || readBackW <= 0 || readBackH <= 0) {
            Log.e(TAG, "onDrawFrame data is invalid");
            return 0;
        }
        prepareDrawFrame();
        int flags = createFlags();
        if (mIsRunBenchmark) {
            mCallStartTime = System.nanoTime();
        }
        int fuTex = faceunity.fuDualInputToTexture(img, tex, flags, w, h, mFrameId++, mItemsArray,
                readBackW, readBackH, readBackImg);
        if (mIsRunBenchmark) {
            mSumRenderTime += System.nanoTime() - mCallStartTime;
        }
        return fuTex;
    }

    /**
     * 销毁 SDK 相关资源，必须在 GL 线程调用。如果没有 GL 环境，请把 mIsCreateEGLContext 设置为 true。
     */
    public void onSurfaceDestroyed() {
        Log.e(TAG, "onSurfaceDestroyed");
        if (mFuItemHandler != null) {
            mFuItemHandler.removeCallbacksAndMessages(null);
            mFuItemHandler.getLooper().quit();
        }
        mFrameId = 0;
        synchronized (mLock) {
            mEventQueue.clear();
        }

        destroyControllerRelated();
        for (int item : mItemsArray) {
            if (item > 0) {
                faceunity.fuDestroyItem(item);
            }
        }
        Arrays.fill(mItemsArray, 0);
        faceunity.fuOnCameraChange();
        faceunity.fuDestroyAllItems();
        faceunity.fuOnDeviceLost();
        faceunity.fuDone();
        if (mIsCreateEGLContext) {
            faceunity.fuReleaseEGLContext();
        }
    }

    private void destroyControllerRelated() {
        if (mControllerBoundItems != null && mControllerBoundItems[0] > 0) {
            int controllerItem = mItemsArray[0];
            faceunity.fuItemSetParam(controllerItem, "quit_human_pose_track_mode", 1.0);
            int[] controllerBoundItems = validateItems(mControllerBoundItems);
            Log.d(TAG, "destroyControllerRelated: unbind " + Arrays.toString(controllerBoundItems));
            faceunity.fuUnBindItems(controllerItem, controllerBoundItems);
            for (int i = controllerBoundItems.length - 1; i >= 0; i--) {
                int ptaItem = controllerBoundItems[i];
                if (ptaItem > 0) {
                    faceunity.fuDestroyItem(ptaItem);
                }
            }
            Arrays.fill(controllerBoundItems, 0);
            mControllerBoundItems = null;
        }
    }

    /**
     * 每帧处理画面时被调用
     */
    private void prepareDrawFrame() {
        // 计算 FPS 和渲染时长
        benchmarkFPS();
        callTrackStatus();
        // 获取内部错误信息，并调用回调接口
        int errorCode = faceunity.fuGetSystemError();
        if (errorCode != 0) {
            String errorMessage = faceunity.fuGetSystemErrorString(errorCode);
            Log.e(TAG, "system error code: " + errorCode + ", error message: " + errorMessage);
            if (mOnSystemErrorListener != null) {
                mOnSystemErrorListener.onSystemError(errorMessage);
            }
        }

        synchronized (mLock) {
            while (!mEventQueue.isEmpty()) {
                mEventQueue.remove(0).run();
            }
        }
    }

    private void callTrackStatus() {
        // 获取人脸是否识别
        int trackFace = faceunity.fuIsTracking();
        // 获取人体是否识别
        int trackHumans = faceunity.fuHumanProcessorGetNumResults();
        // 获取手势是否识别
        int trackGesture = faceunity.fuHandDetectorGetResultNumHands();
        if (mOnTrackStatusChangedListener != null) {
            if (mTrackFaceStatus != trackFace) {
                mTrackFaceStatus = trackFace;
                mOnTrackStatusChangedListener.onTrackStatusChanged(TRACK_TYPE_FACE, trackFace);
            }
            if (mTrackHumanStatus != trackHumans) {
                mTrackHumanStatus = trackHumans;
                mOnTrackStatusChangedListener.onTrackStatusChanged(TRACK_TYPE_HUMAN, trackHumans);
            }
            if (mTrackGestureStatus != trackGesture) {
                mTrackGestureStatus = trackGesture;
                mOnTrackStatusChangedListener.onTrackStatusChanged(TRACK_TYPE_GESTURE, trackGesture);
            }
        }
    }

    /**
     * 类似 GLSurfaceView 的 queueEvent 机制
     *
     * @param r
     */
    public void queueEvent(Runnable r) {
        if (r != null) {
            if (mGlThreadId == Thread.currentThread().getId()) {
                r.run();
            } else {
                synchronized (mLock) {
                    mEventQueue.add(r);
                }
            }
        }
    }

    /**
     * 设备方向发生变化时调用
     *
     * @param deviceOrientation home 下 90，home 右 0，home 上 270，home 左 180
     */
    public void onDeviceOrientationChanged(final int deviceOrientation) {
        if (mDeviceOrientation == deviceOrientation) {
            return;
        }
        Log.d(TAG, "onDeviceOrientationChanged() called with: deviceOrientation = [" + deviceOrientation + "]");
        mDeviceOrientation = deviceOrientation;
        callWhenDeviceChanged();
    }

    /**
     * 相机切换时需要调用
     *
     * @param cameraType            前后置相机 ID
     * @param inputImageOrientation 相机方向
     */
    public void onCameraChanged(final int cameraType, final int inputImageOrientation) {
        if (mCameraType == cameraType && mInputImageOrientation == inputImageOrientation) {
            return;
        }
        Log.d(TAG, "onCameraChanged() called with: cameraType = [" + cameraType
                + "], inputImageOrientation = [" + inputImageOrientation + "]");
        mCameraType = cameraType;
        mInputImageOrientation = inputImageOrientation;
        queueEvent(new Runnable() {
            @Override
            public void run() {
                faceunity.fuHumanProcessorReset();
                faceunity.fuOnCameraChange();
            }
        });
        callWhenDeviceChanged();
    }

    private void callWhenDeviceChanged() {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mRotationMode = createRotationMode();
                int rotationMode = 0;
                if (mRenderMode == RENDER_MODE_NORMAL) {
                    rotationMode = mRotationMode;
                }
                faceunity.fuSetDefaultRotationMode(rotationMode);
            }
        });
    }

    private int createRotationMode() {
        if (mInputTextureType == FURenderer.INPUT_2D_TEXTURE) {
            return faceunity.FU_ROTATION_MODE_0;
        }
        int rotMode = faceunity.FU_ROTATION_MODE_0;
        if (mInputImageOrientation == 270) {
            if (mCameraType == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                rotMode = mDeviceOrientation / 90;
            } else {
                if (mDeviceOrientation == 90) {
                    rotMode = faceunity.FU_ROTATION_MODE_270;
                } else if (mDeviceOrientation == 270) {
                    rotMode = faceunity.FU_ROTATION_MODE_90;
                } else {
                    rotMode = mDeviceOrientation / 90;
                }
            }
        } else if (mInputImageOrientation == 90) {
            if (mCameraType == Camera.CameraInfo.CAMERA_FACING_BACK) {
                if (mDeviceOrientation == 90) {
                    rotMode = faceunity.FU_ROTATION_MODE_270;
                } else if (mDeviceOrientation == 270) {
                    rotMode = faceunity.FU_ROTATION_MODE_90;
                } else {
                    rotMode = mDeviceOrientation / 90;
                }
            } else {
                if (mDeviceOrientation == 0) {
                    rotMode = faceunity.FU_ROTATION_MODE_180;
                } else if (mDeviceOrientation == 90) {
                    rotMode = faceunity.FU_ROTATION_MODE_270;
                } else if (mDeviceOrientation == 180) {
                    rotMode = faceunity.FU_ROTATION_MODE_0;
                } else {
                    rotMode = faceunity.FU_ROTATION_MODE_90;
                }
            }
        }
        return rotMode;
    }

    private int createFlags() {
        int flags = mInputTextureType | mInputImageFormat;
        if (mInputTextureType == INPUT_2D_TEXTURE || mCameraType != Camera.CameraInfo.CAMERA_FACING_FRONT) {
            flags |= faceunity.FU_ADM_FLAG_FLIP_X;
        }
        return flags;
    }

    public void selectEffect(final Effect effect) {
        if (effect == null) {
            return;
        }
        if (!mEffectList.add(effect)) {
            return;
        }
        Log.d(TAG, "selectEffect: " + effect);
        mFuItemHandler.sendMessage(Message.obtain(mFuItemHandler, FUItemHandler.MSG_WHAT_LOAD_BUNDLE, effect));
    }

    public void unselectEffect(final Effect effect) {
        if (effect == null) {
            return;
        }
        if (!mEffectList.remove(effect)) {
            return;
        }
        final int handle = effect.getHandle();
        if (handle <= 0) {
            return;
        }
        Log.d(TAG, "unselectEffect: " + effect);
        queueEvent(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < mItemsArray.length; i++) {
                    if (mItemsArray[i] == handle) {
                        faceunity.fuDestroyItem(handle);
                        mItemsArray[i] = 0;
                        Log.d(TAG, "destroy item handle:" + handle);
                        break;
                    }
                }
                effect.setHandle(0);
                int newLength = 0;
                for (int value : mItemsArray) {
                    if (value > 0) {
                        newLength++;
                    }
                }
                if (newLength == 0) {
                    newLength = ITEMS_ARRAY_COUNT;
                }
                int[] trimmedItems = new int[newLength];
                for (int i = 0, j = 0; i < mItemsArray.length; i++) {
                    if (mItemsArray[i] > 0) {
                        trimmedItems[j++] = mItemsArray[i];
                    }
                }
                mItemsArray = trimmedItems;
                resetTrackStatus();
                Log.i(TAG, "new item array:" + Arrays.toString(trimmedItems));
            }
        });
    }

    public void resetTrackStatus() {
        Log.d(TAG, "resetTrackStatus: ");
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mTrackFaceStatus = -1;
                mTrackHumanStatus = -1;
                mTrackGestureStatus = -1;
            }
        });
    }

    //-----------------------------人脸识别回调相关定义-----------------------------------

    private int mTrackHumanStatus = -1;
    private int mTrackFaceStatus = -1;
    private int mTrackGestureStatus = -1;

    public interface OnTrackStatusChangedListener {
        /**
         * 检测状态发生变化
         *
         * @param type
         * @param status
         */
        void onTrackStatusChanged(int type, int status);
    }

    private OnTrackStatusChangedListener mOnTrackStatusChangedListener;

    //-------------------------错误信息回调相关定义---------------------------------

    public interface OnSystemErrorListener {
        /**
         * SDK 发生错误时调用
         *
         * @param error 错误消息
         */
        void onSystemError(String error);
    }

    private OnSystemErrorListener mOnSystemErrorListener;

    //------------------------------FPS 渲染时长回调相关定义------------------------------------

    private static final int NANO_IN_ONE_MILLI_SECOND = 1_000_000;
    private static final int NANO_IN_ONE_SECOND = 1_000_000_000;
    private static final int FRAME_COUNT = 10;
    private boolean mIsRunBenchmark = false;
    private int mCurrentFrameCount;
    private long mLastFrameTimestamp;
    private long mSumRenderTime;
    private long mCallStartTime;
    private OnDebugListener mOnDebugListener;

    public interface OnDebugListener {
        /**
         * 统计每 10 帧的平均值，FPS 和渲染时间
         *
         * @param fps        FPS
         * @param renderTime 渲染时间
         * @param elapsedTime 帧间耗时
         */
        void onFpsChanged(double fps, double renderTime, double elapsedTime);
    }

    private void benchmarkFPS() {
        if (!mIsRunBenchmark) {
            return;
        }
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

    //--------------------------------------IO handler 线程异步加载道具-------------------------------------

    private class FUItemHandler extends Handler {
        private static final int MSG_WHAT_LOAD_BUNDLE = 666;

        FUItemHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == MSG_WHAT_LOAD_BUNDLE) {
                final Effect effect = (Effect) msg.obj;
                if (effect == null) {
                    return;
                }
                final int itemEffect = loadItem(mContext, effect.getFilePath());
                effect.setHandle(itemEffect);
                if (itemEffect <= 0) {
                    return;
                }
                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        Map<String, Object> params = effect.getParams();
                        if (params != null && params.size() > 0) {
                            Set<Map.Entry<String, Object>> entries = params.entrySet();
                            for (Map.Entry<String, Object> entry : entries) {
                                Object value = entry.getValue();
                                String key = entry.getKey();
                                if (value instanceof Double) {
                                    faceunity.fuItemSetParam(itemEffect, key, (Double) value);
                                } else if (value instanceof String) {
                                    faceunity.fuItemSetParam(itemEffect, key, (String) value);
                                } else if (value instanceof double[]) {
                                    faceunity.fuItemSetParam(itemEffect, key, (double[]) value);
                                }
                                Log.v(TAG, "fuItemSetParam. handle:" + itemEffect + ", key:" + key + ", value:" + value);
                            }
                        }

                        if (mItemsArray[0] > 0) {
                            int newLength = 0;
                            for (int value : mItemsArray) {
                                if (value > 0) {
                                    newLength++;
                                }
                            }
                            int[] enlargedItems = new int[newLength + 1];
                            int length = mItemsArray.length;
                            for (int i = 0, j = 0; i < length; i++) {
                                if (mItemsArray[i] > 0) {
                                    enlargedItems[j++] = mItemsArray[i];
                                }
                            }
                            enlargedItems[newLength] = itemEffect;
                            mItemsArray = enlargedItems;
                        } else {
                            mItemsArray[0] = itemEffect;
                        }
                        resetTrackStatus();
                        Log.i(TAG, "new item array:" + Arrays.toString(mItemsArray));
                    }
                });
            }
        }
    }

    //--------------------------------------Builder----------------------------------------

    /**
     * FURenderer Builder
     */
    public static class Builder {
        private Context context;
        private boolean isCreateEGLContext = false;
        private int maxHumans = 1;
        private int maxFaces = 1;
        private int deviceOrientation = 90;
        private int inputTextureType = INPUT_2D_TEXTURE;
        private int inputImageFormat = 0;
        private int inputImageOrientation = 270;
        private int cameraType = Camera.CameraInfo.CAMERA_FACING_FRONT;
        private boolean isRunBenchmark = false;
        private OnDebugListener onDebugListener;
        private OnTrackStatusChangedListener onTrackStatusChangedListener;
        private OnSystemErrorListener onSystemErrorListener;

        public Builder(Context context) {
            this.context = context.getApplicationContext();
        }

        /**
         * 是否手动创建 EGLContext
         *
         * @param isCreateEGLContext
         * @return
         */

        public Builder setCreateEGLContext(boolean isCreateEGLContext) {
            this.isCreateEGLContext = isCreateEGLContext;
            return this;
        }

        /**
         * 同时识别的最大人脸数
         *
         * @param maxFaces
         * @return
         */
        public Builder setFaces(int maxFaces) {
            this.maxFaces = maxFaces;
            return this;
        }

        /**
         * 同时识别的最大人体数
         *
         * @param maxHumans
         * @return
         */
        public Builder setHumans(int maxHumans) {
            this.maxHumans = maxHumans;
            return this;
        }

        /**
         * 设备方向
         *
         * @param deviceOrientation
         * @return
         */
        public Builder setDeviceOrientation(int deviceOrientation) {
            this.deviceOrientation = deviceOrientation;
            return this;
        }

        /**
         * 输入图像的纹理类型
         *
         * @param inputTextureType OES 或者 2D
         * @return
         */
        public Builder setInputTextureType(int inputTextureType) {
            this.inputTextureType = inputTextureType;
            return this;
        }

        /**
         * 输入图像的 buffer 类型，一般不用修改此项
         *
         * @param inputImageFormat
         * @return
         */
        public Builder setInputImageFormat(int inputImageFormat) {
            this.inputImageFormat = inputImageFormat;
            return this;
        }

        /**
         * 输入图像的方向
         *
         * @param inputImageOrientation
         * @return
         */
        public Builder setInputImageOrientation(int inputImageOrientation) {
            this.inputImageOrientation = inputImageOrientation;
            return this;
        }

        /**
         * 相机前后方向
         *
         * @param cameraType
         * @return
         */
        public Builder setCameraType(int cameraType) {
            this.cameraType = cameraType;
            return this;
        }

        /**
         * 是否需要 benchmark 统计数据
         *
         * @param isRunBenchmark
         * @return
         */
        public Builder setRunBenchmark(boolean isRunBenchmark) {
            this.isRunBenchmark = isRunBenchmark;
            return this;
        }

        /**
         * FPS 和渲染时长数据回调
         *
         * @param onDebugListener
         * @return
         */
        public Builder setOnDebugListener(OnDebugListener onDebugListener) {
            this.onDebugListener = onDebugListener;
            return this;
        }

        /**
         * 人脸识别状态改变回调
         *
         * @param onTrackStatusChangedListener
         * @return
         */
        public Builder setOnTrackStatusChangedListener(OnTrackStatusChangedListener onTrackStatusChangedListener) {
            this.onTrackStatusChangedListener = onTrackStatusChangedListener;
            return this;
        }

        /**
         * SDK 错误信息回调
         *
         * @param onSystemErrorListener
         * @return
         */
        public Builder setOnSystemErrorListener(OnSystemErrorListener onSystemErrorListener) {
            this.onSystemErrorListener = onSystemErrorListener;
            return this;
        }

        public FURenderer build() {
            FURenderer fuRenderer = new FURenderer(context);
            fuRenderer.mIsCreateEGLContext = isCreateEGLContext;
            fuRenderer.mMaxHumans = maxHumans;
            fuRenderer.mMaxFaces = maxFaces;
            fuRenderer.mDeviceOrientation = deviceOrientation;
            fuRenderer.mInputTextureType = inputTextureType;
            fuRenderer.mInputImageFormat = inputImageFormat;
            fuRenderer.mInputImageOrientation = inputImageOrientation;
            fuRenderer.mCameraType = cameraType;
            fuRenderer.mIsRunBenchmark = isRunBenchmark;
            fuRenderer.mOnDebugListener = onDebugListener;
            fuRenderer.mOnTrackStatusChangedListener = onTrackStatusChangedListener;
            fuRenderer.mOnSystemErrorListener = onSystemErrorListener;

            Log.d(TAG, "FURenderer fields. isCreateEGLContext: " + isCreateEGLContext + ", maxFaces: "
                    + maxFaces + ", maxHumans: " + maxHumans + ", inputTextureType: " + inputTextureType
                    + ", inputImageFormat: " + inputImageFormat + ", inputImageOrientation: " + inputImageOrientation
                    + ", deviceOrientation: " + deviceOrientation + ", cameraType: "
                    + (cameraType == Camera.CameraInfo.CAMERA_FACING_FRONT ? "front" : "back") + ", isRunBenchmark: " + isRunBenchmark);
            return fuRenderer;
        }
    }

}
