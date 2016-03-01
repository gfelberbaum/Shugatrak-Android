package com.applivate.shugatrakii;


import android.util.Log;

/**
 *
 * A Logging class to encapsulate logging functions.
 *
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

    /**
     * This is a debugging tool.  It generates a printable string version of an array,
     * with a title parameter to aid debugging.
     * @param array The array to dump
     * @param title A description of the array and the context it's being used in,
     *              for aiding debugging.
     * @return A string version of the contents of 'array'
     */
    public static String arrayToDumpString(Object[] array, String title) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(title);
        stringBuilder.append("   ");
        if (null == array) {
            stringBuilder.append("array is NULL");
        } else {
            stringBuilder.append("array.length = ");
            stringBuilder.append(array.length);
            for (int i = 0; i < array.length; i++) {
                stringBuilder.append("\n");
                stringBuilder.append(i);
                stringBuilder.append(": ");
                stringBuilder.append(String.valueOf(array[i]));
            }
        }
        return stringBuilder.toString();
    }

}
