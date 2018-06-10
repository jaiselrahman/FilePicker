package com.jaisel.filepicker.activity;

import android.Manifest;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.jaisel.filepicker.R;
import com.jaisel.filepicker.loader.FileLoader;
import com.jaisel.filepicker.model.File;

import java.util.ArrayList;

import static android.provider.BaseColumns._ID;
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

public class FilePickerActivity extends AppCompatActivity {
    public static final String FILES = "FILES";

    private static final String TAG = "FilePicker";
    private static final int REQUEST_PERMISSION = 1;

    public final String[] permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};

    private ArrayList<File> files = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filepicker);

        if (savedInstanceState == null) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    Log.d(TAG, "onCreate: show request permission reationale");
                } else {
                    ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION);
                }
            } else {
                loadFiles();
            }
        }
    }

    private void loadFiles() {
        getLoaderManager().initLoader(0, null, new LoaderManager.LoaderCallbacks<Cursor>() {
            @Override
            public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                return new FileLoader(FilePickerActivity.this);
            }

            @Override
            public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
                data.moveToFirst();
                do {
                    File file = new File();
                    file.setId(data.getLong(data.getColumnIndex(_ID)));
                    file.setName(data.getString(data.getColumnIndex(TITLE)));
                    file.setPath(data.getString(data.getColumnIndex(DATA)));
                    file.setSize(data.getLong(data.getColumnIndex(SIZE)));
                    file.setDate(data.getLong(data.getColumnIndex(DATE_ADDED)));
                    file.setMimeType(data.getString(data.getColumnIndex(MIME_TYPE)));
                    file.setMediaType(data.getInt(data.getColumnIndex(MEDIA_TYPE)));
                    file.setBucketId(data.getString(data.getColumnIndex(BUCKET_ID)));
                    file.setBucketName(data.getString(data.getColumnIndex(BUCKET_DISPLAY_NAME)));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        file.setDuration(data.getLong(data.getColumnIndex(HEIGHT)));
                        file.setDuration(data.getLong(data.getColumnIndex(WIDTH)));
                    }
                    file.setDuration(data.getLong(data.getColumnIndex(DURATION)));
                    files.add(file);
                } while (data.moveToNext());
                Intent intent = new Intent();
                intent.putParcelableArrayListExtra(FILES, files);
                setResult(RESULT_OK, intent);
                finish();
            }

            @Override
            public void onLoaderReset(Loader<Cursor> loader) {

            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadFiles();
            }
        }
    }
}
