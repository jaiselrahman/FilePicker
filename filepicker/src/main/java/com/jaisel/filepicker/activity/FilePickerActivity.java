package com.jaisel.filepicker.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.widget.Toast;

import com.jaisel.filepicker.R;
import com.jaisel.filepicker.adapter.FileGalleryAdapter;
import com.jaisel.filepicker.adapter.MultiSelectionAdapter;
import com.jaisel.filepicker.loader.FileLoader;
import com.jaisel.filepicker.loader.FileResultCallback;
import com.jaisel.filepicker.model.File;

import java.util.ArrayList;

public class FilePickerActivity extends AppCompatActivity
        implements MultiSelectionAdapter.OnSelectionListener<FileGalleryAdapter.ViewHolder> {
    public static final String FILES = "FILES";

    private static final String TAG = "FilePicker";
    private static final int REQUEST_PERMISSION = 1;

    public final String[] permissions = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
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

        fileGalleryAdapter = new FileGalleryAdapter(this, files, true);
        fileGalleryAdapter.enableSingleClickSelection(true);
        fileGalleryAdapter.setOnSelectionListener(this);
        fileGalleryAdapter.setMaxSelection(10);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FileGalleryAdapter.REQUEST_TAKE_PHOTO) {
            if (resultCode == RESULT_OK) {
                Uri uri = Uri.fromFile(fileGalleryAdapter.getLastTakenImageFile());
                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                intent.setData(uri);
                sendBroadcast(intent);
                loadFiles();
            } else {
                getContentResolver().delete(fileGalleryAdapter.getLastTakenImageUri(), null, null);
            }
        }
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
        Toast.makeText(this, "Max Limit Reached", Toast.LENGTH_SHORT).show();
    }
}
