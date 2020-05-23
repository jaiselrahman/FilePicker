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
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jaiselrahman.filepicker.R;
import com.jaiselrahman.filepicker.adapter.DirListAdapter;
import com.jaiselrahman.filepicker.config.Configurations;
import com.jaiselrahman.filepicker.model.Dir;
import com.jaiselrahman.filepicker.model.DirViewModel;
import com.jaiselrahman.filepicker.model.MediaFile;
import com.jaiselrahman.filepicker.model.MediaFileLoader;
import com.jaiselrahman.filepicker.view.DividerItemDecoration;

import java.io.File;
import java.util.ArrayList;

public class DirSelectActivity extends AppCompatActivity implements DirListAdapter.OnCameraClickListener {

    public static final String MEDIA_FILES = "MEDIA_FILES";
    public static final String CONFIGS = "CONFIGS";

    private static final int REQUEST_WRITE_PERMISSION = 1;
    private static final int REQUEST_CAMERA_PERMISSION_FOR_CAMERA = 2;
    private static final int REQUEST_CAMERA_PERMISSION_FOR_VIDEO = 3;
    private static final int REQUEST_DOCUMENT = 4;
    private static final int REQUEST_FILE = 5;

    private Configurations configs;
    private DirListAdapter dirAdapter;
    private DirViewModel viewModel;

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

        if (configs.getTitle() != null)
            setTitle(configs.getTitle());

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

        dirAdapter = new DirListAdapter(this, imageSize,
                configs.isImageCaptureEnabled(),
                configs.isVideoCaptureEnabled());

        dirAdapter.setOnClickListener(new DirListAdapter.OnClickListener() {
            @Override
            public void onClick(Dir dir) {
                Intent intent = new Intent(DirSelectActivity.this, FilePickerActivity.class)
                        .putExtra(FilePickerActivity.CONFIGS, configs)
                        .putExtra(FilePickerActivity.DIR_ID, dir.getId())
                        .putExtra(FilePickerActivity.DIR_TITLE, dir.getName());
                startActivityForResult(intent, REQUEST_FILE);
            }
        });

        dirAdapter.setOnCameraClickListener(this);
        RecyclerView recyclerView = findViewById(R.id.file_gallery);
        recyclerView.setLayoutManager(new GridLayoutManager(this, spanCount) {
            @Override
            public boolean isAutoMeasureEnabled() {
                return false;
            }
        });
        recyclerView.setAdapter(dirAdapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(getResources().getDimensionPixelSize(R.dimen.grid_spacing), spanCount));
        recyclerView.setItemAnimator(null);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);

        if (requestPermission(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_WRITE_PERMISSION)) {
            loadDirs();
        }
    }

    private boolean useDocumentUi() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && configs.isShowFiles()
                && !(configs.isShowImages() || configs.isShowVideos() || configs.isShowAudios());
    }

    private void loadDirs() {
        viewModel = new ViewModelProvider(this, new DirViewModel.Factory(getContentResolver(), configs))
                .get(DirViewModel.class);

        viewModel.dirs.observe(this, new Observer<PagedList<Dir>>() {
            @Override
            public void onChanged(PagedList<Dir> dirs) {
                dirAdapter.submitList(dirs);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_WRITE_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadDirs();
            } else {
                Toast.makeText(this, R.string.permission_not_given, Toast.LENGTH_SHORT).show();
                finish();
            }
        } else if (requestCode == REQUEST_CAMERA_PERMISSION_FOR_CAMERA || requestCode == REQUEST_CAMERA_PERMISSION_FOR_VIDEO) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dirAdapter.openCamera(requestCode == REQUEST_CAMERA_PERMISSION_FOR_VIDEO);
            } else {
                Toast.makeText(this, R.string.permission_not_given, Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @SuppressLint("NewApi")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DirListAdapter.CAPTURE_IMAGE_VIDEO) {
            File file = dirAdapter.getLastCapturedFile();
            if (resultCode == RESULT_OK) {
                MediaScannerConnection.scanFile(this, new String[]{file.getAbsolutePath()}, null,
                        new MediaScannerConnection.OnScanCompletedListener() {
                            @Override
                            public void onScanCompleted(String path, final Uri uri) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        viewModel.refresh();
                                    }
                                });
                            }
                        });
            } else {
                getContentResolver().delete(dirAdapter.getLastCapturedUri(),
                        null, null);
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
                    mediaFiles.add(MediaFileLoader.asMediaFile(contentResolver, uri, configs));
                }
            } else {
                mediaFiles.add(MediaFileLoader.asMediaFile(contentResolver, uri, configs));
            }
            Intent intent = new Intent();
            intent.putExtra(MEDIA_FILES, mediaFiles);
            setResult(RESULT_OK, intent);
            finish();
        } else if (requestCode == REQUEST_FILE) {
            if (resultCode == RESULT_OK) {
                setResult(RESULT_OK, data);
                finish();
            } else if (resultCode == RESULT_CANCELED && data != null) {
                ArrayList<MediaFile> selectedFiles = data.getParcelableArrayListExtra(FilePickerActivity.MEDIA_FILES);
                if (selectedFiles != null) {
                    configs.getSelectedMediaFiles().clear();
                    configs.getSelectedMediaFiles().addAll(selectedFiles);
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
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
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
}
