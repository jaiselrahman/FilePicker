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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.jaiselrahman.filepicker.R;
import com.jaiselrahman.filepicker.adapter.FileGalleryAdapter;
import com.jaiselrahman.filepicker.adapter.MultiSelectionAdapter;
import com.jaiselrahman.filepicker.config.Configurations;
import com.jaiselrahman.filepicker.loader.FileLoader;
import com.jaiselrahman.filepicker.loader.FileResultCallback;
import com.jaiselrahman.filepicker.model.MediaFile;
import com.jaiselrahman.filepicker.view.DividerItemDecoration;

import java.io.File;
import java.util.ArrayList;

public class FilePickerActivity extends AppCompatActivity
        implements MultiSelectionAdapter.OnSelectionListener<FileGalleryAdapter.ViewHolder> {
    public static final String MEDIA_FILES = "MEDIA_FILES";
    public static final String CONFIGS = "CONFIGS";
    public static final String TAG = "FilePicker";
    private static final String PATH = "PATH";
    private static final String URI = "URI";
    private static final int REQUEST_PERMISSION = 1;
    public final String[] permissions = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };
    private Configurations configs;
    private ArrayList<MediaFile> mediaFiles = new ArrayList<>();
    private FileGalleryAdapter fileGalleryAdapter;
    private int maxCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.filepicker_gallery);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        configs = getIntent().getParcelableExtra(CONFIGS);
        if (configs == null) {
            configs = new Configurations.Builder().build();
        }

        int spanCount;
        if (getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE) {
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

        fileGalleryAdapter = new FileGalleryAdapter(this, mediaFiles, imageSize,
                configs.isImageCaptureEnabled(),
                configs.isVideoCaptureEnabled());
        fileGalleryAdapter.enableSelection(true);
        fileGalleryAdapter.enableSingleClickSelection(configs.isSingleClickSelection());
        fileGalleryAdapter.setOnSelectionListener(this);
        fileGalleryAdapter.setMaxSelection(configs.getMaxSelection());
        fileGalleryAdapter.setSelectedItems(configs.getSelectedMediaFiles());
        fileGalleryAdapter.setSpanCount(spanCount);
        RecyclerView recyclerView = findViewById(R.id.file_gallery);
        recyclerView.setLayoutManager(new GridLayoutManager(this, spanCount));
        recyclerView.setAdapter(fileGalleryAdapter);
        if (configs.isShowImages() || configs.isShowVideos()) {
            recyclerView.addItemDecoration(new DividerItemDecoration(this));
        }

        if (savedInstanceState == null) {
            if (configs.isCheckPermission()) {
                boolean success = false;
                for (String permission : permissions) {
                    success = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
                }
                if (success)
                    loadFiles(false);
                else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(permissions, REQUEST_PERMISSION);
                }
            }
        } else {
            ArrayList<MediaFile> mediaFiles = savedInstanceState.getParcelableArrayList(MEDIA_FILES);
            if (mediaFiles != null) {
                this.mediaFiles.clear();
                this.mediaFiles.addAll(mediaFiles);
                fileGalleryAdapter.getSelectedItems().clear();
                fileGalleryAdapter.notifyDataSetChanged();
            }
        }

        maxCount = configs.getMaxSelection();
        if (maxCount > 0) {
            setTitle(getResources().getString(R.string.selection_count, fileGalleryAdapter.getSelectedItemCount(), maxCount));
        }
    }

    private void loadFiles(boolean restart) {
        FileLoader.loadFiles(this, new FileResultCallback() {
            @Override
            public void onResult(ArrayList<MediaFile> filesResults) {
                if (filesResults != null) {
                    mediaFiles.clear();
                    mediaFiles.addAll(filesResults);
                    fileGalleryAdapter.notifyDataSetChanged();
                }
            }
        }, configs, restart);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                loadFiles(false);
            }
        }
    }

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
                                            loadFiles(true);
                                        }
                                    });
                                }
                            }
                        });
            } else {
                getContentResolver().delete(fileGalleryAdapter.getLastCapturedUri(),
                        null, null);
            }
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
        File file = fileGalleryAdapter.getLastCapturedFile();
        if (file != null)
            outState.putString(PATH, file.getAbsolutePath());
        outState.putParcelable(URI, fileGalleryAdapter.getLastCapturedUri());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        String path = savedInstanceState.getString(PATH);
        if (path != null)
            fileGalleryAdapter.setLastCapturedFile(new File(path));

        Uri uri = savedInstanceState.getParcelable(URI);
        if (uri != null)
            fileGalleryAdapter.setLastCapturedUri(uri);
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
