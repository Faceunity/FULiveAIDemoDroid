package com.faceunity.fuliveaidemo.activity;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.faceunity.fuliveaidemo.R;
import com.faceunity.fuliveaidemo.renderer.OnRendererListener;
import com.faceunity.fuliveaidemo.util.DensityUtils;
import com.faceunity.fuliveaidemo.util.LifecycleHandler;
import com.faceunity.fuliveaidemo.util.PhotoTaker;
import com.faceunity.fuliveaidemo.util.ToastUtil;
import com.faceunity.fuliveaidemo.view.ConfigFragment;
import com.faceunity.fuliveaidemo.view.OnMultiClickListener;
import com.faceunity.fuliveaidemo.view.adapter.BaseRecyclerAdapter;
import com.faceunity.fuliveaidemo.view.adapter.SpaceItemDecoration;
import com.faceunity.nama.Effect;
import com.faceunity.nama.FURenderer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Richie on 2020.05.21
 */
public abstract class BaseGlActivity extends AppCompatActivity implements PhotoTaker.OnPictureTakeListener,
        OnRendererListener, FURenderer.OnSystemErrorListener {
    private static final String TAG = "BaseGlActivity";
    public static final int RECOGNITION_TYPE_GESTURE = 421;
    public static final int RECOGNITION_TYPE_ACTION = 230;
    public static final int RECOGNITION_TYPE_NONE = -1;
    //    连续三帧检测到才认为成功
    private static final int FACE_DETECT_OK_THRESHOLD = 3;

    private ConfigFragment mConfigFragment;
    private View mViewMask;
    protected TextView mTvTrackStatus;
    private View mIvHeaderMask;
    private View mIvFooterMask;
    private View mTvExpressionTip;
    protected GLSurfaceView mGlSurfaceView;
    protected PhotoTaker mPhotoTaker;
    protected ViewClickListener mViewClickListener;
    protected FURenderer mFURenderer;
    private RecyclerView mRvTongueTrack;
    private TongueTrackAdapter mTongueTrackAdapter;
    private RecyclerView mRvExpression;
    private ExpressionAdapter mExpressionAdapter;
    private RecyclerView mRvRecognition;
    private RecognitionAdapter mRecognitionAdapter;
    private List<RecognitionListData> mActionResourceList;
    private List<RecognitionListData> mGestureResourceList;
    private int mRecognitionIndex = -1;
    private int mRecognitionCount;
    private int mDetectTongueIndex = -1;
    private int mDetectTongueCount;
    private int mRecognitionType = RECOGNITION_TYPE_NONE;
    private boolean mIsDetectTongue;
    private boolean mIsDetectExpression;
    private int[] mDetectExpressionTypes;
    private int[] mDetectExpressionIndexes;
    protected boolean mIsFlipX;
    private LifecycleHandler mLifecycleHandler;
    private Effect mFaceLandmarksEffect;
    private Effect mAiTypeEffect;
    private final Runnable mUpdateHumanRecyclerTask = new Runnable() {
        @Override
        public void run() {
            int gap = 0;
            int skip = 0;
            if (mRecognitionType == RECOGNITION_TYPE_ACTION) {
                gap = 6;
                skip = 1;
            } else if (mRecognitionType == RECOGNITION_TYPE_GESTURE) {
                gap = 5;
                skip = 2;
            }
            if (mRecognitionIndex >= 0) {
                int index = mRecognitionIndex > gap ? mRecognitionIndex + skip : mRecognitionIndex;
                mRecognitionAdapter.setItemSelected(index);
            } else {
                mRecognitionAdapter.clearSingleItemSelected();
            }
        }
    };
    private final Runnable mUpdateExpressionRecyclerTask = new Runnable() {
        @Override
        public void run() {
            mExpressionAdapter.clearMultiItemSelected();
            for (int index : mDetectExpressionIndexes) {
                if (index >= 0) {
                    mExpressionAdapter.setItemSelectedMulti(index);
                }
            }
        }
    };
    private final Runnable mUpdateTongueRecyclerTask = new Runnable() {
        @Override
        public void run() {
            int detectTongueIndex = mDetectTongueIndex;
            if (detectTongueIndex >= 0) {
                mTongueTrackAdapter.setItemSelected(detectTongueIndex);
            } else {
                mTongueTrackAdapter.clearSingleItemSelected();
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawable(null);
        setContentView(R.layout.activity_base);

        mViewClickListener = new ViewClickListener();
        findViewById(R.id.iv_home).setOnClickListener(mViewClickListener);
        findViewById(R.id.iv_config).setOnClickListener(mViewClickListener);
        mTvTrackStatus = findViewById(R.id.tv_track_status);
        mViewMask = findViewById(R.id.v_mask);
        mGlSurfaceView = findViewById(R.id.gl_surface);
        mRvRecognition = findViewById(R.id.rv_recognition);
        mRvRecognition.setTag(false);
        mRvExpression = findViewById(R.id.rv_expression);
        mRvTongueTrack = findViewById(R.id.rv_tongue_track);

        mPhotoTaker = new PhotoTaker();
        mPhotoTaker.setOnPictureTakeListener(this);
        mLifecycleHandler = new LifecycleHandler(getLifecycle());

        initView();
        initGlRenderer();
        initFuRenderer();

        Map<String, Object> paramMap = new HashMap<>(4);
        paramMap.put(ConfigFragment.LANDMARKS_TYPE, FURenderer.FACE_LANDMARKS_239);
        mFaceLandmarksEffect = new Effect(ConfigFragment.FACE_LANDMARKS_BUNDLE_PATH, getString(R.string.config_item_face_landmarks_75), Effect.TYPE_FACE, Effect.MODULE_CODE_FACE_LANDMARKS, paramMap);
        paramMap = new HashMap<>(4);
        paramMap.put("aitype", 1 << 15); // FACEPROCESSOR_EXPRESSION_RECOGNIZER
        mAiTypeEffect = new Effect(ConfigFragment.FACE_EXPRESSION_BUNDLE_PATH, getString(R.string.config_item_face_expression), Effect.TYPE_FACE, Effect.MODULE_CODE_FACE_EXPRESSION, paramMap);
    }

    @Override
    public void onSurfaceCreated() {
        mFURenderer.onSurfaceCreated();
        if (mFaceLandmarksEffect != null) {
            mFURenderer.selectEffect(mFaceLandmarksEffect);
        }
    }

    @Override
    public void onSurfaceChanged(int viewWidth, int viewHeight) {

    }

    @Override
    public void onSurfaceDestroy() {
        mFURenderer.onSurfaceDestroyed();
    }

    @Override
    public void onPictureTakeSucceed(final String path) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ToastUtil.makeText(BaseGlActivity.this, R.string.save_photo_success).show();
                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(path)));
                sendBroadcast(intent);
            }
        });
    }

    @Override
    public void onPictureTakeFailed() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ToastUtil.makeText(BaseGlActivity.this, R.string.save_photo_failure).show();
            }
        });
    }

    @Override
    public void onSystemError(final Effect effect) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ToastUtil.makeText(BaseGlActivity.this, getResources().getString(R.string.toast_invalid_auth)).show();
            }
        });
    }

    /**
     * init view
     */
    protected abstract void initView();

    /**
     * init FURenderer
     */
    protected abstract void initFuRenderer();

    /**
     * init gl Renderer
     */
    protected abstract void initGlRenderer();

    protected void onViewClicked(int id) {
    }

    protected View getRecordView() {
        return null;
    }

    protected void onRenderModeChanged(int renderMode) {
    }

    protected boolean containHumanAvatar() {
        return true;
    }

    /**
     * 1、使用人脸/人体/手势功能时，如未检测到人脸/人体/手势时显示对应提示语；
     * 2、同时使用人脸人体或人脸手势功能时，如同时使用的两个功能都未检测到，而两个不同提示语无法同时显示在屏幕，则优先显示未检测到人脸的提示语。
     */
    protected void queryTrackStatus() {
        int toastStrId = 0;
        boolean invisible = true;
        if (mConfigFragment != null) {
            int selectedTrackType = mConfigFragment.getSelectedTrackType();
            if (selectedTrackType == FURenderer.TRACK_TYPE_HUMAN) {
                boolean hasHuman = mFURenderer.queryHumanTrackStatus() > 0;
                boolean hasFace = mFURenderer.queryFaceTrackStatus() > 0;
                invisible = hasHuman || hasFace;
                if (!invisible) {
                    toastStrId = mConfigFragment.getToastStringId();
                }
                if (hasFace && !hasHuman) {
                    toastStrId = mConfigFragment.getToastStringId();
                    if (toastStrId == R.string.track_status_no_human_full
                            || toastStrId == R.string.track_status_no_human) {
                        invisible = false;
                    }
                }
            } else if (selectedTrackType == FURenderer.TRACK_TYPE_FACE) {
                invisible = mFURenderer.queryFaceTrackStatus() > 0;
                if (!invisible) {
                    toastStrId = R.string.track_status_no_face;
                }
            }
        } else {
            invisible = mFURenderer.queryFaceTrackStatus() > 0;
            if (!invisible) {
                toastStrId = R.string.track_status_no_face;
            }
        }
        final boolean visible = !invisible;
        final int strId = toastStrId;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (visible) {
                    mTvTrackStatus.setVisibility(View.VISIBLE);
                    mTvTrackStatus.setText(strId);
                } else {
                    mTvTrackStatus.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    protected void trackFace() {
        if (mIsDetectTongue) {
            int type = mFURenderer.detectFaceTongue();
            int index = convertToTongueIndex(type, mIsFlipX);
            if (mDetectTongueIndex == index) {
                mDetectTongueCount++;
            } else {
                mDetectTongueCount = 0;
            }
            mDetectTongueIndex = index;
            if (mDetectTongueCount >= FACE_DETECT_OK_THRESHOLD) {
                mLifecycleHandler.post(mUpdateTongueRecyclerTask);
            }
        }

        if (mIsDetectExpression) {
            int[] expressionTypeResult = mDetectExpressionTypes;
            mFURenderer.detectFaceExpression(expressionTypeResult);
            for (int i = 0; i < expressionTypeResult.length; i++) {
                mDetectExpressionIndexes[i] = convertToExpressionIndex(expressionTypeResult[i], mIsFlipX);
            }
            mLifecycleHandler.post(mUpdateExpressionRecyclerTask);
        }
    }

    private static int convertToTongueIndex(int type, boolean isFlipX) {
        switch (type) {
            case FURenderer.FuAiTongueType.FUAITONGUE_UP: {
                return 0;
            }
            case FURenderer.FuAiTongueType.FUAITONGUE_DOWN: {
                return 1;
            }
            case FURenderer.FuAiTongueType.FUAITONGUE_LEFT: {
                return isFlipX ? 3 : 2;
            }
            case FURenderer.FuAiTongueType.FUAITONGUE_RIGHT: {
                return isFlipX ? 2 : 3;
            }
            case FURenderer.FuAiTongueType.FUAITONGUE_LEFT_UP: {
                return isFlipX ? 6 : 4;
            }
            case FURenderer.FuAiTongueType.FUAITONGUE_LEFT_DOWN: {
                return isFlipX ? 7 : 5;
            }
            case FURenderer.FuAiTongueType.FUAITONGUE_RIGHT_UP: {
                return isFlipX ? 4 : 6;
            }
            case FURenderer.FuAiTongueType.FUAITONGUE_RIGHT_DOWN: {
                return isFlipX ? 5 : 7;
            }
            default:
                return -1;
        }
    }

    private static int convertToExpressionIndex(int type, boolean isFlipX) {
        switch (type) {
            case FURenderer.FaceExpressionType.FACE_EXPRESSION_BROW_UP: {
                return 0;
            }
            case FURenderer.FaceExpressionType.FACE_EXPRESSION_BROW_FROWN: {
                return 1;
            }
            case FURenderer.FaceExpressionType.FACE_EXPRESSION_RIGHT_EYE_CLOSE: {
                return isFlipX ? 2 : 3;
            }
            case FURenderer.FaceExpressionType.FACE_EXPRESSION_LEFT_EYE_CLOSE: {
                return isFlipX ? 3 : 2;
            }
            case FURenderer.FaceExpressionType.FACE_EXPRESSION_EYE_WIDE: {
                return 4;
            }
            case FURenderer.FaceExpressionType.FACE_EXPRESSION_MOUTH_SMILE_RIGHT: {
                return isFlipX ? 5 : 6;
            }
            case FURenderer.FaceExpressionType.FACE_EXPRESSION_MOUTH_SMILE_LEFT: {
                return isFlipX ? 6 : 5;
            }
            case FURenderer.FaceExpressionType.FACE_EXPRESSION_MOUTH_SMILE: {
                return 7;
            }
            case FURenderer.FaceExpressionType.FACE_EXPRESSION_MOUTH_FUNNEL: {
                return 8;
            }
            case FURenderer.FaceExpressionType.FACE_EXPRESSION_MOUTH_OPEN: {
                return 9;
            }
            case FURenderer.FaceExpressionType.FACE_EXPRESSION_MOUTH_PUCKER: {
                return 10;
            }
            case FURenderer.FaceExpressionType.FACE_EXPRESSION_MOUTH_ROLL: {
                return 11;
            }
            case FURenderer.FaceExpressionType.FACE_EXPRESSION_MOUTH_PUFF: {
                return 12;
            }
            case FURenderer.FaceExpressionType.FACE_EXPRESSION_MOUTH_FROWN: {
                return 13;
            }
            case FURenderer.FaceExpressionType.FACE_EXPRESSION_HEAD_RIGHT: {
                return isFlipX ? 14 : 15;
            }
            case FURenderer.FaceExpressionType.FACE_EXPRESSION_HEAD_LEFT: {
                return isFlipX ? 15 : 14;
            }
            case FURenderer.FaceExpressionType.FACE_EXPRESSION_HEAD_NOD: {
                return 16;
            }
            default:
                return -1;
        }
    }

    protected void trackHuman() {
        if (mRecognitionType == RECOGNITION_TYPE_NONE) {
            return;
        }
        int index = -1;
        if (mRecognitionType == RECOGNITION_TYPE_ACTION) {
            int type = FURenderer.detectHumanAction();
            index = type;
        } else if (mRecognitionType == RECOGNITION_TYPE_GESTURE) {
            int type = FURenderer.detectHumanGesture();
            index = convertToGestureIndex(type);
        }
        if (mRecognitionIndex == index) {
            mRecognitionCount++;
        } else {
            mRecognitionCount = 0;
        }
        mRecognitionIndex = index;
        // 连续三帧检测到才认为成功
        if (mRecognitionCount >= 3) {
            mLifecycleHandler.post(mUpdateHumanRecyclerTask);
        }
    }

    private static int convertToGestureIndex(int type) {
        switch (type) {
            case FURenderer.FuAiGestureType.FUAIGESTURE_THUMB: {
                return 13;
            }
            case FURenderer.FuAiGestureType.FUAIGESTURE_KORHEART: {
                return 1;
            }
            case FURenderer.FuAiGestureType.FUAIGESTURE_SIX: {
                return 10;
            }
            case FURenderer.FuAiGestureType.FUAIGESTURE_FIST: {
                return 11;
            }
            case FURenderer.FuAiGestureType.FUAIGESTURE_PALM: {
                return 9;
            }
            case FURenderer.FuAiGestureType.FUAIGESTURE_ONE: {
                return 6;
            }
            case FURenderer.FuAiGestureType.FUAIGESTURE_TWO: {
                return 7;
            }
            case FURenderer.FuAiGestureType.FUAIGESTURE_OK: {
                return 8;
            }
            case FURenderer.FuAiGestureType.FUAIGESTURE_ROCK: {
                return 0;
            }
            case FURenderer.FuAiGestureType.FUAIGESTURE_HOLD: {
                return 12;
            }
            case FURenderer.FuAiGestureType.FUAIGESTURE_GREET: {
                return 3;
            }
            case FURenderer.FuAiGestureType.FUAIGESTURE_HEART: {
                return 2;
            }
            case FURenderer.FuAiGestureType.FUAIGESTURE_PHOTO: {
                return 5;
            }
            case FURenderer.FuAiGestureType.FUAIGESTURE_MERGE: {
                return 4;
            }
            default:
                return -1;
        }
    }

    public void setTongueTrackRecyclerVisibility(boolean visible) {
        mRvTongueTrack.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (mTongueTrackAdapter == null) {
            List<Integer> list = initTongueList();
            mTongueTrackAdapter = new TongueTrackAdapter(new ArrayList<>(list));
            mTongueTrackAdapter.setCanManualClick(false);
            ((SimpleItemAnimator) mRvTongueTrack.getItemAnimator()).setSupportsChangeAnimations(false);
            mRvTongueTrack.setHasFixedSize(true);
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
            mRvTongueTrack.setLayoutManager(layoutManager);
            mRvTongueTrack.setAdapter(mTongueTrackAdapter);
        }
        mIsDetectTongue = visible;
    }

    public void setExpressionRecyclerVisibility(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;
        mRvExpression.setVisibility(visibility);
        if (mExpressionAdapter == null) {
            List<Integer> list = initExpressionList();
            mExpressionAdapter = new ExpressionAdapter(new ArrayList<>(list));
            mExpressionAdapter.setCanManualClick(false);
            mRvExpression.setHasFixedSize(true);
            mIvHeaderMask = findViewById(R.id.iv_rv_expression_header_mask);
            mIvFooterMask = findViewById(R.id.iv_rv_expression_footer_mask);
            mTvExpressionTip = findViewById(R.id.tv_expression_tip);
            ((SimpleItemAnimator) mRvExpression.getItemAnimator()).setSupportsChangeAnimations(false);
            LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
            mRvExpression.setLayoutManager(layoutManager);
            mRvExpression.setAdapter(mExpressionAdapter);
            mRvExpression.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    boolean canScrollFoot = recyclerView.canScrollVertically(1);
                    boolean canScrollHead = recyclerView.canScrollVertically(-1);
                    mIvHeaderMask.setVisibility(canScrollHead ? View.VISIBLE : View.INVISIBLE);
                    mIvFooterMask.setVisibility(canScrollFoot ? View.VISIBLE : View.INVISIBLE);
                }
            });
            int size = list.size();
            mDetectExpressionTypes = new int[size];
            mDetectExpressionIndexes = new int[size];
        }
        mIvHeaderMask.setVisibility(visibility);
        mIvFooterMask.setVisibility(visibility);
        mIsDetectExpression = visible;
    }

    public void setRecognitionRecyclerVisibility(int resourceListType, boolean isSelected) {
        mFURenderer.resetTrackStatus();
        if (isSelected) {
            mRecognitionType = resourceListType;
        } else {
            mRecognitionType = RECOGNITION_TYPE_NONE;
        }
        View recordView = getRecordView();
        if (recordView != null) {
            ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) recordView.getLayoutParams();
            int dpSize = isSelected ? 134 : 84;
            layoutParams.bottomMargin = isSelected ? DensityUtils.dp2px(this, dpSize) : DensityUtils.dp2px(this, dpSize);
            recordView.setLayoutParams(layoutParams);
        }

        mRvRecognition.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        if (!isSelected) {
            return;
        }
        List<RecognitionListData> dataSet;
        if (resourceListType == RECOGNITION_TYPE_GESTURE) {
            if (mGestureResourceList == null) {
                mGestureResourceList = initGestureList();
            }
            dataSet = mGestureResourceList;
        } else if (resourceListType == RECOGNITION_TYPE_ACTION) {
            if (mActionResourceList == null) {
                mActionResourceList = initActionList();
            }
            dataSet = mActionResourceList;
        } else {
            dataSet = new ArrayList<>();
        }
        if (mRecognitionAdapter == null) {
            mRvRecognition.setHasFixedSize(true);
            mRvRecognition.setLayoutManager(new GridLayoutManager(BaseGlActivity.this, 8));
            ((SimpleItemAnimator) mRvRecognition.getItemAnimator()).setSupportsChangeAnimations(false);
            int spacePx = DensityUtils.dp2px(BaseGlActivity.this, 1.5f);
            mRvRecognition.addItemDecoration(new SpaceItemDecoration(spacePx, spacePx));
            mRecognitionAdapter = new RecognitionAdapter(new ArrayList<>(dataSet));
            mRecognitionAdapter.setCanManualClick(false);
            mRvRecognition.setAdapter(mRecognitionAdapter);
        } else {
            mRecognitionAdapter.replaceAll(dataSet);
        }
    }

    public void setMaskVisibility(boolean visible) {
        int duration = getResources().getInteger(android.R.integer.config_mediumAnimTime);
        float alpha = visible ? 1F : 0F;
        mViewMask.animate().alpha(alpha).setDuration(duration).start();
    }

    public void setRenderMode(final int renderMode) {
        final Dialog dialog = createLoadingDialog();
        FURenderer.Callback callback = new FURenderer.Callback() {
            @Override
            public void onSuccess() {
                onRenderModeChanged(renderMode);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                    }
                });
            }

            @Override
            public void onFailure() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        ToastUtil.makeText(BaseGlActivity.this, getResources().getString(R.string.toast_invalid_auth)).show();
                    }
                });
            }
        };
        if (renderMode == FURenderer.RENDER_MODE_NORMAL) {
            mFURenderer.destroyController(callback);
        } else if (renderMode == FURenderer.RENDER_MODE_CONTROLLER) {
            dialog.show();
            mFURenderer.loadController(callback);
        }
    }

    public Dialog createLoadingDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null);
        Dialog dialog = new Dialog(this);
        dialog.setCancelable(false);
        dialog.setContentView(view, new ViewGroup.LayoutParams(
                DensityUtils.dp2px(this, 110), DensityUtils.dp2px(this, 110)));
        dialog.getWindow().setBackgroundDrawable(null);
        return dialog;
    }

    public FURenderer getFuRenderer() {
        return mFURenderer;
    }

    public Effect getFaceLandmarksEffect() {
        return mFaceLandmarksEffect;
    }

    public void nullFaceLandmarksEffect() {
        mFaceLandmarksEffect = null;
    }

    public Effect getAiTypeEffect() {
        return mAiTypeEffect;
    }

    private List<RecognitionListData> initGestureList() {
        List<RecognitionListData> gestureResourceList = new ArrayList<>(16);
        gestureResourceList.add(new RecognitionListData(R.drawable.demo_gesture_icon_love));
        gestureResourceList.add(new RecognitionListData(R.drawable.demo_gesture_icon_one_handed));
        gestureResourceList.add(new RecognitionListData(R.drawable.demo_gesture_icon_hands));
        gestureResourceList.add(new RecognitionListData(R.drawable.demo_gesture_icon_clenched_fist));
        gestureResourceList.add(new RecognitionListData(R.drawable.demo_gesture_icon_hands_together));
        gestureResourceList.add(new RecognitionListData(R.drawable.demo_gesture_icon_hands_take_pictures));
        gestureResourceList.add(new RecognitionListData(R.drawable.demo_action_icon_transparent, true));
        gestureResourceList.add(new RecognitionListData(R.drawable.demo_action_icon_transparent, true));
        gestureResourceList.add(new RecognitionListData(R.drawable.demo_gesture_icon_one));
        gestureResourceList.add(new RecognitionListData(R.drawable.demo_gesture_icon_two));
        gestureResourceList.add(new RecognitionListData(R.drawable.demo_gesture_icon_three));
        gestureResourceList.add(new RecognitionListData(R.drawable.demo_gesture_icon_five));
        gestureResourceList.add(new RecognitionListData(R.drawable.demo_gesture_icon_six));
        gestureResourceList.add(new RecognitionListData(R.drawable.demo_gesture_icon_fist));
        gestureResourceList.add(new RecognitionListData(R.drawable.demo_gesture_icon_palm_up));
        gestureResourceList.add(new RecognitionListData(R.drawable.demo_gesture_icon_thumb_up));
        return Collections.unmodifiableList(gestureResourceList);
    }

    private List<RecognitionListData> initActionList() {
        List<RecognitionListData> actionResourceList = new ArrayList<>(16);
        actionResourceList.add(new RecognitionListData(R.drawable.demo_action_icon_0));
        actionResourceList.add(new RecognitionListData(R.drawable.demo_action_icon_1));
        actionResourceList.add(new RecognitionListData(R.drawable.demo_action_icon_2));
        actionResourceList.add(new RecognitionListData(R.drawable.demo_action_icon_3));
        actionResourceList.add(new RecognitionListData(R.drawable.demo_action_icon_4));
        actionResourceList.add(new RecognitionListData(R.drawable.demo_action_icon_5));
        actionResourceList.add(new RecognitionListData(R.drawable.demo_action_icon_6));
        actionResourceList.add(new RecognitionListData(R.drawable.demo_action_icon_transparent, true));
        actionResourceList.add(new RecognitionListData(R.drawable.demo_action_icon_7));
        actionResourceList.add(new RecognitionListData(R.drawable.demo_action_icon_8));
        actionResourceList.add(new RecognitionListData(R.drawable.demo_action_icon_9));
        actionResourceList.add(new RecognitionListData(R.drawable.demo_action_icon_10));
        actionResourceList.add(new RecognitionListData(R.drawable.demo_action_icon_11));
        actionResourceList.add(new RecognitionListData(R.drawable.demo_action_icon_12));
        actionResourceList.add(new RecognitionListData(R.drawable.demo_action_icon_13));
        actionResourceList.add(new RecognitionListData(R.drawable.demo_action_icon_14));
        return Collections.unmodifiableList(actionResourceList);
    }

    private List<Integer> initExpressionList() {
        List<Integer> expressionList = new ArrayList<>(18);
        expressionList.add(R.drawable.demo_expression_icon_raise_eyebrows);
        expressionList.add(R.drawable.demo_expression_icon_frown);
        expressionList.add(R.drawable.demo_expression_icon_close_left_eye);
        expressionList.add(R.drawable.demo_expression_icon_close_right_eye);
        expressionList.add(R.drawable.demo_expression_icon_eyes_wide_open);
        expressionList.add(R.drawable.demo_expression_icon_left_corner_of_mouth);
        expressionList.add(R.drawable.demo_expression_icon_right_corner_of_mouth);
        expressionList.add(R.drawable.demo_expression_icon_smile);
        expressionList.add(R.drawable.demo_expression_icon_mouth_o);
        expressionList.add(R.drawable.demo_expression_icon_mouth_a);
        expressionList.add(R.drawable.demo_expression_icon_pouting);
        expressionList.add(R.drawable.demo_expression_icon_uting_mouth);
        expressionList.add(R.drawable.demo_expression_icon_bulging);
        expressionList.add(R.drawable.demo_expression_icon_twitch);
        expressionList.add(R.drawable.demo_expression_icon_turn_left);
        expressionList.add(R.drawable.demo_expression_icon_turn_right);
        expressionList.add(R.drawable.demo_expression_icon_nod);
        return Collections.unmodifiableList(expressionList);
    }

    private List<Integer> initTongueList() {
        List<Integer> tongueList = new ArrayList<>(8);
        tongueList.add(R.string.tongue_track_up);
        tongueList.add(R.string.tongue_track_down);
        tongueList.add(R.string.tongue_track_left);
        tongueList.add(R.string.tongue_track_right);
        tongueList.add(R.string.tongue_track_left_up);
        tongueList.add(R.string.tongue_track_left_down);
        tongueList.add(R.string.tongue_track_right_up);
        tongueList.add(R.string.tongue_track_right_down);
        return Collections.unmodifiableList(tongueList);
    }

    private static class TongueTrackAdapter extends BaseRecyclerAdapter<Integer> {

        TongueTrackAdapter(@NonNull List<Integer> data) {
            super(data, R.layout.rv_tongue_track);
        }

        @Override
        protected void bindViewHolder(BaseViewHolder viewHolder, Integer item) {
            viewHolder.setText(R.id.tv_item_tongue_track, item);
        }
    }

    private static class ExpressionAdapter extends BaseRecyclerAdapter<Integer> {

        ExpressionAdapter(@NonNull List<Integer> data) {
            super(data, R.layout.rv_expression);
        }

        @Override
        protected void bindViewHolder(BaseViewHolder viewHolder, Integer item) {
            int position = viewHolder.getAdapterPosition();
            viewHolder.setImageResource(R.id.iv_item_expression, item)
                    .setBackground(R.id.iv_item_expression, position == 0 ? R.drawable.selector_expression_item_first
                            : position == getItemCount() - 1 ? R.drawable.selector_expression_item_last
                            : R.drawable.selector_expression_item_mid);
        }

        @Override
        protected int choiceMode() {
            return MULTI_CHOICE_MODE;
        }

    }

    private static class RecognitionAdapter extends BaseRecyclerAdapter<RecognitionListData> {

        RecognitionAdapter(@NonNull List<RecognitionListData> data) {
            super(data, R.layout.rv_recognition);
        }

        @Override
        protected void bindViewHolder(BaseViewHolder viewHolder, RecognitionListData item) {
            viewHolder.setImageResource(R.id.iv_item_recognition, item.iconId);
            viewHolder.setBackground(R.id.iv_item_recognition, item.transparent ? android.R.color.transparent : R.drawable.selector_recognition_item);
        }

    }

    private static class RecognitionListData {
        private final int iconId;
        private final boolean transparent;

        RecognitionListData(int iconId, boolean transparent) {
            this.iconId = iconId;
            this.transparent = transparent;
        }

        RecognitionListData(int iconId) {
            this(iconId, false);
        }
    }

    protected class ViewClickListener extends OnMultiClickListener {

        @Override
        protected void onMultiClick(View v) {
            switch (v.getId()) {
                case R.id.iv_config: {
                    setMaskVisibility(true);
                    FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                    fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_right);
                    if (mConfigFragment == null) {
                        mConfigFragment = ConfigFragment.newInstance(containHumanAvatar());
                        mConfigFragment.setOnFragmentHiddenListener(new ConfigFragment.OnFragmentHiddenListener() {
                            @Override
                            public void onHiddenChanged(boolean hidden) {
                                if (!hidden) {
                                    return;
                                }
                                if (mRvExpression.getVisibility() == View.VISIBLE) {
                                    mTvExpressionTip.setVisibility(View.VISIBLE);
                                    mLifecycleHandler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            mTvExpressionTip.setVisibility(View.INVISIBLE);
                                        }
                                    }, 1500);
                                } else {
                                    if (mTvExpressionTip != null) {
                                        mTvExpressionTip.setVisibility(View.GONE);
                                    }
                                }
                            }
                        });
                        fragmentTransaction.add(R.id.fl_fragment_container, mConfigFragment, ConfigFragment.TAG);
                    } else {
                        fragmentTransaction.show(mConfigFragment);
                    }
                    fragmentTransaction.commit();
                }
                break;
                case R.id.iv_save_photo:
                case R.id.iv_debug:
                case R.id.iv_play_video:
                case R.id.iv_switch_cam: {
                    onViewClicked(v.getId());
                }
                break;
                case R.id.iv_home: {
                    onBackPressed();
                }
                break;
                default:
            }
        }
    }

}