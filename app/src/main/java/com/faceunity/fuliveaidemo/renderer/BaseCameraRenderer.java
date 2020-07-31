package com.faceunity.fuliveaidemo.renderer;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.util.Log;
import android.view.MotionEvent;

import androidx.lifecycle.Lifecycle;

import com.faceunity.fuliveaidemo.gles.ProgramTexture2d;
import com.faceunity.fuliveaidemo.gles.ProgramTextureOES;
import com.faceunity.fuliveaidemo.gles.core.GlUtil;
import com.faceunity.fuliveaidemo.util.DensityUtils;
import com.faceunity.fuliveaidemo.util.LimitFpsUtil;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author Richie on 2019.08.23
 */
public class BaseCameraRenderer extends AbstractLifeCycleRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "BaseCameraRenderer";
    public static final int FACE_BACK = Camera.CameraInfo.CAMERA_FACING_BACK;
    public static final int FACE_FRONT = Camera.CameraInfo.CAMERA_FACING_FRONT;
    public static final int FRONT_CAMERA_ORIENTATION = 270;
    public static final int BACK_CAMERA_ORIENTATION = 90;
    public static final int DEFAULT_PREVIEW_WIDTH = 1280;
    public static final int DEFAULT_PREVIEW_HEIGHT = 720;
    public static final int PREVIEW_BUFFER_SIZE = 3;
    private static final float[] TEXTURE_MATRIX = {0.0f, -1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f};
    protected int mViewWidth;
    protected int mViewHeight;
    protected boolean mIsStopPreview;
    protected int mCameraFacing = FACE_BACK;
    protected int mCameraWidth = DEFAULT_PREVIEW_WIDTH;
    protected int mCameraHeight = DEFAULT_PREVIEW_HEIGHT;
    protected int mCameraTexId;
    protected int mBackCameraOrientation = BACK_CAMERA_ORIENTATION;
    protected int mFrontCameraOrientation = FRONT_CAMERA_ORIENTATION;
    protected int mCameraOrientation = FRONT_CAMERA_ORIENTATION;
    protected float[] mMvpMatrix;
    protected float[] mTexMatrix = Arrays.copyOf(TEXTURE_MATRIX, TEXTURE_MATRIX.length);
    protected byte[] mCameraNv21Byte;
    protected byte[] mNv21ByteCopy;
    protected SurfaceTexture mSurfaceTexture;
    protected GLSurfaceView mGlSurfaceView;
    protected Activity mActivity;
    protected Handler mBackgroundHandler;
    protected boolean mIsPreviewing;
    private ProgramTextureOES mProgramTextureOES;
    private ProgramTexture2d mProgramTexture2d;
    protected int m2DTexId;
    protected OnCameraRendererListener mOnRendererStatusListener;
    private int mSmallViewportWidth;
    private int mSmallViewportHeight;
    private int mSmallViewportX;
    private int mSmallViewportY;
    private int mSmallViewportHorizontalPadding;
    private int mSmallViewportTopPadding;
    private int mSmallViewportBottomPadding;
    private boolean mDrawSmallViewport;
    private int mTouchX;
    private int mTouchY;
    private boolean mRenderRotatedImage;

    protected BaseCameraRenderer(Lifecycle lifecycle, Activity activity, GLSurfaceView glSurfaceView, OnCameraRendererListener onRendererStatusListener) {
        super(lifecycle);
        mGlSurfaceView = glSurfaceView;
        glSurfaceView.setEGLContextClientVersion(GlUtil.getSupportGLVersion(glSurfaceView.getContext()));
        glSurfaceView.setRenderer(this);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        mActivity = activity;
        mOnRendererStatusListener = onRendererStatusListener;
        initCameraInfo();
        mSmallViewportWidth = DensityUtils.dp2px(activity, 90);
        mSmallViewportHeight = DensityUtils.dp2px(activity, 160);
        mSmallViewportHorizontalPadding = DensityUtils.dp2px(activity, 16);
        mSmallViewportTopPadding = DensityUtils.dp2px(activity, 88);
        mSmallViewportBottomPadding = DensityUtils.dp2px(activity, 16);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(TAG, "onSurfaceCreated. thread:" + Thread.currentThread().getName());
        mProgramTexture2d = new ProgramTexture2d();
        mProgramTextureOES = new ProgramTextureOES();
        mCameraTexId = GlUtil.createTextureObject(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                startPreview();
            }
        });
        LimitFpsUtil.setTargetFps(LimitFpsUtil.DEFAULT_FPS);
        mOnRendererStatusListener.onSurfaceCreated();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        if (mViewWidth != width || mViewHeight != height) {
            mMvpMatrix = GlUtil.changeMVPMatrixCrop(width, height, mCameraHeight, mCameraWidth);
        }
        Log.d(TAG, "onSurfaceChanged. viewWidth:" + width + ", viewHeight:" + height
                + ". cameraOrientation:" + mCameraOrientation + ", cameraWidth:" + mCameraWidth
                + ", cameraHeight:" + mCameraHeight + ", cameraTexId:" + mCameraTexId);
        mViewWidth = width;
        mViewHeight = height;
        mSmallViewportX = width - mSmallViewportWidth - mSmallViewportHorizontalPadding;
        mSmallViewportY = mSmallViewportBottomPadding;
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                startPreview();
            }
        });
        mOnRendererStatusListener.onSurfaceChanged(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (mProgramTexture2d == null || mSurfaceTexture == null) {
            return;
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_STENCIL_BUFFER_BIT);
        try {
            mSurfaceTexture.updateTexImage();
            mSurfaceTexture.getTransformMatrix(mTexMatrix);
        } catch (Exception e) {
            Log.e(TAG, "onDrawFrame: ", e);
        }

        if (!mIsStopPreview) {
            if (mCameraNv21Byte != null) {
                if (mNv21ByteCopy == null) {
                    mNv21ByteCopy = new byte[mCameraNv21Byte.length];
                }
                System.arraycopy(mCameraNv21Byte, 0, mNv21ByteCopy, 0, mCameraNv21Byte.length);
            }
            if (mNv21ByteCopy != null) {
                m2DTexId = mOnRendererStatusListener.onDrawFrame(mNv21ByteCopy, mCameraTexId,
                        mCameraWidth, mCameraHeight, mMvpMatrix, mTexMatrix, mSurfaceTexture.getTimestamp());
            }
        }

        if (!mIsStopPreview) {
            if (m2DTexId > 0) {
                mProgramTexture2d.drawFrame(m2DTexId, mRenderRotatedImage ? GlUtil.IDENTITY_MATRIX : mTexMatrix, mMvpMatrix);
            } else if (mCameraTexId > 0) {
                mProgramTextureOES.drawFrame(mCameraTexId, mTexMatrix, mMvpMatrix);
            }
            if (mDrawSmallViewport) {
                GLES20.glViewport(mSmallViewportX, mSmallViewportY, mSmallViewportWidth, mSmallViewportHeight);
                mProgramTextureOES.drawFrame(mCameraTexId, mTexMatrix, GlUtil.IDENTITY_MATRIX);
                GLES20.glViewport(0, 0, mViewWidth, mViewHeight);
            }
            mGlSurfaceView.requestRender();
        }
        LimitFpsUtil.limitFrameRate();
    }

    @Override
    protected void onResume() {
        startBackgroundThread();
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                openCamera(mCameraFacing);
                startPreview();
            }
        });
        mGlSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        mGlSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                destroyGlSurface();
                countDownLatch.countDown();
            }
        });
        try {
            countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // ignored
        }
        mGlSurfaceView.onPause();
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                closeCamera();
            }
        });
        stopBackgroundThread();
    }

    public void setRenderRotatedImage(boolean renderRotatedImage) {
        mRenderRotatedImage = renderRotatedImage;
        mDrawSmallViewport = renderRotatedImage;
    }

    public void onTouchEvent(int x, int y, int action) {
        if (!mDrawSmallViewport) {
            return;
        }
        if (action == MotionEvent.ACTION_MOVE) {
            if (x < mSmallViewportHorizontalPadding || x > mViewWidth - mSmallViewportHorizontalPadding
                    || y < mSmallViewportTopPadding || y > mViewHeight - mSmallViewportBottomPadding) {
                return;
            }
            int touchX = mTouchX;
            int touchY = mTouchY;
            mTouchX = x;
            mTouchY = y;
            int distanceX = x - touchX;
            int distanceY = y - touchY;
            int viewportX = mSmallViewportX;
            int viewportY = mSmallViewportY;
            viewportX += distanceX;
            viewportY -= distanceY;
            if (viewportX < mSmallViewportHorizontalPadding || viewportX + mSmallViewportWidth > mViewWidth - mSmallViewportHorizontalPadding
                    || mViewHeight - viewportY - mSmallViewportHeight < mSmallViewportTopPadding
                    || viewportY < mSmallViewportBottomPadding) {
                return;
            }
            mSmallViewportX = viewportX;
            mSmallViewportY = viewportY;
        } else if (action == MotionEvent.ACTION_DOWN) {
            mTouchX = x;
            mTouchY = y;
        } else if (action == MotionEvent.ACTION_UP) {
            boolean alignLeft = mSmallViewportX < mViewWidth / 2;
            mSmallViewportX = alignLeft ? mSmallViewportHorizontalPadding : mViewWidth - mSmallViewportHorizontalPadding - mSmallViewportWidth;
            mTouchX = 0;
            mTouchY = 0;
        }
    }

    public void changeResolution(int cameraWidth, int cameraHeight) {
    }

    public void switchCamera() {
        if (mBackgroundHandler == null) {
            return;
        }
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                mIsStopPreview = true;
                boolean isFront = mCameraFacing == FACE_FRONT;
                mCameraFacing = isFront ? FACE_BACK : FACE_FRONT;
                mCameraOrientation = isFront ? mBackCameraOrientation : mFrontCameraOrientation;
                closeCamera();
                openCamera(mCameraFacing);
                mOnRendererStatusListener.onCameraChanged(mCameraFacing, mCameraOrientation);
                startPreview();
                mIsStopPreview = false;
            }
        });
    }

    public int getCameraWidth() {
        return mCameraWidth;
    }

    public int getCameraHeight() {
        return mCameraHeight;
    }

    public int getViewWidth() {
        return mViewWidth;
    }

    public int getViewHeight() {
        return mViewHeight;
    }

    protected void initCameraInfo() {
    }

    protected void openCamera(int cameraFacing) {
    }

    protected void startPreview() {
    }

    protected void closeCamera() {
        mCameraNv21Byte = null;
        mNv21ByteCopy = null;
    }

    private void destroyGlSurface() {
        if (mCameraTexId > 0) {
            GLES20.glDeleteTextures(1, new int[]{mCameraTexId}, 0);
            mCameraTexId = 0;
        }
        if (mProgramTexture2d != null) {
            mProgramTexture2d.release();
            mProgramTexture2d = null;
        }
        if (mProgramTextureOES != null) {
            mProgramTextureOES.release();
            mProgramTextureOES = null;
        }
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }

        mOnRendererStatusListener.onSurfaceDestroy();
    }

    private void startBackgroundThread() {
        HandlerThread backgroundThread = new HandlerThread(TAG, Process.THREAD_PRIORITY_BACKGROUND);
        backgroundThread.start();
        mBackgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (mBackgroundHandler != null) {
            mBackgroundHandler.getLooper().quitSafely();
            mBackgroundHandler = null;
        }
    }

}
