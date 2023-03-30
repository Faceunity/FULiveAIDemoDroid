package com.faceunity.nama.view;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.faceunity.nama.Effect;
import com.faceunity.nama.R;
import com.faceunity.nama.view.adapter.BaseRecyclerAdapter;
import com.faceunity.nama.view.adapter.SpaceItemDecoration;
import com.faceunity.nama.view.listener.ChoiceEffectOrAvatarListener;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseEffectControllerView extends LinearLayout {
    public Context mContext;
    public TextView tvTitle;
    public ImageView ivTitle;
    public RecyclerViewAdapter mEffectRecyclerAdapter;
    /*所有效果*/
    public List<Effect> mData = new ArrayList<>();

    public BaseEffectControllerView(Context context) {
        this(context, null);
    }

    public BaseEffectControllerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BaseEffectControllerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        init();
    }

    private void init() {
        View view = inflate(mContext, setContentViewRes(), this);
        tvTitle = view.findViewById(R.id.tv_title);
        ivTitle = view.findViewById(R.id.iv_title);

        RecyclerView rvEffect = view.findViewById(R.id.rv_effect);
        initRecyclerView(rvEffect);
        mEffectRecyclerAdapter = new RecyclerViewAdapter(new ArrayList<>());
        rvEffect.setAdapter(mEffectRecyclerAdapter);
        initView();
    }

    public abstract @LayoutRes
    int setContentViewRes();

    public abstract void initView();

    private void initRecyclerView(RecyclerView recyclerView) {
        int rvItemHorizontalSpace = dp2px(getContext(), 0);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getContext(), 3);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        int rvItemVerticalSpace = dp2px(getContext(), 4);
        recyclerView.addItemDecoration(new SpaceItemDecoration(rvItemHorizontalSpace, rvItemVerticalSpace));
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
    }

    public class RecyclerViewAdapter extends BaseRecyclerAdapter<Effect> {

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
            return getChoiceMode();
        }

        @Override
        protected void handleSelectedState(BaseViewHolder viewHolder, Effect data, boolean selected) {
            super.handleSelectedState(viewHolder, data, selected);
            ((TextView) viewHolder.itemView).setTypeface(selected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        }
    }

    public abstract int getChoiceMode();

    /**
     * 替换所有效果按钮
     *
     * @param effects
     */
    public void replaceData(List<Effect> effects) {
        mEffectRecyclerAdapter.replaceAll(effects);
    }

    /**
     * 更新单个效果按钮
     *
     * @param effect
     */
    public void updateData(Effect effect) {
        mEffectRecyclerAdapter.update(effect);
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dp2px(Context context, float dpValue) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * density + 0.5f);
    }

    /**
     * 还有一个刷新某一些按钮的方法
     * 因为有一些按钮点击了之后其他按钮应该无法点击
     */
    //添加选择监听
    public ChoiceEffectOrAvatarListener mChoiceEffectOrAvatarListener = null;

    public void setOnChoiceEffectListener(ChoiceEffectOrAvatarListener choiceEffectOrAvatarListener) {
        mChoiceEffectOrAvatarListener = choiceEffectOrAvatarListener;
    }
}