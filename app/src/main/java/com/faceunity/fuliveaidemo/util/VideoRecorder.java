package com.faceunity.fuliveaidemo.util;

import android.opengl.EGL14;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.util.Log;

import com.faceunity.fuliveaidemo.encoder.MediaAudioEncoder;
import com.faceunity.fuliveaidemo.encoder.MediaEncoder;
import com.faceunity.fuliveaidemo.encoder.MediaMuxerWrapper;
import com.faceunity.fuliveaidemo.encoder.MediaVideoEncoder;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * 录像
 *
 * @author Richie on 2020.05.22
 */
public final class VideoRecorder {
    private static final int MIN_VIDEO_DURATION_MS = 1000;
    private static final String TAG = "VideoRecorder";
    private File mTempFile;
    private MediaMuxerWrapper mMuxerWrapper;
    private volatile MediaVideoEncoder mVideoEncoder;
    private boolean mIsStopped;
    private long mStartTime;
    private CountDownLatch mCountDownLatch;
    private GLSurfaceView mGlSurfaceView;
    private OnVideoRecordListener mOnVideoRecordListener;

    public VideoRecorder(final GLSurfaceView glSurfaceView) {
        mGlSurfaceView = glSurfaceView;
    }

    public void setOnVideoRecordListener(OnVideoRecordListener onVideoRecordListener) {
        mOnVideoRecordListener = onVideoRecordListener;
    }

    /**
     * start record video, this may cost too much time, so execute in async task
     *
     * @param width
     * @param height
     */
    public void start(final int width, final int height) {
        Log.d(TAG, "start width:" + width + ", height:" + height);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                long start = System.currentTimeMillis();
                mIsStopped = false;
                mStartTime = 0;
                mCountDownLatch = new CountDownLatch(2);
                String fileName = Constant.APP_NAME + "_" + DateUtil.getCurrentDate() + ".mp4";
                mTempFile = new File(FileUtils.getExternalCacheDir(mGlSurfaceView.getContext()), fileName);
                int videoWidth = width / 2 * 2;
                int videoHeight = height / 2 * 2; // 取偶数
                try {
                    mMuxerWrapper = new MediaMuxerWrapper(mTempFile.getAbsolutePath());
                    new MediaVideoEncoder(mMuxerWrapper, mMediaEncoderListener, videoWidth, videoHeight);
                    new MediaAudioEncoder(mMuxerWrapper, mMediaEncoderListener);
                    mMuxerWrapper.prepare();
                    mMuxerWrapper.startRecording();
                } catch (IOException e) {
                    Log.e(TAG, "start: ", e);
                }
                long time = System.currentTimeMillis() - start;
                Log.i(TAG, "start time: " + time + "ms");
            }
        });
    }

    /**
     * stop record video, this may cost too much time, so execute in async task
     */
    public void stop() {
        Log.d(TAG, "stop");
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                mIsStopped = true;
                mStartTime = 0;
                if (mMuxerWrapper != null) {
                    mVideoEncoder = null;
                    mMuxerWrapper.stopRecording();
                    mMuxerWrapper = null;
                }
            }
        });
    }

    /**
     * send video frames
     *
     * @param texId
     * @param mvpMatrix
     * @param texMatrix
     * @param timeStamp
     */
    public void send(final int texId, final float[] mvpMatrix, final float[] texMatrix, final long timeStamp) {
        if (mVideoEncoder == null) {
            return;
        }
        long timeStampMs = timeStamp / 1_000_000;
        if (mStartTime == 0) {
            mStartTime = timeStampMs;
        }
        mVideoEncoder.frameAvailableSoon(texId, texMatrix, mvpMatrix);
        if (mOnVideoRecordListener != null) {
            mOnVideoRecordListener.onProgress(timeStampMs - mStartTime);
        }
    }

    private final MediaEncoder.MediaEncoderListener mMediaEncoderListener = new MediaEncoder.MediaEncoderListener() {
        private long mPreparedTime;

        @Override
        public void onPrepared(final MediaEncoder encoder) {
            if (encoder instanceof MediaVideoEncoder) {
                Log.i(TAG, "onPrepared:");
                mGlSurfaceView.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        if (mIsStopped) {
                            return;
                        }
                        MediaVideoEncoder videoEncoder = (MediaVideoEncoder) encoder;
                        videoEncoder.setEglContext(EGL14.eglGetCurrentContext());
                        mVideoEncoder = videoEncoder;
                    }
                });
                if (!mIsStopped) {
                    if (mOnVideoRecordListener != null) {
                        mOnVideoRecordListener.onPrepare();
                    }
                }
            }
            // when come to the following, means that all encoder is prepared
            mPreparedTime = System.currentTimeMillis();
        }

        @Override
        public void onStopped(final MediaEncoder encoder) {
            mCountDownLatch.countDown();
            // called when MediaVideoEncoder's callback and MediaAudioEncoder's callback are both stoped
            if (mCountDownLatch.getCount() > 0) {
                return;
            }
            // check video duration validation
            boolean valid = System.currentTimeMillis() - mPreparedTime > MIN_VIDEO_DURATION_MS;
            Log.d(TAG, "onStopped: " + valid);
            mPreparedTime = 0;
            mCountDownLatch = null;
            if (mOnVideoRecordListener != null) {
                mOnVideoRecordListener.onStop(valid);
            }
            if (!valid) {
                return;
            }
            // onStopped is called on codec thread, it may be interrupted, so we execute following code async.
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    File videoFile = new File(Constant.VIDEO_FILE_PATH, mTempFile.getName());
                    boolean result = FileUtils.copyFile(mTempFile, videoFile);
                    if (result) {
                        if (mOnVideoRecordListener != null) {
                            mOnVideoRecordListener.onFinish(videoFile.getAbsolutePath());
                        }
                    }
                }
            });
        }
    };

    public interface OnVideoRecordListener {
        /**
         * 开始
         */
        void onPrepare();

        /**
         * 停止
         *
         * @param valid
         */
        void onStop(boolean valid);

        /**
         * 进度
         *
         * @param progress
         */
        void onProgress(long progress);

        /**
         * 完成
         *
         * @param path
         */
        void onFinish(String path);
    }

}
