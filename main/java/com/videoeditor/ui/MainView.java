package com.videoeditor.ui;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.videoeditor.model.Track;
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery;
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class MainView extends JFrame {

    private final JButton playPauseButton;
    private final JButton importButton;
    private final JButton exportButton;
    private final JButton cutButton;
    private final JButton deleteButton;
    private final JButton replayButton;
    private final JButton zoomInButton;
    private final JButton zoomOutButton;

    private final JLabel statusLabel;
    private final JFileChooser fileChooser;
    private final EmbeddedMediaPlayerComponent mediaPlayerComponent;

    private final JPanel trackPanel;


    private boolean isPlaying = false;

    public MainView() {
        // Set FlatLaf Look and Feel
        try {
            UIManager.setLookAndFeel(new FlatDarculaLaf());
        } catch (Exception e) {
            e.printStackTrace();
        }

        setTitle("Swing Video Editor");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        String vlcPath = "C:\\Program Files\\VideoLAN\\VLC";
        System.setProperty("jna.library.path", vlcPath);
        boolean found = new NativeDiscovery().discover();
        System.out.println("VLC Native Discovery: " + found);

        mediaPlayerComponent = new EmbeddedMediaPlayerComponent();
        mediaPlayerComponent.setPreferredSize(new Dimension(700, 350)); // Set fixed size
        mediaPlayerComponent.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));

        fileChooser = new JFileChooser();
        JLabel videoDetailsLabel = new JLabel("Video Details: ");
        statusLabel = new JLabel("Status: ");

        playPauseButton = createButton("Play/Pause");
        importButton = createButton("Import");
        exportButton = createButton("Export");
        cutButton = createButton("Cut");
        deleteButton = createButton("Delete");
        replayButton = createButton("Replay");
        zoomInButton = createButton("Zoom In");
        zoomOutButton = createButton("Zoom Out");

        trackPanel = new JPanel(new BorderLayout());
        trackPanel.setBackground(new Color(60, 63, 65));
        new Track();


        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem openMenuItem = new JMenuItem("Open");
        fileMenu.add(openMenuItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        JPanel importExportPanel = new JPanel();
        JPanel editingToolsPanel = new JPanel();
        JPanel videoPanel = new JPanel(new BorderLayout());
        JPanel controlPanel = new JPanel();
        JPanel trackPanelContainer = new JPanel(new BorderLayout());

        importExportPanel.setLayout(new BoxLayout(importExportPanel, BoxLayout.Y_AXIS));
        importExportPanel.setBorder(BorderFactory.createTitledBorder("Import/Export"));
        importExportPanel.add(importButton);
        importExportPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        importExportPanel.add(exportButton);
        importExportPanel.setBackground(new Color(60, 63, 65));

        editingToolsPanel.setLayout(new BoxLayout(editingToolsPanel, BoxLayout.Y_AXIS));
        editingToolsPanel.setBorder(BorderFactory.createTitledBorder("Editing Tools"));
        editingToolsPanel.add(cutButton);
        editingToolsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        editingToolsPanel.add(deleteButton);
        editingToolsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        editingToolsPanel.setBackground(new Color(60, 63, 65));

        controlPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
        controlPanel.add(playPauseButton);
        controlPanel.add(replayButton);
        controlPanel.add(zoomInButton);
        controlPanel.add(zoomOutButton);
        controlPanel.setBackground(new Color(60, 63, 65));

        setLayout(new GridBagLayout());
        getContentPane().setBackground(new Color(60, 63, 65)); // Set the main background color

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0.2;
        gbc.weighty = 1.0;
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBackground(new Color(60, 63, 65));
        leftPanel.add(importExportPanel);
        leftPanel.add(editingToolsPanel);
        leftPanel.add(Box.createVerticalGlue());
        add(leftPanel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.gridheight = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        add(controlPanel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.gridheight = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 0.8;
        videoPanel.add(mediaPlayerComponent, BorderLayout.CENTER);
        add(videoPanel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.gridheight = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 0.2;
        trackPanelContainer.setBorder(BorderFactory.createTitledBorder("Tracks"));

        trackPanelContainer.add(trackPanel, BorderLayout.CENTER);
        trackPanelContainer.setBackground(new Color(60, 63, 65));
        add(trackPanelContainer, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        gbc.gridheight = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        add(videoDetailsLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 3;
        gbc.gridheight = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        add(statusLabel, gbc);

        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    togglePlayPause();
                }
            }
        });

        setFocusable(true);
        requestFocusInWindow();

        cutButton.addActionListener(e -> setSelectedTool("cut"));
        deleteButton.addActionListener(e -> setSelectedTool("delete"));

    }

    private JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(new Color(75, 75, 75));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        return button;
    }

    public void setMainController() {
        // No implementation needed for now
    }

    public JButton getPlayPauseButton() {
        return playPauseButton;
    }

    public JButton getImportButton() {
        return importButton;
    }

    public JButton getExportButton() {
        return exportButton;
    }

    public JButton getCutButton() {
        return cutButton;
    }

    public JButton getDeleteButton() {
        return deleteButton;
    }

    public JButton getReplayButton() {
        return replayButton;
    }

    public JButton getZoomInButton() {
        return zoomInButton;
    }

    public JButton getZoomOutButton() {
        return zoomOutButton;
    }

    public EmbeddedMediaPlayerComponent getMediaPlayerComponent() {
        return mediaPlayerComponent;
    }

    public JPanel getTrackPanel() {
        return trackPanel;
    }


    public JFileChooser getFileChooser() {
        return fileChooser;
    }

    public void setStatus(String status) {
        statusLabel.setText("Status: " + status);
    }

    public void togglePlayPause() {
        if (isPlaying) {
            mediaPlayerComponent.mediaPlayer().controls().pause();
        } else {
            mediaPlayerComponent.mediaPlayer().controls().play();
        }
        isPlaying = !isPlaying;
    }

    public void setSelectedTool(String tool) {
        setStatus("Selected tool: " + tool);
    }
}
