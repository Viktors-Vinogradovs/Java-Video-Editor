package com.videoeditor.core.metadata;

public class VideoMetadata {
    private final int duration; // Duration in seconds
    private final int width;    // Width of the video
    private final int height;   // Height of the video
    private final double frameRate; // Frame rate of the video

    /**
     * Constructs a VideoMetadata instance with the specified properties.
     *
     * @param duration   Duration of the video in seconds (must be non-negative).
     * @param width      Width of the video in pixels (must be positive).
     * @param height     Height of the video in pixels (must be positive).
     * @param frameRate  Frame rate of the video in frames per second (must be positive).
     * @throws IllegalArgumentException if any parameter is invalid.
     */
    public VideoMetadata(int duration, int width, int height, double frameRate) {
        if (duration < 0) {
            throw new IllegalArgumentException("Duration cannot be negative");
        }
        if (width <= 0) {
            throw new IllegalArgumentException("Width must be positive");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("Height must be positive");
        }
        if (frameRate <= 0) {
            throw new IllegalArgumentException("Frame rate must be positive");
        }
        this.duration = duration;
        this.width = width;
        this.height = height;
        this.frameRate = frameRate;
    }

    public int getDuration() {
        return duration;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public String toString() {
        return "VideoMetadata{" +
                "duration=" + duration +
                ", width=" + width +
                ", height=" + height +
                ", frameRate=" + frameRate +
                '}';
    }
}