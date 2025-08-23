package com.videoeditor.ui.panels;

import com.videoeditor.ui.events.VideoEditorListener;
import com.videoeditor.ui.utils.UIUtils;
import com.videoeditor.ui.utils.TimeUtils;
import com.videoeditor.config.AppConstants;
import com.videoeditor.config.UIConstants;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.logging.Logger;

/**
 * Complete simplified MainView with responsive layout and streamlined controls
 */
public class MainView extends JFrame {
    private static final Logger logger = Logger.getLogger(MainView.class.getName());

    // Core panels
    private JPanel videoPanel;
    private JPanel timelinePanel;
    private JPanel toolsPanel;
    private JPanel statusPanel;

    // UI Components
    private JTextField statusField;
    private JLabel statusIcon;
    private Timer statusTimer;
    public VideoEditorListener listener;

    // Simplified video controls
    private JButton playPauseButton;
    private JLabel timeLabel;
    private SimpleSeekBar seekBar;
    private JSlider volumeSlider;
    private Timer playbackTimer;
    private boolean isPlaying = false;

    // Layout management
    private boolean isCompactMode = false;

    public MainView() {
        logger.info("Initializing simplified VideoEditor MainView");
        setupResponsiveWindow();
        initializePanels();
        setupKeyboardShortcuts();
        setupStatusSystem();
        logger.info("MainView initialization completed");
    }

    private void setupResponsiveWindow() {
        setTitle("VideoEditor Pro");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Responsive sizing
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = Math.min(1400, (int)(screenSize.width * 0.85));
        int height = Math.min(900, (int)(screenSize.height * 0.85));

        setSize(width, height);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(1000, 600));

        getContentPane().setBackground(UIConstants.BACKGROUND_PRIMARY);
        setLayout(new BorderLayout());

