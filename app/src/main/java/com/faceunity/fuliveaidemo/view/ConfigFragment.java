package com.faceunity.fuliveaidemo.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.faceunity.fuliveaidemo.R;
import com.faceunity.fuliveaidemo.activity.BaseGlActivity;
import com.faceunity.fuliveaidemo.util.DensityUtils;
import com.faceunity.fuliveaidemo.view.adapter.BaseRecyclerAdapter;
import com.faceunity.fuliveaidemo.view.adapter.SpaceItemDecoration;
import com.faceunity.nama.Effect;
import com.faceunity.nama.FURenderer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 功能配置页
 *
 * @author Richie on 2020.05.21
 */
public class ConfigFragment extends Fragment {
    public static final String TAG = "ConfigFragment";
    private static final String ASSETS_DIR = "others/";
    public static final String FACE_LANDMARKS_BUNDLE_PATH = ASSETS_DIR + "landmarks.bundle";
    private static final String FACE_TONGUE_BUNDLE_PATH = ASSETS_DIR + "set_tongue.bundle";
    public static final String FACE_EXPRESSION_BUNDLE_PATH = ASSETS_DIR + "aitype.bundle";
    private static final String HUMAN_MASK_BUNDLE_PATH = ASSETS_DIR + "human_mask.bundle";
    private static final String HUMAN_ACTION_BUNDLE_PATH = ASSETS_DIR + "human_action.bundle";
    private static final String HUMAN_GESTURE_BUNDLE_PATH = ASSETS_DIR + "human_gesture.bundle";
    private static final String HAIR_MASK_BUNDLE_PATH = ASSETS_DIR + "hair_normal_algorithm.bundle";
    private static final String HEAD_MASK_BUNDLE_PATH = ASSETS_DIR + "head_mask.bundle";
    private static final String FULL_BODY_LANDMARKS_BUNDLE_PATH = ASSETS_DIR + "bodyLandmarks_dance.bundle";
    private static final String HALF_BODY_LANDMARKS_BUNDLE_PATH = ASSETS_DIR + "bodyLandmarks_selife.bundle";

    /**
     * 人体检测绘制类型，关键点和骨骼
     */
    private static final int HUMAN_TYPE_LANDMARKS = 1;
    private static final int HUMAN_TYPE_SKELETON = 2;

    public static final String LANDMARKS_TYPE = "landmarks_type";
    public static final String CONTAIN_HUMAN_AVATAR = "contain_human_avatar";

    private BaseGlActivity mBaseGlActivity;
    private RecyclerViewAdapter mFaceRecyclerAdapter;
    private RecyclerViewAdapter mHumanRecyclerAdapter;
    private RecyclerViewAdapter mGestureRecyclerAdapter;
    private RecyclerViewAdapter mSegRecyclerAdapter;
    private RecyclerViewAdapter mActionRecyclerAdapter;
    private RecyclerItemClickListener mRecyclerItemClickListener;
    private FrameLayout mFlHumanLandmarks;
    private FrameLayout mFlGestureOutline;
    private ImageView mIvLandmarksBg;
    private TextView mTvLandmarksFull;
    private TextView mTvLandmarksHalf;

    private Effect mHalfEffect;
    private Effect mFullEffect;
    private Map<Integer, List<Effect>> mEffectMap;
    private Map<Effect, List<Effect>> mMutexMap;
    private ViewClickListener mViewClickListener;
    private int mHumanType;

    public static ConfigFragment newInstance(boolean containHumanAvatar) {
        ConfigFragment configFragment = new ConfigFragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean(CONTAIN_HUMAN_AVATAR, containHumanAvatar);
        configFragment.setArguments(bundle);
        return configFragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mBaseGlActivity = (BaseGlActivity) context;
        boolean containHumanAvatar = getArguments().getBoolean(CONTAIN_HUMAN_AVATAR, true);
        initListData(containHumanAvatar);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_config, container, false);
        // consume touch event
        view.findViewById(R.id.cl_config_operation).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        mViewClickListener = new ViewClickListener();
        view.findViewById(R.id.fl_config_mask).setOnClickListener(mViewClickListener);
        view.findViewById(R.id.btn_config_confirm).setOnClickListener(mViewClickListener);
        view.findViewById(R.id.btn_config_reset).setOnClickListener(mViewClickListener);

        mFlHumanLandmarks = view.findViewById(R.id.fl_config_human_landmarks);
        mFlHumanLandmarks.findViewById(R.id.tv_config_human_full).setOnClickListener(mViewClickListener);
        mFlHumanLandmarks.findViewById(R.id.tv_config_human_half).setOnClickListener(mViewClickListener);
        mIvLandmarksBg = mFlHumanLandmarks.findViewById(R.id.iv_config_human_landmarks_bg);
        mTvLandmarksFull = mFlHumanLandmarks.findViewById(R.id.tv_config_human_full);
        mTvLandmarksHalf = mFlHumanLandmarks.findViewById(R.id.tv_config_human_half);
        setLandmarksTextSelected(true);

        FrameLayout flFaceOutline = view.findViewById(R.id.fl_config_face_outline);
        ((ImageView) flFaceOutline.findViewById(R.id.iv_config_outline_img)).setImageResource(R.drawable.demo_sidebar_icon_face);
        ((TextView) flFaceOutline.findViewById(R.id.tv_config_outline_text)).setText(R.string.config_sub_title_face);
        RecyclerView rvConfigFace = view.findViewById(R.id.rv_config_face);
        initRecyclerView(rvConfigFace);
        List<Effect> faceEffects = mEffectMap.get(Effect.TYPE_FACE);
        mFaceRecyclerAdapter = new RecyclerViewAdapter(new ArrayList<>(faceEffects));
        mRecyclerItemClickListener = new RecyclerItemClickListener();
        mFaceRecyclerAdapter.setOnItemClickListener(mRecyclerItemClickListener);
        rvConfigFace.setAdapter(mFaceRecyclerAdapter);

