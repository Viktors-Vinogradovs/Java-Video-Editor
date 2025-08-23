package com.videoeditor.ui.controllers;

import com.videoeditor.export.ExportDialog;
import com.videoeditor.export.ExportService;
import com.videoeditor.export.ExportSettings;
import com.videoeditor.model.project.Project;
import com.videoeditor.model.timeline.Track;
import com.videoeditor.model.timeline.TrackSegment;
import com.videoeditor.ui.dialogs.ExportProgressDialog;
import com.videoeditor.ui.events.VideoEditorListener;
import com.videoeditor.ui.panels.MainView;
import com.videoeditor.ui.panels.TrackPanel;
import com.videoeditor.ui.panels.TrackPanelView;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * FIXED: MainController with proper cursor synchronization during video playback
 */
public class MainController implements VideoEditorListener {
    private static final Logger logger = Logger.getLogger(MainController.class.getName());

    private final MainView mainView;
    private final List<TrackManager> trackManagers = new ArrayList<>();
    private final PlaybackController playbackController;
    private final TrackPanel trackPanel;

    // Clean audio track management (single track only)
    private final AtomicBoolean isPlaying = new AtomicBoolean(false);
    private final AtomicBoolean isUserControlledSeek = new AtomicBoolean(false);
    private final AtomicBoolean isMuted = new AtomicBoolean(false);
    private final AtomicBoolean hasAudioTrack = new AtomicBoolean(false);

    // FIXED: Enhanced cursor management with proper synchronization
    private volatile double cursorPosition = 0.0;
    private volatile double currentTimelinePosition = 0.0;
    private double totalTimelineDuration = 0.0;
    private TrackSegment activeVideoSegment = null;
    private TrackSegment activeAudioSegment = null;
    private File currentLoadedVideo = null;

    // Single audio track support
    private final TrackManager audioTrackManager;

    // FIXED: Improved timers for cursor synchronization
    private Timer uiUpdateTimer;
    private Timer cursorSyncTimer; // NEW: Dedicated cursor sync timer

    public MainController(MainView mainView) {
        logger.info("Initializing MainController with enhanced cursor synchronization");

        this.mainView = mainView;
        this.playbackController = new PlaybackController(mainView.getMediaPlayer());

        // Initialize video track manager
        TrackManager videoTrackManager = new TrackManager(new Track(Track.TrackType.VIDEO, "Video Track"));
        trackManagers.add(videoTrackManager);

        // Initialize SINGLE audio track manager
        audioTrackManager = new TrackManager(new Track(Track.TrackType.AUDIO, "Audio Track"));
        trackManagers.add(audioTrackManager);

        this.trackPanel = new TrackPanel(videoTrackManager, this);
        trackPanel.addTrackManager(audioTrackManager);

        mainView.initialize(trackPanel, this);

        setupPlaybackCallbacks();
        setupEnhancedUIUpdateTimers(); // FIXED: Enhanced timer setup

        logger.info("MainController initialized with synchronized cursor system");
    }

    private void setupPlaybackCallbacks() {
        playbackController.setOnTimeChangedCallback(this::onEnhancedVideoTimeChanged);
        playbackController.setOnPlayStateChangedCallback(this::onEnhancedPlayStateChanged);
        playbackController.setOnMediaLoadedCallback(this::onEnhancedMediaLoaded);
    }

    // FIXED: Enhanced timer setup for better cursor synchronization
    private void setupEnhancedUIUpdateTimers() {
        // Main UI update timer (slower for performance)
        uiUpdateTimer = new Timer(100, e -> {
            if (isPlaying.get()) {
                updateUIWithCurrentTime();
            }
        });
        uiUpdateTimer.start();

        // FIXED: Dedicated high-frequency cursor sync timer
        cursorSyncTimer = new Timer(50, e -> { // 20 FPS for smooth cursor movement
            if (isPlaying.get() && !isUserControlledSeek.get()) {
                synchronizeCursorWithVideo();
            }
        });
        cursorSyncTimer.start();

        logger.info("Enhanced cursor synchronization timers initialized");
    }

