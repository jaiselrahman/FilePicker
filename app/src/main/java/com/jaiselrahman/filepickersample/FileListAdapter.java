package com.jaiselrahman.filepickersample;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.jaiselrahman.filepicker.model.File;

import java.util.ArrayList;

public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.ViewHolder> {
    private ArrayList<File> files;

    public FileListAdapter(ArrayList<File> files) {
        this.files = files;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.file_list_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File file = files.get(position);
        Context context = holder.itemView.getContext();
        holder.filePath.setText(context.getString(R.string.path, file.getPath()));
        holder.fileMime.setText(context.getString(R.string.mime, file.getMimeType()));
        holder.fileSize.setText(context.getString(R.string.size, file.getSize()));
        holder.fileBucketName.setText(context.getString(R.string.bucketname, file.getBucketName()));
        if (file.getMediaType() == File.TYPE_IMAGE
                || file.getMediaType() == File.TYPE_VIDEO) {
            Glide.with(context)
                    .load(Uri.parse("file://" + file.getPath()))
                    .into(holder.fileThumbnail);
        } else {
            holder.fileThumbnail.setImageDrawable(null);
        }
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView fileThumbnail;
        private TextView filePath, fileSize, fileMime, fileBucketName;

        ViewHolder(View view) {
            super(view);
            fileThumbnail = view.findViewById(R.id.file_thumbnail);
            filePath = view.findViewById(R.id.file_path);
            fileSize = view.findViewById(R.id.file_size);
            fileMime = view.findViewById(R.id.file_mime);
            fileBucketName = view.findViewById(R.id.file_bucketname);
        }
    }
}
