package com.faceunity.fuliveaidemo.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import com.faceunity.fuliveaidemo.R;
import com.faceunity.fuliveaidemo.renderer.OnPhotoRendererListener;
import com.faceunity.fuliveaidemo.renderer.PhotoRenderer;
import com.faceunity.fuliveaidemo.util.ToastUtil;
import com.faceunity.nama.FURenderer;
import com.faceunity.nama.utils.LogUtils;

/**
 * @author Richie on 2020.05.21
 */
public class PhotoActivity extends BaseGlActivity implements OnPhotoRendererListener {
    private static final String TAG = "PhotoActivity";
    public static final String PHOTO_PATH = "photo_path";
    private View mIvSavePhoto;

    public static void actionStart(Activity context, String photoPath) {
        Intent intent = new Intent(context, PhotoActivity.class);
        intent.putExtra(PHOTO_PATH, photoPath);
        context.startActivity(intent);
    }

    @Override
    public int onDrawFrame(byte[] photoRgbaByte, int photoTextureId, int photoWidth, int photoHeight) {
        int fuTexId = mFURenderer.onDrawFrameSingleInput(photoTextureId, photoWidth, photoHeight);
        trackFace();
        trackHuman();
        queryTrackStatus();
        mPhotoTaker.send(fuTexId, PhotoRenderer.MATRIX_ROTATE_90, PhotoRenderer.IMAGE_TEXTURE_MATRIX, photoWidth, photoHeight);
        return fuTexId;
    }

    @Override
    protected void initView() {
        findViewById(R.id.iv_switch_cam).setVisibility(View.GONE);
        findViewById(R.id.btn_record_video).setVisibility(View.GONE);
        findViewById(R.id.iv_debug).setVisibility(View.GONE);
        mIvSavePhoto = findViewById(R.id.iv_save_photo);
        mIvSavePhoto.setOnClickListener(mViewClickListener);
    }

    @Override
    protected void initFuRenderer() {
        mFURenderer = new FURenderer.Builder(this)
                .setInputTextureType(FURenderer.INPUT_TEXTURE_2D)
                .setOnSystemErrorListener(this)
                .build();
    }

    @Override
    protected void initGlRenderer() {
        String photoPath = getIntent().getStringExtra(PHOTO_PATH);
        LogUtils.debug(TAG, "photoPath: %s", photoPath);
        new PhotoRenderer(getLifecycle(), photoPath, mGlSurfaceView, this);
    }

    @Override
    protected void onViewClicked(int id) {
        super.onViewClicked(id);
        if (id == R.id.iv_save_photo) {
            mPhotoTaker.mark();
        }
    }

    @Override
    protected View getRecordView() {
        return mIvSavePhoto;
    }

    @Override
    protected boolean containHumanAvatar() {
        return false;
    }

    @Override
    public void onLoadPhotoError(final String error) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ToastUtil.makeText(PhotoActivity.this, error).show();
            }
        });
    }

}
