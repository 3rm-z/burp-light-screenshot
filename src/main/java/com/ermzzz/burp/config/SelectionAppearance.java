package com.ermzzz.burp.config;

import java.awt.Color;

/**
 * Aspetto del rettangolo di selezione screenshot.
 * <p>
 * JVM: {@code -Dburp.lightss.selection.color=...} (vedi {@link #parseColor(String)}).
 */
public final class SelectionAppearance {

    public static final String PROPERTY_SELECTION_COLOR = "burp.lightss.selection.color";

    private SelectionAppearance() {
    }

    /** Colore bordo overlay (default rosso semi-trasparente). */
    public static Color selectionBorderColor() {
        String raw = System.getProperty(PROPERTY_SELECTION_COLOR);
        if (raw == null || raw.isBlank()) {
            return new Color(255, 0, 0, 200);
        }
        try {
            return parseColor(raw.trim());
        } catch (IllegalArgumentException e) {
            return new Color(255, 0, 0, 200);
        }
    }

    /**
     * Formati accettati:
     * <ul>
     *   <li>{@code #RRGGBB} o {@code #RGB}</li>
     *   <li>{@code #RRGGBBAA} (alpha in hex)</li>
     *   <li>{@code r,g,b} o {@code r,g,b,a} con interi 0–255</li>
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
}
