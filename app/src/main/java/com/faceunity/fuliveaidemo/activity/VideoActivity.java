package com.faceunity.fuliveaidemo.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;

import com.faceunity.fuliveaidemo.R;
import com.faceunity.fuliveaidemo.gles.core.GlUtil;
import com.faceunity.fuliveaidemo.renderer.OnVideoRendererListener;
import com.faceunity.fuliveaidemo.renderer.VideoRenderer;
import com.faceunity.fuliveaidemo.util.ToastUtil;
import com.faceunity.fuliveaidemo.util.VideoRecorder;
import com.faceunity.nama.FURenderer;
import com.faceunity.nama.utils.LogUtils;

/**
 * @author Richie on 2020.06.19
 */
public class VideoActivity extends BaseGlActivity implements OnVideoRendererListener, VideoRecorder.OnVideoRecordListener, VideoRenderer.OnMediaEventListener {
    private static final String TAG = "VideoActivity";
    public static final String VIDEO_PATH = "video_path";
    private VideoRenderer mVideoRenderer;
    private VideoRecorder mVideoRecorder;
    private ImageView mIvPlay;
    private View mIvSave;

    public static void actionStart(Activity context, String videoPath) {
        Intent intent = new Intent(context, VideoActivity.class);
        intent.putExtra(VIDEO_PATH, videoPath);
        context.startActivity(intent);
    }

    @Override
    protected void initView() {
        findViewById(R.id.iv_switch_cam).setVisibility(View.GONE);
        findViewById(R.id.iv_debug).setVisibility(View.GONE);
        mIvPlay = findViewById(R.id.iv_play_video);
        mIvPlay.setVisibility(View.VISIBLE);
        mIvPlay.setOnClickListener(mViewClickListener);
        mIvSave = findViewById(R.id.iv_save_photo);
        mIvSave.setVisibility(View.INVISIBLE);
        mIvSave.setOnClickListener(mViewClickListener);
    }

    @Override
    protected void initFuRenderer() {
        mFURenderer = new FURenderer.Builder(this)
                .setInputTextureType(FURenderer.INPUT_TEXTURE_EXTERNAL_OES)
                .build();
    }

    @Override
    protected void initGlRenderer() {
        String videoPath = getIntent().getStringExtra(VIDEO_PATH);
        LogUtils.debug(TAG, "videoPath: %s", videoPath);
        mVideoRenderer = new VideoRenderer(getLifecycle(), videoPath, mGlSurfaceView, this);
        mVideoRenderer.setOnMediaEventListener(this);

        mVideoRecorder = new VideoRecorder(mGlSurfaceView);
        mVideoRecorder.setOnVideoRecordListener(this);
    }

    @Override
    public void onSurfaceChanged(int viewWidth, int viewHeight, int videoWidth, int videoHeight, int videoRotation) {

    }

    @Override
    public int onDrawFrame(int videoTextureId, int videoWidth, int videoHeight, float[] mvpMatrix, long timeStamp) {
        int fuTextureId = mFURenderer.onDrawFrameSingleInput(videoTextureId, videoWidth, videoHeight);
        mVideoRecorder.send(fuTextureId, mvpMatrix, GlUtil.IDENTITY_MATRIX, timeStamp);
        return fuTextureId;
    }

    @Override
    public void onSurfaceDestroy() {
        super.onSurfaceDestroy();
        mVideoRecorder.stop();
    }

    @Override
    protected View getRecordView() {
        return mIvSave;
    }

    @Override
    protected void onViewClicked(int id) {
        super.onViewClicked(id);
        if (id == R.id.iv_play_video) {
            mVideoRenderer.startMediaPlayer();
            mVideoRecorder.start(mVideoRenderer.getVideoWidth(), mVideoRenderer.getVideoHeight());
        } else if (id == R.id.iv_save_photo) {

        }
    }

    @Override
    public void onCompletion() {
        mVideoRecorder.stop();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mIvPlay.setImageResource(R.drawable.video_replay);
                mIvSave.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onError(String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ToastUtil.makeText(VideoActivity.this, message).show();
            }
        });
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

    }


}
