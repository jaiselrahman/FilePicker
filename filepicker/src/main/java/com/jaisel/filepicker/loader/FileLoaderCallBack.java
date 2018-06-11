package com.jaisel.filepicker.loader;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.jaisel.filepicker.model.File;

import java.util.ArrayList;

import static android.provider.BaseColumns._ID;
import static android.provider.MediaStore.Audio.AlbumColumns.ALBUM_ID;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE;
import static android.provider.MediaStore.Files.FileColumns.MIME_TYPE;
import static android.provider.MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME;
import static android.provider.MediaStore.Images.ImageColumns.BUCKET_ID;
import static android.provider.MediaStore.MediaColumns.DATA;
import static android.provider.MediaStore.MediaColumns.DATE_ADDED;
import static android.provider.MediaStore.MediaColumns.HEIGHT;
import static android.provider.MediaStore.MediaColumns.SIZE;
import static android.provider.MediaStore.MediaColumns.TITLE;
import static android.provider.MediaStore.MediaColumns.WIDTH;
import static android.provider.MediaStore.Video.VideoColumns.DURATION;

public class FileLoaderCallBack implements LoaderManager.LoaderCallbacks<Cursor> {
    private Context context;
    private FileResultCallback fileResultCallback;
    private String[] suffixes;

    FileLoaderCallBack(@NonNull Context context, @NonNull FileResultCallback fileResultCallback) {
        this(context, fileResultCallback, null);
    }

    FileLoaderCallBack(@NonNull Context context,
                       @NonNull FileResultCallback fileResultCallback,
                       @Nullable String[] suffixes) {
        this.context = context;
        this.fileResultCallback = fileResultCallback;
        this.suffixes = suffixes;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new FileLoader(context, suffixes);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        ArrayList<File> files = new ArrayList<>();
        data.moveToFirst();
        do {
            File file = new File();
            file.setSize(data.getLong(data.getColumnIndex(SIZE)));
            if (file.getSize() <= 0) continue;
            file.setId(data.getLong(data.getColumnIndex(_ID)));
            file.setName(data.getString(data.getColumnIndex(TITLE)));
            file.setPath(data.getString(data.getColumnIndex(DATA)));
            file.setDate(data.getLong(data.getColumnIndex(DATE_ADDED)));
            file.setMimeType(data.getString(data.getColumnIndex(MIME_TYPE)));
            file.setMediaType(data.getInt(data.getColumnIndex(MEDIA_TYPE)));
            file.setBucketId(data.getString(data.getColumnIndex(BUCKET_ID)));
            file.setBucketName(data.getString(data.getColumnIndex(BUCKET_DISPLAY_NAME)));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                file.setHeight(data.getLong(data.getColumnIndex(HEIGHT)));
                file.setWidth(data.getLong(data.getColumnIndex(WIDTH)));
            }
            file.setDuration(data.getLong(data.getColumnIndex(DURATION)));
            int albumID = data.getInt(data.getColumnIndex(ALBUM_ID));
            if (albumID > 0) {
                file.setThumbnail(ContentUris
                        .withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumID));
            }
            files.add(file);
        } while (data.moveToNext());
        if (fileResultCallback != null) {
            fileResultCallback.onResult(files);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
}
