package com.videoeditor.core.thumbnails;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

/**
 * Service for generating and caching video thumbnails for timeline display
 */
public class VideoThumbnailService {
    private static final Logger logger = Logger.getLogger(VideoThumbnailService.class.getName());

    // Thumbnail settings
    public static final int THUMBNAIL_WIDTH = 80;
    public static final int THUMBNAIL_HEIGHT = 45;

    public VideoThumbnailService() {
        Path thumbnailCacheDir = createThumbnailCacheDirectory();
        logger.info("VideoThumbnailService initialized with cache directory: " + thumbnailCacheDir);
    }

    private Path createThumbnailCacheDirectory() {
        try {
            Path cacheDir = Paths.get(System.getProperty("java.io.tmpdir"), "videoeditor_thumbnails");
            Files.createDirectories(cacheDir);
            return cacheDir;
        } catch (IOException e) {
            logger.warning("Failed to create thumbnail cache directory: " + e.getMessage());
            return Paths.get(System.getProperty("java.io.tmpdir"));
        }
    }


}