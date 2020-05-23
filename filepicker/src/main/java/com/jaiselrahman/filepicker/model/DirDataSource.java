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

import androidx.annotation.NonNull;
import androidx.core.content.ContentResolverCompat;
import androidx.paging.DataSource;
import androidx.paging.PositionalDataSource;

import com.jaiselrahman.filepicker.config.Configurations;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;
import static android.provider.MediaStore.MediaColumns.BUCKET_ID;
import static android.provider.MediaStore.MediaColumns.DATA;
import static android.provider.MediaStore.MediaColumns.DATE_ADDED;
import static android.provider.MediaStore.MediaColumns.DATE_MODIFIED;
import static android.provider.MediaStore.MediaColumns.DATE_TAKEN;
import static android.provider.MediaStore.MediaColumns.SIZE;
import static com.jaiselrahman.filepicker.model.MediaFileDataSource.appendDefaultFileSelection;
import static com.jaiselrahman.filepicker.model.MediaFileDataSource.appendFileSelection;

public class DirDataSource extends PositionalDataSource<Dir> {
    private Configurations configs;
    private ContentResolver contentResolver;

    private String[] projection;
    private String sortOrder;
    private String selection;
    private String[] selectionArgs;
    private Uri uri;

    private DirDataSource(ContentResolver contentResolver, Uri uri, @NonNull Configurations configs) {
        this.contentResolver = contentResolver;
        this.configs = configs;
        this.uri = uri;

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

        this.projection = getDirProjection();
        this.sortOrder = DATE_ADDED + " DESC";
        this.selection = selectionBuilder.toString();
        this.selectionArgs = selectionArgs.toArray(new String[0]);
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams params, @NonNull LoadInitialCallback<Dir> callback) {
        callback.onResult(getDirs(params.requestedStartPosition, params.requestedLoadSize), 0);
    }

    @Override
    public void loadRange(@NonNull LoadRangeParams params, @NonNull LoadRangeCallback<Dir> callback) {
        callback.onResult(getDirs(params.startPosition, params.loadSize));
    }

    private List<Dir> getDirs(int offset, int limit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return getDirsQ(offset);
        }

        Cursor data = ContentResolverCompat.query(contentResolver, uri, projection,
                selection, selectionArgs,
                sortOrder + " LIMIT " + limit + " OFFSET " + offset, null);

        return DirLoader.getDirs(data, configs);
    }

    private List<Dir> getDirsQ(int offset) {
        if (offset != 0) return Collections.emptyList();

        Cursor data = ContentResolverCompat.query(contentResolver, uri, projection,
                selection, selectionArgs,
                sortOrder, null);

        return DirLoader.getDirs(data, configs);
    }

    private static String[] getDirProjection() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return DirLoader.DIR_PROJECTION;
        }

        List<String> projection = new ArrayList<>();
        Collections.addAll(projection, DirLoader.DIR_PROJECTION);
        projection.add("COUNT(*) as " + DirLoader.COUNT);

        return projection.toArray(new String[0]);
    }

    public static class Factory extends DataSource.Factory<Integer, Dir> {
        private ContentResolver contentResolver;
        private Configurations configs;

        private Uri uri;

        Factory(ContentResolver contentResolver, Configurations configs) {
            this.contentResolver = contentResolver;
            this.configs = configs;

            uri = MediaStore.Files.getContentUri("external");
        }

        public Uri getUri() {
            return uri;
        }

        @NonNull
        @Override
        public DataSource<Integer, Dir> create() {
            return new DirDataSource(contentResolver, uri, configs);
        }
    }
}
