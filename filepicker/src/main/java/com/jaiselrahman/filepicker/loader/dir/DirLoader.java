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
import android.database.Cursor;
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
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_NONE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;
import static android.provider.MediaStore.Files.FileColumns.MIME_TYPE;
import static android.provider.MediaStore.Images.ImageColumns.BUCKET_ID;
import static android.provider.MediaStore.MediaColumns.DATA;
import static android.provider.MediaStore.MediaColumns.DATE_ADDED;
import static android.provider.MediaStore.MediaColumns.DISPLAY_NAME;
import static android.provider.MediaStore.MediaColumns.SIZE;

public class DirLoader extends CursorLoader {

    private static final String[] FILE_PROJECTION = new String[]{
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.BUCKET_ID,
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
            MIME_TYPE,
            MEDIA_TYPE
    };

    private Configurations configs;

    DirLoader(Context context, @NonNull Configurations configs) {
        super(context);
        this.configs = configs;
    }

    @Override
    public Cursor loadInBackground() {
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

        return super.loadInBackground();
    }

    private static void appendDefaultFileSelection(StringBuilder selection) {
        selection.append("(")
                .append("(")
                .append(MEDIA_TYPE).append(" = ").append(MEDIA_TYPE_NONE)
                .append(" or ")
                .append(MEDIA_TYPE).append(" > ").append(MEDIA_TYPE_VIDEO)
                .append(")and ")
                .append(MIME_TYPE).append(" <> 'resource/folder'")
                .append(" and ")
                .append(MIME_TYPE).append(" NOT LIKE 'image/%'")
                .append(" and ")
                .append(MIME_TYPE).append(" NOT LIKE 'video/%'")
                .append(" and ")
                .append(MIME_TYPE).append(" NOT LIKE 'audio/%'")
                .append(")");
    }

    private static void appendFileSelection(StringBuilder selectionBuilder, ArrayList<String> selectionArgs, String[] suffixes) {
        selectionBuilder.append("(");
        selectionBuilder.append(DISPLAY_NAME).append(" LIKE ?");
        int size = suffixes.length;
        selectionArgs.add("%." + suffixes[0].replace(".", ""));
        for (int i = 1; i < size; i++) {
            selectionBuilder.append(" or ").append(DISPLAY_NAME).append(" LIKE ?");
            suffixes[i] = suffixes[i].replace(".", "");
            selectionArgs.add("%." + suffixes[i]);
        }
        selectionBuilder.append(")");
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