    // FIXED: New method for continuous cursor synchronization
    private void synchronizeCursorWithVideo() {
        try {
            if (activeVideoSegment != null && playbackController.hasMedia()) {
                // Get current video playback time
                double videoTime = playbackController.getCurrentTime();

                // Calculate timeline position based on segment start + video time
                double timelineTime = activeVideoSegment.getStartTime() + videoTime;

                // Update cursor position if it has changed significantly
                if (Math.abs(timelineTime - currentTimelinePosition) > 0.05) { // 50ms threshold
                    currentTimelinePosition = timelineTime;
                    cursorPosition = timelineTime;

                    // Update UI cursor position on EDT
                    SwingUtilities.invokeLater(() -> updateCursorPositionInUI(timelineTime));
                }

                // Check if we've reached the end of the current segment
                if (timelineTime >= activeVideoSegment.getEndTime() - 0.1) {
                    SwingUtilities.invokeLater(() -> handleSegmentEnd(timelineTime));
                }
            } else if (isPlaying.get() && activeVideoSegment == null) {
                // Playing through empty timeline space
                currentTimelinePosition += 0.05; // Advance by timer interval
                cursorPosition = currentTimelinePosition;

                SwingUtilities.invokeLater(() -> updateCursorPositionInUI(currentTimelinePosition));
            }
        } catch (Exception e) {
            logger.fine("Error in cursor synchronization: " + e.getMessage());
        }
    }

    // FIXED: Enhanced cursor position update method
    private void updateCursorPositionInUI(double position) {
        // Update track panel cursor
        if (trackPanel != null) {
            trackPanel.setCursorPosition(position);
        }

        // Update track panel view cursor
        TrackPanelView trackPanelView = mainView.getTrackPanelView();
        if (trackPanelView != null) {
            trackPanelView.updateCursorPosition(position);
        }

        // Update all track managers
        for (TrackManager manager : trackManagers) {
            manager.setCursorPosition(position);
        }
    }

    // FIXED: Enhanced video time change handler
    private void onEnhancedVideoTimeChanged(double videoTime) {
        if (isUserControlledSeek.get()) {
            return; // Skip updates during user seeking
        }

        double timelineTime = videoTime;
        if (activeVideoSegment != null) {
            timelineTime = activeVideoSegment.getStartTime() + videoTime;
        }

        // Update positions immediately (synchronous)
        currentTimelinePosition = timelineTime;
        cursorPosition = timelineTime;

        // Schedule UI update
        final double finalTimelineTime = timelineTime;
        SwingUtilities.invokeLater(() -> {
            updateCursorPositionInUI(finalTimelineTime);

            // Check for segment transitions
            if (activeVideoSegment != null && finalTimelineTime >= activeVideoSegment.getEndTime()) {
                handleSegmentEnd(finalTimelineTime);
            }
        });

        onTimeChanged(videoTime);
    }

    private void onEnhancedPlayStateChanged(boolean playing) {
        isPlaying.set(playing);
        SwingUtilities.invokeLater(() -> {
            if (playing) {
                mainView.startPlaybackTimer();
                mainView.showNotification("▶ Playing", "ACCENT_SUCCESS");
                logger.info("Playback started - cursor sync enabled");
            } else {
                mainView.stopPlaybackTimer();
                mainView.showNotification("⏸ Playback Paused", "ACCENT_INFO");
                logger.info("Playback paused - cursor sync disabled");
            }
        });

        onPlaybackStateChanged(playing);
    }

    private void onEnhancedMediaLoaded() {
        totalTimelineDuration = playbackController.getDuration();

        SwingUtilities.invokeLater(() -> {
            if (currentLoadedVideo != null) {
                mainView.updateVideoInfo(currentLoadedVideo.getName());
                ensureAudioTrack();
                mainView.showNotification("🎬 Media loaded", "ACCENT_SUCCESS");
            }
            updateUIWithCurrentTime();
        });
    }

    private void ensureAudioTrack() {
        if (!hasAudioTrack.get()) {
            hasAudioTrack.set(true);

            TrackPanelView trackPanelView = mainView.getTrackPanelView();
            if (trackPanelView != null) {
                trackPanelView.enableAudioTrack();
            }

            logger.info("Audio track enabled");
        }
    }

    // FIXED: Improved cursor position method with immediate UI update
    public void setCursorPosition(double position) {
        this.cursorPosition = position;
        this.currentTimelinePosition = position;

        // Immediate UI update for responsiveness
        updateCursorPositionInUI(position);

        logger.fine("Cursor position set to: " + position);
    }

