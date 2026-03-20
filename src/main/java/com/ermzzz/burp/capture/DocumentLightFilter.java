package com.ermzzz.burp.capture;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Trasforma uno screenshot catturato con Burp in tema scuro in uno stile più adatto ai report
 * (sfondo chiaro, contenuto leggibile), senza modificare il Look&amp;Feel di Burp.
 * <p>
 * Non replica pixel-perfect il tema light ufficiale di Burp (impossibile senza API interne
 * o senza rischiare di bloccare l'EDT con {@code updateComponentTreeUI} su tutta l'app).
 */
public final class DocumentLightFilter {

    /** Colore carta / sfondo target (quasi bianco) */
    private static final int BG_R = 250;
    private static final int BG_G = 250;
    private static final int BG_B = 252;

    private DocumentLightFilter() {
    }

    /**
     * Applica euristica “documento chiaro” pixel per pixel.
     */
    public static BufferedImage toDocumentStyle(BufferedImage src) {
        if (src == null) {
            return null;
        }
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = src.getRGB(x, y);
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                float lum = luminance(r, g, b);

                int nr;
                int ng;
                int nb;

                if (lum < 0.42f) {
                    // Probabile sfondo scuro: avvicina a carta chiara mantenendo un po' di tinta
                    float t = lum / 0.42f;
                    float mix = 0.82f * (1f - t);
                    nr = blend(r, BG_R, mix);
                    ng = blend(g, BG_G, mix);
                    nb = blend(b, BG_B, mix);
                } else {
                    // Probabile testo / highlight: scurisci leggermente per contrasto su sfondo chiaro
                    float mix = 0.18f;
                    nr = blend(r, 28, mix);
                    ng = blend(g, 28, mix);
                    nb = blend(b, 32, mix);
                }

                int rgb = (nr << 16) | (ng << 8) | nb;
                out.setRGB(x, y, rgb);
            }
        }
        return out;
    }

    private static float luminance(int r, int g, int b) {
        return (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f;
    }

    private static int blend(int from, int to, float mix) {
        if (mix < 0f) {
            mix = 0f;
        }
        if (mix > 1f) {
            mix = 1f;
        }
        int v = Math.round(from + (to - from) * mix);
        if (v < 0) {
            return 0;
        }
        if (v > 255) {
            return 255;
        }
        return v;
    }
}
