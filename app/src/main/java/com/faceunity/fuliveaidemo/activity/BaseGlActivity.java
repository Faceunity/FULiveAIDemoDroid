package com.faceunity.fuliveaidemo.activity;

import android.content.Intent;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.faceunity.fuliveaidemo.R;
import com.faceunity.fuliveaidemo.renderer.OnRendererListener;
import com.faceunity.fuliveaidemo.util.DensityUtils;
import com.faceunity.fuliveaidemo.util.LifecycleHandler;
import com.faceunity.fuliveaidemo.util.PhotoTaker;
import com.faceunity.fuliveaidemo.util.ScreenUtils;
import com.faceunity.fuliveaidemo.util.ToastUtil;
import com.faceunity.fuliveaidemo.view.ConfigFragment;
import com.faceunity.fuliveaidemo.view.OnMultiClickListener;
import com.faceunity.fuliveaidemo.view.adapter.BaseRecyclerAdapter;
import com.faceunity.fuliveaidemo.view.adapter.SpaceItemDecoration;
import com.faceunity.nama.FURenderer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Richie on 2020.05.21
 */
public abstract class BaseGlActivity extends AppCompatActivity implements PhotoTaker.OnPictureTakeListener,
        OnRendererListener, FURenderer.OnTrackStatusChangedListener {
    private static final String TAG = "BaseGlActivity";
    public static final int RESOURCE_TYPE_GESTURE = 421;
    public static final int RESOURCE_TYPE_ACTION = 230;
    public static final int RESOURCE_TYPE_NONE = -1;
    private ConfigFragment mConfigFragment;
    private View mViewMask;
    protected TextView mTvTrackStatus;
    protected GLSurfaceView mGlSurfaceView;
    protected PhotoTaker mPhotoTaker;
    protected ViewClickListener mViewClickListener;
    protected FURenderer mFURenderer;
    private RecyclerView mRvRecognize;
    private RecognizeAdapter mRecognizeAdapter;
    private List<RecognizeListData> mActionResourceList;
    private List<RecognizeListData> mGestureResourceList;
    private int mRecognizeIndex = -1;
    private int mRecognizeCount;
    private int mResourceListType = RESOURCE_TYPE_NONE;
    private LifecycleHandler mLifecycleHandler;
    private Runnable mUpdateRecyclerTask = new Runnable() {
        @Override
        public void run() {
            int gap = 5;
            int skip = 2;
            if (mResourceListType == RESOURCE_TYPE_ACTION) {
                gap = 6;
                skip = 1;
            } else if (mResourceListType == RESOURCE_TYPE_GESTURE) {
                gap = 5;
                skip = 2;
            }
            if (mRecognizeIndex >= 0) {
                int index = mRecognizeIndex > gap ? mRecognizeIndex + skip : mRecognizeIndex;
                mRecognizeAdapter.setItemSelected(index);
            } else {
                mRecognizeAdapter.clearSingleItemSelected();
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawable(null);
        ScreenUtils.fullScreen(this);
        setContentView(R.layout.activity_base);

        mViewClickListener = new ViewClickListener();
        findViewById(R.id.iv_home).setOnClickListener(mViewClickListener);
        findViewById(R.id.iv_config).setOnClickListener(mViewClickListener);
        mTvTrackStatus = findViewById(R.id.tv_track_status);
        mViewMask = findViewById(R.id.v_mask);
        mGlSurfaceView = findViewById(R.id.gl_surface);
        mRvRecognize = findViewById(R.id.rv_gesture);
        mRvRecognize.setTag(false);

        mPhotoTaker = new PhotoTaker();
        mPhotoTaker.setOnPictureTakeListener(this);
        mLifecycleHandler = new LifecycleHandler(getLifecycle());

        initView();
        initGlRenderer();
        initFuRenderer();
    }

    @Override
    public void onSurfaceCreated() {
        mFURenderer.onSurfaceCreated();
    }

    @Override
    public void onSurfaceChanged(int viewWidth, int viewHeight) {

    }

    @Override
    public void onSurfaceDestroy() {
        mFURenderer.onSurfaceDestroyed();
    }

    @Override
    public void onTrackStatusChanged(final int type, final int status) {
//        LogUtils.d((type == FURenderer.TRACK_TYPE_FACE ? "face" : (type == FURenderer.TRACK_TYPE_HUMAN ? "human" : "gesture")), status);
        // TODO: 2020/6/30 0030 超复杂
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mConfigFragment != null) {
                    int selectedTrackType = mConfigFragment.getSelectedTrackType();
                    if (type == selectedTrackType) {
                        boolean invisible = status > 0;
                        mTvTrackStatus.setVisibility(invisible ? View.INVISIBLE : View.VISIBLE);
                        int toastStringId = mConfigFragment.getToastStringId();
                        if (!invisible) {
                            if (toastStringId > 0) {
                                mTvTrackStatus.setText(toastStringId);
                            } else {
                                mTvTrackStatus.setVisibility(View.INVISIBLE);
                            }
                        } else {
                            mTvTrackStatus.setVisibility(View.INVISIBLE);
                        }
                    } else if (selectedTrackType == 0) {
                        mTvTrackStatus.setVisibility(View.INVISIBLE);
                    }
                } else {
                    mTvTrackStatus.setVisibility(View.INVISIBLE);
                }
            }
        });
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

    protected void trackHuman() {
        if (mResourceListType == RESOURCE_TYPE_NONE) {
            return;
        }
        int index = -1;
        if (mResourceListType == RESOURCE_TYPE_ACTION) {
            int type = mFURenderer.matchHumanAction();
            index = type;
        } else if (mResourceListType == RESOURCE_TYPE_GESTURE) {
            int type = mFURenderer.matchHumanGesture();
            index = convertToGestureIndex(type);
        }
        if (mRecognizeIndex == index) {
            mRecognizeCount++;
        } else {
            mRecognizeCount = 0;
        }
        mRecognizeIndex = index;
        // 连续三帧检测到才认为成功
        if (mRecognizeCount >= 3) {
            mLifecycleHandler.post(mUpdateRecyclerTask);
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

    public void changeRecyclerVisibility(int resourceListType, boolean isSelected) {
        mFURenderer.resetTrackStatus();
        if (isSelected) {
            mResourceListType = resourceListType;
        } else {
            mResourceListType = RESOURCE_TYPE_NONE;
        }
        View recordView = getRecordView();
        if (recordView != null) {
            ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) recordView.getLayoutParams();
            int dpSize = isSelected ? 134 : 84;
            layoutParams.bottomMargin = isSelected ? DensityUtils.dp2px(this, dpSize) : DensityUtils.dp2px(this, dpSize);
            recordView.setLayoutParams(layoutParams);
        }

        mRvRecognize.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        if (!isSelected) {
            return;
        }
        List<RecognizeListData> dataSet;
        if (resourceListType == RESOURCE_TYPE_GESTURE) {
            if (mGestureResourceList == null) {
                mGestureResourceList = initGestureList();
            }
            dataSet = mGestureResourceList;
        } else if (resourceListType == RESOURCE_TYPE_ACTION) {
            if (mActionResourceList == null) {
                mActionResourceList = initActionList();
            }
            dataSet = mActionResourceList;
        } else {
            dataSet = new ArrayList<>();
        }
        if (mRecognizeAdapter == null) {
            mRvRecognize.setHasFixedSize(true);
            mRvRecognize.setLayoutManager(new GridLayoutManager(BaseGlActivity.this, 8));
            ((SimpleItemAnimator) mRvRecognize.getItemAnimator()).setSupportsChangeAnimations(false);
            int spacePx = DensityUtils.dp2px(BaseGlActivity.this, 1.5f);
            mRvRecognize.addItemDecoration(new SpaceItemDecoration(spacePx, spacePx));
            mRecognizeAdapter = new RecognizeAdapter(new ArrayList<>(dataSet));
            mRecognizeAdapter.setCanManualClick(false);
            mRvRecognize.setAdapter(mRecognizeAdapter);
        } else {
            mRecognizeAdapter.replaceAll(dataSet);
        }
    }

    public void setMaskVisibility(boolean visible) {
        int duration = getResources().getInteger(android.R.integer.config_mediumAnimTime);
        float alpha = visible ? 1F : 0F;
        mViewMask.animate().alpha(alpha).setDuration(duration).start();
    }

    public void setRenderMode(final int renderMode) {
        Runnable callback = new Runnable() {
            @Override
            public void run() {
                onRenderModeChanged(renderMode);
            }
        };
        if (renderMode == FURenderer.RENDER_MODE_NORMAL) {
            mFURenderer.destroyController(callback);
        } else if (renderMode == FURenderer.RENDER_MODE_CONTROLLER) {
            mFURenderer.loadController(callback);
        }
    }

    public FURenderer getFuRenderer() {
        return mFURenderer;
    }

    private List<RecognizeListData> initGestureList() {
        List<RecognizeListData> gestureResourceList = new ArrayList<>(16);
        gestureResourceList.add(new RecognizeListData(R.drawable.demo_gesture_icon_love));
        gestureResourceList.add(new RecognizeListData(R.drawable.demo_gesture_icon_one_handed));
        gestureResourceList.add(new RecognizeListData(R.drawable.demo_gesture_icon_hands));
        gestureResourceList.add(new RecognizeListData(R.drawable.demo_gesture_icon_clenched_fist));
        gestureResourceList.add(new RecognizeListData(R.drawable.demo_gesture_icon_hands_together));
        gestureResourceList.add(new RecognizeListData(R.drawable.demo_gesture_icon_hands_take_pictures));
        gestureResourceList.add(new RecognizeListData(R.drawable.demo_action_icon_transparent, true));
        gestureResourceList.add(new RecognizeListData(R.drawable.demo_action_icon_transparent, true));
        gestureResourceList.add(new RecognizeListData(R.drawable.demo_gesture_icon_one));
        gestureResourceList.add(new RecognizeListData(R.drawable.demo_gesture_icon_two));
        gestureResourceList.add(new RecognizeListData(R.drawable.demo_gesture_icon_three));
        gestureResourceList.add(new RecognizeListData(R.drawable.demo_gesture_icon_five));
        gestureResourceList.add(new RecognizeListData(R.drawable.demo_gesture_icon_six));
        gestureResourceList.add(new RecognizeListData(R.drawable.demo_gesture_icon_fist));
        gestureResourceList.add(new RecognizeListData(R.drawable.demo_gesture_icon_palm_up));
        gestureResourceList.add(new RecognizeListData(R.drawable.demo_gesture_icon_thumb_up));
        return Collections.unmodifiableList(gestureResourceList);
    }

    private List<RecognizeListData> initActionList() {
        List<RecognizeListData> actionResourceList = new ArrayList<>(16);
        actionResourceList.add(new RecognizeListData(R.drawable.demo_action_icon_0));
        actionResourceList.add(new RecognizeListData(R.drawable.demo_action_icon_1));
        actionResourceList.add(new RecognizeListData(R.drawable.demo_action_icon_2));
        actionResourceList.add(new RecognizeListData(R.drawable.demo_action_icon_3));
        actionResourceList.add(new RecognizeListData(R.drawable.demo_action_icon_4));
        actionResourceList.add(new RecognizeListData(R.drawable.demo_action_icon_5));
        actionResourceList.add(new RecognizeListData(R.drawable.demo_action_icon_6));
        actionResourceList.add(new RecognizeListData(R.drawable.demo_action_icon_transparent, true));
        actionResourceList.add(new RecognizeListData(R.drawable.demo_action_icon_7));
        actionResourceList.add(new RecognizeListData(R.drawable.demo_action_icon_8));
        actionResourceList.add(new RecognizeListData(R.drawable.demo_action_icon_9));
        actionResourceList.add(new RecognizeListData(R.drawable.demo_action_icon_10));
        actionResourceList.add(new RecognizeListData(R.drawable.demo_action_icon_11));
        actionResourceList.add(new RecognizeListData(R.drawable.demo_action_icon_12));
        actionResourceList.add(new RecognizeListData(R.drawable.demo_action_icon_13));
        actionResourceList.add(new RecognizeListData(R.drawable.demo_action_icon_14));
        return Collections.unmodifiableList(actionResourceList);
    }

    private static class RecognizeAdapter extends BaseRecyclerAdapter<RecognizeListData> {

        RecognizeAdapter(@NonNull List<RecognizeListData> data) {
            super(data, R.layout.rv_recognize);
        }

        @Override
        protected void bindViewHolder(BaseViewHolder viewHolder, RecognizeListData item) {
            viewHolder.setImageResource(R.id.iv_item_gesture, item.iconId);
            viewHolder.setBackground(R.id.iv_item_gesture, item.transparent ? android.R.color.transparent : R.drawable.selector_gesture_item);
        }

    }

    private static class RecognizeListData {
        private int iconId;
        private boolean transparent;

        RecognizeListData(int iconId, boolean transparent) {
            this.iconId = iconId;
            this.transparent = transparent;
        }

        RecognizeListData(int iconId) {
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
                        mConfigFragment = new ConfigFragment();
                        mConfigFragment.initListData(BaseGlActivity.this, containHumanAvatar());
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
