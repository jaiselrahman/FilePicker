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

package com.jaisel.filepicker.loader;

import android.content.Context;
import android.content.CursorLoader;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;

import java.util.ArrayList;
import java.util.Arrays;

import static android.provider.MediaStore.MediaColumns.DATA;
import static android.provider.MediaStore.MediaColumns.MIME_TYPE;

public class FileLoader extends CursorLoader {
    private static final ArrayList<String> ImageSelectionArgs = new ArrayList<>();
    private static final ArrayList<String> AudioSelectionArgs = new ArrayList<>();
    private static final ArrayList<String> VideoSelectionArgs = new ArrayList<>();
    private static final ArrayList<String> FileSelectionArgs = new ArrayList<>();
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
        AudioSelectionArgs.addAll(Arrays.asList("audio/mpeg", "audio/mp3", "audio/x-ms-wma"));
        VideoSelectionArgs.addAll(Arrays.asList("video/mpeg", "video/mp4"));
        FileSelectionArgs.addAll(Arrays.asList("%.txt", "%.pdf"));
    }

    private String selection = MIME_TYPE + "=? or " + MIME_TYPE + "=? or " + MIME_TYPE + "=? or " + MIME_TYPE + "=? or "
            + MIME_TYPE + "=? or " + MIME_TYPE + "=? or " + MIME_TYPE + "=? or "
            + MIME_TYPE + "=? or " + MIME_TYPE + "=? or "
            + DATA + " LIKE ? or " + DATA + " LIKE ?";

    public FileLoader(Context context) {
        this(context, null);
    }

    public FileLoader(Context context, @Nullable String[] suffixes) {
        super(context);
        setProjection(FILE_PROJECTION);
        setUri(MediaStore.Files.getContentUri("external"));
        setSortOrder(MediaStore.Files.FileColumns.DATE_ADDED + " DESC");

        ArrayList<String> selectionArgs = new ArrayList<>();
        selectionArgs.addAll(ImageSelectionArgs);
        selectionArgs.addAll(AudioSelectionArgs);
        selectionArgs.addAll(VideoSelectionArgs);
        selectionArgs.addAll(FileSelectionArgs);
        if (suffixes != null) {
            StringBuilder selectionBuilder = new StringBuilder(selection);
            for (String suffix : suffixes) {
                selectionBuilder.append(" or ")
                        .append(DATA)
                        .append(" LIKE ?");
                selectionArgs.add("%." + suffix);
            }
            setSelection(selectionBuilder.toString());
        } else {
            setSelection(selection);
        }
        setSelectionArgs(selectionArgs.toArray(new String[]{}));
    }

    public static void loadFiles(FragmentActivity activity, FileResultCallback fileResultCallback) {
        loadFiles(activity, fileResultCallback, null);
    }

    public static void loadFiles(FragmentActivity activity, FileResultCallback fileResultCallback, String[] suffixes) {
        activity.getLoaderManager().initLoader(0, null,
                new FileLoaderCallBack(activity, fileResultCallback, suffixes));
    }
}
