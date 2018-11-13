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

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.jaiselrahman.filepicker.config.Configurations;
import com.jaiselrahman.filepicker.model.MediaFile;
import com.jaiselrahman.filepicker.utils.FileUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Locale;

import static android.provider.BaseColumns._ID;
import static android.provider.MediaStore.Audio.AlbumColumns.ALBUM_ID;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE;
import static android.provider.MediaStore.Files.FileColumns.MIME_TYPE;
import static android.provider.MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME;
import static android.provider.MediaStore.Images.ImageColumns.BUCKET_ID;
import static android.provider.MediaStore.MediaColumns.DATA;
import static android.provider.MediaStore.MediaColumns.DATE_ADDED;
import static android.provider.MediaStore.MediaColumns.HEIGHT;
import static android.provider.MediaStore.MediaColumns.SIZE;
import static android.provider.MediaStore.MediaColumns.TITLE;
import static android.provider.MediaStore.MediaColumns.WIDTH;
import static android.provider.MediaStore.Video.VideoColumns.DURATION;

class FileLoaderCallback implements LoaderManager.LoaderCallbacks<Cursor> {
    private Context context;
    private FileResultCallback fileResultCallback;
    private Configurations configs;

    FileLoaderCallback(@NonNull Context context,
                       @NonNull FileResultCallback fileResultCallback,
                       @NonNull Configurations configs) {
        this.context = context;
        this.fileResultCallback = fileResultCallback;
        this.configs = configs;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new FileLoader(context, configs);
    }

    private int getMediaTypeByMimeType(String path) {
        String mime = FileUtils.getMimeTypeByFullPath(path);
        if (!TextUtils.isEmpty(mime)) {
            if (mime.contains("video"))
                return MediaFile.TYPE_VIDEO;
            if (mime.contains("audio"))
                return MediaFile.TYPE_AUDIO;
            if (mime.contains("image"))
                return MediaFile.TYPE_IMAGE;
        }
        return MediaFile.TYPE_FILE;
    }

    private int getMediaTypeByExtension(String path) {
        String suffix = "";
        int dot = path.lastIndexOf('.');
        if (dot >= 0) {
            suffix = path.substring(dot + 1).toLowerCase(Locale.getDefault());
        }
        switch (suffix) {
            case "jpg":
            case "jpeg":
            case "gif":
            case "png":
            case "bmp":
                return MediaFile.TYPE_IMAGE;
            case "avi":
            case "mp4":
            case "mpeg":
            case "mov":
                return MediaFile.TYPE_VIDEO;
            default:
                return MediaFile.TYPE_FILE;
        }
    }

    private ArrayList<String> hiddenPaths = new ArrayList<>();
    private ArrayList<String> handledPaths = new ArrayList<>();

