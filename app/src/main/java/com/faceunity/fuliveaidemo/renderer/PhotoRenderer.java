package com.faceunity.fuliveaidemo.renderer;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import androidx.lifecycle.Lifecycle;

import com.faceunity.fuliveaidemo.gles.ProgramTexture2d;
import com.faceunity.fuliveaidemo.gles.core.GlUtil;
import com.faceunity.fuliveaidemo.util.BitmapUtil;
import com.faceunity.fuliveaidemo.util.LimitFpsUtil;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 渲染图像，采用居中裁剪（CenterCrop）的方式显示
 *
 * @author Richie on 2019.08.19
 */
public class PhotoRenderer extends AbstractLifeCycleRenderer implements GLSurfaceView.Renderer {
    public final static String TAG = PhotoRenderer.class.getSimpleName();
    public static final float[] IMAGE_TEXTURE_MATRIX = {0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f};
    public static final float[] MATRIX_ROTATE_90 = {0.0F, 1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F};

    private GLSurfaceView mGlSurfaceView;
    private OnPhotoRendererListener mOnPhotoRendererListener;
    private String mPhotoPath;
    private byte[] mPhotoRgbaByte;
    private int mPhotoTextureId;
    private int mPhotoWidth = 720;
    private int mPhotoHeight = 1280;
    private float[] mMvpMatrix;
    private ProgramTexture2d mProgramTexture2d;

    public PhotoRenderer(Lifecycle lifecycle, String photoPath, GLSurfaceView glSurfaceView,
                         OnPhotoRendererListener onPhotoRendererListener) {
        super(lifecycle);
        mPhotoPath = photoPath;
        mGlSurfaceView = glSurfaceView;
        glSurfaceView.setEGLContextClientVersion(GlUtil.getSupportGLVersion(glSurfaceView.getContext()));
        glSurfaceView.setRenderer(this);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        mOnPhotoRendererListener = onPhotoRendererListener;
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume: ");
        mGlSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause: ");
        final CountDownLatch count = new CountDownLatch(1);
        mGlSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                onSurfaceDestroy();
                count.countDown();
            }
        });
        try {
            count.await(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // ignored
        }
        mGlSurfaceView.onPause();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(TAG, "onSurfaceCreated. Thread:" + Thread.currentThread().getName());
        mProgramTexture2d = new ProgramTexture2d();
        loadPhoto(mPhotoPath);
        LimitFpsUtil.setTargetFps(LimitFpsUtil.DEFAULT_FPS);
        mOnPhotoRendererListener.onSurfaceCreated();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(TAG, "onSurfaceChanged: viewWidth:" + width + ", viewHeight:" + height + ", photoWidth:"
                + mPhotoWidth + ", photoHeight:" + mPhotoHeight + ", textureId:" + mPhotoTextureId);
        GLES20.glViewport(0, 0, width, height);
        mMvpMatrix = GlUtil.changeMVPMatrixInside(width, height, mPhotoWidth, mPhotoHeight);
        Matrix.rotateM(mMvpMatrix, 0, 90, 0, 0, 1);

        mOnPhotoRendererListener.onSurfaceChanged(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (mProgramTexture2d == null || mPhotoRgbaByte == null) {
            return;
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BITS);
        int fuTextureId = mOnPhotoRendererListener.onDrawFrame(mPhotoRgbaByte, mPhotoTextureId, mPhotoWidth, mPhotoHeight);
        mProgramTexture2d.drawFrame(fuTextureId, IMAGE_TEXTURE_MATRIX, mMvpMatrix);

        LimitFpsUtil.limitFrameRate();
        mGlSurfaceView.requestRender();
    }

    private void loadPhoto(String path) {
        Log.d(TAG, "loadPhoto() path:" + path);
        Bitmap bitmap = BitmapUtil.loadBitmap(path, mPhotoWidth, mPhotoHeight);
        if (bitmap == null) {
            mOnPhotoRendererListener.onLoadPhotoError("图片加载失败:" + path);
            return;
        }

        mPhotoTextureId = GlUtil.createImageTexture(bitmap);
        mPhotoWidth = bitmap.getWidth();
        mPhotoHeight = bitmap.getHeight();
        mPhotoRgbaByte = BitmapUtil.copyRgbaByteFromBitmap(bitmap);
    }

    private void onSurfaceDestroy() {
        Log.d(TAG, "onSurfaceDestroy");
        if (mPhotoTextureId != 0) {
            int[] textures = new int[]{mPhotoTextureId};
            GLES20.glDeleteTextures(1, textures, 0);
            mPhotoTextureId = 0;
        }
        if (mProgramTexture2d != null) {
            mProgramTexture2d.release();
            mProgramTexture2d = null;
        }

        mOnPhotoRendererListener.onSurfaceDestroy();
    }

}
