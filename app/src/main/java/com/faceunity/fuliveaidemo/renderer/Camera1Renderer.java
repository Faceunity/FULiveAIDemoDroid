package com.faceunity.fuliveaidemo.renderer;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.util.Log;

import androidx.lifecycle.Lifecycle;

import com.faceunity.fuliveaidemo.R;
import com.faceunity.fuliveaidemo.gles.core.GlUtil;
import com.faceunity.fuliveaidemo.util.CameraUtils;

import java.util.Map;

/**
 * 针对 Camera API 的渲染封装
 *
 * @author Richie on 2019.08.23
 */
public class Camera1Renderer extends BaseCameraRenderer implements Camera.PreviewCallback {
    private static final String TAG = "Camera1Renderer";
    private final Object mCameraLock = new Object();
    private byte[][] mPreviewCallbackBufferArray;
    private Camera mCamera;
    private int mFrontCameraId;
    private int mBackCameraId;

    public Camera1Renderer(Lifecycle lifecycle, Activity activity, GLSurfaceView glSurfaceView, OnCameraRendererListener onRendererStatusListener) {
        super(lifecycle, activity, glSurfaceView, onRendererStatusListener);
    }

    @Override
    protected void initCameraInfo() {
        int number = Camera.getNumberOfCameras();
        if (number <= 0) {
            throw new RuntimeException("No camera");
        }

        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < number; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mFrontCameraId = i;
                mFrontCameraOrientation = cameraInfo.orientation;
            } else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                mBackCameraId = i;
                mBackCameraOrientation = cameraInfo.orientation;
            }
        }

        mCameraOrientation = mCameraFacing == FACE_FRONT ? mFrontCameraOrientation : mBackCameraOrientation;
        Log.i(TAG, "initCameraInfo. frontCameraId:" + mFrontCameraId + ", frontCameraOrientation:"
                + mFrontCameraOrientation + ", backCameraId:" + mBackCameraId + ", backCameraOrientation:"
                + mBackCameraOrientation);
    }

    @Override
    protected void openCamera(int cameraFacing) {
        try {
            synchronized (mCameraLock) {
                boolean isFront = cameraFacing == FACE_FRONT;
                int cameraId = isFront ? mFrontCameraId : mBackCameraId;
                mCamera = Camera.open(cameraId);
                if (mCamera == null) {
                    throw new RuntimeException("No camera");
                }

                CameraUtils.setCameraDisplayOrientation(mActivity, cameraId, mCamera);
                Log.i(TAG, "openCamera. facing: " + (isFront ? "front" : "back") + ", orientation:"
                        + mCameraOrientation + ", previewWidth:" + mCameraWidth + ", previewHeight:"
                        + mCameraHeight + ", thread:" + Thread.currentThread().getName());

                Camera.Parameters parameters = mCamera.getParameters();
                CameraUtils.setFocusModes(parameters);
                CameraUtils.chooseFrameRate(parameters);
                int[] size = CameraUtils.choosePreviewSize(parameters, mCameraWidth, mCameraHeight);
                mCameraWidth = size[0];
                mCameraHeight = size[1];
                parameters.setPreviewFormat(ImageFormat.NV21);
                CameraUtils.setParameters(mCamera, parameters);
                mOnRendererStatusListener.onCameraOpened(mCameraWidth, mCameraHeight);

                // log camera all parameters
                if (CameraUtils.DEBUG) {
                    Map<String, String> fullCameraParameters = CameraUtils.getFullCameraParameters(mCamera);
                    String fullParams = fullCameraParameters.toString();
                    // log message is too long, so split it.
                    if (fullParams.length() > 1000) {
                        int trunk = fullParams.length() / 1000 + 1;
                        for (int i = 0; i < trunk; i++) {
                            int end = i == trunk - 1 ? fullParams.length() : (i + 1) * 1000;
                            String substring = fullParams.substring(i * 1000, end);
                            Log.v(TAG, "AFTER SET camera parameters: " + substring);
                        }
                    }
                }

                if (mViewWidth > 0 && mViewHeight > 0) {
                    mMvpMatrix = GlUtil.changeMVPMatrixCrop(mViewWidth, mViewHeight, mCameraHeight, mCameraWidth);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "openCamera: ", e);
            mOnRendererStatusListener.onCameraError(mActivity.getString(R.string.camera_open_failed));
        }
    }

    @Override
    protected void startPreview() {
        if (mCameraTexId <= 0) {
            return;
        }
        Log.d(TAG, "startPreview. isPreviewing:" + mIsPreviewing + ", cameraTexId:" + mCameraTexId + ", camera:" + mCamera);
        try {
            synchronized (mCameraLock) {
                if (mCamera == null || mIsPreviewing) {
                    return;
                }
                mCamera.stopPreview();
                if (mPreviewCallbackBufferArray == null) {
                    mPreviewCallbackBufferArray = new byte[PREVIEW_BUFFER_SIZE][mCameraWidth * mCameraHeight * ImageFormat.getBitsPerPixel(ImageFormat.NV21) / 8];
                }
                // must call after stopPreview
                mCamera.setPreviewCallbackWithBuffer(this);
                for (byte[] bytes : mPreviewCallbackBufferArray) {
                    mCamera.addCallbackBuffer(bytes);
                }
                if (mSurfaceTexture == null) {
                    mSurfaceTexture = new SurfaceTexture(mCameraTexId);
                }
                mCamera.setPreviewTexture(mSurfaceTexture);
                mCamera.startPreview();
                mIsPreviewing = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "cameraStartPreview: ", e);
            mOnRendererStatusListener.onCameraError(mActivity.getString(R.string.camera_preview_failed));
        }
    }

    @Override
    protected void closeCamera() {
        Log.d(TAG, "closeCamera. thread:" + Thread.currentThread().getName());
        try {
            synchronized (mCameraLock) {
                if (mCamera != null) {
                    mCamera.stopPreview();
                    mCamera.setPreviewTexture(null);
                    mCamera.setPreviewCallbackWithBuffer(null);
                    mCamera.release();
                    mCamera = null;
                }
                mIsPreviewing = false;
            }
        } catch (Exception e) {
            Log.e(TAG, "releaseCamera: ", e);
            mOnRendererStatusListener.onCameraError(mActivity.getString(R.string.camera_close_failed));
        }
        super.closeCamera();
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        // called on CameraRenderer thread
        mCameraNv21Byte = data;
        if (!mIsStopPreview) {
            mCamera.addCallbackBuffer(data);
            mGlSurfaceView.requestRender();
        }
    }

    @Override
    public void changeResolution(final int cameraWidth, final int cameraHeight) {
        super.changeResolution(cameraWidth, cameraHeight);
        Log.d(TAG, "changeResolution() cameraWidth = [" + cameraWidth + "], cameraHeight = [" + cameraHeight + "]");
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                mIsStopPreview = true;
                mCameraWidth = cameraWidth;
                mCameraHeight = cameraHeight;
                mPreviewCallbackBufferArray = null;
                closeCamera();
                openCamera(mCameraFacing);
                mOnRendererStatusListener.onCameraChanged(mCameraFacing, mCameraOrientation);
                startPreview();
                mIsStopPreview = false;
            }
        });
    }

}
