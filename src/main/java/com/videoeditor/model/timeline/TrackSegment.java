package com.videoeditor.model.timeline;

import java.io.File;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Represents a segment of media on a timeline track.
 * Each segment has a position on the timeline, duration, and references to source media.
 */
public class TrackSegment {
    private final String id;
    private final File originalFile;
    private double startTime;           // Position on timeline (seconds)
    private double duration;            // Duration of segment (seconds)
    private double sourceStartTime;     // Start time in the source file (seconds)
    private final double sourceDuration;      // Duration from source file (seconds)
    private boolean locked;
    private String name;
    private static final Logger logger = Logger.getLogger(TrackSegment.class.getName());

    // Video/Audio specific properties
    private boolean videoEnabled = true;
    private boolean audioEnabled = true;
    private double volume = 1.0;
    private double opacity = 1.0;

    /**
     * Create a new track segment
     * @param originalFile Source media file
     * @param startTime Position on timeline
     * @param duration Duration of segment
     */
    public TrackSegment(File originalFile, double startTime, double duration) {
        this(originalFile, startTime, duration, 0.0, duration);
    }

    /**
     * Create a new track segment with source trimming
     * @param originalFile Source media file
     * @param startTime Position on timeline
     * @param duration Duration of segment
     * @param sourceStartTime Start time in source file
     * @param sourceDuration Duration from source file
     */
    public TrackSegment(File originalFile, double startTime, double duration,
                        double sourceStartTime, double sourceDuration) {
        this.id = UUID.randomUUID().toString();
        this.originalFile = Objects.requireNonNull(originalFile, "Original file cannot be null");
        this.name = originalFile.getName();

        // Validate times
        if (startTime < 0) throw new IllegalArgumentException("Start time cannot be negative");
        if (duration <= 0) throw new IllegalArgumentException("Duration must be positive");
        if (sourceStartTime < 0) throw new IllegalArgumentException("Source start time cannot be negative");
        if (sourceDuration <= 0) throw new IllegalArgumentException("Source duration must be positive");

        this.startTime = startTime;
        this.duration = duration;
        this.sourceStartTime = sourceStartTime;
        this.sourceDuration = sourceDuration;
    }

    // Getters
    public String getId() { return id; }
    public File getOriginalFile() { return originalFile; }
    public double getStartTime() { return startTime; }
    public double getDuration() { return duration; }
    public double getEndTime() { return startTime + duration; }
    public double getSourceStartTime() { return sourceStartTime; }
    public double getSourceDuration() { return sourceDuration; }
    public double getSourceEndTime() { return sourceStartTime + sourceDuration; }

    public String getName() { return name; }
    public double getVolume() { return volume; }

    // Setters with validation
    public void setStartTime(double startTime) {
        if (locked) throw new IllegalStateException("Segment is locked");
        if (startTime < 0) throw new IllegalArgumentException("Start time cannot be negative");
        this.startTime = startTime;
    }

    public void setDuration(double duration) {
        if (locked) throw new IllegalStateException("Segment is locked");
        if (duration <= 0) throw new IllegalArgumentException("Duration must be positive");

        // FIXED: More lenient check - allow up to source duration + small tolerance
        double maxAllowedDuration = sourceDuration + 0.1; // Small tolerance for floating point
        if (duration > maxAllowedDuration) {
            // Instead of throwing exception, cap the duration
            this.duration = sourceDuration;
            logger.warning("Duration capped to source duration: " + sourceDuration);
        } else {
            this.duration = duration;
        }
    }

    public void setSourceStartTime(double sourceStartTime) {
        if (locked) throw new IllegalStateException("Segment is locked");
        if (sourceStartTime < 0) throw new IllegalArgumentException("Source start time cannot be negative");
        this.sourceStartTime = sourceStartTime;
    }


    /**
     * Check if this segment overlaps with another segment
     */
    public boolean overlapsWith(TrackSegment other) {
        return this.startTime < other.getEndTime() && this.getEndTime() > other.startTime;
    }

    /**
     * Check if this segment contains a specific time point
     */
    public boolean containsTime(double time) {
        return time >= startTime && time < getEndTime();
    }


    /**
     * Split this segment at a specific time point
     * @param splitTime Time to split at (must be within segment bounds)
     * @return New segment representing the second part, or null if split is invalid
     */
    public TrackSegment splitAt(double splitTime) {
        if (locked) throw new IllegalStateException("Cannot split locked segment");
        if (splitTime <= startTime || splitTime >= getEndTime()) return null;

        double firstPartDuration = splitTime - startTime;
        double secondPartDuration = getEndTime() - splitTime;

        // Create second part
        TrackSegment secondPart = new TrackSegment(
                originalFile,
                splitTime,
                secondPartDuration,
                sourceStartTime + firstPartDuration,
                secondPartDuration
        );

        // Modify this segment to be the first part
        this.duration = firstPartDuration;

        return secondPart;
    }

    /**
     * Create a copy of this segment
     */
    public TrackSegment copy() {
        TrackSegment copy = new TrackSegment(originalFile, startTime, duration, sourceStartTime, sourceDuration);
        copy.name = this.name;
        copy.locked = this.locked;
        copy.videoEnabled = this.videoEnabled;
        copy.audioEnabled = this.audioEnabled;
        copy.volume = this.volume;
        copy.opacity = this.opacity;
        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TrackSegment that = (TrackSegment) obj;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("TrackSegment{id='%s', file='%s', timeline=%.2f-%.2f, source=%.2f-%.2f}",
                id, originalFile.getName(), startTime, getEndTime(),
                sourceStartTime, getSourceEndTime());
    }
}