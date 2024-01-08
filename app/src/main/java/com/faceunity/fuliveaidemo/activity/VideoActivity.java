package com.faceunity.fuliveaidemo.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.faceunity.FUConfig;
import com.faceunity.core.entity.FURenderFrameData;
import com.faceunity.core.entity.FURenderInputData;
import com.faceunity.core.entity.FURenderOutputData;
import com.faceunity.core.faceunity.FUAIKit;
import com.faceunity.core.faceunity.FURenderKit;
import com.faceunity.core.listener.OnGlRendererListener;
import com.faceunity.core.listener.OnVideoPlayListener;
import com.faceunity.core.media.video.OnVideoRecordingListener;
import com.faceunity.core.media.video.VideoRecordHelper;
import com.faceunity.core.renderer.VideoRenderer;
import com.faceunity.core.utils.GlUtil;
import com.faceunity.fuliveaidemo.R;
import com.faceunity.fuliveaidemo.util.FileUtils;
import com.faceunity.fuliveaidemo.util.ToastUtil;
import com.faceunity.fuliveaidemo.view.RecordButton;
import com.faceunity.nama.FURenderer;
import com.faceunity.nama.utils.FuDeviceUtils;
import com.faceunity.nama.view.listener.TypeEnum;

import java.io.File;

public class VideoActivity extends BaseGlActivity implements OnGlRendererListener, RecordButton.OnRecordListener {
    public static String VIDEO_PATH = "video_path";
    private VideoRenderer mVideoRenderer;
    private int mVideoWidth;
    private int mVideoHeight;
    private String mVideoPath;
    private ImageView mSaveView;
    private ImageView mPlayView;

    public static void start(Activity context, String videoPath) {
        Intent intent = new Intent(context, VideoActivity.class);
        intent.putExtra(VIDEO_PATH, videoPath);
        context.startActivity(intent);
    }

    //region Activity生命周期绑定
    private Boolean isActivityPause = false;

    @Override
    protected void onResume() {
        super.onResume();
        mVideoRenderer.onResume();
        if (isActivityPause) {
            mPlayView.setVisibility(View.VISIBLE);
        }
        isActivityPause = false;
    }

    @Override
    protected void onPause() {
        isActivityPause = true;
        super.onPause();
        mVideoRenderer.onPause();
        if (isRecording) {
            isRecording = false;
            onStopRecord();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mVideoRenderer.onDestroy();
    }

    @Override
    protected void initView() {
        findViewById(R.id.iv_switch_cam).setVisibility(View.GONE);
        findViewById(R.id.iv_debug).setVisibility(View.GONE);
        findViewById(R.id.btn_record_video).setVisibility(View.GONE);
        mSaveView = findViewById(R.id.iv_save_photo);
        mSaveView.setVisibility(View.GONE);
        mPlayView = findViewById(R.id.iv_play_video);
        mPlayView.setVisibility(View.GONE);

        mRecordBtn.setOnRecordListener(this);

        /* 播放*/
        mPlayView.setOnClickListener((view) -> {
            if (mSaveView.isSelected()) {
                Toast.makeText(VideoActivity.this, R.string.save_video_wait, Toast.LENGTH_LONG).show();
                return;
            }
            mSaveView.setVisibility(View.GONE);
            mPlayView.setVisibility(View.GONE);
            mVideoRecordHelper.startRecording(mGlSurfaceView, mVideoWidth, mVideoHeight, mVideoPath);
        });

        /* 保存*/
        mSaveView.setOnClickListener((view) -> {
            if (mSaveView.isSelected()) {
                Toast.makeText(VideoActivity.this, R.string.save_video_wait, Toast.LENGTH_LONG).show();
                return;
            }
            mSaveView.setVisibility(View.GONE);
            new Thread(() -> saveVideoFile()).start();
        });
    }

    @Override
    public void setNormalOrAvatarMode(TypeEnum typeEnum) {
        super.setNormalOrAvatarMode(typeEnum);
        mRecordBtn.setVisibility(View.GONE);
    }

    @Override
    protected View getRecordView() {
        return mRecordBtn;
    }

    @Override
    protected void initFuRenderer() {
        mFURenderer = FURenderer.getInstance();
    }

    @Override
    protected void initGlRenderer() {
        mVideoPath = getIntent().getStringExtra(VIDEO_PATH);
        mVideoRenderer = new VideoRenderer(mGlSurfaceView, mVideoPath, this);
        mVideoRecordHelper = new VideoRecordHelper(this, mOnVideoRecordingListener);
    }

    @Override
    protected boolean containHumanAvatar() {
        return false;
    }

    @Override
    public void onDrawFrameAfter() {

    }

    int currentFrame = 0;

    @Override
    public void onRenderAfter(FURenderOutputData fuRenderOutputData, FURenderFrameData fuRenderFrameData) {
        mVideoWidth = fuRenderOutputData.getTexture().getWidth();
        mVideoHeight = fuRenderOutputData.getTexture().getHeight();

        trackFace();
        trackHuman();
        queryTrackStatus();

        recordingPhoto(fuRenderOutputData, fuRenderFrameData.getTexMatrix());
        recordingVideo(fuRenderOutputData, fuRenderFrameData.getTexMatrix());
        if (currentFrame++ == 5) {
            mVideoRecordHelper.startRecording(mGlSurfaceView, mVideoWidth, mVideoHeight, mVideoPath);
        }
    }

    @Override
    public void onRenderBefore(FURenderInputData fuRenderInputData) {

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
        mVideoRecordHelper.startRecording(mGlSurfaceView, mVideoWidth, mVideoHeight);
    }

    protected void onStopRecord() {
        mRecordBtn.setSecond(0);
        mVideoRecordHelper.stopRecording();
    }

    private File mRecordFile;
    private OnVideoRecordingListener mOnVideoRecordingListener = new OnVideoRecordingListener() {

        @Override
        public void onPrepared() {
            isRecordingPrepared = true;
            mVideoRenderer.startMediaPlayer(mOnVideoPlayListener);
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
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
            isRecordingPrepared = false;
            mRecordFile = file;
            runOnUiThread(() -> mSaveView.setSelected(false));
        }
    };

    /**
     * 保存视频文件
     */
    private void saveVideoFile() {
        if (mRecordFile == null) {
            return;
        }

        if (recordTime < 1100) {
            runOnUiThread(() -> ToastUtil.makeText(VideoActivity.this, R.string.save_video_too_short).show());
        } else {
            String filePath = FileUtils.addVideoToAlbum(VideoActivity.this, mRecordFile);
            if (filePath == null || filePath.trim().length() == 0) {
                runOnUiThread(() -> ToastUtil.makeText(VideoActivity.this, R.string.save_video_failure).show());
            } else {
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(mRecordFile)));
                runOnUiThread(() -> ToastUtil.makeText(VideoActivity.this, R.string.save_video_success).show());
            }
        }
        if (mRecordFile.exists()) {
            mRecordFile.delete();
            mRecordFile = null;
        }
    }

    /**
     * 视频回调
     */
    private OnVideoPlayListener mOnVideoPlayListener = new OnVideoPlayListener() {
        @Override
        public void onError(String error) {

        }

        @Override
        public void onPlayFinish() {
            onStopRecord();
            runOnUiThread(() -> {
                mPlayView.setVisibility(View.VISIBLE);
                mSaveView.setVisibility(View.VISIBLE);
                mSaveView.setSelected(true);
            });
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

    //endregion 视频录制
}
