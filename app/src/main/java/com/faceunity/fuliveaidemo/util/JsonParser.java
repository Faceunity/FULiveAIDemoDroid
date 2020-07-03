package com.faceunity.fuliveaidemo.util;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Richie on 2020.05.29
 */
public final class JsonParser {
    private static final String TAG = "JsonParser";

    private JsonParser() {
    }

    public static List<float[]> parseActionJoint2d(String jsonStr) {
        List<float[]> actionJoint2dsList = new ArrayList<>(16);
        try {
            JSONObject jsonObject = new JSONObject(jsonStr);
            JSONArray actionJoint2ds = jsonObject.optJSONArray("action_joint2ds");
            for (int i = 0, len = actionJoint2ds.length(); i < len; i++) {
                JSONObject actionJoint2d = actionJoint2ds.optJSONObject(i);
                JSONArray joint2ds = actionJoint2d.optJSONArray("joint2ds");
                int length = joint2ds.length();
                float[] joint2dsArray = new float[length];
                for (int j = 0; j < length; j++) {
                    joint2dsArray[j] = (float) joint2ds.optDouble(j);
                }
                actionJoint2dsList.add(joint2dsArray);
            }
        } catch (JSONException e) {
            Log.e(TAG, "parseActionJoint2d: ", e);
        }
        return actionJoint2dsList;
    }
}
