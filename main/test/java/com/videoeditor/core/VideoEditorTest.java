package com.videoeditor.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class VideoEditorTest {

    private VideoEditor videoEditor;

    @BeforeEach
    public void setUp() {
        videoEditor = new VideoEditor();
    }

    @Test
    public void shouldImportVideo() {
        // Create a mock file
        File mockFile = mock(File.class);
        when(mockFile.getAbsolutePath()).thenReturn("path/to/video.mp4");

        // Call the method
        videoEditor.importVideo(mockFile);

        // Verify that the metadataCache contains the file's path
        assertThat(videoEditor.metadataCache).containsKey("path/to/video.mp4");
    }

    @Test
    public void shouldExtractThumbnail() {
        // This test should ideally be run with a real file in an integration test environment.
        File mockFile = mock(File.class);
        when(mockFile.getAbsolutePath()).thenReturn("path/to/video.mp4");

        BufferedImage thumbnail = videoEditor.extractThumbnail(mockFile, 10.0);

        assertThat(thumbnail).isNotNull();
        assertThat(videoEditor.thumbnailCache).containsKey("path/to/video.mp4_thumb_10.0");
    }

    // Add more tests as necessary for other methods
}
