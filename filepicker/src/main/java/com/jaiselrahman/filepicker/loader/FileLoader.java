/*
 *  Copyright (c) 2018, Jaisel Rahman <jaiselrahman@gmail.com>.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.jaiselrahman.filepicker.loader;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


import com.jaiselrahman.filepicker.config.Configurations;
import com.jaiselrahman.filepicker.model.MediaFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.provider.BaseColumns._ID;
import static android.provider.MediaStore.Audio.AlbumColumns.ALBUM_ID;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE;
import static android.provider.MediaStore.Files.FileColumns.MIME_TYPE;
import static android.provider.MediaStore.MediaColumns.BUCKET_DISPLAY_NAME;
import static android.provider.MediaStore.MediaColumns.BUCKET_ID;
import static android.provider.MediaStore.MediaColumns.DATA;
import static android.provider.MediaStore.MediaColumns.DATE_ADDED;
import static android.provider.MediaStore.MediaColumns.DISPLAY_NAME;
import static android.provider.MediaStore.MediaColumns.DURATION;
import static android.provider.MediaStore.MediaColumns.HEIGHT;
import static android.provider.MediaStore.MediaColumns.SIZE;
import static android.provider.MediaStore.MediaColumns.WIDTH;

public class FileLoader {
    private static final List<String> FILE_PROJECTION = Arrays.asList(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Files.FileColumns.HEIGHT,
            MediaStore.Files.FileColumns.WIDTH,
            MediaStore.Video.Media.DURATION
    );

    public static Uri getContentUri(Configurations configs) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                (configs.isShowAudios() && !(configs.isShowFiles() || configs.isShowImages() || configs.isShowVideos()))) {
            return MediaStore.Audio.Media.getContentUri("external");
        } else {
            return MediaStore.Files.getContentUri("external");
        }
    }

    public static List<MediaFile> asMediaFiles(Cursor data, Configurations configs) {
        ArrayList<MediaFile> mediaFiles = new ArrayList<>(data.getCount());
        if (data.moveToFirst())
            do {
                MediaFile mediaFile = asMediaFile(data, configs, null);
                if (mediaFile != null) {
                    mediaFiles.add(mediaFile);
                }
            } while (data.moveToNext());
        return mediaFiles;
    }

    public static MediaFile asMediaFile(@NonNull Cursor data, Configurations configs, @Nullable Uri uri) {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setPath(data.getString(data.getColumnIndex(DATA)));

        long size = data.getLong(data.getColumnIndex(SIZE));
        //noinspection deprecation
        if (size == 0 && mediaFile.getPath() != null) {
            //Check if File size is really zero
            size = new java.io.File(data.getString(data.getColumnIndex(DATA))).length();
            if (size <= 0 && configs.isSkipZeroSizeFiles())
                return null;
        }
        mediaFile.setSize(size);

        mediaFile.setId(data.getLong(data.getColumnIndex(_ID)));
        mediaFile.setName(data.getString(data.getColumnIndex(DISPLAY_NAME)));
        mediaFile.setPath(data.getString(data.getColumnIndex(DATA)));
        mediaFile.setDate(data.getLong(data.getColumnIndex(DATE_ADDED)));
        mediaFile.setMimeType(data.getString(data.getColumnIndex(MIME_TYPE)));
        mediaFile.setBucketId(data.getString(data.getColumnIndex(BUCKET_ID)));
        mediaFile.setBucketName(data.getString(data.getColumnIndex(BUCKET_DISPLAY_NAME)));
        mediaFile.setUri(uri != null ? uri : ContentUris.withAppendedId(getContentUri(configs), mediaFile.getId()));
        mediaFile.setDuration(data.getLong(data.getColumnIndex(DURATION)));

        if (TextUtils.isEmpty(mediaFile.getName())) {
            //noinspection deprecation
            String path = mediaFile.getPath() != null ? mediaFile.getPath() : "";
            mediaFile.setName(path.substring(path.lastIndexOf('/') + 1));
        }

        int mediaTypeIndex = data.getColumnIndex(MEDIA_TYPE);
        if (mediaTypeIndex >= 0) {
            mediaFile.setMediaType(data.getInt(mediaTypeIndex));
        }

        if ((mediaFile.getMediaType() == MediaFile.TYPE_FILE
                || mediaFile.getMediaType() > MediaFile.TYPE_MAX)
                && mediaFile.getMimeType() != null) {
            //Double check correct MediaType
            mediaFile.setMediaType(getMediaType(mediaFile.getMimeType()));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mediaFile.setHeight(data.getLong(data.getColumnIndex(HEIGHT)));
            mediaFile.setWidth(data.getLong(data.getColumnIndex(WIDTH)));
        }

        int albumIdIndex = data.getColumnIndex(ALBUM_ID);
        if (albumIdIndex >= 0) {
            int albumId = data.getInt(albumIdIndex);
            if (albumId >= 0) {
                mediaFile.setThumbnail(ContentUris
                        .withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId));
            }
        }
        return mediaFile;
    }

    @Nullable
    public static MediaFile asMediaFile(ContentResolver contentResolver, Uri uri, Configurations configs) {
        Cursor data = contentResolver.query(uri, FILE_PROJECTION.toArray(new String[0]), null, null, null);
        if (data != null && data.moveToFirst()) {
            return asMediaFile(data, configs, uri);
        }
        return null;
    }

    private static @MediaFile.Type
    int getMediaType(String mime) {
        if (mime.startsWith("image/")) {
            return MediaFile.TYPE_IMAGE;
        } else if (mime.startsWith("video/")) {
            return MediaFile.TYPE_VIDEO;
        } else if (mime.startsWith("audio/")) {
            return MediaFile.TYPE_AUDIO;
        } else {
            return MediaFile.TYPE_FILE;
        }
    }
}
