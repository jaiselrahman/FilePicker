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

import android.content.Context;
import android.content.CursorLoader;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;

import com.jaiselrahman.filepicker.config.Configurations;

import java.util.ArrayList;
import java.util.Arrays;

import static android.provider.MediaStore.MediaColumns.DATA;
import static android.provider.MediaStore.MediaColumns.MIME_TYPE;

public class FileLoader extends CursorLoader {
    private static final ArrayList<String> ImageSelectionArgs = new ArrayList<>();
    private static final ArrayList<String> AudioSelectionArgs = new ArrayList<>();
    private static final ArrayList<String> VideoSelectionArgs = new ArrayList<>();
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
            MediaStore.Audio.AudioColumns.ALBUM_ID
    };

    static {
        ImageSelectionArgs.addAll(Arrays.asList("image/jpeg", "image/png", "image/jpg", "image/gif"));
        AudioSelectionArgs.addAll(Arrays.asList("audio/mpeg", "audio/mp3", "audio/x-ms-wma", "audio/x-wav", "audio/amr", "audio/3gp"));
        VideoSelectionArgs.addAll(Arrays.asList("video/mpeg", "video/mp4"));
    }

    FileLoader(Context context, @NonNull Configurations configs) {
        super(context);

        ArrayList<String> selectionArgs = new ArrayList<>();
        if (configs.isShowImages())
            selectionArgs.addAll(ImageSelectionArgs);
        if (configs.isShowAudios())
            selectionArgs.addAll(AudioSelectionArgs);
        if (configs.isShowVideos())
            selectionArgs.addAll(VideoSelectionArgs);

        StringBuilder selectionBuilder = null;
        if (!selectionArgs.isEmpty()) {
            selectionBuilder = new StringBuilder();
            selectionBuilder.append(MIME_TYPE).append(" = ?");
            int size = selectionArgs.size();
            for (int i = 1; i < size; i++) {
                selectionBuilder.append(" or ").append(MIME_TYPE).append(" = ?");
            }
        }

        String[] suffixes = configs.getSuffixes();
        if (configs.isShowFiles() && suffixes != null && suffixes.length > 0) {
            if (selectionBuilder == null) {
                selectionBuilder = new StringBuilder();
            } else {
                selectionBuilder.append(" or ");
            }
            selectionBuilder.append(DATA).append(" LIKE ?");
            int size = suffixes.length;
            selectionArgs.add("%." + suffixes[0].replace(".", ""));
            for (int i = 1; i < size; i++) {
                selectionBuilder.append(" or ").append(DATA).append(" LIKE ?");
                suffixes[i] = suffixes[i].replace(".", "");
                selectionArgs.add("%." + suffixes[i]);
            }
        }

        if (selectionBuilder != null) {
            setProjection(FILE_PROJECTION);
            setUri(MediaStore.Files.getContentUri("external"));
            setSortOrder(MediaStore.Files.FileColumns.DATE_ADDED + " DESC");
            setSelection(selectionBuilder.toString());
            setSelectionArgs(selectionArgs.toArray(new String[selectionArgs.size()]));
        }
    }

    public static void loadFiles(FragmentActivity activity, FileResultCallback fileResultCallback, Configurations configs) {
        if (configs.isShowFiles() || configs.isShowVideos() || configs.isShowAudios() || configs.isShowImages())
            activity.getLoaderManager().initLoader(0, null,
                    new FileLoaderCallBack(activity, fileResultCallback, configs));
        else
            fileResultCallback.onResult(null);
    }
}
