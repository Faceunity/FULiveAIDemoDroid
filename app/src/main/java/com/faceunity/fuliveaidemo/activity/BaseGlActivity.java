package com.faceunity.fuliveaidemo.activity;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.faceunity.core.entity.FURenderOutputData;
import com.faceunity.core.media.photo.OnPhotoRecordingListener;
import com.faceunity.core.media.photo.PhotoRecordHelper;
import com.faceunity.core.utils.GlUtil;
import com.faceunity.fuliveaidemo.R;
import com.faceunity.fuliveaidemo.util.DensityUtils;
import com.faceunity.fuliveaidemo.util.FileUtils;
import com.faceunity.fuliveaidemo.util.LifecycleHandler;
import com.faceunity.fuliveaidemo.util.ToastUtil;
import com.faceunity.fuliveaidemo.view.ConfigFragment;
import com.faceunity.fuliveaidemo.view.OnMultiClickListener;
import com.faceunity.fuliveaidemo.view.RecordButton;
import com.faceunity.fuliveaidemo.view.adapter.BaseRecyclerAdapter;
import com.faceunity.fuliveaidemo.view.adapter.SpaceItemDecoration;
import com.faceunity.nama.FURenderer;
import com.faceunity.nama.view.bean.TrackType;
import com.faceunity.nama.view.listener.TypeEnum;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Richie on 2020.05.21
 */
