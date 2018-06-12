package com.jaisel.filepicker;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.jaisel.filepicker.activity.FilePickerActivity;
import com.jaisel.filepicker.config.Configurations;
import com.jaisel.filepicker.model.File;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private final static int FILE_REQUEST_CODE = 1;
    private FileListAdapter fileListAdapter;
    private ArrayList<File> files = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RecyclerView recyclerView = findViewById(R.id.file_list);
        fileListAdapter = new FileListAdapter(files);
        recyclerView.setAdapter(fileListAdapter);

        findViewById(R.id.launch_filepicker).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, FilePickerActivity.class);
                intent.putExtra(FilePickerActivity.CONFIGS, new Configurations.Builder()
                        .setCheckPermission(true)
                        .setSelectedFiles(files)
                        .setShowImages(true)
                        .enableImageCapture(true)
                        .setShowVideos(false)
                        .setSkipZeroSizeFiles(true)
                        .build());
                startActivityForResult(intent, FILE_REQUEST_CODE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_REQUEST_CODE
                && resultCode == RESULT_OK
                && data != null) {
            files.clear();
            files.addAll(data.<File>getParcelableArrayListExtra(FilePickerActivity.FILES));
            fileListAdapter.notifyDataSetChanged();
        }
    }
}
