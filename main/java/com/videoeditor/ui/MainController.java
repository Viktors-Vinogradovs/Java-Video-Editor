package com.videoeditor.ui;

import com.videoeditor.core.VideoEditor;
import com.videoeditor.model.Track;
import com.videoeditor.model.TrackSegment;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class MainController {
    static {
        Logger.getLogger(MainController.class.getName());
    }

    private final VideoEditor videoEditor;
    private final MainView mainView;
    private Track track;
    private File currentVideoFile;
    private final Timer playbackTimer;
    private List<TrackSegment> selectedSegments = new ArrayList<>();

    public MainController(VideoEditor videoEditor, MainView mainView) {
        this.videoEditor = videoEditor;
        this.mainView = mainView;
        this.mainView.setMainController();

        this.mainView.getImportButton().addActionListener(e -> importVideo());
        this.mainView.getExportButton().addActionListener(e -> exportVideo());
        this.mainView.getPlayPauseButton().addActionListener(e -> togglePlayPause());
        this.mainView.getReplayButton().addActionListener(e -> replayVideo());
        this.mainView.getZoomInButton().addActionListener(e -> zoomIn());
        this.mainView.getZoomOutButton().addActionListener(e -> zoomOut());
        this.mainView.getCutButton().addActionListener(e -> cutSegment());
        this.mainView.getDeleteButton().addActionListener(e -> deleteSelectedSegment());

        playbackTimer = new Timer(100, e -> updateCursor());
    }

    public void importVideo() {
        JFileChooser fileChooser = mainView.getFileChooser();
        int result = fileChooser.showOpenDialog(mainView);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            currentVideoFile = file;
            videoEditor.importVideo(file);
            mainView.getMediaPlayerComponent().mediaPlayer().media().startPaused(file.getAbsolutePath());

            track = new Track();
            double duration = videoEditor.getVideoDuration(file);
            track.addSegment(new TrackSegment(file.getAbsolutePath(), "", 0, duration));

            TrackPanel trackPanel = new TrackPanel(track, videoEditor, this);
            trackPanel.setTrackPanelListener(this::seekToTime);
            trackPanel.loadThumbnailsAndWaveformsInBackground();

            mainView.getTrackPanel().removeAll();
            mainView.getTrackPanel().add(trackPanel);

            mainView.getTrackPanel().revalidate();
            mainView.getTrackPanel().repaint();

        }
    }

    private void seekToTime(double time) {
        if (currentVideoFile != null) {
            mainView.getMediaPlayerComponent().mediaPlayer().controls().setTime((long) (time * 1000));
            mainView.getTrackPanel().getMousePosition();
        }
    }

    public void exportVideo() {
        JFileChooser fileChooser = mainView.getFileChooser();
        int result = fileChooser.showSaveDialog(mainView);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            videoEditor.exportVideo(currentVideoFile, file);
        }
    }

    public void togglePlayPause() {
        if (currentVideoFile != null) {
            if (mainView.getMediaPlayerComponent().mediaPlayer().status().isPlaying()) {
                mainView.getMediaPlayerComponent().mediaPlayer().controls().pause();
                playbackTimer.stop();
            } else {
                mainView.getMediaPlayerComponent().mediaPlayer().controls().play();
                playbackTimer.start();
            }
        }
    }

    public void replayVideo() {
        if (currentVideoFile != null) {
            mainView.getMediaPlayerComponent().mediaPlayer().controls().setTime(0);
            mainView.getMediaPlayerComponent().mediaPlayer().controls().play();
            playbackTimer.start();
        }
    }

    public void zoomIn() {
        zoom(1.1);
    }

    public void zoomOut() {
        zoom(0.9);
    }

    private void zoom(double factor) {
        Component trackComponent = mainView.getTrackPanel().getComponent(0);
        if (trackComponent instanceof TrackPanel) {
            ((TrackPanel) trackComponent).setZoomLevel(((TrackPanel) trackComponent).getZoomLevel() * factor);
        }


    }

    public void cutSegment() {
        if (selectedSegments.size() == 1) {
            TrackSegment selectedSegment = selectedSegments.getFirst();
            double cutTime = mainView.getMediaPlayerComponent().mediaPlayer().status().time() / 1000.0;
            double relativeCutTime = cutTime - selectedSegment.getStartTime();
            track.splitSegment(selectedSegment, relativeCutTime);
            refreshPanels();
        }
    }

    public void deleteSelectedSegment() {
        if (!selectedSegments.isEmpty()) {
            List<TrackSegment> modifiableSegments = new ArrayList<>(selectedSegments);
            for (TrackSegment selectedSegment : modifiableSegments) {
                double startTime = selectedSegment.getStartTime();
                double endTime = startTime + selectedSegment.getDuration();
                currentVideoFile = videoEditor.deleteSegment(currentVideoFile, startTime, endTime);
                track.removeSegment(selectedSegment);
            }
            selectedSegments.clear();
            if (currentVideoFile != null) {
                mainView.getMediaPlayerComponent().mediaPlayer().media().startPaused(currentVideoFile.getAbsolutePath());
            }
            refreshPanels();
        }
    }

    private void refreshPanels() {
        Component trackComponent = mainView.getTrackPanel().getComponent(0);
        if (trackComponent instanceof TrackPanel) {
            ((TrackPanel) trackComponent).refresh();
        }
    }

    public void selectSegments(List<TrackSegment> segments) {
        this.selectedSegments = new ArrayList<>(segments);
    }

    private void updateCursor() {
        long currentTime = mainView.getMediaPlayerComponent().mediaPlayer().status().time();
        double currentTimeSeconds = currentTime / 1000.0;
        Component trackComponent = mainView.getTrackPanel().getComponent(0);
        if (trackComponent instanceof TrackPanel) {
            ((TrackPanel) trackComponent).updateCursorPosition(currentTimeSeconds);
        }

    }
}
