package com.videoeditor.ui.utils;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.videoeditor.config.UIConstants;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Modern UI utilities with glassmorphism, animations, and current design trends
 */
public class UIUtils {

    // ============ CORE SETUP ============

    public static void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel(new FlatDarculaLaf());

            // Modern UI Manager settings
            UIManager.put("Button.arc", UIConstants.RADIUS_MD);
            UIManager.put("Component.arc", UIConstants.RADIUS_MD);
            UIManager.put("ProgressBar.arc", UIConstants.RADIUS_FULL);
            UIManager.put("TextComponent.arc", UIConstants.RADIUS_SM);
            UIManager.put("ScrollBar.width", 8);
            UIManager.put("ScrollBar.thumbArc", UIConstants.RADIUS_FULL);
            UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
            UIManager.put("ScrollBar.track", UIConstants.SURFACE_LOW);
            UIManager.put("ScrollBar.thumb", UIConstants.INTERACTIVE_DEFAULT);

            // Button styling
            UIManager.put("Button.background", UIConstants.ACCENT_PRIMARY);
            UIManager.put("Button.foreground", UIConstants.TEXT_PRIMARY);
            UIManager.put("Button.hoverBackground", UIConstants.ACCENT_PRIMARY_HOVER);
            UIManager.put("Button.pressedBackground", UIConstants.ACCENT_PRIMARY_PRESSED);

