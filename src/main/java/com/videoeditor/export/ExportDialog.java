package com.videoeditor.export;

import com.videoeditor.config.UIConstants;
import com.videoeditor.ui.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * Modern export dialog with preview and settings
 */
public class ExportDialog extends JDialog {
    private final ExportSettings exportSettings;
    private File outputFile;
    private boolean approved = false;

    // UI Components
    private JTextField outputPathField;
    private JComboBox<String> resolutionCombo;
    private JComboBox<ExportQuality> qualityCombo;
    private JComboBox<ExportFormat> formatCombo;
    private JSpinner frameRateSpinner;
    private JLabel estimatedSizeLabel;

    public ExportDialog(Frame parent) {
        super(parent, "Export Video", true);
        this.exportSettings = new ExportSettings();

        initializeComponents();
        layoutComponents();
        setupEventHandlers();
        updateEstimatedSize();

        setSize(500, 400);
        setLocationRelativeTo(parent);
    }

    private void initializeComponents() {
        // Output file selection
        outputPathField = new JTextField();
        outputPathField.setEditable(false);
        outputPathField.setBackground(UIConstants.SURFACE_LOW);
        outputPathField.setForeground(UIConstants.TEXT_PRIMARY);

        // Resolution selection
        String[] resolutions = {
                "1920x1080 (Full HD)",
                "1280x720 (HD)",
                "3840x2160 (4K)",
                "854x480 (SD)"
        };
        resolutionCombo = new JComboBox<>(resolutions);
        resolutionCombo.setSelectedIndex(0); // Default to Full HD

        // Quality selection
        qualityCombo = new JComboBox<>(ExportQuality.values());
        qualityCombo.setSelectedItem(ExportQuality.HIGH);
        qualityCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ExportQuality) {
                    setText(((ExportQuality) value).getDisplayName());
                }
                return this;
            }
        });

        // Format selection
        formatCombo = new JComboBox<>(ExportFormat.values());
        formatCombo.setSelectedItem(ExportFormat.MP4);
        formatCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ExportFormat) {
                    setText(((ExportFormat) value).getDisplayName());
                }
                return this;
            }
        });

        // Frame rate
        frameRateSpinner = new JSpinner(new SpinnerNumberModel(30.0, 1.0, 60.0, 1.0));

        // Estimated size
        estimatedSizeLabel = new JLabel("Estimated size: Calculating...");
        estimatedSizeLabel.setForeground(UIConstants.TEXT_SECONDARY);
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
        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        // Title
        JLabel titleLabel = UIUtils.createHeading("Export Video", 2);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 3;
        mainPanel.add(titleLabel, gbc);

        // Output file
        gbc.gridwidth = 1; gbc.gridy++;
        mainPanel.add(createLabel("Output File:"), gbc);

        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        mainPanel.add(outputPathField, gbc);

        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JButton browseButton = UIUtils.createSecondaryButton("Browse...", "Choose output location", this::browseOutputFile);
        mainPanel.add(browseButton, gbc);

        // Resolution
        gbc.gridx = 0; gbc.gridy++; gbc.weightx = 0;
        mainPanel.add(createLabel("Resolution:"), gbc);

        gbc.gridx = 1; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(resolutionCombo, gbc);

        // Quality
        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 1;
        mainPanel.add(createLabel("Quality:"), gbc);

        gbc.gridx = 1; gbc.gridwidth = 2;
        mainPanel.add(qualityCombo, gbc);

        // Format
        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 1;
        mainPanel.add(createLabel("Format:"), gbc);

        gbc.gridx = 1; gbc.gridwidth = 2;
        mainPanel.add(formatCombo, gbc);

        // Frame rate
        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 1;
        mainPanel.add(createLabel("Frame Rate:"), gbc);

        gbc.gridx = 1; gbc.gridwidth = 2;
        mainPanel.add(frameRateSpinner, gbc);

        // Estimated size
        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 3;
        mainPanel.add(estimatedSizeLabel, gbc);

        add(mainPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        JButton cancelButton = UIUtils.createSecondaryButton("Cancel", "Cancel export", e -> dispose());
        JButton exportButton = UIUtils.createPrimaryButton("Export", "Start export process", this::startExport);

        buttonPanel.add(cancelButton);
        buttonPanel.add(exportButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(UIConstants.TEXT_PRIMARY);
        label.setFont(UIUtils.createFont(UIConstants.FONT_SIZE_BASE, Font.BOLD));
        return label;
    }

    private void setupEventHandlers() {
        resolutionCombo.addActionListener(e -> {
            updateExportSettings();
            updateEstimatedSize();
        });

        qualityCombo.addActionListener(e -> {
            updateExportSettings();
            updateEstimatedSize();
        });

        formatCombo.addActionListener(e -> {
            updateExportSettings();
            updateFileExtension();
        });

        frameRateSpinner.addChangeListener(e -> {
            updateExportSettings();
            updateEstimatedSize();
        });
    }

    private void browseOutputFile(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Export As");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        ExportFormat format = (ExportFormat) formatCombo.getSelectedItem();
        assert format != null;
        String extension = format.getExtension();

        fileChooser.setSelectedFile(new File("VideoEditor_Export" + extension));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            outputFile = fileChooser.getSelectedFile();

            // Ensure correct extension
            if (!outputFile.getName().toLowerCase().endsWith(extension)) {
                outputFile = new File(outputFile.getAbsolutePath() + extension);
            }

            outputPathField.setText(outputFile.getAbsolutePath());
            updateEstimatedSize();
        }
    }

    private void updateExportSettings() {
        // Update resolution
        String resolution = (String) resolutionCombo.getSelectedItem();
        assert resolution != null;
        if (resolution.startsWith("1920x1080")) {
            exportSettings.setWidth(1920);
            exportSettings.setHeight(1080);
        } else if (resolution.startsWith("1280x720")) {
            exportSettings.setWidth(1280);
            exportSettings.setHeight(720);
        } else if (resolution.startsWith("3840x2160")) {
            exportSettings.setWidth(3840);
            exportSettings.setHeight(2160);
        } else if (resolution.startsWith("854x480")) {
            exportSettings.setWidth(854);
            exportSettings.setHeight(480);
        }

        // Update other settings
        exportSettings.setQuality((ExportQuality) qualityCombo.getSelectedItem());
        exportSettings.setFormat((ExportFormat) formatCombo.getSelectedItem());
        exportSettings.setFrameRate((Double) frameRateSpinner.getValue());
    }

    private void updateFileExtension() {
        if (outputFile != null) {
            ExportFormat format = (ExportFormat) formatCombo.getSelectedItem();
            assert format != null;
            String extension = format.getExtension();

            String path = outputFile.getAbsolutePath();
            int lastDot = path.lastIndexOf('.');
            if (lastDot > 0) {
                path = path.substring(0, lastDot);
            }
            path += extension;

            outputFile = new File(path);
            outputPathField.setText(outputFile.getAbsolutePath());
        }
    }

    private void updateEstimatedSize() {
        // Simple size estimation based on resolution and quality
        int pixels = exportSettings.getWidth() * exportSettings.getHeight();
        double quality = exportSettings.getQuality().getCrf();

        // Rough estimation: lower CRF = higher bitrate
        double estimatedBitrateMbps = (pixels / 1000000.0) * (30 - quality) / 10.0;
        double estimatedSizeMB = estimatedBitrateMbps * 60 * 5; // Assume 5 minutes

        estimatedSizeLabel.setText(String.format("Estimated size: ~%.1f MB (depends on content length)", estimatedSizeMB));
    }

    private void startExport(ActionEvent e) {
        if (outputFile == null) {
            JOptionPane.showMessageDialog(this,
                    "Please choose an output file location.",
                    "No Output File",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        updateExportSettings();
        approved = true;
        dispose();
    }

    public boolean isApproved() {
        return approved;
    }

    public ExportSettings getExportSettings() {
        return exportSettings;
    }

    public File getOutputFile() {
        return outputFile;
    }
}