    /**
     * 文件夹是否包含.nomedia标记
     */
    private boolean isFolderHidden(String path) {
        if (hiddenPaths.contains(path)) {
            return true;
        } else {
            for (String string : hiddenPaths) {
                if (path.contains(string)) {
                    // 父目录已经存在.nomedia标记了
                    return true;
                }
            }
        }
        File file = new File(path);
        if (file.isDirectory()) {
            File[] files = file.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return !TextUtils.isEmpty(name) && name.toLowerCase(Locale.getDefault()).contains(".nomedia");
                }
            });
            if (files.length > 0) {
                hiddenPaths.add(path);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        ArrayList<MediaFile> mediaFiles = new ArrayList<>();
        if (data.moveToFirst())
            do {
                long size = data.getLong(data.getColumnIndex(SIZE));
                String path = data.getString(data.getColumnIndex(DATA));
                if (size == 0) {
                    //Check if File size is really zero
                    size = new File(path).length();
                    if (size <= 0 && configs.isSkipZeroSizeFiles())
                        continue;
                }
                int mediaType = data.getInt(data.getColumnIndex(MEDIA_TYPE));
                int adjustedType = mediaType;
                if (mediaType == MediaFile.TYPE_FILE) {
                    adjustedType = getMediaTypeByMimeType(path);
                    if (adjustedType == MediaFile.TYPE_FILE) {
                        adjustedType = getMediaTypeByExtension(path);
                    }
                }
                long duration = data.getLong(data.getColumnIndex(DURATION));
                if (configs.isSkipHiddenFiles()) {
                    if (path.contains("/.")) {
                        // 父文件夹包含隐藏属性
                        continue;
                    }
                    String parent = path.substring(0, path.lastIndexOf('/'));
                    if (hiddenPaths.contains(parent)) {
                        // 当前文件夹中包含".nomedia"标记
                        continue;
                    }
                    if (!handledPaths.contains(parent)) {
                        if (isFolderHidden(parent)) {
                            continue;
                        } else {
                            // 目录不含有隐藏属性，则将其加入已处理列表，后续不再重复判断目录的隐藏标记
                            handledPaths.add(parent);
                        }
                    }
                    if (duration == 0 && mediaType == MediaFile.TYPE_FILE && adjustedType == MediaFile.TYPE_VIDEO) {
                        // 隐藏的视频文件，时长为0
                        continue;
                    }
                }
                if (mediaType == MediaFile.TYPE_FILE && adjustedType == MediaFile.TYPE_IMAGE) {
                    // 修正图片
                    mediaType = MediaFile.TYPE_IMAGE;
                }
                if (mediaType == MediaFile.TYPE_FILE && adjustedType == MediaFile.TYPE_VIDEO) {
                    // 修正视频
                    mediaType = MediaFile.TYPE_VIDEO;
                }
                MediaFile mediaFile = new MediaFile();
                mediaFile.setSize(size);
                mediaFile.setId(data.getLong(data.getColumnIndex(_ID)));
                mediaFile.setName(data.getString(data.getColumnIndex(TITLE)));
                mediaFile.setPath(path);
                mediaFile.setDate(data.getLong(data.getColumnIndex(DATE_ADDED)));
                mediaFile.setMimeType(data.getString(data.getColumnIndex(MIME_TYPE)));
                mediaFile.setMediaType(mediaType);
                mediaFile.setBucketId(data.getString(data.getColumnIndex(BUCKET_ID)));
                mediaFile.setBucketName(data.getString(data.getColumnIndex(BUCKET_DISPLAY_NAME)));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    mediaFile.setHeight(data.getLong(data.getColumnIndex(HEIGHT)));
                    mediaFile.setWidth(data.getLong(data.getColumnIndex(WIDTH)));
                }
                mediaFile.setDuration(duration);
                int albumID = data.getInt(data.getColumnIndex(ALBUM_ID));
                if (albumID > 0) {
                    mediaFile.setThumbnail(ContentUris
                            .withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumID));
                }
                if (mediaFile.getMediaType() == MediaFile.TYPE_FILE) {
                    // 如果是普通文件，则重置其文件类型
                    if (path.endsWith(".doc") || path.endsWith(".docx")) {
                        mediaFile.setMediaType(MediaFile.TYPE_WORD);
                    } else if (path.endsWith(".xls") || path.endsWith(".xlsx")) {
                        mediaFile.setMediaType(MediaFile.TYPE_EXCEL);
                    } else if (path.endsWith(".ppt") || path.endsWith(".pptx")) {
                        mediaFile.setMediaType(MediaFile.TYPE_PPT);
                    } else if (path.endsWith(".pdf")) {
                        mediaFile.setMediaType(MediaFile.TYPE_PDF);
                    } else if (path.endsWith(".rar") || path.endsWith(".zip") || path.endsWith(".tar") || path.endsWith(".gz") || path.endsWith(".7z")) {
                        mediaFile.setMediaType(MediaFile.TYPE_ZIP);
                    } else if (path.endsWith(".txt") || path.endsWith(".log") || path.endsWith(".txt")) {
                        mediaFile.setMediaType(MediaFile.TYPE_TXT);
                    }
                }
                mediaFiles.add(mediaFile);
            } while (data.moveToNext());
        fileResultCallback.onResult(mediaFiles);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
}
