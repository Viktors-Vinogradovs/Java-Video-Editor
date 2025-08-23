package com.videoeditor.ui.panels;

import com.videoeditor.ui.events.VideoEditorListener;
import com.videoeditor.ui.utils.UIUtils;
import com.videoeditor.ui.events.DragAndDropHandler;
import com.videoeditor.config.UIConstants;
import com.videoeditor.config.AppConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.logging.Logger;

public class MediaPoolPanel extends JPanel {
    private static final Logger logger = Logger.getLogger(MediaPoolPanel.class.getName());
    private ArrayList<JPanel> mediaSlots;
    private VideoEditorListener listener;

    public MediaPoolPanel() {
        super();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setBorder(UIUtils.createModernTitledBorder("Media Library"));

        // FIXED: Increased width to show full "Empty Slot " text
        setPreferredSize(new Dimension(350, 420)); // Changed from 350 to 420
        setMinimumSize(new Dimension(300, 380)); // Changed from 300 to 380
    }

    public void initialize(VideoEditorListener listener) {
        this.listener = listener;
        setupCleanMediaPool();
    }

    private void setupCleanMediaPool() {
        JPanel mediaContainer = new JPanel();
        mediaContainer.setLayout(new BoxLayout(mediaContainer, BoxLayout.Y_AXIS));
        mediaContainer.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(mediaContainer);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // Shorter text that definitely fits
        JLabel helpLabel = new JLabel("Click to import • Drag to timeline");
        helpLabel.setForeground(UIConstants.TEXT_SECONDARY);
        helpLabel.setHorizontalAlignment(SwingConstants.CENTER);
        helpLabel.setFont(UIUtils.createFont(UIConstants.FONT_SIZE_XS, Font.PLAIN));
        helpLabel.setBorder(BorderFactory.createEmptyBorder(8, 5, 18, 5));
        mediaContainer.add(helpLabel);

        initializeMediaSlots(mediaContainer);
        add(scrollPane);
    }

    private void initializeMediaSlots(JPanel container) {
        mediaSlots = new ArrayList<>();
        for (int i = 0; i < AppConstants.MAX_MEDIA_SLOTS; i++) {
            JPanel slot = createStyledMediaSlot(listener, this);
            mediaSlots.add(slot);
            container.add(slot);
            container.add(Box.createRigidArea(new Dimension(0, 12)));
        }
    }

    private JPanel createStyledMediaSlot(VideoEditorListener listener, MediaPoolPanel mediaPoolPanel) {
        JPanel slot = getjPanel();

        JPanel thumbnailPanel = createStyledThumbnailPanel();
        JPanel infoPanel = createStyledMediaInfoPanel();

        slot.add(thumbnailPanel, BorderLayout.WEST);
        slot.add(infoPanel, BorderLayout.CENTER);

        // Enhanced mouse interaction
        slot.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                ((JPanel)e.getSource()).setCursor(new Cursor(Cursor.HAND_CURSOR));
                ((JPanel)e.getSource()).repaint();
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                ((JPanel)e.getSource()).setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                ((JPanel)e.getSource()).repaint();
            }
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                JPanel clickedSlot = (JPanel) e.getSource();
                JPanel infoPanel = (JPanel) clickedSlot.getComponent(1);
                JLabel nameLabel = (JLabel) infoPanel.getComponent(0);

