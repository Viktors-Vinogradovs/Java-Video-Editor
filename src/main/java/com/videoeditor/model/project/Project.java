package com.videoeditor.model.project;

import com.videoeditor.model.timeline.Track;
import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

public class Project {
    private static final Logger logger = Logger.getLogger(Project.class.getName());

    private final String name;
    private final List<Track> tracks;
    private final List<File> mediaFiles;

    public Project(String name) {
        UUID.randomUUID();
        this.name = name != null ? name : "Untitled Project";
        this.tracks = new ArrayList<>();
        this.mediaFiles = new ArrayList<>();
        LocalDateTime.now();
        LocalDateTime.now();

        logger.info("Created new project: " + this.name);
    }

    // Project modification tracking
    public void markAsModified() {
        LocalDateTime.now();
        logger.fine("Project marked as modified: " + name);
    }

    // Track management
    public void addTrack(Track track) {
        if (track != null && !tracks.contains(track)) {
            tracks.add(track);
            markAsModified();
            logger.info("Added track to project: " + track.getName());
        }
    }

    // Project statistics
    public double getTotalDuration() {
        return tracks.stream()
                .mapToDouble(Track::getTotalDuration)
                .max()
                .orElse(0.0);
    }

    // Getters and setters
    public String getName() { return name; }
    public List<Track> getTracks() { return new ArrayList<>(tracks); }
    @Override
    public String toString() {
        return String.format("Project{name='%s', tracks=%d, mediaFiles=%d, duration=%.2fs}",
                name, tracks.size(), mediaFiles.size(), getTotalDuration());
    }
}