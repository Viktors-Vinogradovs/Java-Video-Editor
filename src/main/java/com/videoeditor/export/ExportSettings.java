package com.videoeditor.export;

/**
 * Export configuration settings
 */
public class ExportSettings {
    private int width = 1920;
    private int height = 1080;
    private double frameRate = 30.0;
    private ExportFormat format = ExportFormat.MP4;
    private ExportQuality quality = ExportQuality.HIGH;


    // Getters and setters
    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }

    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }

    public double getFrameRate() { return frameRate; }
    public void setFrameRate(double frameRate) { this.frameRate = frameRate; }

    public ExportFormat getFormat() { return format; }
    public void setFormat(ExportFormat format) { this.format = format; }

    public ExportQuality getQuality() { return quality; }
    public void setQuality(ExportQuality quality) { this.quality = quality; }

    @Override
    public String toString() {
        return String.format("%dx%d @ %.1ffps, %s, %s", width, height, frameRate, format, quality);
    }
}

enum ExportFormat {
    MP4("MP4", ".mp4"),
    MOV("QuickTime", ".mov"),
    AVI("AVI", ".avi");

    private final String displayName;
    private final String extension;

    ExportFormat(String displayName, String extension) {
        this.displayName = displayName;
        this.extension = extension;
    }

    public String getDisplayName() { return displayName; }
    public String getExtension() { return extension; }
}

enum ExportQuality {
    LOW("Low", "ultrafast", 28),
    MEDIUM("Medium", "fast", 23),
    HIGH("High", "medium", 18),
    HIGHEST("Highest", "slow", 15);

    private final String displayName;
    private final String preset;
    private final int crf;

    ExportQuality(String displayName, String preset, int crf) {
        this.displayName = displayName;
        this.preset = preset;
        this.crf = crf;
    }

    public String getDisplayName() { return displayName; }
    public String getPreset() { return preset; }
    public int getCrf() { return crf; }
}