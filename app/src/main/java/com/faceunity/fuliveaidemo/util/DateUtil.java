package com.faceunity.fuliveaidemo.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @author Richie on 2020.05.22
 */
public final class DateUtil {
    private DateUtil() {
    }

    public static String getCurrentDate() {
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
        return df.format(new Date());
    }
}
