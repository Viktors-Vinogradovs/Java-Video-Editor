package com.videoeditor.model.timeline;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * Enhanced Track class with better audio support and track management.
 * Manages segments, handles overlaps, and maintains track integrity.
 */
public class Track {
    private static final Logger logger = Logger.getLogger(Track.class.getName());

    public enum TrackType {
        VIDEO("Video", "🎬"),
        AUDIO("Audio", "🎵"),
        SUBTITLE("Subtitle", "📝");

        private final String displayName;
        private final String icon;

        TrackType(String displayName, String icon) {
            this.displayName = displayName;
            this.icon = icon;
        }

        public String getDisplayName() { return displayName; }
        public String getIcon() { return icon; }
    }

    private final String id;
    private final TrackType type;
    private final List<TrackSegment> segments;
    private final List<TrackListener> listeners;
    private final String name;
    private boolean locked = false;

    // Audio-specific properties
    private final double volume = 1.0; // 0.0 to 1.0


    public Track(TrackType type, String name) {
        this.locked = locked;
        this.id = UUID.randomUUID().toString();
        this.type = Objects.requireNonNull(type, "Track type cannot be null");
        this.name = Objects.requireNonNull(name, "Track name cannot be null");
        this.segments = new ArrayList<>();
        this.listeners = new CopyOnWriteArrayList<>();
        getDefaultColorForType(type);
        logger.info("Created new " + type + " track: " + name);
    }

    private void getDefaultColorForType(TrackType type) {
        switch (type) {
            case VIDEO:
                new Color(0, 122, 255);
                return;
            case AUDIO:
                new Color(52, 199, 89);
                return;
            case SUBTITLE:
                new Color(255, 149, 0);
                return;
            default:
                new Color(128, 128, 128);
        }
    }

    public TrackType getType() { return type; }
    public String getName() { return name; }
    public boolean isLocked() { return locked; }
    public boolean isVisible() {
        return true; }
    public boolean isMuted() {
        return false; }
    public double getHeight() { // UI height
        return 50.0; }
    public double getVolume() { return volume; }

    public boolean isSoloMode() {
        return false; }

    /**
     * Get a defensive copy of segments sorted by start time
     */
    public List<TrackSegment> getSegments() {
        List<TrackSegment> sortedSegments = new ArrayList<>(segments);
        sortedSegments.sort(Comparator.comparingDouble(TrackSegment::getStartTime));
        return sortedSegments;
    }

