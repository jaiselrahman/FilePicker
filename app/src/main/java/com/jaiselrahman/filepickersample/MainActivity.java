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

package com.jaiselrahman.filepickersample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.jaiselrahman.filepicker.activity.FilePickerActivity;
import com.jaiselrahman.filepicker.config.Configurations;
import com.jaiselrahman.filepicker.model.MediaFile;
import com.jaiselrahman.filepicker.utils.FilePickerProvider;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private final static int FILE_REQUEST_CODE = 1;
    private FileListAdapter fileListAdapter;
    private ArrayList<MediaFile> mediaFiles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RecyclerView recyclerView = findViewById(R.id.file_list);
        fileListAdapter = new FileListAdapter(mediaFiles);
        recyclerView.setAdapter(fileListAdapter);

        findViewById(R.id.launch_imagePicker).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, FilePickerActivity.class);
                intent.putExtra(FilePickerActivity.CONFIGS, new Configurations.Builder()
                        .setCheckPermission(true)
                        .setSelectedMediaFiles(mediaFiles)
                        .enableImageCapture(true)
                        .setShowVideos(false)
                        .setSkipZeroSizeFiles(true)
                        .setMaxSelection(10)
                        .build());
                startActivityForResult(intent, FILE_REQUEST_CODE);
            }
        });

        findViewById(R.id.launch_videoPicker).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, FilePickerActivity.class);
                intent.putExtra(FilePickerActivity.CONFIGS, new Configurations.Builder()
                        .setCheckPermission(true)
                        .setSelectedMediaFiles(mediaFiles)
                        .enableVideoCapture(true)
                        .setShowImages(false)
                        .setMaxSelection(10)
                        .build());
                startActivityForResult(intent, FILE_REQUEST_CODE);
            }
        });

        findViewById(R.id.launch_audioPicker).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, FilePickerActivity.class);
                intent.putExtra(FilePickerActivity.CONFIGS, new Configurations.Builder()
                        .setCheckPermission(true)
                        .setSelectedMediaFiles(mediaFiles)
                        .setShowImages(false)
                        .setShowVideos(false)
                        .setShowAudios(true)
                        .setMaxSelection(10)
                        .build());
                startActivityForResult(intent, FILE_REQUEST_CODE);
            }
        });

        findViewById(R.id.launch_filePicker).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, FilePickerActivity.class);
                intent.putExtra(FilePickerActivity.CONFIGS, new Configurations.Builder()
                        .setCheckPermission(true)
                        .setSelectedMediaFiles(mediaFiles)
                        .setShowFiles(true)
                        .setShowImages(false)
                        .setShowVideos(false)
                        .setSuffixes("pdf", "zip", "tar", "gz", "rar", "7z", "doc", "docx", "ppt", "pptx", "xls", "xlsx")
                        .setMaxSelection(10)
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
            mediaFiles.clear();
            mediaFiles.addAll(data.<MediaFile>getParcelableArrayListExtra(FilePickerActivity.MEDIA_FILES));
            fileListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.share_log) {
            shareLogFile();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void shareLogFile() {
        File logFile = new File(getExternalCacheDir(), "logcat.txt");
        try {
            if (logFile.exists())
                logFile.delete();
            logFile.createNewFile();
            Runtime.getRuntime().exec("logcat -f " + logFile.getAbsolutePath() + " -t 100 *:W Glide:S " + FilePickerActivity.TAG);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (logFile.exists()) {
            Intent intentShareFile = new Intent(Intent.ACTION_SEND);
            intentShareFile.setType("text/plain");
            intentShareFile.putExtra(Intent.EXTRA_STREAM,
                    FilePickerProvider.getUriForFile(this, logFile));
            intentShareFile.putExtra(Intent.EXTRA_SUBJECT, "FilePicker Log File");
            intentShareFile.putExtra(Intent.EXTRA_TEXT, "FilePicker Log File");
            startActivity(Intent.createChooser(intentShareFile, "Share"));
        }
    }
}