        FrameLayout flHumanOutline = view.findViewById(R.id.fl_config_human_outline);
        ((ImageView) flHumanOutline.findViewById(R.id.iv_config_outline_img)).setImageResource(R.drawable.demo_sidebar_icon_body);
        ((TextView) flHumanOutline.findViewById(R.id.tv_config_outline_text)).setText(R.string.config_sub_title_human);
        RecyclerView rvConfigHuman = view.findViewById(R.id.rv_config_human);
        initRecyclerView(rvConfigHuman);
        List<Effect> humanEffects = mEffectMap.get(Effect.TYPE_HUMAN);
        mHumanRecyclerAdapter = new RecyclerViewAdapter(new ArrayList<>(humanEffects));
        mHumanRecyclerAdapter.setOnItemClickListener(mRecyclerItemClickListener);
        rvConfigHuman.setAdapter(mHumanRecyclerAdapter);

        FrameLayout flGestureOutline = view.findViewById(R.id.fl_config_gesture_outline);
        ((ImageView) flGestureOutline.findViewById(R.id.iv_config_outline_img)).setImageResource(R.drawable.demo_sidebar_icon_gesture);
        ((TextView) flGestureOutline.findViewById(R.id.tv_config_outline_text)).setText(R.string.config_sub_title_gesture);
        RecyclerView rvConfigGesture = view.findViewById(R.id.rv_config_gesture);
        initRecyclerView(rvConfigGesture);
        List<Effect> gestureEffects = mEffectMap.get(Effect.TYPE_GESTURE);
        mGestureRecyclerAdapter = new RecyclerViewAdapter(new ArrayList<>(gestureEffects));
        mGestureRecyclerAdapter.setOnItemClickListener(mRecyclerItemClickListener);
        rvConfigGesture.setAdapter(mGestureRecyclerAdapter);
        mFlGestureOutline = flGestureOutline;

        FrameLayout flSegOutline = view.findViewById(R.id.fl_config_seg_outline);
        ((ImageView) flSegOutline.findViewById(R.id.iv_config_outline_img)).setImageResource(R.drawable.demo_sidebar_icon_segmentation);
        ((TextView) flSegOutline.findViewById(R.id.tv_config_outline_text)).setText(R.string.config_sub_title_seg);
        RecyclerView rvConfigSeg = view.findViewById(R.id.rv_config_seg);
        initRecyclerView(rvConfigSeg);
        List<Effect> segEffects = mEffectMap.get(Effect.TYPE_SEGMENTATION);
        mSegRecyclerAdapter = new RecyclerViewAdapter(new ArrayList<>(segEffects));
        mSegRecyclerAdapter.setOnItemClickListener(mRecyclerItemClickListener);
        rvConfigSeg.setAdapter(mSegRecyclerAdapter);

        FrameLayout flActionOutline = view.findViewById(R.id.fl_config_action_outline);
        ((ImageView) flActionOutline.findViewById(R.id.iv_config_outline_img)).setImageResource(R.drawable.demo_sidebar_icon_action);
        ((TextView) flActionOutline.findViewById(R.id.tv_config_outline_text)).setText(R.string.config_sub_title_action);
        RecyclerView rvConfigAction = view.findViewById(R.id.rv_config_action);
        initRecyclerView(rvConfigAction);
        List<Effect> actionEffects = mEffectMap.get(Effect.TYPE_ACTION);
        mActionRecyclerAdapter = new RecyclerViewAdapter(new ArrayList<>(actionEffects));
        mActionRecyclerAdapter.setOnItemClickListener(mRecyclerItemClickListener);
        rvConfigAction.setAdapter(mActionRecyclerAdapter);

