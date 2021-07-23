package com.faceunity.nama.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.faceunity.nama.Effect;
import com.faceunity.nama.R;
import com.faceunity.nama.factory.AvatarDataFactory;
import com.faceunity.nama.factory.EffectSepMultipleFactory;
import com.faceunity.nama.factory.EffectSpeDataFactory;
import com.faceunity.nama.view.adapter.BaseRecyclerAdapter;
import com.faceunity.nama.view.listener.TypeEnum;
import com.faceunity.nama.widget.SwiButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


/**
 * 特殊选择效果
 * 用于 人体效果
 * 人体模式（effect） 骨骼模式（avatar）
 */
public class EffectSpeControllerView extends BaseEffectControllerView {
    private RecyclerItemClickListener mRecyclerItemClickListener;
    /*初始化数据工厂*/
    private EffectSpeDataFactory mEffectSpeDataFactory;
    /*avatar*/
    private AvatarDataFactory mAvatarDataFactory;

    /*展开动画*/
    private ValueAnimator mExpandAnimator;
    /*关闭动画*/
    private ValueAnimator mShrinkAnimator;
    private static final float ANIMATION_START_VALUE = 0.5F;
    private static final float ANIMATION_END_VALUE = 40F;
    private static final int ANIMATION_DURATION = 200;

    private SwiButton swiButton;

    //此时使用的模式 效果模式 或者 AVTAR模式
    private TypeEnum typeEnum = TypeEnum.EFFECT;

    /*获取关键点效果列表*/
    public List<Effect> mLandMarkBeans = new ArrayList<>();

    /*获取骨骼效果列表*/
    public List<Effect> mSkeletonBeans = new ArrayList<>();

    public EffectSpeControllerView(Context context) {
        this(context, null);
    }

    public EffectSpeControllerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EffectSpeControllerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public int setContentViewRes() {
        return R.layout.layout_effct_spe_controller;
    }

    @Override
    public void initView() {
        swiButton = findViewById(R.id.swb_choice);
        mRecyclerItemClickListener = new RecyclerItemClickListener();
        mEffectRecyclerAdapter.setOnItemClickListener(mRecyclerItemClickListener);
        swiButton.setOnChoiceListener(buttonEnum -> {
            //选择按钮
            if (typeEnum == TypeEnum.EFFECT) {
                //效果模式
                if (mChoiceEffectOrAvatarListener != null) {
                    mChoiceEffectOrAvatarListener.onSpeEffect(getChoiceLandMarkEffect(buttonEnum));
                }
            } else {
                //AVATAR模式
                if (mChoiceEffectOrAvatarListener != null)
                    mChoiceEffectOrAvatarListener.onSpeEffect(getChoiceSkeletonEffect(buttonEnum));
            }
        });
    }

    @Override
    public int getChoiceMode() {
        return BaseRecyclerAdapter.SINGLE_CAN_CANCEL_CHOICE_MODE;
    }

    /**
     * 处理点击逻辑
     * 按钮点击规则
     * 人脸 人体 手势 分割 动作 -->
     * 规则 -> 人脸
     * 选中 舌头 表情 情绪 自动选中人脸
     * 取消人脸 自动取消 舌头 表情 情绪
     * <p>
     * 其他都是单选规则
     */
    private class RecyclerItemClickListener implements BaseRecyclerAdapter.OnItemClickListener<Effect> {
        @Override
        public void onItemClick(BaseRecyclerAdapter<Effect> adapter, View view, int position) {
            //需要开启的效果选项
            Effect effect = adapter.getItem(position);
            //点击前是否选中
            boolean isSelected = !view.isSelected();

            if (effect == null) {
                return;
            }

            freshData(effect, isSelected);

            if (Effect.MODULE_CODE_HUMAN_SKELETON == effect.getAuthCode() && isSelected) {//人体骨骼
                typeEnum = TypeEnum.AVATAR;
            } else {//其他 应该都是加载Effect 效果的情况
                typeEnum = TypeEnum.EFFECT;
            }

            if (isSelected) {
                //选中 - 打开或者关闭人关键点、打开人体骨骼
                expand();
            } else {
                //未选中 - 关闭人体骨骼
                shrink(effect);
            }
        }
    }

