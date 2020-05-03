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

package com.jaiselrahman.filepicker.adapter;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.request.RequestOptions;
import com.jaiselrahman.filepicker.R;
import com.jaiselrahman.filepicker.model.Dir;
import com.jaiselrahman.filepicker.utils.FilePickerProvider;
import com.jaiselrahman.filepicker.view.SquareImage;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import static android.os.Environment.DIRECTORY_MOVIES;
import static android.os.Environment.DIRECTORY_PICTURES;
import static android.os.Environment.getExternalStoragePublicDirectory;
import static com.jaiselrahman.filepicker.activity.FilePickerActivity.TAG;

public class DirListAdapter extends RecyclerView.Adapter<DirListAdapter.ViewHolder> {
    public static final int CAPTURE_IMAGE_VIDEO = 1;
    private ArrayList<Dir> mediaDirs;
    private Activity activity;
    private RequestManager glideRequest;
    private OnClickListener onClickListener;
    private OnCameraClickListener onCameraClickListener;
    private boolean showCamera;
    private boolean showVideoCamera;
    private File lastCapturedFile;
    private Uri lastCapturedUri;
    private SimpleDateFormat TimeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());

    public DirListAdapter(Activity activity, ArrayList<Dir> mediaDirs, int imageSize, boolean showCamera, boolean showVideoCamera) {
        this.mediaDirs = mediaDirs;
        this.activity = activity;
        this.showCamera = showCamera;
        this.showVideoCamera = showVideoCamera;
        glideRequest = Glide.with(this.activity)
                .applyDefaultRequestOptions(RequestOptions
                        .sizeMultiplierOf(0.70f)
                        .optionalCenterCrop()
                        .placeholder(R.drawable.ic_dir)
                        .override(imageSize));
    }

    public File getLastCapturedFile() {
        return lastCapturedFile;
    }

    public void setLastCapturedFile(File file) {
        lastCapturedFile = file;
    }

    public Uri getLastCapturedUri() {
        return lastCapturedUri;
    }

    public void setLastCapturedUri(Uri uri) {
        lastCapturedUri = uri;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(activity)
                .inflate(R.layout.filepicker_dir_item, parent, false);
        return new ViewHolder(v, onClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (showCamera) {
            if (position == 0) {
                handleCamera(holder.openCamera, false);
                return;
            }
            if (showVideoCamera) {
                if (position == 1) {
                    handleCamera(holder.openVideoCamera, true);
                    return;
                }
                holder.openVideoCamera.setVisibility(View.GONE);
                position--;
            }
            holder.openCamera.setVisibility(View.GONE);
            position--;
        } else if (showVideoCamera) {
            if (position == 0) {
                handleCamera(holder.openVideoCamera, true);
                return;
            }
            holder.openVideoCamera.setVisibility(View.GONE);
            position--;
        }

        holder.dir = mediaDirs.get(position);

        holder.dirName.setText(holder.dir.getName());

        glideRequest.load(holder.dir.getPreview())
                .into(holder.dirPreview);
    }

    private void handleCamera(final ImageView openCamera, final boolean forVideo) {
        openCamera.setVisibility(View.VISIBLE);
        openCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onCameraClickListener != null && !onCameraClickListener.onCameraClick(forVideo))
                    return;
                openCamera(forVideo);
            }
        });
    }

    public void openCamera(boolean forVideo) {
        Intent intent;
        String fileName;
        File dir;
        Uri externalContentUri;
        if (forVideo) {
            intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            fileName = "/VID_" + getTimeStamp() + ".mp4";
            dir = getExternalStoragePublicDirectory(DIRECTORY_MOVIES);
            externalContentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        } else {
            intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            dir = getExternalStoragePublicDirectory(DIRECTORY_PICTURES);
            fileName = "/IMG_" + getTimeStamp() + ".jpeg";
            externalContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }
        if (!dir.exists() && !dir.mkdir()) {
            Log.d(TAG, "onClick: " +
                    (forVideo ? "MOVIES" : "PICTURES") + " Directory not exists");
            return;
        }
        lastCapturedFile = new File(dir.getAbsolutePath() + fileName);

        Uri fileUri = FilePickerProvider.getUriForFile(activity, lastCapturedFile);

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DATA, lastCapturedFile.getAbsolutePath());
        values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, System.currentTimeMillis());
        lastCapturedUri = activity.getContentResolver().insert(externalContentUri, values);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
        activity.startActivityForResult(intent, CAPTURE_IMAGE_VIDEO);
    }

    private String getTimeStamp() {
        return TimeStamp.format(new Date());
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    public void setOnCameraClickListener(OnCameraClickListener onCameraClickListener) {
        this.onCameraClickListener = onCameraClickListener;
    }

    @Override
    public int getItemCount() {
        if (showCamera) {
            if (showVideoCamera)
                return mediaDirs.size() + 2;
            return mediaDirs.size() + 1;
        } else if (showVideoCamera) {
            return mediaDirs.size() + 1;
        }
        return mediaDirs.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView openCamera, openVideoCamera;
        private SquareImage dirPreview;
        private TextView dirName;
        private Dir dir;

        ViewHolder(View v, final OnClickListener onClickListener) {
            super(v);
            openCamera = v.findViewById(R.id.file_open_camera);
            openVideoCamera = v.findViewById(R.id.file_open_video_camera);
            dirPreview = v.findViewById(R.id.dir_preview);
            dirName = v.findViewById(R.id.dir_name);

            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClickListener.onClick(dir);
                }
            });
        }
    }

    public interface OnCameraClickListener {
        boolean onCameraClick(boolean forVideo);
    }

    public interface OnClickListener {
        void onClick(Dir dir);
    }
}
