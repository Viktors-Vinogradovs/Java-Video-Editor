package com.videoeditor.ui.controllers;

import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import javax.swing.SwingUtilities;
import java.util.logging.Logger;
import java.util.function.Consumer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class PlaybackController {
    private static final Logger logger = Logger.getLogger(PlaybackController.class.getName());

    private final EmbeddedMediaPlayer mediaPlayer;
    private final ScheduledExecutorService timeUpdateExecutor;
    private final AtomicBoolean isPlaying = new AtomicBoolean(false);
    private final AtomicBoolean isUserSeeking = new AtomicBoolean(false);

    // Callbacks
    private Consumer<Double> onTimeChangedCallback;
    private Consumer<Boolean> onPlayStateChangedCallback;
    private Runnable onMediaLoadedCallback;

    // Current state
    private String currentMediaPath;
    private long mediaDurationMs = 0;
    private volatile long lastKnownTimeMs = 0;

    // Seeking management
    private volatile long lastSeekTime = 0;
    private static final long SEEK_DEBOUNCE_MS = 100;

    public PlaybackController(EmbeddedMediaPlayer mediaPlayer) {
        this.mediaPlayer = mediaPlayer;
        this.timeUpdateExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PlaybackController-TimeUpdater");
            t.setDaemon(true);
            return t;
        });

        setupMediaPlayerEvents();
        startTimeUpdateLoop();

        logger.info("PlaybackController initialized with proper event handling");
    }

    private void setupMediaPlayerEvents() {
        mediaPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void mediaChanged(uk.co.caprica.vlcj.player.base.MediaPlayer mediaPlayer,
                                     uk.co.caprica.vlcj.media.MediaRef media) {
                logger.info("Media changed event received");
            }

            @Override
            public void playing(uk.co.caprica.vlcj.player.base.MediaPlayer mediaPlayer) {
                logger.info("Playing event received");
                isPlaying.set(true);
                SwingUtilities.invokeLater(() -> {
                    if (onPlayStateChangedCallback != null) {
                        onPlayStateChangedCallback.accept(true);
                    }
                });
            }

            @Override
            public void paused(uk.co.caprica.vlcj.player.base.MediaPlayer mediaPlayer) {
                logger.info("Paused event received");
                isPlaying.set(false);
                SwingUtilities.invokeLater(() -> {
                    if (onPlayStateChangedCallback != null) {
                        onPlayStateChangedCallback.accept(false);
                    }
                });
            }

            @Override
            public void stopped(uk.co.caprica.vlcj.player.base.MediaPlayer mediaPlayer) {
                logger.info("Stopped event received");
                isPlaying.set(false);
                SwingUtilities.invokeLater(() -> {
                    if (onPlayStateChangedCallback != null) {
                        onPlayStateChangedCallback.accept(false);
                    }
                });
            }

            @Override
            public void timeChanged(uk.co.caprica.vlcj.player.base.MediaPlayer mediaPlayer, long newTime) {
                // Only update if user is not actively seeking
                if (!isUserSeeking.get()) {
                    lastKnownTimeMs = newTime;

                    // Throttle UI updates to avoid overwhelming the EDT
                    SwingUtilities.invokeLater(() -> {
                        if (onTimeChangedCallback != null && !isUserSeeking.get()) {
                            onTimeChangedCallback.accept(newTime / 1000.0);
                        }
                    });
                }
            }

            @Override
            public void lengthChanged(uk.co.caprica.vlcj.player.base.MediaPlayer mediaPlayer, long newLength) {
                mediaDurationMs = newLength;
                logger.info("Media duration: " + (newLength / 1000.0) + " seconds");

                SwingUtilities.invokeLater(() -> {
                    if (onMediaLoadedCallback != null) {
                        onMediaLoadedCallback.run();
                    }
                });
            }

            @Override
            public void error(uk.co.caprica.vlcj.player.base.MediaPlayer mediaPlayer) {
                logger.severe("Media player error occurred");
                isPlaying.set(false);
            }
        });
    }

    private void startTimeUpdateLoop() {
        // Backup time update mechanism in case VLC events are not reliable
        timeUpdateExecutor.scheduleAtFixedRate(() -> {
            try {
                if (isPlaying.get() && !isUserSeeking.get() && mediaPlayer != null) {
                    long currentTime = mediaPlayer.status().time();

                    // Only update if time has actually changed significantly
                    if (Math.abs(currentTime - lastKnownTimeMs) > 100) { // 100ms threshold
                        lastKnownTimeMs = currentTime;

                        SwingUtilities.invokeLater(() -> {
                            if (onTimeChangedCallback != null && !isUserSeeking.get()) {
                                onTimeChangedCallback.accept(currentTime / 1000.0);
                            }
                        });
                    }
                }
            } catch (Exception e) {
                logger.fine("Error in time update loop: " + e.getMessage());
            }
        }, 100, 100, TimeUnit.MILLISECONDS);
    }

    /**
     * Load and prepare media for playback
     */
    public void loadMedia(String filePath) {
        if (filePath == null || filePath.equals(currentMediaPath)) {
            return;
        }

        logger.info("Loading media: " + filePath);

        // Stop current playback
        if (currentMediaPath != null) {
            stop();
        }

        currentMediaPath = filePath;

        try {
            // Prepare media but don't start playing
            boolean success = mediaPlayer.media().prepare(filePath);
            if (success) {
                logger.info("Media prepared successfully: " + filePath);

                // Give VLC time to initialize
                SwingUtilities.invokeLater(() -> {
                    try {
                        Thread.sleep(200);
                        // Ensure we start paused
                        if (mediaPlayer.status().isPlaying()) {
                            mediaPlayer.controls().pause();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            } else {
                logger.warning("Failed to prepare media: " + filePath);
            }
        } catch (Exception e) {
            logger.severe("Exception loading media: " + e.getMessage());
            currentMediaPath = null;
        }
    }

    /**
     * Start/resume playback
     */
    public void play() {
        if (currentMediaPath == null) {
            logger.warning("No media loaded, cannot play");
            return;
        }

        try {
            if (!mediaPlayer.status().isPlaying()) {
                mediaPlayer.controls().play();
                logger.info("Started playback");
            }
        } catch (Exception e) {
            logger.warning("Error starting playback: " + e.getMessage());
        }
    }

    /**
     * Pause playback
     */
    public void pause() {
        try {
            if (mediaPlayer.status().isPlaying()) {
                mediaPlayer.controls().pause();
                logger.info("Paused playback");
            }
        } catch (Exception e) {
            logger.warning("Error pausing playback: " + e.getMessage());
        }
    }

    /**
     * Stop playback completely
     */
    public void stop() {
        try {
            mediaPlayer.controls().stop();
            isPlaying.set(false);
            lastKnownTimeMs = 0;
            logger.info("Stopped playback");
        } catch (Exception e) {
            logger.warning("Error stopping playback: " + e.getMessage());
        }
    }

    /**
     * Seek to specific time with debouncing
     */
    public void seekTo(double timeSeconds) {
        long currentTime = System.currentTimeMillis();

        // Debounce rapid seek requests
        if (currentTime - lastSeekTime < SEEK_DEBOUNCE_MS) {
            return;
        }

        lastSeekTime = currentTime;

        if (currentMediaPath == null) {
            logger.fine("No media loaded, cannot seek");
            return;
        }

        long timeMs = (long) (timeSeconds * 1000);

        // Clamp to valid range
        timeMs = Math.max(0, Math.min(timeMs, mediaDurationMs));

        isUserSeeking.set(true);

        try {
            mediaPlayer.controls().setTime(timeMs);
            lastKnownTimeMs = timeMs;
            logger.fine("Seeked to: " + timeSeconds + "s");

            // Clear seeking flag after a short delay
            SwingUtilities.invokeLater(() -> {
                try {
                    Thread.sleep(150);
                    isUserSeeking.set(false);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

        } catch (Exception e) {
            logger.fine("Error seeking: " + e.getMessage());
            isUserSeeking.set(false);
        }
    }

    /**
     * Get current playback time in seconds
     */
    public double getCurrentTime() {
        try {
            if (isUserSeeking.get()) {
                return lastKnownTimeMs / 1000.0;
            }

            long currentTime = mediaPlayer.status().time();
            return Math.max(0, currentTime / 1000.0);
        } catch (Exception e) {
            return lastKnownTimeMs / 1000.0;
        }
    }

    /**
     * Get media duration in seconds
     */
    public double getDuration() {
        return Math.max(0, mediaDurationMs / 1000.0);
    }

    /**
     * Check if media is loaded and ready
     */
    public boolean hasMedia() {
        return currentMediaPath != null && mediaDurationMs > 0;
    }

    /**
     * Set volume (0.0 to 1.0)
     */
    public void setVolume(double volume) {
        try {
            int vlcVolume = (int) (Math.max(0, Math.min(1, volume)) * 100);
            mediaPlayer.audio().setVolume(vlcVolume);
        } catch (Exception e) {
            logger.warning("Error setting volume: " + e.getMessage());
        }
    }

    /**
     * Set mute state
     */
    public void setMuted(boolean muted) {
        try {
            mediaPlayer.audio().setMute(muted);
        } catch (Exception e) {
            logger.warning("Error setting mute: " + e.getMessage());
        }
    }

    // Callback setters
    public void setOnTimeChangedCallback(Consumer<Double> callback) {
        this.onTimeChangedCallback = callback;
    }

    public void setOnPlayStateChangedCallback(Consumer<Boolean> callback) {
        this.onPlayStateChangedCallback = callback;
    }

    public void setOnMediaLoadedCallback(Runnable callback) {
        this.onMediaLoadedCallback = callback;
    }

    /**
     * Cleanup resources
     */
    public void shutdown() {
        timeUpdateExecutor.shutdown();
        try {
            if (!timeUpdateExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                timeUpdateExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            timeUpdateExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        stop();
        logger.info("PlaybackController shutdown complete");
    }
}