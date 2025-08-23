package com.videoeditor.export;

import com.videoeditor.core.processing.VideoProcessingService;
import com.videoeditor.model.timeline.Track;
import com.videoeditor.model.timeline.TrackSegment;
import com.videoeditor.model.project.Project;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Handles video export functionality using FFmpeg
 */
public class ExportService {
    private static final Logger logger = Logger.getLogger(ExportService.class.getName());

    private final VideoProcessingService videoProcessingService;
    private ExportProgressListener progressListener;

    public ExportService() {
        this.videoProcessingService = new VideoProcessingService();
    }

    public void setProgressListener(ExportProgressListener listener) {
        this.progressListener = listener;
    }

    /**
     * Export project to video file
     */
    public CompletableFuture<ExportResult> exportProject(Project project, File outputFile, ExportSettings settings) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Starting export: " + outputFile.getAbsolutePath());

                if (progressListener != null) {
                    progressListener.onProgressUpdate("Preparing export...", 0);
                }

                // Get video and audio tracks
                List<Track> videoTracks = getTracksByType(project, Track.TrackType.VIDEO);
                List<Track> audioTracks = getTracksByType(project, Track.TrackType.AUDIO);

                if (videoTracks.isEmpty()) {
                    throw new RuntimeException("No video tracks found to export");
                }

                // Create temporary directory for processing
                Path tempDir = Files.createTempDirectory("videoeditor_export_");

