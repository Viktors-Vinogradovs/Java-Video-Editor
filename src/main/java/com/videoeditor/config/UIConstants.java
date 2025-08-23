package com.videoeditor.config;

import java.awt.*;

public final class UIConstants {
    private UIConstants() {} // Utility class


    public static final Dimension TRACK_PANEL_SIZE = new Dimension(1000, 600);
    public static final Dimension CONTROL_PANEL_SIZE = new Dimension(1000, 60);

    // Colors - Dark Theme
    public static final Color BACKGROUND_DARK = new Color(34, 34, 34);
    public static final Color BACKGROUND_LIGHT = new Color(50, 50, 50);
    public static final Color BACKGROUND_MEDIUM = new Color(42, 42, 42);
    public static final Color BACKGROUND_PRIMARY = new Color(28, 28, 30);
    public static final Color BACKGROUND_SECONDARY = new Color(36, 36, 38);
    public static final Color SURFACE_ELEVATED = new Color(60, 60, 65);

    // Modern UI Surface Colors
    public static final Color SURFACE_LOW = new Color(37, 37, 37);
    public static final Color SURFACE_MEDIUM = new Color(45, 45, 48);
    public static final Color SURFACE_HIGH = new Color(60, 60, 65);

    // Glassmorphism Colors
    public static final Color GLASS_BACKGROUND = new Color(255, 255, 255, 20);
    public static final Color GLASS_BORDER = new Color(255, 255, 255, 40);

    // Accent Colors
    public static final Color ACCENT_PRIMARY = new Color(0, 122, 255);
    public static final Color ACCENT_SUCCESS = new Color(40, 167, 69);
    public static final Color ACCENT_INFO = new Color(23, 162, 184);
    public static final Color ACCENT_WARNING = new Color(255, 193, 7);
    public static final Color ACCENT_ERROR = new Color(220, 53, 69);

    // Modern Accent Variants
    public static final Color ACCENT_PRIMARY_HOVER = new Color(30, 144, 255);
    public static final Color ACCENT_PRIMARY_PRESSED = new Color(0, 100, 210);

    // Interactive Colors
    public static final Color INTERACTIVE_DEFAULT = ACCENT_PRIMARY;

    // Text Colors
    public static final Color TEXT_PRIMARY = Color.WHITE;
    public static final Color TEXT_SECONDARY = new Color(174, 174, 178);
    public static final Color TEXT_TERTIARY = new Color(99, 99, 102);

    // Border Colors
    public static final Color BORDER_DEFAULT = new Color(72, 72, 74);
    public static final Color BORDER_FOCUS = ACCENT_PRIMARY;

    // Timeline Colors
    public static final Color TIMELINE_CURSOR = new Color(255, 69, 58);
    public static final Color TIMELINE_SELECTION = new Color(0, 122, 255, 60);

    // Border Radius
    public static final int RADIUS_SM = 4;
    public static final int RADIUS_MD = 8;
    public static final int RADIUS_LG = 12;
    public static final int RADIUS_FULL = 9999;

    // Typography
    public static final int FONT_SIZE_XS = 10;
    public static final int FONT_SIZE_SM = 12;
    public static final int FONT_SIZE_BASE = 14;
    public static final int FONT_SIZE_LG = 16;
    public static final int FONT_SIZE_XL = 18;
    public static final int FONT_SIZE_2XL = 24;
    public static final int FONT_SIZE_3XL = 30;

    // Spacing
    public static final int SPACE_SM = 8;
    public static final int SPACE_MD = 12;
    // Component Sizes
    public static final int BUTTON_HEIGHT_MD = 40;


    // Utility methods for color manipulation
    public static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(),
                Math.max(0, Math.min(255, alpha)));
    }

    public static Color brighten(Color color, float factor) {
        int r = (int) Math.min(255, color.getRed() + (255 - color.getRed()) * factor);
        int g = (int) Math.min(255, color.getGreen() + (255 - color.getGreen()) * factor);
        int b = (int) Math.min(255, color.getBlue() + (255 - color.getBlue()) * factor);
        return new Color(r, g, b, color.getAlpha());
    }

    public static Color darken(Color color, float factor) {
        int r = (int) Math.max(0, color.getRed() * (1 - factor));
        int g = (int) Math.max(0, color.getGreen() * (1 - factor));
        int b = (int) Math.max(0, color.getBlue() * (1 - factor));
        return new Color(r, g, b, color.getAlpha());
    }
}