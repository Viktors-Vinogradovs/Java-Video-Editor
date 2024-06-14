package com.videoeditor.core;

/**
 * @param duration Duration in seconds
 */
public record VideoMetadata(int duration) {

    @Override
    public String toString() {
        return "VideoMetadata{" +
                "duration=" + duration +
                '}';
    }
}
