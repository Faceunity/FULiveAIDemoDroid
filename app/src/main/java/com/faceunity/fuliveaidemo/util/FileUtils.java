package com.faceunity.fuliveaidemo.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Richie on 2018.08.30
 */
public final class FileUtils {
    private static final String TAG = "FileUtils";
    public static final String IMAGE_FORMAT_JPG = ".jpg";
    public static final String IMAGE_FORMAT_JPEG = ".jpeg";
    public static final String IMAGE_FORMAT_PNG = ".png";

    private FileUtils() {
    }

    public static boolean saveBitmap(Bitmap bitmap, File file) {
        if (bitmap == null || file == null) {
            return false;
        }
        if (file.exists()) {
            file.delete();
        }
        Bitmap.CompressFormat format = Bitmap.CompressFormat.JPEG;
        try (FileOutputStream stream = new FileOutputStream(file)) {
            bitmap.compress(format, 100, stream);
            stream.flush();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "saveBitmap: ", e);
        }
        return false;
    }

    public static void createFile(String path) {
        if (path == null) {
            return;
        }
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public static boolean copyFile(File src, File dest) {
        if (src == null || dest == null) {
            return false;
        }
        try (FileInputStream is = new FileInputStream(src)) {
            return copyFile(is, dest);
        } catch (IOException e) {
            Log.e(TAG, "copyFile: ", e);
        }
        return false;
    }

    public static boolean copyFile(InputStream is, File dest) {
        if (is == null || dest == null) {
            return false;
        }
        if (dest.exists()) {
            dest.delete();
        }
        try (BufferedInputStream bis = new BufferedInputStream(is); BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(dest))) {
            byte[] bytes = new byte[1024 * 10];
            int length;
            while ((length = bis.read(bytes)) != -1) {
                bos.write(bytes, 0, length);
            }
            bos.flush();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "copyFile: ", e);
        }
        return false;
    }

    /**
     * 应用外部文件目录
     *
     * @param context
     * @return
     */
    public static File getExternalFileDir(Context context) {
        File fileDir = context.getExternalFilesDir(null);
        if (fileDir == null) {
            fileDir = context.getFilesDir();
        }
        return fileDir;
    }

    /**
     * 应用外部的缓存目录
     *
     * @param context
     * @return
     */
    public static File getExternalCacheDir(Context context) {
        File cacheDir = context.getExternalCacheDir();
        if (cacheDir == null) {
            cacheDir = context.getCacheDir();
        }
        return cacheDir;
    }

    public static File getThumbnailDir(Context context) {
        File fileDir = getExternalFileDir(context);
        File thumbDir = new File(fileDir, "thumb");
        if (!thumbDir.exists()) {
            thumbDir.mkdirs();
        }
        return thumbDir;
    }

    public static String readStringFromAssetsFile(Context context, String path) throws IOException {
        try (InputStream is = context.getAssets().open(path)) {
            byte[] bytes = new byte[is.available()];
            is.read(bytes);
            return new String(bytes);
        }
    }

    private static void copyAssetsFile(Context context, File dir, String assetsPath) {
        String fileName = assetsPath.substring(assetsPath.lastIndexOf("/") + 1);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File dest = new File(dir, fileName);
        if (!dest.exists()) {
            try {
                InputStream is = context.getAssets().open(assetsPath);
                FileUtils.copyFile(is, dest);
            } catch (IOException e) {
                Log.e(TAG, "copyAssetsFile: ", e);
            }
        }
    }

    public static String readStringFromFile(File file) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            byte[] bytes = new byte[bis.available()];
            bis.read(bytes);
            return new String(bytes);
        }
    }

    public static void deleteFile(File file) {
        if (file.exists()) {
            file.delete();
        }
    }

}
