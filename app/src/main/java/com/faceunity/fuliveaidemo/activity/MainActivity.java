package com.faceunity.fuliveaidemo.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.faceunity.FUConfig;
import com.faceunity.fuliveaidemo.R;
import com.faceunity.fuliveaidemo.util.FileUtils;
import com.faceunity.fuliveaidemo.util.PermissionUtil;
import com.faceunity.fuliveaidemo.util.ScreenUtils;
import com.faceunity.fuliveaidemo.util.ToastUtil;
import com.faceunity.fuliveaidemo.util.UriUtil;
import com.faceunity.fuliveaidemo.view.OnMultiClickListener;
import com.faceunity.nama.utils.FuDeviceUtils;
import com.faceunity.nama.utils.LogUtils;

import java.io.File;

/**
 * @author Richie on 2020.05.21
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int IMAGE_REQUEST_CODE_PHOTO = 735;
    private static final int IMAGE_REQUEST_CODE_VIDEO = 736;
    private String mPhotoPath, mVideoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScreenUtils.fullScreen(this);
        setContentView(R.layout.activity_main);

        PermissionUtil.checkPermission(this);

        ViewTouchListener viewTouchListener = new ViewTouchListener();
        findViewById(R.id.cv_photo).setOnTouchListener(viewTouchListener);
        findViewById(R.id.cv_camera).setOnTouchListener(viewTouchListener);
        findViewById(R.id.cv_video).setOnTouchListener(viewTouchListener);

        FUConfig.DEVICE_LEVEL = FuDeviceUtils.judgeDeviceLevel(this);
    }

    private class ViewTouchListener implements View.OnTouchListener {
        private static final float SCALE_FACTOR = 0.96F;
        private static final int ANIMATION_DURATION = 60;
        private int mLongPressTimeout = ViewConfiguration.getLongPressTimeout();
        private long mStartTimestamp;
        private long mLastClickTime;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    v.animate().scaleX(SCALE_FACTOR).scaleY(SCALE_FACTOR).setDuration(ANIMATION_DURATION).start();
                    mStartTimestamp = System.currentTimeMillis();
                }
                break;
                case MotionEvent.ACTION_UP: {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(ANIMATION_DURATION).start();

                    long current = System.currentTimeMillis();
                    if (current - mLastClickTime > OnMultiClickListener.MIN_CLICK_DELAY_TIME
                            && current - mStartTimestamp < mLongPressTimeout) {
                        switch (v.getId()) {
                            case R.id.cv_camera: {
                                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                                startActivity(intent);
                            }
                            break;
                            case R.id.cv_photo: {
                                Intent intentPhoto = new Intent();
                                intentPhoto.setAction(Intent.ACTION_OPEN_DOCUMENT);
                                intentPhoto.addCategory(Intent.CATEGORY_OPENABLE);
                                intentPhoto.setType("image/*");
                                ResolveInfo resolveInfo = getPackageManager().resolveActivity(intentPhoto,
                                        PackageManager.MATCH_DEFAULT_ONLY);
                                if (resolveInfo == null) {
                                    intentPhoto.setAction(Intent.ACTION_GET_CONTENT);
                                    intentPhoto.removeCategory(Intent.CATEGORY_OPENABLE);
                                }
                                startActivityForResult(intentPhoto, IMAGE_REQUEST_CODE_PHOTO);
                            }
                            break;
                            case R.id.cv_video:
                                Intent intentVideo = new Intent();
                                intentVideo.addCategory(Intent.CATEGORY_OPENABLE);
                                intentVideo.setType("video/*");
                                intentVideo.setAction(Intent.ACTION_OPEN_DOCUMENT);
                                ResolveInfo resolveInfo = getPackageManager().resolveActivity(intentVideo, PackageManager.MATCH_DEFAULT_ONLY);
                                if (resolveInfo == null) {
                                    intentVideo.setAction(Intent.ACTION_GET_CONTENT);
                                    intentVideo.removeCategory(Intent.CATEGORY_OPENABLE);
                                }
                                startActivityForResult(intentVideo, IMAGE_REQUEST_CODE_VIDEO);
                                break;
                            default:
                        }
                    }
                    mLastClickTime = System.currentTimeMillis();
                    mStartTimestamp = 0;
                }
                default:
            }
            return true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!TextUtils.isEmpty(mPhotoPath)) {
            PhotoActivity.actionStart(this, mPhotoPath);
            mPhotoPath = null;
        }
        if (!TextUtils.isEmpty(mVideoPath)) {
            VideoActivity.start(this, mVideoPath);
            mVideoPath = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        LogUtils.debug(TAG, "onActivityResult. intent: %s", data != null ? data.toUri(0) : "");
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) {
            return;
        }
        Uri uri = data.getData();
        if (requestCode == IMAGE_REQUEST_CODE_PHOTO) {
            String photoPath = UriUtil.getFileAbsolutePath(this, uri);
            if (TextUtils.isEmpty(photoPath) || !new File(photoPath).exists()) {
                ToastUtil.makeText(MainActivity.this, R.string.image_not_exist).show();
                return;
            }
            if (!checkIsImage(photoPath)) {
                ToastUtil.makeText(this, R.string.wrong_image_format).show();
                return;
            }
            mPhotoPath = photoPath;
        } else if (requestCode == IMAGE_REQUEST_CODE_VIDEO) {
            String videoPath = UriUtil.getFileAbsolutePath(this, uri);
            if (TextUtils.isEmpty(videoPath) || !new File(videoPath).exists()) {
                ToastUtil.makeText(MainActivity.this, R.string.image_not_exist).show();
                return;
            }
            mVideoPath = videoPath;
        }
    }

    private boolean checkIsImage(String path) {
        String name = new File(path).getName().toLowerCase();
        return name.endsWith(FileUtils.IMAGE_FORMAT_PNG)
                || name.endsWith(FileUtils.IMAGE_FORMAT_JPG)
                || name.endsWith(FileUtils.IMAGE_FORMAT_JPEG);
    }

}
