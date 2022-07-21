package com.faceunity.fuliveaidemo.util;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.StringRes;

import com.faceunity.fuliveaidemo.R;

/**
 * @author Richie on 2020.05.26
 */
public final class ToastUtil {
    private static Toast sNormalToast;

    private ToastUtil() {
    }

    public static Toast makeText(Context context, @StringRes int strId) {
        return makeText(context, context.getString(strId));
    }

    public static Toast makeText(Context context, String text) {
        if (sNormalToast == null) {
            Resources resources = context.getResources();
            TextView textView = new TextView(context);
            textView.setTextColor(resources.getColor(android.R.color.white));
            textView.setGravity(Gravity.CENTER);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, DensityUtils.dp2px(context, 13));
            textView.setBackgroundResource(R.drawable.shape_bg_toast_default);
            int hPadding = DensityUtils.dp2px(context, 15);
            int vPadding = DensityUtils.dp2px(context, 9f);
            textView.setPadding(hPadding, vPadding, hPadding, vPadding);
            textView.setText(text);
            sNormalToast = new Toast(context);
            sNormalToast.setView(textView);
            sNormalToast.setDuration(Toast.LENGTH_SHORT);
        } else {
            ((TextView) sNormalToast.getView()).setText(text);
        }
        sNormalToast.setGravity(Gravity.CENTER, 0, 0);
        return sNormalToast;
    }

}
