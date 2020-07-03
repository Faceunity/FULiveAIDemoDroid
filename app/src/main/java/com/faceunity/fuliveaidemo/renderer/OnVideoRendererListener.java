package com.faceunity.fuliveaidemo.renderer;

/**
 * @author Richie on 2020.06.19
 */
public interface OnVideoRendererListener extends OnRendererListener {

    /**
     * Called when surface'size changed.
     *
     * @param viewWidth
     * @param viewHeight
     * @param videoWidth
     * @param videoHeight
     * @param videoRotation
     */
    void onSurfaceChanged(int viewWidth, int viewHeight, int videoWidth, int videoHeight, int videoRotation);

    /**
     * Called when drawing current frame
     *
     * @param videoTextureId
     * @param videoWidth
     * @param videoHeight
     * @param mvpMatrix
     * @param timeStamp
     * @return
     */
    int onDrawFrame(int videoTextureId, int videoWidth, int videoHeight, float[] mvpMatrix, long timeStamp);
}
