package com.example.aman.saavnfucker;

import android.util.Log;

/**
 * Created by Aman on 10-10-2016.
 */

public class Logs {
    private static OnLogListener onLogListener;

    public static void bind(OnLogListener onLogListener) {
        Logs.onLogListener = onLogListener;
    }

    interface OnLogListener {
        void onLogAdded(String msg);
    }

    public static void d(String msg) {
        if (onLogListener != null)
            onLogListener.onLogAdded(msg);
        Log.d("Aman", msg);
    }

    public static void wtf(Throwable e){
        if(onLogListener != null)
            onLogListener.onLogAdded(Log.getStackTraceString(e));
        Log.wtf("Aman",e);
    }
}