public abstract class BaseGlActivity extends AppCompatActivity implements OnPhotoRecordingListener {
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
    protected ViewClickListener mViewClickListener;
    protected FURenderer mFURenderer;
    private RecyclerView mRvTongueTrack;
    private RecyclerView mRvEmotionTrack;
    private TongueTrackAdapter mTongueTrackAdapter;
    private TongueTrackAdapter mEmotionTrackAdapter;
    private RecyclerView mRvExpression;
    private ExpressionAdapter mExpressionAdapter;
    private RecyclerView mRvRecognition;
    private RecognitionAdapter mRecognitionAdapter;
    public RecordButton mRecordBtn;
    private List<RecognitionListData> mActionResourceList;
    private List<RecognitionListData> mGestureResourceList;
    private int mRecognitionIndex = -1;
    private int mRecognitionCount;
    private int mDetectTongueIndex = -1;
    private int mDetectTongueCount;
    private int mDetectEmotionIndex = -1;
    private boolean mDetectEmotionConfuse;
    private int mDetectEmotionCount;
    private int mRecognitionType = RECOGNITION_TYPE_NONE;
    private boolean mIsDetectTongue;
    private boolean mIsDetectEmotion;
    private boolean mIsDetectExpression;
    private int[] mDetectEmotionResult;
    private int[] mDetectExpressionTypes;
    private int[] mDetectExpressionIndexes;
    protected boolean mIsFlipX;
    private LifecycleHandler mLifecycleHandler;
    private TrackType trackType = new TrackType();
    public TypeEnum mTypeEnum = TypeEnum.EFFECT;

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
            int index = mDetectTongueIndex;
            if (index >= 0) {
                mTongueTrackAdapter.setItemSelected(index);
            } else {
                mTongueTrackAdapter.clearSingleItemSelected();
            }
        }
    };
    private final Runnable mUpdateEmotionRecyclerTask = new Runnable() {
        @Override
        public void run() {
            int index = mDetectEmotionIndex;
            if (index >= 0) {
                mEmotionTrackAdapter.setItemSelected(index);
            } else {
                mEmotionTrackAdapter.clearSingleItemSelected();
            }
            mEmotionTrackAdapter.updateLastItem(mDetectEmotionConfuse);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawable(null);
        setContentView(R.layout.activity_base);

        mViewClickListener = new ViewClickListener();
        mRecordBtn = findViewById(R.id.btn_record_video);
        findViewById(R.id.iv_home).setOnClickListener(mViewClickListener);
        findViewById(R.id.iv_config).setOnClickListener(mViewClickListener);
        mTvTrackStatus = findViewById(R.id.tv_track_status);
        mViewMask = findViewById(R.id.v_mask);
        mGlSurfaceView = findViewById(R.id.gl_surface);
        mRvRecognition = findViewById(R.id.rv_recognition);
        mRvRecognition.setTag(false);
        mRvExpression = findViewById(R.id.rv_expression);
        mRvTongueTrack = findViewById(R.id.rv_tongue_track);
        mRvEmotionTrack = findViewById(R.id.rv_emotion_track);
        mLifecycleHandler = new LifecycleHandler(getLifecycle());
        mPhotoRecordHelper = new PhotoRecordHelper(mOnPhotoRecordingListener);
        initView();
        initGlRenderer();
        initFuRenderer();
        findViewById(R.id.iv_config).performClick();
    }

    @Override
    public void onRecordSuccess(Bitmap bitmap) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mConfigFragment != null) {
            mConfigFragment.bindCurrentRenderer();
        }
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
     * 查一下当前的人脸 人体 手势 选择情况
     */
    public void sure() {
        trackType = mConfigFragment.getSelectedTrackType();
    }

    /**
     * 提示优先级（手势和人体功能不可能同时出现）
     * 未检测到人脸 > 未检测到人体 or 未检测到手势 > 未检测到全身，全身入境试试哦~
     * <p>
     * 例子：如下
     * 单功能提示：
     * 未检测到单功能项就做出对应提示（比如开启人脸 -> 未检测到的话“未检测到人脸”）
     * 复合功能提示：
     * 人脸 + 身体（全身 or 半身）
     * ① 无人脸 无人体 -> 未检测到人脸
     * ② 无人脸 有人体 -> 未检测到人脸
     * ③ 有人脸 无人体 -> 未检测到人体 or 未检测到全身，全身入境试试哦~
     * ④ 有人脸 有人体 -> 不提示
     * <p>
     * 人脸 + 手势
     * ① 无人脸 无手势 -> 未检测到人脸
     * ② 无人脸 有手势 -> 未检测到人脸
     * ③ 有人脸 无手势 -> 未检测到手势
     * ④ 有人脸 有手势 -> 不提示
     */
    protected void queryTrackStatus() {
        @StringRes int toastStrId = 0;
        boolean invisible = true;
        if (mConfigFragment != null) {
            if (trackType.face && trackType.body >= 0) {//人脸 + 人体
                boolean hasHuman = mFURenderer.queryHumanTrackStatus() > 0;
                boolean hasFace = mFURenderer.queryFaceTrackStatus() > 0;
                if (!hasFace) {
                    //没人脸
                    if (!hasFace) {//没人脸
                        invisible = false;
                        toastStrId = R.string.track_status_no_face;
                    }
                } else {
                    //有人脸
                    if (!hasHuman) {
                        //有人脸没人体
                        if (trackType.body == 0) {
                            //全身
                            toastStrId = R.string.track_status_no_human_full;
                        } else {
                            //半身
                            toastStrId = R.string.track_status_no_human;
                        }
                        invisible = false;
                    }
                }

            } else if (trackType.face && trackType.gesture) {//人脸 + 手势
                boolean hasFace = mFURenderer.queryFaceTrackStatus() > 0;
                boolean gestureNum = mFURenderer.handProcessorGetNumResults() > 0;
                if (!hasFace) {
                    //没人脸
                    if (!hasFace) {//没人脸
                        invisible = false;
                        toastStrId = R.string.track_status_no_face;
                    }
                } else {
                    //有人脸
                    if (!gestureNum) {
                        //有人脸没手势
                        invisible = false;
                        toastStrId = R.string.track_status_no_gesture;
                    }
                }
            } else if (trackType.face) {//人脸、
                boolean hasFace = mFURenderer.queryFaceTrackStatus() > 0;
                if (!hasFace) {//没人脸
                    invisible = false;
                    toastStrId = R.string.track_status_no_face;
                }
            } else if (trackType.body >= 0) {//人体
                boolean hasHuman = mFURenderer.queryHumanTrackStatus() > 0;
                if (!hasHuman) {//无人体
                    if (trackType.body == 0) {
                        //全身
                        toastStrId = R.string.track_status_no_human_full;
                    } else {
                        //半身
                        toastStrId = R.string.track_status_no_human;
                    }
                    invisible = false;
                }
            } else if (trackType.gesture) {//手势
                boolean gestureNum = mFURenderer.handProcessorGetNumResults() > 0;
                if (!gestureNum) {//没手指
                    invisible = false;
                    toastStrId = R.string.track_status_no_gesture;
                }
            }
        }
        final boolean visible = !invisible;
        final int strId = toastStrId;
        runOnUiThread(() -> {
            if (visible) {
                mTvTrackStatus.setVisibility(View.VISIBLE);
                mTvTrackStatus.setText(strId);
            } else {
                mTvTrackStatus.setVisibility(View.INVISIBLE);
            }
        });
    }

    protected void trackFace() {
        //舌头检测
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

        //表情
        if (mIsDetectExpression) {
            int[] expressionTypeResult = mDetectExpressionTypes;
            mFURenderer.detectFaceExpression(expressionTypeResult);
            for (int i = 0; i < expressionTypeResult.length; i++) {
                mDetectExpressionIndexes[i] = convertToExpressionIndex(expressionTypeResult[i], mIsFlipX);
            }
            mLifecycleHandler.post(mUpdateExpressionRecyclerTask);
        }

        //情绪
        if (mIsDetectEmotion) {
            int[] detectEmotionResult = mDetectEmotionResult;
            mFURenderer.detectFaceEmotion(detectEmotionResult);
            int index = convertToEmotionIndex(detectEmotionResult[0]);
//            Log.v(TAG, "trackFace: emotion " + emotion + ", index " + index);
            if (mDetectEmotionIndex == index) {
                mDetectEmotionCount++;
            } else {
                mDetectEmotionCount = 0;
            }
            mDetectEmotionIndex = index;
            mDetectEmotionConfuse = detectEmotionResult[1] != 0;
            if (mDetectEmotionCount >= FACE_DETECT_OK_THRESHOLD) {
                mLifecycleHandler.post(mUpdateEmotionRecyclerTask);
            }
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

    private static int convertToEmotionIndex(int type) {
        switch (type) {
            case FURenderer.FuAiEmotionType.FUAIEMOTION_NEUTRAL: {
                return 0;
            }
            case FURenderer.FuAiEmotionType.FUAIEMOTION_SURPRISE: {
                return 1;
            }
            case FURenderer.FuAiEmotionType.FUAIEMOTION_HAPPY: {
                return 2;
            }
            case FURenderer.FuAiEmotionType.FUAIEMOTION_DISGUST: {
                return 3;
            }
            case FURenderer.FuAiEmotionType.FUAIEMOTION_ANGRY: {
                return 4;
            }
            case FURenderer.FuAiEmotionType.FUAIEMOTION_FEAR: {
                return 5;
            }
            case FURenderer.FuAiEmotionType.FUAIEMOTION_SAD: {
                return 6;
            }
            case FURenderer.FuAiEmotionType.FUAIEMOTION_CONFUSE: {
                return 7;
            }
            default:
                return -1;
        }
    }

    private static int convertToExpressionIndex(int type, boolean isFlipX) {
        switch (type) {
            case FURenderer.FuAiFaceExpressionType.FACE_EXPRESSION_BROW_UP: {
                return 0;
            }
            case FURenderer.FuAiFaceExpressionType.FACE_EXPRESSION_BROW_FROWN: {
                return 1;
            }
            case FURenderer.FuAiFaceExpressionType.FACE_EXPRESSION_RIGHT_EYE_CLOSE: {
                return isFlipX ? 2 : 3;
            }
            case FURenderer.FuAiFaceExpressionType.FACE_EXPRESSION_LEFT_EYE_CLOSE: {
                return isFlipX ? 3 : 2;
            }
            case FURenderer.FuAiFaceExpressionType.FACE_EXPRESSION_EYE_WIDE: {
                return 4;
            }
            case FURenderer.FuAiFaceExpressionType.FACE_EXPRESSION_MOUTH_SMILE_RIGHT: {
                return isFlipX ? 5 : 6;
            }
            case FURenderer.FuAiFaceExpressionType.FACE_EXPRESSION_MOUTH_SMILE_LEFT: {
                return isFlipX ? 6 : 5;
            }
            case FURenderer.FuAiFaceExpressionType.FACE_EXPRESSION_MOUTH_SMILE: {
                return 7;
            }
            case FURenderer.FuAiFaceExpressionType.FACE_EXPRESSION_MOUTH_FUNNEL: {
                return 8;
            }
            case FURenderer.FuAiFaceExpressionType.FACE_EXPRESSION_MOUTH_OPEN: {
                return 9;
            }
            case FURenderer.FuAiFaceExpressionType.FACE_EXPRESSION_MOUTH_PUCKER: {
                return 10;
            }
            case FURenderer.FuAiFaceExpressionType.FACE_EXPRESSION_MOUTH_ROLL: {
                return 11;
            }
            case FURenderer.FuAiFaceExpressionType.FACE_EXPRESSION_MOUTH_PUFF: {
                return 12;
            }
            case FURenderer.FuAiFaceExpressionType.FACE_EXPRESSION_MOUTH_FROWN: {
                return 13;
            }
            case FURenderer.FuAiFaceExpressionType.FACE_EXPRESSION_HEAD_RIGHT: {
                return isFlipX ? 14 : 15;
            }
            case FURenderer.FuAiFaceExpressionType.FACE_EXPRESSION_HEAD_LEFT: {
                return isFlipX ? 15 : 14;
            }
            case FURenderer.FuAiFaceExpressionType.FACE_EXPRESSION_HEAD_NOD: {
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
            int type = mFURenderer.detectHumanAction();
            index = type;
        } else if (mRecognitionType == RECOGNITION_TYPE_GESTURE) {
            int type = mFURenderer.detectHumanGesture();
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
        RecyclerView rvTongueTrack = mRvTongueTrack;
        rvTongueTrack.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (mTongueTrackAdapter == null) {
            List<Integer> list = initTongueList();
            TongueTrackAdapter tongueTrackAdapter = new TongueTrackAdapter(new ArrayList<>(list));
            tongueTrackAdapter.setCanManualClick(false);
            ((SimpleItemAnimator) rvTongueTrack.getItemAnimator()).setSupportsChangeAnimations(false);
            rvTongueTrack.setHasFixedSize(true);
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
            rvTongueTrack.setLayoutManager(layoutManager);
            rvTongueTrack.setAdapter(tongueTrackAdapter);
            mTongueTrackAdapter = tongueTrackAdapter;
        }
        mIsDetectTongue = visible;
    }

    public void setEmotionTrackRecyclerVisibility(boolean visible) {
        RecyclerView rvEmotionTrack = mRvEmotionTrack;
        rvEmotionTrack.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (mEmotionTrackAdapter == null) {
            List<Integer> list = initEmotionList();
            TongueTrackAdapter emotionTrackAdapter = new TongueTrackAdapter(new ArrayList<>(list));
            emotionTrackAdapter.setCanManualClick(false);
            ((SimpleItemAnimator) rvEmotionTrack.getItemAnimator()).setSupportsChangeAnimations(false);
            rvEmotionTrack.setHasFixedSize(true);
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
            rvEmotionTrack.setLayoutManager(layoutManager);
            rvEmotionTrack.setAdapter(emotionTrackAdapter);
            mEmotionTrackAdapter = emotionTrackAdapter;
            mDetectEmotionResult = new int[2];
        }
        mIsDetectEmotion = visible;
    }

    public void setNormalOrAvatarMode(TypeEnum typeEnum) {
        this.mTypeEnum = typeEnum;
        if (TypeEnum.EFFECT == typeEnum) {
            mRecordBtn.setVisibility(View.VISIBLE);
        } else {
            mRecordBtn.setVisibility(View.GONE);
            //展示dialog什么的
            Dialog loadingDialog = createLoadingDialog();
            loadingDialog.show();

            mLifecycleHandler.postDelayed(() -> {
                if (loadingDialog != null && loadingDialog.isShowing())
                    loadingDialog.dismiss();
            }, 500);
        }
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

    public Dialog createLoadingDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null);
        Dialog dialog = new Dialog(this);
        dialog.setCancelable(false);
        dialog.setContentView(view, new ViewGroup.LayoutParams(
                DensityUtils.dp2px(this, 110), DensityUtils.dp2px(this, 110)));
        dialog.getWindow().setBackgroundDrawable(null);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);//去掉白色背景
        return dialog;
    }


    private static List<RecognitionListData> initGestureList() {
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

    private static List<RecognitionListData> initActionList() {
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

    private static List<Integer> initExpressionList() {
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

    private static List<Integer> initTongueList() {
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

    private static List<Integer> initEmotionList() {
        List<Integer> tongueList = new ArrayList<>(8);
        tongueList.add(R.string.emotion_track_neutral);
        tongueList.add(R.string.emotion_track_surprise);
        tongueList.add(R.string.emotion_track_happy);
        tongueList.add(R.string.emotion_track_disgust);
        tongueList.add(R.string.emotion_track_angry);
        tongueList.add(R.string.emotion_track_fear);
        tongueList.add(R.string.emotion_track_sad);
        tongueList.add(R.string.emotion_track_confuse);
        return Collections.unmodifiableList(tongueList);
    }

    private static class TongueTrackAdapter extends BaseRecyclerAdapter<Integer> {
        private boolean mConfuse;

        TongueTrackAdapter(@NonNull List<Integer> data) {
            super(data, R.layout.rv_tongue_track);
        }

        @Override
        protected void bindViewHolder(BaseViewHolder viewHolder, Integer item) {
            viewHolder.setText(R.id.tv_item_tongue_track, item);
            if (item == R.string.emotion_track_confuse) {
                viewHolder.setVisibility(R.id.tv_item_tongue_track_status, View.VISIBLE)
                        .setText(R.id.tv_item_tongue_track_status, mConfuse ? R.string.emotion_track_yes : R.string.emotion_track_no)
                        .setBackground(R.id.fl_tongue_track, R.drawable.shape_bg_float_text_b);
            }
        }

        public void updateLastItem(boolean confuse) {
            mConfuse = confuse;
            int index = getItemCount() - 1;
            notifyItemChanged(index);
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
                        fragmentTransaction.hide(mConfigFragment);
                    } else {
                        mConfigFragment.setTakeEffects();
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

    //region 拍照
    public PhotoRecordHelper mPhotoRecordHelper;
    public volatile Boolean isTakePhoto = false;

    /**
     * 获取拍摄的照片
     */
    private final OnPhotoRecordingListener mOnPhotoRecordingListener = this::onReadBitmap;


    protected void onReadBitmap(Bitmap bitmap) {
        String path = FileUtils.addBitmapToAlbum(this, bitmap);
        runOnUiThread(() -> {
            if (path == null) {
                ToastUtil.makeText(this, R.string.save_photo_failure).show();
            } else {
                ToastUtil.makeText(this, R.string.save_photo_success).show();
                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(path)));
                sendBroadcast(intent);
            }
        });
    }

    /*录制保存*/
    public void recordingPhoto(FURenderOutputData outputData, float[] texMatrix) {
        if (outputData == null || outputData.getTexture() == null || outputData.getTexture().getTexId() <= 0) {
            return;
        }
        if (isTakePhoto) {
            isTakePhoto = false;
            mPhotoRecordHelper.sendRecordingData(outputData.getTexture().getTexId(), texMatrix, GlUtil.IDENTITY_MATRIX, outputData.getTexture().getWidth(), outputData.getTexture().getHeight());
        }
    }
    //endregion 拍照
}