        int defaultSelect = 0;
        mFaceRecyclerAdapter.setItemSelected(defaultSelect);
        mRecyclerItemClickListener.onItemClick(mFaceRecyclerAdapter, new View(mBaseGlActivity), defaultSelect);
        return view;
    }

    private void initRecyclerView(RecyclerView recyclerView) {
        int rvItemHorizontalSpace = DensityUtils.dp2px(mBaseGlActivity, 4);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getContext(), 3);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        int rvItemVerticalSpace = DensityUtils.dp2px(mBaseGlActivity, 4);
        recyclerView.addItemDecoration(new SpaceItemDecoration(rvItemHorizontalSpace, rvItemVerticalSpace));
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
    }

    public int getSelectedTrackType() {
        if (mHumanRecyclerAdapter == null || mActionRecyclerAdapter == null) {
            return FURenderer.TRACK_TYPE_FACE;
        }
        boolean isSelectedHuman = false;
        if (mHumanRecyclerAdapter.getSelectedItems().size() > 0 || mActionRecyclerAdapter.getSelectedItems().size() > 0) {
            isSelectedHuman = true;
        } else {
            SparseArray<Effect> selectedItems = mSegRecyclerAdapter.getSelectedItems();
            if (selectedItems.size() > 0) {
                Effect effect = selectedItems.valueAt(0);
                if (getResources().getString(R.string.config_item_seg_portrait).equals(effect.getDescription())) {
                    isSelectedHuman = true;
                }
            }
        }
        if (isSelectedHuman) {
            return FURenderer.TRACK_TYPE_HUMAN;
        }
//        if (mGestureRecyclerAdapter.getSelectedItems().size() > 0) {
//            return FURenderer.TRACK_TYPE_GESTURE;
//        }
        boolean isSelectedHead = mFaceRecyclerAdapter.getSelectedItems().size() > 0;
        if (!isSelectedHead) {
            SparseArray<Effect> selectedItems = mSegRecyclerAdapter.getSelectedItems();
            if (selectedItems.size() > 0) {
                Effect effect = selectedItems.valueAt(0);
                String description = effect.getDescription();
                if (getResources().getString(R.string.config_item_seg_hair).equals(description)
                        || getResources().getString(R.string.config_item_seg_head).equals(description)) {
                    isSelectedHead = true;
                }
            }
        }
        if (isSelectedHead) {
            return FURenderer.TRACK_TYPE_FACE;
        }
        return 0;
    }

    public int getToastStringId() {
        boolean isSelectedHumanFull = mRecyclerItemClickListener.mHumanState == RecyclerItemClickListener.HUMAN_STATE_FULL;
        boolean isSelectAction = mActionRecyclerAdapter.getSelectedItems().size() > 0;
        boolean isSelectedFace = mFaceRecyclerAdapter.getSelectedItems().size() > 0;
        boolean isSelectedHuman = false;
//        if (!isSelectedFace) {
        SparseArray<Effect> selectedItems = mSegRecyclerAdapter.getSelectedItems();
        if (selectedItems.size() > 0) {
            Effect effect = selectedItems.valueAt(0);
            String description = effect.getDescription();
            if (getResources().getString(R.string.config_item_seg_hair).equals(description)
                    || getResources().getString(R.string.config_item_seg_head).equals(description)) {
//                    isSelectedFace = true;
            } else {
                isSelectedHuman = true;
            }
        }
//        }
//        if (isSelectedFace) {
//            return R.string.track_status_no_face;
//        }
        boolean isSelectedHumanHalf = mRecyclerItemClickListener.mHumanState == RecyclerItemClickListener.HUMAN_STATE_HALF;
        if (isSelectedHumanFull && !isSelectAction && !isSelectedHuman && !isSelectedFace) {
            return R.string.track_status_no_human_full;
        }
        if ((isSelectAction || isSelectedHumanHalf || isSelectedHuman) && !isSelectedFace) {
            return R.string.track_status_no_human;
        }
//        boolean isSelectGesture = mGestureRecyclerAdapter.getSelectedItems().size() > 0;
//        if (isSelectGesture) {
//            return R.string.track_status_no_gesture;
//        }
        return R.string.track_status_no_face;
    }

    private class RecyclerItemClickListener implements BaseRecyclerAdapter.OnItemClickListener<Effect> {
        private static final int ANIMATION_DURATION = 200;
        private static final float ANIMATION_START_VALUE = 0.5F;
        private static final float ANIMATION_END_VALUE = 40F;
        private static final int OUTLINE_MARGIN = 48;
        private static final int HUMAN_STATE_FULL = 484;
        private static final int HUMAN_STATE_HALF = 641;
        private static final int HUMAN_STATE_NONE = 376;
        private int mHumanState = HUMAN_STATE_NONE;
        private ValueAnimator mExpandAnimator;
        private ValueAnimator mShrinkAnimator;
        private int mPx24 = DensityUtils.dp2px(mBaseGlActivity, 24);
        private int mPx72 = DensityUtils.dp2px(mBaseGlActivity, 72);

        void clickLandmarks() {
            if (mHumanState == HUMAN_STATE_FULL || mHumanState == HUMAN_STATE_HALF) {
                shrink();
                mHumanState = HUMAN_STATE_NONE;
            }
        }

        // 手势识别、人体骨骼、人像分割、头发分割、头部分割5个功能是独立的，不可与其他功能共用
        // 人体关键点、动作识别 可以共用，都只能检测1人
        // 稠密点，表情识别和舌头检测都需依赖人脸特征点检测；
        // 人脸所有功能都可以同时使用生效；
        // 人脸和人体骨骼不共用，和其他功能都支持同时使用。
        @Override
        public void onItemClick(BaseRecyclerAdapter<Effect> adapter, View view, int position) {
            Effect item = adapter.getItem(position);
            if (item == null) {
                return;
            }
            boolean isSelected = view.isSelected();
            boolean refresh = true; // 在已有选中的情况下，是否再次刷新互斥状态
            String description = item.getDescription();
            boolean selectHumanLandmarks = getResources().getString(R.string.config_item_human_landmark).equals(description);
            boolean selectSkeleton = getResources().getString(R.string.config_item_human_skeleton).equals(description);
            if (selectHumanLandmarks) {
                int index = mActionRecyclerAdapter.getSelectedItems().indexOfValue(mEffectMap.get(Effect.TYPE_ACTION).get(0));
                if (index >= 0) {
                    refresh = false;
                }
            } else if (getResources().getString(R.string.config_item_action_recognition).equals(description)) {
                int index = mHumanRecyclerAdapter.getSelectedItems().indexOfValue(mEffectMap.get(Effect.TYPE_HUMAN).get(0));
                if (index >= 0) {
                    refresh = false;
                }
            } else if (getResources().getString(R.string.config_item_face_landmarks_75).equals(description)
                    || getResources().getString(R.string.config_item_face_tongue).equals(description)
                    || getResources().getString(R.string.config_item_face_expression).equals(description)
            ) {
                int size = adapter.getSelectedItems().size();
                if (isSelected) {
                    if (!getResources().getString(R.string.config_item_face_landmarks_75).equals(description) && size > 0) {
                        refresh = false;
                    }
                } else {
                    if (size > 1) {
                        refresh = false;
                    }
                }
                if (refresh) {
                    int index = mHumanRecyclerAdapter.getSelectedItems().indexOfValue(mEffectMap.get(Effect.TYPE_HUMAN).get(0));
                    if (index >= 0) {
                        refresh = false;
                    }
                }
                if (refresh) {
                    size = mGestureRecyclerAdapter.getSelectedItems().size();
                    if (size > 0) {
                        refresh = false;
                    }
                }
                if (refresh) {
                    size = mSegRecyclerAdapter.getSelectedItems().size();
                    if (size > 0) {
                        refresh = false;
                    }
                }
                if (refresh) {
                    size = mActionRecyclerAdapter.getSelectedItems().size();
                    if (size > 0) {
                        refresh = false;
                    }
                }
            }
            if (selectHumanLandmarks || selectSkeleton) {
                mHumanType = selectHumanLandmarks ? HUMAN_TYPE_LANDMARKS : HUMAN_TYPE_SKELETON;
                if (mHumanState == HUMAN_STATE_FULL || mHumanState == HUMAN_STATE_HALF) {
                    shrink();
                    mHumanState = HUMAN_STATE_NONE;
                } else {
                    expand();
                    mHumanState = HUMAN_STATE_FULL;
                }
            } else {
                if (getResources().getString(R.string.config_item_action_recognition).equals(description)) {
                    mBaseGlActivity.setRecognitionRecyclerVisibility(BaseGlActivity.RECOGNITION_TYPE_ACTION, !isSelected);
                } else if (getResources().getString(R.string.config_item_gesture_recognition).equals(description)) {
                    mBaseGlActivity.setRecognitionRecyclerVisibility(BaseGlActivity.RECOGNITION_TYPE_GESTURE, !isSelected);
                } else {
                    if (getResources().getString(R.string.config_item_face_landmarks_75).equals(description)) {
                        if (isSelected) {
                            // 取消选中人脸模块所有功能
                            SparseArray<Effect> selectedItems = adapter.getSelectedItems();
                            Effect tongueTrack = null;
                            Effect expression = null;
                            for (int i = 0, size = selectedItems.size(); i < size; i++) {
                                Effect effect = selectedItems.valueAt(i);
                                String desc = effect.getDescription();
                                if (getResources().getString(R.string.config_item_face_tongue).equals(desc)) {
                                    tongueTrack = effect;
                                } else if (getResources().getString(R.string.config_item_face_expression).equals(desc)) {
                                    expression = effect;
                                }
                            }
                            if (tongueTrack != null) {
                                adapter.clearItemSelected(tongueTrack);
                                mBaseGlActivity.setTongueTrackRecyclerVisibility(false);
                                mBaseGlActivity.getFuRenderer().unselectEffect(tongueTrack);
                            }
                            if (expression != null) {
                                adapter.clearItemSelected(expression);
                                mBaseGlActivity.setExpressionRecyclerVisibility(false);
                            }
                        }
                    } else if (getResources().getString(R.string.config_item_face_tongue).equals(description)) {
                        Effect landmarks75 = null;
                        SparseArray<Effect> selectedItems = adapter.getSelectedItems();
                        for (int i = 0; i < selectedItems.size(); i++) {
                            Effect effect = selectedItems.valueAt(i);
                            if (getResources().getString(R.string.config_item_face_landmarks_75).equals(effect.getDescription())) {
                                landmarks75 = effect;
                            }
                        }
                        if (isSelected) {
                            mBaseGlActivity.getFuRenderer().selectEffect(landmarks75);
                        } else {
                            if (landmarks75 == null) {
                                adapter.setItemSelected(0);
                                landmarks75 = mEffectMap.get(Effect.TYPE_FACE).get(0);
                                mBaseGlActivity.getFuRenderer().selectEffect(landmarks75);
                            }
                        }
                        mBaseGlActivity.setTongueTrackRecyclerVisibility(!isSelected);
                    } else if (getResources().getString(R.string.config_item_face_expression).equals(description)) {
                        Effect landmarks75 = null;
                        SparseArray<Effect> selectedItems = adapter.getSelectedItems();
                        for (int i = 0; i < selectedItems.size(); i++) {
                            Effect effect = selectedItems.valueAt(i);
                            if (getResources().getString(R.string.config_item_face_landmarks_75).equals(effect.getDescription())) {
                                landmarks75 = effect;
                            }
                        }
                        if (isSelected) {
                            mBaseGlActivity.getFuRenderer().selectEffect(landmarks75);
                        } else {
                            if (landmarks75 == null) {
                                adapter.setItemSelected(0);
                                landmarks75 = mEffectMap.get(Effect.TYPE_FACE).get(0);
                                mBaseGlActivity.getFuRenderer().selectEffect(landmarks75);
                            }
                        }
                        mBaseGlActivity.setExpressionRecyclerVisibility(!isSelected);
                        return;
                    }
                }
                clickEffect(item, !isSelected);
            }
            if (refresh) {
                List<Effect> mutexEffects = mMutexMap.get(item);
                if (mutexEffects != null) {
                    for (Effect mutexEffect : mutexEffects) {
                        mutexEffect.setState(isSelected ? Effect.STATE_ENABLE : Effect.STATE_DISABLE);
                        mFaceRecyclerAdapter.update(mutexEffect);
                        mHumanRecyclerAdapter.update(mutexEffect);
                        mGestureRecyclerAdapter.update(mutexEffect);
                        mSegRecyclerAdapter.update(mutexEffect);
                        mActionRecyclerAdapter.update(mutexEffect);
                    }
                }
                List<Effect> humanEffects = mEffectMap.get(Effect.TYPE_HUMAN);
                if (humanEffects.size() > 1) {
                    Effect skeleton = humanEffects.get(1);
                    boolean hasFace = mFaceRecyclerAdapter.getSelectedItems().size() > 0;
                    boolean hasGesture = mGestureRecyclerAdapter.getSelectedItems().size() > 0;
                    boolean hasSeg = mSegRecyclerAdapter.getSelectedItems().size() > 0;
                    boolean hasAction = mActionRecyclerAdapter.getSelectedItems().size() > 0;
                    if ((hasFace || hasGesture || hasSeg || hasAction) && !selectSkeleton
                            && skeleton.getState() == Effect.STATE_ENABLE) {
                        skeleton.setState(Effect.STATE_DISABLE);
                    }
                }
            }
        }

        private void expand() {
            if (mExpandAnimator != null && mExpandAnimator.isRunning()) {
                mExpandAnimator.end();
            }
            mExpandAnimator = ValueAnimator.ofFloat(ANIMATION_START_VALUE, ANIMATION_END_VALUE).setDuration(ANIMATION_DURATION);
            mExpandAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    ConstraintLayout.LayoutParams flLandmarksParams = (ConstraintLayout.LayoutParams) mFlHumanLandmarks.getLayoutParams();
                    float animatedValue = (float) animation.getAnimatedValue();
                    int height = DensityUtils.dp2px(mBaseGlActivity, animatedValue);
                    if (height > 0) {
                        flLandmarksParams.height = height;
                    }
                    mFlHumanLandmarks.setLayoutParams(flLandmarksParams);
                    ConstraintLayout.LayoutParams flGestureOutlineParams = (ConstraintLayout.LayoutParams) mFlGestureOutline.getLayoutParams();
                    flGestureOutlineParams.topMargin = mPx24 + DensityUtils.dp2px(mBaseGlActivity, animation.getAnimatedFraction() * OUTLINE_MARGIN);
                    mFlGestureOutline.setLayoutParams(flGestureOutlineParams);
                }
            });
            mExpandAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    mFlHumanLandmarks.setVisibility(View.VISIBLE);
                    if (mTvLandmarksFull.isSelected()) {
                        if (mHumanType == HUMAN_TYPE_LANDMARKS) {
                            mBaseGlActivity.getFuRenderer().unselectEffect(mHalfEffect);
                            mBaseGlActivity.getFuRenderer().selectEffect(mFullEffect);
                        } else if (mHumanType == HUMAN_TYPE_SKELETON) {
                            mBaseGlActivity.setRenderMode(FURenderer.RENDER_MODE_CONTROLLER);
                            mBaseGlActivity.getFuRenderer().setHumanTrackScene(FURenderer.HUMAN_TRACK_SCENE_FULL);
                        }
                    } else {
                        if (mHumanType == HUMAN_TYPE_LANDMARKS) {
                            mBaseGlActivity.getFuRenderer().unselectEffect(mFullEffect);
                            mBaseGlActivity.getFuRenderer().selectEffect(mHalfEffect);
                        } else if (mHumanType == HUMAN_TYPE_SKELETON) {
                            mBaseGlActivity.setRenderMode(FURenderer.RENDER_MODE_CONTROLLER);
                            mBaseGlActivity.getFuRenderer().setHumanTrackScene(FURenderer.HUMAN_TRACK_SCENE_HALF);
                            mBaseGlActivity.getFuRenderer().resetTrackStatus();
                        }
                    }
                }
            });
            mExpandAnimator.start();
        }

        private void shrink() {
            if (mShrinkAnimator != null && mShrinkAnimator.isRunning()) {
                mShrinkAnimator.end();
            }
            mShrinkAnimator = ValueAnimator.ofFloat(ANIMATION_END_VALUE, ANIMATION_START_VALUE).setDuration(ANIMATION_DURATION);
            mShrinkAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    ConstraintLayout.LayoutParams flLandmarksParams = (ConstraintLayout.LayoutParams) mFlHumanLandmarks.getLayoutParams();
                    float animatedValue = (float) animation.getAnimatedValue();
                    int height = DensityUtils.dp2px(mBaseGlActivity, animatedValue);
                    if (height > 0) {
                        flLandmarksParams.height = height;
                    }
                    mFlHumanLandmarks.setLayoutParams(flLandmarksParams);

                    ConstraintLayout.LayoutParams flGestureOutlineParams = (ConstraintLayout.LayoutParams) mFlGestureOutline.getLayoutParams();
                    // 72dp -- 24dp = 48dp
                    flGestureOutlineParams.topMargin = mPx72 - DensityUtils.dp2px(mBaseGlActivity, animation.getAnimatedFraction() * OUTLINE_MARGIN);
                    mFlGestureOutline.setLayoutParams(flGestureOutlineParams);
                }
            });
            mShrinkAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    mFlHumanLandmarks.setVisibility(View.GONE);
                    if (mHumanType == HUMAN_TYPE_LANDMARKS) {
                        mBaseGlActivity.getFuRenderer().unselectEffect(mFullEffect);
                        mBaseGlActivity.getFuRenderer().unselectEffect(mHalfEffect);
                    } else if (mHumanType == HUMAN_TYPE_SKELETON) {
                        mBaseGlActivity.setRenderMode(FURenderer.RENDER_MODE_NORMAL);
                    }
                    resetViewLandmarks();
                }
            });
            mShrinkAnimator.start();
        }

    }

    private void clickEffect(Effect effect, boolean enable) {
        if (enable) {
            mBaseGlActivity.getFuRenderer().selectEffect(effect);
            if (mBaseGlActivity.getFaceLandmarksEffect() != null && effect.getDescription().equals(getResources().getString(R.string.config_item_face_landmarks_75))) {
                mBaseGlActivity.nullFaceLandmarksEffect();
            }
        } else {
            mBaseGlActivity.getFuRenderer().unselectEffect(effect);
        }
    }

    private void dismissSelf() {
        FragmentTransaction fragmentTransaction = mBaseGlActivity.getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_right);
        fragmentTransaction.hide(ConfigFragment.this).commit();
        mBaseGlActivity.setMaskVisibility(false);
    }

    private void handleEffectSelection(List<Effect> effects, SparseArray<Effect> selectedEffects) {
        Set<Effect> selectedSet = new HashSet<>();
        for (int i = 0, size = selectedEffects.size(); i < size; i++) {
            selectedSet.add(selectedEffects.valueAt(i));
        }
        List<Effect> unselectedEffects = new ArrayList<>();
        for (Effect effect : effects) {
            if (!selectedSet.contains(effect)) {
                unselectedEffects.add(effect);
            }
        }
        for (Effect effect : unselectedEffects) {
            mBaseGlActivity.getFuRenderer().unselectEffect(effect);
        }
    }

    private void setLandmarksTextSelected(boolean fullSelected) {
        if (fullSelected) {
            mTvLandmarksFull.setSelected(true);
            mTvLandmarksFull.setTypeface(Typeface.DEFAULT_BOLD);
            mTvLandmarksHalf.setSelected(false);
            mTvLandmarksHalf.setTypeface(Typeface.DEFAULT);
        } else {
            mTvLandmarksFull.setSelected(false);
            mTvLandmarksFull.setTypeface(Typeface.DEFAULT);
            mTvLandmarksHalf.setSelected(true);
            mTvLandmarksHalf.setTypeface(Typeface.DEFAULT_BOLD);
        }
    }

    private void resetViewLandmarks() {
        mViewClickListener.mIsSelectLeft = true;
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mIvLandmarksBg.getLayoutParams();
        layoutParams.leftMargin = DensityUtils.dp2px(mBaseGlActivity, ViewClickListener.ANIMATION_START_VALUE);
        mIvLandmarksBg.setLayoutParams(layoutParams);
        setLandmarksTextSelected(true);
    }

    private class ViewClickListener extends OnMultiClickListener {
        private static final float ANIMATION_START_VALUE = 4F;
        private static final float ANIMATION_END_VALUE = 136F;
        private static final int ANIMATION_DURATION = 200;
        private ValueAnimator mLeftToRightAnimator;
        private ValueAnimator mRightToLeftAnimator;
        private boolean mIsSelectLeft = true;

        @Override
        protected void onMultiClick(final View v) {
            switch (v.getId()) {
                case R.id.fl_config_mask:
                case R.id.btn_config_confirm: {
                    dismissSelf();
                }
                break;
                case R.id.btn_config_reset: {
                    mFaceRecyclerAdapter.clearMultiItemSelected();
                    mHumanRecyclerAdapter.clearMultiItemSelected();
                    mRecyclerItemClickListener.clickLandmarks();
                    mGestureRecyclerAdapter.clearMultiItemSelected();
                    mSegRecyclerAdapter.clearMultiItemSelected();
                    mActionRecyclerAdapter.clearMultiItemSelected();

                    handleEffectSelection(mEffectMap.get(Effect.TYPE_FACE), mFaceRecyclerAdapter.getSelectedItems());
                    handleEffectSelection(mEffectMap.get(Effect.TYPE_HUMAN), mHumanRecyclerAdapter.getSelectedItems());
                    handleEffectSelection(mEffectMap.get(Effect.TYPE_GESTURE), mGestureRecyclerAdapter.getSelectedItems());
                    handleEffectSelection(mEffectMap.get(Effect.TYPE_SEGMENTATION), mSegRecyclerAdapter.getSelectedItems());
                    handleEffectSelection(mEffectMap.get(Effect.TYPE_ACTION), mActionRecyclerAdapter.getSelectedItems());
                    // reselect aitype
                    mBaseGlActivity.getFuRenderer().selectEffect(new Effect(mBaseGlActivity.getAiTypeEffect()));

                    enableRecyclerItem(Effect.TYPE_FACE, mFaceRecyclerAdapter);
                    enableRecyclerItem(Effect.TYPE_HUMAN, mHumanRecyclerAdapter);
                    enableRecyclerItem(Effect.TYPE_GESTURE, mGestureRecyclerAdapter);
                    enableRecyclerItem(Effect.TYPE_SEGMENTATION, mSegRecyclerAdapter);
                    enableRecyclerItem(Effect.TYPE_ACTION, mActionRecyclerAdapter);
                    mBaseGlActivity.setRenderMode(FURenderer.RENDER_MODE_NORMAL);
                    resetViewLandmarks();

                    mBaseGlActivity.setRecognitionRecyclerVisibility(BaseGlActivity.RECOGNITION_TYPE_GESTURE, false);
                    mBaseGlActivity.setRecognitionRecyclerVisibility(BaseGlActivity.RECOGNITION_TYPE_ACTION, false);
                    mBaseGlActivity.setExpressionRecyclerVisibility(false);
                    mBaseGlActivity.setTongueTrackRecyclerVisibility(false);
                }
                break;
                case R.id.tv_config_human_full: {
                    if (mIsSelectLeft) {
                        break;
                    }
                    mIsSelectLeft = true;
                    if (mLeftToRightAnimator != null && mLeftToRightAnimator.isRunning()) {
                        mLeftToRightAnimator.end();
                    }
                    mRecyclerItemClickListener.mHumanState = RecyclerItemClickListener.HUMAN_STATE_FULL;
                    mLeftToRightAnimator = ValueAnimator.ofFloat(ANIMATION_END_VALUE, ANIMATION_START_VALUE).setDuration(ANIMATION_DURATION);
                    mLeftToRightAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            float value = (float) animation.getAnimatedValue();
                            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mIvLandmarksBg.getLayoutParams();
                            layoutParams.leftMargin = DensityUtils.dp2px(mBaseGlActivity, value);
                            mIvLandmarksBg.setLayoutParams(layoutParams);
                        }
                    });
                    mLeftToRightAnimator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            setLandmarksTextSelected(true);
                            if (mHumanType == HUMAN_TYPE_LANDMARKS) {
                                mBaseGlActivity.getFuRenderer().unselectEffect(mHalfEffect);
                                mBaseGlActivity.getFuRenderer().selectEffect(mFullEffect);
                            } else if (mHumanType == HUMAN_TYPE_SKELETON) {
                                mBaseGlActivity.getFuRenderer().setHumanTrackScene(FURenderer.HUMAN_TRACK_SCENE_FULL);
                                mBaseGlActivity.getFuRenderer().resetTrackStatus();
                            }
                        }
                    });
                    mLeftToRightAnimator.start();
                }
                break;
                case R.id.tv_config_human_half: {
                    if (!mIsSelectLeft) {
                        break;
                    }
                    mIsSelectLeft = false;
                    if (mRightToLeftAnimator != null && mRightToLeftAnimator.isRunning()) {
                        mRightToLeftAnimator.end();
                    }
                    mRecyclerItemClickListener.mHumanState = RecyclerItemClickListener.HUMAN_STATE_HALF;
                    mRightToLeftAnimator = ValueAnimator.ofFloat(ANIMATION_START_VALUE, ANIMATION_END_VALUE).setDuration(ANIMATION_DURATION);
                    mRightToLeftAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            float value = (float) animation.getAnimatedValue();
                            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mIvLandmarksBg.getLayoutParams();
                            layoutParams.leftMargin = DensityUtils.dp2px(mBaseGlActivity, value);
                            mIvLandmarksBg.setLayoutParams(layoutParams);
                        }
                    });
                    mRightToLeftAnimator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            setLandmarksTextSelected(false);
                            if (mHumanType == HUMAN_TYPE_LANDMARKS) {
                                mBaseGlActivity.getFuRenderer().unselectEffect(mFullEffect);
                                mBaseGlActivity.getFuRenderer().selectEffect(mHalfEffect);
                            } else if (mHumanType == HUMAN_TYPE_SKELETON) {
                                mBaseGlActivity.getFuRenderer().setHumanTrackScene(FURenderer.HUMAN_TRACK_SCENE_HALF);
                                mBaseGlActivity.getFuRenderer().resetTrackStatus();
                            }
                        }
                    });
                    mRightToLeftAnimator.start();
                }
                break;
                default:
            }
        }

        private void enableRecyclerItem(int effectType, RecyclerViewAdapter recyclerViewAdapter) {
            List<Effect> effects = mEffectMap.get(effectType);
            for (Effect effect : effects) {
                if (effect.getState() != Effect.STATE_ENABLE) {
                    effect.setState(Effect.STATE_ENABLE);
                    recyclerViewAdapter.update(effect);
                }
            }
        }
    }

    private static class RecyclerViewAdapter extends BaseRecyclerAdapter<Effect> {

        RecyclerViewAdapter(@NonNull List<Effect> data) {
            super(data, R.layout.rv_config_classification);
        }

        @Override
        protected void bindViewHolder(BaseViewHolder viewHolder, Effect item) {
            TextView itemView = (TextView) viewHolder.itemView;
            itemView.setText(item.getDescription());
            itemView.setEnabled(item.getState() == Effect.STATE_ENABLE);
        }

        @Override
        protected int choiceMode() {
            return BaseRecyclerAdapter.MULTI_CHOICE_MODE;
        }

        @Override
        protected void handleSelectedState(BaseViewHolder viewHolder, Effect data, boolean selected) {
            super.handleSelectedState(viewHolder, data, selected);
            ((TextView) viewHolder.itemView).setTypeface(selected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        }
    }

    private void initListData(boolean containHumanAvatar) {
        Resources resources = mBaseGlActivity.getResources();
        Map<Integer, List<Effect>> effectMap = new HashMap<>(16);

        List<Effect> faceEffects = new ArrayList<>();
        faceEffects.add(mBaseGlActivity.getFaceLandmarksEffect());
        faceEffects.add(new Effect(FACE_TONGUE_BUNDLE_PATH, resources.getString(R.string.config_item_face_tongue), Effect.TYPE_FACE));
        faceEffects.add(mBaseGlActivity.getAiTypeEffect());
        effectMap.put(Effect.TYPE_FACE, Collections.unmodifiableList(faceEffects));

        List<Effect> humanEffects = new ArrayList<>();
        humanEffects.add(new Effect("", resources.getString(R.string.config_item_human_landmark), Effect.TYPE_HUMAN));
        if (containHumanAvatar) {
            humanEffects.add(new Effect("", resources.getString(R.string.config_item_human_skeleton), Effect.TYPE_HUMAN));
        }
        effectMap.put(Effect.TYPE_HUMAN, Collections.unmodifiableList(humanEffects));

        List<Effect> gestureEffects = new ArrayList<>();
        gestureEffects.add(new Effect(HUMAN_GESTURE_BUNDLE_PATH, resources.getString(R.string.config_item_gesture_recognition), Effect.TYPE_GESTURE));
        effectMap.put(Effect.TYPE_GESTURE, Collections.unmodifiableList(gestureEffects));

        List<Effect> segEffects = new ArrayList<>();
        segEffects.add(new Effect(HUMAN_MASK_BUNDLE_PATH, resources.getString(R.string.config_item_seg_portrait), Effect.TYPE_SEGMENTATION));
        segEffects.add(new Effect(HAIR_MASK_BUNDLE_PATH, resources.getString(R.string.config_item_seg_hair), Effect.TYPE_SEGMENTATION));
        segEffects.add(new Effect(HEAD_MASK_BUNDLE_PATH, resources.getString(R.string.config_item_seg_head), Effect.TYPE_SEGMENTATION));
        effectMap.put(Effect.TYPE_SEGMENTATION, Collections.unmodifiableList(segEffects));

        List<Effect> actionEffects = new ArrayList<>();
        actionEffects.add(new Effect(HUMAN_ACTION_BUNDLE_PATH, resources.getString(R.string.config_item_action_recognition), Effect.TYPE_ACTION));
        effectMap.put(Effect.TYPE_ACTION, Collections.unmodifiableList(actionEffects));
        mEffectMap = Collections.unmodifiableMap(effectMap);

        mHalfEffect = new Effect(HALF_BODY_LANDMARKS_BUNDLE_PATH);
        mFullEffect = new Effect(FULL_BODY_LANDMARKS_BUNDLE_PATH);

        Map<Effect, List<Effect>> mutexMap = new HashMap<>(16);
        List<Effect> mutexEffect = new ArrayList<>();
        if (humanEffects.size() > 1) {
            mutexEffect.add(humanEffects.get(1));
        }
        mutexMap.put(faceEffects.get(0), Collections.unmodifiableList(mutexEffect));

        mutexEffect = new ArrayList<>();
        if (humanEffects.size() > 1) {
            mutexEffect.add(humanEffects.get(1));
        }
        mutexMap.put(faceEffects.get(1), Collections.unmodifiableList(mutexEffect));

        mutexEffect = new ArrayList<>();
        if (humanEffects.size() > 1) {
            mutexEffect.add(humanEffects.get(1));
        }
        mutexMap.put(faceEffects.get(2), Collections.unmodifiableList(mutexEffect));

        mutexEffect = new ArrayList<>();
        if (humanEffects.size() > 1) {
            mutexEffect.add(humanEffects.get(1));
        }
        mutexEffect.addAll(gestureEffects);
        mutexEffect.addAll(segEffects);
        mutexMap.put(humanEffects.get(0), Collections.unmodifiableList(mutexEffect));

        if (containHumanAvatar) {
            mutexEffect = new ArrayList<>(faceEffects);
            mutexEffect.add(humanEffects.get(0));
            mutexEffect.addAll(gestureEffects);
            mutexEffect.addAll(segEffects);
            mutexEffect.addAll(actionEffects);
            if (humanEffects.size() > 1) {
                mutexMap.put(humanEffects.get(1), Collections.unmodifiableList(mutexEffect));
            }
        }

        mutexEffect = new ArrayList<>();
        mutexEffect.addAll(humanEffects);
        mutexEffect.addAll(segEffects);
        mutexEffect.addAll(actionEffects);
        mutexMap.put(gestureEffects.get(0), Collections.unmodifiableList(mutexEffect));

        mutexEffect = new ArrayList<>();
        mutexEffect.addAll(humanEffects);
        mutexEffect.addAll(gestureEffects);
        mutexEffect.addAll(actionEffects);
        mutexEffect.add(segEffects.get(1));
        mutexEffect.add(segEffects.get(2));
        mutexMap.put(segEffects.get(0), Collections.unmodifiableList(mutexEffect));

        mutexEffect = new ArrayList<>();
        mutexEffect.addAll(humanEffects);
        mutexEffect.addAll(gestureEffects);
        mutexEffect.addAll(actionEffects);
        mutexEffect.add(segEffects.get(0));
        mutexEffect.add(segEffects.get(2));
        mutexMap.put(segEffects.get(1), Collections.unmodifiableList(mutexEffect));

        mutexEffect = new ArrayList<>();
        mutexEffect.addAll(humanEffects);
        mutexEffect.addAll(gestureEffects);
        mutexEffect.addAll(actionEffects);
        mutexEffect.add(segEffects.get(0));
        mutexEffect.add(segEffects.get(1));
        mutexMap.put(segEffects.get(2), Collections.unmodifiableList(mutexEffect));

        mutexEffect = new ArrayList<>();
        if (humanEffects.size() > 1) {
            mutexEffect.add(humanEffects.get(1));
        }
        mutexEffect.addAll(gestureEffects);
        mutexEffect.addAll(segEffects);
        mutexMap.put(actionEffects.get(0), Collections.unmodifiableList(mutexEffect));
        mMutexMap = Collections.unmodifiableMap(mutexMap);
    }

}
