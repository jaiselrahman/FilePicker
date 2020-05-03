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

import android.content.Context;
import android.os.Build;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;

import com.jaiselrahman.filepicker.config.Configurations;

import java.io.File;
import java.util.ArrayList;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;
import static android.provider.MediaStore.Files.FileColumns.MIME_TYPE;
import static android.provider.MediaStore.MediaColumns.BUCKET_ID;
import static android.provider.MediaStore.MediaColumns.DATA;
import static android.provider.MediaStore.MediaColumns.DATE_ADDED;
import static android.provider.MediaStore.MediaColumns.SIZE;
import static com.jaiselrahman.filepicker.loader.FileLoader.appendDefaultFileSelection;
import static com.jaiselrahman.filepicker.loader.FileLoader.appendFileSelection;

public class DirLoader extends CursorLoader {

    private static final String[] FILE_PROJECTION = new String[]{
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.BUCKET_ID,
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
            MIME_TYPE,
            MEDIA_TYPE
    };

    DirLoader(Context context, @NonNull Configurations configs) {
        super(context);

        ArrayList<String> selectionArgs = new ArrayList<>();
        StringBuilder selectionBuilder = new StringBuilder(200);

        String rootPath = configs.getRootPath();
        if (rootPath != null) {
            selectionBuilder.append(DATA).append(" LIKE ? and ");
            if (!rootPath.endsWith(File.separator)) rootPath += File.separator;
            selectionArgs.add(rootPath + "%");
        }

        selectionBuilder.append("(");

        if (configs.isShowImages()) {
            if (selectionBuilder.charAt(selectionBuilder.length() - 1) != '(')
                selectionBuilder.append(" or ");
            selectionBuilder.append(MEDIA_TYPE).append(" = ").append(MEDIA_TYPE_IMAGE);
        }

        if (configs.isShowVideos()) {
            if (selectionBuilder.charAt(selectionBuilder.length() - 1) != '(')
                selectionBuilder.append(" or ");
            selectionBuilder.append(MEDIA_TYPE).append(" = ").append(MEDIA_TYPE_VIDEO);
        }

        if (configs.isShowAudios()) {
            if (selectionBuilder.charAt(selectionBuilder.length() - 1) != '(')
                selectionBuilder.append(" or ");
            selectionBuilder.append(MEDIA_TYPE).append(" = ").append(MEDIA_TYPE_AUDIO);
        }

        if (configs.isShowFiles()) {
            if (selectionBuilder.charAt(selectionBuilder.length() - 1) != '(')
                selectionBuilder.append(" or ");

            String[] suffixes = configs.getSuffixes();
            if (suffixes != null && suffixes.length > 0) {
                appendFileSelection(selectionBuilder, selectionArgs, suffixes);
            } else {
                appendDefaultFileSelection(selectionBuilder);
            }
        }

        selectionBuilder.append(") and ");

        if (configs.isSkipZeroSizeFiles()) {
            selectionBuilder.append(SIZE).append(" > 0 ").append(" and ");
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            selectionBuilder.append(BUCKET_ID).append(" IS NOT NULL) GROUP BY (").append(BUCKET_ID);
        } else {
            selectionBuilder.append(BUCKET_ID).append(" IS NOT NULL");
        }

        if (selectionBuilder.length() != 0) {
            setProjection(FILE_PROJECTION);
            setUri(MediaStore.Files.getContentUri("external"));
            setSortOrder(DATE_ADDED + " DESC");
            setSelection(selectionBuilder.toString());
            setSelectionArgs(selectionArgs.toArray(new String[0]));
        }
    }

    public static void loadDirs(FragmentActivity activity, DirResultCallback dirResultCallback, Configurations configs, boolean restart) {
        if (configs.isShowFiles() || configs.isShowVideos() || configs.isShowAudios() || configs.isShowImages()) {
            DirLoaderCallback dirLoaderCallBack = new DirLoaderCallback(activity, dirResultCallback, configs);
            if (!restart) {
                LoaderManager.getInstance(activity).initLoader(0, null, dirLoaderCallBack);
            } else {
                LoaderManager.getInstance(activity).restartLoader(0, null, dirLoaderCallBack);
            }
        } else {
            dirResultCallback.onResult(null);
        }
    }
}
