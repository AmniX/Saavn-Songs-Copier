package com.example.aman.saavnfucker;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.mpatric.mp3agic.InvalidDataException;

import java.io.File;
import java.io.FileNotFoundException;

import static com.example.aman.saavnfucker.SongUtil.saveSong;

/**
 * Created by Aman on 10-10-2016.
 */

public class SaavnPopupService extends Service {
    private WindowManager windowManager;
    private View widget;
    private WindowManager.LayoutParams params;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (windowManager != null && widget != null)
                windowManager.removeView(widget);
        } catch (Exception e) {
            Logs.wtf(e);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        widget = LayoutInflater.from(this).inflate(R.layout.saavn_widget,null,false);
        widget.findViewById(R.id.buttonSave).setOnClickListener(v -> {
            if (SaavnNotificationService.currentStatusBarNotification != null)
                try {
                    saveSong(SaavnPopupService.this, SaavnNotificationService.currentStatusBarNotification);
                } catch (FileNotFoundException e) {
                    Logs.d("File is Not Ready to Copy Yet! Trying Again to Copy File.");
                    widget.postDelayed(() -> widget.performClick(),500);
                } catch (InvalidDataException e) {
                    Logs.d("Song Saved But Unable to save Meta Info. Possibly Different Extension");
                    Toast.makeText(SaavnPopupService.this, "Song Saved But Unable to save Meta Info. Possibly Different Extension", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Logs.wtf(e);
                }
        });
        widget.findViewById(R.id.buttonSave).setOnLongClickListener(v -> {
            windowManager.removeView(widget);
            stopSelf();
            return true;
        });
        widget.findViewById(R.id.buttonShare).setOnClickListener(v -> {
            if (SaavnNotificationService.currentStatusBarNotification != null)
                try {
                    File savedSong = SongUtil.saveSong(SaavnPopupService.this, SaavnNotificationService.currentStatusBarNotification);
                    Uri uri = Uri.fromFile(savedSong);
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("audio/*");
                    share.putExtra(Intent.EXTRA_STREAM, uri);
                    share.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    share.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(Intent.createChooser(share, "Share via"));
                } catch (FileNotFoundException e) {
                    Logs.d("File is Not Ready to Copy Yet! Trying Again to Copy File.");
                    widget.postDelayed(() -> widget.performClick(),500);
                } catch (InvalidDataException e) {
                    Logs.d("Song Saved But Unable to save Meta Info. Possibly Different Extension");
                    Toast.makeText(SaavnPopupService.this, "Song Saved But Unable to save Meta Info. Possibly Different Extension", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Logs.wtf(e);
                }
        });
        widget.findViewById(R.id.buttonShare).setOnLongClickListener(v -> {
            windowManager.removeView(widget);
            stopSelf();
            return true;
        });

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.CENTER_VERTICAL | Gravity.END;
        params.x = 0;
        params.y = 100;
        //widget.setOnTouchListener(new WidgetMover());
        widget.setPadding(2, 2, 2, 2);
        windowManager.addView(widget, params);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    private class WidgetMover implements View.OnTouchListener {
        private int initialX;
        private int initialY;
        private float initialTouchX;
        private float initialTouchY;
        long touchStartTime;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchStartTime = System.currentTimeMillis();
                    initialX = params.x;
                    initialY = params.y;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    params.x = 0;//initialX + (int) (event.getRawX() - initialTouchX);
                    params.y = initialY + (int) (event.getRawY() - initialTouchY);
                    windowManager.updateViewLayout(v, params);
                    break;
            }
            return false;
        }
    }


}
