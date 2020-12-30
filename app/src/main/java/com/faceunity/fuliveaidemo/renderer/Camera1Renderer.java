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

/**
 * 针对 Camera API 的渲染封装
 *
 * @author Richie on 2019.08.23
 */
public class Camera1Renderer extends BaseCameraRenderer implements Camera.PreviewCallback {
    private static final String TAG = "Camera1Renderer";
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
            synchronized (Camera1Renderer.class) {
                if (mCamera != null) {
                    return;
                }
                boolean isFront = cameraFacing == FACE_FRONT;
                int cameraId = isFront ? mFrontCameraId : mBackCameraId;
                Camera camera = Camera.open(cameraId);
                if (camera == null) {
                    throw new RuntimeException("No camera");
                }

                Log.i(TAG, "openCamera. facing: " + (isFront ? "front" : "back") + ", orientation:"
                        + mCameraOrientation + ", previewWidth:" + mCameraWidth + ", previewHeight:"
                        + mCameraHeight + ", thread:" + Thread.currentThread().getName());

                CameraUtils.setCameraDisplayOrientation(mActivity, cameraId, camera);
                Camera.Parameters parameters = camera.getParameters();
                CameraUtils.setFocusModes(parameters);
                CameraUtils.chooseFrameRate(parameters);
                int[] size = CameraUtils.choosePreviewSize(parameters, mCameraWidth, mCameraHeight);
                mCameraWidth = size[0];
                mCameraHeight = size[1];
                parameters.setPreviewFormat(ImageFormat.NV21);
                CameraUtils.setParameters(camera, parameters);
                mCamera = camera;
            }
            if (mViewWidth > 0 && mViewHeight > 0) {
                mMvpMatrix = GlUtil.changeMVPMatrixCrop(mViewWidth, mViewHeight, mCameraHeight, mCameraWidth);
            }
            mOnRendererStatusListener.onCameraOpened(mCameraWidth, mCameraHeight);
        } catch (Exception e) {
            Log.e(TAG, "openCamera: ", e);
            mOnRendererStatusListener.onCameraError(mActivity.getString(R.string.camera_open_failed));
        }
    }

    @Override
    protected void startPreview() {
        Camera camera = mCamera;
        if (mCameraTexId <= 0 || camera == null || mIsPreviewing) {
            return;
        }
        Log.d(TAG, "startPreview. cameraTexId:" + mCameraTexId + ", camera:" + camera);
        try {
            camera.stopPreview();
            if (mPreviewCallbackBufferArray == null) {
                mPreviewCallbackBufferArray = new byte[PREVIEW_BUFFER_SIZE][mCameraWidth * mCameraHeight
                        * ImageFormat.getBitsPerPixel(ImageFormat.NV21) / 8];
            }
            // must call after stopPreview
            camera.setPreviewCallbackWithBuffer(this);
            for (byte[] bytes : mPreviewCallbackBufferArray) {
                camera.addCallbackBuffer(bytes);
            }
            if (mSurfaceTexture == null) {
                mSurfaceTexture = new SurfaceTexture(mCameraTexId);
            }
            camera.setPreviewTexture(mSurfaceTexture);
            camera.startPreview();
            mIsPreviewing = true;
        } catch (Exception e) {
            Log.e(TAG, "cameraStartPreview: ", e);
            mOnRendererStatusListener.onCameraError(mActivity.getString(R.string.camera_preview_failed));
        }
    }

    @Override
    protected void closeCamera() {
        Log.d(TAG, "closeCamera. camera:" + mCamera);
        try {
            synchronized (Camera1Renderer.class) {
                Camera camera = mCamera;
                if (camera != null) {
                    camera.stopPreview();
                    camera.setPreviewTexture(null);
                    camera.setPreviewCallbackWithBuffer(null);
                    camera.release();
                    mCamera = null;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "releaseCamera: ", e);
            mCamera = null;
            mOnRendererStatusListener.onCameraError(mActivity.getString(R.string.camera_close_failed));
        }
        mIsPreviewing = false;
        super.closeCamera();
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        // called on CameraRenderer thread
        mCameraNv21Byte = data;
        camera.addCallbackBuffer(data);
        if (!mIsStopPreview) {
            mGlSurfaceView.requestRender();
        }
    }

    @Override
    public void changeResolution(final int cameraWidth, final int cameraHeight) {
        Log.d(TAG, "changeResolution() cameraWidth = [" + cameraWidth + "], cameraHeight = [" + cameraHeight + "]");
        super.changeResolution(cameraWidth, cameraHeight);
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
