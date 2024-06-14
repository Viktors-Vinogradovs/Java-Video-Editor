package com.videoeditor.model;

public class TrackSegment {
    private final String filePath;
    private final String audioFilePath;
    private final double startTime;
    private double duration;

    public TrackSegment(String filePath, String audioFilePath, double startTime, double duration) {
        this.filePath = filePath;
        this.audioFilePath = audioFilePath;
        this.startTime = startTime;
        this.duration = duration;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getAudioFilePath() {
        return audioFilePath;
    }

    public double getStartTime() {
        return startTime;
    }

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public double getEndTime() {
        return this.startTime + this.duration;
    }
}