            // Tooltip styling
            UIManager.put("ToolTip.background", UIConstants.SURFACE_HIGH);
            UIManager.put("ToolTip.foreground", UIConstants.TEXT_PRIMARY);
            UIManager.put("ToolTip.border", createGlassBorder());

        } catch (Exception e) {
            java.util.logging.Logger.getLogger(UIUtils.class.getName())
                    .severe("Failed to set modern look and feel: " + e.getMessage());
        }
    }

    // ============ MODERN BUTTONS ============

    public static JButton createPrimaryButton(String text, String tooltip, ActionListener listener) {
        return createModernButton(text, tooltip, ButtonStyle.PRIMARY, listener);
    }

    public static JButton createSecondaryButton(String text, String tooltip, ActionListener listener) {
        return createModernButton(text, tooltip, ButtonStyle.SECONDARY, listener);
    }

    public static JButton createButton(String text, String tooltip, ButtonStyle style, ActionListener listener) {
        return createModernButton(text, tooltip, style, listener);
    }

    public enum ButtonStyle {
        PRIMARY, SECONDARY, GHOST, DANGER
    }

    public static JButton createModernButton(String text, String tooltip, ButtonStyle style, ActionListener listener) {
        JButton button = new JButton(text) {
            private boolean isHovered = false;
            private boolean isPressed = false;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                setupRenderingHints(g2d);

                Color bgColor;
                Color textColor = UIConstants.TEXT_PRIMARY;

                switch (style) {
                    case PRIMARY:
                        bgColor = isPressed ? UIConstants.ACCENT_PRIMARY_PRESSED :
                                isHovered ? UIConstants.ACCENT_PRIMARY_HOVER :
                                        UIConstants.ACCENT_PRIMARY;
                        break;
                    case SECONDARY:
                        bgColor = isPressed ? UIConstants.darken(UIConstants.SURFACE_HIGH, 0.1f) :
                                isHovered ? UIConstants.brighten(UIConstants.SURFACE_HIGH, 0.1f) :
                                        UIConstants.SURFACE_HIGH;
                        break;
                    case GHOST:
                        bgColor = isPressed ? UIConstants.withAlpha(UIConstants.ACCENT_PRIMARY, 40) :
                                isHovered ? UIConstants.withAlpha(UIConstants.ACCENT_PRIMARY, 20) :
                                        UIConstants.withAlpha(UIConstants.ACCENT_PRIMARY, 0);
                        textColor = UIConstants.ACCENT_PRIMARY;
                        break;
                    case DANGER:
                        bgColor = isPressed ? UIConstants.darken(UIConstants.ACCENT_ERROR, 0.1f) :
                                isHovered ? UIConstants.brighten(UIConstants.ACCENT_ERROR, 0.1f) :
                                        UIConstants.ACCENT_ERROR;
                        break;
                    default:
                        bgColor = UIConstants.ACCENT_PRIMARY;
                }

                // Draw button background with glassmorphism
                if (style == ButtonStyle.GHOST) {
                    g2d.setColor(UIConstants.GLASS_BACKGROUND);
                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), UIConstants.RADIUS_MD, UIConstants.RADIUS_MD);

                    g2d.setColor(UIConstants.GLASS_BORDER);
                    g2d.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, UIConstants.RADIUS_MD, UIConstants.RADIUS_MD);
                }

                g2d.setColor(bgColor);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), UIConstants.RADIUS_MD, UIConstants.RADIUS_MD);

                // Draw text
                g2d.setColor(textColor);
                FontMetrics fm = g2d.getFontMetrics();
                int textX = (getWidth() - fm.stringWidth(getText())) / 2;
                int textY = (getHeight() + fm.getAscent()) / 2 - 2;
                g2d.drawString(getText(), textX, textY);
            }

            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        isHovered = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        isHovered = false;
                        repaint();
                    }

                    @Override
                    public void mousePressed(MouseEvent e) {
                        isPressed = true;
                        repaint();
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        isPressed = false;
                        repaint();
                    }
                });
            }
        };

        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setForeground(UIConstants.TEXT_PRIMARY);
        button.setFont(createFont(UIConstants.FONT_SIZE_BASE, Font.BOLD));
        button.setToolTipText(tooltip);
        button.addActionListener(listener);
        button.setPreferredSize(new Dimension(140, UIConstants.BUTTON_HEIGHT_MD));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return button;
    }

    // ============ TYPOGRAPHY ============

    public static Font createFont(int size, int style) {
        return new Font(Font.SANS_SERIF, style, size);
    }

    public static JLabel createHeading(String text, int level) {
        JLabel label = new JLabel(text);

        int fontSize;
        switch (level) {
            case 1: fontSize = UIConstants.FONT_SIZE_3XL; break;
            case 2: fontSize = UIConstants.FONT_SIZE_2XL; break;
            case 3: fontSize = UIConstants.FONT_SIZE_XL; break;
            default: fontSize = UIConstants.FONT_SIZE_LG; break;
        }

        label.setFont(createFont(fontSize, Font.BOLD));
        label.setForeground(UIConstants.TEXT_PRIMARY);
        return label;
    }

    // ============ UTILITY METHODS ============

    public static void setupRenderingHints(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }

    public static Border createGlassBorder() {
        return BorderFactory.createLineBorder(UIConstants.GLASS_BORDER);
    }

    // ============ COMPATIBILITY METHODS ============

    public static Border createModernTitledBorder(String title) {
        return BorderFactory.createTitledBorder(
                createGlassBorder(),
                title,
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                createFont(UIConstants.FONT_SIZE_SM, Font.BOLD),
                UIConstants.TEXT_PRIMARY
        );
    }
    // ============ LEGACY BUTTON METHODS (for backward compatibility) ============

    /**
     * Legacy method - creates a modern button with Color parameter (for backward compatibility)
     * @deprecated Use createPrimaryButton, createSecondaryButton, or createButton with ButtonStyle instead
     */
    @Deprecated
    public static JButton createModernButton(String text, String toolTip, Color bgColor, ActionListener listener) {
        // Convert old color-based approach to new style-based approach
        if (bgColor.equals(UIConstants.ACCENT_PRIMARY)) {
            return createPrimaryButton(text, toolTip, listener);
        } else if (bgColor.equals(UIConstants.ACCENT_ERROR)) {
            return createButton(text, toolTip, ButtonStyle.DANGER, listener);
        } else {
            return createSecondaryButton(text, toolTip, listener);
        }
    }

    public static void setupShortcut(InputMap inputMap, ActionMap actionMap, String keyStroke, String actionName, Runnable action) {
        inputMap.put(KeyStroke.getKeyStroke(keyStroke), actionName);
        actionMap.put(actionName, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                action.run();
            }
        });
    }

}