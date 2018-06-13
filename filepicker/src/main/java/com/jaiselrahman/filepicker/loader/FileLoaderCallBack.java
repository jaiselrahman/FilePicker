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
import android.support.annotation.NonNull;

import com.jaiselrahman.filepicker.config.Configurations;
import com.jaiselrahman.filepicker.model.File;

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

class FileLoaderCallBack implements LoaderManager.LoaderCallbacks<Cursor> {
    private Context context;
    private FileResultCallback fileResultCallback;
    private Configurations configs;

    FileLoaderCallBack(@NonNull Context context,
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
        ArrayList<File> files = new ArrayList<>();
        if (data.moveToFirst())
            do {
                long size = data.getLong(data.getColumnIndex(SIZE));
                if (size == 0) {
                    //Check if File size is really zero
                    size = new java.io.File(data.getString(data.getColumnIndex(DATA))).length();
                    if (size <= 0 && configs.isSkipZeroSizeFiles())
                        continue;
                }
                File file = new File();
                file.setSize(size);
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
        data.close();
        fileResultCallback.onResult(files);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
}