    private void handleSegmentEnd(double timelineTime) {
        TrackSegment nextVideoSegment = findVideoSegmentAtTime(timelineTime + 0.1);
        TrackSegment nextAudioSegment = findAudioSegmentAtTime(timelineTime + 0.1);

        if (nextVideoSegment != null) {
            switchToVideoPlayback(nextVideoSegment, timelineTime);
        } else {
            playbackController.pause();
            startTimelineMovement();
        }

        if (nextAudioSegment != null && !nextAudioSegment.equals(activeAudioSegment)) {
            activeAudioSegment = nextAudioSegment;
            logger.info("Switched to audio segment: " + nextAudioSegment.getName());
        }
    }

    @Override
    public void onPlayPauseToggled() {
        if (isPlaying.get()) {
            playbackController.pause();
            stopTimelinePlayback();
        } else {
            startEnhancedTimelinePlayback();
        }
    }

    @Override
    public void onSeekRequested(double timePosition) {
        logger.info("Seek requested to position: " + timePosition + " seconds");

        // FIXED: Better seek handling with cursor sync
        isUserControlledSeek.set(true);

        // Update positions immediately
        currentTimelinePosition = timePosition;
        cursorPosition = timePosition;

        // Update UI immediately
        updateCursorPositionInUI(timePosition);

        try {
            TrackSegment videoSegment = findVideoSegmentAtTime(timePosition);
            TrackSegment audioSegment = findAudioSegmentAtTime(timePosition);

            if (videoSegment != null) {
                if (!videoSegment.equals(activeVideoSegment)) {
                    switchToVideoSegment(videoSegment, timePosition);
                }
                double videoTime = timePosition - videoSegment.getStartTime();
                playbackController.seekTo(videoTime);
            } else {
                showBlackScreen();
            }

            if (audioSegment != null && !audioSegment.equals(activeAudioSegment)) {
                activeAudioSegment = audioSegment;
                logger.info("Seeking to audio segment: " + audioSegment.getName());
            }

            SwingUtilities.invokeLater(() -> mainView.updateTimeDisplay(timePosition, getTotalMediaDuration()));

        } finally {
            // FIXED: Shorter delay before re-enabling automatic cursor sync
            SwingUtilities.invokeLater(() -> {
                Timer delayTimer = new Timer(100, e -> { // Reduced from 200ms to 100ms
                    isUserControlledSeek.set(false);
                    ((Timer) e.getSource()).stop();
                });
                delayTimer.setRepeats(false);
                delayTimer.start();
            });
        }
    }

    @Override
    public void onVolumeChanged(double volume) {
        double currentVolume = Math.max(0.0, Math.min(1.0, volume));
        playbackController.setVolume(currentVolume);
        mainView.setVolume(currentVolume);

        mainView.showNotification("🔊 Volume: " + (int)(currentVolume * 100) + "%", "ACCENT_INFO");
        logger.info("Volume changed to: " + (int)(currentVolume * 100) + "%");
    }

    @Override
    public void onMuteToggled() {
        isMuted.set(!isMuted.get());
        playbackController.setMuted(isMuted.get());

        String message = isMuted.get() ? "🔇 Audio Muted" : "🔊 Audio Unmuted";
        mainView.showNotification(message, "ACCENT_INFO");
        logger.info("Mute toggled: " + isMuted.get());
    }

    @Override
    public void onAudioTrackRequested() {
        ensureAudioTrack();
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        logger.fine("Playback state changed: " + isPlaying);
    }

    @Override
    public void onTimeChanged(double currentTime) {
        SwingUtilities.invokeLater(() -> mainView.updateTimeDisplay(currentTime, getTotalMediaDuration()));
    }

    private void startEnhancedTimelinePlayback() {
        double cursorPos = getCurrentCursorPosition();
        TrackSegment videoSegment = findVideoSegmentAtTime(cursorPos);
        TrackSegment audioSegment = findAudioSegmentAtTime(cursorPos);

        if (videoSegment != null) {
            if (!videoSegment.equals(activeVideoSegment)) {
                switchToVideoSegment(videoSegment, cursorPos);
            }

            if (audioSegment != null && !audioSegment.equals(activeAudioSegment)) {
                activeAudioSegment = audioSegment;
                logger.info("Starting playback with audio segment: " + audioSegment.getName());
            }

            double videoTime = cursorPos - videoSegment.getStartTime();
            playbackController.seekTo(videoTime);

            SwingUtilities.invokeLater(() -> {
                Timer startDelay = new Timer(100, e -> {
                    playbackController.play();
                    ((Timer) e.getSource()).stop();
                });
                startDelay.setRepeats(false);
                startDelay.start();
            });
        } else {
            showBlackScreen();
            startTimelineMovement();
        }
    }

