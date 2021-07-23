package com.faceunity.nama.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.faceunity.nama.Effect;
import com.faceunity.nama.R;
import com.faceunity.nama.factory.EffectFactory;
import com.faceunity.nama.factory.EffectMultiDataFactory;
import com.faceunity.nama.factory.EffectSepMultipleFactory;
import com.faceunity.nama.factory.EffectSingleDataFactory;
import com.faceunity.nama.view.bean.TrackType;
import com.faceunity.nama.view.listener.ChoiceEffectOrAvatarListener;
import com.faceunity.nama.view.listener.TypeEnum;

import java.util.HashSet;
import java.util.Iterator;

public class EffectControllerView extends LinearLayout implements ChoiceEffectOrAvatarListener, View.OnClickListener {
    private Context mContext;
    /*单选控制器 手势*/
    private EffectSingleControllerView mEffectGestureSingleControllerView;
    /*单选控制器 分割*/
    private EffectSingleControllerView mEffectSegmentationSingleControllerView;
    /*单选控制器 动作*/
    private EffectSingleControllerView mEffectActionSingleControllerView;
    /*人脸多选控制器*/
    private EffectMultiControllerView mEffectFaceMultiControllerView;
    /*特殊人体控制器*/
    private EffectSpeControllerView mEffectBodySpeControllerView;

    /*总工厂控制器*/
    private EffectFactory mEffectFactory;
    /*手势工厂*/
    private EffectSingleDataFactory mEffectGestureSingleDataFactory;
    /*分割工厂*/
    private EffectSingleDataFactory mEffectSegmentationSingleDataFactory;
    /*动作工厂*/
    private EffectSingleDataFactory mEffectActionSingleDataFactory;
    /*人脸工厂*/
    private EffectMultiDataFactory mEffectFaceMultiDataFactory;
    /*人体工厂*/
    private EffectSepMultipleFactory mEffectBodySpeDataFactory;
    /*选中的效果 - 单选*/
    private HashSet<Effect> mChoiceSingleEffects = new HashSet<>();
    /*选中的效果 - 多选*/
    private HashSet<Effect> mChoiceMultiEffects = new HashSet<>();
    /*选中的效果 - 特殊*/
    private HashSet<Effect> mChoiceSpeEffects = new HashSet<>();
    //重置按钮
    private TextView btnConfigReset;
    //确认按钮
    private TextView btnConfigConfirm;

    public EffectControllerView(Context context) {
        this(context, null);
    }

