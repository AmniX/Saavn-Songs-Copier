package com.example.aman.saavnfucker;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class SaavnNotificationService extends NotificationListenerService {
    public static StatusBarNotification currentStatusBarNotification = null;
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
            Logs.d("Posted. Ticker :" + sbn.getNotification().tickerText + "\t" + sbn.getPackageName());
            onSaavnNotificationListener.onNotificationPosted(sbn);
            currentStatusBarNotification = sbn;
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sbn.getPackageName().equals("com.saavn.android")) {
            Logs.d("Removed. Ticker :" + sbn.getNotification().tickerText + "\t" + sbn.getPackageName());
        }
    }

    public static void bind(OnSaavnNotificationListener onSaavnNotificationListener) {
        SaavnNotificationService.onSaavnNotificationListener = onSaavnNotificationListener;
    }

    public static void unbind() {
        onSaavnNotificationListener = null;
    }

}