package com.faceunity.fuliveaidemo.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import com.faceunity.FUConfig;
import com.faceunity.core.entity.FURenderFrameData;
import com.faceunity.core.entity.FURenderInputData;
import com.faceunity.core.entity.FURenderOutputData;
import com.faceunity.core.enumeration.FUFaceProcessorDetectModeEnum;
import com.faceunity.core.enumeration.FUHumanProcessorDetectModeEnum;
import com.faceunity.core.faceunity.FUAIKit;
import com.faceunity.core.faceunity.FURenderKit;
import com.faceunity.core.listener.OnGlRendererListener;
import com.faceunity.core.renderer.PhotoRenderer;
import com.faceunity.fuliveaidemo.R;
import com.faceunity.nama.FURenderer;
import com.faceunity.nama.utils.FuDeviceUtils;
import com.faceunity.nama.utils.LogUtils;
import com.faceunity.nama.view.listener.TypeEnum;

/**
 * @author Richie on 2020.05.21
 */
public class PhotoActivity extends BaseGlActivity implements OnGlRendererListener {
    private static final String TAG = "PhotoActivity";
    public static final String PHOTO_PATH = "photo_path";
    private View mIvSavePhoto;
    private PhotoRenderer photoRenderer;

    @Override
    protected void onResume() {
        super.onResume();
        photoRenderer.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        photoRenderer.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        photoRenderer.onDestroy();
    }

    public static void actionStart(Activity context, String photoPath) {
        Intent intent = new Intent(context, PhotoActivity.class);
        intent.putExtra(PHOTO_PATH, photoPath);
        context.startActivity(intent);
    }

    @Override
    protected void initView() {
        findViewById(R.id.iv_switch_cam).setVisibility(View.GONE);
        findViewById(R.id.iv_debug).setVisibility(View.GONE);
        mIvSavePhoto = findViewById(R.id.iv_save_photo);
        mIvSavePhoto.setOnClickListener(mViewClickListener);
    }

    @Override
    public void setNormalOrAvatarMode(TypeEnum typeEnum) {
        super.setNormalOrAvatarMode(typeEnum);
        mRecordBtn.setVisibility(View.GONE);
    }

    @Override
    protected void initFuRenderer() {
        mFURenderer = FURenderer.getInstance();
    }

    @Override
    protected void initGlRenderer() {
        String photoPath = getIntent().getStringExtra(PHOTO_PATH);
        LogUtils.debug(TAG, "photoPath: %s", photoPath);
        photoRenderer = new PhotoRenderer(mGlSurfaceView, photoPath, this);
    }

    @Override
    protected void onViewClicked(int id) {
        super.onViewClicked(id);
        if (id == R.id.iv_save_photo) {
            isTakePhoto = true;
        }
    }

    @Override
    protected View getRecordView() {
        return mIvSavePhoto;
    }

    @Override
    protected boolean containHumanAvatar() {
        return false;
    }

    @Override
    public void onDrawFrameAfter() {

    }

    @Override
    public void onRenderAfter(FURenderOutputData fuRenderOutputData, FURenderFrameData fuRenderFrameData) {
        trackFace();
        trackHuman();
        queryTrackStatus();
        recordingPhoto(fuRenderOutputData, fuRenderFrameData.getTexMatrix());
    }

    @Override
    public void onRenderBefore(FURenderInputData fuRenderInputData) {
        FUAIKit.getInstance().humanProcessorReset();
    }

    @Override
    public void onSurfaceChanged(int i, int i1) {

    }

    @Override
    public void onSurfaceCreated() {
        FUAIKit.getInstance().setMaxFaces(FUConfig.FU_MAX_FACE);
        FUAIKit.getInstance().faceProcessorSetDetectMode(FUFaceProcessorDetectModeEnum.IMAGE);
        FUAIKit.getInstance().setHumanProcessorDetectMode(FUHumanProcessorDetectModeEnum.IMAGE);
        // 高精度 2 级以上
        FUAIKit.getInstance().faceProcessorSetFaceLandmarkQuality(FUConfig.DEVICE_LEVEL >= 2 ? 2 : 1);
        // 高精度增强遮挡 先关闭，用户主动开
        FUAIKit.getInstance().fuFaceProcessorSetFaceLandmarkHpOccu(0);
        //高端机开启小脸检测
        if (FUConfig.DEVICE_LEVEL > FuDeviceUtils.DEVICE_LEVEL_ONE)
            FUAIKit.getInstance().fuFaceProcessorSetDetectSmallFace(true);
    }

    @Override
    public void onSurfaceDestroy() {
        FUAIKit.getInstance().faceProcessorSetDetectMode(FUFaceProcessorDetectModeEnum.VIDEO);
        FUAIKit.getInstance().setHumanProcessorDetectMode(FUHumanProcessorDetectModeEnum.VIDEO);
        FURenderKit.getInstance().release();
    }
}