    /**
     * 单按钮规则 刷新所有按钮的是否可点击情况
     *
     * @param effect   当前操作的effect
     * @param isSelect 当前选中 or 不选中
     */
    public void freshData(Effect effect, boolean isSelect) {
        if (mData == null || mData.isEmpty()) {
            return;
        }

        Iterator<Effect> iterator = mData.iterator();
        while (iterator.hasNext()) {
            Effect data = iterator.next();
            if (isSelect) {
                if (effect.equals(data)) {
                    data.setState(Effect.STATE_ENABLE);
                } else {
                    data.setState(Effect.STATE_DISABLE);
                }
            } else {
                data.setState(Effect.STATE_ENABLE);
            }
            mEffectRecyclerAdapter.update(data);
        }
    }

    /**
     * 展开 左右选择器
     */
    private void expand() {
        if (mExpandAnimator != null && mExpandAnimator.isRunning()) {
            mExpandAnimator.end();
        }
        mExpandAnimator = ValueAnimator.ofFloat(ANIMATION_START_VALUE, ANIMATION_END_VALUE).setDuration(ANIMATION_DURATION);
        mExpandAnimator.addUpdateListener(animation -> {
            LayoutParams swiButtonLayoutParams = (LayoutParams) swiButton.getLayoutParams();
            float animatedValue = (float) animation.getAnimatedValue();
            int height = dp2px(mContext, animatedValue);
            if (height > 0) {
                swiButtonLayoutParams.height = height;
            }
            swiButton.setLayoutParams(swiButtonLayoutParams);
        });
        mExpandAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                swiButton.setVisibility(VISIBLE);
                if (typeEnum == TypeEnum.EFFECT) {
                    //效果模式
                    if (mChoiceEffectOrAvatarListener != null) {
                        mChoiceEffectOrAvatarListener.onSpeEffect(getChoiceLandMarkEffect(swiButton.getChoiceButton()));
                    }
                } else {
                    //AVATAR模式
                    if (mChoiceEffectOrAvatarListener != null) {
                        mChoiceEffectOrAvatarListener.onSpeEffect(getChoiceSkeletonEffect(swiButton.getChoiceButton()));
                    }
                }
            }
        });
        mExpandAnimator.start();
    }

    /**
     * 快速展开 左右选择器 不通知外界
     */
    public void expandUnObserver() {
        if (mExpandAnimator != null && mExpandAnimator.isRunning()) {
            mExpandAnimator.end();
        }
        mExpandAnimator = ValueAnimator.ofFloat(ANIMATION_START_VALUE, ANIMATION_END_VALUE).setDuration(ANIMATION_DURATION);
        mExpandAnimator.addUpdateListener(animation -> {
            LayoutParams swiButtonLayoutParams = (LayoutParams) swiButton.getLayoutParams();
            float animatedValue = (float) animation.getAnimatedValue();
            int height = dp2px(mContext, animatedValue);
            if (height > 0) {
                swiButtonLayoutParams.height = height;
            }
            swiButton.setLayoutParams(swiButtonLayoutParams);
        });
        mExpandAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                swiButton.setVisibility(VISIBLE);
            }
        });
        mExpandAnimator.start();
    }

    /**
     * 合起 左右选择器
     */
    private void shrink(Effect effect) {
        if (mShrinkAnimator != null && mShrinkAnimator.isRunning()) {
            mShrinkAnimator.end();
        }
        mShrinkAnimator = ValueAnimator.ofFloat(ANIMATION_END_VALUE, ANIMATION_START_VALUE).setDuration(ANIMATION_DURATION);
        mShrinkAnimator.addUpdateListener(animation -> {
            LayoutParams swiButtonLayoutParams = (LayoutParams) swiButton.getLayoutParams();
            float animatedValue = (float) animation.getAnimatedValue();
            int height = dp2px(mContext, animatedValue);
            if (height > 0) {
                swiButtonLayoutParams.height = height;
            }
            swiButton.setLayoutParams(swiButtonLayoutParams);
        });
        mShrinkAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                swiButton.setVisibility(GONE);
                //将按钮设置到左边
                swiButton.rightToLeftQuick();
            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                if (mChoiceEffectOrAvatarListener != null) {
                    if (Effect.MODULE_CODE_HUMAN_SKELETON == effect.getAuthCode())//关闭人体骨骼
                        mChoiceEffectOrAvatarListener.onSpeEffect(null);
                    else //关闭人体关键点
                        mChoiceEffectOrAvatarListener.onSpeEffect(null);
                }
            }
        });
        mShrinkAnimator.start();
    }

    /**
     * 快速合起 左右选择器 不通知外界
     */
    public void shrinkUnObserver() {
        if (mShrinkAnimator != null && mShrinkAnimator.isRunning()) {
            mShrinkAnimator.end();
        }
        mShrinkAnimator = ValueAnimator.ofFloat(ANIMATION_END_VALUE, ANIMATION_START_VALUE).setDuration(ANIMATION_DURATION);
        mShrinkAnimator.addUpdateListener(animation -> {
            LayoutParams swiButtonLayoutParams = (LayoutParams) swiButton.getLayoutParams();
            float animatedValue = (float) animation.getAnimatedValue();
            int height = dp2px(mContext, animatedValue);
            if (height > 0) {
                swiButtonLayoutParams.height = height;
            }
            swiButton.setLayoutParams(swiButtonLayoutParams);
        });
        mShrinkAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                swiButton.setVisibility(GONE);
                //将按钮设置到左边
                swiButton.rightToLeftQuick();
            }
        });
        mShrinkAnimator.start();
    }

    public void bindEffectFactory(EffectSepMultipleFactory effectSpeDataFactory) {
        this.mEffectSpeDataFactory = effectSpeDataFactory.getEffectSpeDataFactory();
        this.mAvatarDataFactory = effectSpeDataFactory.getAvatarDataFactory();
        initListData();
    }

    /**
     * 初始化数据
     */
    private void initListData() {
        mData = mEffectSpeDataFactory.getEffectBeans();
        mLandMarkBeans = mEffectSpeDataFactory.getLandMarkEffectBeans();
        mSkeletonBeans = mEffectSpeDataFactory.getSkeletonBeans();
        mEffectRecyclerAdapter.setData(mData);

        HashMap<String, Integer> titleMap = mEffectSpeDataFactory.getTitleMap();
        @StringRes int titleTv = titleMap.get("title_tv");
        @DrawableRes int titleImg = titleMap.get("title_img");
        tvTitle.setText(titleTv);
        ivTitle.setImageResource(titleImg);
    }

    /**
     * 获取当前选中的人体关键点按钮
     */
    public Effect getChoiceLandMarkEffect(SwiButton.ButtonEnum buttonEnum) {
        if (buttonEnum == SwiButton.ButtonEnum.LEFT) {
            return mLandMarkBeans.get(0);
        } else {
            return mLandMarkBeans.get(1);
        }
    }

    /**
     * 获取当前选中的骨骼按钮
     */
    public Effect getChoiceSkeletonEffect(SwiButton.ButtonEnum buttonEnum) {
        if (buttonEnum == SwiButton.ButtonEnum.LEFT) {
            return mSkeletonBeans.get(0);
        } else {
            return mSkeletonBeans.get(1);
        }
    }

    /**
     * 根据选中的情况选尼玛的
     *
     * @param takeEffect
     */
    public void setTakeEffect(Effect takeEffect) {
        mEffectRecyclerAdapter.clearSingleItemSelected();
        if (takeEffect != null) {
            if (takeEffect.getType() == Effect.TYPE_HUMAN)
                mEffectRecyclerAdapter.setItemSelected(0);
            else {
                mEffectRecyclerAdapter.setItemSelected(1);
            }
            freshData(takeEffect, true);
            expandUnObserver();

            //设置按钮选中位置
            if (mLandMarkBeans.get(0).equals(takeEffect) || mSkeletonBeans.get(0).equals(takeEffect)) {
                swiButton.rightToLeftQuick();
            } else {
                swiButton.leftToRightQuick();
            }
        } else {
            shrinkUnObserver();
        }
    }
}
