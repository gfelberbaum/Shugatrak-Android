package com.applivate.shugatrak2;


import android.util.Log;

/**
 * Created by paul on 18/06/2015.
 */
public class Logging {

    public static final String uniqueId = "SGTK";

    public static void Debug(String msg) {
        if (Debug.DEBUG) {
            Log.d(uniqueId, msg);
        }
    }

    public static void Debug(String tag, String msg) {
        if (Debug.DEBUG) {
            Log.i(uniqueId, tag + ":  " + msg);
        }
    }


    public static void Info(String msg) {
        Log.i(uniqueId, msg);
    }

    public static void Info(String tag, String msg) {
        Log.i(uniqueId, tag + ":  " + msg);
    }


    public static void Warning(String msg) {
        Log.w(uniqueId, msg);
    }

    public static void Warning(String tag, String msg) {
        Log.w(uniqueId, tag + ":  " + msg);
    }

    public static void Warning(String tag, String msg, Throwable throwable) {
        Log.w(uniqueId, tag + ":  EXCEPTION: " + throwable.getMessage() + " - " + msg, throwable);
    }



    public static void Verbose(String msg) {
        if (Debug.VERBOSE) {
            Log.v(uniqueId, msg);
        }
    }

    public static void Verbose(String tag, String msg) {
        if (Debug.VERBOSE) {
            Log.v(uniqueId, tag + ":  " + msg);
        }
    }


    public static void Error(String msg) {
        Log.e(uniqueId, msg);
    }

    public static void Error(String tag, String msg) {
        Log.e(uniqueId, tag + ":  " + msg);
    }

    public static void Error(String tag, String msg, Throwable throwable) {
        Log.e(uniqueId, tag + ":  EXCEPTION: " + throwable.getMessage() + " - " + msg, throwable);
    }

    public static String arrayToDumpString(Object[] array, String title) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(title + "   ");
        if (null == array) {
            stringBuilder.append("array is NULL");
        } else {
            stringBuilder.append("array.length = " + array.length);
            for (int i = 0; i < array.length; i++) {
                stringBuilder.append("\n");
                stringBuilder.append(i + ": ");
                stringBuilder.append(String.valueOf(array[i]));
            }
        }
        return stringBuilder.toString();
    }

}
