package com.ermzzz.burp.capture;

import java.awt.image.BufferedImage;

/**
 * Light sharpening on mid tones (text) to reduce grainy look after color filtering.
 */
public final class LightOutputSharpen {

    private static final float STRENGTH = 0.42f;

    private LightOutputSharpen() {
    }

    public static void apply(BufferedImage img) {
        if (img == null) {
            return;
        }
        int w = img.getWidth();
        int h = img.getHeight();
        if (w < 3 || h < 3) {
            return;
        }
        int[] px = new int[w * h];
        img.getRGB(0, 0, w, h, px, 0, w);
        int[] out = px.clone();

        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                int i = y * w + x;
                int c = px[i];
                int r = (c >> 16) & 0xFF;
                int g = (c >> 8) & 0xFF;
                int b = c & 0xFF;
                int lum = (2126 * r + 7152 * g + 722 * b) / 10000;
                if (lum < 38 || lum > 242) {
                    continue;
                }
                int ru = (px[i - w] >> 16) & 0xFF;
                int rd = (px[i + w] >> 16) & 0xFF;
                int rl = (px[i - 1] >> 16) & 0xFF;
                int rr = (px[i + 1] >> 16) & 0xFF;
                int gu = (px[i - w] >> 8) & 0xFF;
                int gd = (px[i + w] >> 8) & 0xFF;
                int gl = (px[i - 1] >> 8) & 0xFF;
                int gr = (px[i + 1] >> 8) & 0xFF;
                int bu = (px[i - w]) & 0xFF;
                int bd = (px[i + w]) & 0xFF;
                int bl = (px[i - 1]) & 0xFF;
                int br = (px[i + 1]) & 0xFF;

                int rAvg = (ru + rd + rl + rr) / 4;
                int gAvg = (gu + gd + gl + gr) / 4;
                int bAvg = (bu + bd + bl + br) / 4;

                int nr = clamp255(Math.round(r + STRENGTH * (r - rAvg)));
                int ng = clamp255(Math.round(g + STRENGTH * (g - gAvg)));
                int nb = clamp255(Math.round(b + STRENGTH * (b - bAvg)));
                out[i] = (nr << 16) | (ng << 8) | nb;
            }
        }
        img.setRGB(0, 0, w, h, out, 0, w);
    }

    private static int clamp255(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
