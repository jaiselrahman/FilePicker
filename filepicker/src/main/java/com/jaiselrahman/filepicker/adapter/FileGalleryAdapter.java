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

package com.jaiselrahman.filepicker.adapter;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.request.RequestOptions;
import com.jaiselrahman.filepicker.R;
import com.jaiselrahman.filepicker.model.MediaFile;
import com.jaiselrahman.filepicker.utils.FilePickerProvider;
import com.jaiselrahman.filepicker.utils.FileUtils;
import com.jaiselrahman.filepicker.utils.TimeUtils;
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

public class FileGalleryAdapter extends MultiSelectionAdapter<FileGalleryAdapter.ViewHolder>
        implements MultiSelectionAdapter.OnSelectionListener<FileGalleryAdapter.ViewHolder> {
    public static final int CAPTURE_IMAGE_VIDEO = 1;
    private ArrayList<MediaFile> mediaFiles;
    private Activity activity;
    private RequestManager glideRequest;
    private OnSelectionListener<ViewHolder> onSelectionListener;
    private boolean showCamera;
    private boolean showVideoCamera;
    private File lastCapturedFile;
    private Uri lastCapturedUri;
    private SimpleDateFormat TimeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());

    public FileGalleryAdapter(Activity activity, ArrayList<MediaFile> mediaFiles, int imageSize, boolean showCamera, boolean showVideoCamera) {
        super(mediaFiles);
        this.mediaFiles = mediaFiles;
        this.activity = activity;
        this.showCamera = showCamera;
        this.showVideoCamera = showVideoCamera;
        glideRequest = Glide.with(this.activity)
                .applyDefaultRequestOptions(RequestOptions
                        .sizeMultiplierOf(0.70f)
                        .optionalCenterCrop()
                        .override(imageSize));
        super.setOnSelectionListener(this);
        if (showCamera && showVideoCamera)
            setItemStartPostion(2);
        else if (showCamera || showVideoCamera)
            setItemStartPostion(1);
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

    private static final int TYPE_IMAGE = 0, TYPE_FILE = 1;

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(activity)
                .inflate(viewType == TYPE_IMAGE ? R.layout.filegallery_item : R.layout.file_gallery_line_item, parent, false);
        return new ViewHolder(v);
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

        super.onBindViewHolder(holder, position);
        MediaFile mediaFile = mediaFiles.get(position);
//        setThumbnailPadding(holder.fileThumbnail, !(mediaFile.getMediaType() == MediaFile.TYPE_VIDEO ||
//                mediaFile.getMediaType() == MediaFile.TYPE_IMAGE ||
//                mediaFile.getMediaType() == MediaFile.TYPE_AUDIO));
        if (mediaFile.getMediaType() == MediaFile.TYPE_VIDEO ||
                mediaFile.getMediaType() == MediaFile.TYPE_IMAGE) {
            glideRequest.load(mediaFile.getPath()).into(holder.fileThumbnail);
        } else if (mediaFile.getMediaType() == MediaFile.TYPE_AUDIO) {
            glideRequest.load(mediaFile.getThumbnail()).apply(RequestOptions.placeholderOf(R.drawable.ic_audio)).into(holder.fileThumbnail);
        } else if (mediaFile.getMediaType() == MediaFile.TYPE_TXT) {
            holder.fileThumbnail.setImageResource(R.drawable.ic_txt);
        } else if (mediaFile.getMediaType() == MediaFile.TYPE_WORD) {
            holder.fileThumbnail.setImageResource(R.drawable.ic_word);
        } else if (mediaFile.getMediaType() == MediaFile.TYPE_EXCEL) {
            holder.fileThumbnail.setImageResource(R.drawable.ic_excel);
        } else if (mediaFile.getMediaType() == MediaFile.TYPE_PPT) {
            holder.fileThumbnail.setImageResource(R.drawable.ic_ppt);
        } else if (mediaFile.getMediaType() == MediaFile.TYPE_PDF) {
            holder.fileThumbnail.setImageResource(R.drawable.ic_pdf);
        } else if (mediaFile.getMediaType() == MediaFile.TYPE_ZIP) {
            holder.fileThumbnail.setImageResource(R.drawable.ic_zip);
        } else {
            String mime = FileUtils.getMimeTypeByFullPath(mediaFile.getPath());
            if (!TextUtils.isEmpty(mime)) {
                if (mime.contains("video")) {
                    mediaFile.setMediaType(MediaFile.TYPE_VIDEO);
                    holder.fileThumbnail.setImageResource(R.drawable.ic_video);
                } else if (mime.contains("audio")) {
                    mediaFile.setMediaType(MediaFile.TYPE_AUDIO);
                    holder.fileThumbnail.setImageResource(R.drawable.ic_audio);
                } else if (mime.contains("image")) {
                    mediaFile.setMediaType(MediaFile.TYPE_IMAGE);
                    glideRequest.load(mediaFile.getPath()).into(holder.fileThumbnail);
                    //holder.fileThumbnail.setImageResource(R.drawable.ic_image);
                } else {
                    holder.fileThumbnail.setImageResource(R.drawable.ic_file);
                }
            } else {
                holder.fileThumbnail.setImageResource(R.drawable.ic_file);
            }
        }

        if (mediaFile.getMediaType() == MediaFile.TYPE_VIDEO ||
                mediaFile.getMediaType() == MediaFile.TYPE_AUDIO) {
            holder.fileDuration.setVisibility(View.VISIBLE);
            holder.fileDuration.setText(TimeUtils.getDuration(mediaFile.getDuration()));
        } else {
            holder.fileDuration.setVisibility(View.GONE);
        }

        if (mediaFile.getMediaType() == MediaFile.TYPE_FILE
                || mediaFile.getMediaType() == MediaFile.TYPE_AUDIO || mediaFile.getMediaType() >= MediaFile.TYPE_WORD) {
            holder.fileName.setVisibility(View.VISIBLE);
            String name = mediaFile.getName();
            if (!isImageObject(mediaFile.getMediaType())) {
                name = name + mediaFile.getSuffix();
            }
            holder.fileName.setText(name);
        } else {
            holder.fileName.setVisibility(View.GONE);
        }
        if (isImageObject(mediaFile.getMediaType())) {
            holder.fileSelected.setVisibility(isSelected(mediaFile) ? View.VISIBLE : View.GONE);
        } else {
            holder.fileSelected.setVisibility(View.VISIBLE);
            holder.fileSelected.setImageResource(isSelected(mediaFile) ? R.drawable.ic_check_box_checked : R.drawable.ic_check_box_unchecked);
        }
    }

    private boolean isImageObject(int type) {
        return type == MediaFile.TYPE_VIDEO || type == MediaFile.TYPE_IMAGE;
    }

    @Override
    protected boolean isItemNeedFullLine(int position) {
        if (showCamera || showVideoCamera) {
            return false;
        }
        // 不是具有图片的内容时，沾满全行
        return !isImageObject(mediaFiles.get(position).getMediaType());
    }

    @Override
    protected int getItemSpanSize(int position) {
        if (showCamera || showVideoCamera) {
            return 1;
        }
        return isImageObject(mediaFiles.get(position).getMediaType()) ? 1 : getSpanCount();
    }

    private void setThumbnailPadding(SquareImage image, boolean padding) {
        int pad = padding ? image.getResources().getDimensionPixelOffset(R.dimen._default_dimen_padding_) : 0;
        image.setPadding(pad, pad, pad, pad);
    }

    private void handleCamera(ImageView openCamera, final boolean forVideo) {
        openCamera.setVisibility(View.VISIBLE);
        openCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PackageManager packageManager = v.getContext().getPackageManager();
                if (null == packageManager) {
                    return;
                }
                if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                    Toast.makeText(v.getContext(), R.string.toast_text_no_feature_for_camera, Toast.LENGTH_LONG).show();
                    return;
                }
                Intent intent;
                String fileName;
                File dir;
                Uri externalContentUri;
                if (forVideo) {
                    intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                    fileName = "/VID_" + getTimeStamp() + ".mp4";
                    dir = getExternalStoragePublicDirectory(DIRECTORY_MOVIES);
                    externalContentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
                    // 录制时长限制
                    if (0 < getMaxVideoDuration()) {
                        // 限制录制时间(10秒=10)
                        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, getMaxVideoDuration());
                    }
                    // 文件长度限制
                    if (0 < getMaxVideoFileSize()) {
                        // 限制录制大小(10M=10 * 1024 * 1024L)
                        intent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, getMaxVideoFileSize());
                    }
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
                try {
                    activity.startActivityForResult(intent, CAPTURE_IMAGE_VIDEO);
                } catch (Exception ignore) {
                    Toast.makeText(v.getContext(), forVideo ? R.string.toast_text_no_feature_for_capture_video : R.string.toast_text_no_feature_for_capture_image, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private String getTimeStamp() {
        return TimeStamp.format(new Date());
    }

    @Override
    public int getItemViewType(int position) {
        if (showCamera || showVideoCamera) {
            return TYPE_IMAGE;
        }
        MediaFile file = mediaFiles.get(position);
        switch (file.getMediaType()) {
            //case MediaFile.TYPE_AUDIO:
            case MediaFile.TYPE_IMAGE:
            case MediaFile.TYPE_VIDEO:
                return TYPE_IMAGE;
            default:
                return TYPE_FILE;
        }
    }

    @Override
    public void setOnSelectionListener(OnSelectionListener<ViewHolder> onSelectionListener) {
        this.onSelectionListener = onSelectionListener;
    }

    @Override
    public int getItemCount() {
        if (showCamera) {
            if (showVideoCamera)
                return mediaFiles.size() + 2;
            return mediaFiles.size() + 1;
        } else if (showVideoCamera) {
            return mediaFiles.size() + 1;
        }
        return mediaFiles.size();
    }

    @Override
    public void onSelectionBegin() {
        if (onSelectionListener != null) {
            onSelectionListener.onSelectionBegin();
        }
    }

    @Override
    public void onSelected(ViewHolder view, int position) {
        if (onSelectionListener != null) {
            onSelectionListener.onSelected(view, position);
        }
        view.fileSelected.setVisibility(View.VISIBLE);
        MediaFile file = mediaFiles.get(position);
        if (!isImageObject(file.getMediaType())) {
            view.fileSelected.setImageResource(R.drawable.ic_check_box_checked);
        }
    }

    @Override
    public void onUnSelected(ViewHolder view, int position) {
        if (onSelectionListener != null) {
            onSelectionListener.onUnSelected(view, position);
        }
        MediaFile file = mediaFiles.get(position);
        if (!isImageObject(file.getMediaType())) {
            view.fileSelected.setVisibility(View.VISIBLE);
            view.fileSelected.setImageResource(R.drawable.ic_check_box_unchecked);
        } else {
            view.fileSelected.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSelectAll() {
        if (onSelectionListener != null) {
            onSelectionListener.onSelectAll();
        }
    }

    @Override
    public void onUnSelectAll() {
        if (onSelectionListener != null) {
            onSelectionListener.onUnSelectAll();
        }
    }

    @Override
    public void onSelectionEnd() {
        if (onSelectionListener != null) {
            onSelectionListener.onSelectionEnd();
        }
    }

    @Override
    public void onMaxReached() {
        if (onSelectionListener != null) {
            onSelectionListener.onMaxReached();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView fileSelected, openCamera, openVideoCamera;
        private SquareImage fileThumbnail;
        private TextView fileDuration, fileName;

        ViewHolder(View v) {
            super(v);
            openCamera = v.findViewById(R.id.file_open_camera);
            openVideoCamera = v.findViewById(R.id.file_open_video_camera);
            fileThumbnail = v.findViewById(R.id.file_thumbnail);
            fileDuration = v.findViewById(R.id.file_duration);
            fileName = v.findViewById(R.id.file_name);
            fileSelected = v.findViewById(R.id.file_selected);
        }
    }
}