    // FIXED: Enhanced timeline movement with proper cursor sync
    private void startTimelineMovement() {
        isPlaying.set(true);

        Thread timelineThread = new Thread(() -> {
            double frameTime = 1.0 / 30.0; // 30 FPS for timeline movement

            while (isPlaying.get()) {
                try {
                    currentTimelinePosition += frameTime;
                    cursorPosition = currentTimelinePosition;

                    SwingUtilities.invokeLater(() -> {
                        updateCursorPositionInUI(currentTimelinePosition);

                        TrackSegment videoSegment = findVideoSegmentAtTime(currentTimelinePosition);
                        TrackSegment audioSegment = findAudioSegmentAtTime(currentTimelinePosition);

                        if (videoSegment != null && !isUserControlledSeek.get()) {
                            switchToVideoPlayback(videoSegment, currentTimelinePosition);
                        }

                        if (audioSegment != null && !audioSegment.equals(activeAudioSegment)) {
                            activeAudioSegment = audioSegment;
                            logger.fine("Timeline movement reached audio segment: " + audioSegment.getName());
                        }
                    });

                    Thread.sleep((long)(frameTime * 1000));

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "Timeline-Movement");

        timelineThread.setDaemon(true);
        timelineThread.start();

        SwingUtilities.invokeLater(() -> mainView.startPlaybackTimer());
    }

    private void stopTimelinePlayback() {
        isPlaying.set(false);
        SwingUtilities.invokeLater(() -> mainView.stopPlaybackTimer());
    }

    private void switchToVideoPlayback(TrackSegment videoSegment, double timelinePosition) {
        stopTimelinePlayback();
        switchToVideoSegment(videoSegment, timelinePosition);

        TrackSegment audioSegment = findAudioSegmentAtTime(timelinePosition);
        if (audioSegment != null) {
            activeAudioSegment = audioSegment;
        }

        double videoTime = timelinePosition - videoSegment.getStartTime();
        playbackController.seekTo(videoTime);

        SwingUtilities.invokeLater(() -> {
            Timer playDelay = new Timer(100, e -> {
                playbackController.play();
                mainView.showNotification("🎬 Playing: " + videoSegment.getOriginalFile().getName(), "ACCENT_SUCCESS");
                ((Timer) e.getSource()).stop();
            });
            playDelay.setRepeats(false);
            playDelay.start();
        });
    }

    @Override
    public void onCursorMoved(double timelinePosition) {
        // FIXED: Enhanced cursor movement handling
        isUserControlledSeek.set(true);

        // Update positions immediately
        currentTimelinePosition = timelinePosition;
        cursorPosition = timelinePosition;

        // Update UI immediately for responsiveness
        updateCursorPositionInUI(timelinePosition);

        try {
            TrackSegment videoSegment = findVideoSegmentAtTime(timelinePosition);
            TrackSegment audioSegment = findAudioSegmentAtTime(timelinePosition);

            if (videoSegment != null) {
                if (!videoSegment.equals(activeVideoSegment)) {
                    switchToVideoSegment(videoSegment, timelinePosition);
                }
                double videoTime = timelinePosition - videoSegment.getStartTime();
                playbackController.seekTo(videoTime);
            } else {
                showBlackScreen();
            }

            if (audioSegment != null && !audioSegment.equals(activeAudioSegment)) {
                activeAudioSegment = audioSegment;
                logger.info("Cursor moved to audio segment: " + audioSegment.getName());
            }

        } finally {
            // FIXED: Shorter re-enable delay for better responsiveness
            SwingUtilities.invokeLater(() -> {
                Timer delayTimer = new Timer(100, e -> { // Reduced delay
                    isUserControlledSeek.set(false);
                    ((Timer) e.getSource()).stop();
                });
                delayTimer.setRepeats(false);
                delayTimer.start();
            });
        }
    }

    private void switchToVideoSegment(TrackSegment newVideoSegment, double timelinePosition) {
        if (newVideoSegment == null) {
            activeVideoSegment = null;
            activeAudioSegment = null;
            currentLoadedVideo = null;
            return;
        }

        for (TrackManager manager : trackManagers) {
            manager.clearSelection();
        }
        trackManagers.get(0).selectSegment(newVideoSegment);

        activeVideoSegment = newVideoSegment;

        if (!newVideoSegment.getOriginalFile().equals(currentLoadedVideo)) {
            currentLoadedVideo = newVideoSegment.getOriginalFile();
            playbackController.loadMedia(currentLoadedVideo.getAbsolutePath());

            ensureAudioTrack();

            logger.info("Switched to video: " + currentLoadedVideo.getName());
        }
    }

    @Override
    public void onSegmentCreated(File originalFile, double startTime, double duration) {
        try {
            double realDuration = duration;
            try {
                playbackController.loadMedia(originalFile.getAbsolutePath());
                Thread.sleep(500);

                double videoDuration = playbackController.getDuration();
                if (videoDuration > 0 && videoDuration < 86400) {
                    realDuration = videoDuration;
                    logger.info("Got real video duration: " + realDuration + " seconds");
                }
            } catch (Exception e) {
                logger.warning("Could not get real video duration: " + e.getMessage());
            }

            // Create video segment
            TrackManager videoTrackManager = trackManagers.get(0);
            TrackManager.SegmentOperation videoResult = videoTrackManager.addSegmentAtTime(originalFile, startTime, realDuration);

            // Automatically create audio segment if video segment was successful and audio track exists
            TrackManager.SegmentOperation audioResult = null;
            if (videoResult.success && hasAudioTrack.get()) {
                audioResult = audioTrackManager.addSegmentAtTime(originalFile, startTime, realDuration);
                if (audioResult.success) {
                    logger.info("Automatically created audio segment for: " + originalFile.getName());
                }
            }

            if (videoResult.success) {
                TrackSegment newVideoSegment = findVideoSegmentAtTime(startTime);
                if (newVideoSegment != null) {
                    switchToVideoSegment(newVideoSegment, startTime);
                    setCursorPosition(startTime);

                    String message = hasAudioTrack.get() && audioResult != null && audioResult.success ?
                            "🎬 Video + Audio added: " + originalFile.getName() :
                            "🎬 Video added: " + originalFile.getName();
                    mainView.showNotification(message, "ACCENT_SUCCESS");

                    updateTimelineDuration();
                }
            } else {
                mainView.showNotification("❌ Failed to add segment: " + videoResult.message, "ACCENT_ERROR");
            }

        } catch (Exception e) {
            logger.severe("Error in onSegmentCreated: " + e.getMessage());
            mainView.showNotification("❌ Error creating segment: " + e.getMessage(), "ACCENT_ERROR");
        }

        mainView.refreshVideoPreview();
    }

    @Override
    public void onVideoDroppedFromMediaPool(File videoFile, double dropPosition) {
        logger.info("Processing dropped video: " + videoFile.getName() + " at position: " + dropPosition);

        if (!hasAudioTrack.get()) {
            onAudioTrackRequested();
        }

        onSegmentCreated(videoFile, dropPosition, 10.0);
    }

    // Find segment methods
    private TrackSegment findVideoSegmentAtTime(double timelinePosition) {
        if (trackManagers.isEmpty()) return null;

        for (Track track : trackManagers.get(0).getTracks()) {
            for (TrackSegment segment : track.getSegments()) {
                if (timelinePosition >= segment.getStartTime() &&
                        timelinePosition < segment.getEndTime()) {
                    return segment;
                }
            }
        }
        return null;
    }

    private TrackSegment findAudioSegmentAtTime(double timelinePosition) {
        if (!hasAudioTrack.get() || audioTrackManager == null) return null;

        for (Track track : audioTrackManager.getTracks()) {
            for (TrackSegment segment : track.getSegments()) {
                if (timelinePosition >= segment.getStartTime() &&
                        timelinePosition < segment.getEndTime()) {
                    return segment;
                }
            }
        }
        return null;
    }

    private void showBlackScreen() {
        if (currentLoadedVideo != null) {
            playbackController.stop();
            activeVideoSegment = null;
            activeAudioSegment = null;
            currentLoadedVideo = null;
        }

        mainView.showNotification("⚫ Playing through empty space", "ACCENT_INFO");
    }

    private void updateTimelineDuration() {
        double maxDuration = 0;
        for (TrackManager manager : trackManagers) {
            double duration = manager.getTotalDuration();
            if (duration > maxDuration) {
                maxDuration = duration;
            }
        }
        totalTimelineDuration = maxDuration;
    }

    private void updateUIWithCurrentTime() {
        double currentTime = getCurrentPlaybackTime();
        double totalTime = getTotalMediaDuration();

        SwingUtilities.invokeLater(() -> {
            mainView.updateTimeDisplay(currentTime, totalTime);
        });
    }

    private double getCurrentCursorPosition() {
        return cursorPosition;
    }

    // Navigation methods
    @Override
    public void onPreviousFrameRequested() {
        double frameTime = 1.0 / 30.0;
        double newPosition = Math.max(0, currentTimelinePosition - frameTime);
        onCursorMoved(newPosition);
        mainView.showNotification("⏮ Previous frame", "ACCENT_INFO");
    }

    @Override
    public void onNextFrameRequested() {
        double frameTime = 1.0 / 30.0;
        double newPosition = currentTimelinePosition + frameTime;
        onCursorMoved(newPosition);
        mainView.showNotification("⏭ Next frame", "ACCENT_INFO");
    }

    @Override
    public void onReplayRequested() {
        if (activeVideoSegment != null) {
            double segmentStart = activeVideoSegment.getStartTime();
            onCursorMoved(segmentStart);

            SwingUtilities.invokeLater(() -> {
                Timer replayDelay = new Timer(100, e -> {
                    if (!isPlaying.get()) {
                        onPlayPauseToggled();
                    }
                    ((Timer) e.getSource()).stop();
                });
                replayDelay.setRepeats(false);
                replayDelay.start();
            });

            mainView.showNotification("🔄 Replaying from start", "ACCENT_SUCCESS");
        } else {
            mainView.showNotification("⚠️ No active segment to replay", "ACCENT_WARNING");
        }
    }

    // File operations
    @Override
    public void onImportVideoRequested() {
        File file = mainView.showFileChooserForImport();
        if (file != null) {
            mainView.addVideoToMediaPool(file);
            mainView.showNotification("📥 Imported: " + file.getName(), "ACCENT_SUCCESS");
            logger.info("Import: " + file.getAbsolutePath());
        }
    }

    @Override
    public void onExportVideoRequested() {
        // Create project from current timeline state
        Project currentProject = createProjectFromTimeline();

        if (currentProject.getTracks().isEmpty()) {
            mainView.showNotification("❌ No content to export", "ACCENT_WARNING");
            return;
        }

        // Show export dialog
        ExportDialog exportDialog = new ExportDialog((Frame) SwingUtilities.getWindowAncestor(mainView));
        exportDialog.setVisible(true);

        if (exportDialog.isApproved()) {
            File outputFile = exportDialog.getOutputFile();
            ExportSettings settings = exportDialog.getExportSettings();

            // Show progress dialog
            ExportProgressDialog progressDialog = new ExportProgressDialog(
                    (Frame) SwingUtilities.getWindowAncestor(mainView),
                    "Exporting Video"
            );

            // Start export
            ExportService exportService = new ExportService();
            exportService.setProgressListener(progressDialog);

            CompletableFuture<ExportService.ExportResult> exportFuture =
                    exportService.exportProject(currentProject, outputFile, settings);

            progressDialog.setVisible(true);

            exportFuture.whenComplete((result, throwable) -> {
                SwingUtilities.invokeLater(() -> {
                    progressDialog.dispose();

                    if (result != null && result.success) {
                        mainView.showNotification("✅ Export completed: " + outputFile.getName(), "ACCENT_SUCCESS");

                        // Ask if user wants to open the file location
                        int choice = JOptionPane.showConfirmDialog(
                                mainView,
                                "Export completed successfully!\nWould you like to open the output folder?",
                                "Export Complete",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.INFORMATION_MESSAGE
                        );

                        if (choice == JOptionPane.YES_OPTION) {
                            try {
                                Desktop.getDesktop().open(outputFile.getParentFile());
                            } catch (Exception e) {
                                logger.warning("Could not open output folder: " + e.getMessage());
                            }
                        }
                    } else {
                        String errorMsg = result != null ? result.message : "Unknown error occurred";
                        mainView.showNotification("❌ Export failed: " + errorMsg, "ACCENT_ERROR");

                        JOptionPane.showMessageDialog(
                                mainView,
                                "Export failed: " + errorMsg,
                                "Export Error",
                                JOptionPane.ERROR_MESSAGE
                        );
                    }
                });
            });
        }
    }

    private Project createProjectFromTimeline() {
        Project project = new Project("Export Project");

        for (TrackManager trackManager : trackManagers) {
            for (Track track : trackManager.getTracks()) {
                if (!track.getSegments().isEmpty()) {
                    project.addTrack(track);
                }
            }
        }

        return project;
    }

    // Editing operations
    @Override
    public void onZoomInRequested() {
        double newZoom = trackPanel.getZoomLevel() * 1.2;
        trackPanel.setZoomLevel(newZoom);
        mainView.showNotification("🔍 Zoomed in: " + String.format("%.1fx", newZoom), "ACCENT_INFO");
    }

    @Override
    public void onZoomOutRequested() {
        double newZoom = trackPanel.getZoomLevel() / 1.2;
        trackPanel.setZoomLevel(newZoom);
        mainView.showNotification("🔍 Zoomed out: " + String.format("%.1fx", newZoom), "ACCENT_INFO");
    }

    @Override
    public void onCutRequested() {
        double cursorPos = getCurrentCursorPosition();

        // Cut both video and audio tracks
        TrackManager.SegmentOperation videoResult = trackManagers.get(0).cutAllTracksAtTime(cursorPos);
        TrackManager.SegmentOperation audioResult = null;

        if (hasAudioTrack.get()) {
            audioResult = audioTrackManager.cutAllTracksAtTime(cursorPos);
        }

        if (videoResult.success) {
            String message = hasAudioTrack.get() && audioResult != null && audioResult.success ?
                    "✂️ Cut both audio and video tracks" :
                    "✂️ Cut video track";
            mainView.showNotification(message, "ACCENT_SUCCESS");
        } else {
            mainView.showNotification("❌ Cut failed: " + videoResult.message, "ACCENT_ERROR");
        }

        mainView.refreshVideoPreview();
    }

    @Override
    public void onDeleteRequested() {
        // Delete from both video and audio tracks
        TrackManager.SegmentOperation videoResult = trackManagers.get(0).deleteSelectedSegments();
        TrackManager.SegmentOperation audioResult = null;

        if (hasAudioTrack.get()) {
            audioResult = audioTrackManager.deleteSelectedSegments();
        }

        if (videoResult.success) {
            String message = hasAudioTrack.get() && audioResult != null && audioResult.success ?
                    "🗑️ Deleted from audio and video tracks" :
                    "🗑️ Deleted from video track";
            mainView.showNotification(message, "ACCENT_SUCCESS");

            // Clear active segments if they were deleted
            if (activeVideoSegment != null && trackManagers.get(0).isSelected(activeVideoSegment)) {
                activeVideoSegment = null;
                activeAudioSegment = null;
                currentLoadedVideo = null;
                playbackController.stop();
            }

            updateTimelineDuration();
        } else {
            mainView.showNotification("❌ Delete failed: " + videoResult.message, "ACCENT_ERROR");
        }

        mainView.refreshVideoPreview();
    }

    @Override
    public double getCurrentPlaybackTime() {
        if (activeVideoSegment != null) {
            return currentTimelinePosition;
        }
        return playbackController.getCurrentTime();
    }

    @Override
    public double getTotalMediaDuration() {
        return Math.max(totalTimelineDuration, playbackController.getDuration());
    }

    @Override
    public boolean isCurrentlyPlaying() {
        return isPlaying.get();
    }

    @Override
    public void onViewChanged(double zoomLevel, double viewStartTime, double viewEndTime) {
        logger.fine("View changed: zoom=" + zoomLevel + ", start=" + viewStartTime + ", end=" + viewEndTime);
    }

    // Utility methods
    public double getCurrentTimelinePosition() {
        return currentTimelinePosition;
    }

    public boolean hasAudioTrack() {
        return hasAudioTrack.get();
    }

    public void setVolume(double volume) {
        onVolumeChanged(volume);
    }

    public void setMuted(boolean muted) {
        if (muted != isMuted.get()) {
            onMuteToggled();
        }
    }

    /**
     * FIXED: Enhanced shutdown with timer cleanup
     */
    public void shutdown() {
        if (uiUpdateTimer != null) {
            uiUpdateTimer.stop();
        }

        // FIXED: Also cleanup cursor sync timer
        if (cursorSyncTimer != null) {
            cursorSyncTimer.stop();
        }

        playbackController.shutdown();
        logger.info("Enhanced MainController shutdown complete");
    }
}
