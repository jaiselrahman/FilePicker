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
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.jaiselrahman.filepicker.config.Configurations;
import com.jaiselrahman.filepicker.utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import static android.provider.MediaStore.Images.ImageColumns.BUCKET_ID;
import static android.provider.MediaStore.MediaColumns.DATA;
import static android.provider.MediaStore.MediaColumns.DATE_ADDED;
import static android.provider.MediaStore.MediaColumns.MIME_TYPE;
import static com.jaiselrahman.filepicker.activity.FilePickerActivity.TAG;

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

    private Configurations configs;

    FileLoader(Context context, @NonNull Configurations configs) {
        super(context);
        this.configs = configs;
        ArrayList<String> selectionArgs = new ArrayList<>();
        StringBuilder selectionBuilder = new StringBuilder();

        String rootPath = configs.getRootPath();
        if (rootPath != null) {
            selectionBuilder.append(DATA).append(" LIKE ?");
            if (!rootPath.endsWith(File.separator)) rootPath += File.separator;
            selectionArgs.add(rootPath + "%");
        }

        if (configs.isShowImages())
            selectionArgs.addAll(ImageSelectionArgs);
        if (configs.isShowAudios())
            selectionArgs.addAll(AudioSelectionArgs);
        if (configs.isShowVideos())
            selectionArgs.addAll(VideoSelectionArgs);

        if (selectionBuilder.length() != 0)
            selectionBuilder.append(" and ");

        selectionBuilder.append("(");

        boolean hasMime = (rootPath == null && !selectionArgs.isEmpty()) || selectionArgs.size() == 0;
        if (hasMime) {
            selectionBuilder.append(MIME_TYPE).append(" = ?");
            int size = selectionArgs.size();
            for (int i = 1; i < size; i++) {
                selectionBuilder.append(" or ").append(MIME_TYPE).append(" = ?");
            }
        }

        String[] suffixes = configs.getSuffixes();
        if (configs.isShowFiles() && suffixes != null && suffixes.length > 0) {
            if (hasMime) {
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

        List<String> folders = getFoldersToIgnore();
        if (folders.size() > 0) {
            selectionBuilder.append(") and (").append(DATA).append(" NOT LIKE ? ");
            selectionArgs.add(folders.get(0) + "%");
            int size = folders.size();
            for (int i = 0; i < size; i++) {
                selectionBuilder.append(" and ").append(DATA).append(" NOT LIKE ? ");
                selectionArgs.add(folders.get(i) + "%");
            }
        }
        selectionBuilder.append(")");

        if (selectionBuilder.length() != 0) {
            setProjection(FILE_PROJECTION);
            setUri(MediaStore.Files.getContentUri("external"));
            setSortOrder(DATE_ADDED + " DESC");
            setSelection(selectionBuilder.toString());
            setSelectionArgs(selectionArgs.toArray(new String[0]));
        }
    }

    private List<String> getFoldersToIgnore() {
        Uri uri = MediaStore.Files.getContentUri("external");
        String[] projection = new String[]{DATA};
        String selection = BUCKET_ID + " IS NOT NULL) GROUP BY (" + BUCKET_ID;
        String sortOrder = DATA + " ASC";
        Cursor cursor = getContext().getContentResolver().query(uri, projection, selection, null, sortOrder);
        if (cursor == null) {
            Log.e(TAG, "IgnoreFolders Cursor NULL");
            return new ArrayList<>();
        }
        ArrayList<String> folders = new ArrayList<>();
        try {
            if (cursor.moveToFirst()) {
                do {
                    String path = cursor.getString(cursor.getColumnIndex(DATA));
                    String parent = FileUtils.getParent(path);
                    if (!isExcluded(parent, folders) && FileUtils.toIgnoreFolder(path, configs)) {
                        folders.add(parent);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        cursor.close();
        return folders;
    }

    private boolean isExcluded(String path, List<String> ignoredPaths) {
        for (String p : ignoredPaths) {
            if (path.startsWith(p)) return true;
        }
        return false;
    }

    public static void loadFiles(FragmentActivity activity, FileResultCallback fileResultCallback, Configurations configs, boolean restart) {
        if (configs.isShowFiles() || configs.isShowVideos() || configs.isShowAudios() || configs.isShowImages()) {
            FileLoaderCallback fileLoaderCallBack = new FileLoaderCallback(activity, fileResultCallback, configs);
            if (!restart) {
                activity.getLoaderManager().initLoader(0, null, fileLoaderCallBack);
            } else {
                activity.getLoaderManager().restartLoader(0, null, fileLoaderCallBack);
            }
        } else {
            fileResultCallback.onResult(null);
        }
    }
}
