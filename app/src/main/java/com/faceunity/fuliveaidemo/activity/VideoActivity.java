package com.faceunity.fuliveaidemo.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.faceunity.core.entity.FURenderFrameData;
import com.faceunity.core.entity.FURenderInputData;
import com.faceunity.core.entity.FURenderOutputData;
import com.faceunity.core.faceunity.FURenderKit;
import com.faceunity.core.listener.OnGlRendererListener;
import com.faceunity.core.media.video.OnVideoRecordingListener;
import com.faceunity.core.media.video.VideoRecordHelper;
import com.faceunity.core.renderer.VideoRenderer;
import com.faceunity.fuliveaidemo.R;
import com.faceunity.nama.FURenderer;
 

import java.io.File;

public class VideoActivity extends BaseGlActivity implements OnGlRendererListener,OnVideoRecordingListener {
    public static String VIDEO_PATH = "video_path";
    private VideoRecordHelper videoRecordHelper;
    private VideoRenderer mVideoRenderer;

    public static void start(Activity context, String videoPath) {
        Intent intent = new Intent(context, VideoActivity.class);
        intent.putExtra(VIDEO_PATH, videoPath);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        videoRecordHelper = new VideoRecordHelper(this, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mVideoRenderer.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mVideoRenderer.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mVideoRenderer.onDestroy();
    }

    @Override
    protected void initView() {
        findViewById(R.id.iv_switch_cam).setVisibility(View.GONE);
        findViewById(R.id.btn_record_video).setVisibility(View.GONE);
        findViewById(R.id.iv_debug).setVisibility(View.GONE);
        findViewById(R.id.iv_save_photo).setVisibility(View.GONE);
        mGlSurfaceView.setOnClickListener(v -> {
            if (null != videoRecordHelper) {
                videoRecordHelper.startRecording(mGlSurfaceView,480,640);
            }
        });
    }

    @Override
    protected void initFuRenderer() {
        mFURenderer = FURenderer.getInstance();
    }

    @Override
    protected void initGlRenderer() {
        String videoPath = getIntent().getStringExtra(VIDEO_PATH);
        mVideoRenderer = new VideoRenderer(mGlSurfaceView, videoPath,this);
    }

    @Override
    protected boolean containHumanAvatar() {
        return false;
    }

    @Override
    public void onDrawFrameAfter() {

    }

    @Override
    public void onRenderAfter( FURenderOutputData fuRenderOutputData,  FURenderFrameData fuRenderFrameData) {
        trackFace();
        trackHuman();
        queryTrackStatus();
        recordingPhoto(fuRenderOutputData,fuRenderFrameData.getTexMatrix());
    }

    @Override
    public void onRenderBefore( FURenderInputData fuRenderInputData) {

    }

    @Override
    public void onPrepared() {

    }

    @Override
    public void onProcess(Long time) {

    }

    @Override
    public void onFinish(File file) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
                Toast.makeText(VideoActivity.this, file.getPath(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onSurfaceChanged(int i, int i1) {

    }

    @Override
    public void onSurfaceCreated() {

    }

    @Override
    public void onSurfaceDestroy() {
        FURenderKit.getInstance().release();
    }
}
