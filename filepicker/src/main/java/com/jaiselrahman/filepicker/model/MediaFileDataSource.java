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

package com.jaiselrahman.filepicker.model;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContentResolverCompat;
import androidx.paging.DataSource;
import androidx.paging.PositionalDataSource;

import com.jaiselrahman.filepicker.activity.FilePickerActivity;
import com.jaiselrahman.filepicker.config.Configurations;
import com.jaiselrahman.filepicker.loader.FileLoader;
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
import static android.provider.MediaStore.MediaColumns.BUCKET_ID;
import static android.provider.MediaStore.MediaColumns.DATA;
import static android.provider.MediaStore.MediaColumns.DATE_ADDED;
import static android.provider.MediaStore.MediaColumns.DISPLAY_NAME;
import static android.provider.MediaStore.MediaColumns.SIZE;

public class MediaFileDataSource extends PositionalDataSource<MediaFile> {

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
    private ContentResolver contentResolver;

    private String[] projection;
    private String sortOrder;
    private String selection;
    private String[] selectionArgs;
    private Uri uri;

    private MediaFileDataSource(ContentResolver contentResolver, Uri uri, @NonNull Configurations configs, Long dirId) {
        this.contentResolver = contentResolver;
        this.configs = configs;
        this.uri = uri;

        StringBuilder selectionBuilder = new StringBuilder(100);

        ArrayList<String> selectionArgs = new ArrayList<>();

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

        List<String> folders = getFoldersToIgnore(contentResolver, configs);
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

        this.projection = projection.toArray(new String[0]);
        this.sortOrder = DATE_ADDED + " DESC";
        this.selection = selectionBuilder.toString();
        this.selectionArgs = selectionArgs.toArray(new String[0]);
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams params, @NonNull LoadInitialCallback<MediaFile> callback) {
        callback.onResult(getMediaFiles(params.requestedStartPosition, params.requestedLoadSize), 0);
    }

    @Override
    public void loadRange(@NonNull LoadRangeParams params, @NonNull LoadRangeCallback<MediaFile> callback) {
        callback.onResult(getMediaFiles(params.startPosition, params.loadSize));
    }

    private List<MediaFile> getMediaFiles(int offset, int limit) {
        Cursor data = ContentResolverCompat.query(contentResolver, uri, projection,
                selection, selectionArgs,
                sortOrder + " LIMIT " + limit + " OFFSET " + offset, null);

        return FileLoader.asMediaFiles(data, configs);
    }

    private static boolean canUseAlbumId(Configurations configs) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                (configs.isShowAudios() && !(configs.isShowFiles() || configs.isShowImages() || configs.isShowVideos()));
    }

    private static boolean canUseMediaType(Configurations configs) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                (!configs.isShowAudios() && (configs.isShowFiles() || configs.isShowImages() || configs.isShowVideos()));
    }

    @NonNull
    private static List<String> getFoldersToIgnore(ContentResolver contentResolver, Configurations configs) {
        Uri uri = MediaStore.Files.getContentUri("external");

        String[] projection = new String[]{DATA};

        String selection;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            selection = BUCKET_ID + " IS NOT NULL) GROUP BY (" + BUCKET_ID;
        } else {
            selection = BUCKET_ID + " IS NOT NULL";
        }

        String sortOrder = DATA + " ASC";

        Cursor cursor = ContentResolverCompat.query(
                contentResolver, uri, projection, selection,
                null, sortOrder, null
        );
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

    private static void appendDefaultFileSelection(StringBuilder selection) {
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

    private static void appendFileSelection(StringBuilder selectionBuilder, List<String> selectionArgs, String[] suffixes) {
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

    private static boolean isExcluded(String path, List<String> ignoredPaths) {
        for (String p : ignoredPaths) {
            if (path.startsWith(p)) return true;
        }
        return false;
    }

    public static class Factory extends DataSource.Factory<Integer, MediaFile> {
        private ContentResolver contentResolver;
        private Configurations configs;
        private Long dirId;

        private Uri uri;

        public Factory(ContentResolver contentResolver, Configurations configs, Long dirId) {
            this.contentResolver = contentResolver;
            this.configs = configs;
            this.dirId = dirId;

            uri = FileLoader.getContentUri(configs);
        }

        public Uri getUri() {
            return uri;
        }

        @NonNull
        @Override
        public DataSource<Integer, MediaFile> create() {
            return new MediaFileDataSource(contentResolver, uri, configs, dirId);
        }
    }
}
