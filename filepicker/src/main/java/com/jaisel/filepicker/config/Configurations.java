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

package com.jaisel.filepicker.config;

import android.os.Parcel;
import android.os.Parcelable;

import com.jaisel.filepicker.model.File;

import java.util.ArrayList;

public class Configurations implements Parcelable {
    public static final Creator<Configurations> CREATOR = new Creator<Configurations>() {
        @Override
        public Configurations createFromParcel(Parcel in) {
            return new Configurations(in);
        }

        @Override
        public Configurations[] newArray(int size) {
            return new Configurations[size];
        }
    };
    private final boolean imageCaptureEnabled;
    private final boolean videoCaptureEnabled;
    private final boolean showVideos;
    private final boolean showImages;
    private final boolean showAudios;
    private final boolean showFiles;
    private final boolean singleClickSelection;
    private final boolean checkPermission, skipZeroSizeFiles;
    private final int imageSize, maxSelection;
    private final int landscapeSpanCount;
    private final int portraitSpanCount;
    private final String[] suffixes;
    private final ArrayList<File> selectedFiles;

    private Configurations(boolean imageCapture, boolean videoCapture,
                           boolean showVideos, boolean showImages, boolean showAudios, boolean showFiles,
                           boolean singleClickSelection, boolean checkPermission, boolean skipZeroSizeFiles,
                           int imageSize, int maxSelection, int landscapeSpanCount, int portraitSpanCount,
                           String[] suffixes, ArrayList<File> selectedFiles) {
        this.imageCaptureEnabled = imageCapture;
        this.videoCaptureEnabled = videoCapture;
        this.showVideos = showVideos;
        this.showImages = showImages;
        this.showAudios = showAudios;
        this.showFiles = showFiles;
        this.singleClickSelection = singleClickSelection;
        this.checkPermission = checkPermission;
        this.skipZeroSizeFiles = skipZeroSizeFiles;
        this.imageSize = imageSize;
        this.maxSelection = maxSelection;
        this.landscapeSpanCount = landscapeSpanCount;
        this.portraitSpanCount = portraitSpanCount;
        this.suffixes = suffixes;
        this.selectedFiles = selectedFiles;
    }

    protected Configurations(Parcel in) {
        imageCaptureEnabled = in.readByte() != 0;
        videoCaptureEnabled = in.readByte() != 0;
        showVideos = in.readByte() != 0;
        showImages = in.readByte() != 0;
        showAudios = in.readByte() != 0;
        showFiles = in.readByte() != 0;
        singleClickSelection = in.readByte() != 0;
        checkPermission = in.readByte() != 0;
        skipZeroSizeFiles = in.readByte() != 0;
        imageSize = in.readInt();
        maxSelection = in.readInt();
        landscapeSpanCount = in.readInt();
        portraitSpanCount = in.readInt();
        suffixes = in.createStringArray();
        selectedFiles = in.createTypedArrayList(File.CREATOR);
    }

    public boolean isShowVideos() {
        return showVideos;
    }

    public boolean isShowImages() {
        return showImages;
    }

    public boolean isShowAudios() {
        return showAudios;
    }

    public boolean isShowFiles() {
        return showFiles;
    }

    public boolean isSkipZeroSizeFiles() {
        return skipZeroSizeFiles;
    }

    public ArrayList<File> getSelectedFiles() {

        return selectedFiles;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (imageCaptureEnabled ? 1 : 0));
        dest.writeByte((byte) (videoCaptureEnabled ? 1 : 0));
        dest.writeByte((byte) (showVideos ? 1 : 0));
        dest.writeByte((byte) (showImages ? 1 : 0));
        dest.writeByte((byte) (showAudios ? 1 : 0));
        dest.writeByte((byte) (showFiles ? 1 : 0));
        dest.writeByte((byte) (singleClickSelection ? 1 : 0));
        dest.writeByte((byte) (checkPermission ? 1 : 0));
        dest.writeByte((byte) (skipZeroSizeFiles ? 1 : 0));
        dest.writeInt(imageSize);
        dest.writeInt(maxSelection);
        dest.writeInt(landscapeSpanCount);
        dest.writeInt(portraitSpanCount);
        dest.writeStringArray(suffixes);
        dest.writeTypedList(selectedFiles);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public boolean isSingleClickSelection() {
        return singleClickSelection;
    }

