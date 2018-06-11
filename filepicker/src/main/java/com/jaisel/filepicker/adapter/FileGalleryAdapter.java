package com.jaisel.filepicker.adapter;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.jaisel.filepicker.R;
import com.jaisel.filepicker.model.File;
import com.jaisel.filepicker.utils.TimeUtils;
import com.jaisel.filepicker.view.SquareImage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import static android.os.Environment.DIRECTORY_PICTURES;
import static android.os.Environment.getExternalStoragePublicDirectory;

public class FileGalleryAdapter extends MultiSelectionAdapter<FileGalleryAdapter.ViewHolder>
        implements MultiSelectionAdapter.OnSelectionListener<FileGalleryAdapter.ViewHolder> {
    public static final int REQUEST_TAKE_PHOTO = 1;
    private static final String TAG = "FileGallerAdapter";
    private ArrayList<File> files;
    private Activity activity;
    private RequestManager glideRequest;
    private OnSelectionListener<ViewHolder> onSelectionListener;
    private boolean showCamera;
    private java.io.File imageFile;
    private Uri imageUri;

    public FileGalleryAdapter(Activity activity, ArrayList<File> files, boolean showCamera) {
        super(files);
        this.files = files;
        this.activity = activity;
        this.showCamera = showCamera;
        glideRequest = Glide.with(this.activity);
        super.setOnSelectionListener(this);
    }

    public Uri getLastTakenImageUri() {
        return imageUri;
    }

    public java.io.File getLastTakenImageFile() {
        return imageFile;
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
                holder.openCamera.setVisibility(View.VISIBLE);
                holder.openCamera.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                        String imageName = "IMG_" + timeStamp + "_";
                        imageFile = null;
                        imageUri = null;
                        try {
                            java.io.File pictureDir = getExternalStoragePublicDirectory(DIRECTORY_PICTURES);
                            if (!pictureDir.exists() && !pictureDir.mkdir()) {
                                Log.d(TAG, "onClick: openCamera Picture Directory not exists");
                                return;
                            }
                            imageFile = java.io.File.createTempFile(
                                    imageName,
                                    ".jpg",
                                    pictureDir
                            );
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (imageFile != null) {
                            ContentValues contentValues = new ContentValues(1);
                            contentValues.put(MediaStore.Images.Media.DATA, imageFile.getAbsolutePath());
                            imageUri = activity.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                            activity.startActivityForResult(intent, REQUEST_TAKE_PHOTO);
                        }
                    }
                });
                return;
            } else {
                holder.openCamera.setVisibility(View.GONE);
                position--;
            }
        }
        super.onBindViewHolder(holder, position);
        File file = files.get(position);
        if (file.getMediaType() == File.TYPE_VIDEO ||
                file.getMediaType() == File.TYPE_IMAGE) {
            glideRequest.load(file.getPath())
                    .into(holder.fileThumbnail);
        } else if (file.getMediaType() == File.TYPE_AUDIO) {
            glideRequest.load(file.getThumbnail())
                    .into(holder.fileThumbnail);
        } else {
            holder.fileThumbnail.setImageDrawable(null);
        }

        if (file.getMediaType() == File.TYPE_VIDEO ||
                file.getMediaType() == File.TYPE_AUDIO) {
            holder.fileDuration.setVisibility(View.VISIBLE);
            holder.fileDuration.setText(TimeUtils.getDuration(file.getDuration()));
        } else {
            holder.fileDuration.setVisibility(View.GONE);
        }

        if (file.getMediaType() == File.TYPE_FILE
                || file.getMediaType() == File.TYPE_AUDIO) {
            holder.fileName.setVisibility(View.VISIBLE);
            holder.fileName.setText(file.getName());
        } else {
            holder.fileName.setVisibility(View.GONE);
        }

        holder.fileSelected.setVisibility(isSelected(file) ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setOnSelectionListener(OnSelectionListener<ViewHolder> onSelectionListener) {
        this.onSelectionListener = onSelectionListener;
    }

    @Override
    public int getItemCount() {
        if (showCamera) {
            return files.size() + 1;
        }
        return files.size();
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

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView fileSelected, openCamera;
        private SquareImage fileThumbnail;
        private TextView fileDuration, fileName;

        ViewHolder(View v) {
            super(v);
            openCamera = v.findViewById(R.id.file_open_camera);
            fileThumbnail = v.findViewById(R.id.file_thumbnail);
            fileDuration = v.findViewById(R.id.file_duration);
            fileName = v.findViewById(R.id.file_name);
            fileSelected = v.findViewById(R.id.file_selected);
        }
    }
}
