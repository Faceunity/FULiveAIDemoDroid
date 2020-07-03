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
import androidx.recyclerview.widget.LinearLayoutManager;
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
 * @author Richie on 2020.05.21
 */
public class ConfigFragment extends Fragment {
    public static final String TAG = "ConfigFragment";
    private static final String GRAPHICS_DIR = "others/";
    private static final String HUMAN_MASK_BUNDLE_PATH = GRAPHICS_DIR + "human_mask.bundle";
    private static final String HUMAN_ACTION_BUNDLE_PATH = GRAPHICS_DIR + "human_action.bundle";
    private static final String HUMAN_GESTURE_BUNDLE_PATH = GRAPHICS_DIR + "human_gesture.bundle";
    private static final String HAIR_MASK_BUNDLE_PATH = GRAPHICS_DIR + "hair_normal_algorithm.bundle";
    private static final String HEAD_MASK_BUNDLE_PATH = GRAPHICS_DIR + "head_mask.bundle";
    private static final String FULL_BODY_LANDMARKS_BUNDLE_PATH = GRAPHICS_DIR + "bodyLandmarks_dance.bundle";
    private static final String HALF_BODY_LANDMARKS_BUNDLE_PATH = GRAPHICS_DIR + "bodyLandmarks_selife.bundle";

    /**
     * 人体检测绘制类型，关键点和骨骼
     */
    private static final int HUMAN_TYPE_LANDMARKS = 1;
    private static final int HUMAN_TYPE_SKELETON = 2;

    private BaseGlActivity mBaseGlActivity;
    private HumanRecyclerAdapter mHumanRecyclerAdapter;
    private HumanRecyclerAdapter mGestureRecyclerAdapter;
    private HumanRecyclerAdapter mSegRecyclerAdapter;
    private HumanRecyclerAdapter mActionRecyclerAdapter;
    private HumanRecyclerClickListener mHumanRecyclerClickListener;
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

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mBaseGlActivity = (BaseGlActivity) context;
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

        FrameLayout flHumanOutline = view.findViewById(R.id.fl_config_human_outline);
        ((ImageView) flHumanOutline.findViewById(R.id.iv_config_outline_img)).setImageResource(R.drawable.demo_sidebar_icon_body);
        ((TextView) flHumanOutline.findViewById(R.id.tv_config_outline_text)).setText(R.string.config_sub_title_human);
        RecyclerView rvConfigHuman = view.findViewById(R.id.rv_config_human);
        initRecyclerView(rvConfigHuman);
        List<Effect> humanEffects = mEffectMap.get(Effect.TYPE_HUMAN);
        mHumanRecyclerAdapter = new HumanRecyclerAdapter(new ArrayList<>(humanEffects));
        mHumanRecyclerClickListener = new HumanRecyclerClickListener();
        mHumanRecyclerAdapter.setOnItemClickListener(mHumanRecyclerClickListener);
        rvConfigHuman.setAdapter(mHumanRecyclerAdapter);

        FrameLayout flGestureOutline = view.findViewById(R.id.fl_config_gesture_outline);
        ((ImageView) flGestureOutline.findViewById(R.id.iv_config_outline_img)).setImageResource(R.drawable.demo_sidebar_icon_gesture);
        ((TextView) flGestureOutline.findViewById(R.id.tv_config_outline_text)).setText(R.string.config_sub_title_gesture);
        RecyclerView rvConfigGesture = view.findViewById(R.id.rv_config_gesture);
        initRecyclerView(rvConfigGesture);
        List<Effect> gestureEffects = mEffectMap.get(Effect.TYPE_GESTURE);
        mGestureRecyclerAdapter = new HumanRecyclerAdapter(new ArrayList<>(gestureEffects));
        mGestureRecyclerAdapter.setOnItemClickListener(mHumanRecyclerClickListener);
        rvConfigGesture.setAdapter(mGestureRecyclerAdapter);
        mFlGestureOutline = flGestureOutline;

