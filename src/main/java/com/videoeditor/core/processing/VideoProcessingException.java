package com.videoeditor.core.processing;

/**
 * Custom exception for video processing errors.
 */
public class VideoProcessingException extends RuntimeException {
    public VideoProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
