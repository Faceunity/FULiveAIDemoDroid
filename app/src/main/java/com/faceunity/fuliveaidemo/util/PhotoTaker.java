package com.faceunity.fuliveaidemo.util;

import android.graphics.Bitmap;

import java.io.File;

/**
 * 拍照
 *
 * @author Richie on 2020.05.22
 */
public final class PhotoTaker {
    private volatile boolean mIsTaking = false;
    private volatile boolean mIsToTake = false;
    private OnPictureTakeListener mOnPictureTakeListener;

    private BitmapUtil.OnReadBitmapListener mOnReadBitmapListener = new BitmapUtil.OnReadBitmapListener() {

        @Override
        public void onReadBitmapListener(Bitmap bitmap) {
            // call on async thread
            File file = new File(Constant.PHOTO_FILE_PATH, Constant.APP_NAME + "_" + DateUtil.getCurrentDate() + ".jpg");
            boolean result = FileUtils.saveBitmap(bitmap, file);
            if (mOnPictureTakeListener != null) {
                if (result) {
                    mOnPictureTakeListener.onPictureTakeSucceed(file.getAbsolutePath());
                } else {
                    mOnPictureTakeListener.onPictureTakeFailed();
                }
            }
        }
    };

    public void setOnPictureTakeListener(OnPictureTakeListener onPictureTakeListener) {
        mOnPictureTakeListener = onPictureTakeListener;
    }

    public void mark() {
        if (mIsTaking) {
            return;
        }
        mIsToTake = true;
        mIsTaking = true;
    }

    public void send(final int texId, final float[] mvpMatrix, float[] texMatrix, final int texWidth, final int texHeight) {
        if (!mIsToTake) {
            return;
        }
        BitmapUtil.glReadBitmap(texId, texMatrix, mvpMatrix, texWidth, texHeight, mOnReadBitmapListener, false);
        mIsToTake = false;
        mIsTaking = false;
    }

    public interface OnPictureTakeListener {
        /**
         * 完成
         *
         * @param path
         */
        void onPictureTakeSucceed(String path);

        /**
         * 失败
         */
        void onPictureTakeFailed();
    }
}
