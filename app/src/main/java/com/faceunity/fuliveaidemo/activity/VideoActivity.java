package com.faceunity.fuliveaidemo.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.faceunity.fuliveaidemo.R;
import com.faceunity.fuliveaidemo.gles.core.GlUtil;
import com.faceunity.fuliveaidemo.renderer.VideoRenderer;
import com.faceunity.fuliveaidemo.util.VideoRecorder;
import com.faceunity.nama.FURenderer;

import java.io.File;

public class VideoActivity extends BaseGlActivity implements VideoRenderer.OnRendererStatusListener, VideoRecorder.OnVideoRecordListener {
    public static String VIDEO_PATH = "video_path";

    private VideoRenderer mVideoRenderer;
    private VideoRecorder mVideoRecorder;

    public static void start(Activity context, String videoPath) {
        Intent intent = new Intent(context, VideoActivity.class);
        intent.putExtra(VIDEO_PATH, videoPath);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mVideoRecorder = new VideoRecorder(mGlSurfaceView);
        mVideoRecorder.setOnVideoRecordListener(this);
        mVideoRenderer.setOnMediaEventListener(new VideoRenderer.OnMediaEventListener() {
            @Override
            public void onCompletion() {
                mVideoRecorder.stop();
            }

            @Override
            public void onLoadError(String error) {}
        });
    }

    @Override
    protected void initView() {
        findViewById(R.id.iv_switch_cam).setVisibility(View.GONE);
        findViewById(R.id.btn_record_video).setVisibility(View.GONE);
        findViewById(R.id.iv_debug).setVisibility(View.GONE);
        findViewById(R.id.iv_save_photo).setVisibility(View.GONE);
        mGlSurfaceView.setOnClickListener(v -> {
            if (null != mVideoRenderer) {
                mVideoRenderer.startMediaPlayer();
                mVideoRecorder.start(mVideoRenderer.getVideoWidth(), mVideoRenderer.getVideoHeight());
            }
        });
    }

    @Override
    protected void initFuRenderer() {
        mFURenderer = new FURenderer.Builder(this)
                .setInputTextureType(FURenderer.INPUT_TEXTURE_EXTERNAL_OES)
                .setOnSystemErrorListener(this)
                .build();
    }

    @Override
    protected void initGlRenderer() {
        String videoPath = getIntent().getStringExtra(VIDEO_PATH);
        mVideoRenderer = new VideoRenderer(getLifecycle(), videoPath, mGlSurfaceView, this);
    }

    @Override
    protected boolean containHumanAvatar() {
        return false;
    }

    @Override
    public void onSurfaceChanged(int viewWidth, int viewHeight, int videoWidth, int videoHeight, int videoRotation, boolean isSystemCameraRecord) {

    }

    @Override
    public int onDrawFrame(int videoTextureId, int videoWidth, int videoHeight, float[] mvpMatrix, long timeStamp) {
        int fuTexId = mFURenderer.onDrawFrameSingleInput(videoTextureId, videoWidth, videoHeight);
        trackFace();
        trackHuman();
        queryTrackStatus();
        mPhotoTaker.send(fuTexId, GlUtil.IDENTITY_MATRIX, VideoRenderer.mTexMatrix, videoWidth, videoHeight);
        mVideoRecorder.send(fuTexId, GlUtil.IDENTITY_MATRIX, VideoRenderer.mTexMatrix, timeStamp);
        return fuTexId;
    }

    @Override
    public void onPrepare() {

    }

    @Override
    public void onStop(boolean valid) {

    }

    @Override
    public void onProgress(long progress) {

    }

    @Override
    public void onFinish(String path) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(path))));
                Toast.makeText(VideoActivity.this, path, Toast.LENGTH_LONG).show();
            }
        });
    }
}