    public EffectControllerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EffectControllerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        init();
    }

    public void init() {
        inflate(mContext, R.layout.layout_effct_controller, this);
        mEffectGestureSingleControllerView = findViewById(R.id.gesture_controller);
        mEffectSegmentationSingleControllerView = findViewById(R.id.segmentation_controller);
        mEffectActionSingleControllerView = findViewById(R.id.action_controller);
        mEffectFaceMultiControllerView = findViewById(R.id.face_controller);
        mEffectBodySpeControllerView = findViewById(R.id.body_controller);
        btnConfigReset = findViewById(R.id.btn_config_reset);
        btnConfigConfirm = findViewById(R.id.btn_config_confirm);

        mEffectGestureSingleControllerView.setOnChoiceEffectListener(this);
        mEffectSegmentationSingleControllerView.setOnChoiceEffectListener(this);
        mEffectActionSingleControllerView.setOnChoiceEffectListener(this);
        mEffectFaceMultiControllerView.setOnChoiceEffectListener(this);
        mEffectBodySpeControllerView.setOnChoiceEffectListener(this);

        btnConfigReset.setOnClickListener(this);
        btnConfigConfirm.setOnClickListener(this);
    }

    public void bindEffectFactory(EffectFactory effectFactory) {
        mEffectFactory = effectFactory;
        mEffectGestureSingleDataFactory = effectFactory.getEffectGestureSingleDataFactory();
        mEffectSegmentationSingleDataFactory = effectFactory.getEffectSegmentationSingleDataFactory();
        mEffectActionSingleDataFactory = effectFactory.getEffectActionSingleDataFactory();
        mEffectFaceMultiDataFactory = effectFactory.getEffectFaceMultiDataFactory();
        mEffectBodySpeDataFactory = effectFactory.getEffectSpeDataFactory();

        mEffectGestureSingleControllerView.bindEffectFactory(mEffectGestureSingleDataFactory);
        mEffectSegmentationSingleControllerView.bindEffectFactory(mEffectSegmentationSingleDataFactory);
        mEffectActionSingleControllerView.bindEffectFactory(mEffectActionSingleDataFactory);
        mEffectFaceMultiControllerView.bindEffectFactory(mEffectFaceMultiDataFactory);
        mEffectBodySpeControllerView.bindEffectFactory(mEffectBodySpeDataFactory);
    }

    /**
     * 设置人脸默认选中哪一个
     *
     * @param defaultSelect
     */
    public void setFaceDefaultPosition(int defaultSelect) {
        mEffectFaceMultiControllerView.setDefaultPosition(defaultSelect);
    }

    @Override
    public void onChoiceEffects(HashSet<Effect> effects) {
        mChoiceMultiEffects.clear();
        mChoiceMultiEffects.addAll(effects);

        choiceEffectsSetDisableOrEnable();
    }

    @Override
    public void onSpeEffect(Effect data) {
        mChoiceSpeEffects.clear();
        if (data != null) {
            mChoiceSpeEffects.add(data);
        }

        choiceEffectsSetDisableOrEnable();
    }

    @Override
    public void onEffect(boolean isSelected, Effect data) {
        if (data == null) {
            return;
        }

        if (isSelected)
            mChoiceSingleEffects.add(data);
        else
            mChoiceSingleEffects.remove(data);

        choiceEffectsSetDisableOrEnable();
    }

    @Override
    public void onSure() {
        sure();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_config_reset) {
            //清除选中状态
            mChoiceSingleEffects.clear();
            mChoiceMultiEffects.clear();
            mChoiceSpeEffects.clear();
            //1.清空选中状态 2.控制按钮可否点击
            choiceEffectsSetDisableOrEnableResetEffect();
            sure();
        } else if (v.getId() == R.id.btn_config_confirm) {
            sure();
        }
    }

    /**
     * 点击确认按钮
     */
    private void sure() {
        //保存当前的效果情况
        mEffectFactory.getTakeEffects().clear();
        mEffectFactory.getTakeEffects().addAll(mChoiceSingleEffects);
        mEffectFactory.getTakeEffects().addAll(mChoiceMultiEffects);
        mEffectFactory.getTakeEffects().addAll(mChoiceSpeEffects);

        //遍历当前选中的效果，判断是Effect模式 还是 Avatar模式
        TypeEnum typeEnum = TypeEnum.EFFECT;
        Effect skeleton = null;
        boolean isGesture = false;//是否选择手势
        boolean isAction = false;//是否选择动作
        boolean isExpression = false;//是否选择表情
        boolean isTongue = false;//是否选择舌头
        boolean isEmotion = false;//是否选择情绪

        for (Effect effect : mEffectFactory.getTakeEffects()) {
            //判断是否选中骨骼
            if (Effect.TYPE_HUMAN_FULL_OR_HALF == effect.getType()) {
                //说明选中了骨骼
                typeEnum = TypeEnum.AVATAR;
                skeleton = effect;
                continue;
            }

            //判断是否选中手势
            if (Effect.MODULE_CODE_HAND_GESTURE == effect.getAuthCode()) {
                isGesture = true;
                continue;
            }

            //判断是否选中动作识别
            if (Effect.MODULE_CODE_ACTION == effect.getAuthCode()) {
                isAction = true;
                continue;
            }

            //判断是否选中表情
            if (Effect.MODULE_CODE_FACE_EXPRESSION == effect.getAuthCode()) {
                isExpression = true;
                continue;
            }

            //判断是否选中舌头
            if (Effect.MODULE_CODE_FACE_TONGUE == effect.getAuthCode()) {
                isTongue = true;
                continue;
            }

            //判断是否选中情绪
            if (Effect.MODULE_CODE_FACE_EMOTION == effect.getAuthCode()) {
                isEmotion = true;
                continue;
            }
        }

        if (TypeEnum.AVATAR == typeEnum && skeleton != null && Effect.TYPE_HUMAN_FULL_OR_HALF == skeleton.getType()) {
            //启动avatar
            mEffectFactory.getEffectSpeDataFactory().getAvatarDataFactory().bindCurrentRenderer();
            //设置当前的类型
            mEffectFactory.getEffectSpeDataFactory().getAvatarDataFactory().setHumanTrackSceneFull(skeleton.getAuthCode() == Effect.MODULE_CODE_HUMAN_SKELETON_FULL);
        } else {
            //移除这个效果
            mEffectFactory.getEffectSpeDataFactory().getAvatarDataFactory().removeScene();
            //渲染所有选中的效果
            mEffectFactory.replaceProps(mEffectFactory.getTakeEffects());
        }

        mEffectFactory.setTypeEnum(typeEnum);

        if (mOnEffectChoiceListener != null) {
            //通知确认
            mOnEffectChoiceListener.sure();
            //通知模式
            mOnEffectChoiceListener.setNormalOrAvatarMode(typeEnum);
            //通知手势
            mOnEffectChoiceListener.setGestureRecognitionRecyclerVisibility(isGesture);
            //通知动作
            mOnEffectChoiceListener.setActionRecognitionRecyclerVisibility(isAction);
            //通知表情
            mOnEffectChoiceListener.setExpressionRecyclerVisibility(isExpression);
            //通知舌头
            mOnEffectChoiceListener.setTongueTrackRecyclerVisibility(isTongue);
            //通知情绪
            mOnEffectChoiceListener.setEmotionTrackRecyclerVisibility(isEmotion);
        }
    }

    /**
     * 获取当前选中的效果情况
     *
     * @return
     */
    public TrackType getSelectedTrackType() {
        TrackType trackType = new TrackType();

        for (Effect effect : mEffectFactory.getTakeEffects()) {
            if (Effect.TYPE_FACE == effect.getType()) {
                trackType.face = true;
                continue;
            }

            if (Effect.TYPE_HUMAN == effect.getType()
                    || Effect.TYPE_HUMAN_FULL_OR_HALF == effect.getType()
                    || Effect.TYPE_SEGMENTATION == effect.getType()
                    || Effect.TYPE_ACTION == effect.getType()) {
                if (Effect.MODULE_CODE_HUMAN_LANDMARKS_FULL == effect.getAuthCode()
                        || Effect.MODULE_CODE_HUMAN_SKELETON_FULL == effect.getAuthCode()) {
                    trackType.body = 0;
                } else {
                    if (Effect.TYPE_SEGMENTATION == effect.getType() && (Effect.MODULE_CODE_HAIR_SEGMENTATION == effect.getAuthCode() || Effect.MODULE_CODE_HEAD_SEGMENTATION == effect.getAuthCode())) {
                        trackType.face = true;
                    } else {
                        trackType.body = 1;
                    }
                }
                continue;
            }

            if (Effect.TYPE_GESTURE == effect.getType()) {
                trackType.gesture = true;
                continue;
            }
        }

        return trackType;
    }

    public interface OnEffectChoiceListener {
        //手势识别
        void setGestureRecognitionRecyclerVisibility(boolean visibility);

        //动作识别
        void setActionRecognitionRecyclerVisibility(boolean visibility);

        //表情
        void setExpressionRecyclerVisibility(boolean visibility);

        //舌头
        void setTongueTrackRecyclerVisibility(boolean visibility);

        //情绪
        void setEmotionTrackRecyclerVisibility(boolean visibility);

        //avatar 模式 或者普通模式
        void setNormalOrAvatarMode(TypeEnum typeEnum);

        //确认按钮
        void sure();
    }

    private OnEffectChoiceListener mOnEffectChoiceListener;

    public void setOnEffectChoiceListener(OnEffectChoiceListener onEffectChoiceListener) {
        this.mOnEffectChoiceListener = onEffectChoiceListener;
    }

    /**
     * 根据保存的特效情况恢复 -> 1.按钮是否被选中 2.按钮可否被点击
     */
    public void setTakeEffects() {
        mChoiceSingleEffects.clear();
        mChoiceMultiEffects.clear();
        mChoiceSpeEffects.clear();

        //1.将选中的效果分开

        //1.手势
        Effect gestureEffect = null;
        //2.分割
        Effect segmentationEffect = null;
        //3.动作
        Effect actionEffect = null;
        //4.人脸
        HashSet<Effect> faceEffects = new HashSet<>();
        //5.人体
        Effect bodyEffect = null;
        //6.骨骼
        Effect skeletonEffect = null;

        for (Effect effect : mEffectFactory.getTakeEffects()) {
            if (effect == null) {
                break;
            }
            if (effect.getType() == Effect.TYPE_GESTURE) {
                //手势
                gestureEffect = effect;
                mChoiceSingleEffects.add(effect);
            } else if (effect.getType() == Effect.TYPE_SEGMENTATION) {
                //分割
                segmentationEffect = effect;
                mChoiceSingleEffects.add(effect);
            } else if (effect.getType() == Effect.TYPE_ACTION) {
                //动作
                actionEffect = effect;
                mChoiceSingleEffects.add(effect);
            } else if (effect.getType() == Effect.TYPE_FACE) {
                //人脸
                faceEffects.add(effect);
                mChoiceMultiEffects.add(effect);
            } else if (effect.getType() == Effect.TYPE_HUMAN) {
                //人体
                bodyEffect = effect;
                mChoiceSpeEffects.add(effect);
            } else if (effect.getType() == Effect.TYPE_HUMAN_FULL_OR_HALF) {
                //骨骼
                skeletonEffect = effect;
                mChoiceSpeEffects.add(effect);
            }
        }

        //2.根据当前效果将总列表中需要高亮的部分高亮出来 - 就算没选中也进去取消之
        mEffectGestureSingleControllerView.setTakeEffect(gestureEffect);//将手势高亮
        mEffectSegmentationSingleControllerView.setTakeEffect(segmentationEffect);//将分割高亮
        mEffectActionSingleControllerView.setTakeEffect(actionEffect);//将动作高亮
        mEffectFaceMultiControllerView.setTakeEffects(faceEffects);//将人脸高亮
        if (bodyEffect != null) {//将人体高亮
            mEffectBodySpeControllerView.setTakeEffect(bodyEffect);
        } else if (skeletonEffect != null) {
            mEffectBodySpeControllerView.setTakeEffect(skeletonEffect);
        } else {
            mEffectBodySpeControllerView.setTakeEffect(null);
        }

        /**
         * 完成几个控件按钮之间的互相作用 -> 主要是影响其他控件是否可以点击某个按钮
         * 单控件规则
         * 人脸：
         * 1.可多选。
         * 2.舌头检测，表情识别，情绪识别 选择后自动选择人脸特征点
         * 3.取消人脸特征点自动取消，舌头检测，表情识别，情绪识别
         * <p>
         * 人体
         * 1.单选
         * 2.二选其他 另一个不可点击
         * <p>
         * 手势
         * 1.单选
         * <p>
         * 分割
         * 1.单选
         * <p>
         * 动作
         * 1.单选
         * 2.其余两个不可点击
         * <p>
         * 复合规则 不再描述单选的控件情况（因为上述已经做出判断）
         * 1.针对人体骨骼 -> 任意按钮被选中 骨骼不可点击 同样 骨骼被选中其他任意按钮不可点击
         * 2.人体关键点选中 -> 手势识别 人像分割 头发分割 头部分割不可选中
         * 3.手势识别选中 -> 人体关键点 人体骨骼 人体分割 头发分割 头部分割
         * 4.人像分割 or 头发分割 or 头部分割选中-> 人体关键点 人体骨骼 手势识别 动作识别 不可点击
         * 5.动作识别选中 -> 人体骨骼 手势识别 人像分割 头发分割 头部分割不可点击
         * <p>
         * 目前这个回调是多选的回调，也就是人脸这一项的回调，所以不需要特别判断
         *
         * @param effects
         */
        //3.根据当前选中的效果进行按钮可否点击设置
        if (skeletonEffect != null) {//选中了骨骼
            //人脸不可点击
            saveEffectsToSetRecItemEnableOrDisable(mEffectFaceMultiDataFactory.getEffectBeans().iterator(), false, mEffectFaceMultiControllerView);
            //手势不可点击
            saveEffectsToSetRecItemEnableOrDisable(mEffectGestureSingleDataFactory.getEffectBeans().iterator(), false, mEffectGestureSingleControllerView);
            //分割不可点击
            saveEffectsToSetRecItemEnableOrDisable(mEffectSegmentationSingleDataFactory.getEffectBeans().iterator(), false, mEffectSegmentationSingleControllerView);
            //动作不可点击
            saveEffectsToSetRecItemEnableOrDisable(mEffectActionSingleDataFactory.getEffectBeans().iterator(), false, mEffectActionSingleControllerView);
            //人体关键点不可以点击
            saveEffectsToSetRecBodyLandmarksEnableOrDisable(false);
            //骨骼可以点击
            saveEffectsToSetRecBodySkeletonEnableOrDisable(true);
        } else {
            if (mEffectFactory.getTakeEffects().isEmpty()) {
                //人脸可以点击
                saveEffectsToSetRecItemEnableOrDisable(mEffectFaceMultiDataFactory.getEffectBeans().iterator(), true, mEffectFaceMultiControllerView);
                //人体可以点击
                saveEffectsToSetRecItemEnableOrDisable(mEffectBodySpeDataFactory.getEffectBeans().iterator(), true, mEffectBodySpeControllerView);
                //手势可以点击
                saveEffectsToSetRecItemEnableOrDisable(mEffectGestureSingleDataFactory.getEffectBeans().iterator(), true, mEffectGestureSingleControllerView);
                //分割可以点击
                saveEffectsToSetRecItemEnableOrDisable(mEffectSegmentationSingleDataFactory.getEffectBeans().iterator(), true, mEffectSegmentationSingleControllerView);
                //动作可以点击
                saveEffectsToSetRecItemEnableOrDisable(mEffectActionSingleDataFactory.getEffectBeans().iterator(), true, mEffectActionSingleControllerView);
                //人体可以点击
                saveEffectsToSetRecItemEnableOrDisable(mEffectBodySpeDataFactory.getEffectBeans().iterator(), true, mEffectBodySpeControllerView);
            } else {
                //骨骼不可点击
                saveEffectsToSetRecBodySkeletonEnableOrDisable(false);
                //有人体关键点 || 动作
                if (bodyEffect != null || actionEffect != null) {
                    //人脸可以点击
                    saveEffectsToSetRecItemEnableOrDisable(mEffectFaceMultiDataFactory.getEffectBeans().iterator(), true, mEffectFaceMultiControllerView);
                    //人体关键点可以点击
                    saveEffectsToSetRecBodyLandmarksEnableOrDisable(true);
                    //手势不可点击
                    saveEffectsToSetRecItemEnableOrDisable(mEffectGestureSingleDataFactory.getEffectBeans().iterator(), false, mEffectGestureSingleControllerView);
                    //分割不可点击
                    saveEffectsToSetRecItemEnableOrDisable(mEffectSegmentationSingleDataFactory.getEffectBeans().iterator(), false, mEffectSegmentationSingleControllerView);
                    //动作可以点击
                    saveEffectsToSetRecItemEnableOrDisable(mEffectActionSingleDataFactory.getEffectBeans().iterator(), true, mEffectActionSingleControllerView);
                } else if (gestureEffect != null) {//手势
                    //人脸可以点击
                    saveEffectsToSetRecItemEnableOrDisable(mEffectFaceMultiDataFactory.getEffectBeans().iterator(), true, mEffectFaceMultiControllerView);
                    //人体关键点不可点击
                    saveEffectsToSetRecBodyLandmarksEnableOrDisable(false);
                    //手势可以点击
                    saveEffectsToSetRecItemEnableOrDisable(mEffectGestureSingleDataFactory.getEffectBeans().iterator(), true, mEffectGestureSingleControllerView);
                    //分割不可点击
                    saveEffectsToSetRecItemEnableOrDisable(mEffectSegmentationSingleDataFactory.getEffectBeans().iterator(), false, mEffectSegmentationSingleControllerView);
                    //动作不可点击
                    saveEffectsToSetRecItemEnableOrDisable(mEffectActionSingleDataFactory.getEffectBeans().iterator(), false, mEffectActionSingleControllerView);
                } else if (segmentationEffect != null) {//分割
                    //人脸可以点击
                    saveEffectsToSetRecItemEnableOrDisable(mEffectFaceMultiDataFactory.getEffectBeans().iterator(), true, mEffectFaceMultiControllerView);
                    //人体关键点不可点击
                    saveEffectsToSetRecBodyLandmarksEnableOrDisable(false);
                    //手势可以点击
                    saveEffectsToSetRecItemEnableOrDisable(mEffectGestureSingleDataFactory.getEffectBeans().iterator(), false, mEffectGestureSingleControllerView);
                    //分割不可点击
//                    saveEffectsToSetRecItemEnableOrDisable(mEffectSegmentationSingleDataFactory.getEffectBeans().iterator(), true, mEffectSegmentationSingleControllerView);
                    //动作不可点击
                    saveEffectsToSetRecItemEnableOrDisable(mEffectActionSingleDataFactory.getEffectBeans().iterator(), false, mEffectActionSingleControllerView);
                } else {
                    //人脸可以选
                    setRecItemEnableOrDisable(mEffectFaceMultiDataFactory.getEffectBeans().iterator(), true, mEffectFaceMultiControllerView);
                    //人体关键点可选
                    setRecBodyLandmarksEnableOrDisable(true);
                    //手势可选
                    setRecItemEnableOrDisable(mEffectGestureSingleDataFactory.getEffectBeans().iterator(), true, mEffectGestureSingleControllerView);
                    //分割可选
                    setRecItemEnableOrDisable(mEffectSegmentationSingleDataFactory.getEffectBeans().iterator(), true, mEffectSegmentationSingleControllerView);
                    //动作可选
                    setRecItemEnableOrDisable(mEffectActionSingleDataFactory.getEffectBeans().iterator(), true, mEffectActionSingleControllerView);
                }
            }
        }
    }


    //以下控制面板上所有的按钮是否可以点击

    /**
     * 根据当前选中的效果，设置其他按钮可否被点击，并且重设一次所有按钮选中情况
     */
    private void choiceEffectsSetDisableOrEnableResetEffect() {
        HashSet mChoiceEffects = new HashSet();
        mChoiceEffects.addAll(mChoiceSingleEffects);
        mChoiceEffects.addAll(mChoiceSpeEffects);
        mChoiceEffects.addAll(mChoiceMultiEffects);
        realChoiceEffectsSetDisableOrEnable(mChoiceEffects, true);
    }

    /**
     * 根据当前选中的效果，设置其他按钮可否被点击
     */
    private void choiceEffectsSetDisableOrEnable() {
        HashSet mChoiceEffects = new HashSet();
        mChoiceEffects.addAll(mChoiceSingleEffects);
        mChoiceEffects.addAll(mChoiceSpeEffects);
        mChoiceEffects.addAll(mChoiceMultiEffects);
        realChoiceEffectsSetDisableOrEnable(mChoiceEffects, false);
    }

    /**
     * 在控件一直显示的情况下
     * 根据当前选中的效果，设置按钮可否被点击
     *
     * @param effects     当前选中的效果
     * @param resetEffect 是否根据传入的效果重新设置一下控件选中情况
     */
    private void realChoiceEffectsSetDisableOrEnable(HashSet<Effect> effects, Boolean resetEffect) {
        //1.手势
        Effect gestureEffect = null;
        //2.分割
        Effect segmentationEffect = null;
        //3.动作
        Effect actionEffect = null;
        //4.人脸
        HashSet<Effect> faceEffects = new HashSet<>();
        //5.人体
        Effect bodyEffect = null;
        //6.骨骼
        Effect skeletonEffect = null;

        for (Effect effect : effects) {
            if (effect == null) {
                break;
            }
            if (effect.getType() == Effect.TYPE_GESTURE) {
                //手势
                gestureEffect = effect;
            } else if (effect.getType() == Effect.TYPE_SEGMENTATION) {
                //分割
                segmentationEffect = effect;
            } else if (effect.getType() == Effect.TYPE_ACTION) {
                //动作
                actionEffect = effect;
            } else if (effect.getType() == Effect.TYPE_FACE) {
                //人脸
                faceEffects.add(effect);
            } else if (effect.getType() == Effect.TYPE_HUMAN) {
                //人体
                bodyEffect = effect;
            } else if (effect.getType() == Effect.TYPE_HUMAN_FULL_OR_HALF) {
                //骨骼
                skeletonEffect = effect;
            }
        }

        //是否需要手动设置选中效果
        if (resetEffect) {
            //2.根据当前效果将总列表中需要高亮的部分高亮出来 - 就算没选中也进去取消之
            mEffectGestureSingleControllerView.setTakeEffect(gestureEffect);//将手势高亮
            mEffectSegmentationSingleControllerView.setTakeEffect(segmentationEffect);//将分割高亮
            mEffectActionSingleControllerView.setTakeEffect(actionEffect);//将动作高亮
            mEffectFaceMultiControllerView.setTakeEffects(faceEffects);//将人脸高亮
            if (bodyEffect != null) {//将人体高亮
                mEffectBodySpeControllerView.setTakeEffect(bodyEffect);
            } else if (skeletonEffect != null) {
                mEffectBodySpeControllerView.setTakeEffect(skeletonEffect);
            } else {
                mEffectBodySpeControllerView.setTakeEffect(null);
            }
        }

        /**
         * 完成几个控件按钮之间的互相作用 -> 主要是影响其他控件是否可以点击某个按钮
         * 单控件规则
         * 人脸：
         * 1.可多选。
         * 2.舌头检测，表情识别，情绪识别 选择后自动选择人脸特征点
         * 3.取消人脸特征点自动取消，舌头检测，表情识别，情绪识别
         * <p>
         * 人体
         * 1.单选
         * 2.二选其他 另一个不可点击
         * <p>
         * 手势
         * 1.单选
         * <p>
         * 分割
         * 1.单选
         * <p>
         * 动作
         * 1.单选
         * 2.其余两个不可点击
         * <p>
         * 复合规则 不再描述单选的控件情况（因为上述已经做出判断）
         * 1.针对人体骨骼 -> 任意按钮被选中 骨骼不可点击 同样 骨骼被选中其他任意按钮不可点击
         * 2.人体关键点选中 -> 手势识别 人像分割 头发分割 头部分割不可选中
         * 3.手势识别选中 -> 人体关键点 人体骨骼 人体分割 头发分割 头部分割
         * 4.人像分割 or 头发分割 or 头部分割选中-> 人体关键点 人体骨骼 手势识别 动作识别 不可点击
         * 5.动作识别选中 -> 人体骨骼 手势识别 人像分割 头发分割 头部分割不可点击
         * <p>
         * 目前这个回调是多选的回调，也就是人脸这一项的回调，所以不需要特别判断
         *
         * @param effects
         */
        if (skeletonEffect != null) {
            //1.选中了骨骼
            //人脸不可选
            setRecItemEnableOrDisable(mEffectFaceMultiDataFactory.getEffectBeans().iterator(), false, mEffectFaceMultiControllerView);
            //手势不可选
            setRecItemEnableOrDisable(mEffectGestureSingleDataFactory.getEffectBeans().iterator(), false, mEffectGestureSingleControllerView);
            //分割不可选
            setRecItemEnableOrDisable(mEffectSegmentationSingleDataFactory.getEffectBeans().iterator(), false, mEffectSegmentationSingleControllerView);
            //动作不可选
            setRecItemEnableOrDisable(mEffectActionSingleDataFactory.getEffectBeans().iterator(), false, mEffectActionSingleControllerView);
            //选中骨骼
            setRecBodySkeletonEnableOrDisable(true);
            //人体关键点不可点击
            setRecBodyLandmarksEnableOrDisable(false);
        } else {
            if (effects.isEmpty()) {
                //2.所有效果均没有被选中
                //人脸可选
                setRecItemEnableOrDisable(mEffectFaceMultiDataFactory.getEffectBeans().iterator(), true, mEffectFaceMultiControllerView);
                //人体可选
                setRecItemEnableOrDisable(mEffectBodySpeDataFactory.getEffectBeans().iterator(), true, mEffectBodySpeControllerView);
                //手势可选
                setRecItemEnableOrDisable(mEffectGestureSingleDataFactory.getEffectBeans().iterator(), true, mEffectGestureSingleControllerView);
                //分割可选
                setRecItemEnableOrDisable(mEffectSegmentationSingleDataFactory.getEffectBeans().iterator(), true, mEffectSegmentationSingleControllerView);
                //动作可选
                setRecItemEnableOrDisable(mEffectActionSingleDataFactory.getEffectBeans().iterator(), true, mEffectActionSingleControllerView);
                //人体可以点击
                saveEffectsToSetRecItemEnableOrDisable(mEffectBodySpeDataFactory.getEffectBeans().iterator(), true, mEffectBodySpeControllerView);
            } else {
                //3.有效果被选中
                setRecBodySkeletonEnableOrDisable(false);
                //有人体关键点 || 动作
                if (bodyEffect != null || actionEffect != null) {
                    //人脸可以选
                    setRecItemEnableOrDisable(mEffectFaceMultiDataFactory.getEffectBeans().iterator(), true, mEffectFaceMultiControllerView);
                    //人体关键点可选
                    setRecBodyLandmarksEnableOrDisable(true);
                    //手势不可选
                    setRecItemEnableOrDisable(mEffectGestureSingleDataFactory.getEffectBeans().iterator(), false, mEffectGestureSingleControllerView);
                    //分割不可选
                    setRecItemEnableOrDisable(mEffectSegmentationSingleDataFactory.getEffectBeans().iterator(), false, mEffectSegmentationSingleControllerView);
                    //动作可选
                    setRecItemEnableOrDisable(mEffectActionSingleDataFactory.getEffectBeans().iterator(), true, mEffectActionSingleControllerView);
                } else if (gestureEffect != null) {//手势
                    //人脸可以选
                    setRecItemEnableOrDisable(mEffectFaceMultiDataFactory.getEffectBeans().iterator(), true, mEffectFaceMultiControllerView);
                    //人体关键点不可选
                    setRecBodyLandmarksEnableOrDisable(false);
                    //手势可选
                    setRecItemEnableOrDisable(mEffectGestureSingleDataFactory.getEffectBeans().iterator(), true, mEffectGestureSingleControllerView);
                    //分割不可选
                    setRecItemEnableOrDisable(mEffectSegmentationSingleDataFactory.getEffectBeans().iterator(), false, mEffectSegmentationSingleControllerView);
                    //动作不可选
                    setRecItemEnableOrDisable(mEffectActionSingleDataFactory.getEffectBeans().iterator(), false, mEffectActionSingleControllerView);
                } else if (segmentationEffect != null) {//分割
                    //人脸可以选
                    setRecItemEnableOrDisable(mEffectFaceMultiDataFactory.getEffectBeans().iterator(), true, mEffectFaceMultiControllerView);
                    //人体关键点不可选
                    setRecBodyLandmarksEnableOrDisable(false);
                    //手势可选
                    setRecItemEnableOrDisable(mEffectGestureSingleDataFactory.getEffectBeans().iterator(), false, mEffectGestureSingleControllerView);
                    //分割可选
//                    setRecItemEnableOrDisable(mEffectSegmentationSingleDataFactory.getEffectBeans().iterator(), true, mEffectSegmentationSingleControllerView);
                    //动作不可选
                    setRecItemEnableOrDisable(mEffectActionSingleDataFactory.getEffectBeans().iterator(), false, mEffectActionSingleControllerView);
                } else {
                    //人脸可以选
                    setRecItemEnableOrDisable(mEffectFaceMultiDataFactory.getEffectBeans().iterator(), true, mEffectFaceMultiControllerView);
                    //人体关键点可选
                    setRecBodyLandmarksEnableOrDisable(true);
                    //手势可选
                    setRecItemEnableOrDisable(mEffectGestureSingleDataFactory.getEffectBeans().iterator(), true, mEffectGestureSingleControllerView);
                    //分割可选
                    setRecItemEnableOrDisable(mEffectSegmentationSingleDataFactory.getEffectBeans().iterator(), true, mEffectSegmentationSingleControllerView);
                    //动作可选
                    setRecItemEnableOrDisable(mEffectActionSingleDataFactory.getEffectBeans().iterator(), true, mEffectActionSingleControllerView);
                }
            }
        }
    }

    /**
     * 下面是根据不同的情况设置不同的按钮可否被点击
     * 1.重新打开控件的时候，强制根据Effects保存的选择情况直接设置所有按钮可否被点击。
     * 2.控件一直在界面选择过程中，根据当前按钮是否需要修改，修改按钮可否被点击。
     */

    //第一种 根据保存情况恢复当前按钮可否点击

    /**
     * 根据当前保存的选择的Effects情况，控制一个rec项 全部可选 或者 不可选
     *
     * @param iterator
     * @param isSelect
     * @param baseEffectControllerView
     */
    private void saveEffectsToSetRecItemEnableOrDisable(Iterator<Effect> iterator, boolean isSelect, BaseEffectControllerView baseEffectControllerView) {
        while (iterator.hasNext()) {
            Effect effect = iterator.next();
            if (isSelect) {
                effect.setState(Effect.STATE_ENABLE);
            } else {
                effect.setState(Effect.STATE_DISABLE);
            }
            baseEffectControllerView.updateData(effect);
        }
    }

    /**
     * 根据当前保存的选择的Effects情况，控制骨骼是否可以被选中
     *
     * @param isSelect
     */
    private void saveEffectsToSetRecBodySkeletonEnableOrDisable(boolean isSelect) {
        Iterator<Effect> iterator = mEffectBodySpeDataFactory.getEffectBeans().iterator();
        while (iterator.hasNext()) {
            Effect effect = iterator.next();
            //骨骼不可选
            if (Effect.MODULE_CODE_HUMAN_SKELETON == effect.getAuthCode()) {
                if (isSelect)
                    effect.setState(Effect.STATE_ENABLE);
                else
                    effect.setState(Effect.STATE_DISABLE);
                mEffectBodySpeControllerView.updateData(effect);
            }
        }
    }

    /**
     * 根据当前保存的选择的Effects情况，控制人体关键点是否选中
     *
     * @param isSelect
     */
    private void saveEffectsToSetRecBodyLandmarksEnableOrDisable(boolean isSelect) {
        Iterator<Effect> iterator = mEffectBodySpeDataFactory.getEffectBeans().iterator();
        while (iterator.hasNext()) {
            Effect effect = iterator.next();
            //骨骼不可选
            if (Effect.MODULE_CODE_HUMAN_LANDMARKS == effect.getAuthCode()) {
                if (isSelect)
                    effect.setState(Effect.STATE_ENABLE);
                else
                    effect.setState(Effect.STATE_DISABLE);
                mEffectBodySpeControllerView.updateData(effect);
            }
        }
    }

    //第二种

    /**
     * 控制一个rec项 全部可选 或者 不可选
     * 1.如果当前选择按钮与我想设置的按钮状态不一样则设置过去
     *
     * @param iterator
     * @param isSelect
     * @param baseEffectControllerView
     */
    private void setRecItemEnableOrDisable(Iterator<Effect> iterator, boolean isSelect, BaseEffectControllerView baseEffectControllerView) {
        while (iterator.hasNext()) {
            Effect effect = iterator.next();
            if (isSelect && effect.getState() == Effect.STATE_DISABLE) {
                effect.setState(Effect.STATE_ENABLE);
                baseEffectControllerView.updateData(effect);
            } else if (!isSelect && effect.getState() == Effect.STATE_ENABLE) {
                effect.setState(Effect.STATE_DISABLE);
                baseEffectControllerView.updateData(effect);
            }
        }
    }

    /**
     * 1.如果当前选择按钮与我想设置的按钮状态不一样则设置过去
     *
     * @param isSelect
     */
    private void setRecBodySkeletonEnableOrDisable(boolean isSelect) {
        Iterator<Effect> iterator = mEffectBodySpeDataFactory.getEffectBeans().iterator();
        while (iterator.hasNext()) {
            Effect effect = iterator.next();
            //骨骼不可选
            if (Effect.MODULE_CODE_HUMAN_SKELETON == effect.getAuthCode()) {
                if (isSelect && effect.getState() == Effect.STATE_DISABLE) {
                    effect.setState(Effect.STATE_ENABLE);
                    mEffectBodySpeControllerView.updateData(effect);
                } else if (!isSelect && effect.getState() == Effect.STATE_ENABLE) {
                    effect.setState(Effect.STATE_DISABLE);
                    mEffectBodySpeControllerView.updateData(effect);
                }
            }
        }
    }

    /**
     * 1.如果当前选择按钮与我想设置的按钮状态不一样则设置过去
     *
     * @param isSelect
     */
    private void setRecBodyLandmarksEnableOrDisable(boolean isSelect) {
        Iterator<Effect> iterator = mEffectBodySpeDataFactory.getEffectBeans().iterator();
        while (iterator.hasNext()) {
            Effect effect = iterator.next();
            //骨骼不可选
            if (Effect.MODULE_CODE_HUMAN_LANDMARKS == effect.getAuthCode()) {
                if (isSelect && effect.getState() == Effect.STATE_DISABLE) {
                    effect.setState(Effect.STATE_ENABLE);
                    mEffectBodySpeControllerView.updateData(effect);
                } else if (!isSelect && effect.getState() == Effect.STATE_ENABLE) {
                    effect.setState(Effect.STATE_DISABLE);
                    mEffectBodySpeControllerView.updateData(effect);
                }
            }
        }
    }
}
