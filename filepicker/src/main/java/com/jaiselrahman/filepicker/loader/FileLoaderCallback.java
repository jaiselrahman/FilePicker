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

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;

import com.jaiselrahman.filepicker.config.Configurations;
import com.jaiselrahman.filepicker.model.MediaFile;

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

class FileLoaderCallback implements LoaderManager.LoaderCallbacks<Cursor> {
    private Context context;
    private FileResultCallback fileResultCallback;
    private Configurations configs;

    FileLoaderCallback(@NonNull Context context,
                       @NonNull FileResultCallback fileResultCallback,
                       @NonNull Configurations configs) {
        this.context = context;
        this.fileResultCallback = fileResultCallback;
        this.configs = configs;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new FileLoader(context, configs);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        ArrayList<MediaFile> mediaFiles = new ArrayList<>();
        if (data.moveToFirst())
            do {
                long size = data.getLong(data.getColumnIndex(SIZE));
                if (size == 0) {
                    //Check if File size is really zero
                    size = new java.io.File(data.getString(data.getColumnIndex(DATA))).length();
                    if (size <= 0 && configs.isSkipZeroSizeFiles())
                        continue;
                }
                MediaFile mediaFile = new MediaFile();
                mediaFile.setSize(size);
                mediaFile.setId(data.getLong(data.getColumnIndex(_ID)));
                mediaFile.setName(data.getString(data.getColumnIndex(TITLE)));
                mediaFile.setPath(data.getString(data.getColumnIndex(DATA)));
                mediaFile.setDate(data.getLong(data.getColumnIndex(DATE_ADDED)));
                mediaFile.setMimeType(data.getString(data.getColumnIndex(MIME_TYPE)));
                mediaFile.setBucketId(data.getString(data.getColumnIndex(BUCKET_ID)));
                mediaFile.setBucketName(data.getString(data.getColumnIndex(BUCKET_DISPLAY_NAME)));
                mediaFile.setUri(ContentUris.withAppendedId(FileLoader.getContentUri(configs), mediaFile.getId()));
                mediaFile.setDuration(data.getLong(data.getColumnIndex(DURATION)));

                if (mediaFile.getMediaType() == MediaFile.TYPE_FILE
                        && mediaFile.getMimeType() != null) {
                    //Double check correct MediaType
                    mediaFile.setMediaType(getMediaType(mediaFile.getMimeType()));
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    mediaFile.setHeight(data.getLong(data.getColumnIndex(HEIGHT)));
                    mediaFile.setWidth(data.getLong(data.getColumnIndex(WIDTH)));
                }

                int mediaTypeIndex = data.getColumnIndex(MEDIA_TYPE);
                if (mediaTypeIndex >= 0) {
                    mediaFile.setMediaType(data.getInt(mediaTypeIndex));
                }

                int albumIdIndex = data.getColumnIndex(ALBUM_ID);
                if (albumIdIndex >= 0) {
                    int albumId = data.getInt(albumIdIndex);
                    if(albumId >= 0) {
                        mediaFile.setThumbnail(ContentUris
                                .withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId));
                    }
                }
                mediaFiles.add(mediaFile);
            } while (data.moveToNext());
        fileResultCallback.onResult(mediaFiles);
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

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
}
