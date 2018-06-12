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

package com.jaisel.filepicker.activity;

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
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.jaisel.filepicker.R;
import com.jaisel.filepicker.adapter.FileGalleryAdapter;
import com.jaisel.filepicker.adapter.MultiSelectionAdapter;
import com.jaisel.filepicker.config.Configurations;
import com.jaisel.filepicker.loader.FileLoader;
import com.jaisel.filepicker.loader.FileResultCallback;
import com.jaisel.filepicker.model.File;

import java.util.ArrayList;

public class FilePickerActivity extends AppCompatActivity
        implements MultiSelectionAdapter.OnSelectionListener<FileGalleryAdapter.ViewHolder> {
    public static final String FILES = "FILES";
    public static final String CONFIGS = "CONFIGS";
    private static final String TAG = "FilePickerActivity";
    private static final int REQUEST_PERMISSION = 1;
    public final String[] permissions = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };
    private Configurations configs;
    private ArrayList<File> files = new ArrayList<>();
    private FileGalleryAdapter fileGalleryAdapter;

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

        fileGalleryAdapter = new FileGalleryAdapter(this, files, imageSize,
                configs.isImageCaptureEnabled(),
                configs.isVideoCaptureEnabled());
        fileGalleryAdapter.enableSelection(true);
        fileGalleryAdapter.enableSingleClickSelection(configs.isSingleClickSelection());
        fileGalleryAdapter.setOnSelectionListener(this);
        fileGalleryAdapter.setMaxSelection(configs.getMaxSelection());
        fileGalleryAdapter.setSelectedItems(configs.getSelectedFiles());
        RecyclerView recyclerView = findViewById(R.id.file_gallery);
        recyclerView.setLayoutManager(new GridLayoutManager(this, spanCount));
        recyclerView.setAdapter(fileGalleryAdapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.HORIZONTAL));

        if (configs.isCheckPermission() && savedInstanceState == null) {
            boolean success = false;
            for (String permission : permissions) {
                success = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
            }
            if (success)
                loadFiles();
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(permissions, REQUEST_PERMISSION);
            }
        } else {
            ArrayList<File> files = savedInstanceState.getParcelableArrayList(FILES);
            if (files != null) {
                this.files.clear();
                this.files.addAll(files);
                fileGalleryAdapter.getSelectedItems().clear();
                fileGalleryAdapter.notifyDataSetChanged();
            }
        }
    }

    private void loadFiles() {
        FileLoader.loadFiles(this, new FileResultCallback() {
            @Override
            public void onResult(ArrayList<File> filesResults) {
                if (filesResults != null) {
                    files.clear();
                    files.addAll(filesResults);
                    fileGalleryAdapter.notifyDataSetChanged();
                }
            }
        }, configs);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                loadFiles();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FileGalleryAdapter.CAPTURE_IMAGE_VIDEO) {
            Uri uri = fileGalleryAdapter.getLastCapturedFileUri();
            if (resultCode == RESULT_OK) {
                MediaScannerConnection.scanFile(this, new String[]{uri.getPath()}, null,
                        new MediaScannerConnection.OnScanCompletedListener() {
                            @Override
                            public void onScanCompleted(String path, final Uri uri) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (uri != null) {
                                            loadFiles();
                                        }
                                    }
                                });
                            }
                        });
            } else {
                new java.io.File(uri.getPath()).delete();
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
            intent.putExtra(FILES, fileGalleryAdapter.getSelectedItems());
            setResult(RESULT_OK, intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(FILES, files);
    }

    @Override
    public void onSelectionBegin() {

    }

    @Override
    public void onSelected(FileGalleryAdapter.ViewHolder viewHolder, int position) {

    }

    @Override
    public void onUnSelected(FileGalleryAdapter.ViewHolder viewHolder, int position) {

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
        Toast.makeText(this, "Maximum Limit Reached", Toast.LENGTH_SHORT).show();
    }
}
