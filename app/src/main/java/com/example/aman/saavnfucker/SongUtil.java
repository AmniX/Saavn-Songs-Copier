package com.example.aman.saavnfucker;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * Created by Aman on 10-10-2016.
 */

public class SongUtil {
    public static File saveSong(Context context, StatusBarNotification statusBarNotification) throws Exception {
        File folder = getMusicDirectory();
        if (!folder.exists())
            if (folder.mkdir())
                folder.mkdirs();
        File sourceFile = getcurrmp3Path();
        SongInfo songInfo = getSongInfo(statusBarNotification);
        File destination = new File(folder, songInfo.getFileName());
        if (destination.exists()) {
            Toast.makeText(context, "Songs is Already Saved to Saavn Folder!", Toast.LENGTH_LONG).show();
            Logs.d("Songs is Already Saved to Saavn Music Folder!");
            return destination;
        } else {
            copy(sourceFile, destination);
            writeTags(sourceFile, destination, songInfo);
            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(destination)));
            Logs.d(destination.getName() + " Saved to Saavn Music Folder!");
            Toast.makeText(context, destination.getName() + " Saved to Saavn Music Folder!", Toast.LENGTH_LONG).show();
        }
        return destination;
    }

    private static void writeTags(File sourceFile, File destinationFile, SongInfo songInfo) throws InvalidDataException, IOException, UnsupportedTagException, NotSupportedException {
        ID3v2 id3v2Tag;
        Mp3File mp3File = new Mp3File(sourceFile.getAbsoluteFile());
        if (mp3File.hasId3v2Tag())
            id3v2Tag = mp3File.getId3v2Tag();
        else {
            id3v2Tag = new ID3v24Tag();
            mp3File.setId3v2Tag(id3v2Tag);
        }
        mp3File.setId3v2Tag(id3v2Tag);
        Logs.d("Writing Tags with Data - " + songInfo);
        id3v2Tag.setTitle(songInfo.getTitle());
        id3v2Tag.setArtist(songInfo.getArtist());
        id3v2Tag.setAlbum(songInfo.getAlbum());
        id3v2Tag.setAlbumImage(songInfo.getImageBytes(), "image/jpeg");
        mp3File.save(destinationFile.getAbsolutePath());
    }

    private static void copy(File src, File dst) throws IOException {
        Logs.d("Copying Mp3 File");
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0)
            out.write(buf, 0, len);
        in.close();
        out.close();
        out.flush();
        Logs.d("File Copied!");
    }

    public static File getcurrmp3Path() {
        if (new File(Environment.getExternalStorageDirectory() + "/Android/data/com.saavn.android/songs/curr.mp4").exists())
            return new File(Environment.getExternalStorageDirectory() + "/Android/data/com.saavn.android/songs/curr.mp4");
        return new File(Environment.getExternalStorageDirectory() + "/Android/data/com.saavn.android/songs/curr.mp3");
    }

    private static File getMusicDirectory() {
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "Saavn Music");
    }

    private static SongInfo getSongInfo(StatusBarNotification sbn) throws Exception {
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

    private static String getString(Parcel parcel) {
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
