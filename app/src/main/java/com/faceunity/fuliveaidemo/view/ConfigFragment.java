package com.faceunity.fuliveaidemo.view;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.faceunity.fuliveaidemo.R;
import com.faceunity.fuliveaidemo.activity.BaseGlActivity;
import com.faceunity.nama.factory.EffectFactory;
import com.faceunity.nama.view.EffectControllerView;
import com.faceunity.nama.view.bean.TrackType;
import com.faceunity.nama.view.listener.TypeEnum;

/**
 * 功能配置页
 *
 * @author Richie on 2020.05.21
 */
public class ConfigFragment extends Fragment {
    public static final String TAG = "ConfigFragment";

    public static final String CONTAIN_HUMAN_AVATAR = "contain_human_avatar";

    private BaseGlActivity mBaseGlActivity;
    private ViewClickListener mViewClickListener;
    private boolean isOnCreate;

    private EffectControllerView mEffectControllerView;
    private EffectFactory mEffectFactory;

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
    }

    boolean hasGesture = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        isOnCreate = true;
        View view = inflater.inflate(R.layout.fragment_config, container, false);
        mViewClickListener = new ViewClickListener();
        view.findViewById(R.id.fl_config_mask).setOnClickListener(mViewClickListener);
        boolean containHumanAvatar = getArguments().getBoolean(CONTAIN_HUMAN_AVATAR, true);
        mEffectFactory = new EffectFactory(containHumanAvatar);
        mEffectControllerView = view.findViewById(R.id.effect_controller);
        mEffectControllerView.bindEffectFactory(mEffectFactory);
        mEffectControllerView.setOnEffectChoiceListener(new EffectControllerView.OnEffectChoiceListener() {
            @Override
            public void setGestureRecognitionRecyclerVisibility(boolean visibility) {
                hasGesture = visibility;
            }

            @Override
            public void setActionRecognitionRecyclerVisibility(boolean visibility) {
                //临时写的
                if (hasGesture) {
                    mBaseGlActivity.setRecognitionRecyclerVisibility(BaseGlActivity.RECOGNITION_TYPE_GESTURE, true);
                } else if (visibility) {
                    mBaseGlActivity.setRecognitionRecyclerVisibility(BaseGlActivity.RECOGNITION_TYPE_ACTION, true);
                } else {
                    mBaseGlActivity.setRecognitionRecyclerVisibility(BaseGlActivity.RECOGNITION_TYPE_NONE, false);
                }
            }

            @Override
            public void setExpressionRecyclerVisibility(boolean visibility) {
                mBaseGlActivity.setExpressionRecyclerVisibility(visibility);
            }

            @Override
            public void setTongueTrackRecyclerVisibility(boolean visibility) {
                mBaseGlActivity.setTongueTrackRecyclerVisibility(visibility);
            }

            @Override
            public void setEmotionTrackRecyclerVisibility(boolean visibility) {
                mBaseGlActivity.setEmotionTrackRecyclerVisibility(visibility);
            }

            @Override
            public void setNormalOrAvatarMode(TypeEnum typeEnum) {
                mBaseGlActivity.setNormalOrAvatarMode(typeEnum);
            }

            @Override
            public void sure() {
                mBaseGlActivity.sure();
                dismissSelf();
            }
        });

        //这个方法要在监听后面调用
        mEffectControllerView.setFaceDefaultPosition(0);
        return view;
    }

    public void bindCurrentRenderer() {
        if (isOnCreate) {
            isOnCreate = false;
            return;
        }

        if (mEffectFactory != null)
            mEffectFactory.bindCurrentRenderer();
    }

    public TrackType getSelectedTrackType() {
        return mEffectControllerView.getSelectedTrackType();
    }

    /**
     * 重新渲染一下控件
     */
    public void setTakeEffects() {
        if (mEffectControllerView != null)
            mEffectControllerView.setTakeEffects();
    }


    private class ViewClickListener extends OnMultiClickListener {
        @Override
        protected void onMultiClick(View v) {
            switch (v.getId()) {
                case R.id.fl_config_mask: {
                    dismissSelf();
                }
            }
        }
    }


    private void dismissSelf() {
        FragmentTransaction fragmentTransaction = mBaseGlActivity.getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_right);
        fragmentTransaction.hide(ConfigFragment.this).commit();
        mBaseGlActivity.setMaskVisibility(false);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (mOnFragmentHiddenListener != null) {
            mOnFragmentHiddenListener.onHiddenChanged(hidden);
        }
    }

    private OnFragmentHiddenListener mOnFragmentHiddenListener;

    public void setOnFragmentHiddenListener(OnFragmentHiddenListener onFragmentHiddenListener) {
        mOnFragmentHiddenListener = onFragmentHiddenListener;
    }

    public interface OnFragmentHiddenListener {
        /**
         * fragment 可见性变化
         *
         * @param hidden
         */
        void onHiddenChanged(boolean hidden);
    }
}
