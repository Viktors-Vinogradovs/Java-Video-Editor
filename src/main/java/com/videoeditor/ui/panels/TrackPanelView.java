package com.videoeditor.ui.panels;

import com.videoeditor.config.UIConstants;
import com.videoeditor.ui.events.DragAndDropHandler;
import com.videoeditor.ui.events.VideoEditorListener;
import com.videoeditor.ui.utils.TimeUtils;
import com.videoeditor.ui.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Clean TrackPanelView without distracting audio headers
 */
public class TrackPanelView extends JPanel {
    private static final Logger logger = Logger.getLogger(TrackPanelView.class.getName());

    private VideoEditorListener listener;
    private final AtomicBoolean isPlaying = new AtomicBoolean(false);

    // Clean audio track support (no visual clutter)
    private boolean hasAudioTrack = false;

    // Smooth cursor animation
    private Timer cursorAnimationTimer;
    private double currentCursorPosition = 0.0;
    private double targetCursorPosition = 0.0;
    private static final double CURSOR_SMOOTH_FACTOR = 0.15;

    public TrackPanelView() {
        super(new BorderLayout());
        setOpaque(false);
        setPreferredSize(new Dimension(UIConstants.TRACK_PANEL_SIZE.width,
                UIConstants.TRACK_PANEL_SIZE.height + 100));
        setBorder(UIUtils.createModernTitledBorder("Timeline"));

        setupSmoothCursorAnimation();
        logger.info("Clean TrackPanelView initialized");
    }

    public void initialize(TrackPanel trackPanelComponent, VideoEditorListener listener) {
        this.listener = listener;

        if (trackPanelComponent == null) {
            add(createCleanEmptyTimelinePanel(), BorderLayout.CENTER);
        } else {
            // Create clean layout without audio headers
            JPanel trackContainer = createCleanTrackContainer(trackPanelComponent);
            add(trackContainer, BorderLayout.CENTER);
            setupTimelineDropTarget(trackPanelComponent);
        }

        logger.info("Clean TrackPanelView initialized");
    }

    private void setupSmoothCursorAnimation() {
        // FIXED: Simple cursor animation without isPlaying references
        cursorAnimationTimer = new Timer(16, e -> { // 60 FPS for smooth animation
            if (Math.abs(targetCursorPosition - currentCursorPosition) > 0.001) {
                double difference = targetCursorPosition - currentCursorPosition;
                currentCursorPosition += difference * CURSOR_SMOOTH_FACTOR;
                repaint();
            }
        });
        cursorAnimationTimer.start();
    }
    private JPanel createCleanTrackContainer(TrackPanel trackPanelComponent) {
        JPanel container = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                UIUtils.setupRenderingHints(g2d);

                // Clean background without audio indicators
                GradientPaint gradient = new GradientPaint(
                        0, 0, UIConstants.BACKGROUND_SECONDARY,
                        0, getHeight(), UIConstants.BACKGROUND_PRIMARY
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                // Clean grid lines
                drawCleanGrid(g2d);

                // Smooth cursor rendering
                drawSmoothCursor(g2d);
            }
        };

        container.setLayout(new BorderLayout());
        container.setOpaque(false);

        // Add the main track panel without extra headers
        container.add(trackPanelComponent, BorderLayout.CENTER);