    public int getMaxSelection() {
        return maxSelection;
    }

    public int getLandscapeSpanCount() {
        return landscapeSpanCount;
    }

    public int getPortraitSpanCount() {
        return portraitSpanCount;
    }

    public boolean isCheckPermission() {
        return checkPermission;
    }

    public boolean isImageCaptureEnabled() {
        return imageCaptureEnabled;
    }

    public boolean isVideoCaptureEnabled() {
        return videoCaptureEnabled;
    }

    public int getImageSize() {
        return imageSize;
    }

    public String[] getSuffixes() {
        return suffixes;
    }

    public static class Builder {
        private boolean imageCapture = false, videoCapture = false,
                checkPermission = false, showImages = true, showVideos = true,
                showFiles = false, showAudios = false, singleClickSelection = true,
                skipZeroSizeFiles = true;
        private int imageSize = -1, maxSelection = 10;
        private int landscapeSpanCount = 5;
        private int portraitSpanCount = 3;
        private String[] suffixes = new String[]{
                "txt", "pdf", "html", "rtf", "csv", "xml",
                "zip", "tar", "gz", "rar", "7z", "torrent",
                "doc", "docx", "odt", "ott",
                "ppt", "pptx", "pps",
                "xls", "xlsx", "ods", "ots"};
        private ArrayList<File> selectedFiles = null;

        public Builder setSingleClickSelection(boolean singleClickSelection) {
            this.singleClickSelection = singleClickSelection;
            return this;
        }

        public Builder setSkipZeroSizeFiles(boolean skipZeroSizeFiles) {
            this.skipZeroSizeFiles = skipZeroSizeFiles;
            return this;
        }

        public Builder setLandscapeSpanCount(int landscapeSpanCount) {
            this.landscapeSpanCount = landscapeSpanCount;
            return this;
        }

        public Builder setPortraitSpanCount(int portraitSpanCount) {
            this.portraitSpanCount = portraitSpanCount;
            return this;
        }

        public Builder setShowImages(boolean showImages) {
            this.showImages = showImages;
            return this;
        }

        public Builder setMaxSelection(int maxSelection) {
            this.maxSelection = maxSelection;
            return this;
        }

        public Builder setShowVideos(boolean showVideos) {
            this.showVideos = showVideos;
            return this;
        }

        public Builder setShowFiles(boolean showFiles) {
            this.showFiles = showFiles;
            return this;
        }

        public Builder setShowAudios(boolean showAudios) {
            this.showAudios = showAudios;
            return this;
        }

        public Builder setCheckPermission(boolean checkPermission) {
            this.checkPermission = checkPermission;
            return this;
        }

        public Builder enableImageCapture(boolean imageCapture) {
            this.imageCapture = imageCapture;
            return this;
        }

        public Builder enableVideoCapture(boolean videoCapture) {
            this.videoCapture = videoCapture;
            return this;
        }

        public Builder setImageSize(int imageSize) {
            this.imageSize = imageSize;
            return this;
        }

        public Builder setSuffixes(String... suffixes) {
            this.suffixes = suffixes;
            return this;
        }

        public Builder setSelectedFiles(ArrayList<File> selectedFiles) {
            this.selectedFiles = selectedFiles;
            return this;
        }

        public Configurations build() {
            return new Configurations(imageCapture, videoCapture, showVideos, showImages, showAudios, showFiles,
                    singleClickSelection, checkPermission, skipZeroSizeFiles, imageSize, maxSelection, landscapeSpanCount, portraitSpanCount, suffixes, selectedFiles);
        }
    }
}
