package com.videoeditor;

import com.videoeditor.ui.controllers.MainController;
import com.videoeditor.ui.panels.MainView;
import com.videoeditor.ui.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.ConsoleHandler;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        // Configure detailed logging
        configureLogging();

        // Set system properties
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("apple.awt.application.name", "VideoEdit Pro");

        // Log system info
        logger.info("Java Version: " + System.getProperty("java.version"));
        logger.info("OS: " + System.getProperty("os.name"));
        logger.info("VLC Native Path: " + System.getProperty("jna.library.path"));

        // Ensure we're on EDT
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(Main::initializeApplication);
        } else {
            initializeApplication();
        }
    }

    private static void configureLogging() {
        Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(Level.ALL);

        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.ALL);
        rootLogger.addHandler(consoleHandler);
    }

    private static void initializeApplication() {
        try {
            logger.info("=== Starting Video Editor Application ===");

            // Set look and feel first
            try {
                UIUtils.setLookAndFeel();
                logger.info("Look and feel set successfully");
            } catch (Exception e) {
                logger.warning("Failed to set look and feel: " + e.getMessage());
                // Continue anyway with default L&F
            }

            // Create main view
            logger.info("Creating MainView...");
            MainView mainView = new MainView();
            logger.info("MainView created successfully");

            // Create controller
            logger.info("Creating MainController...");
            new MainController(mainView);
            logger.info("MainController created successfully");

            // Make visible
            logger.info("Making window visible...");
            mainView.setVisible(true);

            // Force repaint
            SwingUtilities.invokeLater(() -> {
                mainView.revalidate();
                mainView.repaint();
                logger.info("Window should be visible now");

                // Log component hierarchy
                logComponentHierarchy(mainView, 0);
            });

        } catch (Exception e) {
            logger.severe("Failed to initialize application: " + e.getMessage());
            e.printStackTrace();

            // Show error dialog
            JOptionPane.showMessageDialog(
                    null,
                    "Failed to start Video Editor:\n" + e.getMessage() + "\n\nCheck console for details.",
                    "Startup Error",
                    JOptionPane.ERROR_MESSAGE
            );

            System.exit(1);
        }
    }

    private static void logComponentHierarchy(Component comp, int level) {
        String indent = "  ".repeat(level);
        logger.info(indent + comp.getClass().getSimpleName() +
                " [" + comp.getBounds() + "]" +
                " visible=" + comp.isVisible());

        if (comp instanceof Container) {
            Container container = (Container) comp;
            for (Component child : container.getComponents()) {
                logComponentHierarchy(child, level + 1);
            }
        }
    }
}