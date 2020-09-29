package com.faceunity.nama;

import java.util.Map;

/**
 * 道具
 *
 * @author Richie on 2019.12.20
 */
public class Effect {
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
    private Map<String, Object> params;

    public Effect(String filePath) {
        this(filePath, "", 0, null);
    }

    public Effect(String filePath, String description, int type) {
        this(filePath, description, type, null);
    }

    public Effect(String filePath, String description, int type, Map<String, Object> params) {
        this.filePath = filePath;
        this.description = description;
        this.type = type;
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
                ", params=" + params +
                '}';
    }
}
