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
import java.util.HashSet;
import java.util.List;

import static android.provider.MediaStore.Images.ImageColumns.BUCKET_ID;
import static android.provider.MediaStore.MediaColumns.BUCKET_DISPLAY_NAME;
import static android.provider.MediaStore.MediaColumns.DATA;

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
        ArrayList<Dir> mediaDirs = new ArrayList<>();
        ArrayList<String> ignoredPaths = new ArrayList<>();

        HashSet<Long> dirIds = new HashSet<>(mediaDirs.size() / 8);

        if (data.moveToFirst())
            do {
                String path = data.getString(data.getColumnIndex(DATA));
                String parent = FileUtils.getParent(path);

                long dirId = data.getInt(data.getColumnIndex(BUCKET_ID));

                if (!isExcluded(parent, ignoredPaths) && !FileUtils.toIgnoreFolder(path, configs) &&
                        (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || !dirIds.contains(dirId))) {
                    Dir mediaDir = new Dir();

                    mediaDir.setId(dirId);
                    mediaDir.setName(data.getString(data.getColumnIndex(BUCKET_DISPLAY_NAME)));
                    mediaDir.setPreview(getPreview(data));

                    mediaDirs.add(mediaDir);

                    dirIds.add(dirId);
                } else {
                    ignoredPaths.add(path);
                }
            } while (data.moveToNext());
        dirResultCallback.onResult(mediaDirs);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
    }

    private static Uri getPreview(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID));
        String mimeType = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE));

        if (mimeType == null || id <= 0) return null;

        Uri contentUri;

        if (mimeType.startsWith("image/")) {
            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else if (mimeType.startsWith("audio/")) {
            contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        } else if (mimeType.startsWith("video/")) {
            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        } else {
            contentUri = MediaStore.Files.getContentUri("external");
        }

        return ContentUris.withAppendedId(contentUri, id);
    }

    private boolean isExcluded(String path, List<String> ignoredPaths) {
        for (String p : ignoredPaths) {
            if (path.startsWith(p)) return true;
        }
        return false;
    }
}
