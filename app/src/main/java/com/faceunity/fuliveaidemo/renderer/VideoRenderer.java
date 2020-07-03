package com.faceunity.fuliveaidemo.renderer;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import androidx.lifecycle.Lifecycle;

import com.faceunity.fuliveaidemo.gles.ProgramTexture2d;
import com.faceunity.fuliveaidemo.gles.ProgramTextureOES;
import com.faceunity.fuliveaidemo.gles.core.GlUtil;
import com.faceunity.fuliveaidemo.util.LimitFpsUtil;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 渲染视频
 *
 * @author Richie on 2020.06.19
 */
public class VideoRenderer extends AbstractLifeCycleRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "VideoRenderer";
    private GLSurfaceView mGlSurfaceView;
    private OnVideoRendererListener mOnVideoRendererListener;
    private OnMediaEventListener mOnMediaEventListener;
    private String mVideoPath;
    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;
    private int mVideoTextureId;
    private int mVideoWidth;
    private int mVideoHeight;
    private int mVideoRotation = 0;
    private float[] mTexMatrix = new float[16];
    private float[] mMvpMatrix;
    private ProgramTexture2d mProgramTexture2d;
    private ProgramTextureOES mProgramTextureOes;
    private SimpleExoPlayer mSimpleExoPlayer;
    private Context mContext;
    private Handler mPlayerHandler;

    public VideoRenderer(Lifecycle lifecycle, String videoPath, GLSurfaceView glSurfaceView, OnVideoRendererListener onVideoRendererListener) {
        super(lifecycle);
        mVideoPath = videoPath;
        mGlSurfaceView = glSurfaceView;
        mContext = glSurfaceView.getContext();
        mOnVideoRendererListener = onVideoRendererListener;
    }

    @Override
    protected void onResume() {
        super.onResume();
        startPlayerThread();
        mPlayerHandler.post(new Runnable() {
            @Override
            public void run() {
                createExoMediaPlayer();
            }
        });
        mGlSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPlayerHandler.post(new Runnable() {
            @Override
            public void run() {
                releaseExoMediaPlayer();
                stopPlayerThread();
            }
        });
        final CountDownLatch count = new CountDownLatch(1);
        mGlSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                onSurfaceDestroy();
                releaseSurface();
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
        mProgramTexture2d = new ProgramTexture2d();
        mProgramTextureOes = new ProgramTextureOES();
        mVideoTextureId = GlUtil.createTextureObject(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        LimitFpsUtil.setTargetFps(LimitFpsUtil.DEFAULT_FPS);
        createSurface();
        mPlayerHandler.post(new Runnable() {
            @Override
            public void run() {
                mSimpleExoPlayer.setVideoSurface(mSurface);
            }
        });
        mOnVideoRendererListener.onSurfaceCreated();
        retrieveMediaMetaData();
        Log.d(TAG, "onSurfaceCreated: videoWidth:" + mVideoWidth + ", videoHeight:" + mVideoHeight + ", videoRotation:" + mVideoRotation);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        mOnVideoRendererListener.onSurfaceChanged(width, height, mVideoWidth, mVideoHeight, mVideoRotation);
        boolean isLandscape = mVideoRotation % 180 == 0;
        mMvpMatrix = GlUtil.changeMVPMatrixInside(width, height, isLandscape ? mVideoWidth : mVideoHeight,
                isLandscape ? mVideoHeight : mVideoWidth);
        Log.d(TAG, "onSurfaceChanged() width:" + width + ", height:" + height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (mProgramTexture2d == null || mSurfaceTexture == null) {
            return;
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        try {
            mSurfaceTexture.updateTexImage();
            mSurfaceTexture.getTransformMatrix(mTexMatrix);
        } catch (Exception e) {
            Log.e(TAG, "onDrawFrame: ", e);
            return;
        }

        int fuTextureId = mOnVideoRendererListener.onDrawFrame(mVideoTextureId, mVideoWidth,
                mVideoHeight, mTexMatrix, mSurfaceTexture.getTimestamp());
        if (fuTextureId > 0) {
            mProgramTexture2d.drawFrame(fuTextureId, mTexMatrix, mMvpMatrix);
        } else if (mVideoTextureId > 0) {
            mProgramTextureOes.drawFrame(mVideoTextureId, mTexMatrix, mMvpMatrix);
        }

        LimitFpsUtil.limitFrameRate();
        mGlSurfaceView.requestRender();
    }

    public void setOnMediaEventListener(OnMediaEventListener onMediaEventListener) {
        mOnMediaEventListener = onMediaEventListener;
    }

    public int getVideoWidth() {
        return mVideoRotation % 180 == 0 ? mVideoWidth : mVideoHeight;
    }

    public int getVideoHeight() {
        return mVideoRotation % 180 == 0 ? mVideoHeight : mVideoWidth;
    }

    public void startMediaPlayer() {
        Log.d(TAG, "startMediaPlayer: ");
        mPlayerHandler.post(new Runnable() {
            @Override
            public void run() {
                mSimpleExoPlayer.seekTo(0);
                mSimpleExoPlayer.setPlayWhenReady(true);
            }
        });
    }

    private void createExoMediaPlayer() {
        Log.d(TAG, "createExoMediaPlayer: ");
        mSimpleExoPlayer = new SimpleExoPlayer.Builder(mContext).build();
        MediaEventListener mediaEventListener = new MediaEventListener();
        mSimpleExoPlayer.addListener(mediaEventListener);
        mSimpleExoPlayer.setPlayWhenReady(false);
        String userAgent = Util.getUserAgent(mContext, mContext.getPackageName());
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(mContext, userAgent);
        ProgressiveMediaSource.Factory mediaSourceFactory = new ProgressiveMediaSource.Factory(dataSourceFactory);
        Uri uri = Uri.fromFile(new File(mVideoPath));
        MediaSource mediaSource = mediaSourceFactory.createMediaSource(uri);
        mSimpleExoPlayer.prepare(mediaSource);
    }

    private void releaseExoMediaPlayer() {
        Log.d(TAG, "releaseExoMediaPlayer: ");
        if (mSimpleExoPlayer != null) {
            mSimpleExoPlayer.stop(true);
            mSimpleExoPlayer.release();
            mSimpleExoPlayer = null;
        }
    }

    private void retrieveMediaMetaData() {
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        try {
            mediaMetadataRetriever.setDataSource(mVideoPath);
            mVideoWidth = Integer.parseInt(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            mVideoHeight = Integer.parseInt(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
            mVideoRotation = Integer.parseInt(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));
        } catch (Exception e) {
            Log.e(TAG, "MediaMetadataRetriever extractMetadata: ", e);
        } finally {
            mediaMetadataRetriever.release();
        }
    }

    private void createSurface() {
        Log.d(TAG, "createSurface: ");
        mSurfaceTexture = new SurfaceTexture(mVideoTextureId);
        mSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                mGlSurfaceView.requestRender();
            }
        });
        mSurface = new Surface(mSurfaceTexture);
    }

    private void releaseSurface() {
        Log.d(TAG, "releaseSurface: ");
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
    }

    private void onSurfaceDestroy() {
        Log.d(TAG, "onSurfaceDestroy");
        if (mVideoTextureId > 0) {
            GLES20.glDeleteTextures(1, new int[]{mVideoTextureId}, 0);
            mVideoTextureId = 0;
        }
        if (mProgramTexture2d != null) {
            mProgramTexture2d.release();
            mProgramTexture2d = null;
        }
        if (mProgramTextureOes != null) {
            mProgramTextureOes.release();
            mProgramTextureOes = null;
        }

        mOnVideoRendererListener.onSurfaceDestroy();
    }

    private void startPlayerThread() {
        HandlerThread playerThread = new HandlerThread("exo_player");
        playerThread.start();
        mPlayerHandler = new Handler(playerThread.getLooper());
    }

    private void stopPlayerThread() {
        mPlayerHandler.getLooper().quitSafely();
        mPlayerHandler = null;
    }

    private class MediaEventListener implements Player.EventListener {

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            switch (playbackState) {
//                case Player.STATE_IDLE:
//                    break;
//                case Player.STATE_BUFFERING:
//                    break;
                case Player.STATE_READY:
                    if (playWhenReady) {
                        Log.d(TAG, "onPlayerStateChanged: prepared " + Thread.currentThread().getName());
                        mGlSurfaceView.requestRender();
                    }
                    break;
                case Player.STATE_ENDED:
                    Log.d(TAG, "onPlayerStateChanged: completion " + Thread.currentThread().getName());
                    if (mOnMediaEventListener != null) {
                        mOnMediaEventListener.onCompletion();
                    }
                    break;
                default:
            }
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            Log.w(TAG, "onPlayerError: ", error);
            String message;
            switch (error.type) {
                case ExoPlaybackException.TYPE_SOURCE:
                    message = "数据源异常";
                    break;
                case ExoPlaybackException.TYPE_RENDERER:
                    message = "解码异常";
                    break;
                case ExoPlaybackException.TYPE_UNEXPECTED:
                default:
                    message = "其他异常";
            }
            if (mOnMediaEventListener != null) {
                mOnMediaEventListener.onError(message);
            }
        }
    }


    public interface OnMediaEventListener {
        /**
         * Called when the end of a media source is reached during playback.
         */
        void onCompletion();

        /**
         * Called when error happened
         *
         * @param message
         */
        void onError(String message);
    }

}
