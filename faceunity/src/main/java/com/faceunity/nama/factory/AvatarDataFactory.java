package com.faceunity.nama.factory;

import com.faceunity.core.avatar.model.Avatar;
import com.faceunity.core.avatar.model.Scene;
import com.faceunity.core.avatar.scene.ProcessorConfig;
import com.faceunity.core.entity.FUBundleData;
import com.faceunity.core.entity.FUCoordinate3DData;
import com.faceunity.core.faceunity.FURenderKit;
import com.faceunity.core.faceunity.FUSceneKit;
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
    public Boolean isHumanTrackSceneFull;

    /* 场景  */
    private Scene sceneModel;
    /* 男孩对象  */
    private Avatar boyAvatarModel;


    public AvatarDataFactory(boolean isFull) {
        isHumanTrackSceneFull = isFull;
        antialiasing = new Antialiasing(new FUBundleData(BUNDLE_ANTI_ALIASING));
        boyAvatarModel = AvatarSource.buildBoyData(isFull);
        sceneModel = AvatarSource.buildSceneModel(boyAvatarModel);
    }

    /**
     * 设置当前驱动类型
     *
     */
    public void setHumanTrackSceneFull() {
        sceneModel.processorConfig.setTrackScene(isHumanTrackSceneFull ? ProcessorConfig.TrackScene.SceneFull : ProcessorConfig.TrackScene.SceneHalf);
        if (isHumanTrackSceneFull) {
            boyAvatarModel.transForm.setPosition(new FUCoordinate3DData(0.0, 58.14, -618.94));
        } else {
            boyAvatarModel.transForm.setPosition(new FUCoordinate3DData(0.0, 11.76, -183.89));
        }
    }

    public void bindCurrentRenderer() {
        mFURenderKit.getFUAIController().setMaxFaces(1);
        mFURenderKit.setAntialiasing(antialiasing);
        FUSceneKit.getInstance().addScene(sceneModel);
        FUSceneKit.getInstance().setCurrentScene(sceneModel);
        setHumanTrackSceneFull();
    }

    public void removeScene() {
        mFURenderKit.getFUAIController().setMaxFaces(4);
        FUSceneKit.getInstance().removeScene(sceneModel);
    }

    public void release() {
        FUSceneKit.getInstance().release();
    }
}