        return container;
    }

    private void drawCleanGrid(Graphics2D g2d) {
        // Clean grid with minimal visual noise
        g2d.setColor(UIConstants.withAlpha(UIConstants.ACCENT_PRIMARY, 15)); // More subtle
        g2d.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                0, new float[]{2f, 4f}, 0));

        int gridSpacing = 50;
        int width = getWidth();
        int height = getHeight();

        // Vertical grid lines only where needed
        for (int x = gridSpacing; x < width; x += gridSpacing) {
            g2d.drawLine(x, 0, x, height);

            // Time markers every 5 lines (more subtle)
            if ((x / gridSpacing) % 5 == 0) {
                g2d.setColor(UIConstants.withAlpha(UIConstants.ACCENT_PRIMARY, 40)); // Very subtle
                g2d.setStroke(new BasicStroke(1f));
                g2d.drawLine(x, 0, x, 20); // Shorter markers

                // Smaller time labels
                double timeSeconds = ((double) x / gridSpacing) * 0.5;
                String timeLabel = String.format("%.1fs", timeSeconds);
                g2d.setFont(UIUtils.createFont(9, Font.PLAIN)); // Smaller font
                g2d.setColor(UIConstants.withAlpha(UIConstants.TEXT_SECONDARY, 120)); // More subtle
                FontMetrics fm = g2d.getFontMetrics();
                int labelWidth = fm.stringWidth(timeLabel);
                g2d.drawString(timeLabel, x - labelWidth/2, 15);
            }

            g2d.setColor(UIConstants.withAlpha(UIConstants.ACCENT_PRIMARY, 15));
            g2d.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    0, new float[]{2f, 4f}, 0));
        }
    }

    private void drawSmoothCursor(Graphics2D g2d) {
        int cursorX = (int) (currentCursorPosition * 50);

        if (cursorX >= 0 && cursorX <= getWidth()) {
            // Minimal outer glow
            g2d.setColor(UIConstants.withAlpha(UIConstants.TIMELINE_CURSOR, 40));
            g2d.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawLine(cursorX, 0, cursorX, getHeight());

            // Main cursor line
            g2d.setColor(UIConstants.TIMELINE_CURSOR);
            g2d.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawLine(cursorX, 0, cursorX, getHeight());

            // Clean cursor head
            g2d.setColor(UIConstants.TIMELINE_CURSOR);
            int headSize = 10;
            g2d.fillPolygon(
                    new int[]{cursorX - headSize/2, cursorX + headSize/2, cursorX},
                    new int[]{5, 5, 5 + headSize},
                    3
            );

            // Clean cursor time display
            String timeText = TimeUtils.formatTime(currentCursorPosition);
            g2d.setColor(Color.WHITE);
            g2d.setFont(UIUtils.createFont(10, Font.BOLD));
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(timeText);

            // Background for time text
            g2d.setColor(new Color(0, 0, 0, 160));
            g2d.fillRoundRect(cursorX - textWidth/2 - 3, 8, textWidth + 6, 14, 3, 3);

            // Time text
            g2d.setColor(Color.WHITE);
            g2d.drawString(timeText, cursorX - textWidth/2, 18);
        }
    }


    private JPanel createCleanEmptyTimelinePanel() {
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                UIUtils.setupRenderingHints(g2d);

                // Clean background
                GradientPaint gradient = new GradientPaint(
                        0, 0, UIConstants.BACKGROUND_MEDIUM,
                        0, getHeight(), UIConstants.BACKGROUND_PRIMARY
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                // Clean dashed border
                g2d.setColor(UIConstants.ACCENT_PRIMARY);
                g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                        0, new float[]{10, 10}, 0));
                g2d.drawRoundRect(30, 30, getWidth() - 60, getHeight() - 60, 16, 16);
            }
        };
        panel.setOpaque(false);

        // Clean info label without audio references
        JLabel infoLabel = new JLabel("<html><div style='text-align: center;'>" +
                "<h2 style='color: #ffffff; margin-bottom: 15px;'>🎬 Ready to Create</h2>" +
                "<p style='color: #aeaeb2; font-size: 16px; margin-bottom: 10px;'>" +
                "Drag media files here to start your timeline</p>" +
                "<p style='color: #aeaeb2; font-size: 13px;'>" +
                "Use <strong>Ctrl+I</strong> to import • <strong>Space</strong> to play" +
                "</p>" +
                "</div></html>", SwingConstants.CENTER);
        infoLabel.setFont(UIUtils.createFont(15, Font.PLAIN));

        panel.add(infoLabel, BorderLayout.CENTER);
        return panel;
    }

    private void setupTimelineDropTarget(TrackPanel trackPanelComponent) {
        new DragAndDropHandler(trackPanelComponent, (file, dropPosition) -> {
            logger.info("Clean drop: " + file.getAbsolutePath() + " at position: " + dropPosition);

            if (listener != null) {
                listener.onVideoDroppedFromMediaPool(file, dropPosition);

                // Silently enable audio track without visual indicators
                enableAudioTrack();

                // Clean notification
                Component mainView = SwingUtilities.getAncestorOfClass(MainView.class, trackPanelComponent);
                if (mainView instanceof MainView) {
                    ((MainView) mainView).showNotification(
                            "🎬 Added to timeline: " + file.getName(),
                            "ACCENT_SUCCESS"
                    );
                }
            }
        });
    }

    // Clean audio track support without visual clutter
    public void enableAudioTrack() {
        if (!hasAudioTrack) {
            hasAudioTrack = true;
            // No visual changes - just enable functionality silently

            logger.info("Audio track enabled silently");

            // Notify listener about audio track creation
            if (listener != null) {
                listener.onAudioTrackRequested();
            }
        }
    }

    // Clean cursor position methods with smooth animation
    public void updateCursorPosition(double position) {
        this.targetCursorPosition = position;
        // Always update immediately for better synchronization
        this.currentCursorPosition = position;
        repaintCursorArea();
    }


    // Clean playback controls
    public void togglePlayPause() {
        boolean wasPlaying = isPlaying.get();
        isPlaying.set(!wasPlaying);

        if (listener != null) {
            listener.onPlayPauseToggled();
        }

        // Clean feedback
        Component mainView = SwingUtilities.getAncestorOfClass(MainView.class, this);
        if (mainView instanceof MainView) {
            String status = isPlaying.get() ? "▶ Playing" : "⏸ Paused";
            ((MainView) mainView).showNotification(status, "ACCENT_INFO");
        }

        logger.info("Playback toggled: " + (isPlaying.get() ? "playing" : "paused"));
    }

    // Performance optimization
    public void repaintCursorArea() {
        int cursorX = (int) (currentCursorPosition * 50);
        repaint(Math.max(0, cursorX - 30), 0, 60, getHeight());
    }


    // Clean audio level visualization (minimal)
    public void updateAudioLevels(float leftLevel, float rightLevel) {
        if (hasAudioTrack) {
            // Minimal logging without visual indicators
            logger.fine("Audio levels - L: " + leftLevel + " R: " + rightLevel);
        }
    }

    // Cleanup resources
    public void dispose() {
        if (cursorAnimationTimer != null) {
            cursorAnimationTimer.stop();
        }
        logger.info("Clean TrackPanelView disposed");
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        UIUtils.setupRenderingHints(g2d);

    }
}