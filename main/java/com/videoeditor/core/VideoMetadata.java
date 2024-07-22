package com.videoeditor.core;

public class VideoMetadata {

    private final int duration;

    public VideoMetadata(int duration) {
        this.duration = duration;
    }

    @Override
    public String toString() {
        return "VideoMetadata{" +
                "duration=" + duration +
                '}';
    }
}
