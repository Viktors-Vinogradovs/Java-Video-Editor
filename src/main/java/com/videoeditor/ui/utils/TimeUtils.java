package com.videoeditor.ui.utils;

public final class TimeUtils {
    private TimeUtils() {} // Utility class

    /**
     * Format time in seconds to MM:SS.ff format (consistent across app)
     */
    public static String formatTime(double timeInSeconds) {
        if (timeInSeconds < 0) return "00:00.00";

        int minutes = (int) (timeInSeconds / 60);
        int seconds = (int) (timeInSeconds % 60);
        int centiseconds = (int) ((timeInSeconds % 1) * 100);

        return String.format("%02d:%02d.%02d", minutes, seconds, centiseconds);
    }

}