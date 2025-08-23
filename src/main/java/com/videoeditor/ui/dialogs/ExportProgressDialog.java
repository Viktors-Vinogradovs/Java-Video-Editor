package com.videoeditor.ui.dialogs;

import com.videoeditor.export.ExportService;
import com.videoeditor.config.UIConstants;
import com.videoeditor.ui.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Progress dialog for export operations
 */
public class ExportProgressDialog extends JDialog implements ExportService.ExportProgressListener {
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JLabel timeLabel;
    private JButton cancelButton;

    private long startTime;
    private boolean cancelled = false;

    public ExportProgressDialog(Frame parent, String title) {
        super(parent, title, true);

        initializeComponents();
        layoutComponents();
        setupEventHandlers();

        setSize(400, 200);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        startTime = System.currentTimeMillis();
    }

    private void initializeComponents() {
        // Progress bar
        progressBar = new JProgressBar(0, 100) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                UIUtils.setupRenderingHints(g2d);

                // Custom progress bar styling
                g2d.setColor(UIConstants.SURFACE_LOW);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

                if (getValue() > 0) {
                    int progressWidth = (int) ((double) getValue() / getMaximum() * getWidth());

                    GradientPaint gradient = new GradientPaint(
                            0, 0, UIConstants.ACCENT_PRIMARY,
                            progressWidth, 0, UIConstants.brighten(UIConstants.ACCENT_PRIMARY, 0.3f)
                    );
                    g2d.setPaint(gradient);
                    g2d.fillRoundRect(0, 0, progressWidth, getHeight(), 8, 8);
                }

                // Progress text
                g2d.setColor(UIConstants.TEXT_PRIMARY);
                g2d.setFont(UIUtils.createFont(12, Font.BOLD));
                String text = getValue() + "%";
                FontMetrics fm = g2d.getFontMetrics();
                int textX = (getWidth() - fm.stringWidth(text)) / 2;
                int textY = (getHeight() + fm.getAscent()) / 2 - 2;
                g2d.drawString(text, textX, textY);
            }
        };
        progressBar.setStringPainted(false);
        progressBar.setOpaque(false);

        // Status label
        statusLabel = new JLabel("Preparing export...");
        statusLabel.setForeground(UIConstants.TEXT_PRIMARY);
        statusLabel.setFont(UIUtils.createFont(UIConstants.FONT_SIZE_BASE, Font.PLAIN));

        // Time label
        timeLabel = new JLabel("Elapsed: 00:00");
        timeLabel.setForeground(UIConstants.TEXT_SECONDARY);
        timeLabel.setFont(UIUtils.createFont(UIConstants.FONT_SIZE_SM, Font.PLAIN));

        // Cancel button
        cancelButton = UIUtils.createSecondaryButton("Cancel", "Cancel export", e -> cancelExport());

        // Timer for elapsed time
        Timer elapsedTimer = new Timer(1000, e -> updateElapsedTime());
        elapsedTimer.start();
    }

    private void layoutComponents() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(UIConstants.BACKGROUND_PRIMARY);

        // Main panel
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                UIUtils.setupRenderingHints(g2d);

                GradientPaint gradient = new GradientPaint(
                        0, 0, UIConstants.BACKGROUND_PRIMARY,
                        0, getHeight(), UIConstants.BACKGROUND_SECONDARY
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 20, 30));

        // Title
        JLabel titleLabel = UIUtils.createHeading("Exporting Video...", 3);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(titleLabel);

        mainPanel.add(Box.createVerticalStrut(20));

        // Status
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(statusLabel);

        mainPanel.add(Box.createVerticalStrut(15));

        // Progress bar
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        progressBar.setMaximumSize(new Dimension(300, 20));
        mainPanel.add(progressBar);

        mainPanel.add(Box.createVerticalStrut(15));

        // Time
        timeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(timeLabel);

        add(mainPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setOpaque(false);
        buttonPanel.add(cancelButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void setupEventHandlers() {
        // Prevent closing during export
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                cancelExport();
            }
        });
    }

    private void cancelExport() {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to cancel the export?\nThis will stop the export process.",
                "Cancel Export",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (choice == JOptionPane.YES_OPTION) {
            cancelled = true;
            statusLabel.setText("Cancelling export...");
            cancelButton.setEnabled(false);

            // Note: In a real implementation, you'd need to interrupt the FFmpeg process
            dispose();
        }
    }

    private void updateElapsedTime() {
        long elapsed = System.currentTimeMillis() - startTime;
        int seconds = (int) (elapsed / 1000) % 60;
        int minutes = (int) (elapsed / 60000);

        timeLabel.setText(String.format("Elapsed: %02d:%02d", minutes, seconds));
    }

    @Override
    public void onProgressUpdate(String message, int progress) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(message);

            if (progress >= 0) {
                progressBar.setValue(progress);

                if (progress >= 100) {
                    cancelButton.setText("Close");
                    cancelButton.removeActionListener(cancelButton.getActionListeners()[0]);
                    cancelButton.addActionListener(e -> dispose());
                }
            }
        });
    }

}