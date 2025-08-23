package com.videoeditor.core.processing;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VideoProcessingService {
    private static final Logger logger = Logger.getLogger(VideoProcessingService.class.getName());

    /**
     * Executes a given FFmpeg command synchronously.
     *
     * @param command The FFmpeg command as a list of arguments.
     * @return The exit code of the command (0 for success, non-zero for failure).
     * @throws VideoProcessingException if the command fails to execute.
     */
    public int executeCommand(List<String> command) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info(line);
            }

            int exitCode = process.waitFor();
            logger.log(Level.INFO, "Command exited with code: {0}", exitCode);
            return exitCode;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to execute command: {0}", command);
            throw new VideoProcessingException("Command execution failed", e);
        }
    }

}