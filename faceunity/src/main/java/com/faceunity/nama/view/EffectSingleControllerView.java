package com.faceunity.nama.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.faceunity.nama.Effect;
import com.faceunity.nama.R;
import com.faceunity.nama.factory.EffectSingleDataFactory;
import com.faceunity.nama.view.adapter.BaseRecyclerAdapter;

import java.util.HashMap;
import java.util.Iterator;

/**
 * 单选效果
 * 适用于 1.手势 2.分割 3.动作
 */
public class EffectSingleControllerView extends BaseEffectControllerView {
    private RecyclerItemClickListener mRecyclerItemClickListener;
    /*初始化数据工厂*/
    private EffectSingleDataFactory mEffectSingleDataFactory;


    public EffectSingleControllerView(Context context) {
        this(context, null);
    }

    public EffectSingleControllerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EffectSingleControllerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
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
        return BaseRecyclerAdapter.SINGLE_CAN_CANCEL_CHOICE_MODE;
    }

    /**
     * 处理点击逻辑
     */
    private class RecyclerItemClickListener implements BaseRecyclerAdapter.OnItemClickListener<Effect> {
        @Override
        public void onItemClick(BaseRecyclerAdapter<Effect> adapter, View view, int position) {
            //每一次点击的时候都需要主动的通知外界我选中了哪一些按钮
            Effect effect = adapter.getItem(position);
            boolean isSelected = !view.isSelected();
            freshData(effect, isSelected);

            if (mChoiceEffectOrAvatarListener != null) {
                mChoiceEffectOrAvatarListener.onEffect(isSelected, effect);
            }
        }
    }

    /**
     * 刷新所有按钮的是否可点击情况
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

    public void bindEffectFactory(EffectSingleDataFactory effectSingleDataFactory) {
        this.mEffectSingleDataFactory = effectSingleDataFactory;
        initListData();
    }

    /**
     * 初始化数据
     */
    private void initListData() {
        mData = mEffectSingleDataFactory.getEffectBeans();
        mEffectRecyclerAdapter.setData(mData);

        HashMap<String, Integer> titleMap = mEffectSingleDataFactory.getTitleMap();
        @StringRes int titleTv = titleMap.get("title_tv");
        @DrawableRes int titleImg = titleMap.get("title_img");
        tvTitle.setText(titleTv);
        ivTitle.setImageResource(titleImg);
    }

    /**
     * 用于之后进来恢复数据
     * 设置当前保存的按钮
     * 当前已经实现效果的按钮
     */
    public void setTakeEffect(Effect takeEffect) {
        //将其设置到adapter
        mEffectRecyclerAdapter.clearSingleItemSelected();
        mEffectRecyclerAdapter.setItemSelected(takeEffect);
        if (takeEffect != null)
            freshData(takeEffect, true);
    }
}
