package com.faceunity.nama;

import java.util.Map;

/**
 * 道具
 *
 * @author Richie on 2019.12.20
 */
public class Effect {
    /**
     * 证书各模块验证码
     */
    public static final String MODULE_CODE_FACE_LANDMARKS = "0-4096";
    public static final String MODULE_CODE_FACE_TONGUE = "0-8192";
    public static final String MODULE_CODE_FACE_EXPRESSION = "2048-0";
    public static final String MODULE_CODE_HUMAN_LANDMARKS = "0-16384";
    public static final String MODULE_CODE_HUMAN_SKELETON = "0-128";
    public static final String MODULE_CODE_HAND_GESTURE = "512-0";
    public static final String MODULE_CODE_HUMAN_SEGMENTATION = "256-0";
    public static final String MODULE_CODE_HAIR_SEGMENTATION = "1048576-0";
    public static final String MODULE_CODE_HEAD_SEGMENTATION = "0-32768";
    public static final String MODULE_CODE_ACTION = "0-65536";

    /* type */
    public static final int TYPE_FACE = 1;
    public static final int TYPE_HUMAN = 2;
    public static final int TYPE_GESTURE = 3;
    public static final int TYPE_SEGMENTATION = 4;
    public static final int TYPE_ACTION = 5;

    /* state */
    public static final int STATE_ENABLE = 1;
    public static final int STATE_DISABLE = 2;

    private String filePath;
    private String description;
    private int handle;
    private int type;
    private int state = STATE_ENABLE;
    private String authCode;
    private Map<String, Object> params;

    public Effect(String filePath, String authCode) {
        this(filePath, "", 0, authCode);
    }

    public Effect(String filePath, String description, int type, String authCode) {
        this(filePath, description, type, authCode, null);
    }

    public Effect(String filePath, String description, int type, String authCode, Map<String, Object> params) {
        this.filePath = filePath;
        this.description = description;
        this.type = type;
        this.authCode = authCode;
        this.params = params;
    }

    public Effect(Effect effect) {
        this.filePath = effect.filePath;
        this.description = effect.description;
        this.type = effect.type;
        this.params = effect.params;
        this.state = effect.state;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getHandle() {
        return handle;
    }

    public void setHandle(int handle) {
        this.handle = handle;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Effect effect = (Effect) o;
        if (filePath != null ? !filePath.equals(effect.filePath) : effect.filePath != null) {
            return false;
        }
        return description != null ? description.equals(effect.description) : effect.description == null;
    }

    @Override
    public int hashCode() {
        int result = filePath != null ? filePath.hashCode() : 0;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Effect{" +
                "filePath='" + filePath + '\'' +
                ", description='" + description + '\'' +
                ", type='" + type +
                ", handle=" + handle +
                ", state=" + state +
                ", authCode=" + authCode +
                ", params=" + params +
                '}';
    }
}
