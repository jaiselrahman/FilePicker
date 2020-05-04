/*
 *  Copyright (c) 2020, Jaisel Rahman <jaiselrahman@gmail.com>.
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

package com.jaiselrahman.filepicker.loader.dir;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import com.jaiselrahman.filepicker.config.Configurations;
import com.jaiselrahman.filepicker.model.Dir;
import com.jaiselrahman.filepicker.utils.FileUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

class DirLoaderCallback implements LoaderManager.LoaderCallbacks<Cursor> {
    private Context context;
    private DirResultCallback dirResultCallback;
    private Configurations configs;

    DirLoaderCallback(@NonNull Context context,
                      @NonNull DirResultCallback dirResultCallback,
                      @NonNull Configurations configs) {
        this.context = context;
        this.dirResultCallback = dirResultCallback;
        this.configs = configs;
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new DirLoader(context, configs);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            dirResultCallback.onResult(getDirs(data, configs));
        } else {
            dirResultCallback.onResult(getDirsQ(data, configs));
        }
    }

    private static List<Dir> getDirs(Cursor data, Configurations configs) {
        List<Dir> dirs = new ArrayList<>();
        List<String> ignoredPaths = new ArrayList<>();

        if (data.moveToFirst())
            do {
                String path = data.getString(DirLoader.COLUMN_DATA);
                String parent = FileUtils.getParent(path);

                if (!isExcluded(parent, ignoredPaths) && !FileUtils.toIgnoreFolder(path, configs)) {
                    Dir mediaDir = new Dir();
                    mediaDir.setId(data.getInt(DirLoader.COLUMN_BUCKET_ID));
                    mediaDir.setName(data.getString(DirLoader.COLUMN_BUCKET_DISPLAY_NAME));
                    mediaDir.setCount(data.getInt(DirLoader.COLUMN_COUNT));
                    mediaDir.setPreview(getPreview(data));
                    dirs.add(mediaDir);
                } else {
                    ignoredPaths.add(path);
                }
            } while (data.moveToNext());

        return dirs;
    }

    private static List<Dir> getDirsQ(Cursor data, Configurations configs) {
        HashMap<Long, Dir> dirs = new HashMap<>();
        List<String> ignoredPaths = new ArrayList<>();

        if (data.moveToFirst())
            do {
                String path = data.getString(DirLoader.COLUMN_DATA);
                String parent = FileUtils.getParent(path);

                if (!isExcluded(parent, ignoredPaths) && !FileUtils.toIgnoreFolder(path, configs)) {
                    long dirId = data.getInt(DirLoader.COLUMN_BUCKET_ID);

                    Dir mediaDir = dirs.get(dirId);

                    if (mediaDir == null) {
                        mediaDir = new Dir();
                        mediaDir.setId(dirId);
                        mediaDir.setName(data.getString(DirLoader.COLUMN_BUCKET_DISPLAY_NAME));
                        mediaDir.setPreview(getPreview(data));
                        mediaDir.setCount(1);

                        dirs.put(dirId, mediaDir);
                    } else {
                        mediaDir.setCount(mediaDir.getCount() + 1);
                    }
                } else {
                    ignoredPaths.add(path);
                }
            } while (data.moveToNext());

        return new ArrayList<>(dirs.values());
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
    }

    private static Uri getPreview(Cursor cursor) {
        long id = cursor.getLong(DirLoader.COLUMN_ID);
        int mediaType = cursor.getInt(DirLoader.COLUMN_MEDIA_TYPE);

        if (id <= 0) return null;

        Uri contentUri;

        if (mediaType == MEDIA_TYPE_IMAGE) {
            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else if (mediaType == MEDIA_TYPE_AUDIO) {
            contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        } else if (mediaType == MEDIA_TYPE_VIDEO) {
            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        } else {
            return null;
        }

        return ContentUris.withAppendedId(contentUri, id);
    }

    private static boolean isExcluded(String path, List<String> ignoredPaths) {
        for (String p : ignoredPaths) {
            if (path.startsWith(p)) return true;
        }
        return false;
    }
}
