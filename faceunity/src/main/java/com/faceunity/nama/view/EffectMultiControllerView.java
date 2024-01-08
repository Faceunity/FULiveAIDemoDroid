package com.faceunity.nama.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.faceunity.nama.Effect;
import com.faceunity.nama.R;
import com.faceunity.nama.factory.EffectMultiDataFactory;
import com.faceunity.nama.view.adapter.BaseRecyclerAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * 多选效果 1.多选 2.带人脸规则多选
 */
public class EffectMultiControllerView extends BaseEffectControllerView {
    /*选中的效果*/
    private HashSet<Effect> mChoiceEffects = new HashSet<>();
    /*初始化数据工厂*/
    private EffectMultiDataFactory mEffectMultiDataFactory;
    private RecyclerItemClickListener mRecyclerItemClickListener;

    public EffectMultiControllerView(Context context) {
        this(context, null);
    }

    public EffectMultiControllerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EffectMultiControllerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public int setContentViewRes() {
        return R.layout.layout_effct_single_or_multi_controller;
    }

    @Override
    public void initView() {
        mRecyclerItemClickListener = new RecyclerItemClickListener();
        mEffectRecyclerAdapter.setOnItemClickListener(mRecyclerItemClickListener);
    }

    @Override
    public int getChoiceMode() {
        return BaseRecyclerAdapter.MULTI_CHOICE_MODE;
    }

    /**
     * 处理点击逻辑
     * 人脸识别
     */
    private class RecyclerItemClickListener implements BaseRecyclerAdapter.OnItemClickListener<Effect> {
        @Override
        public void onItemClick(BaseRecyclerAdapter<Effect> adapter, View view, int position) {
            //需要开启的效果选项
            Effect effect = adapter.getItem(position);
            //当我选择某一个按钮的时候其他的按钮需要联动
            String authCode = effect.getAuthCode();
            //当前是否选择
            boolean isSelected = !view.isSelected();

            //普通多选规则

            //人脸多选规则
            if (Effect.MODULE_CODE_FACE_LANDMARKS.equals(authCode)) {//人脸特征点
                //如果是取消要取消其他选项
                if (!isSelected) {
                    adapter.clearMultiItemSelected();
                }
            } else if (Effect.MODULE_CODE_FACE_TONGUE.equals(authCode)
                    || Effect.MODULE_CODE_FACE_EXPRESSION.equals(authCode)
                    || Effect.MODULE_CODE_FACE_EMOTION.equals(authCode)
                    || Effect.MODULE_CODE_FACE_ARMESH.equals(authCode)) {//舌头检测，表情识别，情绪识别,ARMESH
                //如果选中了舌头那么人脸效果肯定会选中
                if (getLandMarkEffect() != null)
                    adapter.setItemSelected(getLandMarkEffect());
            }

            //每一次点击的时候都需要主动的通知外界我选中了哪一些按钮
            if (mChoiceEffectOrAvatarListener != null) {
                mChoiceEffectOrAvatarListener.onChoiceEffects(getChoiceEffects());
            }
        }
    }

    /**
     * 获取人脸特征点Effect
     *
     * @return
     */
    public Effect getLandMarkEffect() {
        if (mData == null || mData.isEmpty()) {
            return null;
        }

        for (Effect effect : mData) {
            if (Effect.MODULE_CODE_FACE_LANDMARKS == effect.getAuthCode()) {
                return effect;
            }
        }

        return null;
    }

    public void bindEffectFactory(EffectMultiDataFactory effectMultiDataFactory) {
        this.mEffectMultiDataFactory = effectMultiDataFactory;
        initListData();
    }

    /**
     * 初始化数据
     */
    private void initListData() {
        HashMap<String, Integer> titleMap = mEffectMultiDataFactory.getTitleMap();
        @StringRes int titleTv = titleMap.get("title_tv");
        @DrawableRes int titleImg = titleMap.get("title_img");
        tvTitle.setText(titleTv);
        ivTitle.setImageResource(titleImg);
        mData = mEffectMultiDataFactory.getEffectBeans();
        mEffectRecyclerAdapter.setData(mData);
    }

    /**
     * 设置默认选择
     */
    public void setDefaultPosition (int defaultSelect){
        mEffectRecyclerAdapter.setItemSelected(defaultSelect);
        mRecyclerItemClickListener.onItemClick(mEffectRecyclerAdapter, new View(getContext()), defaultSelect);

        if (mChoiceEffectOrAvatarListener != null) {
            mChoiceEffectOrAvatarListener.onSure();
        }
    }

    /**
     * 获取当前选中的按钮
     */
    public HashSet<Effect> getChoiceEffects() {
        mChoiceEffects.clear();
        SparseArray<Effect> selectedItems = mEffectRecyclerAdapter.getSelectedItems();
        for (int i = 0; i < selectedItems.size(); i++) {
            int key = selectedItems.keyAt(i);
            mChoiceEffects.add(selectedItems.get(key));
        }
        return mChoiceEffects;
    }

    /**
     * 用于之后进来恢复数据
     * 设置当前保存的按钮
     * 当前已经实现效果的按钮
     */
    public void setTakeEffects(HashSet<Effect> takeEffects) {
        mChoiceEffects.clear();
        mChoiceEffects.addAll(takeEffects);

        //将其设置到adapter
        mEffectRecyclerAdapter.clearMultiItemSelected();
        mEffectRecyclerAdapter.setItemsSelected(new ArrayList<>(mChoiceEffects));
    }

    /**
     * 清除选中状态
     */
    public void clearSelectedItems() {
        mChoiceEffects.clear();
        mEffectRecyclerAdapter.clearMultiItemSelected();
    }
}
