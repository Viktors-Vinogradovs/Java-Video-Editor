package com.videoeditor.ui.panels;

import com.videoeditor.config.UIConstants;
import com.videoeditor.core.processing.VideoProcessingService;
import com.videoeditor.core.thumbnails.VideoThumbnailService;
import com.videoeditor.model.timeline.Track;
import com.videoeditor.model.timeline.TrackListener;
import com.videoeditor.model.timeline.TrackSegment;
import com.videoeditor.ui.controllers.TrackManager;
import com.videoeditor.ui.events.VideoEditorListener;
import com.videoeditor.ui.utils.UIUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import com.videoeditor.ui.utils.TimeUtils;

/**
 * Complete Enhanced TrackPanel with improved thumbnail generation and smooth cursor
 */
public class TrackPanel extends JPanel implements TrackListener {
    private static final Logger logger = Logger.getLogger(TrackPanel.class.getName());

    // Enhanced UI Constants
    private static final int TRACK_HEIGHT = 90;
    private static final int AUDIO_TRACK_HEIGHT = 70;
    private static final int TRACK_PADDING = 10;
    private static final int SEGMENT_PADDING = 4;
    private static final int CURSOR_WIDTH = 3;
    private static final int TIMELINE_HEIGHT = 40;
    private static final int TRACK_LABEL_WIDTH = 160;
    private static final int HANDLE_SIZE = 12;

    // Enhanced Colors for different track types
    private static final Map<Track.TrackType, Color> SEGMENT_COLORS = new HashMap<>();
    private static final Map<Track.TrackType, Color> TRACK_BACKGROUND_COLORS = new HashMap<>();

    static {
        SEGMENT_COLORS.put(Track.TrackType.VIDEO, new Color(0, 122, 255));
        SEGMENT_COLORS.put(Track.TrackType.AUDIO, new Color(52, 199, 89));
        SEGMENT_COLORS.put(Track.TrackType.SUBTITLE, new Color(255, 149, 0));

        TRACK_BACKGROUND_COLORS.put(Track.TrackType.VIDEO, new Color(45, 45, 48));
        TRACK_BACKGROUND_COLORS.put(Track.TrackType.AUDIO, new Color(40, 48, 45));
        TRACK_BACKGROUND_COLORS.put(Track.TrackType.SUBTITLE, new Color(48, 45, 40));
    }

    // Core components
    private final List<TrackManager> trackManagers;
    private final VideoEditorListener editorListener;

    // Services
    private VideoProcessingService videoProcessingService;

    // View state
    private double zoomLevel = 1.0;
    private double viewStartTime = 0.0;
    private double pixelsPerSecond = 60.0;

    // Enhanced interaction state
    private InteractionMode mode = InteractionMode.NONE;
    private TrackSegment activeSegment = null;
    private Track activeTrack = null;
    private Point dragStart = null;
    private double dragStartTime = 0;
    private Rectangle selectionRect = null;
    private ResizeHandle activeHandle = ResizeHandle.NONE;

    // Cursor management
    private double targetCursorPosition = 0.0;
    private double currentCursorPosition = 0.0;
    private boolean isDraggingCursor = false;
    private boolean isUserDragging = false;

    private static final double CURSOR_LERP_SPEED = 0.25;
    private static final double CURSOR_SNAP_THRESHOLD = 0.01;

    private float cursorPulse = 0.0f;

    // Video thumbnails cache
    private final Map<String, BufferedImage[]> thumbnailSequenceCache = new HashMap<>();

    // Interaction modes
    private enum InteractionMode {
        NONE, DRAGGING_CURSOR, DRAGGING_SEGMENT, RESIZING_SEGMENT, SELECTING
    }

    private enum ResizeHandle {
        NONE, LEFT, RIGHT
    }

    public TrackPanel(TrackManager videoTrackManager, VideoEditorListener editorListener) {
        this.trackManagers = new ArrayList<>();
        this.trackManagers.add(videoTrackManager);
        this.editorListener = editorListener;

        setupPanel();
        setupEnhancedEventHandlers();
        setupSmoothCursorSystem();
        initializeServices();

        videoTrackManager.addListener(this);
        logger.info("Enhanced TrackPanel initialized with improved thumbnail system");
    }

    /**
     * Add audio track manager for multi-track support
     */
    public void addTrackManager(TrackManager audioTrackManager) {
        this.trackManagers.add(audioTrackManager);
        audioTrackManager.addListener(this);
        updatePanelSize();
        repaint();
        logger.info("Audio track manager added to TrackPanel");
    }

    private void setupPanel() {
        setBackground(UIConstants.BACKGROUND_PRIMARY);
        setPreferredSize(new Dimension(1000, calculatePanelHeight()));
        setDoubleBuffered(true);
        setFocusable(true);
        ToolTipManager.sharedInstance().registerComponent(this);
        setupKeyBindings();
    }

    private void setupKeyBindings() {
        InputMap inputMap = getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = getActionMap();

        bindKey(inputMap, actionMap, KeyEvent.VK_DELETE, 0, "delete", this::deleteSelectedSegments);
        bindKey(inputMap, actionMap, KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK, "selectAll", this::selectAllSegments);
        bindKey(inputMap, actionMap, KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK, "copy", this::copySelectedSegments);
        bindKey(inputMap, actionMap, KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK, "split", this::splitAtCursor);
    }

