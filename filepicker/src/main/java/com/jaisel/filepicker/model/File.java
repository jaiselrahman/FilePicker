package com.jaisel.filepicker.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;

public class File implements Parcelable {
    public static final int TYPE_FILE = 0;
    public static final int TYPE_IMAGE = 1;
    public static final int TYPE_AUDIO = 2;
    public static final int TYPE_VIDEO = 3;
    public static final Creator<File> CREATOR = new Creator<File>() {
        @Override
        public File createFromParcel(Parcel in) {
            return new File(in);
        }

        @Override
        public File[] newArray(int size) {
            return new File[size];
        }
    };
    private long id, size, duration, date;
    private long height, width;
    private String name, path, mimeType, bucketId, bucketName;
    private @Type
    int mediaType;

    public File() {
    }

    protected File(Parcel in) {
        id = in.readLong();
        size = in.readLong();
        duration = in.readLong();
        date = in.readLong();
        height = in.readLong();
        width = in.readLong();
        name = in.readString();
        path = in.readString();
        mimeType = in.readString();
        bucketId = in.readString();
        bucketName = in.readString();
        mediaType = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeLong(size);
        dest.writeLong(duration);
        dest.writeLong(date);
        dest.writeLong(height);
        dest.writeLong(width);
        dest.writeString(name);
        dest.writeString(path);
        dest.writeString(mimeType);
        dest.writeString(bucketId);
        dest.writeString(bucketName);
        dest.writeInt(mediaType);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public long getHeight() {
        return height;
    }

    public void setHeight(long height) {
        this.height = height;
    }

    public long getWidth() {
        return width;
    }

    public void setWidth(long width) {
        this.width = width;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getBucketId() {
        return bucketId;
    }

    public void setBucketId(String bucketId) {
        this.bucketId = bucketId;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public @Type
    int getMediaType() {
        return mediaType;
    }

    public void setMediaType(@Type int mediaType) {
        this.mediaType = mediaType;
    }

    @IntDef({TYPE_FILE, TYPE_IMAGE, TYPE_AUDIO, TYPE_VIDEO})
    public @interface Type {
    }
}
