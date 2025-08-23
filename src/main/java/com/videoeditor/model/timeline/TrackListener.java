package com.videoeditor.model.timeline;


/**
 * Enhanced listener interface for track and segment events.
 * Provides detailed event notifications for UI updates and system coordination.
 */
public interface TrackListener {

    /**
     * Called when any track is updated (segments added/removed/modified)
     * This is the primary method that must be implemented.
     */
    void onTrackUpdated();

    /**
     * Called when a specific track is added to the timeline
     * @param track The track that was added
     */
    default void onTrackAdded(Track track) {
        onTrackUpdated();
    }

    /**
     * Called when a specific track is removed from the timeline
     * @param track The track that was removed
     */
    default void onTrackRemoved(Track track) {
        onTrackUpdated();
    }

    /**
     * Called when a segment is added to any track
     * @param track The track containing the segment
     * @param segment The segment that was added
     */
    default void onSegmentAdded(Track track, TrackSegment segment) {
        onTrackUpdated();
    }

    /**
     * Called when a segment is removed from any track
     * @param track The track that contained the segment
     * @param segment The segment that was removed
     */
    default void onSegmentRemoved(Track track, TrackSegment segment) {
        onTrackUpdated();
    }


    /**
     * Called when a segment is split into two parts
     * @param track The track containing the segments
     * @param originalSegment The original segment (now the first part)
     * @param newSegment The new segment (second part)
     * @param splitTime The time where the split occurred
     */
    default void onSegmentSplit(Track track, TrackSegment originalSegment, TrackSegment newSegment, double splitTime) {
        onTrackUpdated();
    }

    /**
     * Called when the cursor/playhead position changes
     * @param oldPosition The previous cursor position
     * @param newPosition The new cursor position
     */
    default void onCursorPositionChanged(double oldPosition, double newPosition) {
        // Default empty implementation
    }

    /**
     * Called when segment selection changes
     * @param selectedSegments The currently selected segments
     */
    default void onSelectionChanged(java.util.Set<TrackSegment> selectedSegments) {
        // Default empty implementation
    }


}