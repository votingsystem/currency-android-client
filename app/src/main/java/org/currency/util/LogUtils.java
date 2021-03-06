package org.currency.util;

import android.util.Log;

public class LogUtils {

    public static void LOGD(final String tag, String message) {
        if (Constants.IS_DEBUG_SESSION) {
            Log.d(tag, message);
        }
    }

    public static void LOGD(final String tag, String message, Throwable cause) {
        if (Constants.IS_DEBUG_SESSION) {
            Log.d(tag, message, cause);
        }
    }

    public static void LOGE(final String tag, String message) {
        Log.e(tag, message);
    }

    public static void LOGE(final String tag, String message, Throwable cause) {
        Log.e(tag, message, cause);
    }

}