                if (nameLabel.getText().equals("Empty Slot ")) {
                    if (listener != null) {
                        listener.onImportVideoRequested();
                    }
                }
            }
        });

        setupContextMenu(slot, infoPanel, listener, mediaPoolPanel);
        return slot;
    }

    private JPanel getjPanel() {
        JPanel slot = new JPanel(new BorderLayout(12, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                UIUtils.setupRenderingHints(g2d);

                // Modern slot background
                Color bgColor = UIConstants.SURFACE_MEDIUM;
                g2d.setColor(bgColor);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

                // Subtle border
                g2d.setColor(UIConstants.withAlpha(UIConstants.BORDER_DEFAULT, 80));
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);

            }
        };

        slot.setOpaque(false);
        // FIXED: Increased width to accommodate full text
        slot.setPreferredSize(new Dimension(280, 80)); // Changed from 220 to 280
        slot.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        slot.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return slot;
    }

    private JPanel createStyledThumbnailPanel() {
        JPanel thumbnail = new JPanel() {

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                UIUtils.setupRenderingHints(g2d);

                // Modern thumbnail background
                GradientPaint gradient = new GradientPaint(
                        0, 0, UIConstants.SURFACE_HIGH,
                        0, getHeight(), UIConstants.SURFACE_MEDIUM
                );
                g2d.setPaint(gradient);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

                // Border
                g2d.setColor(UIConstants.BORDER_DEFAULT);
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);

                // Draw icon based on state
                g2d.setColor(UIConstants.TEXT_SECONDARY);
                g2d.setFont(new Font("Arial", Font.PLAIN, 20));
                String icon = "📁";
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(icon)) / 2;
                int y = (getHeight() + fm.getAscent()) / 2 - 2;
                g2d.drawString(icon, x, y);
            }

        };

        thumbnail.setPreferredSize(new Dimension(60, 60));
        thumbnail.setOpaque(false);
        return thumbnail;
    }

    private JPanel createStyledMediaInfoPanel() {
        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        JLabel nameLabel = new JLabel("Empty Slot ");
        nameLabel.setForeground(UIConstants.TEXT_SECONDARY);
        nameLabel.setFont(UIUtils.createFont(13, Font.BOLD));
        // FIXED: Don't constrain label size - let it size itself naturally

        JLabel durationLabel = new JLabel("Click to import");
        durationLabel.setForeground(UIConstants.withAlpha(UIConstants.TEXT_SECONDARY, 160));
        durationLabel.setFont(UIUtils.createFont(11, Font.PLAIN));

        JLabel formatLabel = new JLabel("Ready");
        formatLabel.setForeground(UIConstants.ACCENT_PRIMARY);
        formatLabel.setFont(UIUtils.createFont(11, Font.PLAIN));

        info.add(nameLabel);
        info.add(Box.createRigidArea(new Dimension(0, 4)));
        info.add(durationLabel);
        info.add(Box.createRigidArea(new Dimension(0, 4)));
        info.add(formatLabel);

        return info;
    }

    private void setupContextMenu(JPanel slot, JPanel infoPanel, VideoEditorListener listener, MediaPoolPanel mediaPoolPanel) {
        JPopupMenu contextMenu = new JPopupMenu();

        JMenuItem previewItem = getjMenuItem(infoPanel, listener, mediaPoolPanel);

        JMenuItem renameItem = getjMenuItem(infoPanel, mediaPoolPanel);

        JMenuItem deleteItem = getMenuItem(slot, infoPanel, mediaPoolPanel);

        contextMenu.add(previewItem);
        contextMenu.add(renameItem);
        contextMenu.add(new JSeparator());
        contextMenu.add(deleteItem);

        slot.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    contextMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    contextMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    private static JMenuItem getMenuItem(JPanel slot, JPanel infoPanel, MediaPoolPanel mediaPoolPanel) {
        JMenuItem deleteItem = new JMenuItem("Remove");
        deleteItem.addActionListener(e -> {
            JLabel nameLabel = (JLabel) infoPanel.getComponent(0);
            JLabel durationLabel = (JLabel) infoPanel.getComponent(2);
            JLabel formatLabel = (JLabel) infoPanel.getComponent(4);

            nameLabel.setText("Empty Slot ");
            nameLabel.setForeground(UIConstants.TEXT_SECONDARY);
            durationLabel.setText("Click to import");
            durationLabel.setForeground(UIConstants.withAlpha(UIConstants.TEXT_SECONDARY, 160));
            formatLabel.setText("Ready");
            formatLabel.setForeground(UIConstants.ACCENT_PRIMARY);

            // Reset thumbnail
            JPanel thumbnailPanel = (JPanel) slot.getComponent(0);
            thumbnailPanel.repaint();

            slot.repaint();
            ((MainView) SwingUtilities.getAncestorOfClass(MainView.class, mediaPoolPanel))
                    .showNotification("Removed media", "ACCENT_WARNING");
        });
        return deleteItem;
    }

    private static JMenuItem getjMenuItem(JPanel infoPanel, MediaPoolPanel mediaPoolPanel) {
        JMenuItem renameItem = new JMenuItem("Rename");
        renameItem.addActionListener(e -> {
            JLabel nameLabel = (JLabel) infoPanel.getComponent(0);
            String newName = JOptionPane.showInputDialog(
                    SwingUtilities.getAncestorOfClass(MainView.class, mediaPoolPanel),
                    "Enter new name:",
                    nameLabel.getText()
            );
            if (newName != null && !newName.trim().isEmpty()) {
                nameLabel.setText(newName);
                ((MainView) SwingUtilities.getAncestorOfClass(MainView.class, mediaPoolPanel))
                        .showNotification("Renamed to: " + newName, "ACCENT_SUCCESS");
            }
        });
        return renameItem;
    }

    private static JMenuItem getjMenuItem(JPanel infoPanel, VideoEditorListener listener, MediaPoolPanel mediaPoolPanel) {
        JMenuItem previewItem = new JMenuItem("Preview");
        previewItem.addActionListener(e -> {
            JLabel nameLabel = (JLabel) infoPanel.getComponent(0);
            if (!nameLabel.getText().equals("Empty Slot ")) {
                nameLabel.getText();
                listener.onImportVideoRequested();
                ((MainView) SwingUtilities.getAncestorOfClass(MainView.class, mediaPoolPanel))
                        .showNotification("Previewing: " + nameLabel.getText(), "ACCENT_PRIMARY");
            }
        });
        return previewItem;
    }

    public void addVideo(File file) {
        logger.info("Adding video to media pool: " + file.getName());

        for (JPanel slot : mediaSlots) {
            if (slot.getComponentCount() >= 2) {
                JPanel infoPanel = (JPanel) slot.getComponent(1);
                if (infoPanel.getComponentCount() > 0) {
                    JLabel nameLabel = (JLabel) infoPanel.getComponent(0);
                    if (nameLabel.getText().equals("Empty Slot ")) {
                        // Update thumbnail with generated preview
                        if (slot.getComponentCount() > 0) {
                            JPanel thumbnailPanel = (JPanel) slot.getComponent(0);

                            // Generate thumbnail for video file
                            BufferedImage thumbnail = generateVideoThumbnail(file);
                            if (thumbnail != null && thumbnailPanel != null) {
                                try {
                                    java.lang.reflect.Method setThumbnail = thumbnailPanel.getClass().getMethod("setThumbnail", BufferedImage.class);
                                    java.lang.reflect.Method setFileName = thumbnailPanel.getClass().getMethod("setFileName", String.class);
                                    setThumbnail.invoke(thumbnailPanel, thumbnail);
                                    setFileName.invoke(thumbnailPanel, file.getName());
                                } catch (Exception e) {
                                    logger.fine("Could not set thumbnail: " + e.getMessage());
                                    thumbnailPanel.repaint(); // Fallback to icon change
                                }
                            } else {
                                assert thumbnailPanel != null;
                                thumbnailPanel.repaint(); // Fallback to icon change
                            }
                        }

                        // Update name
                        nameLabel.setText(file.getName());
                        nameLabel.setForeground(UIConstants.TEXT_PRIMARY);

                        // Update duration
                        if (infoPanel.getComponentCount() > 2) {
                            JLabel durationLabel = (JLabel) infoPanel.getComponent(2);
                            durationLabel.setText("Unknown");
                            durationLabel.setForeground(UIConstants.TEXT_PRIMARY);
                            durationLabel.setToolTipText("Duration will be available when video is loaded");
                        }

                        // Update format
                        if (infoPanel.getComponentCount() > 4) {
                            JLabel formatLabel = (JLabel) infoPanel.getComponent(4);
                            formatLabel.setText(getFileExtension(file).toUpperCase());
                            formatLabel.setForeground(UIConstants.ACCENT_SUCCESS);
                        }

                        // Enable drag and drop
                        DragAndDropHandler.enableDrag(slot, file);
                        slot.repaint();

                        // Clean notification
                        SwingUtilities.invokeLater(() -> {
                            Component ancestor = SwingUtilities.getAncestorOfClass(MainView.class, this);
                            if (ancestor instanceof MainView) {
                                ((MainView) ancestor).showNotification("Imported: " + file.getName(), "ACCENT_SUCCESS");
                            }
                        });
                        return;
                    }
                }
            }
        }

        // Media library full notification
        SwingUtilities.invokeLater(() -> {
            Component ancestor = SwingUtilities.getAncestorOfClass(MainView.class, this);
            if (ancestor instanceof MainView) {
                ((MainView) ancestor).showNotification("Media Library Full", "ACCENT_WARNING");
            }
        });
    }

    private BufferedImage generateVideoThumbnail(File videoFile) {
        try {
            // Generate a colorful thumbnail based on file properties
            BufferedImage thumbnail = new BufferedImage(60, 60, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = thumbnail.createGraphics();
            UIUtils.setupRenderingHints(g2d);

            // Create gradient based on file name hash for unique colors
            int hash = videoFile.getName().hashCode();
            Color color1 = new Color(
                    Math.abs((hash & 0xFF0000) >> 16) % 180 + 50,
                    Math.abs((hash & 0xFF00) >> 8) % 180 + 50,
                    Math.abs(hash & 0xFF) % 180 + 50
            );
            Color color2 = UIConstants.darken(color1, 0.4f);

            // Gradient background
            GradientPaint gradient = new GradientPaint(0, 0, color1, 60, 60, color2);
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, 60, 60);

            // Add film strip perforations
            g2d.setColor(new Color(0, 0, 0, 120));
            for (int i = 0; i < 60; i += 8) {
                g2d.fillRect(0, i, 4, 4);
                g2d.fillRect(56, i, 4, 4);
            }

            // Add play button icon
            g2d.setColor(Color.WHITE);
            int[] xPoints = {20, 45, 20};
            int[] yPoints = {18, 30, 42};
            g2d.fillPolygon(xPoints, yPoints, 3);

            // Add shine effect
            g2d.setColor(new Color(255, 255, 255, 60));
            g2d.fillOval(15, 10, 30, 20);

            g2d.dispose();
            return thumbnail;

        } catch (Exception e) {
            logger.warning("Failed to generate video thumbnail: " + e.getMessage());
            return null;
        }
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        int lastIndex = name.lastIndexOf('.');
        return lastIndex > 0 ? name.substring(lastIndex + 1) : "";
    }
}