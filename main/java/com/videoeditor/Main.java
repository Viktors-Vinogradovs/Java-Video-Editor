package com.videoeditor;

import com.videoeditor.core.VideoEditor;
import com.videoeditor.ui.MainController;
import com.videoeditor.ui.MainView;

import javax.swing.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        configureLogging();
        SwingUtilities.invokeLater(Main::initializeApplication);
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
