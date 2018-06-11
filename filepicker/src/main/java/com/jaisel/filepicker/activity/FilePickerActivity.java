package com.jaisel.filepicker.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import com.jaisel.filepicker.R;
import com.jaisel.filepicker.adapter.FileGalleryAdapter;
import com.jaisel.filepicker.loader.FileLoader;
import com.jaisel.filepicker.loader.FileResultCallback;
import com.jaisel.filepicker.model.File;

import java.util.ArrayList;

public class FilePickerActivity extends AppCompatActivity {
    public static final String FILES = "FILES";

    private static final String TAG = "FilePicker";
    private static final int REQUEST_PERMISSION = 1;

    public final String[] permissions = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private ArrayList<File> files = new ArrayList<>();
    private FileGalleryAdapter fileGalleryAdapter;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.filepicker_gallery);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fileGalleryAdapter = new FileGalleryAdapter(this, files);
        recyclerView = findViewById(R.id.file_gallery);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerView.setAdapter(fileGalleryAdapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.HORIZONTAL));

        if (savedInstanceState == null) {
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
                fileGalleryAdapter.notifyDataSetChanged();
            }
        }

    }

    private void loadFiles() {
        FileLoader.loadFiles(this, new FileResultCallback() {
            @Override
            public void onResult(ArrayList<File> filesResults) {
//                Intent intent = new Intent();
//                intent.putExtra(FILES, files);
//                setResult(RESULT_OK, intent);
//                finish();
                files.clear();
                files.addAll(filesResults);
                fileGalleryAdapter.notifyDataSetChanged();

            }
        }, new String[]{"zip", "torrent"});
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(FILES, files);
    }
}
