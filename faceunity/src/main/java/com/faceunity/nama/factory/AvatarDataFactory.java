package com.faceunity.nama.factory;

import com.faceunity.core.avatar.model.PTAAvatar;
import com.faceunity.core.avatar.model.PTAScene;
import com.faceunity.core.avatar.scene.SceneHumanProcessor;
import com.faceunity.core.entity.FUBundleData;
import com.faceunity.core.entity.FUCoordinate3DData;
import com.faceunity.core.enumeration.FUAITypeEnum;
import com.faceunity.core.faceunity.FURenderKit;
import com.faceunity.core.model.antialiasing.Antialiasing;
import com.faceunity.nama.source.AvatarSource;

import java.io.File;

/**
 * DESC：
 * Created on 2021/3/30
 */
public class AvatarDataFactory {
    public static String BUNDLE_ANTI_ALIASING = "graphics" + File.separator + "fxaa.bundle";

    /*渲染控制器*/
    private FURenderKit mFURenderKit = FURenderKit.getInstance();
    /*3D抗锯齿*/
    public final Antialiasing antialiasing;

    /* 驱动类型是否为全身  */
    private Boolean isHumanTrackSceneFull;

    /* 场景  */
    private PTAScene sceneModel;
    /* 男孩对象  */
    private PTAAvatar boyAvatarModel;


    public AvatarDataFactory(boolean isFull) {
        isHumanTrackSceneFull = isFull;
        antialiasing = new Antialiasing(new FUBundleData(BUNDLE_ANTI_ALIASING));
        boyAvatarModel = AvatarSource.buildBoyData(isFull);
        sceneModel = AvatarSource.buildSceneModel(boyAvatarModel);
    }

    /**
     * 获取当前驱动类型
     *
     * @return
     */
    public boolean isHumanTrackSceneFull() {
        return isHumanTrackSceneFull;
    }

    /**
     * 设置当前驱动类型
     *
     * @param isFull
     */
    public void setHumanTrackSceneFull(boolean isFull) {
        isHumanTrackSceneFull = isFull;
        sceneModel.getMSceneHumanProcessor().setTrackScene(isFull ? SceneHumanProcessor.TrackScene.SceneFull : SceneHumanProcessor.TrackScene.SceneHalf);
        if (isFull) {
            boyAvatarModel.getMAvatarTransForm().setPosition(new FUCoordinate3DData(0.0, 58.14, -618.94));
        } else {
            boyAvatarModel.getMAvatarTransForm().setPosition(new FUCoordinate3DData(0.0, 11.76, -183.89));
        }
    }

    public void bindCurrentRenderer() {
        mFURenderKit.getFUAIController().setMaxFaces(1);
        mFURenderKit.setAntialiasing(antialiasing);
        mFURenderKit.getAvatarContainer().addScene(sceneModel);
        setHumanTrackSceneFull(isHumanTrackSceneFull);
    }

    public void removeScene() {
        mFURenderKit.getFUAIController().setMaxFaces(4);
        mFURenderKit.getAvatarContainer().removeScene(sceneModel);
        mFURenderKit.getAvatarContainer().release();
    }
}
