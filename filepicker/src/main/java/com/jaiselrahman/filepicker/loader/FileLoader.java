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

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_NONE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;
import static android.provider.MediaStore.Images.ImageColumns.BUCKET_ID;
import static android.provider.MediaStore.MediaColumns.DATA;
import static android.provider.MediaStore.MediaColumns.DATE_ADDED;
import static android.provider.MediaStore.MediaColumns.DISPLAY_NAME;
import static android.provider.MediaStore.MediaColumns.SIZE;

public class FileLoader extends CursorLoader {
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

    private Configurations configs;
    private StringBuilder selectionBuilder = new StringBuilder(100);
    private ArrayList<String> selectionArgs = new ArrayList<>();

    FileLoader(Context context, @NonNull Configurations configs, Long dirId) {
        super(context);
        this.configs = configs;

        String rootPath = configs.getRootPath();
        if (rootPath != null) {
            selectionBuilder.append(DATA).append(" LIKE ? and ");
            if (!rootPath.endsWith(File.separator)) rootPath += File.separator;
            selectionArgs.add(rootPath + "%");
        }

        if (dirId != null) {
            if (selectionBuilder.length() != 0)
                selectionBuilder.append(" and ");
            selectionBuilder.append(BUCKET_ID).append("=").append(dirId).append(" and ");
        }

        if (canUseMediaType(configs)) {

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
        }

        if (configs.isSkipZeroSizeFiles()) {
            selectionBuilder.append(SIZE).append(" > 0 ");
        }


        List<String> projection = new ArrayList<>(FILE_PROJECTION);

        if (canUseMediaType(configs)) {
            projection.add(MediaStore.Files.FileColumns.MEDIA_TYPE);
        }

        if (canUseAlbumId(configs)) {
            projection.add(MediaStore.Audio.AudioColumns.ALBUM_ID);
        }

        setProjection(projection.toArray(new String[0]));
        setUri(getContentUri(configs));
        setSortOrder(DATE_ADDED + " DESC");
    }

    @Override
    public Cursor loadInBackground() {

        List<String> folders = getFoldersToIgnore();
        if (folders.size() > 0) {
            selectionBuilder.append(" and(").append(DATA).append(" NOT LIKE ? ");
            selectionArgs.add(folders.get(0) + "%");
            int size = folders.size();
            for (int i = 1; i < size; i++) {
                selectionBuilder.append(" and ").append(DATA).append(" NOT LIKE ? ");
                selectionArgs.add(folders.get(i) + "%");
            }
            selectionBuilder.append(")");
        }

        setSelection(selectionBuilder.toString());
        setSelectionArgs(selectionArgs.toArray(new String[0]));
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
            return new ArrayList<>();
        }

        int dataColumnIndex = cursor.getColumnIndex(DATA);

        ArrayList<String> folders = new ArrayList<>();
        try {
            if (cursor.moveToFirst()) {
                do {
                    String path = cursor.getString(dataColumnIndex);
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

    private static boolean isExcluded(String path, List<String> ignoredPaths) {
        for (String p : ignoredPaths) {
            if (path.startsWith(p)) return true;
        }
        return false;
    }

    public static void appendDefaultFileSelection(StringBuilder selection) {
        selection.append("(")
                .append("(")
                .append(MEDIA_TYPE).append(" = ").append(MEDIA_TYPE_NONE)
                .append(" or ")
                .append(MEDIA_TYPE).append(" > ").append(MEDIA_TYPE_VIDEO)
                .append(")and ")
                .append(MediaStore.Files.FileColumns.MIME_TYPE).append(" <> 'resource/folder'")
                .append(" and ")
                .append(MediaStore.Files.FileColumns.MIME_TYPE).append(" NOT LIKE 'image/%'")
                .append(" and ")
                .append(MediaStore.Files.FileColumns.MIME_TYPE).append(" NOT LIKE 'video/%'")
                .append(" and ")
                .append(MediaStore.Files.FileColumns.MIME_TYPE).append(" NOT LIKE 'audio/%'")
                .append(")");
    }

    public static void appendFileSelection(StringBuilder selectionBuilder, ArrayList<String> selectionArgs, String[] suffixes) {
        selectionBuilder.append("(").append(DISPLAY_NAME).append(" LIKE ?");
        selectionArgs.add("%." + suffixes[0].replace(".", ""));

        int size = suffixes.length;
        for (int i = 1; i < size; i++) {
            selectionBuilder.append(" or ").append(DISPLAY_NAME).append(" LIKE ?");
            suffixes[i] = suffixes[i].replace(".", "");
            selectionArgs.add("%." + suffixes[i]);
        }
        selectionBuilder.append(")");
    }

    public static void loadFiles(FragmentActivity activity, FileResultCallback fileResultCallback, Configurations configs, Long dirId, boolean restart) {
        if (configs.isShowFiles() || configs.isShowVideos() || configs.isShowAudios() || configs.isShowImages()) {
            FileLoaderCallback fileLoaderCallBack = new FileLoaderCallback(activity, fileResultCallback, configs, dirId);
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