                try {
                    // Process video timeline
                    File videoFile = processVideoTimeline(videoTracks, tempDir, settings);

                    if (progressListener != null) {
                        progressListener.onProgressUpdate("Processing audio...", 30);
                    }

                    // Process audio timeline (if exists)
                    File audioFile = null;
                    if (!audioTracks.isEmpty()) {
                        audioFile = processAudioTimeline(audioTracks, tempDir);
                    }

                    if (progressListener != null) {
                        progressListener.onProgressUpdate("Combining tracks...", 60);
                    }

                    // Combine video and audio
                    combineVideoAndAudio(videoFile, audioFile, outputFile, settings);

                    if (progressListener != null) {
                        progressListener.onProgressUpdate("Export complete!", 100);
                    }

                    return new ExportResult(true, "Export completed successfully", outputFile);

                } finally {
                    // Cleanup temp directory
                    cleanupTempDirectory(tempDir);
                }

            } catch (Exception e) {
                logger.severe("Export failed: " + e.getMessage());
                if (progressListener != null) {
                    progressListener.onProgressUpdate("Export failed: " + e.getMessage(), -1);
                }
                return new ExportResult(false, "Export failed: " + e.getMessage(), null);
            }
        });
    }

    /**
     * Process video timeline into single video file
     */
    private File processVideoTimeline(List<Track> videoTracks, Path tempDir, ExportSettings settings) {
        List<String> ffmpegCommand = new ArrayList<>();
        ffmpegCommand.add("ffmpeg");
        ffmpegCommand.add("-y"); // Overwrite output files

        // Get all video segments sorted by time
        List<TrackSegment> allSegments = new ArrayList<>();
        for (Track track : videoTracks) {
            allSegments.addAll(track.getSegments());
        }
        allSegments.sort(Comparator.comparingDouble(TrackSegment::getStartTime));

        if (allSegments.isEmpty()) {
            throw new RuntimeException("No video segments found");
        }

        // Create filter complex for timeline
        StringBuilder filterComplex = new StringBuilder();

        // Add input files
        for (TrackSegment segment : allSegments) {
            ffmpegCommand.add("-ss");
            ffmpegCommand.add(String.valueOf(segment.getSourceStartTime()));
            ffmpegCommand.add("-t");
            ffmpegCommand.add(String.valueOf(segment.getDuration()));
            ffmpegCommand.add("-i");
            ffmpegCommand.add(segment.getOriginalFile().getAbsolutePath());
        }

        // Build filter for concatenation
        if (allSegments.size() == 1) {
            // Single segment - just copy
            filterComplex.append("[0:v]scale=")
                    .append(settings.getWidth()).append(":").append(settings.getHeight())
                    .append("[v]");
        } else {
            // Multiple segments - concatenate
            for (int i = 0; i < allSegments.size(); i++) {
                filterComplex.append("[").append(i).append(":v]scale=")
                        .append(settings.getWidth()).append(":").append(settings.getHeight())
                        .append("[v").append(i).append("];");
            }

            // Concatenate all scaled videos
            for (int i = 0; i < allSegments.size(); i++) {
                filterComplex.append("[v").append(i).append("]");
            }
            filterComplex.append("concat=n=").append(allSegments.size()).append(":v=1[v]");
        }

        ffmpegCommand.add("-filter_complex");
        ffmpegCommand.add(filterComplex.toString());
        ffmpegCommand.add("-map");
        ffmpegCommand.add("[v]");

        // Video encoding settings
        ffmpegCommand.add("-c:v");
        ffmpegCommand.add("libx264");
        ffmpegCommand.add("-preset");
        ffmpegCommand.add(settings.getQuality().getPreset());
        ffmpegCommand.add("-crf");
        ffmpegCommand.add(String.valueOf(settings.getQuality().getCrf()));
        ffmpegCommand.add("-r");
        ffmpegCommand.add(String.valueOf(settings.getFrameRate()));

        File outputFile = tempDir.resolve("video_timeline.mp4").toFile();
        ffmpegCommand.add(outputFile.getAbsolutePath());

        logger.info("Executing video processing: " + String.join(" ", ffmpegCommand));

        int exitCode = videoProcessingService.executeCommand(ffmpegCommand);
        if (exitCode != 0) {
            throw new RuntimeException("Video processing failed with exit code: " + exitCode);
        }

        return outputFile;
    }

    /**
     * Process audio timeline into single audio file
     */
    private File processAudioTimeline(List<Track> audioTracks, Path tempDir) {
        List<String> ffmpegCommand = new ArrayList<>();
        ffmpegCommand.add("ffmpeg");
        ffmpegCommand.add("-y");

        // Get all audio segments
        List<TrackSegment> allSegments = new ArrayList<>();
        for (Track track : audioTracks) {
            allSegments.addAll(track.getSegments());
        }
        allSegments.sort(Comparator.comparingDouble(TrackSegment::getStartTime));

        if (allSegments.isEmpty()) {
            return null;
        }

        // Add input files
        for (int i = 0; i < allSegments.size(); i++) {
            TrackSegment segment = allSegments.get(i);
            ffmpegCommand.add("-ss");
            ffmpegCommand.add(String.valueOf(segment.getSourceStartTime()));
            ffmpegCommand.add("-t");
            ffmpegCommand.add(String.valueOf(segment.getDuration()));
            ffmpegCommand.add("-i");
            ffmpegCommand.add(segment.getOriginalFile().getAbsolutePath());
        }

        // Audio filter
        if (allSegments.size() == 1) {
            ffmpegCommand.add("-map");
            ffmpegCommand.add("0:a");
        } else {
            StringBuilder filterComplex = new StringBuilder();
            for (int i = 0; i < allSegments.size(); i++) {
                filterComplex.append("[").append(i).append(":a]");
            }
            filterComplex.append("concat=n=").append(allSegments.size()).append(":v=0:a=1[a]");

            ffmpegCommand.add("-filter_complex");
            ffmpegCommand.add(filterComplex.toString());
            ffmpegCommand.add("-map");
            ffmpegCommand.add("[a]");
        }

        // Audio encoding
        ffmpegCommand.add("-c:a");
        ffmpegCommand.add("aac");
        ffmpegCommand.add("-b:a");
        ffmpegCommand.add("192k");
        ffmpegCommand.add("-ar");
        ffmpegCommand.add("48000");

        File outputFile = tempDir.resolve("audio_timeline.aac").toFile();
        ffmpegCommand.add(outputFile.getAbsolutePath());

        int exitCode = videoProcessingService.executeCommand(ffmpegCommand);
        if (exitCode != 0) {
            throw new RuntimeException("Audio processing failed with exit code: " + exitCode);
        }

        return outputFile;
    }

    /**
     * Combine video and audio into final output
     */
    private void combineVideoAndAudio(File videoFile, File audioFile, File outputFile, ExportSettings settings) {
        List<String> ffmpegCommand = new ArrayList<>();
        ffmpegCommand.add("ffmpeg");
        ffmpegCommand.add("-y");

        // Input video
        ffmpegCommand.add("-i");
        ffmpegCommand.add(videoFile.getAbsolutePath());

        if (audioFile != null) {
            // Input audio
            ffmpegCommand.add("-i");
            ffmpegCommand.add(audioFile.getAbsolutePath());

            // Map both streams
            ffmpegCommand.add("-map");
            ffmpegCommand.add("0:v");
            ffmpegCommand.add("-map");
            ffmpegCommand.add("1:a");
        } else {
            // Video only
            ffmpegCommand.add("-map");
            ffmpegCommand.add("0:v");
        }

        // Copy streams (already processed)
        ffmpegCommand.add("-c");
        ffmpegCommand.add("copy");

        // Output format
        if (settings.getFormat() == ExportFormat.MP4) {
            ffmpegCommand.add("-f");
            ffmpegCommand.add("mp4");
        }

        ffmpegCommand.add(outputFile.getAbsolutePath());

        int exitCode = videoProcessingService.executeCommand(ffmpegCommand);
        if (exitCode != 0) {
            throw new RuntimeException("Final combining failed with exit code: " + exitCode);
        }
    }

    private List<Track> getTracksByType(Project project, Track.TrackType type) {
        return project.getTracks().stream()
                .filter(track -> track.getType() == type)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private void cleanupTempDirectory(Path tempDir) {
        try {
            Files.walk(tempDir)
                    .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            logger.warning("Failed to delete temp file: " + path);
                        }
                    });
        } catch (IOException e) {
            logger.warning("Failed to cleanup temp directory: " + tempDir);
        }
    }

    // Supporting classes
    public static class ExportResult {
        public final boolean success;
        public final String message;
        public final File outputFile;

        public ExportResult(boolean success, String message, File outputFile) {
            this.success = success;
            this.message = message;
            this.outputFile = outputFile;
        }
    }

    public interface ExportProgressListener {
        void onProgressUpdate(String message, int progress);
    }
}