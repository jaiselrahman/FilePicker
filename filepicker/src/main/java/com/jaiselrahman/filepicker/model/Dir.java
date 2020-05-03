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

package com.jaiselrahman.filepicker.model;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class Dir implements Parcelable {
    private long id;
    private String name;
    private Uri preview;
    private int count;

    public Dir() {
    }

    protected Dir(Parcel in) {
        id = in.readLong();
        name = in.readString();
        preview = in.readParcelable(Uri.class.getClassLoader());
        count = in.readInt();
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setPreview(Uri preview) {
        this.preview = preview;
    }

    public Uri getPreview() {
        return preview;
    }

    public static final Creator<Dir> CREATOR = new Creator<Dir>() {
        @Override
        public Dir createFromParcel(Parcel in) {
            return new Dir(in);
        }

        @Override
        public Dir[] newArray(int size) {
            return new Dir[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(name);
        dest.writeParcelable(preview, flags);
        dest.writeInt(count);
    }
}
