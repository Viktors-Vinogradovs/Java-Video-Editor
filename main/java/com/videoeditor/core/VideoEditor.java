package com.videoeditor.core;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VideoEditor {

    private static final Logger logger = Logger.getLogger(VideoEditor.class.getName());

    public void importVideo(File file) {
        logger.log(Level.INFO, "Importing video: {0}", file.getAbsolutePath());
        displayVideoDetails(file);
    }

    private void displayVideoDetails(File file) {
        String command = String.format("ffmpeg -i \"%s\" -f null -", file.getAbsolutePath());
        executeCommand(command);
    }

    public BufferedImage extractThumbnail(File file, double time) {
        String outputPath = "thumbnail_" + time + ".png"; // Unique output path for each thumbnail
        String command = String.format("ffmpeg -ss %f -i \"%s\" -vframes 1 -q:v 2 \"%s\"", time, file.getAbsolutePath(), outputPath);
        logger.log(Level.INFO, "Executing thumbnail extraction command: {0}", command);
        int exitCode = executeCommand(command);

        if (exitCode == 0) {
            try {
                BufferedImage thumbnail = ImageIO.read(new File(outputPath));
                File thumbnailFile = new File(outputPath);
                if (!thumbnailFile.delete()) {
                    logger.log(Level.WARNING, "Failed to delete thumbnail file: {0}", outputPath);
                }
                return thumbnail;
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to read thumbnail image", e);
                return null;
            }
        } else {
            logger.log(Level.SEVERE, "FFmpeg command for thumbnail extraction failed with exit code: {0}", exitCode);
            return null;
        }
    }

    public BufferedImage extractWaveform(File file) {
        String outputPath = "waveform.png";
        String command = String.format("ffmpeg -i \"%s\" -filter_complex \"[0:a]showwavespic=s=640x120:colors=blue\" -frames:v 1 -update 1 \"%s\"", file.getAbsolutePath(), outputPath);
        logger.log(Level.INFO, "Executing command: {0}", command);
        int exitCode = executeCommand(command);

        if (exitCode == 0) {
            try {
                BufferedImage waveform = ImageIO.read(new File(outputPath));
                if (waveform != null) {
                    logger.log(Level.INFO, "Waveform image read successfully: {0}", outputPath);
                } else {
                    logger.log(Level.SEVERE, "Waveform image is null: {0}", outputPath);
                }
                File waveformFile = new File(outputPath);
                if (!waveformFile.delete()) {
                    logger.log(Level.WARNING, "Failed to delete waveform file: {0}", outputPath);
                }
                return waveform;
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to read waveform image", e);
                return null;
            }
        } else {
            logger.log(Level.SEVERE, "FFmpeg command for waveform extraction failed with exit code: {0}", exitCode);
            return null;
        }
    }

    public void exportVideo(File inputFile, File outputFile) {
        String command = String.format("ffmpeg -i \"%s\" -c copy \"%s\"", inputFile.getAbsolutePath(), outputFile.getAbsolutePath());
        int exitCode = executeCommand(command);

        if (exitCode == 0) {
            logger.log(Level.INFO, "Exported video to: {0}", outputFile.getAbsolutePath());
        } else {
            logger.log(Level.SEVERE, "FFmpeg command for exporting video failed with exit code: {0}", exitCode);
        }
    }

    public int getVideoDuration(File file) {
        String command = String.format("ffmpeg -i \"%s\"", file.getAbsolutePath());
        int duration = 0;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            Pattern pattern = Pattern.compile("Duration: (\\d+):(\\d+):(\\d+\\.\\d+)");
            while ((line = reader.readLine()) != null) {
                logger.log(Level.INFO, line); // Debug line
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    int hours = Integer.parseInt(matcher.group(1));
                    int minutes = Integer.parseInt(matcher.group(2));
                    double seconds = Double.parseDouble(matcher.group(3));
                    duration = (int) (hours * 3600 + minutes * 60 + seconds);
                    break;
                }
            }

            process.waitFor();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to get video duration", e);
        }
        return duration;
    }

    public File deleteSegment(File inputFile, double startTime, double endTime) {
        File outputFile = new File("output_trimmed.mp4");
        String command = String.format(
                "ffmpeg -i \"%s\" -vf \"select='not(between(t,%f,%f))',setpts=N/FRAME_RATE/TB\" -af \"aselect='not(between(t,%f,%f))',asetpts=N/SR/TB\" -y \"%s\"",
                inputFile.getAbsolutePath(), startTime, endTime, startTime, endTime, outputFile.getAbsolutePath()
        );
        logger.log(Level.INFO, "Executing command: {0}", command);
        int exitCode = executeCommand(command);

        if (exitCode == 0) {
            logger.log(Level.INFO, "Deleted segment from {0} to {1}, output file: {2}", new Object[]{startTime, endTime, outputFile.getAbsolutePath()});
            return outputFile;
        } else {
            logger.log(Level.SEVERE, "FFmpeg command for deleting segment failed with exit code: {0}", exitCode);
            return inputFile;
        }
    }

    private int executeCommand(String command) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                logger.log(Level.INFO, line);
            }

            int exitCode = process.waitFor();
            logger.log(Level.INFO, "Exited with code: {0}", exitCode);
            return exitCode;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Command execution failed", e);
            return -1;
        }
    }
}