        FrameLayout flSegOutline = view.findViewById(R.id.fl_config_seg_outline);
        ((ImageView) flSegOutline.findViewById(R.id.iv_config_outline_img)).setImageResource(R.drawable.demo_sidebar_icon_segmentation);
        ((TextView) flSegOutline.findViewById(R.id.tv_config_outline_text)).setText(R.string.config_sub_title_seg);
        RecyclerView rvConfigSeg = view.findViewById(R.id.rv_config_seg);
        initRecyclerView(rvConfigSeg);
        List<Effect> segEffects = mEffectMap.get(Effect.TYPE_SEGMENTATION);
        mSegRecyclerAdapter = new HumanRecyclerAdapter(new ArrayList<>(segEffects));
        mSegRecyclerAdapter.setOnItemClickListener(mHumanRecyclerClickListener);
        rvConfigSeg.setAdapter(mSegRecyclerAdapter);

        FrameLayout flActionOutline = view.findViewById(R.id.fl_config_action_outline);
        ((ImageView) flActionOutline.findViewById(R.id.iv_config_outline_img)).setImageResource(R.drawable.demo_sidebar_icon_action);
        ((TextView) flActionOutline.findViewById(R.id.tv_config_outline_text)).setText(R.string.config_sub_title_action);
        RecyclerView rvConfigAction = view.findViewById(R.id.rv_config_action);
        initRecyclerView(rvConfigAction);
        List<Effect> actionEffects = mEffectMap.get(Effect.TYPE_ACTION);
        mActionRecyclerAdapter = new HumanRecyclerAdapter(new ArrayList<>(actionEffects));
        mActionRecyclerAdapter.setOnItemClickListener(mHumanRecyclerClickListener);
        rvConfigAction.setAdapter(mActionRecyclerAdapter);
        return view;
    }

    private void initRecyclerView(RecyclerView recyclerView) {
        int rvItemHorizonSpace = DensityUtils.dp2px(mBaseGlActivity, 4);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setHasFixedSize(true);
        recyclerView.addItemDecoration(new SpaceItemDecoration(rvItemHorizonSpace, 0));
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
    }

    public int getSelectedTrackType() {
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
        if (mGestureRecyclerAdapter.getSelectedItems().size() > 0) {
            return FURenderer.TRACK_TYPE_GESTURE;
        }
        boolean isSelectedHead = false;
        SparseArray<Effect> selectedItems = mSegRecyclerAdapter.getSelectedItems();
        if (selectedItems.size() > 0) {
            Effect effect = selectedItems.valueAt(0);
            String description = effect.getDescription();
            if (getResources().getString(R.string.config_item_seg_hair).equals(description)
                    || getResources().getString(R.string.config_item_seg_head).equals(description)) {
                isSelectedHead = true;
            }
        }
        if (isSelectedHead) {
            return FURenderer.TRACK_TYPE_FACE;
        }
        return 0;
    }

    public int getToastStringId() {
        boolean isSelectedHumanFull = mHumanRecyclerClickListener.mHumanState == HumanRecyclerClickListener.HUMAN_STATE_FULL;
        boolean isSelectAction = mActionRecyclerAdapter.getSelectedItems().size() > 0;
        boolean isSelectedHead = false;
        boolean isSelectedHuman = false;
        SparseArray<Effect> selectedItems = mSegRecyclerAdapter.getSelectedItems();
        if (selectedItems.size() > 0) {
            Effect effect = selectedItems.valueAt(0);
            String description = effect.getDescription();
            if (getResources().getString(R.string.config_item_seg_hair).equals(description)
                    || getResources().getString(R.string.config_item_seg_head).equals(description)) {
                isSelectedHead = true;
            } else {
                isSelectedHuman = true;
            }
        }
        boolean isSelectedHumanHalf = mHumanRecyclerClickListener.mHumanState == HumanRecyclerClickListener.HUMAN_STATE_HALF;
        if (isSelectedHumanFull && !isSelectAction && !isSelectedHuman) {
            return R.string.track_status_no_human_full;
        }
        if (isSelectAction || isSelectedHumanHalf) {
            return R.string.track_status_no_human;
        }
        boolean isSelectGesture = mGestureRecyclerAdapter.getSelectedItems().size() > 0;
        if (isSelectGesture) {
            return R.string.track_status_no_gesture;
        }
        if (isSelectedHead) {
            return R.string.track_status_no_face;
        }
        if (isSelectedHuman) {
            return R.string.track_status_no_human;
        }
        return 0;
    }

    private class HumanRecyclerClickListener implements BaseRecyclerAdapter.OnItemClickListener<Effect> {
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
        // 人体检测、人体关键点、动作识别3个功能可以共用，都只能检测1人
        @Override
        public void onItemClick(BaseRecyclerAdapter<Effect> adapter, View view, int position) {
            Effect item = adapter.getItem(position);
            boolean isSelected = view.isSelected();
            boolean refresh = true; // mark whether to change view enabled/disable state
            String description = item.getDescription();
            boolean selectLandmarks = getResources().getString(R.string.config_item_human_landmark).equals(description);
            if (selectLandmarks) {
                int index = mActionRecyclerAdapter.getSelectedItems().indexOfValue(mEffectMap.get(Effect.TYPE_ACTION).get(0));
                if (index >= 0) {
                    refresh = false;
                }
            } else if (getResources().getString(R.string.config_item_action_recognize).equals(description)) {
                int index = mHumanRecyclerAdapter.getSelectedItems().indexOfValue(mEffectMap.get(Effect.TYPE_HUMAN).get(0));
                if (index >= 0) {
                    refresh = false;
                }
            }
            if (refresh) {
                List<Effect> mutexEffects = mMutexMap.get(item);
                if (mutexEffects != null) {
                    for (Effect mutexEffect : mutexEffects) {
                        mutexEffect.setState(isSelected ? Effect.STATE_ENABLE : Effect.STATE_DISABLE);
                        mHumanRecyclerAdapter.update(mutexEffect);
                        mGestureRecyclerAdapter.update(mutexEffect);
                        mSegRecyclerAdapter.update(mutexEffect);
                        mActionRecyclerAdapter.update(mutexEffect);
                    }
                }
            }

            boolean selectSkeleton = getResources().getString(R.string.config_item_human_skeleton).equals(description);
            if (selectLandmarks || selectSkeleton) {
                mHumanType = selectLandmarks ? HUMAN_TYPE_LANDMARKS : HUMAN_TYPE_SKELETON;
                if (mHumanState == HUMAN_STATE_FULL || mHumanState == HUMAN_STATE_HALF) {
                    shrink();
                    mHumanState = HUMAN_STATE_NONE;
                } else {
                    expand();
                    mHumanState = HUMAN_STATE_FULL;
                }
            } else {
                if (getResources().getString(R.string.config_item_action_recognize).equals(description)) {
                    mBaseGlActivity.changeRecyclerVisibility(BaseGlActivity.RESOURCE_TYPE_ACTION, !isSelected);
                } else if (getResources().getString(R.string.config_item_gesture_recognize).equals(description)) {
                    mBaseGlActivity.changeRecyclerVisibility(BaseGlActivity.RESOURCE_TYPE_GESTURE, !isSelected);
                }
                clickEffect(item, isSelected);
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

    private void clickEffect(Effect effect, boolean isSelect) {
        if (isSelect) {
            mBaseGlActivity.getFuRenderer().unselectEffect(effect);
        } else {
            mBaseGlActivity.getFuRenderer().selectEffect(effect);
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
                    mHumanRecyclerAdapter.clearMultiItemSelected();
                    mHumanRecyclerClickListener.clickLandmarks();
                    mGestureRecyclerAdapter.clearMultiItemSelected();
                    mSegRecyclerAdapter.clearMultiItemSelected();
                    mActionRecyclerAdapter.clearMultiItemSelected();
                    handleEffectSelection(mEffectMap.get(Effect.TYPE_HUMAN), mHumanRecyclerAdapter.getSelectedItems());
                    handleEffectSelection(mEffectMap.get(Effect.TYPE_GESTURE), mGestureRecyclerAdapter.getSelectedItems());
                    handleEffectSelection(mEffectMap.get(Effect.TYPE_SEGMENTATION), mSegRecyclerAdapter.getSelectedItems());
                    handleEffectSelection(mEffectMap.get(Effect.TYPE_ACTION), mActionRecyclerAdapter.getSelectedItems());

                    List<Effect> effects = mEffectMap.get(Effect.TYPE_HUMAN);
                    for (Effect effect : effects) {
                        if (effect.getState() != Effect.STATE_ENABLE) {
                            effect.setState(Effect.STATE_ENABLE);
                            mHumanRecyclerAdapter.update(effect);
                        }
                    }
                    effects = mEffectMap.get(Effect.TYPE_GESTURE);
                    for (Effect effect : effects) {
                        if (effect.getState() != Effect.STATE_ENABLE) {
                            effect.setState(Effect.STATE_ENABLE);
                            mGestureRecyclerAdapter.update(effect);
                        }
                    }
                    effects = mEffectMap.get(Effect.TYPE_SEGMENTATION);
                    for (Effect effect : effects) {
                        if (effect.getState() != Effect.STATE_ENABLE) {
                            effect.setState(Effect.STATE_ENABLE);
                            mSegRecyclerAdapter.update(effect);
                        }
                    }
                    effects = mEffectMap.get(Effect.TYPE_ACTION);
                    for (Effect effect : effects) {
                        if (effect.getState() != Effect.STATE_ENABLE) {
                            effect.setState(Effect.STATE_ENABLE);
                            mActionRecyclerAdapter.update(effect);
                        }
                    }
                    mBaseGlActivity.setRenderMode(FURenderer.RENDER_MODE_NORMAL);
                    resetViewLandmarks();
                    mBaseGlActivity.changeRecyclerVisibility(BaseGlActivity.RESOURCE_TYPE_GESTURE, false);
                    mBaseGlActivity.changeRecyclerVisibility(BaseGlActivity.RESOURCE_TYPE_ACTION, false);
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
                    mHumanRecyclerClickListener.mHumanState = HumanRecyclerClickListener.HUMAN_STATE_FULL;
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
                    mHumanRecyclerClickListener.mHumanState = HumanRecyclerClickListener.HUMAN_STATE_HALF;
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
    }

    private static class HumanRecyclerAdapter extends BaseRecyclerAdapter<Effect> {

        HumanRecyclerAdapter(@NonNull List<Effect> data) {
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

    public void initListData(Context context, boolean containHumanAvatar) {
        Resources resources = context.getResources();
        Map<Integer, List<Effect>> effectMap = new HashMap<>(16);

        List<Effect> humanEffects = new ArrayList<>();
        humanEffects.add(new Effect("", resources.getString(R.string.config_item_human_landmark), Effect.TYPE_HUMAN));
        if (containHumanAvatar) {
            humanEffects.add(new Effect("", resources.getString(R.string.config_item_human_skeleton), Effect.TYPE_HUMAN));
        }
        effectMap.put(Effect.TYPE_HUMAN, Collections.unmodifiableList(humanEffects));

        List<Effect> gestureEffects = new ArrayList<>();
        gestureEffects.add(new Effect(HUMAN_GESTURE_BUNDLE_PATH, resources.getString(R.string.config_item_gesture_recognize), Effect.TYPE_GESTURE));
        effectMap.put(Effect.TYPE_GESTURE, Collections.unmodifiableList(gestureEffects));

        List<Effect> segEffects = new ArrayList<>();
        segEffects.add(new Effect(HUMAN_MASK_BUNDLE_PATH, resources.getString(R.string.config_item_seg_portrait), Effect.TYPE_SEGMENTATION));
        segEffects.add(new Effect(HAIR_MASK_BUNDLE_PATH, resources.getString(R.string.config_item_seg_hair), Effect.TYPE_SEGMENTATION));
        segEffects.add(new Effect(HEAD_MASK_BUNDLE_PATH, resources.getString(R.string.config_item_seg_head), Effect.TYPE_SEGMENTATION));
        effectMap.put(Effect.TYPE_SEGMENTATION, Collections.unmodifiableList(segEffects));

        List<Effect> actionEffects = new ArrayList<>();
        actionEffects.add(new Effect(HUMAN_ACTION_BUNDLE_PATH, resources.getString(R.string.config_item_action_recognize), Effect.TYPE_ACTION));
        effectMap.put(Effect.TYPE_ACTION, Collections.unmodifiableList(actionEffects));
        mEffectMap = Collections.unmodifiableMap(effectMap);

        mHalfEffect = new Effect(HALF_BODY_LANDMARKS_BUNDLE_PATH);
        mFullEffect = new Effect(FULL_BODY_LANDMARKS_BUNDLE_PATH);

        Map<Effect, List<Effect>> mutexMap = new HashMap<>(16);
        List<Effect> mutexEffect = new ArrayList<>();
        if (humanEffects.size() > 1) {
            mutexEffect.add(humanEffects.get(1));
        }
        mutexEffect.addAll(gestureEffects);
        mutexEffect.addAll(segEffects);
        mutexEffect = new ArrayList<>(mutexEffect);
        mutexMap.put(humanEffects.get(0), Collections.unmodifiableList(mutexEffect));

        if (containHumanAvatar) {
            mutexEffect = new ArrayList<>();
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
