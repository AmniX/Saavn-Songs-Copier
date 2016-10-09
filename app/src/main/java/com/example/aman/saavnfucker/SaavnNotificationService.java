package com.example.aman.saavnfucker;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

public class SaavnNotificationService extends NotificationListenerService {
    private String TAG = "Aman";
    private static OnSaavnNotificationListener onSaavnNotificationListener;


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn.getPackageName().equals("com.saavn.android") && onSaavnNotificationListener != null) {
            onSaavnNotificationListener.onNotificationPosted(sbn);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sbn.getPackageName().equals("com.saavn.android")) {
            Log.i(TAG, "********** onNOtificationRemoved");
            Log.i(TAG, "Key :" + sbn.getKey() + "\t" + sbn.getNotification().tickerText + "\t" + sbn.getPackageName());
        }
    }

    public static void bind(OnSaavnNotificationListener onSaavnNotificationListener) {
        SaavnNotificationService.onSaavnNotificationListener = onSaavnNotificationListener;
    }

    public static void unbind() {
        onSaavnNotificationListener = null;
    }

}