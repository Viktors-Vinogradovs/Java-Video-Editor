package com.videoeditor.ui.controllers;

import com.videoeditor.model.timeline.Track;
import com.videoeditor.model.timeline.TrackListener;
import com.videoeditor.model.timeline.TrackSegment;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

/**
 * Manages tracks and segments for a video editing timeline.
 * Provides functionality for adding, removing, and manipulating tracks and segments,
 * with support for selection, snapping, and listener notifications.
 */
public class TrackManager {
    private static final Logger logger = Logger.getLogger(TrackManager.class.getName());
    private final List<Track> tracks = new ArrayList<>();
    private final Set<TrackSegment> selectedSegments = new HashSet<>();
    private final List<TrackSegment> clipboard = new ArrayList<>();
    private final List<TrackListener> listeners = new ArrayList<>();
    private double cursorPosition = 0.0;
    private boolean snapToGrid = true;
    private double gridSize = 1.0;

    public TrackManager(Track initialTrack) {
        tracks.add(initialTrack);
        logger.info("TrackManager initialized with initial track: " + initialTrack.getName());
    }

    public static class SegmentOperation {
        public final boolean success;
        public final String message;

        public SegmentOperation(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static SegmentOperation success(String message) {
            return new SegmentOperation(true, message);
        }

        public static SegmentOperation failure(String message) {
            return new SegmentOperation(false, message);
        }
    }

    public SegmentOperation addSegmentAtTime(File file, double startTime, double duration) {
        if (startTime < 0 || duration <= 0) {
            return SegmentOperation.failure("Invalid start time or duration");
        }

        TrackSegment segment = new TrackSegment(file, startTime, duration);
        Track targetTrack = tracks.stream()
                .filter(t -> !t.isLocked())
                .findFirst()
                .orElse(null);

        if (targetTrack == null) {
            return SegmentOperation.failure("No available track to add segment");
        }

        try {
            targetTrack.insertSegmentAtTime(segment, startTime, Track.OverlapBehavior.REJECT);
            notifyListenersOnSegmentAdded(targetTrack, segment);
            return SegmentOperation.success("Added segment at " + startTime);
        } catch (IllegalStateException e) {
            return SegmentOperation.failure("Failed to add segment: " + e.getMessage());
        }
    }

    public SegmentOperation deleteSelectedSegments() {
        if (selectedSegments.isEmpty()) {
            return SegmentOperation.failure("No segments selected");
        }

        for (TrackSegment segment : new HashSet<>(selectedSegments)) {
            Track track = findTrackContainingSegment(segment);
            if (track != null && !track.isLocked()) {
                track.removeSegment(segment);
                notifyListenersOnSegmentRemoved(track, segment);
            }
        }
        selectedSegments.clear();
        notifyListenersOnSelectionChanged();
        return SegmentOperation.success("Deleted " + selectedSegments.size() + " segments");
    }

    public SegmentOperation copySelectedSegments() {
        if (selectedSegments.isEmpty()) {
            return SegmentOperation.failure("No segments selected to copy");
        }

        clipboard.clear();
        for (TrackSegment segment : selectedSegments) {
            clipboard.add(segment.copy());
        }
        return SegmentOperation.success("Copied " + clipboard.size() + " segments to clipboard");
    }

    public SegmentOperation cutAllTracksAtTime(double time) {
        if (time < 0) {
            return SegmentOperation.failure("Invalid cut time");
        }

        boolean cutPerformed = false;
        for (Track track : tracks) {
            if (track.isLocked()) continue;

            List<TrackSegment> segmentsToCut = track.getSegmentsAtTime(time);
            for (TrackSegment segment : segmentsToCut) {
                TrackSegment newSegment = segment.splitAt(time);
                if (newSegment != null) {
                    track.insertSegmentAtTime(newSegment, newSegment.getStartTime(), Track.OverlapBehavior.REJECT);
                    notifyListenersOnSegmentSplit(track, segment, newSegment, time);
                    cutPerformed = true;
                }
            }
        }

        if (cutPerformed) {
            notifyListeners();
            return SegmentOperation.success("Cut tracks at " + time);
        }
        return SegmentOperation.failure("No segments to cut at " + time);
    }

    public double getTotalDuration() {
        return tracks.stream()
                .flatMap(track -> track.getSegments().stream())
                .mapToDouble(segment -> segment.getStartTime() + segment.getDuration())
                .max()
                .orElse(0.0);
    }

    public List<Track> getTracks() {
        return new ArrayList<>(tracks);
    }

    public void setCursorPosition(double position) {
        if (position < 0) return;
        double oldPosition = this.cursorPosition;
        this.cursorPosition = position;
        notifyListenersOnCursorPositionChanged(oldPosition, position);
    }

    public void selectSegment(TrackSegment segment) {
        selectedSegments.add(segment);
        notifyListenersOnSelectionChanged();
    }

    public void selectSegments(List<TrackSegment> segments) {
        selectedSegments.clear();
        selectedSegments.addAll(segments);
        notifyListenersOnSelectionChanged();
    }

    public void clearSelection() {
        selectedSegments.clear();
        notifyListenersOnSelectionChanged();
    }

    public boolean isSelected(TrackSegment segment) {
        return selectedSegments.contains(segment);
    }

    public Track findTrackContainingSegment(TrackSegment segment) {
        return tracks.stream()
                .filter(track -> track.getSegments().contains(segment))
                .findFirst()
                .orElse(null);
    }

    public void addListener(TrackListener listener) {
        listeners.add(listener);
    }

    public void notifyListeners() {
        listeners.forEach(TrackListener::onTrackUpdated);
    }

    private void notifyListenersOnSegmentAdded(Track track, TrackSegment segment) {
        listeners.forEach(listener -> listener.onSegmentAdded(track, segment));
    }

    private void notifyListenersOnSegmentRemoved(Track track, TrackSegment segment) {
        listeners.forEach(listener -> listener.onSegmentRemoved(track, segment));
    }

    private void notifyListenersOnSegmentSplit(Track track, TrackSegment originalSegment, TrackSegment newSegment, double splitTime) {
        listeners.forEach(listener -> listener.onSegmentSplit(track, originalSegment, newSegment, splitTime));
    }

    private void notifyListenersOnCursorPositionChanged(double oldPosition, double newPosition) {
        listeners.forEach(listener -> listener.onCursorPositionChanged(oldPosition, newPosition));
    }

    private void notifyListenersOnSelectionChanged() {
        listeners.forEach(listener -> listener.onSelectionChanged(new HashSet<>(selectedSegments)));
    }

    public boolean isSnapToGrid() {
        return snapToGrid;
    }

    public double getGridSize() {
        return gridSize;
    }

}