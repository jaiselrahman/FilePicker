/*
 *  Copyright (c) 2019, Jaisel Rahman <jaiselrahman@gmail.com>.
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

import android.provider.MediaStore;

import com.jaiselrahman.filepicker.config.Configurations;

import java.io.File;
import java.util.regex.Matcher;

import static java.io.File.separatorChar;

public class FileUtils {
    public static boolean toIgnoreFolder(String path, Configurations configs) {
        String parent = getParent(path);
        if (configs.isIgnoreHiddenFile() && getName(parent).startsWith(".")) return true;
        if (configs.getIgnorePathMatchers() != null) {
            for (Matcher matcher : configs.getIgnorePathMatchers()) {
                if (matcher.reset(path).matches()) {
                    return true;
                }
            }
        }
        if (configs.isIgnoreNoMediaDir()) {
            return new File(parent, MediaStore.MEDIA_IGNORE_FILENAME).exists();
        }
        return false;
    }

    public static String getParent(String path) {
        int index = path.lastIndexOf(separatorChar);
        int prefixLength = getPrefixLength(path);
        if (index < prefixLength) {
            if ((prefixLength > 0) && (path.length() > prefixLength))
                return path.substring(0, prefixLength);
            return "";
        }
        return path.substring(0, index);
    }

    private static String getName(String path) {
        int index = path.lastIndexOf(separatorChar);
        int prefixLength = getPrefixLength(path);
        if (index < prefixLength) return path.substring(prefixLength);
        return path.substring(index + 1);
    }

    private static int getPrefixLength(String path) {
        return path.length() == 0 ? 0 : (path.charAt(0) == '/' ? 1 : 0);
    }
}