    private void bindKey(InputMap inputMap, ActionMap actionMap, int keyCode, int modifiers,
                         String name, Runnable action) {
        inputMap.put(KeyStroke.getKeyStroke(keyCode, modifiers), name);
        actionMap.put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    private void setupEnhancedEventHandlers() {
        EnhancedMouseHandler mouseHandler = new EnhancedMouseHandler();
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        addMouseWheelListener(mouseHandler);
    }

    private void setupSmoothCursorSystem() {
        // FIXED: Faster interpolation for more responsive cursor during playback
        // 60 FPS for smoothness
        // Only smooth when not playing and not dragging
        // During playback, cursor follows immediately without smoothing
        // Smooth cursor animation
        Timer cursorSmoothTimer = new Timer(16, e -> { // 60 FPS for smoothness
            boolean playing = isCurrentlyPlaying();

            if (!isUserDragging && !playing) {
                // Only smooth when not playing and not dragging
                if (Math.abs(targetCursorPosition - currentCursorPosition) > CURSOR_SNAP_THRESHOLD) {
                    double diff = targetCursorPosition - currentCursorPosition;
                    currentCursorPosition += diff * CURSOR_LERP_SPEED;

                    if (Math.abs(diff) < CURSOR_SNAP_THRESHOLD) {
                        currentCursorPosition = targetCursorPosition;
                    }

                    repaintCursorArea();
                }
            }
            // During playback, cursor follows immediately without smoothing
            else if (playing) {
                currentCursorPosition = targetCursorPosition;
                repaintCursorArea();
            }
        });
        cursorSmoothTimer.start();

        // Pulse animation timer
        // Animation
        Timer cursorAnimationTimer = new Timer(50, e -> {
            cursorPulse += 0.15f;
            if (cursorPulse > Math.PI * 2) cursorPulse = 0;

            if (isDraggingCursor) {
                repaintCursorArea();
            }
        });
        cursorAnimationTimer.start();
    }

    private void initializeServices() {
        try {
            this.videoProcessingService = new VideoProcessingService();
            logger.info("Video processing and thumbnail services initialized");
        } catch (Exception e) {
            logger.warning("Failed to initialize services: " + e.getMessage());
        }
    }

    private void repaintCursorArea() {
        // Use target position for immediate visual feedback during playback
        boolean playing = isCurrentlyPlaying();
        double repaintPosition = playing ? targetCursorPosition : currentCursorPosition;
        int cursorX = timeToPixel(repaintPosition);
        repaint(cursorX - 25, 0, 50, getHeight()); // Slightly wider repaint area
    }

    // ============ ENHANCED PAINTING WITH IMPROVED THUMBNAILS ============

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        UIUtils.setupRenderingHints(g2d);

        updatePanelSize();

        // Draw all layers
        drawBackground(g2d);
        drawTimeline(g2d);
        drawGrid(g2d);
        drawAllTracks(g2d);
        drawEnhancedSmoothCursor(g2d);
        drawSelectionAndHover(g2d);
    }

