package com.example.aman.saavnfucker;

import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;

/**
 * Created by Aman on 09-10-2016.
 */

public class SongInfo {
    @Override
    public String toString() {
        return "title : " + title + " artist : " + artist + " album : " + album;
    }

    public String getFileName() {
        return title.trim().replace(" ", "_") + "_" + album.trim().replace(" ", "_") + ".mp3".trim();
    }

    private String title, album, artist;
    private Bitmap image;
    private int year;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public Bitmap getImage() {
        return image;
    }

    public void setImage(Bitmap image) {
        this.image = image;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public byte[] getImageBytes() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        return stream.toByteArray();
    }
}
