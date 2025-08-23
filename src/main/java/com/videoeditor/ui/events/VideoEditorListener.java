package com.videoeditor.ui.events;

import java.io.File;

/**
 * Enhanced interface for handling video editor events from the UI components.
 * Includes support for audio tracks, seeking, and enhanced playback control.
 */
public interface VideoEditorListener {
    // Playback control events
    void onPlayPauseToggled();
    void onPreviousFrameRequested();
    void onNextFrameRequested();
    void onReplayRequested();

    // New seeking and audio control events
    void onSeekRequested(double timePosition);
    void onVolumeChanged(double volume);
    void onMuteToggled();

    // Timeline and view control events
    void onZoomInRequested();
    void onZoomOutRequested();
    void onViewChanged(double zoomLevel, double viewStartTime, double viewEndTime);
    void onCursorMoved(double position);

    // Editing events
    void onCutRequested();
    void onDeleteRequested();

    // File I/O events
    void onImportVideoRequested();
    void onExportVideoRequested();

    double getCurrentPlaybackTime();
    double getTotalMediaDuration();
    boolean isCurrentlyPlaying();

    // Segment and track events
    void onSegmentCreated(File originalFile, double startTime, double duration);
    void onVideoDroppedFromMediaPool(File file, double dropPosition);

    // Audio track events
    void onAudioTrackRequested();

    // Enhanced playback events
    void onPlaybackStateChanged(boolean isPlaying);
    void onTimeChanged(double currentTime);
}