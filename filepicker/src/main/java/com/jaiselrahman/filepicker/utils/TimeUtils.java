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

import java.util.concurrent.TimeUnit;

public class TimeUtils {
    public static String getDuration(long duration) {
        long hours = TimeUnit.MILLISECONDS.toHours(duration);
        duration -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration);
        duration -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(duration);
        StringBuilder durationBuilder = new StringBuilder();
        if (hours > 0) {
            durationBuilder.append(hours)
                    .append(":");
        }
        if (minutes < 10)
            durationBuilder.append('0');
        durationBuilder.append(minutes)
                .append(":");
        if (seconds < 10)
            durationBuilder.append('0');
        durationBuilder.append(seconds);
        return durationBuilder.toString();
    }
}
