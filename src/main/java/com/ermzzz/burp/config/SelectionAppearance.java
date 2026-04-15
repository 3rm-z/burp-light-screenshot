package com.ermzzz.burp.config;

import java.awt.Color;
import java.util.List;

/**
 * Appearance settings for screenshot selection border.
 * <p>
 * JVM option: {@code -Dburp.lightss.selection.color=...} (see {@link #parseColor(String)}).
 */
public final class SelectionAppearance {

    public static final String PROPERTY_SELECTION_COLOR = "burp.lightss.selection.color";

    private SelectionAppearance() {
    }

    private static final Color DEFAULT_COLOR = new Color(255, 0, 0, 200);
    private static volatile Color currentColor = loadFromPropertyOrDefault();

    public static final List<Preset> PRESETS = List.of(
            new Preset("Cyan", new Color(0, 229, 255, 220)),
            new Preset("Magenta", new Color(255, 0, 191, 220)),
            new Preset("Lime", new Color(57, 255, 20, 220)),
            new Preset("Orange", new Color(255, 122, 0, 220)),
            new Preset("Red", new Color(255, 45, 45, 220))
    );

    /** Current runtime overlay border color. */
    public static Color selectionBorderColor() {
        return currentColor;
    }

    public static void setSelectionBorderColor(Color color) {
        if (color != null) {
            currentColor = color;
        }
    }

    public static void resetToPropertyOrDefault() {
        currentColor = loadFromPropertyOrDefault();
    }

    /** Overlay border color from JVM property or default value. */
    private static Color loadFromPropertyOrDefault() {
        String raw = System.getProperty(PROPERTY_SELECTION_COLOR);
        if (raw == null || raw.isBlank()) {
            return DEFAULT_COLOR;
        }
        try {
            return parseColor(raw.trim());
        } catch (IllegalArgumentException e) {
            return DEFAULT_COLOR;
        }
    }

    /**
     * Accepted formats:
     * <ul>
     *   <li>{@code #RRGGBB} or {@code #RGB}</li>
     *   <li>{@code #RRGGBBAA} (alpha in hex)</li>
     *   <li>{@code r,g,b} or {@code r,g,b,a} with 0-255 integer values</li>
     * </ul>
     */
    public static Color parseColor(String s) {
        if (s.startsWith("#")) {
            String hex = s.substring(1);
            if (hex.length() == 3) {
                int r = expandNibble(hex.charAt(0));
                int g = expandNibble(hex.charAt(1));
                int b = expandNibble(hex.charAt(2));
                return new Color(r, g, b, 255);
            }
            if (hex.length() == 6) {
                return new Color(
                        Integer.parseInt(hex.substring(0, 2), 16),
                        Integer.parseInt(hex.substring(2, 4), 16),
                        Integer.parseInt(hex.substring(4, 6), 16),
                        255
                );
            }
            if (hex.length() == 8) {
                return new Color(
                        Integer.parseInt(hex.substring(0, 2), 16),
                        Integer.parseInt(hex.substring(2, 4), 16),
                        Integer.parseInt(hex.substring(4, 6), 16),
                        Integer.parseInt(hex.substring(6, 8), 16)
                );
            }
            throw new IllegalArgumentException("Hex color length: " + hex.length());
        }
        String[] parts = s.split(",");
        if (parts.length < 3) {
            throw new IllegalArgumentException("Expected r,g,b[,a]");
        }
        int r = Integer.parseInt(parts[0].trim());
        int g = Integer.parseInt(parts[1].trim());
        int b = Integer.parseInt(parts[2].trim());
        int a = parts.length >= 4 ? Integer.parseInt(parts[3].trim()) : 255;
        return new Color(clamp255(r), clamp255(g), clamp255(b), clamp255(a));
    }

    private static int expandNibble(char c) {
        int v = Character.digit(c, 16);
        if (v < 0) {
            throw new IllegalArgumentException("Bad hex: " + c);
        }
        return (v << 4) | v;
    }

    private static int clamp255(int v) {
        return Math.max(0, Math.min(255, v));
    }

    public static final class Preset {
        private final String label;
        private final Color color;

        public Preset(String label, Color color) {
            this.label = label;
            this.color = color;
        }

        public String label() {
            return label;
        }

        public Color color() {
            return color;
        }
    }
}
