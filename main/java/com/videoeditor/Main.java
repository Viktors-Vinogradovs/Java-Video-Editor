package com.videoeditor;

import com.videoeditor.core.VideoEditor;
import com.videoeditor.ui.MainController;
import com.videoeditor.ui.MainView;
import com.videoeditor.ui.TrackPanel;

import javax.swing.*;
import java.io.File;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        configureLogging();

        SwingUtilities.invokeLater(Main::initializeApplication);

        VideoEditor editor = new VideoEditor();

        File videoFile = new File("path/to/video.mp4");

        // Import video asynchronously
        editor.importVideoAsync(videoFile);

        // Extract thumbnail asynchronously
        editor.extractThumbnailAsync(videoFile, 10.0);

        // Extract waveform asynchronously
        editor.extractWaveformAsync(videoFile);

        // Export video asynchronously
        File outputFile = new File("path/to/output.mp4");
        editor.exportVideoAsync(videoFile, outputFile);

        // The main thread can continue doing other tasks or update the UI
        System.out.println("Asynchronous tasks started.");
    }

    private static void configureLogging() {
        // Configure logger to show messages in the console
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.ALL);
        logger.addHandler(consoleHandler);
        logger.setLevel(Level.ALL);
    }

    private static void initializeApplication() {
        try {
            logger.log(Level.INFO, "Initializing Video Editor Application...");
            VideoEditor videoEditor = new VideoEditor();
            MainView mainView = new MainView();
            MainController mainController = new MainController(videoEditor, mainView);
            new MainController(videoEditor, mainView);
            mainView.setMainController();
            mainView.setVisible(true);
            logger.log(Level.INFO, "Application started successfully.");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Application failed to start.", e);
            JOptionPane.showMessageDialog(null, "Failed to start the application. Please check the logs for more details.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
