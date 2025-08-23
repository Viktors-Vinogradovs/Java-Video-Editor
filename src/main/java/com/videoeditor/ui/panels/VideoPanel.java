package com.videoeditor.ui.panels;

import com.videoeditor.config.UIConstants;
import com.videoeditor.ui.utils.UIUtils;
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.logging.Logger;

/**
 * Simplified video panel with clean interface and no complex loading bars
 */
public class VideoPanel extends JPanel {
    private static final Logger logger = Logger.getLogger(VideoPanel.class.getName());

    private final EmbeddedMediaPlayerComponent mediaPlayerComponent;
    private JLabel videoInfoLabel;

    public VideoPanel() {
        super(new BorderLayout());
        logger.info("Initializing simplified VideoPanel");

        mediaPlayerComponent = createSimpleMediaPlayer();
        setupSimpleVideoPanel();

        logger.info("Simplified VideoPanel initialized successfully");
    }

    private EmbeddedMediaPlayerComponent createSimpleMediaPlayer() {
        try {
            EmbeddedMediaPlayerComponent component = new EmbeddedMediaPlayerComponent();

            // Simple audio configuration
            SwingUtilities.invokeLater(() -> {
                try {
                    EmbeddedMediaPlayer player = component.mediaPlayer();
                    if (player != null) {
                        player.audio().setMute(false);
                        player.audio().setVolume(80);
                        logger.info("Audio configured for video player");
                    }
                } catch (Exception e) {
                    logger.warning("Could not configure audio: " + e.getMessage());
                }
            });

            return component;

        } catch (Exception e) {
            logger.severe("Failed to create media player: " + e.getMessage());
            throw new RuntimeException("Could not create media player component", e);
        }
    }

    private void setupSimpleVideoPanel() {
        setOpaque(false);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Create clean video container
        JPanel videoContainer = getjPanel();

        // Add media player to container
        videoContainer.add(mediaPlayerComponent, BorderLayout.CENTER);

        // Simple video info overlay
        videoInfoLabel = createSimpleInfoLabel();

        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setOpaque(false);
        infoPanel.setBorder(new EmptyBorder(5, 8, 5, 8));
        infoPanel.add(videoInfoLabel, BorderLayout.WEST);

        // Layout
        add(infoPanel, BorderLayout.NORTH);
        add(videoContainer, BorderLayout.CENTER);

        setBackground(null);
        setOpaque(false);
    }

    private JPanel getjPanel() {
        JPanel videoContainer = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Clean black background for video
                g2d.setColor(Color.BLACK);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

                // Subtle border
                g2d.setColor(UIConstants.BORDER_DEFAULT);
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            }
        };
        videoContainer.setOpaque(false);
        return videoContainer;
    }

    private JLabel createSimpleInfoLabel() {
        JLabel label = new JLabel("Ready for Video") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                // Simple text with subtle shadow
                g2d.setColor(new Color(0, 0, 0, 100));
                g2d.drawString(getText(), 1, getHeight() - 1);

                g2d.setColor(getForeground());
                g2d.drawString(getText(), 0, getHeight() - 2);
            }
        };

        label.setForeground(UIConstants.TEXT_PRIMARY);
        label.setFont(UIUtils.createFont(12, Font.BOLD));
        return label;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Simple gradient background
        GradientPaint gradient = new GradientPaint(
                0, 0, UIConstants.BACKGROUND_SECONDARY,
                0, getHeight(), UIConstants.BACKGROUND_PRIMARY
        );
        g2d.setPaint(gradient);
        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

        // Minimal border
        g2d.setColor(UIConstants.BORDER_DEFAULT);
        g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
    }

    /**
     * Update video info with simple display
     */
    public void updateVideoInfo(String title, String resolution, String format) {
        SwingUtilities.invokeLater(() -> {
            if (videoInfoLabel != null) {
                String displayText;
                if (title != null && !title.equals("Ready for Video")) {
                    // Show basic file info with truncated title
                    String shortTitle = (title.length() > 30) ? title.substring(0, 27) + "..." : title;
                    displayText = String.format("%s • %s • %s",
                            shortTitle,
                            resolution != null ? resolution : "Unknown",
                            format != null ? format : "");
                } else {
                    displayText = "Ready for Video";
                }

                videoInfoLabel.setText(displayText);
                videoInfoLabel.setToolTipText(title); // Full title on hover
            }
        });
    }

    /**
     * Get the embedded media player
     */
    public EmbeddedMediaPlayer getMediaPlayer() {
        if (mediaPlayerComponent != null) {
            return mediaPlayerComponent.mediaPlayer();
        }
        logger.warning("Media player component is null");
        return null;
    }

    /**
     * Get the media player component
     */
    public EmbeddedMediaPlayerComponent getMediaPlayerComponent() {
        return mediaPlayerComponent;
    }

    /**
     * Clean up resources
     */
    public void dispose() {
        try {
            if (mediaPlayerComponent != null) {
                mediaPlayerComponent.release();
                logger.info("Media player component released");
            }
        } catch (Exception e) {
            logger.warning("Error disposing video panel: " + e.getMessage());
        }
    }
    }