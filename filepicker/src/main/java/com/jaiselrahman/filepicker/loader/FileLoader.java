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
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.doctoror.rxcursorloader.RxCursorLoader;
import com.jaiselrahman.filepicker.config.Configurations;
import com.jaiselrahman.filepicker.model.MediaFile;
import com.jaiselrahman.filepicker.utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.BackpressureStrategy;
import io.reactivex.FlowableSubscriber;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

import static android.provider.BaseColumns._ID;
import static android.provider.MediaStore.Audio.AlbumColumns.ALBUM_ID;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE;
import static android.provider.MediaStore.Images.ImageColumns.BUCKET_ID;
import static android.provider.MediaStore.MediaColumns.BUCKET_DISPLAY_NAME;
import static android.provider.MediaStore.MediaColumns.DATA;
import static android.provider.MediaStore.MediaColumns.DATE_ADDED;
import static android.provider.MediaStore.MediaColumns.DISPLAY_NAME;
import static android.provider.MediaStore.MediaColumns.DURATION;
import static android.provider.MediaStore.MediaColumns.HEIGHT;
import static android.provider.MediaStore.MediaColumns.MIME_TYPE;
import static android.provider.MediaStore.MediaColumns.SIZE;
import static android.provider.MediaStore.MediaColumns.WIDTH;
import static com.jaiselrahman.filepicker.activity.FilePickerActivity.TAG;

public class FileLoader {
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


    private Configurations configs;
    private Context context;
    private CompositeDisposable disposable = new CompositeDisposable();
    private final RxCursorLoader.Query query;

    public FileLoader(Context context, @NonNull Configurations configs) {
        this.context = context;
        this.configs = configs;

        ImageSelectionArgs.addAll(Arrays.asList("image/jpeg", "image/png", "image/jpg", "image/gif"));
        AudioSelectionArgs.addAll(Arrays.asList("audio/mpeg", "audio/mp3", "audio/x-ms-wma", "audio/x-wav", "audio/amr", "audio/3gp"));
        VideoSelectionArgs.addAll(Arrays.asList("video/mpeg", "video/mp4"));

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

        this.query = new RxCursorLoader.Query.Builder()
                .setContentUri(getContentUri(configs))
                .setProjection(FILE_PROJECTION.toArray(new String[0]))
                .setSelection(selectionBuilder.toString().concat(" OFFSET ?"))
                .setSelectionArgs(selectionArgs.toArray(new String[0]))
                .create();

    }

    static Uri getContentUri(Configurations configs) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && (configs.isShowAudios() && !(configs.isShowFiles() || configs.isShowImages() || configs.isShowVideos()))) {
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
        Cursor cursor = context.getContentResolver().query(uri, projection, selection, null, sortOrder);
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

    public void loadFiles(Consumer<ArrayList<MediaFile>> consumer) {

        disposable.add(RxCursorLoader
                .single(context.getContentResolver(), query)
                .map(new Function<Cursor, ArrayList<MediaFile>>() {
                    @Override
                    public ArrayList<MediaFile> apply(Cursor cursor) {
                        ArrayList<MediaFile> mediaFiles = new ArrayList<>();
                        if (cursor.moveToFirst())
                            do {
                                MediaFile mediaFile = asMediaFile(cursor, configs, null);
                                if (mediaFile != null) {
                                    mediaFiles.add(mediaFile);
                                }
                            } while (cursor.moveToNext());

                        mediaFiles.addAll(mediaFiles);
                        return mediaFiles;
                    }
                })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(consumer, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        Log.e("FilePickerError", throwable.getMessage());
                    }
                }));

    }

    @Nullable
    public static MediaFile asMediaFile(ContentResolver contentResolver, Uri uri, Configurations configs) {
        Cursor data = contentResolver.query(uri, FILE_PROJECTION.toArray(new String[0]), null, null, null);
        if (data != null && data.moveToFirst()) {
            return FileLoaderCallback.asMediaFile(data, configs, uri);
        }
        return null;
    }

    private MediaFile asMediaFile(@NonNull Cursor data, Configurations configs, @Nullable Uri uri) {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setPath(data.getString(data.getColumnIndex(DATA)));

        long size = data.getLong(data.getColumnIndex(SIZE));
        //noinspection deprecation
        if (size == 0 && mediaFile.getPath() != null) {
            //Check if File size is really zero
            size = new java.io.File(data.getString(data.getColumnIndex(DATA))).length();
            if (size <= 0 && configs.isSkipZeroSizeFiles())
                return null;
        }
        mediaFile.setSize(size);

        mediaFile.setId(data.getLong(data.getColumnIndex(_ID)));
        mediaFile.setName(data.getString(data.getColumnIndex(DISPLAY_NAME)));
        mediaFile.setPath(data.getString(data.getColumnIndex(DATA)));
        mediaFile.setDate(data.getLong(data.getColumnIndex(DATE_ADDED)));
        mediaFile.setMimeType(data.getString(data.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)));
        mediaFile.setBucketId(data.getString(data.getColumnIndex(BUCKET_ID)));
        mediaFile.setBucketName(data.getString(data.getColumnIndex(BUCKET_DISPLAY_NAME)));
        mediaFile.setUri(uri != null ? uri : ContentUris.withAppendedId(FileLoader.getContentUri(configs), mediaFile.getId()));
        mediaFile.setDuration(data.getLong(data.getColumnIndex(DURATION)));

        if (TextUtils.isEmpty(mediaFile.getName())) {
            //noinspection deprecation
            String path = mediaFile.getPath() != null ? mediaFile.getPath() : "";
            mediaFile.setName(path.substring(path.lastIndexOf('/') + 1));
        }

        int mediaTypeIndex = data.getColumnIndex(MEDIA_TYPE);
        if (mediaTypeIndex >= 0) {
            mediaFile.setMediaType(data.getInt(mediaTypeIndex));
        }

        if ((mediaFile.getMediaType() == MediaFile.TYPE_FILE
                || mediaFile.getMediaType() > MediaFile.TYPE_MAX)
                && mediaFile.getMimeType() != null) {
            //Double check correct MediaType
            mediaFile.setMediaType(getMediaType(mediaFile.getMimeType()));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mediaFile.setHeight(data.getLong(data.getColumnIndex(HEIGHT)));
            mediaFile.setWidth(data.getLong(data.getColumnIndex(WIDTH)));
        }

        int albumIdIndex = data.getColumnIndex(ALBUM_ID);
        if (albumIdIndex >= 0) {
            int albumId = data.getInt(albumIdIndex);
            if (albumId >= 0) {
                mediaFile.setThumbnail(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId));
            }
        }
        return mediaFile;
    }

    private static @MediaFile.Type
    int getMediaType(String mime) {
        if (mime.startsWith("image/")) {
            return MediaFile.TYPE_IMAGE;
        } else if (mime.startsWith("video/")) {
            return MediaFile.TYPE_VIDEO;
        } else if (mime.startsWith("audio/")) {
            return MediaFile.TYPE_AUDIO;
        } else {
            return MediaFile.TYPE_FILE;
        }
    }


}
