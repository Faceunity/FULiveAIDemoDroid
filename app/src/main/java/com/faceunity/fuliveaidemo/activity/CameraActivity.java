package com.faceunity.fuliveaidemo.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.faceunity.FUConfig;
import com.faceunity.core.entity.FUCameraConfig;
import com.faceunity.core.entity.FURenderFrameData;
import com.faceunity.core.entity.FURenderInputData;
import com.faceunity.core.entity.FURenderOutputData;
import com.faceunity.core.enumeration.CameraFacingEnum;
import com.faceunity.core.faceunity.FUAIKit;
import com.faceunity.core.faceunity.FURenderKit;
import com.faceunity.core.listener.OnGlRendererListener;
import com.faceunity.core.media.video.OnVideoRecordingListener;
import com.faceunity.core.media.video.VideoRecordHelper;
import com.faceunity.core.renderer.CameraRenderer;
import com.faceunity.core.utils.GlUtil;
import com.faceunity.fuliveaidemo.R;
import com.faceunity.fuliveaidemo.util.DecimalUtils;
import com.faceunity.fuliveaidemo.util.FileUtils;
import com.faceunity.fuliveaidemo.util.ToastUtil;
import com.faceunity.fuliveaidemo.view.RecordButton;
import com.faceunity.nama.FURenderer;
import com.faceunity.nama.utils.FuDeviceUtils;
import com.faceunity.nama.view.listener.TypeEnum;

import java.io.File;

/**
 * @author Richie on 2020.05.21
 */
