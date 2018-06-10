package com.jaisel.filepicker.loader;

import android.content.Context;
import android.content.CursorLoader;
import android.provider.MediaStore;

import static android.provider.MediaStore.MediaColumns.DATA;
import static android.provider.MediaStore.MediaColumns.MIME_TYPE;

public class FileLoader extends CursorLoader {
    private static final String[] FILE_PROJECTION = {
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.TITLE,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Files.FileColumns.HEIGHT,
            MediaStore.Files.FileColumns.WIDTH,
            MediaStore.Video.Media.DURATION,
    };

    public FileLoader(Context context) {
        super(context);
        setProjection(FILE_PROJECTION);
        setUri(MediaStore.Files.getContentUri("external"));
        setSortOrder(MediaStore.Files.FileColumns.DATE_ADDED + " DESC");

        setSelection(MIME_TYPE + "=? or " + MIME_TYPE + "=? or " + MIME_TYPE + "=? or " + MIME_TYPE + "=? or "
                + MIME_TYPE + "=? or " + MIME_TYPE + "=? or "
                + MIME_TYPE + "=? or " + MIME_TYPE + "=? or " + MIME_TYPE + "=? or "
                + DATA + " LIKE ? or " + DATA + " LIKE ?");

        String[] selectionArgs = new String[]{"image/jpeg", "image/png", "image/jpg", "image/gif",
                "video/mpeg", "video/mp4",
                "audio/mpeg", "audio/mp3", "audio/x-ms-wma",
                "%.txt", "%.pdf"};
        setSelectionArgs(selectionArgs);
    }
}