        // Add resize listener for responsive behavior
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                handleResize();
            }
        });
    }

    private void handleResize() {
        Dimension currentSize = getSize();
        boolean shouldBeCompact = currentSize.width < 1200; // Direct threshold value

        if (shouldBeCompact != isCompactMode) {
            isCompactMode = shouldBeCompact;
            adjustLayoutForSize();
            updateComponentsForCompactMode();
        }

    }

    private void adjustLayoutForSize() {
        if (isCompactMode) {
            layoutComponentsCompact();
        } else {
            layoutComponentsNormal();
        }
        revalidate();
        repaint();
    }

    private void initializePanels() {
        videoPanel = new VideoPanel();
        timelinePanel = new TrackPanelView();
        toolsPanel = new MediaPoolPanel();
        statusPanel = createSimpleStatusPanel();

        layoutComponentsNormal();
    }

    private void layoutComponentsNormal() {
        getContentPane().removeAll();

        // Main content area
        JPanel mainContent = new JPanel(new BorderLayout(5, 5));
        mainContent.setOpaque(false);
        mainContent.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Top: Video section with simplified controls
        JPanel videoSection = createSimpleVideoSection();

        // Center: Timeline
        JPanel timelineSection = createTimelineSection();

        // Right: Compact tools panel
        JPanel rightPanel = createCompactRightPanel();

        // Main layout
        mainContent.add(videoSection, BorderLayout.NORTH);
        mainContent.add(timelineSection, BorderLayout.CENTER);
        mainContent.add(rightPanel, BorderLayout.EAST);

        add(mainContent, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);
    }

    private void layoutComponentsCompact() {
        getContentPane().removeAll();

        // Create tabbed interface for compact mode
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(UIConstants.BACKGROUND_PRIMARY);
        tabbedPane.setForeground(UIConstants.TEXT_PRIMARY);

        // Video tab
        JPanel videoTab = new JPanel(new BorderLayout());
        videoTab.setOpaque(false);
        videoTab.add(createSimpleVideoSection(), BorderLayout.CENTER);
        tabbedPane.addTab("🎬 Video", videoTab);

        // Timeline tab
        JPanel timelineTab = new JPanel(new BorderLayout());
        timelineTab.setOpaque(false);
        timelineTab.add(createTimelineSection(), BorderLayout.CENTER);
        tabbedPane.addTab("⏱ Timeline", timelineTab);

        // Media tab
        JPanel mediaTab = new JPanel(new BorderLayout());
        mediaTab.setOpaque(false);
        mediaTab.add(toolsPanel, BorderLayout.CENTER);
        tabbedPane.addTab("📁 Media", mediaTab);

        add(tabbedPane, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);
    }

    private JPanel createSimpleVideoSection() {
        JPanel section = new JPanel(new BorderLayout());
        section.setOpaque(false);
        section.setPreferredSize(new Dimension(0, isCompactMode ? 280 : 340));

        // Video player container
        JPanel videoContainer = new JPanel(new BorderLayout());
        videoContainer.setOpaque(false);
        videoContainer.setBorder(BorderFactory.createEmptyBorder(10, 15, 5, 15));
        videoContainer.add(videoPanel, BorderLayout.CENTER);

        // Simplified control bar
        JPanel controlBar = createSimpleControlBar();

        section.add(videoContainer, BorderLayout.CENTER);
        section.add(controlBar, BorderLayout.SOUTH);

        return section;
    }

    private JPanel createSimpleControlBar() {
        JPanel controlBar = new JPanel(new BorderLayout());
        controlBar.setOpaque(false);
        controlBar.setPreferredSize(new Dimension(0, 55));
        controlBar.setBorder(BorderFactory.createEmptyBorder(8, 15, 12, 15));

        // Left: Enhanced play controls with better spacing
        JPanel playControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        playControls.setOpaque(false);

        playPauseButton = createEnhancedControlButton("▶", "Play/Pause (Space)", e -> togglePlayPause());
        JButton stopButton = createEnhancedControlButton("⏹", "Stop", e -> stopPlayback());
        JButton prevButton = createEnhancedControlButton("⏮", "Previous 10s (J)", e -> seekRelative(-10));
        JButton nextButton = createEnhancedControlButton("⏭", "Next 10s (L)", e -> seekRelative(10));

        playControls.add(prevButton);
        playControls.add(playPauseButton);
        playControls.add(stopButton);
        playControls.add(nextButton);

        // Center: Enhanced time display and seek bar
        JPanel centerPanel = new JPanel(new BorderLayout(12, 0));
        centerPanel.setOpaque(false);

        timeLabel = new JLabel("00:00 / 00:00");
        timeLabel.setForeground(UIConstants.TEXT_PRIMARY);
        timeLabel.setFont(UIUtils.createFont(12, Font.BOLD));
        timeLabel.setPreferredSize(new Dimension(90, 30));
        timeLabel.setHorizontalAlignment(SwingConstants.CENTER);

        seekBar = new SimpleSeekBar();
        seekBar.addSeekListener(this::seekToPosition);

        centerPanel.add(timeLabel, BorderLayout.WEST);
        centerPanel.add(seekBar, BorderLayout.CENTER);

        // Right: Enhanced volume controls
        JPanel volumePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        volumePanel.setOpaque(false);

        JLabel volumeIcon = getjLabel();

        volumeSlider = new JSlider(0, 100, 80) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                UIUtils.setupRenderingHints(g2d);

                // Custom slider styling
                int width = getWidth();
                int height = getHeight();
                int trackHeight = 4;
                int trackY = (height - trackHeight) / 2;

                // Track background
                g2d.setColor(UIConstants.SURFACE_LOW);
                g2d.fillRoundRect(0, trackY, width, trackHeight, trackHeight, trackHeight);

                // Track fill
                int fillWidth = (int) ((double) getValue() / getMaximum() * width);
                g2d.setColor(UIConstants.ACCENT_PRIMARY);
                g2d.fillRoundRect(0, trackY, fillWidth, trackHeight, trackHeight, trackHeight);

                // Thumb
                g2d.setColor(Color.WHITE);
                int thumbX = fillWidth - 6;
                g2d.fillOval(thumbX, trackY - 3, 12, 10);
            }
        };
        volumeSlider.setPreferredSize(new Dimension(70, 20));
        volumeSlider.setOpaque(false);
        volumeSlider.addChangeListener(e -> {
            if (listener != null) {
                listener.onVolumeChanged(volumeSlider.getValue() / 100.0);
            }
        });

        volumePanel.add(volumeIcon);
        volumePanel.add(volumeSlider);

        controlBar.add(playControls, BorderLayout.WEST);
        controlBar.add(centerPanel, BorderLayout.CENTER);
        controlBar.add(volumePanel, BorderLayout.EAST);

        return controlBar;
    }

    private static JLabel getjLabel() {
        JLabel volumeIcon = new JLabel("🔊") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                UIUtils.setupRenderingHints(g2d);

                // Background for better visibility
                g2d.setColor(UIConstants.withAlpha(UIConstants.SURFACE_MEDIUM, 80));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);

                super.paintComponent(g);
            }
        };
        volumeIcon.setPreferredSize(new Dimension(24, 24));
        volumeIcon.setHorizontalAlignment(SwingConstants.CENTER);
        return volumeIcon;
    }

    private JButton createEnhancedControlButton(String text, String tooltip, java.awt.event.ActionListener action) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                UIUtils.setupRenderingHints(g2d);

                // Enhanced background with better visibility
                if (getModel().isPressed()) {
                    g2d.setColor(UIConstants.withAlpha(UIConstants.ACCENT_PRIMARY, 50));
                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                } else if (getModel().isRollover()) {
                    g2d.setColor(UIConstants.withAlpha(UIConstants.ACCENT_PRIMARY, 25));
                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                }

                // Subtle border for definition
                g2d.setColor(UIConstants.withAlpha(UIConstants.BORDER_DEFAULT, 40));
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);

                // Text with shadow for better contrast
                g2d.setColor(new Color(0, 0, 0, 80));
                g2d.setFont(getFont());
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent()) / 2 - 1;
                g2d.drawString(getText(), x + 1, y + 1);

                // Main text
                g2d.setColor(UIConstants.TEXT_PRIMARY);
                g2d.drawString(getText(), x, y);
            }
        };

        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(36, 36));
        button.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        button.setToolTipText(tooltip);
        button.addActionListener(action);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return button;
    }

    // Enhanced seek bar component with better visibility
    /**
     * Simplified seek bar component with enhanced visibility
     */
    private static class SimpleSeekBar extends JComponent {
        private double progress = 0.0; // 0.0 to 1.0
        private boolean isDragging = false;
        private SeekListener seekListener;

        interface SeekListener {
            void onSeek(double position); // 0.0 to 1.0
        }

        public SimpleSeekBar() {
            setPreferredSize(new Dimension(0, 10));
            setupMouseHandlers();
        }

        public void addSeekListener(SeekListener listener) {
            this.seekListener = listener;
        }

        public void setProgress(double progress) {
            this.progress = Math.max(0, Math.min(1, progress));
            repaint();
        }

        private void setupMouseHandlers() {
            MouseAdapter mouseHandler = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    isDragging = true;
                    updateProgress(e.getX());
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (isDragging) {
                        isDragging = false;
                        updateProgress(e.getX());
                    }
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (isDragging) {
                        updateProgress(e.getX());
                    }
                }
            };

            addMouseListener(mouseHandler);
            addMouseMotionListener(mouseHandler);
        }

        private void updateProgress(int x) {
            double newProgress = Math.max(0, Math.min(1, (double) x / getWidth()));
            setProgress(newProgress);
            if (seekListener != null) {
                seekListener.onSeek(newProgress);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            UIUtils.setupRenderingHints(g2d);

            int width = getWidth();
            int height = getHeight();
            int barHeight = 6;
            int y = (height - barHeight) / 2;

            // Background track with better visibility
            g2d.setColor(UIConstants.SURFACE_LOW);
            g2d.fillRoundRect(0, y, width, barHeight, barHeight, barHeight);

            // Subtle border for definition
            g2d.setColor(UIConstants.withAlpha(UIConstants.BORDER_DEFAULT, 60));
            g2d.drawRoundRect(0, y, width - 1, barHeight - 1, barHeight, barHeight);

            // Progress with gradient
            if (progress > 0) {
                int progressWidth = (int) (width * progress);

                // Gradient progress bar
                GradientPaint gradient = new GradientPaint(
                        0, y, UIConstants.ACCENT_PRIMARY,
                        progressWidth, y, UIConstants.brighten(UIConstants.ACCENT_PRIMARY, 0.2f)
                );
                g2d.setPaint(gradient);
                g2d.fillRoundRect(0, y, progressWidth, barHeight, barHeight, barHeight);

                // Enhanced thumb
                if (isDragging || isMouseOver()) {
                    // Glow effect
                    g2d.setColor(UIConstants.withAlpha(UIConstants.ACCENT_PRIMARY, 100));
                    int thumbX = progressWidth - 8;
                    g2d.fillOval(thumbX - 2, y - 4, 16, 14);

                    // Main thumb
                    g2d.setColor(Color.WHITE);
                    g2d.fillOval(thumbX, y - 2, 12, 10);

                    // Thumb border
                    g2d.setColor(UIConstants.ACCENT_PRIMARY);
                    g2d.drawOval(thumbX, y - 2, 12, 10);
                }
            }
        }

        private boolean isMouseOver() {
            Point mousePos = getMousePosition();
            return mousePos != null;
        }
    }

    private JPanel createTimelineSection() {
        JPanel section = new JPanel(new BorderLayout());
        section.setOpaque(false);
        section.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_DEFAULT),
                "Timeline",
                0, 0,
                UIUtils.createFont(12, Font.BOLD),
                UIConstants.ACCENT_PRIMARY
        ));

        // Enhanced timeline controls with better spacing
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 5));
        controls.setOpaque(false);
        controls.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 5));

        // Create better icon buttons
        JButton zoomOut = createEnhancedToolButton("🔍-", "Zoom Out (Ctrl+-)", e -> {
            if (listener != null) listener.onZoomOutRequested();
        });
        JButton zoomIn = createEnhancedToolButton("🔍+", "Zoom In (Ctrl++)", e -> {
            if (listener != null) listener.onZoomInRequested();
        });
        JButton cut = createEnhancedToolButton("✂️", "Cut at Cursor (Ctrl+X)", e -> {
            if (listener != null) listener.onCutRequested();
        });
        JButton delete = createEnhancedToolButton("🗑️", "Delete Selected (Del)", e -> {
            if (listener != null) listener.onDeleteRequested();
        });

        // Add some spacing between groups
        controls.add(zoomOut);
        controls.add(zoomIn);
        controls.add(Box.createHorizontalStrut(10)); // Spacer
        controls.add(cut);
        controls.add(delete);

        section.add(controls, BorderLayout.NORTH);
        section.add(timelinePanel, BorderLayout.CENTER);

        return section;
    }

    private JButton createEnhancedToolButton(String text, String tooltip, java.awt.event.ActionListener action) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                UIUtils.setupRenderingHints(g2d);

                // Enhanced hover effect
                if (getModel().isRollover()) {
                    g2d.setColor(UIConstants.withAlpha(UIConstants.ACCENT_PRIMARY, 25));
                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                }

                if (getModel().isPressed()) {
                    g2d.setColor(UIConstants.withAlpha(UIConstants.ACCENT_PRIMARY, 40));
                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                }

                // Border for better visibility
                g2d.setColor(UIConstants.withAlpha(UIConstants.BORDER_DEFAULT, 60));
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);

                // Text/Icon
                g2d.setColor(UIConstants.TEXT_PRIMARY);
                g2d.setFont(getFont());
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent()) / 2 - 2;
                g2d.drawString(getText(), x, y);
            }
        };

        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(32, 32));
        button.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        button.setToolTipText(tooltip);
        button.addActionListener(action);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return button;
    }

    private JPanel createCompactRightPanel() {
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setOpaque(false);
        rightPanel.setPreferredSize(new Dimension(isCompactMode ? 200 : 250, 0));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

        // Quick actions at top
        JPanel actionsPanel = createActionsPanel();

        rightPanel.add(actionsPanel, BorderLayout.NORTH);
        rightPanel.add(toolsPanel, BorderLayout.CENTER);

        return rightPanel;
    }

    private JPanel createActionsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_DEFAULT),
                "Actions",
                0, 0,
                UIUtils.createFont(12, Font.BOLD),
                UIConstants.ACCENT_PRIMARY
        ));

        // Essential actions
        JButton importBtn = createActionButton("📥 Import Media", e -> {
            if (listener != null) listener.onImportVideoRequested();
        });
        JButton exportBtn = createActionButton("📤 Export Video", e -> {
            if (listener != null) listener.onExportVideoRequested();
        });

        panel.add(importBtn);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(exportBtn);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Quick tips
        JLabel tipsLabel = new JLabel("<html><small><b>Quick Tips:</b><br>" +
                "• Space = Play/Pause<br>" +
                "• Ctrl+I = Import<br>" +
                "• Ctrl+E = Export<br>" +
                "• Drag files to timeline</small></html>");
        tipsLabel.setForeground(UIConstants.TEXT_SECONDARY);
        panel.add(tipsLabel);

        return panel;
    }

    private JButton createActionButton(String text, java.awt.event.ActionListener action) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                UIUtils.setupRenderingHints(g2d);

                // Background
                if (getModel().isRollover()) {
                    g2d.setColor(UIConstants.withAlpha(UIConstants.ACCENT_PRIMARY, 20));
                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                }

                if (getModel().isPressed()) {
                    g2d.setColor(UIConstants.withAlpha(UIConstants.ACCENT_PRIMARY, 40));
                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                }

                // Text
                g2d.setColor(UIConstants.TEXT_PRIMARY);
                g2d.setFont(getFont());
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent()) / 2 - 2;
                g2d.drawString(getText(), x, y);
            }
        };

        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        button.setFont(UIUtils.createFont(12, Font.PLAIN));
        button.addActionListener(action);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return button;
    }

    private JPanel createSimpleStatusPanel() {
        JPanel status = new JPanel(new BorderLayout());
        status.setBackground(new Color(25, 25, 27));
        status.setPreferredSize(new Dimension(0, 28));
        status.setBorder(BorderFactory.createEmptyBorder(4, 15, 4, 15));

        statusIcon = new JLabel("⚡");
        statusIcon.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        statusField = new JTextField();
        statusField.setOpaque(false);
        statusField.setBorder(null);
        statusField.setEditable(false);
        statusField.setForeground(UIConstants.TEXT_SECONDARY);
        statusField.setFont(UIUtils.createFont(11, Font.PLAIN));

        JPanel leftSide = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftSide.setOpaque(false);
        leftSide.add(statusIcon);
        leftSide.add(statusField);

        status.add(leftSide, BorderLayout.WEST);

        return status;
    }

    private void setupStatusSystem() {
        statusTimer = new Timer(8000, e -> showTip());
        statusTimer.setRepeats(true);
        statusTimer.start();

        SwingUtilities.invokeLater(() -> {
            if (statusField != null && statusIcon != null) {
                statusIcon.setText("✨");
                statusField.setText("Welcome to VideoEditor - Ready to create");
                statusField.setForeground(UIConstants.ACCENT_SUCCESS);
            }
        });
    }

    private void showTip() {
        String[] tips = {
                "Tip: Drag video files to the timeline to start editing",
                "Tip: Use Space bar to play/pause your video",
                "Tip: Press J/L for frame-by-frame navigation",
                "Tip: Use Ctrl+I to import media files",
                "Tip: Press Ctrl+E to export your finished video"
        };

        String tip = tips[(int) (Math.random() * tips.length)];
        SwingUtilities.invokeLater(() -> {
            if (statusField != null && statusIcon != null) {
                statusIcon.setText("💡");
                statusField.setText(tip);
                statusField.setForeground(UIConstants.ACCENT_INFO);
            }
        });
    }

    private void setupKeyboardShortcuts() {
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getRootPane().getActionMap();

        // Essential shortcuts
        UIUtils.setupShortcut(inputMap, actionMap, "ctrl I", "import", () -> {
            if (listener != null) listener.onImportVideoRequested();
        });

        UIUtils.setupShortcut(inputMap, actionMap, "ctrl E", "export", () -> {
            if (listener != null) listener.onExportVideoRequested();
        });

        UIUtils.setupShortcut(inputMap, actionMap, "SPACE", "playPause", this::togglePlayPause);
        UIUtils.setupShortcut(inputMap, actionMap, "K", "playPause", this::togglePlayPause);

        UIUtils.setupShortcut(inputMap, actionMap, "J", "previousFrame", () -> {
            if (listener != null) listener.onPreviousFrameRequested();
        });

        UIUtils.setupShortcut(inputMap, actionMap, "L", "nextFrame", () -> {
            if (listener != null) listener.onNextFrameRequested();
        });

        UIUtils.setupShortcut(inputMap, actionMap, "ctrl X", "cut", () -> {
            if (listener != null) listener.onCutRequested();
        });

        UIUtils.setupShortcut(inputMap, actionMap, "DELETE", "delete", () -> {
            if (listener != null) listener.onDeleteRequested();
        });

        UIUtils.setupShortcut(inputMap, actionMap, "M", "mute", () -> {
            if (listener != null) listener.onMuteToggled();
        });

        setFocusable(true);
        requestFocusInWindow();
    }

    // Simplified playback methods
    private void togglePlayPause() {
        if (listener != null) {
            listener.onPlayPauseToggled();
            isPlaying = !isPlaying;
            playPauseButton.setText(isPlaying ? "⏸" : "▶");
            showNotification(isPlaying ? "▶ Playing" : "⏸ Paused", "INFO");
        }
    }

    private void stopPlayback() {
        isPlaying = false;
        playPauseButton.setText("▶");
        seekBar.setProgress(0);
        updateTimeDisplay(0, 0);
        showNotification("⏹ Stopped", "INFO");
    }

    private void seekRelative(int seconds) {
        if (listener != null) {
            double currentTime = getCurrentPlaybackTime();
            double newTime = Math.max(0, currentTime + seconds);
            listener.onSeekRequested(newTime);
            showNotification(seconds > 0 ? "⏭ Forward " + seconds + "s" : "⏮ Back " + Math.abs(seconds) + "s", "INFO");
        }
    }

    private void seekToPosition(double position) {
        if (listener != null) {
            double totalDuration = getTotalMediaDuration();
            double seekTime = position * totalDuration;
            listener.onSeekRequested(seekTime);
        }
    }

    // Public methods for controller interface
    public void updateTimeDisplay(double currentTime, double totalTime) {
        if (timeLabel != null && seekBar != null) {
            SwingUtilities.invokeLater(() -> {
                String current = TimeUtils.formatTime(currentTime);
                String total = TimeUtils.formatTime(totalTime);
                timeLabel.setText(current + " / " + total);

                if (totalTime > 0) {
                    double progress = currentTime / totalTime;
                    seekBar.setProgress(Math.max(0, Math.min(1, progress)));
                }
            });
        }
    }

    public void showNotification(String message, String type) {
        SwingUtilities.invokeLater(() -> {
            try {
                String icon = "ℹ️";
                Color textColor = UIConstants.TEXT_PRIMARY;

                switch (type.toUpperCase()) {
                    case "SUCCESS":
                    case "ACCENT_SUCCESS":
                        icon = "✅";
                        textColor = UIConstants.ACCENT_SUCCESS;
                        break;
                    case "WARNING":
                    case "ACCENT_WARNING":
                        icon = "⚠️";
                        textColor = UIConstants.ACCENT_WARNING;
                        break;
                    case "ERROR":
                    case "ACCENT_ERROR":
                        icon = "❌";
                        textColor = UIConstants.ACCENT_ERROR;
                        break;
                    case "INFO":
                    case "ACCENT_INFO":
                        icon = "💡";
                        textColor = UIConstants.ACCENT_INFO;
                        break;
                }

                if (statusIcon != null && statusField != null) {
                    statusIcon.setText(icon);
                    statusField.setText(message);
                    statusField.setForeground(textColor);
                }

                // Auto-clear notification after 3 seconds
                Timer clearTimer = new Timer(3000, e -> {
                    if (statusIcon != null && statusField != null) {
                        statusIcon.setText("⚡");
                        statusField.setText("Ready");
                        statusField.setForeground(UIConstants.TEXT_SECONDARY);
                    }
                });
                clearTimer.setRepeats(false);
                clearTimer.start();

            } catch (Exception e) {
                logger.warning("Error showing notification: " + e.getMessage());
            }
        });
    }

    public void addVideoToMediaPool(File file) {
        ((MediaPoolPanel) toolsPanel).addVideo(file);
        updateVideoInfo(file.getName());
        showNotification("📥 Imported: " + file.getName(), "SUCCESS");

        if (listener != null) {
            listener.onAudioTrackRequested();
        }
    }

    public void updateVideoInfo(String videoName) {
        SwingUtilities.invokeLater(() -> {
            if (videoPanel instanceof VideoPanel) {
                VideoPanel vPanel = (VideoPanel) videoPanel;
                vPanel.updateVideoInfo(videoName, "1920x1080", "MP4");
            }
        });
    }

    public File showFileChooserForImport() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Import Media Files");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Video Files", AppConstants.SUPPORTED_VIDEO_FORMATS));

        int result = fileChooser.showOpenDialog(this);
        return result == JFileChooser.APPROVE_OPTION ? fileChooser.getSelectedFile() : null;
    }

    public void initialize(TrackPanel trackPanelComponent, VideoEditorListener listener) {
        logger.info("Initializing MainView");
        this.listener = listener;

        ((TrackPanelView) timelinePanel).initialize(trackPanelComponent, listener);
        ((MediaPoolPanel) toolsPanel).initialize(listener);

        // Update components for current mode
        updateComponentsForCompactMode();

        requestFocusInWindow();
        showNotification("🚀 VideoEditor ready", "SUCCESS");

        logger.info("MainView initialization completed");
    }

    // Media player access
    public EmbeddedMediaPlayer getMediaPlayer() {
        return ((VideoPanel) videoPanel).getMediaPlayer();
    }

    public TrackPanelView getTrackPanelView() { return (TrackPanelView) timelinePanel; }

    public void refreshVideoPreview() {
        logger.info("Refreshing video preview");
        VideoPanel vPanel = (VideoPanel) videoPanel;
        if (vPanel.getMediaPlayerComponent() != null && vPanel.getMediaPlayer() != null) {
            vPanel.getMediaPlayerComponent().revalidate();
            vPanel.getMediaPlayerComponent().repaint();
            videoPanel.revalidate();
            videoPanel.repaint();
        }
    }

    // Timer management
    public void startPlaybackTimer() {
        if (playbackTimer != null) {
            playbackTimer.stop();
        }
        playbackTimer = new Timer(100, e -> {
            // Update UI during playback if needed
            if (isPlaying && listener != null) {
                double currentTime = getCurrentPlaybackTime();
                double totalTime = getTotalMediaDuration();
                updateTimeDisplay(currentTime, totalTime);
            }
        });
        playbackTimer.start();
    }

    public void stopPlaybackTimer() {
        if (playbackTimer != null) {
            playbackTimer.stop();
        }
    }

    // Volume control
    public void setVolume(double volume) {
        if (volumeSlider != null) {
            volumeSlider.setValue((int)(volume * 100));
        }
    }

    // Media state queries for controller interface
    private double getCurrentPlaybackTime() {
        if (listener instanceof com.videoeditor.ui.controllers.MainController) {
            return listener.getCurrentPlaybackTime();
        }
        return 0.0;
    }

    private double getTotalMediaDuration() {
        if (listener instanceof com.videoeditor.ui.controllers.MainController) {
            return listener.getTotalMediaDuration();
        }
        return 0.0;
    }

    // Compact mode utilities
    private void updateComponentsForCompactMode() {
        // Update video panel if it supports compact mode
        if (videoPanel instanceof VideoPanel) {
            // VideoPanel will handle compact mode internally based on size
            videoPanel.revalidate();
            videoPanel.repaint();
        }

        // Update button sizes
        if (playPauseButton != null) {
            int size = isCompactMode ? 28 : 32;
            playPauseButton.setPreferredSize(new Dimension(size, size));
        }

        // Update time label
        if (timeLabel != null) {
            int width = isCompactMode ? 75 : 85;
            timeLabel.setPreferredSize(new Dimension(width, 30));
        }

        // Update volume slider
        if (volumeSlider != null) {
            int width = isCompactMode ? 50 : 60;
            volumeSlider.setPreferredSize(new Dimension(width, 20));
        }
    }

    // Window state management
    public void saveWindowState() {
        // Could save window position, size, etc. to preferences
        logger.info("Window state: " + getBounds() + ", compact: " + isCompactMode);
    }

    @Override
    public void dispose() {
        try {
            // Clean up timers
            if (statusTimer != null) {
                statusTimer.stop();
            }
            if (playbackTimer != null) {
                playbackTimer.stop();
            }

            // Save state before closing
            saveWindowState();

            // Dispose video panel
            if (videoPanel instanceof VideoPanel) {
                ((VideoPanel) videoPanel).dispose();
            }

            // Dispose timeline panel
            if (timelinePanel instanceof TrackPanelView) {
                ((TrackPanelView) timelinePanel).dispose();
            }

            super.dispose();
            logger.info("MainView disposed successfully");
        } catch (Exception e) {
            logger.warning("Error disposing MainView: " + e.getMessage());
        }
    }
}