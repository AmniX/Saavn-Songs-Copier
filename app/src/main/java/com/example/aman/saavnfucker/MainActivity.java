package com.example.aman.saavnfucker;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.mpatric.mp3agic.InvalidDataException;

import java.io.FileNotFoundException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnSaavnNotificationListener, Logs.OnLogListener {
    private int twiceDelay = 500;
    private int totalSongs = 0;
    private int currentSongs;
    private boolean allowed = true;
    private boolean seek2Next = true;
    private Toolbar toolbar;
    private SwitchCompat saveAll;
    private SwitchCompat saveButton;
    private LogsTextView logView;
    private LinearLayout content_main;
    private SwitchCompat seekButton;
    private NestedScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        initView();
        Logs.bind(this);
        checkPermission();
    }

    private void checkPermission() {
        Logs.d("Checking Permissions!");
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            askPermission();
            Logs.d("Permissions Not Granted!");
            return;
        }
        Logs.d("Permissions Granted!");
        checkNotificationAccess();
    }

    private void askPermission() {
        Logs.d("Asking Permissions!");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 40);
        }else
            initActivity();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 40)
            checkPermission();
    }

    private void checkNotificationAccess() {
        if (Settings.Secure.getString(this.getContentResolver(), "enabled_notification_listeners").contains(getApplicationContext().getPackageName()))
            checkDrawOverLays();
        else {
            Toast.makeText(MainActivity.this, "Please Enable Notification Access", Toast.LENGTH_LONG).show();
            startActivityForResult(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"), 41);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 41)
            checkNotificationAccess();
        else if (requestCode == 42)
            checkDrawOverLays();
    }

    private void checkDrawOverLays() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(MainActivity.this))
                initActivity();
            else {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 42);
                Toast.makeText(MainActivity.this, "Please Enable Draw Over Other App to Draw on Saavn App", Toast.LENGTH_LONG).show();
            }
        } else
            initActivity();
    }

    private void initActivity() {
        totalSongs = SongUtil.getcurrmp3Path().getParentFile().list().length - 1;
        SaavnNotificationService.bind(this);
        saveButton.setChecked(isServiceRunning(SaavnPopupService.class));
        saveButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked)
                startService(new Intent(this, SaavnPopupService.class));
            else
                stopService(new Intent(this, SaavnPopupService.class));
        });
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> allservices = manager.getRunningServices(Integer.MAX_VALUE);
        for (int i = 0; i < allservices.size(); i++)
            if (allservices.get(i).service.getClassName().equals(serviceClass.getName()))
                return true;
        return false;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        SaavnNotificationService.unbind();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification statusBarNotification) {
        if (!allowed || !saveAll.isChecked())
            return;
        if (currentSongs >= totalSongs) {
            seek2Next = false;
            return;
        }
        allowed = false;
        runOnUiThread(() -> new Handler().postDelayed(() -> allowed = true, twiceDelay));
        runOnUiThread(() -> new Handler().postDelayed(() -> {
            try {
                SongUtil.saveSong(MainActivity.this, statusBarNotification);
                if (seek2Next && seekButton.isChecked())
                    runOnUiThread(() -> new Handler().postDelayed(this::seek2NextSong, 1000));
                currentSongs++;
            } catch (FileNotFoundException e) {
                onNotificationPosted(statusBarNotification);
            } catch (InvalidDataException e) {
                Logs.d("Song Saved But Unable to save Meta Info. Possibly Different Extension");
                if (seek2Next && seekButton.isChecked())
                    runOnUiThread(() -> new Handler().postDelayed(this::seek2NextSong, 3000));
                currentSongs++;
            } catch (Exception e) {
                Logs.wtf(e);
            }
        }, 500));
    }


    @MainThread
    private void seek2NextSong() {
        Intent i = new Intent(Intent.ACTION_MEDIA_BUTTON);
        i.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT));
        sendStickyBroadcast(i);
    }


    @Override
    public void onLogAdded(String msg) {
        runOnUiThread(() -> {
            logView.appendLine(msg);
            scrollView.fullScroll(NestedScrollView.FOCUS_DOWN);
        });

    }

    private void initView() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        saveAll = (SwitchCompat) findViewById(R.id.saveAll);
        saveButton = (SwitchCompat) findViewById(R.id.saveButton);
        logView = (LogsTextView) findViewById(R.id.logView);
        content_main = (LinearLayout) findViewById(R.id.content_main);
        seekButton = (SwitchCompat) findViewById(R.id.seekButton);
        scrollView = (NestedScrollView) findViewById(R.id.scrollView);
    }
}
