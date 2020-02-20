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

package com.jaiselrahman.filepicker.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jaiselrahman.filepicker.R;
import com.jaiselrahman.filepicker.adapter.FileGalleryAdapter;
import com.jaiselrahman.filepicker.adapter.FileGalleryAdapter.OnCameraClickListener;
import com.jaiselrahman.filepicker.adapter.MultiSelectionAdapter.OnSelectionListener;
import com.jaiselrahman.filepicker.config.Configurations;
import com.jaiselrahman.filepicker.loader.FileLoader;
import com.jaiselrahman.filepicker.loader.FileResultCallback;
import com.jaiselrahman.filepicker.model.MediaFile;
import com.jaiselrahman.filepicker.view.DividerItemDecoration;

import java.io.File;
import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class FilePickerActivity extends AppCompatActivity implements OnSelectionListener<FileGalleryAdapter.ViewHolder>, OnCameraClickListener {

    public static final String MEDIA_FILES = "MEDIA_FILES";
    public static final String SELECTED_MEDIA_FILES = "SELECTED_MEDIA_FILES";
    public static final String CONFIGS = "CONFIGS";
    public static final String TAG = "FilePicker";
    private static final String PATH = "PATH";
    private static final String URI = "URI";
    private static final int REQUEST_WRITE_PERMISSION = 1;
    private static final int REQUEST_CAMERA_PERMISSION_FOR_CAMERA = 2;
    private static final int REQUEST_CAMERA_PERMISSION_FOR_VIDEO = 3;
    private static final int REQUEST_DOCUMENT = 4;
    private Configurations configs;
    private ArrayList<MediaFile> mediaFiles = new ArrayList<>();
    private FileGalleryAdapter fileGalleryAdapter;
    private int maxCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        configs = getIntent().getParcelableExtra(CONFIGS);
        if (configs == null) {
            configs = new Configurations.Builder().build();
        }

        if (useDocumentUi()) {
            MimeTypeMap mimeType = MimeTypeMap.getSingleton();
            String[] suffixes = configs.getSuffixes();
            String[] mime = new String[suffixes.length];
            for (int i = 0; i < suffixes.length; i++) {
                mime[i] = mimeType.getMimeTypeFromExtension(
                        suffixes[i].replace(".", "")
                );
            }

            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                    .setType("*/*")
                    .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, configs.getMaxSelection() > 1)
                    .putExtra(Intent.EXTRA_MIME_TYPES, mime);
            startActivityForResult(intent, REQUEST_DOCUMENT);
            return;
        }

        setContentView(R.layout.filepicker_gallery);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        int spanCount;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            spanCount = configs.getLandscapeSpanCount();
        } else {
            spanCount = configs.getPortraitSpanCount();
        }

        int imageSize = configs.getImageSize();
        if (imageSize <= 0) {
            Point point = new Point();
            getWindowManager().getDefaultDisplay().getSize(point);
            imageSize = Math.min(point.x, point.y) / configs.getPortraitSpanCount();
        }

        boolean isSingleChoice = configs.isSingleChoiceMode();

        fileGalleryAdapter = new FileGalleryAdapter(this, mediaFiles, imageSize,
                configs.isImageCaptureEnabled(),
                configs.isVideoCaptureEnabled());
        fileGalleryAdapter.enableSelection(true);
        fileGalleryAdapter.enableSingleClickSelection(configs.isSingleClickSelection());
        fileGalleryAdapter.setOnSelectionListener(this);
        fileGalleryAdapter.setSingleChoiceMode(isSingleChoice);
        fileGalleryAdapter.setMaxSelection(isSingleChoice ? 1 : configs.getMaxSelection());
        fileGalleryAdapter.setSelectedItems(configs.getSelectedMediaFiles());
        fileGalleryAdapter.setOnCameraClickListener(this);
        RecyclerView recyclerView = findViewById(R.id.file_gallery);
        recyclerView.setLayoutManager(new GridLayoutManager(this, spanCount));
        recyclerView.setAdapter(fileGalleryAdapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(getResources().getDimensionPixelSize(R.dimen.grid_spacing), spanCount));
        recyclerView.setItemAnimator(null);

        maxCount = configs.getMaxSelection();
        if (maxCount > 0) {
            setTitle(getResources().getString(R.string.selection_count, fileGalleryAdapter.getSelectedItemCount(), maxCount));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (requestPermission(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_WRITE_PERMISSION)) {
            loadFiles();
        }
    }

    private boolean useDocumentUi() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && configs.isShowFiles()
                && !(configs.isShowImages() || configs.isShowVideos() || configs.isShowAudios());
    }

    private void loadFiles() {
        FileLoader fileLoader = new FileLoader(this, configs);
        fileLoader.loadFiles(new Consumer<ArrayList<MediaFile>>() {
            @Override
            public void accept(ArrayList<MediaFile> mediaFiles) {
                FilePickerActivity.this.mediaFiles.clear();
                FilePickerActivity.this.mediaFiles.addAll(mediaFiles);
                fileGalleryAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {

            case REQUEST_WRITE_PERMISSION:
                if (grantResults.length > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        loadFiles();
                    } else {
                        Toast.makeText(this, R.string.permission_not_given, Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
                return;

            case REQUEST_CAMERA_PERMISSION_FOR_CAMERA:
            case REQUEST_CAMERA_PERMISSION_FOR_VIDEO:
                if (grantResults.length > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        fileGalleryAdapter.openCamera(requestCode == REQUEST_CAMERA_PERMISSION_FOR_VIDEO);
                    } else {
                        Toast.makeText(this, R.string.permission_not_given, Toast.LENGTH_SHORT).show();
                    }
                }
                return;

            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @SuppressLint("NewApi")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FileGalleryAdapter.CAPTURE_IMAGE_VIDEO) {
            File file = fileGalleryAdapter.getLastCapturedFile();
            if (resultCode == RESULT_OK) {
                MediaScannerConnection.scanFile(this, new String[]{file.getAbsolutePath()}, null,
                        new MediaScannerConnection.OnScanCompletedListener() {
                            @Override
                            public void onScanCompleted(String path, final Uri uri) {
                                if (uri != null) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            loadFiles();
                                        }
                                    });
                                }
                            }
                        });
            } else {
                getContentResolver().delete(fileGalleryAdapter.getLastCapturedUri(), null, null);
            }
        } else if (requestCode == REQUEST_DOCUMENT) {
            ContentResolver contentResolver = getContentResolver();
            ArrayList<MediaFile> mediaFiles = new ArrayList<>();
            if (data == null) {
                finish();
                return;
            }
            Uri uri = data.getData();
            if (uri == null) {
                ClipData clipData = data.getClipData();
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    uri = clipData.getItemAt(i).getUri();
                    mediaFiles.add(FileLoader.asMediaFile(contentResolver, uri, configs));
                }
            } else {
                mediaFiles.add(FileLoader.asMediaFile(contentResolver, uri, configs));
            }
            Intent intent = new Intent();
            intent.putExtra(MEDIA_FILES, mediaFiles);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.filegallery_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.done) {
            Intent intent = new Intent();
            intent.putExtra(MEDIA_FILES, fileGalleryAdapter.getSelectedItems());
            setResult(RESULT_OK, intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (useDocumentUi()) return;
        File file = fileGalleryAdapter.getLastCapturedFile();
        if (file != null)
            outState.putString(PATH, file.getAbsolutePath());
        outState.putParcelable(URI, fileGalleryAdapter.getLastCapturedUri());
        outState.putParcelableArrayList(SELECTED_MEDIA_FILES, fileGalleryAdapter.getSelectedItems());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (useDocumentUi()) return;
        String path = savedInstanceState.getString(PATH);
        if (path != null)
            fileGalleryAdapter.setLastCapturedFile(new File(path));

        Uri uri = savedInstanceState.getParcelable(URI);
        if (uri != null)
            fileGalleryAdapter.setLastCapturedUri(uri);

        ArrayList<MediaFile> mediaFiles = savedInstanceState.getParcelableArrayList(SELECTED_MEDIA_FILES);
        if (mediaFiles != null) {
            fileGalleryAdapter.setSelectedItems(mediaFiles);
            fileGalleryAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onCameraClick(boolean forVideo) {
        return requestPermission(
                new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                forVideo ? REQUEST_CAMERA_PERMISSION_FOR_VIDEO : REQUEST_CAMERA_PERMISSION_FOR_CAMERA
        );
    }

    public boolean requestPermission(String[] permissions, int requestCode) {
        int checkResult = PackageManager.PERMISSION_GRANTED;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                checkResult = PackageManager.PERMISSION_DENIED;
                break;
            }
        }
        if (checkResult != PackageManager.PERMISSION_GRANTED) {
            if (configs.isCheckPermission()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(permissions, requestCode);
                }
            } else {
                Toast.makeText(this, R.string.permission_not_given, Toast.LENGTH_SHORT).show();
                finish();
            }
            return false;
        }
        return true;
    }

    @Override
    public void onSelectionBegin() {

    }

    @Override
    public void onSelected(FileGalleryAdapter.ViewHolder viewHolder, int position) {
        if (maxCount > 0) {
            setTitle(getResources().getString(R.string.selection_count, fileGalleryAdapter.getSelectedItemCount(), maxCount));
        }
    }

    @Override
    public void onUnSelected(FileGalleryAdapter.ViewHolder viewHolder, int position) {
        if (maxCount > 0) {
            setTitle(getResources().getString(R.string.selection_count, fileGalleryAdapter.getSelectedItemCount(), maxCount));
        }
    }

    @Override
    public void onSelectAll() {

    }

    @Override
    public void onUnSelectAll() {

    }

    @Override
    public void onSelectionEnd() {

    }

    @Override
    public void onMaxReached() {
    }
}