    /**
     * Get segments that exist at a specific time
     */
    public List<TrackSegment> getSegmentsAtTime(double time) {
        return segments.stream()
                .filter(segment -> segment.containsTime(time))
                .sorted(Comparator.comparingDouble(TrackSegment::getStartTime))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * Get the total duration of the track (end time of last segment)
     */
    public double getTotalDuration() {
        return segments.stream()
                .mapToDouble(TrackSegment::getEndTime)
                .max()
                .orElse(0.0);
    }

    /**
     * Add a segment to the track
     */
    public void addSegment(TrackSegment segment) {
        if (locked) {
            throw new IllegalStateException("Cannot add segment to locked track");
        }

        Objects.requireNonNull(segment, "Segment cannot be null");
        validateSegment(segment);

        if (segments.contains(segment)) {
            logger.warning("Attempted to add duplicate segment: " + segment.getId());
            return;
        }

        segments.add(segment);
        logger.info("Added segment to track " + name + ": " + segment);
        notifyListeners();
        notifySegmentAdded(segment);
    }

    /**
     * Remove a segment from the track
     */
    public void removeSegment(TrackSegment segment) {
        if (locked) {
            throw new IllegalStateException("Cannot remove segment from locked track");
        }

        boolean removed = segments.remove(segment);
        if (removed) {
            logger.info("Removed segment from track " + name + ": " + segment);
            notifyListeners();
            notifySegmentRemoved(segment);
        }
    }

    /**
     * Validate a single segment for correctness and overlaps
     */
    public void validateSegment(TrackSegment segment) {
        Objects.requireNonNull(segment, "Segment cannot be null");

        if (segment.getStartTime() < 0) {
            throw new IllegalStateException("Segment start time cannot be negative: " + segment.getStartTime());
        }

        if (segment.getDuration() <= 0) {
            throw new IllegalStateException("Segment duration must be positive: " + segment.getDuration());
        }

        // For audio tracks, allow overlaps (mixing)
        if (type != TrackType.AUDIO) {
            List<TrackSegment> overlapping = findOverlappingSegments(segment);
            if (!overlapping.isEmpty()) {
                throw new IllegalStateException("Segment overlaps with existing segment(s): " + overlapping);
            }
        }
    }

    /**
     * Insert segment at specific time, handling overlaps intelligently
     */
    public void insertSegmentAtTime(TrackSegment segment, double insertTime, OverlapBehavior behavior) {
        if (locked) {
            throw new IllegalStateException("Cannot insert segment into locked track");
        }

        segment.setStartTime(insertTime);
        List<TrackSegment> overlapping = findOverlappingSegments(segment);

        // For audio tracks, allow overlapping (mixing capability)
        if (type == TrackType.AUDIO && behavior == OverlapBehavior.REJECT && !overlapping.isEmpty()) {
            // Change behavior to allow mixing for audio
            behavior = OverlapBehavior.ALLOW_MIXING;
        }

        if (overlapping.isEmpty() || behavior == OverlapBehavior.ALLOW_MIXING) {
            addSegment(segment);
            return;
        }

        handleOverlap(segment, overlapping, behavior);
    }

    /**
     * Find segments that overlap with the given segment
     */
    public List<TrackSegment> findOverlappingSegments(TrackSegment segment) {
        return segments.stream()
                .filter(s -> !s.equals(segment) && s.overlapsWith(segment))
                .sorted(Comparator.comparingDouble(TrackSegment::getStartTime))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private void notifyListeners() {
        for (TrackListener listener : listeners) {
            try {
                listener.onTrackUpdated();
            } catch (Exception e) {
                logger.warning("Error notifying track listener: " + e.getMessage());
            }
        }
    }

    private void notifySegmentAdded(TrackSegment segment) {
        for (TrackListener listener : listeners) {
            try {
                listener.onSegmentAdded(this, segment);
            } catch (Exception e) {
                logger.warning("Error notifying segment added: " + e.getMessage());
            }
        }
    }

    private void notifySegmentRemoved(TrackSegment segment) {
        for (TrackListener listener : listeners) {
            try {
                listener.onSegmentRemoved(this, segment);
            } catch (Exception e) {
                logger.warning("Error notifying segment removed: " + e.getMessage());
            }
        }
    }

    /**
     * Handle overlap based on specified behavior
     */
    private void handleOverlap(TrackSegment newSegment, List<TrackSegment> overlapping, OverlapBehavior behavior) {
        switch (behavior) {
            case REJECT:
                return;

            case OVERWRITE:
                for (TrackSegment segment : overlapping) {
                    removeSegment(segment);
                }
                addSegment(newSegment);
                return;

            case SPLIT:
                return;

            case PUSH:
                handlePushInsertion(newSegment, overlapping);
                return;

            case ALLOW_MIXING:
                addSegment(newSegment);
                return;

            default:
        }
    }

    private void handlePushInsertion(TrackSegment newSegment, List<TrackSegment> overlapping) {
        // Push overlapping segments to the right
        double pushAmount = newSegment.getEndTime() - overlapping.get(0).getStartTime();

        for (TrackSegment segment : overlapping) {
            segment.setStartTime(segment.getStartTime() + pushAmount);
        }

        addSegment(newSegment);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Track track = (Track) obj;
        return Objects.equals(id, track.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Track{id='%s', name='%s', type=%s, segments=%d, duration=%.2f, volume=%.0f%%}",
                id, name, type, segments.size(), getTotalDuration(), volume * 100);
    }

    // Enhanced enums and classes
    public enum OverlapBehavior {
        REJECT,         // Don't allow overlaps
        OVERWRITE,      // Remove overlapping segments
        SPLIT,          // Split overlapping segments
        PUSH,           // Push overlapping segments to make room
        ALLOW_MIXING    // Allow overlaps (for audio tracks)
    }
    public static class TimeRange {
        public final double start;
        public final double end;
        public final double duration;

        public TimeRange(double start, double end) {
            this.start = start;
            this.end = end;
            this.duration = end - start;
        }

        @Override
        public String toString() {
            return String.format("TimeRange{%.2f-%.2f (%.2f)}", start, end, duration);
        }
    }
}