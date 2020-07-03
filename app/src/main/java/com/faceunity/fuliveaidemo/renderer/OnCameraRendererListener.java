package com.faceunity.fuliveaidemo.renderer;

/**
 * @author Richie on 2019.08.23
 */
public interface OnCameraRendererListener extends OnRendererListener {

    /**
     * Called when drawing current frame
     *
     * @param cameraNv21Byte
     * @param cameraTexId
     * @param cameraWidth
     * @param cameraHeight
     * @param mvpMatrix
     * @param texMatrix
     * @param timeStamp
     * @return
     */
    int onDrawFrame(byte[] cameraNv21Byte, int cameraTexId, int cameraWidth, int cameraHeight, float[] mvpMatrix, float[] texMatrix, long timeStamp);

    /**
     * Called when camera changed
     *
     * @param cameraFacing      FACE_BACK = 0, FACE_FRONT = 1
     * @param cameraOrientation
     */
    void onCameraChanged(int cameraFacing, int cameraOrientation);

    /**
     * Called when camera is opened
     *
     * @param cameraWidth
     * @param cameraHeight
     */
    void onCameraOpened(int cameraWidth, int cameraHeight);

    /**
     * Called when camera operation happens error
     *
     * @param message
     */
    void onCameraError(String message);
}