public class CameraActivity extends BaseGlActivity implements RecordButton.OnRecordListener,
        FURenderer.OnDebugListener, OnGlRendererListener {
    private CameraRenderer mCameraRenderer;
    private TextView mTvDebugInfo;
    private FUCameraConfig mFuCameraConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mVideoRecordHelper = new VideoRecordHelper(this, mOnVideoRecordingListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraRenderer.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraRenderer.onPause();
        if (isRecording) {
            isRecording = false;
            onStopRecord();
        }
    }

    @Override
    protected void onDestroy() {
        mCameraRenderer.onDestroy();
        super.onDestroy();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mCameraRenderer.onTouchEvent((int) event.getX(), (int) event.getY(), event.getAction());
        return super.onTouchEvent(event);
    }

    @Override
    protected void initView() {
        findViewById(R.id.iv_save_photo).setVisibility(View.GONE);
        mRecordBtn.setOnRecordListener(this);
        mTvDebugInfo = findViewById(R.id.tv_debug_info);
        mTvDebugInfo.setVisibility(View.VISIBLE);
        mTvDebugInfo.setTag(false);
        findViewById(R.id.iv_debug).setOnClickListener(mViewClickListener);
        findViewById(R.id.iv_switch_cam).setOnClickListener(mViewClickListener);
    }

    @Override
    protected void initFuRenderer() {
        mFURenderer = FURenderer.getInstance();
    }

    @Override
    protected void initGlRenderer() {
        mFuCameraConfig = new FUCameraConfig();
        mCameraRenderer = new CameraRenderer(mGlSurfaceView, mFuCameraConfig, this);
    }

    @Override
    protected View getRecordView() {
        return mRecordBtn;
    }

    @Override
    protected void onRenderModeChanged(int renderMode) {
        super.onRenderModeChanged(renderMode);
        final boolean renderController = renderMode == FURenderer.RENDER_MODE_CONTROLLER;
        runOnUiThread(() -> mRecordBtn.setVisibility(renderController ? View.INVISIBLE : View.VISIBLE));
    }

    @Override
    public void onFpsChanged(final double fps, final double renderTime, double elapsedTime) {
        final float[] faceRotationEuler = mFURenderer.getFaceRotationEuler();
        boolean isShow = (boolean) mTvDebugInfo.getTag();
        if (!isShow) {
            runOnUiThread(() -> {
                boolean isAllZero = true;
                for (float v : faceRotationEuler) {
                    if (!DecimalUtils.floatEquals(v, 0f)) {
                        isAllZero = false;
                        break;
                    }
                }
                String upperText = "resolution:\n" + mFuCameraConfig.cameraWidth + "x" + mFuCameraConfig.cameraHeight
                        + "\nfps: " + (int) fps + "\nframe cost: " + (int) renderTime + "ms\n";
                String lowerText;
                if (isAllZero || mTypeEnum == TypeEnum.AVATAR) {
                    lowerText = "yaw: null\npitch: null\nroll: null";
                } else {
                    boolean isFront = mFuCameraConfig.cameraFacing == CameraFacingEnum.CAMERA_FRONT;
                    float yaw = isFront ? -faceRotationEuler[1] : faceRotationEuler[1];
                    float pitch = faceRotationEuler[0];
                    float roll = isFront ? -faceRotationEuler[2] : faceRotationEuler[2];
                    lowerText = String.format("yaw: %.2f°\npitch: %.2f°\nroll: %.2f°", yaw, pitch, roll);
                }
                mTvDebugInfo.setText(upperText.concat(lowerText));
            });
        }
    }

    @Override
    public void takePicture() {
        isTakePhoto = true;
    }

    @Override
    public void startRecord() {
        if (!isRecording) {
            isRecording = true;
            onStartRecord();
        }
    }

    @Override
    public void stopRecord() {
        if (isRecording) {
            isRecording = false;
            onStopRecord();
        }
    }

    @Override
    public void setNormalOrAvatarMode(TypeEnum mode) {
        super.setNormalOrAvatarMode(mode);
        if (mode == TypeEnum.AVATAR) {
            mCameraRenderer.drawSmallViewport(true);
        } else {
            mCameraRenderer.drawSmallViewport(false);
        }
    }

    @Override
    protected void onViewClicked(int id) {
        super.onViewClicked(id);
        switch (id) {
            case R.id.iv_switch_cam: {
                mCameraRenderer.switchCamera();
            }
            break;
            case R.id.iv_debug: {
                boolean isShow = (boolean) mTvDebugInfo.getTag();
                mTvDebugInfo.setTag(!isShow);
                mTvDebugInfo.setVisibility(isShow ? View.VISIBLE : View.GONE);
            }
            break;
            default:
        }
    }

    @Override
    public void onDrawFrameAfter() {

    }

    @Override
    public void onRenderAfter(FURenderOutputData fuRenderOutputData, FURenderFrameData fuRenderFrameData) {
        mFURenderer.benchmarkFPS(this);
        trackFace();
        trackHuman();
        queryTrackStatus();
        recordingPhoto(fuRenderOutputData, fuRenderFrameData.getTexMatrix());
        recordingVideo(fuRenderOutputData, fuRenderFrameData.getTexMatrix());
    }

    @Override
    public void onRenderBefore(@Nullable FURenderInputData fuRenderInputData) {
        fuRenderInputData.setImageBuffer(null);
        fuRenderInputData.getRenderConfig().setNeedBufferReturn(false);
        mFURenderer.setCallStartTime(System.nanoTime());
    }

    @Override
    public void onSurfaceChanged(int i, int i1) {

    }

    @Override
    public void onSurfaceCreated() {
        FUAIKit.getInstance().setMaxFaces(FUConfig.FU_MAX_FACE);
        FUAIKit.getInstance().faceProcessorSetFaceLandmarkQuality(FUConfig.DEVICE_LEVEL >= 2 ? 2 : 1);
        //高端机开启小脸检测
        if (FUConfig.DEVICE_LEVEL > FuDeviceUtils.DEVICE_LEVEL_ONE)
            FUAIKit.getInstance().fuFaceProcessorSetDetectSmallFace(true);
    }

    @Override
    public void onSurfaceDestroy() {
        FURenderKit.getInstance().release();
    }

    //region 视频录制
    private VideoRecordHelper mVideoRecordHelper;
    private volatile boolean isRecordingPrepared = false;
    private boolean isRecording = false;
    private volatile long recordTime = 0;

    protected void onStartRecord() {
        mVideoRecordHelper.startRecording(mGlSurfaceView, mCameraRenderer.getFUCamera().getCameraHeight(), mCameraRenderer.getFUCamera().getCameraWidth());
    }

    protected void onStopRecord() {
        mRecordBtn.setSecond(0);
        mVideoRecordHelper.stopRecording();
    }

    private OnVideoRecordingListener mOnVideoRecordingListener = new OnVideoRecordingListener() {

        @Override
        public void onPrepared() {
            isRecordingPrepared = true;
        }

        @Override
        public void onProcess(Long time) {
            recordTime = time;
            runOnUiThread(() -> {
                if (isRecording) {
                    mRecordBtn.setSecond(time);
                }
            });

        }

        @Override
        public void onFinish(File file) {
            isRecordingPrepared = false;

            if (recordTime < 1100) {
                runOnUiThread(() -> ToastUtil.makeText(CameraActivity.this, R.string.save_video_too_short).show());
            } else {
                String filePath = FileUtils.addVideoToAlbum(CameraActivity.this, file);
                if (filePath == null || filePath.trim().length() == 0) {
                    runOnUiThread(() -> ToastUtil.makeText(CameraActivity.this, R.string.save_video_failure).show());
                } else {
                    sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
                    runOnUiThread(() -> ToastUtil.makeText(CameraActivity.this, R.string.save_video_success).show());
                }
            }
            if (file.exists()) {
                file.delete();
            }
        }

    };

    private void recordingVideo(FURenderOutputData outputData, float[] texMatrix) {
        if (outputData == null || outputData.getTexture() == null || outputData.getTexture().getTexId() <= 0) {
            return;
        }
        if (isRecordingPrepared) {
            mVideoRecordHelper.frameAvailableSoon(outputData.getTexture().getTexId(), texMatrix, GlUtil.IDENTITY_MATRIX);
        }
    }
    //endregion 视频录制
}
