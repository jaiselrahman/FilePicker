package com.jaisel.filepicker.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
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

import java.util.ArrayList;

public class FileGalleryAdapter extends MultiSelectionAdapter<FileGalleryAdapter.ViewHolder>
        implements MultiSelectionAdapter.OnSelectionListener<FileGalleryAdapter.ViewHolder> {
    private ArrayList<File> files;
    private Context context;
    private RequestManager glideRequest;
    private OnSelectionListener<ViewHolder> onSelectionListener;

    public FileGalleryAdapter(Context context, ArrayList<File> files) {
        super(files);
        this.files = files;
        this.context = context;
        glideRequest = Glide.with(context);
        super.setOnSelectionListener(this);
        setMaxSelection(10);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.filegallery_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        File file = files.get(position);
        if (file.getMediaType() == File.TYPE_VIDEO ||
                file.getMediaType() == File.TYPE_IMAGE) {
            glideRequest.load(file.getPath())
                    .into(holder.fileThumnail);
        } else if (file.getMediaType() == File.TYPE_AUDIO) {
            glideRequest.load(file.getThumbnail())
                    .into(holder.fileThumnail);
        } else {
            holder.fileThumnail.setImageDrawable(null);
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
        private ImageView fileSelected;
        private SquareImage fileThumnail;
        private TextView fileDuration, fileName;

        ViewHolder(View v) {
            super(v);
            fileThumnail = v.findViewById(R.id.file_thumbnail);
            fileDuration = v.findViewById(R.id.file_duration);
            fileName = v.findViewById(R.id.file_name);
            fileSelected = v.findViewById(R.id.file_selected);
        }
    }
}
