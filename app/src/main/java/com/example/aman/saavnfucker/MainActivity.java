package com.example.aman.saavnfucker;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.ID3v24Tag;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.NotSupportedException;
import com.mpatric.mp3agic.UnsupportedTagException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements OnSaavnNotificationListener {
    private int twiceDelay = 500;
    private int totalSongs = 0;
    private int currentSongs;
    private boolean allowed = true;
    private boolean seek2Next = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermission();
    }

    private void checkPermission() {
        try {
            for (String requestedPermission : getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_PERMISSIONS).requestedPermissions) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, requestedPermission) == PackageManager.PERMISSION_DENIED) {
                    askPermission();
                    return;
                }
            }
            initActivity();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

    }

    private void askPermission() {
        try {
            requestPermissions(getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_PERMISSIONS).requestedPermissions, 40);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 40)
            checkPermission();
    }

    private void initActivity() {
        if (Settings.Secure.getString(this.getContentResolver(), "enabled_notification_listeners").contains(getApplicationContext().getPackageName()))
            initActivityFinal();
        else {
            startActivityForResult(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"), 41);
            Toast.makeText(MainActivity.this, "Enable Notification Access", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 41)
            initActivity();
    }

    private void initActivityFinal() {
        totalSongs = getcurrmp3Path().getParentFile().list().length - 1;
        SaavnNotificationService.bind(this);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            //seek2NextSong();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SaavnNotificationService.unbind();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification statusBarNotification) {
        if (!allowed)
            return;
        if (currentSongs >= totalSongs) {
            seek2Next = false;
            return;
        }
        allowed = false;
        runOnUiThread(() -> new Handler().postDelayed(() -> allowed = true, twiceDelay));
        runOnUiThread(() -> new Handler().postDelayed(() -> {
            try {
                String songname = saveSong(statusBarNotification);
                if (seek2Next)
                    runOnUiThread(() -> new Handler().postDelayed(this::seek2NextSong, 1000));
                Log.d("AmniX", songname + " Song Saved");
                Toast.makeText(MainActivity.this, songname + " Saved", Toast.LENGTH_SHORT).show();
                currentSongs++;
            } catch (FileNotFoundException e) {
                onNotificationPosted(statusBarNotification);
            } catch (InvalidDataException e) {
                Log.d("AmniX", "Song Saved But Unable to save Meta Info. Possibly Different Extension");
                Toast.makeText(MainActivity.this, "Song Saved But Unable to save Meta Info. Possibly Different Extension", Toast.LENGTH_LONG).show();
                if (seek2Next)
                    runOnUiThread(() -> new Handler().postDelayed(this::seek2NextSong, 3000));
                currentSongs++;
            } catch (Exception e) {
                Log.wtf("AmniX", e);
            }
        }, 500));
    }


    @MainThread
    private void seek2NextSong() {
        Intent i = new Intent(Intent.ACTION_MEDIA_BUTTON);
        i.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT));
        sendStickyBroadcast(i);
    }

    @WorkerThread
    private String saveSong(StatusBarNotification statusBarNotification) throws Exception {
        File folder = getMusicDirectory();
        if (!folder.exists())
            if (folder.mkdir())
                folder.mkdirs();
        File sourceFile = getcurrmp3Path();
        SongInfo songInfo = getSongInfo(statusBarNotification);
        File destination = new File(folder, songInfo.getFileName());
        copy(sourceFile, destination);
        writeTags(sourceFile, destination, songInfo);
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(destination)));
        return destination.getName();
    }

    public void writeTags(File sourceFile, File destinationFile, SongInfo songInfo) throws InvalidDataException, IOException, UnsupportedTagException, NotSupportedException {
        ID3v2 id3v2Tag;
        Mp3File mp3File = new Mp3File(sourceFile.getAbsoluteFile());
        if (mp3File.hasId3v2Tag())
            id3v2Tag = mp3File.getId3v2Tag();
        else {
            id3v2Tag = new ID3v24Tag();
            mp3File.setId3v2Tag(id3v2Tag);
        }
        mp3File.setId3v2Tag(id3v2Tag);
        id3v2Tag.setTitle(songInfo.getTitle());
        id3v2Tag.setArtist(songInfo.getArtist());
        id3v2Tag.setAlbum(songInfo.getAlbum());
        id3v2Tag.setAlbumImage(songInfo.getImageBytes(), "image/jpeg");
        mp3File.save(destinationFile.getAbsolutePath());
    }

    public void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0)
            out.write(buf, 0, len);
        in.close();
        out.close();
        out.flush();
    }

    private File getcurrmp3Path() {
        if (new File(Environment.getExternalStorageDirectory() + "/Android/data/com.saavn.android/songs/curr.mp4").exists())
            return new File(Environment.getExternalStorageDirectory() + "/Android/data/com.saavn.android/songs/curr.mp4");
        return new File(Environment.getExternalStorageDirectory() + "/Android/data/com.saavn.android/songs/curr.mp3");
    }

    private File getMusicDirectory() {
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "Saavn Music");
    }

    private SongInfo getSongInfo(StatusBarNotification sbn) throws Exception {
        SongInfo songInfo = new SongInfo();
        RemoteViews remoteView = sbn.getNotification().bigContentView;
        Field field = remoteView.getClass().getDeclaredField("mActions");
        field.setAccessible(true);
        ArrayList<Parcelable> actions = (ArrayList<Parcelable>) field.get(remoteView);
        Parcel name = Parcel.obtain();
        actions.get(8).writeToParcel(name, 0);
        songInfo.setTitle(getString(name));
        Parcel album_artist = Parcel.obtain();
        actions.get(9).writeToParcel(name, 0);
        String[] album_artistArr = getString(album_artist).split("—");
        songInfo.setArtist(album_artistArr[1].trim());
        songInfo.setAlbum(album_artistArr[0].trim());
        songInfo.setImage(sbn.getNotification().largeIcon);
        return songInfo;
    }

    private String getString(Parcel parcel) {
        parcel.setDataPosition(0);
        parcel.readInt();
        parcel.readInt();
        parcel.readString();
        parcel.readInt();
        String returnValue = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel).toString().trim();
        parcel.recycle();
        return returnValue;
    }


}
