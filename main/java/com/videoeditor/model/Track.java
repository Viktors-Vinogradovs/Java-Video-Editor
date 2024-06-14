package com.videoeditor.model;

import java.util.ArrayList;
import java.util.List;

public class Track {
    private final List<TrackSegment> segments;
    private final List<TrackListener> listeners;

    public Track() {
        this.segments = new ArrayList<>();
        this.listeners = new ArrayList<>();
    }

    public void addSegment(TrackSegment segment) {
        segments.add(segment);
        notifyListeners();
    }

    public void removeSegment(TrackSegment segment) {
        segments.remove(segment);
        notifyListeners();
    }

    public List<TrackSegment> getSegments() {
        return segments;
    }

    public void addTrackListener(TrackListener listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        for (TrackListener listener : listeners) {
            listener.onTrackUpdated();
        }
    }

    public double getTotalDuration() {
        double totalDuration = 0;
        for (TrackSegment segment : segments) {
            totalDuration += segment.getDuration();
        }
        return totalDuration;
    }

    public void splitSegment(TrackSegment segment, double splitTime) {
        if (splitTime <= 0 || splitTime >= segment.getDuration()) {
            throw new IllegalArgumentException("Split time must be within the segment duration.");
        }

        TrackSegment newSegment = new TrackSegment(segment.getFilePath(), segment.getAudioFilePath(), segment.getStartTime() + splitTime, segment.getDuration() - splitTime);
        segment.setDuration(splitTime);

        int index = segments.indexOf(segment);
        segments.add(index + 1, newSegment);

        notifyListeners();
    }
}
