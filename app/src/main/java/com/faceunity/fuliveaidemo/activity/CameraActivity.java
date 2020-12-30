package com.faceunity.fuliveaidemo.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.faceunity.fuliveaidemo.R;
import com.faceunity.fuliveaidemo.gles.core.GlUtil;
import com.faceunity.fuliveaidemo.renderer.BaseCameraRenderer;
import com.faceunity.fuliveaidemo.renderer.Camera1Renderer;
import com.faceunity.fuliveaidemo.renderer.OnCameraRendererListener;
import com.faceunity.fuliveaidemo.util.CameraUtils;
import com.faceunity.fuliveaidemo.util.DecimalUtils;
import com.faceunity.fuliveaidemo.util.LifeCycleSensorManager;
import com.faceunity.fuliveaidemo.util.ToastUtil;
import com.faceunity.fuliveaidemo.util.VideoRecorder;
import com.faceunity.fuliveaidemo.view.RecordButton;
import com.faceunity.nama.FURenderer;

import java.io.File;

/**
 * @author Richie on 2020.05.21
 */
public class CameraActivity extends BaseGlActivity implements OnCameraRendererListener, RecordButton.OnRecordListener,
        FURenderer.OnDebugListener, LifeCycleSensorManager.OnAccelerometerChangedListener, VideoRecorder.OnVideoRecordListener {
    private BaseCameraRenderer mCameraRenderer;
    private RecordButton mRecordBtn;
    private TextView mTvDebugInfo;
    private VideoRecorder mVideoRecorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LifeCycleSensorManager lifeCycleSensorManager = new LifeCycleSensorManager(this, getLifecycle());
        lifeCycleSensorManager.setOnAccelerometerChangedListener(this);

        mVideoRecorder = new VideoRecorder(mGlSurfaceView);
        mVideoRecorder.setOnVideoRecordListener(this);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mCameraRenderer.onTouchEvent((int) event.getX(), (int) event.getY(), event.getAction());
        return super.onTouchEvent(event);
    }

    @Override
    protected void initView() {
        findViewById(R.id.iv_save_photo).setVisibility(View.GONE);
        mRecordBtn = findViewById(R.id.btn_record_video);
        mRecordBtn.setOnRecordListener(this);
        mTvDebugInfo = findViewById(R.id.tv_debug_info);
        mTvDebugInfo.setVisibility(View.VISIBLE);
        mTvDebugInfo.setTag(false);
        findViewById(R.id.iv_debug).setOnClickListener(mViewClickListener);
        findViewById(R.id.iv_switch_cam).setOnClickListener(mViewClickListener);
    }

    @Override
    protected void initFuRenderer() {
        mFURenderer = new FURenderer.Builder(this)
                .setCameraFacing(mCameraRenderer.getCameraFacing())
                .setInputTextureType(FURenderer.INPUT_TEXTURE_EXTERNAL_OES)
                .setInputImageOrientation(CameraUtils.getCameraOrientation(mCameraRenderer.getCameraFacing()))
                .setOnSystemErrorListener(this)
                .setRunBenchmark(true)
                .setOnDebugListener(this)
                .build();
    }

    @Override
    protected void initGlRenderer() {
        mCameraRenderer = new Camera1Renderer(getLifecycle(), this, mGlSurfaceView, this);
        mIsFlipX = mCameraRenderer.getCameraFacing() == FURenderer.CAMERA_FACING_FRONT;
    }

    @Override
    public void onSurfaceCreated() {
        super.onSurfaceCreated();
        mCameraRenderer.setRenderRotatedImage(false);
    }

    @Override
    protected View getRecordView() {
        return mRecordBtn;
    }

    @Override
    public int onDrawFrame(byte[] cameraNv21Byte, int cameraTexId, int cameraWidth, int cameraHeight, float[] mvpMatrix, float[] texMatrix, long timeStamp) {
        int fuTexId = mFURenderer.drawFrame(cameraNv21Byte, cameraTexId, cameraWidth, cameraHeight);
        trackFace();
        trackHuman();
        queryTrackStatus();
        mPhotoTaker.send(fuTexId, GlUtil.IDENTITY_MATRIX, texMatrix, cameraHeight, cameraWidth);
        mVideoRecorder.send(fuTexId, GlUtil.IDENTITY_MATRIX, texMatrix, timeStamp);
        return fuTexId;
    }

    @Override
    protected void onRenderModeChanged(int renderMode) {
        super.onRenderModeChanged(renderMode);
        final boolean renderController = renderMode == FURenderer.RENDER_MODE_CONTROLLER;
        mCameraRenderer.setRenderRotatedImage(renderController);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecordBtn.setVisibility(renderController ? View.INVISIBLE : View.VISIBLE);
            }
        });
    }

    @Override
    public void onCameraChanged(int cameraFacing, int cameraOrientation) {
        mIsFlipX = cameraFacing == FURenderer.CAMERA_FACING_FRONT;
        mFURenderer.onCameraChanged(cameraFacing, cameraOrientation);
    }

    @Override
    public void onCameraOpened(int cameraWidth, int cameraHeight) {
    }

    @Override
    public void onCameraError(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ToastUtil.makeText(CameraActivity.this, message);
            }
        });
    }

    @Override
    public void onFpsChanged(final double fps, final double renderTime, double elapsedTime) {
        final float[] faceRotationEuler = mFURenderer.getFaceRotationEuler();
        boolean isShow = (boolean) mTvDebugInfo.getTag();
        if (!isShow) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    boolean isAllZero = true;
                    for (float v : faceRotationEuler) {
                        if (!DecimalUtils.floatEquals(v, 0f)) {
                            isAllZero = false;
                            break;
                        }
                    }
                    String upperText = "resolution:\n" + mCameraRenderer.getCameraWidth() + "x" + mCameraRenderer.getCameraHeight()
                            + "\nfps: " + (int) fps + "\nframe cost: " + (int) renderTime + "ms\n";
                    String lowerText;
                    if (isAllZero) {
                        lowerText = "yaw: null\npitch: null\nroll: null";
                    } else {
                        boolean isFront = mCameraRenderer.getCameraFacing() == BaseCameraRenderer.FACE_FRONT;
                        float yaw = isFront ? -faceRotationEuler[1] : faceRotationEuler[1];
                        float pitch = faceRotationEuler[0];
                        float roll = isFront ? -faceRotationEuler[2] : faceRotationEuler[2];
                        lowerText = String.format("yaw: %.2f°\npitch: %.2f°\nroll: %.2f°", yaw, pitch, roll);
                    }
                    mTvDebugInfo.setText(upperText.concat(lowerText));
                }
            });
        }
    }

    @Override
    public void takePicture() {
        mPhotoTaker.mark();
    }

    @Override
    public void startRecord() {
        int videoWidth = BaseCameraRenderer.DEFAULT_PREVIEW_HEIGHT;
        int videoHeight = BaseCameraRenderer.DEFAULT_PREVIEW_WIDTH;
        mVideoRecorder.start(videoWidth, videoHeight);
    }

    @Override
    public void stopRecord() {
        mVideoRecorder.stop();
    }

    @Override
    public void onAccelerometerChanged(float x, float y, float z) {
        if (Math.abs(x) > 3 || Math.abs(y) > 3) {
            if (Math.abs(x) > Math.abs(y)) {
                mFURenderer.onDeviceOrientationChanged(x > 0 ? 0 : 180);
            } else {
                mFURenderer.onDeviceOrientationChanged(y > 0 ? 90 : 270);
            }
        }
    }

    @Override
    public void onPrepare() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecordBtn.setSecond(0);
            }
        });
    }

    @Override
    public void onStop(final boolean valid) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecordBtn.setSecond(0);
                if (!valid) {
                    ToastUtil.makeText(CameraActivity.this, R.string.save_video_too_short).show();
                }
            }
        });
    }

    @Override
    public void onProgress(final long progress) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecordBtn.setSecond(progress);
            }
        });
    }

    @Override
    public void onFinish(final String path) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(path))));
                ToastUtil.makeText(CameraActivity.this, R.string.save_video_success).show();
            }
        });
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

}
