package com.jaisel.filepicker.utils;

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
