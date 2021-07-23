package com.faceunity.nama.source;

import com.faceunity.core.avatar.avatar.AvatarTransForm;
import com.faceunity.core.avatar.model.PTAAvatar;
import com.faceunity.core.avatar.model.PTAScene;
import com.faceunity.core.entity.FUBundleData;
import com.faceunity.core.entity.FUCoordinate3DData;

import java.io.File;
import java.util.ArrayList;

/**
 * DESC：Avatar数据构造
 * Created on 2021/3/30
 */
public class AvatarSource {
    // Avatar
    private static String BUNDLE_AVATAR_CONTROLLER = "graphics" + File.separator + "controller_cpp.bundle";
    private static String BUNDLE_AVATAR_CONFIG = "pta" + File.separator + "controller_config.bundle";
    private static String BUNDLE_AVATAR_BACKGROUND = "pta" + File.separator + "default_bg.bundle";

    public static PTAScene buildSceneModel(PTAAvatar avatar) {
        FUBundleData controlBundle = new FUBundleData(BUNDLE_AVATAR_CONTROLLER);
        FUBundleData avatarConfig = new FUBundleData(BUNDLE_AVATAR_CONFIG);
        ArrayList<PTAAvatar> avatars = new ArrayList<PTAAvatar>();
        avatars.add(avatar);
        PTAScene sceneModel = new PTAScene(controlBundle, avatarConfig, avatars);
        sceneModel.getMSceneHumanProcessor().setEnableHumanProcessor(true);
        sceneModel.getMSceneBackground().setBackgroundBundle(new FUBundleData(BUNDLE_AVATAR_BACKGROUND));
        return sceneModel;
    }

    /**
     * 获取男孩对象
     *
     * @return
     */
    public static PTAAvatar buildBoyData(boolean isFull) {
        String ptaBoyDir = "pta/";
        ArrayList<FUBundleData> components = new ArrayList();
        components.add(new FUBundleData(ptaBoyDir + "fakeman.bundle"));
        ArrayList<FUBundleData> animations = buildAnimations();
        PTAAvatar model = new PTAAvatar(components, animations);
        AvatarTransForm avatarTransForm = model.getMAvatarTransForm();
        avatarTransForm.setPosition(isFull ? new FUCoordinate3DData(0.0, 58.14, -618.94) : new FUCoordinate3DData(0.0, 11.76, -183.89));
        return model;
    }

    /**
     * 构造动画参数
     *
     * @return
     */
    public static ArrayList<FUBundleData> buildAnimations() {
        String animDir = "pta/gesture/";
        ArrayList<FUBundleData> animations = new ArrayList();
        animations.add(new FUBundleData(animDir + "anim_idle.bundle"));
        animations.add(new FUBundleData(animDir + "anim_eight.bundle"));
        animations.add(new FUBundleData(animDir + "anim_fist.bundle"));
        animations.add(new FUBundleData(animDir + "anim_greet.bundle"));
        animations.add(new FUBundleData(animDir + "anim_gun.bundle"));
        animations.add(new FUBundleData(animDir + "anim_heart.bundle"));
        animations.add(new FUBundleData(animDir + "anim_hold.bundle"));
        animations.add(new FUBundleData(animDir + "anim_korheart.bundle"));
        animations.add(new FUBundleData(animDir + "anim_merge.bundle"));
        animations.add(new FUBundleData(animDir + "anim_ok.bundle"));
        animations.add(new FUBundleData(animDir + "anim_one.bundle"));
        animations.add(new FUBundleData(animDir + "anim_palm.bundle"));
        animations.add(new FUBundleData(animDir + "anim_rock.bundle"));
        animations.add(new FUBundleData(animDir + "anim_six.bundle"));
        animations.add(new FUBundleData(animDir + "anim_thumb.bundle"));
        animations.add(new FUBundleData(animDir + "anim_two.bundle"));
        return animations;
    }


}
