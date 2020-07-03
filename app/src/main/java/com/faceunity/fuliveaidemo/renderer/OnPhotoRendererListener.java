package com.faceunity.fuliveaidemo.renderer;

/**
 * @author Richie on 2020.05.25
 */
public interface OnPhotoRendererListener extends OnRendererListener {
    /**
     * Called when drawing current frame.
     *
     * @param photoRgbaByte
     * @param photoTextureId
     * @param photoWidth
     * @param photoHeight
     * @return
     */
    int onDrawFrame(byte[] photoRgbaByte, int photoTextureId, int photoWidth, int photoHeight);

    /**
     * Called when error happened
     *
     * @param error
     */
    void onLoadPhotoError(String error);
}
