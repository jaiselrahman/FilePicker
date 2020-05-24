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
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.paging.AsyncPagedListDiffer;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.AsyncDifferConfig;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListUpdateCallback;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.request.RequestOptions;
import com.jaiselrahman.filepicker.R;
import com.jaiselrahman.filepicker.model.MediaFile;
import com.jaiselrahman.filepicker.utils.FilePickerProvider;
import com.jaiselrahman.filepicker.utils.TimeUtils;
import com.jaiselrahman.filepicker.view.SquareImage;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.os.Environment.DIRECTORY_MOVIES;
import static android.os.Environment.DIRECTORY_PICTURES;
import static android.os.Environment.getExternalStoragePublicDirectory;
import static com.jaiselrahman.filepicker.activity.FilePickerActivity.TAG;

public class FileGalleryAdapter extends MultiSelectionAdapter<FileGalleryAdapter.ViewHolder>
        implements MultiSelectionAdapter.OnSelectionListener<FileGalleryAdapter.ViewHolder>,
        ListUpdateCallback {
    public static final int CAPTURE_IMAGE_VIDEO = 1;
    private Activity activity;
    private RequestManager glideRequest;
    private OnSelectionListener<ViewHolder> onSelectionListener;
    private OnCameraClickListener onCameraClickListener;
    private boolean showCamera;
    private boolean showVideoCamera;
    private String lastCapturedFile;
    private Uri lastCapturedUri;
    private SimpleDateFormat TimeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());

    private AsyncPagedListDiffer<MediaFile> differ = new AsyncPagedListDiffer<MediaFile>(this, new AsyncDifferConfig.Builder<>(ITEM_CALLBACK).build());

    public FileGalleryAdapter(Activity activity, int imageSize, boolean showCamera, boolean showVideoCamera) {
        this.activity = activity;
        this.showCamera = showCamera;
        this.showVideoCamera = showVideoCamera;

        setDiffer(differ);

        glideRequest = Glide.with(this.activity)
                .applyDefaultRequestOptions(RequestOptions
                        .sizeMultiplierOf(0.70f)
                        .optionalCenterCrop()
                        .override(imageSize));
        super.setOnSelectionListener(this);
        if (showCamera && showVideoCamera)
            setItemStartPosition(2);
        else if (showCamera || showVideoCamera)
            setItemStartPosition(1);
    }

    public String getLastCapturedFile() {
        return lastCapturedFile;
    }

    public void setLastCapturedFile(String file) {
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
                .inflate(R.layout.filegallery_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (showCamera) {
            if (position == 0) {
                holder.bind(null, false);
                return;
            }
            if (showVideoCamera) {
                if (position == 1) {
                    holder.bind(null, true);
                    return;
                }
                position--;
            }
            position--;
        } else if (showVideoCamera) {
            if (position == 0) {
                holder.bind(null, true);
                return;
            }
            position--;
        }

        super.onBindViewHolder(holder, position);

        MediaFile mediaFile = getItem(position);

        holder.bind(mediaFile, null);
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

            lastCapturedFile = dir.getAbsolutePath() + fileName;
            lastCapturedUri = FilePickerProvider.getUriForFile(activity, new File(lastCapturedFile));
        } else {
            intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            dir = getExternalStoragePublicDirectory(DIRECTORY_PICTURES);
            fileName = "/IMG_" + getTimeStamp() + ".jpeg";
            externalContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

            lastCapturedFile = dir.getAbsolutePath() + fileName;

            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DATA, lastCapturedFile);
            values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, System.currentTimeMillis());
            lastCapturedUri = activity.getContentResolver().insert(externalContentUri, values);
        }
        if (!dir.exists() && !dir.mkdir()) {
            Log.d(TAG, "onClick: " +
                    (forVideo ? "MOVIES" : "PICTURES") + " Directory not exists");
            return;
        }

        intent.putExtra(MediaStore.EXTRA_OUTPUT, lastCapturedUri);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        activity.startActivityForResult(intent, CAPTURE_IMAGE_VIDEO);
    }

    private String getTimeStamp() {
        return TimeStamp.format(new Date());
    }

    @Override
    public void setOnSelectionListener(OnSelectionListener<ViewHolder> onSelectionListener) {
        this.onSelectionListener = onSelectionListener;
    }

    public void setOnCameraClickListener(OnCameraClickListener onCameraClickListener) {
        this.onCameraClickListener = onCameraClickListener;
    }

    @Override
    public int getItemCount() {
        if (showCamera) {
            if (showVideoCamera)
                return super.getItemCount() + 2;
            return super.getItemCount() + 1;
        } else if (showVideoCamera) {
            return super.getItemCount() + 1;
        }
        return super.getItemCount();
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
    }

    @Override
    public void onUnSelected(ViewHolder view, int position) {
        if (onSelectionListener != null) {
            onSelectionListener.onUnSelected(view, position);
        }
        view.fileSelected.setVisibility(View.GONE);
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

    public class ViewHolder extends MultiSelectionAdapter.ViewHolder {
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

            openCamera.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onCameraClickListener != null && !onCameraClickListener.onCameraClick(false))
                        return;
                    openCamera(false);
                }
            });

            openVideoCamera.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onCameraClickListener != null && !onCameraClickListener.onCameraClick(true))
                        return;
                    openCamera(true);
                }
            });
        }

        void bind(MediaFile mediaFile, Boolean forVideo) {
            if (forVideo == null) {
                openCamera.setVisibility(View.GONE);
                openVideoCamera.setVisibility(View.GONE);
            } else {
                openCamera.setVisibility(forVideo ? View.GONE : View.VISIBLE);
                openVideoCamera.setVisibility(forVideo ? View.VISIBLE : View.GONE);
            }

            if (mediaFile == null) return;

            if (mediaFile.getMediaType() == MediaFile.TYPE_VIDEO ||
                    mediaFile.getMediaType() == MediaFile.TYPE_IMAGE) {
                glideRequest.load(mediaFile.getUri())
                        .into(fileThumbnail);
            } else if (mediaFile.getMediaType() == MediaFile.TYPE_AUDIO) {
                glideRequest.load(mediaFile.getThumbnail())
                        .apply(RequestOptions.placeholderOf(R.drawable.ic_audio))
                        .into(fileThumbnail);
            } else {
                fileThumbnail.setImageResource(R.drawable.ic_file);
            }

            if (mediaFile.getMediaType() == MediaFile.TYPE_VIDEO ||
                    mediaFile.getMediaType() == MediaFile.TYPE_AUDIO) {
                fileDuration.setVisibility(View.VISIBLE);
                fileDuration.setText(TimeUtils.getDuration(mediaFile.getDuration()));
            } else {
                fileDuration.setVisibility(View.GONE);
            }

            if (mediaFile.getMediaType() == MediaFile.TYPE_FILE
                    || mediaFile.getMediaType() == MediaFile.TYPE_AUDIO) {
                fileName.setVisibility(View.VISIBLE);
                fileName.setText(mediaFile.getName());
            } else {
                fileName.setVisibility(View.GONE);
            }

            fileSelected.setVisibility(isSelected(mediaFile) ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onInserted(int position, int count) {
        notifyItemRangeInserted(itemStartPosition + position, count);
    }

    @Override
    public void onRemoved(int position, int count) {
        notifyItemRangeRemoved(itemStartPosition + position, count);
    }

    @Override
    public void onMoved(int fromPosition, int toPosition) {
        notifyItemMoved(itemStartPosition + fromPosition, itemStartPosition + toPosition);
    }

    @Override
    public void onChanged(int position, int count, Object payload) {
        notifyItemRangeChanged(itemStartPosition + position, count, payload);
    }

    public void submitList(PagedList<MediaFile> mediaFiles) {
        differ.submitList(mediaFiles);
    }

    public interface OnCameraClickListener {
        boolean onCameraClick(boolean forVideo);
    }

    private static final DiffUtil.ItemCallback<MediaFile> ITEM_CALLBACK = new DiffUtil.ItemCallback<MediaFile>() {
        @Override
        public boolean areItemsTheSame(@NonNull MediaFile oldItem, @NonNull MediaFile newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull MediaFile oldItem, @NonNull MediaFile newItem) {
            return oldItem.getName().equals(newItem.getName());
        }
    };
}
