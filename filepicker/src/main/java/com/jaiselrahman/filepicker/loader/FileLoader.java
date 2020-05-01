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
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;

import com.jaiselrahman.filepicker.config.Configurations;
import com.jaiselrahman.filepicker.model.MediaFile;
import com.jaiselrahman.filepicker.utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.provider.MediaStore.Images.ImageColumns.BUCKET_ID;
import static android.provider.MediaStore.MediaColumns.DATA;
import static android.provider.MediaStore.MediaColumns.DATE_ADDED;
import static android.provider.MediaStore.MediaColumns.MIME_TYPE;
import static com.jaiselrahman.filepicker.activity.FilePickerActivity.TAG;

public class FileLoader extends CursorLoader {
    private static final ArrayList<String> ImageSelectionArgs = new ArrayList<>();
    private static final ArrayList<String> AudioSelectionArgs = new ArrayList<>();
    private static final ArrayList<String> VideoSelectionArgs = new ArrayList<>();
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

    static {
        ImageSelectionArgs.addAll(Arrays.asList("image/jpeg", "image/png", "image/jpg", "image/gif"));
        AudioSelectionArgs.addAll(Arrays.asList("audio/mpeg", "audio/mp3", "audio/x-ms-wma", "audio/x-wav", "audio/amr", "audio/3gp"));
        VideoSelectionArgs.addAll(Arrays.asList("video/mpeg", "video/mp4"));
    }

    private Configurations configs;

    FileLoader(Context context, @NonNull Configurations configs) {
        super(context);
        this.configs = configs;
    }

    @Override
    public Cursor loadInBackground() {
        ArrayList<String> selectionArgs = new ArrayList<>();
        StringBuilder selectionBuilder = new StringBuilder(100);

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

        int mimeSize = rootPath == null ? selectionArgs.size() : selectionArgs.size() - 1;
        if (mimeSize > 0) {
            selectionBuilder.append(MIME_TYPE).append(" = ?");
            for (int i = 1; i < mimeSize; i++) {
                selectionBuilder.append(" or ").append(MIME_TYPE).append(" = ?");
            }
        }

        String[] suffixes = configs.getSuffixes();
        if (configs.isShowFiles() && suffixes != null && suffixes.length > 0) {
            if (mimeSize > 0) {
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

        List<String> projection = new ArrayList<>(FILE_PROJECTION);

        if (selectionBuilder.length() != 0) {
            if (canUseAlbumId(configs)) {
                projection.add(MediaStore.Audio.AudioColumns.ALBUM_ID);
            }
            if (canUseMediaType(configs)) {
                projection.add(MediaStore.Files.FileColumns.MEDIA_TYPE);
            }
            setProjection(projection.toArray(new String[0]));
            setUri(getContentUri(configs));
            setSortOrder(DATE_ADDED + " DESC");
            setSelection(selectionBuilder.toString());
            setSelectionArgs(selectionArgs.toArray(new String[0]));
        }
        return super.loadInBackground();
    }

    static Uri getContentUri(Configurations configs) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                (configs.isShowAudios() && !(configs.isShowFiles() || configs.isShowImages() || configs.isShowVideos()))) {
            return MediaStore.Audio.Media.getContentUri("external");
        } else {
            return MediaStore.Files.getContentUri("external");
        }
    }

    private static boolean canUseAlbumId(Configurations configs) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                (configs.isShowAudios() && !(configs.isShowFiles() || configs.isShowImages() || configs.isShowVideos()));
    }

    private static boolean canUseMediaType(Configurations configs) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                (!configs.isShowAudios() && (configs.isShowFiles() || configs.isShowImages() || configs.isShowVideos()));
    }

    private List<String> getFoldersToIgnore() {
        Uri uri = MediaStore.Files.getContentUri("external");
        String[] projection = new String[]{DATA};
        String selection;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            selection = BUCKET_ID + " IS NOT NULL) GROUP BY (" + BUCKET_ID;
        } else {
            selection = BUCKET_ID + " IS NOT NULL";
        }
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
                LoaderManager.getInstance(activity).initLoader(0, null, fileLoaderCallBack);
            } else {
                LoaderManager.getInstance(activity).restartLoader(0, null, fileLoaderCallBack);
            }
        } else {
            fileResultCallback.onResult(null);
        }
    }

    @Nullable
    public static MediaFile asMediaFile(ContentResolver contentResolver, Uri uri, Configurations configs) {
        Cursor data = contentResolver.query(uri, FILE_PROJECTION.toArray(new String[0]), null, null, null);
        if (data != null && data.moveToFirst()) {
            return FileLoaderCallback.asMediaFile(data, configs, uri);
        }
        return null;
    }
}
