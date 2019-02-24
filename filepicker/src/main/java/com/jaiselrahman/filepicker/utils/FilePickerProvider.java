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

package com.jaiselrahman.filepicker.utils;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import java.io.File;

public class FilePickerProvider extends FileProvider {
    private static final String FILE_PROVIDER = ".filepicker.provider";

    public static String getAuthority(@NonNull Context context) {
        return context.getPackageName() + FILE_PROVIDER;
    }

    public static Uri getUriForFile(@NonNull Context context, @NonNull File file) {
        return getUriForFile(context, getAuthority(context), file);
    }
}