    private void drawBackground(Graphics2D g2d) {
        GradientPaint gradient = new GradientPaint(
                0, 0, UIConstants.BACKGROUND_SECONDARY,
                0, getHeight(), UIConstants.BACKGROUND_PRIMARY
        );
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, getWidth(), getHeight());
    }

    private void drawTimeline(Graphics2D g2d) {
        g2d.setColor(UIConstants.GLASS_BACKGROUND);
        g2d.fillRoundRect(0, 0, getWidth(), TIMELINE_HEIGHT, UIConstants.RADIUS_SM, UIConstants.RADIUS_SM);

        g2d.setColor(UIConstants.GLASS_BORDER);
        g2d.drawRoundRect(0, 0, getWidth() - 1, TIMELINE_HEIGHT - 1, UIConstants.RADIUS_SM, UIConstants.RADIUS_SM);

        drawTimeMarkers(g2d);
    }

    private void drawTimeMarkers(Graphics2D g2d) {
        g2d.setFont(UIUtils.createFont(UIConstants.FONT_SIZE_XS, Font.PLAIN));

        double timeStep = calculateTimeStep() / 4;
        double startTime = Math.floor(viewStartTime / timeStep) * timeStep;

        for (double time = startTime; time <= viewStartTime + getWidth() / pixelsPerSecond; time += timeStep) {
            int x = timeToPixel(time);
            if (x >= 0 && x <= getWidth()) {
                boolean isMajor = (Math.round(time / (timeStep * 4)) * (timeStep * 4)) == Math.round(time * 100) / 100.0;

                if (isMajor) {
                    g2d.setColor(UIConstants.ACCENT_PRIMARY);
                    g2d.setStroke(new BasicStroke(2f));
                    g2d.drawLine(x, TIMELINE_HEIGHT - 12, x, TIMELINE_HEIGHT - 4);

                    String timeLabel = TimeUtils.formatTime(time);
                    FontMetrics fm = g2d.getFontMetrics();
                    int labelWidth = fm.stringWidth(timeLabel);

                    g2d.setColor(UIConstants.TEXT_PRIMARY);
                    g2d.drawString(timeLabel, x - labelWidth / 2, 20);
                } else {
                    g2d.setColor(UIConstants.withAlpha(UIConstants.ACCENT_PRIMARY, 100));
                    g2d.setStroke(new BasicStroke(1f));
                    g2d.drawLine(x, TIMELINE_HEIGHT - 8, x, TIMELINE_HEIGHT - 4);
                }
            }
        }
        g2d.setStroke(new BasicStroke(1f));
    }

    private void drawGrid(Graphics2D g2d) {
        if (!isSnapToGrid()) return;

        g2d.setColor(UIConstants.withAlpha(UIConstants.ACCENT_PRIMARY, 20));
        float[] dash = {3.0f, 6.0f};
        g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));

        double gridSize = getGridSize();
        double startTime = Math.floor(viewStartTime / gridSize) * gridSize;

        for (double time = startTime; time <= viewStartTime + getWidth() / pixelsPerSecond; time += gridSize) {
            int x = timeToPixel(time);
            if (x >= TRACK_LABEL_WIDTH && x <= getWidth()) {
                g2d.drawLine(x, TIMELINE_HEIGHT, x, getHeight());
            }
        }
        g2d.setStroke(new BasicStroke(1f));
    }

    private void drawAllTracks(Graphics2D g2d) {
        int y = TIMELINE_HEIGHT + TRACK_PADDING;

        for (TrackManager trackManager : trackManagers) {
            for (Track track : trackManager.getTracks()) {
                int trackHeight = getTrackHeight(track);
                drawCompleteTrack(g2d, track, y, trackHeight);
                y += trackHeight + TRACK_PADDING;
            }
        }
    }

    private int getTrackHeight(Track track) {
        return track.getType() == Track.TrackType.AUDIO ? AUDIO_TRACK_HEIGHT : TRACK_HEIGHT;
    }

    private void drawCompleteTrack(Graphics2D g2d, Track track, int y, int trackHeight) {
        // Track background with enhanced styling
        Color bgColor = TRACK_BACKGROUND_COLORS.getOrDefault(track.getType(), UIConstants.SURFACE_LOW);
        if (!track.isVisible()) bgColor = UIConstants.darken(bgColor, 0.3f);
        if (track.isLocked()) bgColor = UIConstants.darken(bgColor, 0.2f);

        g2d.setColor(bgColor);
        g2d.fillRoundRect(0, y, getWidth(), trackHeight, UIConstants.RADIUS_MD, UIConstants.RADIUS_MD);

        // Track border with type-specific accent
        Color borderColor = SEGMENT_COLORS.getOrDefault(track.getType(), UIConstants.GLASS_BORDER);
        g2d.setColor(UIConstants.withAlpha(borderColor, 60));
        g2d.drawRoundRect(0, y, getWidth() - 1, trackHeight - 1, UIConstants.RADIUS_MD, UIConstants.RADIUS_MD);

        // Draw track header and segments
        drawCompleteTrackHeader(g2d, track, y, trackHeight);
        drawAllSegmentsInTrack(g2d, track, y, trackHeight);
    }

    private void drawCompleteTrackHeader(Graphics2D g2d, Track track, int y, int trackHeight) {
        // Header background with enhanced gradient
        GradientPaint headerGradient = new GradientPaint(
                0, y, UIConstants.SURFACE_HIGH,
                0, y + trackHeight, UIConstants.SURFACE_MEDIUM
        );
        g2d.setPaint(headerGradient);
        g2d.fillRoundRect(0, y, TRACK_LABEL_WIDTH, trackHeight, UIConstants.RADIUS_MD, 0);

        // Type indicator with enhanced styling
        Color typeColor = SEGMENT_COLORS.getOrDefault(track.getType(), UIConstants.ACCENT_PRIMARY);
        g2d.setColor(typeColor);
        g2d.fillOval(12, y + 8, 20, 20);

        // Track type icon
        g2d.setColor(Color.WHITE);
        g2d.setFont(UIUtils.createFont(12, Font.BOLD));
        String icon = track.getType().getIcon();
        FontMetrics iconFm = g2d.getFontMetrics();
        int iconX = 22 - iconFm.stringWidth(icon) / 2;
        int iconY = y + 18 + iconFm.getAscent() / 2;
        g2d.drawString(icon, iconX, iconY);

        // Track name and type
        g2d.setColor(UIConstants.TEXT_PRIMARY);
        g2d.setFont(UIUtils.createFont(UIConstants.FONT_SIZE_SM, Font.BOLD));
        g2d.drawString(track.getName(), 40, y + 20);

        g2d.setColor(UIConstants.TEXT_SECONDARY);
        g2d.setFont(UIUtils.createFont(UIConstants.FONT_SIZE_XS, Font.PLAIN));
        g2d.drawString(track.getType().getDisplayName(), 40, y + 35);

        // Control indicators
        drawTrackControls(g2d, track, y, trackHeight);
    }

    private void drawTrackControls(Graphics2D g2d, Track track, int y, int trackHeight) {
        int controlY = y + trackHeight - 20;
        int controlX = 8;

        // Mute indicator
        if (track.isMuted()) {
            g2d.setColor(UIConstants.ACCENT_ERROR);
            g2d.fillRoundRect(controlX, controlY, 16, 12, 4, 4);
            g2d.setColor(Color.WHITE);
            g2d.setFont(UIUtils.createFont(8, Font.BOLD));
            g2d.drawString("M", controlX + 5, controlY + 9);
            controlX += 20;
        }

        // Solo indicator (for audio tracks)
        if (track.getType() == Track.TrackType.AUDIO && track.isSoloMode()) {
            g2d.setColor(UIConstants.ACCENT_WARNING);
            g2d.fillRoundRect(controlX, controlY, 16, 12, 4, 4);
            g2d.setColor(Color.WHITE);
            g2d.setFont(UIUtils.createFont(8, Font.BOLD));
            g2d.drawString("S", controlX + 5, controlY + 9);
            controlX += 20;
        }

        // Lock indicator
        if (track.isLocked()) {
            g2d.setColor(UIConstants.ACCENT_WARNING);
            g2d.fillRoundRect(controlX, controlY, 16, 12, 4, 4);
            g2d.setColor(Color.WHITE);
            g2d.setFont(UIUtils.createFont(8, Font.BOLD));
            g2d.drawString("L", controlX + 5, controlY + 9);
        }

        // Volume indicator for audio tracks
        if (track.getType() == Track.TrackType.AUDIO) {
            drawVolumeIndicator(g2d, track, controlY);
        }
    }

    private void drawVolumeIndicator(Graphics2D g2d, Track track, int y) {
        if (track.isMuted()) return;

        int width = 30;
        int height = 8;
        double volume = track.getVolume();

        // Background
        g2d.setColor(UIConstants.SURFACE_LOW);
        g2d.fillRoundRect(120, y, width, height, 4, 4);

        // Volume level
        int volumeWidth = (int) (width * volume);
        Color volumeColor = volume > 1.0 ? UIConstants.ACCENT_WARNING : UIConstants.ACCENT_SUCCESS;
        g2d.setColor(volumeColor);
        g2d.fillRoundRect(120, y, volumeWidth, height, 4, 4);

        // Border
        g2d.setColor(UIConstants.BORDER_DEFAULT);
        g2d.drawRoundRect(120, y, width - 1, height - 1, 4, 4);
    }

    private void drawAllSegmentsInTrack(Graphics2D g2d, Track track, int trackY, int trackHeight) {
        for (TrackSegment segment : track.getSegments()) {
            drawCompleteSegmentWithThumbnailSequence(g2d, segment, track, trackY, trackHeight);
        }
    }

    private void drawCompleteSegmentWithThumbnailSequence(Graphics2D g2d, TrackSegment segment, Track track, int trackY, int trackHeight) {
        int x = timeToPixel(segment.getStartTime());
        int width = Math.max(4, (int) (segment.getDuration() * pixelsPerSecond));
        int y = trackY + SEGMENT_PADDING;
        int height = trackHeight - (2 * SEGMENT_PADDING);

        // Skip if outside visible area
        if (x + width < TRACK_LABEL_WIDTH || x > getWidth()) return;

        // Clip to track area
        if (x < TRACK_LABEL_WIDTH) {
            width -= (TRACK_LABEL_WIDTH - x);
            x = TRACK_LABEL_WIDTH;
        }

        // Get segment color
        Color segmentColor = SEGMENT_COLORS.getOrDefault(track.getType(), UIConstants.ACCENT_PRIMARY);
        if (isSegmentSelected(segment)) {
            segmentColor = UIConstants.ACCENT_WARNING;
        }

        // Draw segment background
        RoundRectangle2D.Float shape = new RoundRectangle2D.Float(x, y, width, height,
                UIConstants.RADIUS_SM, UIConstants.RADIUS_SM);

        GradientPaint segmentGradient = new GradientPaint(
                x, y, UIConstants.brighten(segmentColor, 0.1f),
                x, y + height, UIConstants.darken(segmentColor, 0.1f)
        );
        g2d.setPaint(segmentGradient);
        g2d.fill(shape);

        // Draw content based on track type
        if (track.getType() == Track.TrackType.VIDEO && width > 60) {
            drawVideoThumbnailSequence(g2d, segment, x, y, width, height);
        } else if (track.getType() == Track.TrackType.AUDIO && width > 20) {
            drawAudioWaveformInSegment(g2d, segment, x, y, width, height);
        }

        // Draw segment border
        g2d.setColor(UIConstants.darken(segmentColor, 0.2f));
        g2d.setStroke(new BasicStroke(1f));
        g2d.draw(shape);

        // Draw segment info overlay
        if (width > 80) {
            drawSegmentInfoOverlay(g2d, segment, x, y, width, height);
        }

        // Draw resize handles for selected segments
        if (isSegmentSelected(segment) && width > 30) {
            drawResizeHandles(g2d, x, y, width, height);
        }
    }

    // ============ IMPROVED THUMBNAIL GENERATION ============

    /**
     * Draw video thumbnail sequence within segment with improved generation
     */
    private void drawVideoThumbnailSequence(Graphics2D g2d, TrackSegment segment, int x, int y, int width, int height) {
        try {
            // Get or generate thumbnails for this segment
            BufferedImage[] thumbnails = getOrCreateImprovedThumbnailSequence(segment, width);

            if (thumbnails.length > 0) {
                int thumbnailWidth = Math.min(VideoThumbnailService.THUMBNAIL_WIDTH, width / thumbnails.length);
                int thumbnailHeight = Math.min(VideoThumbnailService.THUMBNAIL_HEIGHT, height - 4);

                // Draw thumbnails in sequence
                for (int i = 0; i < thumbnails.length; i++) {
                    BufferedImage thumbnail = thumbnails[i];
                    if (thumbnail != null) {
                        int thumbX = x + (i * thumbnailWidth);
                        int thumbY = y + 2;

                        // Clip thumbnail to segment boundaries
                        Shape oldClip = g2d.getClip();
                        g2d.setClip(x, y, width, height);

                        // Draw thumbnail with smooth scaling
                        g2d.drawImage(thumbnail, thumbX, thumbY, thumbnailWidth, thumbnailHeight, null);

                        // Draw subtle separator between thumbnails
                        if (i < thumbnails.length - 1) {
                            g2d.setColor(UIConstants.withAlpha(Color.BLACK, 30));
                            g2d.drawLine(thumbX + thumbnailWidth - 1, thumbY, thumbX + thumbnailWidth - 1, thumbY + thumbnailHeight);
                        }

                        g2d.setClip(oldClip);
                    }
                }

                // Draw film strip effect
                drawFilmStripEffect(g2d, x, y, width, height);
            } else {
                // Fallback: draw placeholder pattern
                drawThumbnailPlaceholder(g2d, x, y, width, height);
            }
        } catch (Exception e) {
            logger.warning("Error drawing thumbnail sequence: " + e.getMessage());
            drawThumbnailPlaceholder(g2d, x, y, width, height);
        }
    }

    /**
     * Improved thumbnail generation with better fallback handling
     */
    private BufferedImage[] getOrCreateImprovedThumbnailSequence(TrackSegment segment, int segmentWidth) {
        String cacheKey = segment.getId() + "_" + segmentWidth + "_" + zoomLevel;

        // Check cache first
        BufferedImage[] cached = thumbnailSequenceCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // Calculate number of thumbnails needed
        int numThumbnails = Math.max(1, Math.min(8, segmentWidth / 80));

        try {
            BufferedImage[] realThumbnails = generateRealThumbnails(segment, numThumbnails);
            if (realThumbnails != null) {
                thumbnailSequenceCache.put(cacheKey, realThumbnails);
                return realThumbnails;
            }
        } catch (Exception e) {
            logger.warning("Failed to generate real thumbnails: " + e.getMessage());
        }

        // Fallback to improved placeholder thumbnails
        BufferedImage[] placeholders = createImprovedVideoPlaceholders(segment, numThumbnails);
        thumbnailSequenceCache.put(cacheKey, placeholders);
        return placeholders;
    }

    /**
     * Generate real video thumbnails using FFmpeg
     */
    private BufferedImage[] generateRealThumbnails(TrackSegment segment, int count) {
        if (videoProcessingService == null) {
            return null;
        }

        try {
            BufferedImage[] thumbnails = new BufferedImage[count];
            File videoFile = segment.getOriginalFile();
            double segmentDuration = segment.getDuration();

            for (int i = 0; i < count; i++) {
                // Calculate time offset for this thumbnail
                double timeOffset = (double) i / Math.max(1, count - 1) * segmentDuration;
                double absoluteTime = segment.getSourceStartTime() + timeOffset;

                // Generate single thumbnail using FFmpeg
                BufferedImage thumbnail = extractSingleFrame(videoFile, absoluteTime);
                if (thumbnail != null) {
                    thumbnails[i] = resizeImage(thumbnail
                    );
                } else {
                    // Individual frame failed, use placeholder for this frame
                    thumbnails[i] = createSingleVideoPlaceholder(i + 1, segment, count);
                }
            }

            return thumbnails;
        } catch (Exception e) {
            logger.warning("Error generating real thumbnails: " + e.getMessage());
            return null;
        }
    }

    /**
     * Extract a single frame from video at specific time
     */
    private BufferedImage extractSingleFrame(File videoFile, double timeSeconds) {
        try {
            // Create temporary output file
            File tempThumbnail = File.createTempFile("thumb_", ".jpg");
            tempThumbnail.deleteOnExit();

            // Build FFmpeg command for frame extraction
            java.util.List<String> command = Arrays.asList(
                    "ffmpeg",
                    "-v", "quiet",
                    "-ss", String.valueOf(timeSeconds),
                    "-i", videoFile.getAbsolutePath(),
                    "-vframes", "1",
                    "-vf", "scale=80:45",
                    "-q:v", "2",
                    "-y",
                    tempThumbnail.getAbsolutePath()
            );

            // Execute FFmpeg command
            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0 && tempThumbnail.exists() && tempThumbnail.length() > 0) {
                BufferedImage image = ImageIO.read(tempThumbnail);
                tempThumbnail.delete();
                return image;
            }

        } catch (Exception e) {
            logger.fine("Failed to extract frame at " + timeSeconds + "s: " + e.getMessage());
        }

        return null;
    }

    /**
     * Resize image to target dimensions
     */
    private BufferedImage resizeImage(BufferedImage original) {
        BufferedImage resized = new BufferedImage(VideoThumbnailService.THUMBNAIL_WIDTH, VideoThumbnailService.THUMBNAIL_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(original, 0, 0, VideoThumbnailService.THUMBNAIL_WIDTH, VideoThumbnailService.THUMBNAIL_HEIGHT, null);
        g2d.dispose();
        return resized;
    }

    /**
     * Create improved placeholder thumbnails that look more like video frames
     */
    private BufferedImage[] createImprovedVideoPlaceholders(TrackSegment segment, int count) {
        BufferedImage[] placeholders = new BufferedImage[count];

        for (int i = 0; i < count; i++) {
            placeholders[i] = createVideoStylePlaceholder(segment, i, count);
        }

        return placeholders;
    }

    /**
     * Create a single video-style placeholder thumbnail
     */
    private BufferedImage createVideoStylePlaceholder(TrackSegment segment, int index, int total) {
        BufferedImage placeholder = new BufferedImage(
                VideoThumbnailService.THUMBNAIL_WIDTH,
                VideoThumbnailService.THUMBNAIL_HEIGHT,
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D g2d = placeholder.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Create a gradient based on position in sequence
        float progress = (float) index / Math.max(1, total - 1);
        Color baseColor = new Color(30 + (int) (progress * 40), 35 + (int) (progress * 50), 45 + (int) (progress * 60));
        Color endColor = UIConstants.darken(baseColor, 0.4f);

        GradientPaint gradient = new GradientPaint(
                0, 0, baseColor,
                placeholder.getWidth(), placeholder.getHeight(), endColor
        );
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, placeholder.getWidth(), placeholder.getHeight());

        // Add film strip effect
        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.fillRect(0, 0, placeholder.getWidth(), 3);
        g2d.fillRect(0, placeholder.getHeight() - 3, placeholder.getWidth(), 3);

        // Add perforations
        for (int x = 5; x < placeholder.getWidth(); x += 8) {
            g2d.fillRect(x, 0, 2, 3);
            g2d.fillRect(x, placeholder.getHeight() - 3, 2, 3);
        }

        // Add video icon
        g2d.setColor(new Color(255, 255, 255, 120));
        g2d.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        String icon = "🎬";
        FontMetrics fm = g2d.getFontMetrics();
        int iconX = (placeholder.getWidth() - fm.stringWidth(icon)) / 2;
        int iconY = (placeholder.getHeight() + fm.getAscent()) / 2 - 8;
        g2d.drawString(icon, iconX, iconY);

        // Add time indicator instead of just frame number
        double timeInSegment = (double) index / Math.max(1, total - 1) * segment.getDuration();
        String timeText = String.format("%.1fs", segment.getSourceStartTime() + timeInSegment);

        g2d.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 8));
        g2d.setColor(new Color(255, 255, 255, 180));
        FontMetrics timeFm = g2d.getFontMetrics();
        int timeX = (placeholder.getWidth() - timeFm.stringWidth(timeText)) / 2;
        int timeY = placeholder.getHeight() - 4;
        g2d.drawString(timeText, timeX, timeY);

        g2d.dispose();
        return placeholder;
    }

    /**
     * Create a single placeholder for when individual frame extraction fails
     */
    private BufferedImage createSingleVideoPlaceholder(int frameNumber, TrackSegment segment, int total) {
        BufferedImage placeholder = new BufferedImage(
                VideoThumbnailService.THUMBNAIL_WIDTH,
                VideoThumbnailService.THUMBNAIL_HEIGHT,
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D g2d = placeholder.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Dark background
        g2d.setColor(new Color(25, 25, 30));
        g2d.fillRect(0, 0, placeholder.getWidth(), placeholder.getHeight());

        // Border
        g2d.setColor(UIConstants.BORDER_DEFAULT);
        g2d.drawRect(0, 0, placeholder.getWidth() - 1, placeholder.getHeight() - 1);

        // Video icon
        g2d.setColor(new Color(255, 255, 255, 80));
        g2d.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        String icon = "🎬";
        FontMetrics fm = g2d.getFontMetrics();
        int x = (placeholder.getWidth() - fm.stringWidth(icon)) / 2;
        int y = (placeholder.getHeight() + fm.getAscent()) / 2 - 5;
        g2d.drawString(icon, x, y);

        // Time info instead of frame number
        double timeOffset = (double) (frameNumber - 1) / Math.max(1, total - 1) * segment.getDuration();
        String timeText = String.format("%.1fs", segment.getSourceStartTime() + timeOffset);

        g2d.setColor(UIConstants.TEXT_SECONDARY);
        g2d.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 7));
        FontMetrics timeFm = g2d.getFontMetrics();
        int timeX = (placeholder.getWidth() - timeFm.stringWidth(timeText)) / 2;
        g2d.drawString(timeText, timeX, placeholder.getHeight() - 3);

        g2d.dispose();
        return placeholder;
    }

    /**
     * Draw film strip effect over thumbnails
     */
    private void drawFilmStripEffect(Graphics2D g2d, int x, int y, int width, int height) {
        g2d.setColor(UIConstants.withAlpha(Color.BLACK, 80));

        // Top and bottom strips
        g2d.fillRect(x, y, width, 2);
        g2d.fillRect(x, y + height - 2, width, 2);

        // Perforations
        int perfSize = 3;
        int perfSpacing = 8;
        for (int px = x; px < x + width; px += perfSpacing) {
            // Top perforations
            g2d.fillRect(px, y - 1, perfSize, 4);
            // Bottom perforations
            g2d.fillRect(px, y + height - 3, perfSize, 4);
        }
    }

    /**
     * Draw placeholder when thumbnails aren't available
     */
    private void drawThumbnailPlaceholder(Graphics2D g2d, int x, int y, int width, int height) {
        // Gradient background
        GradientPaint gradient = new GradientPaint(
                x, y, UIConstants.withAlpha(UIConstants.ACCENT_PRIMARY, 40),
                x + width, y + height, UIConstants.withAlpha(UIConstants.ACCENT_PRIMARY, 80)
        );
        g2d.setPaint(gradient);
        g2d.fillRect(x + 2, y + 2, width - 4, height - 4);

        // Video icon
        g2d.setColor(UIConstants.withAlpha(Color.WHITE, 120));
        g2d.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, Math.min(24, height / 2)));
        String icon = "🎬";
        FontMetrics fm = g2d.getFontMetrics();
        int iconX = x + (width - fm.stringWidth(icon)) / 2;
        int iconY = y + (height + fm.getAscent()) / 2 - 2;
        g2d.drawString(icon, iconX, iconY);
    }

    /**
     * Enhanced segment info overlay with duration
     */
    private void drawSegmentInfoOverlay(Graphics2D g2d, TrackSegment segment, int x, int y, int width, int height) {
        // Semi-transparent overlay for text
        g2d.setColor(UIConstants.withAlpha(Color.BLACK, 120));
        g2d.fillRect(x, y + height - 20, width, 20);

        // Segment name
        g2d.setColor(Color.WHITE);
        g2d.setFont(UIUtils.createFont(10, Font.BOLD));
        String name = segment.getName();
        if (name.length() > 20) {
            name = name.substring(0, 17) + "...";
        }
        g2d.drawString(name, x + 4, y + height - 8);

        // Duration info if space allows
        if (width > 120) {
            g2d.setFont(UIUtils.createFont(9, Font.PLAIN));
            String duration = TimeUtils.formatTime(segment.getDuration());
            FontMetrics fm = g2d.getFontMetrics();
            int durationWidth = fm.stringWidth(duration);
            g2d.drawString(duration, x + width - durationWidth - 4, y + height - 8);
        }
    }

    private void drawAudioWaveformInSegment(Graphics2D g2d, TrackSegment segment, int x, int y, int width, int height) {
        g2d.setColor(UIConstants.withAlpha(UIConstants.TEXT_PRIMARY, 100));
        Random random = new Random(segment.getId().hashCode());

        int samples = width / 2;
        int centerY = y + height / 2;
        int maxAmplitude = height / 4;

        for (int i = 0; i < samples; i++) {
            int sampleX = x + i * 2;
            int amplitude = (int) (random.nextFloat() * maxAmplitude * segment.getVolume());
            g2d.drawLine(sampleX, centerY - amplitude, sampleX, centerY + amplitude);
        }
    }

    private boolean isSegmentSelected(TrackSegment segment) {
        for (TrackManager manager : trackManagers) {
            if (manager.isSelected(segment)) {
                return true;
            }
        }
        return false;
    }

    private void drawResizeHandles(Graphics2D g2d, int x, int y, int width, int height) {
        int handleY = y + height / 2 - HANDLE_SIZE / 2;

        g2d.setColor(UIConstants.ACCENT_PRIMARY);
        g2d.fillRect(x - HANDLE_SIZE / 2, handleY, HANDLE_SIZE, HANDLE_SIZE);
        g2d.fillRect(x + width - HANDLE_SIZE / 2, handleY, HANDLE_SIZE, HANDLE_SIZE);
    }

    private void drawEnhancedSmoothCursor(Graphics2D g2d) {
        // Use current position for immediate responsiveness during playback
        boolean playing = isCurrentlyPlaying();
        double renderPosition = playing ? targetCursorPosition : currentCursorPosition;
        int x = timeToPixel(renderPosition);

        if (x < TRACK_LABEL_WIDTH || x > getWidth()) return;

        float alpha = isDraggingCursor ? 1.0f : (float) (Math.sin(cursorPulse) * 0.3 + 0.7);

        if (isDraggingCursor) {
            g2d.setColor(UIConstants.withAlpha(UIConstants.TIMELINE_CURSOR, (int) (200 * alpha)));
            g2d.setStroke(new BasicStroke(16f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawLine(x, 0, x, getHeight());
        }

        // Glow effect - more prominent during playback
        int glowAlpha = playing ? 120 : 80;
        g2d.setColor(UIConstants.withAlpha(UIConstants.TIMELINE_CURSOR, (int) (glowAlpha * alpha)));
        g2d.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine(x, 0, x, getHeight());

        // Main cursor line - slightly thicker during playback
        int lineWidth = playing ? CURSOR_WIDTH + 1 : CURSOR_WIDTH;
        g2d.setColor(UIConstants.TIMELINE_CURSOR);
        g2d.setStroke(new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine(x, 0, x, getHeight());

        // Enhanced cursor head
        if (isDraggingCursor || playing) {
            g2d.setColor(UIConstants.brighten(UIConstants.TIMELINE_CURSOR, 0.3f));
            g2d.fillPolygon(
                    new int[]{x - 12, x + 12, x},
                    new int[]{2, 2, 18},
                    3
            );

            // Time display when dragging or playing
            String timeText = TimeUtils.formatTime(renderPosition);
            g2d.setColor(Color.WHITE);
            g2d.setFont(UIUtils.createFont(11, Font.BOLD));
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(timeText);

            g2d.setColor(new Color(0, 0, 0, 220));
            g2d.fillRoundRect(x - textWidth / 2 - 8, 25, textWidth + 16, 20, 8, 8);

            g2d.setColor(UIConstants.TIMELINE_CURSOR);
            g2d.drawRoundRect(x - textWidth / 2 - 8, 25, textWidth + 16, 20, 8, 8);

            g2d.setColor(Color.WHITE);
            g2d.drawString(timeText, x - textWidth / 2, 39);
        } else {
            g2d.setColor(UIConstants.TIMELINE_CURSOR);
            g2d.fillPolygon(
                    new int[]{x - 8, x + 8, x},
                    new int[]{2, 2, 14},
                    3
            );
        }

        g2d.setStroke(new BasicStroke(1f));
    }


    private void drawSelectionAndHover(Graphics2D g2d) {
        if (selectionRect != null) {
            drawSelectionRect(g2d);
        }

    }

    private void drawSelectionRect(Graphics2D g2d) {
        g2d.setColor(UIConstants.TIMELINE_SELECTION);
        g2d.fillRect(selectionRect.x, selectionRect.y, selectionRect.width, selectionRect.height);

        g2d.setColor(UIConstants.ACCENT_PRIMARY);
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawRect(selectionRect.x, selectionRect.y, selectionRect.width, selectionRect.height);
        g2d.setStroke(new BasicStroke(1f));
    }

    // ============ UTILITY METHODS ============

    private int calculatePanelHeight() {
        int totalHeight = TIMELINE_HEIGHT + TRACK_PADDING;

        for (TrackManager trackManager : trackManagers) {
            for (Track track : trackManager.getTracks()) {
                totalHeight += getTrackHeight(track) + TRACK_PADDING;
            }
        }

        return totalHeight + 40;
    }

    private void updatePanelSize() {
        int newHeight = calculatePanelHeight();
        if (getPreferredSize().height != newHeight) {
            setPreferredSize(new Dimension(getPreferredSize().width, newHeight));
            revalidate();
        }
    }

    private int timeToPixel(double time) {
        return TRACK_LABEL_WIDTH + (int) ((time - viewStartTime) * pixelsPerSecond);
    }

    public double pixelToTime(int pixel) {
        return viewStartTime + (pixel - TRACK_LABEL_WIDTH) / pixelsPerSecond;
    }

    private Track getTrackAtY(int y) {
        if (y < TIMELINE_HEIGHT) return null;

        int currentY = TIMELINE_HEIGHT + TRACK_PADDING;

        for (TrackManager trackManager : trackManagers) {
            for (Track track : trackManager.getTracks()) {
                int trackHeight = getTrackHeight(track);
                if (y >= currentY && y < currentY + trackHeight) {
                    return track;
                }
                currentY += trackHeight + TRACK_PADDING;
            }
        }

        return null;
    }

    private TrackSegment getSegmentAt(int x, int y) {
        if (x < TRACK_LABEL_WIDTH) return null;

        Track track = getTrackAtY(y);
        if (track == null) return null;

        double time = pixelToTime(x);
        return track.getSegmentsAtTime(time).stream()
                .findFirst()
                .orElse(null);
    }

    private Track findTrackContainingSegment(TrackSegment segment) {
        for (TrackManager trackManager : trackManagers) {
            Track track = trackManager.findTrackContainingSegment(segment);
            if (track != null) {
                return track;
            }
        }
        return null;
    }

    private TrackManager findTrackManagerContainingSegment(TrackSegment segment) {
        for (TrackManager trackManager : trackManagers) {
            if (trackManager.findTrackContainingSegment(segment) != null) {
                return trackManager;
            }
        }
        return null;
    }

    private ResizeHandle getResizeHandleAt(int x, int y) {
        TrackSegment segment = getSegmentAt(x, y);
        if (segment == null || !isSegmentSelected(segment)) {
            return ResizeHandle.NONE;
        }

        int segmentX = timeToPixel(segment.getStartTime());
        int segmentEndX = timeToPixel(segment.getEndTime());

        if (Math.abs(x - segmentX) <= HANDLE_SIZE) {
            return ResizeHandle.LEFT;
        }
        if (Math.abs(x - segmentEndX) <= HANDLE_SIZE) {
            return ResizeHandle.RIGHT;
        }

        return ResizeHandle.NONE;
    }

    private double calculateTimeStep() {
        double[] steps = {0.1, 0.5, 1, 5, 10, 30, 60, 300, 600};
        double targetPixelsPerStep = 100;

        for (double step : steps) {
            double pixels = step * pixelsPerSecond;
            if (pixels >= targetPixelsPerStep) {
                return step;
            }
        }

        return steps[steps.length - 1];
    }

    // ============ ZOOM AND VIEW METHODS ============

    public void setZoomLevel(double newZoomLevel) {
        zoomLevel = Math.max(0.1, Math.min(newZoomLevel, 10.0));
        pixelsPerSecond = 60.0 * zoomLevel;

        double centerTime = viewStartTime + (getWidth() - TRACK_LABEL_WIDTH) / (2.0 * pixelsPerSecond);
        viewStartTime = centerTime - (getWidth() - TRACK_LABEL_WIDTH) / (2.0 * pixelsPerSecond);
        viewStartTime = Math.max(0, viewStartTime);

        clearThumbnailSequenceCache();
        notifyViewChanged();
        repaint();
    }

    public double getZoomLevel() {
        return zoomLevel;
    }

    private void notifyViewChanged() {
        double viewEndTime = viewStartTime + (getWidth() - TRACK_LABEL_WIDTH) / pixelsPerSecond;
        editorListener.onViewChanged(zoomLevel, viewStartTime, viewEndTime);
    }

    // ============ ACTION METHODS ============

    private void selectAllSegments() {
        for (TrackManager trackManager : trackManagers) {
            List<TrackSegment> allSegments = trackManager.getTracks().stream()
                    .flatMap(track -> track.getSegments().stream())
                    .collect(Collectors.toList());
            trackManager.selectSegments(allSegments);
        }
        repaint();
    }

    private void deleteSelectedSegments() {
        for (TrackManager trackManager : trackManagers) {
            TrackManager.SegmentOperation result = trackManager.deleteSelectedSegments();
            if (!result.success) {
                logger.warning("Failed to delete segments: " + result.message);
            }
        }
        repaint();
    }

    private void copySelectedSegments() {
        for (TrackManager trackManager : trackManagers) {
            TrackManager.SegmentOperation result = trackManager.copySelectedSegments();
            if (!result.success) {
                logger.warning("Failed to copy segments: " + result.message);
            }
        }
    }

    private void splitAtCursor() {
        double cursorTime = getCursorPosition();
        for (TrackManager trackManager : trackManagers) {
            TrackManager.SegmentOperation result = trackManager.cutAllTracksAtTime(cursorTime);
            if (!result.success) {
                logger.warning("Failed to split at cursor: " + result.message);
            }
        }
        repaint();
    }

    // ============ CURSOR METHODS ============

    private double getCursorPosition() {
        return currentCursorPosition;
    }

    public void setCursorPosition(double position) {
        // Update both target and current positions for immediate response
        this.targetCursorPosition = Math.max(0, position);
        this.currentCursorPosition = position; // FIXED: Set immediately for responsive feel

        // Force immediate cursor area repaint for smooth movement
        repaintCursorArea();

        logger.fine("TrackPanel cursor position set to: " + position);
    }


    private boolean isCurrentlyPlaying() {
        if (editorListener instanceof com.videoeditor.ui.controllers.MainController) {
            return editorListener.isCurrentlyPlaying();
        }
        return false;
    }


    private boolean isSnapToGrid() {
        return !trackManagers.isEmpty() && trackManagers.get(0).isSnapToGrid();
    }

    private double getGridSize() {
        return trackManagers.isEmpty() ? 1.0 : trackManagers.get(0).getGridSize();
    }

    // ============ TRACK LISTENER IMPLEMENTATION ============

    @Override
    public void onTrackUpdated() {
        updatePanelSize();
        repaint();
    }

    @Override
    public void onTrackAdded(Track track) {
        updatePanelSize();
        repaint();
    }

    @Override
    public void onTrackRemoved(Track track) {
        updatePanelSize();
        repaint();
    }

    @Override
    public void onSegmentAdded(Track track, TrackSegment segment) {
        clearThumbnailSequenceCache();
        repaint();
    }

    @Override
    public void onSegmentRemoved(Track track, TrackSegment segment) {
        clearThumbnailSequenceCache();
        repaint();
    }

    @Override
    public void onCursorPositionChanged(double oldPosition, double newPosition) {
        setCursorPosition(newPosition);
    }

    @Override
    public void onSelectionChanged(Set<TrackSegment> selectedSegments) {
        repaint();
    }

    // ============ CACHE MANAGEMENT ============

    public void clearThumbnailSequenceCache() {
        thumbnailSequenceCache.clear();
        logger.info("Thumbnail cache cleared");
    }

    // ============ ENHANCED MOUSE HANDLER ============

    private class EnhancedMouseHandler extends MouseAdapter implements MouseWheelListener {

        @Override
        public void mousePressed(MouseEvent e) {
            requestFocusInWindow();

            if (e.getY() <= TIMELINE_HEIGHT) {
                handleTimelineInteraction(e);
                return;
            }

            handleTrackInteraction(e);
        }

        private void handleTimelineInteraction(MouseEvent e) {
            if (e.getX() < TRACK_LABEL_WIDTH) return;

            int cursorX = timeToPixel(currentCursorPosition);

            if (Math.abs(e.getX() - cursorX) <= 15) {
                mode = InteractionMode.DRAGGING_CURSOR;
                isDraggingCursor = true;
                isUserDragging = true;
                setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));

                double newTime = pixelToTime(e.getX());
                currentCursorPosition = newTime;
                targetCursorPosition = newTime;

                repaintCursorArea();
            } else {
                double time = pixelToTime(e.getX());
                targetCursorPosition = time;
                currentCursorPosition = time;

                if (editorListener != null) {
                    editorListener.onCursorMoved(time);
                }
                repaintCursorArea();
            }
        }

        private void handleTrackInteraction(MouseEvent e) {
            activeHandle = getResizeHandleAt(e.getX(), e.getY());
            if (activeHandle != ResizeHandle.NONE) {
                mode = InteractionMode.RESIZING_SEGMENT;
                activeSegment = getSegmentAt(e.getX(), e.getY());
                activeTrack = findTrackContainingSegment(activeSegment);
                dragStart = e.getPoint();
                dragStartTime = activeSegment.getStartTime();
                return;
            }

            TrackSegment segment = getSegmentAt(e.getX(), e.getY());
            if (segment != null) {
                TrackManager trackManager = findTrackManagerContainingSegment(segment);
                if (trackManager != null) {
                    if (!e.isShiftDown() && !trackManager.isSelected(segment)) {
                        trackManager.clearSelection();
                    }
                    trackManager.selectSegment(segment);

                    mode = InteractionMode.DRAGGING_SEGMENT;
                    activeSegment = segment;
                    activeTrack = findTrackContainingSegment(segment);
                    dragStart = e.getPoint();
                    dragStartTime = segment.getStartTime();
                }
                return;
            }

            if (e.getButton() == MouseEvent.BUTTON1) {
                mode = InteractionMode.SELECTING;
                dragStart = e.getPoint();
                selectionRect = new Rectangle(dragStart);

                if (!e.isShiftDown()) {
                    for (TrackManager trackManager : trackManagers) {
                        trackManager.clearSelection();
                    }
                }
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            switch (mode) {
                case DRAGGING_CURSOR:
                    handleCursorDrag(e);
                    break;
                case DRAGGING_SEGMENT:
                    handleSegmentDrag(e);
                    break;
                case RESIZING_SEGMENT:
                    handleSegmentResize(e);
                    break;
                case SELECTING:
                    handleSelection();
                    break;
            }
        }

        private void handleSelection() {
        }

        private void handleCursorDrag(MouseEvent e) {
            if (e.getX() >= TRACK_LABEL_WIDTH) {
                double time = Math.max(0, pixelToTime(e.getX()));

                currentCursorPosition = time;
                targetCursorPosition = time;

                if (editorListener != null) {
                    editorListener.onCursorMoved(time);
                }

                repaintCursorArea();
            }
        }

        private void handleSegmentDrag(MouseEvent e) {
            if (activeSegment == null || activeTrack == null || activeTrack.isLocked()) return;

            double deltaTime = pixelToTime(e.getX()) - pixelToTime(dragStart.x);
            double newTime = Math.max(0, dragStartTime + deltaTime);

            if (isSnapToGrid()) {
                double gridSize = getGridSize();
                newTime = Math.round(newTime / gridSize) * gridSize;
            }

            activeSegment.setStartTime(newTime);
            repaint();
        }

        private void handleSegmentResize(MouseEvent e) {
            if (activeSegment == null || activeTrack == null || activeTrack.isLocked()) return;

            double newTime = pixelToTime(e.getX());

            if (isSnapToGrid()) {
                double gridSize = getGridSize();
                newTime = Math.round(newTime / gridSize) * gridSize;
            }

            if (activeHandle == ResizeHandle.LEFT) {
                // Resize from left side - adjust start time and duration
                double endTime = activeSegment.getEndTime();
                double newDuration = endTime - newTime;

                if (newDuration > 0.1 && newTime >= 0) {
                    // Make sure we don't go beyond the source video boundaries
                    double maxStartTime = activeSegment.getSourceStartTime() + activeSegment.getSourceDuration() - 0.1;
                    if (newTime <= maxStartTime) {
                        activeSegment.setStartTime(newTime);
                        activeSegment.setDuration(newDuration);

                        // Update source start time proportionally
                        double sourceOffset = newTime - activeSegment.getStartTime();
                        activeSegment.setSourceStartTime(Math.max(0, activeSegment.getSourceStartTime() + sourceOffset));
                    }
                }
            } else if (activeHandle == ResizeHandle.RIGHT) {
                // Resize from right side - adjust duration only
                double newDuration = newTime - activeSegment.getStartTime();

                if (newDuration > 0.1) {
                    // Make sure we don't exceed the source video duration
                    double maxDuration = activeSegment.getSourceDuration() -
                            (activeSegment.getStartTime() - activeSegment.getSourceStartTime());
                    newDuration = Math.min(newDuration, maxDuration);

                    if (newDuration > 0.1) {
                        activeSegment.setDuration(newDuration);
                    }
                }
            }

            // Clear thumbnail cache since segment timing changed
            clearThumbnailSequenceCache();
            repaint();
        }
    }
}