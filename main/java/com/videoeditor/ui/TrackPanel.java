package com.videoeditor.ui;

import com.videoeditor.core.VideoEditor;
import com.videoeditor.model.Track;
import com.videoeditor.model.TrackSegment;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TrackPanel extends JPanel {

    private static final Logger logger = Logger.getLogger(TrackPanel.class.getName());

    private final Track track;
    private double cursorPosition = 0;
    private double zoomLevel = 1.0;
    private int panOffset = 0;
    private int initialPanOffset = 0;
    private int initialMouseX = 0;
    private final VideoEditor videoEditor;
    private final List<BufferedImage> thumbnails = new ArrayList<>();
    private final List<BufferedImage> waveforms = new ArrayList<>();
    private TrackPanelListener trackPanelListener;
    private boolean draggingCursor = false;
    private final MainController mainController;
    private TrackSegment selectedSegment;

    public TrackPanel(Track track, VideoEditor videoEditor, MainController mainController) {
        this.track = track;
        this.videoEditor = videoEditor;
        this.mainController = mainController;
        track.addTrackListener(this::refresh);
        setPreferredSize(new Dimension(800, 100));
        setBackground(new Color(60, 63, 65));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                initialMouseX = e.getX();
                initialPanOffset = panOffset;
                if (isCursorClicked(e)) {
                    draggingCursor = true;
                } else {
                    draggingCursor = false;
                    if (e.isControlDown()) {
                        selectSegment(e);
                    } else {
                        clearSelection();
                        selectSegment(e);
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                draggingCursor = false;
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (draggingCursor) {
                    updateCursorOnDrag(e);
                } else {
                    int dx = e.getX() - initialMouseX;
                    panOffset = initialPanOffset + dx;
                    if (panOffset < 0) {
                        panOffset = 0;
                    }
                    repaint();
                }
            }
        });
    }

    private boolean isCursorClicked(MouseEvent e) {
        int cursorX = (int) ((cursorPosition / track.getTotalDuration()) * (getWidth() * zoomLevel)) + panOffset;
        return Math.abs(e.getX() - cursorX) <= 5;
    }

    private void updateCursorOnDrag(MouseEvent e) {
        int x = e.getX();
        double newCursorTime = ((double) (x - panOffset) / getWidth()) * track.getTotalDuration();
        if (newCursorTime >= 0 && newCursorTime <= track.getTotalDuration()) {
            cursorPosition = newCursorTime;
            repaint();
            if (trackPanelListener != null) {
                trackPanelListener.onTrackClicked(cursorPosition);
            }
        }
    }

    private void selectSegment(MouseEvent e) {
        int x = e.getX();
        double clickedTime = ((double) (x - panOffset) / getWidth()) * track.getTotalDuration();
        for (TrackSegment segment : track.getSegments()) {
            if (clickedTime >= segment.getStartTime() && clickedTime <= segment.getEndTime()) {
                mainController.selectSegments(List.of(segment));
                setSelectedSegment(segment);
                repaint();
                break;
            }
        }
    }

    private void clearSelection() {
        mainController.selectSegments(new ArrayList<>());
        deselectSegment();
        repaint();
    }

    public void setZoomLevel(double zoomLevel) {
        this.zoomLevel = zoomLevel;
        revalidate();
        repaint();
    }

    public double getZoomLevel() {
        return zoomLevel;
    }

    public void updateCursorPosition(double time) {
        cursorPosition = time;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawTrack(g);
        drawTimeline(g); // Draw the timeline with time indicators
    }

    private void drawTrack(Graphics g) {
        int panelWidth = (int) (getWidth() * zoomLevel);
        int panelHeight = getHeight();

        // Draw background grid
        g.setColor(new Color(50, 50, 50));
        for (int i = 0; i < panelWidth; i += 20) {
            g.drawLine(i + panOffset, 0, i + panOffset, panelHeight);
        }

        double totalDuration = track.getTotalDuration();
        List<TrackSegment> segments = track.getSegments();

        int x = panOffset;
        for (TrackSegment segment : segments) {
            int segmentWidth = (int) ((segment.getDuration() / totalDuration) * panelWidth);
            g.setColor(new Color(30, 30, 30));
            g.fillRect(x, 0, segmentWidth, panelHeight);

            if (segment.equals(selectedSegment)) {
                g.setColor(Color.GREEN);
                g.drawRect(x, 0, segmentWidth, panelHeight);
            }

            g.setColor(Color.WHITE);
            g.drawString(segment.getFilePath(), x + 5, panelHeight / 2);
            x += segmentWidth;
        }

        if (!thumbnails.isEmpty()) {
            int thumbnailHeight = panelHeight / 2;
            int thumbnailWidth = panelWidth / thumbnails.size();
            for (int i = 0; i < thumbnails.size(); i++) {
                BufferedImage thumbnail = thumbnails.get(i);
                if (thumbnail != null) {
                    g.drawImage(thumbnail, i * thumbnailWidth + panOffset, 10, thumbnailWidth, thumbnailHeight, null);
                } else {
                    logger.log(Level.WARNING, "Thumbnail image is null at index {0}", i);
                }
            }
        }

        if (!waveforms.isEmpty()) {
            int waveformHeight = panelHeight / 2;
            int waveformWidth = panelWidth / waveforms.size();
            for (int i = 0; i < waveforms.size(); i++) {
                BufferedImage waveform = waveforms.get(i);
                if (waveform != null) {
                    g.drawImage(waveform, i * waveformWidth + panOffset, panelHeight / 2 + 10, waveformWidth, waveformHeight, null);
                } else {
                    logger.log(Level.WARNING, "Waveform image is null at index {0}", i);
                }
            }
        }

        int cursorX = (int) ((cursorPosition / totalDuration) * panelWidth) + panOffset;
        g.setColor(Color.RED);
        g.fillRect(cursorX - 2, 0, 4, panelHeight);
    }

    private void drawTimeline(Graphics g) {
        int panelWidth = (int) (getWidth() * zoomLevel);
        int panelHeight = getHeight();
        int timelineHeight = panelHeight / 5; // Reserve a small part of the panel for the timeline

        double totalDuration = track.getTotalDuration();
        double timePerPixel = totalDuration / panelWidth;

        // Draw tick marks and time labels
        g.setFont(new Font("Arial", Font.PLAIN, 10)); // Adjust font size for smaller panel
        for (int i = 0; i < panelWidth; i++) {
            double time = i * timePerPixel;
            if (i % 100 == 0) { // Major tick mark
                g.setColor(Color.WHITE);
                g.drawLine(i + panOffset, panelHeight - timelineHeight, i + panOffset, panelHeight);
                g.drawString(formatTime(time), i + panOffset + 2, panelHeight - timelineHeight - 2);
            } else if (i % 10 == 0) { // Minor tick mark
                g.setColor(Color.LIGHT_GRAY);
                g.drawLine(i + panOffset, panelHeight - timelineHeight / 2, i + panOffset, panelHeight);
            }
        }
    }

    private String formatTime(double time) {
        int hours = (int) (time / 3600);
        int minutes = (int) ((time % 3600) / 60);
        int seconds = (int) (time % 60);
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public void refresh() {
        revalidate();
        repaint();
    }

    public void loadThumbnailsAndWaveformsInBackground() {
        new Thread(this::loadThumbnails).start();
        new Thread(this::loadWaveforms).start();
    }

    private void loadThumbnails() {
        thumbnails.clear();
        double interval = track.getTotalDuration() / 5; // Generate 5 thumbnails for simplicity
        for (int i = 0; i < 5; i++) {
            BufferedImage thumbnail = videoEditor.extractThumbnail(new File(track.getSegments().getFirst().getFilePath()), i * interval);
            if (thumbnail != null) {
                thumbnails.add(thumbnail);
            } else {
                logger.log(Level.WARNING, "Failed to load thumbnail at interval {0}", i * interval);
            }
        }
        repaint();
    }

    private void loadWaveforms() {
        waveforms.clear();
        double interval = track.getTotalDuration() / 5; // Generate 5 waveforms for simplicity
        for (int i = 0; i < 5; i++) {
            BufferedImage waveform = videoEditor.extractWaveform(new File(track.getSegments().getFirst().getFilePath()));
            if (waveform != null) {
                waveforms.add(waveform);
            } else {
                logger.log(Level.WARNING, "Failed to load waveform at interval {0}", i * interval);
            }
        }
        repaint();
    }

    public void setSelectedSegment(TrackSegment segment) {
        this.selectedSegment = segment;
    }

    public void deselectSegment() {
        this.selectedSegment = null;
    }

    public interface TrackPanelListener {
        void onTrackClicked(double time);
    }

    public void setTrackPanelListener(TrackPanelListener listener) {
        this.trackPanelListener = listener;
    }
}
