package com.ermzzz.burp.capture;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Avvicina i colori a un light theme tipo FlatLaf / Burp (senza cambiare il L&amp;F in runtime).
 * Euristica su HSB: carta bianca, toolbar grigio, arancio metodo HTTP, teal header, verdi status, XML ecc.
 */
public final class FlatLafApproxFilter {

    private FlatLafApproxFilter() {
    }

    public static void apply(BufferedImage img) {
        if (img == null) {
            return;
        }
        int w = img.getWidth();
        int h = img.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = img.getRGB(x, y);
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                float[] hsb = Color.RGBtoHSB(r, g, b, null);
                float H = hsb[0];
                float S = hsb[1];
                float Br = hsb[2];

                float a = 0f;
                int tr = 255;
                int tg = 255;
                int tb = 255;

                if (S < 0.11f && Br > 0.84f) {
                    // Editor / carta → #FFFFFF
                    a = smoothstep(0.84f, 0.97f, Br) * (1f - S / 0.11f) * 0.72f;
                    tr = 255;
                    tg = 255;
                    tb = 255;
                } else if (S < 0.14f && Br > 0.68f && Br < 0.86f) {
                    // Barra tab / toolbar → ~#E8E8E8
                    a = (1f - S / 0.14f) * 0.42f;
                    tr = 232;
                    tg = 232;
                    tb = 232;
                } else if (S < 0.12f && Br > 0.78f && Br <= 0.92f) {
                    // Gutter numeri riga → ~#F5F5F5
                    a = (1f - S / 0.12f) * 0.35f;
                    tr = 245;
                    tg = 245;
                    tb = 245;
                } else if (isRedHue(H) && S > 0.16f && Br > 0.22f) {
                    // Tag XML / coral → #E53935
                    a = smoothstep(0.16f, 0.42f, S) * 0.52f;
                    tr = 229;
                    tg = 57;
                    tb = 53;
                } else if (H >= 0.70f && H <= 0.86f && S > 0.14f) {
                    // Attributi viola → #8E24AA
                    a = smoothstep(0.14f, 0.38f, S) * 0.48f;
                    tr = 142;
                    tg = 36;
                    tb = 170;
                } else if (H >= 0.52f && H <= 0.68f && S > 0.14f && Br > 0.22f) {
                    // Stringhe / valori blu → #1976D2
                    a = smoothstep(0.14f, 0.42f, S) * 0.48f;
                    tr = 25;
                    tg = 118;
                    tb = 210;
                } else if (H >= 0.38f && H <= 0.54f && S > 0.11f) {
                    // Nomi header teal → #006B6B
                    a = smoothstep(0.11f, 0.36f, S) * 0.55f;
                    tr = 0;
                    tg = 107;
                    tb = 107;
                } else if (H >= 0.20f && H <= 0.40f && S > 0.14f) {
                    // 200 OK verde → #2E7D32
                    a = smoothstep(0.14f, 0.38f, S) * 0.48f;
                    tr = 46;
                    tg = 125;
                    tb = 50;
                } else if (H >= 0.02f && H <= 0.12f && S > 0.22f && Br > 0.32f) {
                    // GET / Send arancio → #FF6D00
                    a = smoothstep(0.22f, 0.52f, S) * 0.58f;
                    tr = 255;
                    tg = 109;
                    tb = 0;
                } else if (H >= 0.58f && H <= 0.75f && S > 0.12f && Br < 0.35f) {
                    // Testo navy / URL scuri → #1A1A2E
                    a = smoothstep(0.12f, 0.30f, S) * 0.35f;
                    tr = 26;
                    tg = 26;
                    tb = 46;
                } else if (H >= 0.62f && H <= 0.78f && S > 0.18f && Br > 0.18f && Br < 0.55f) {
                    // Burp AI / navy button → #283593
                    a = smoothstep(0.18f, 0.40f, S) * 0.45f;
                    tr = 40;
                    tg = 53;
                    tb = 147;
                }

                if (a > 0.02f) {
                    int nr = lerp255(r, tr, a);
                    int ng = lerp255(g, tg, a);
                    int nb = lerp255(b, tb, a);
                    img.setRGB(x, y, (nr << 16) | (ng << 8) | nb);
                }
            }
        }
    }

    private static boolean isRedHue(float H) {
        return H <= 0.045f || H >= 0.965f;
    }

    private static int lerp255(int from, int to, float a) {
        return Math.min(255, Math.max(0, Math.round(from * (1f - a) + to * a)));
    }

    private static float smoothstep(float e0, float e1, float x) {
        if (e1 <= e0) {
            return x >= e1 ? 1f : 0f;
        }
        float t = (x - e0) / (e1 - e0);
        if (t < 0f) {
            t = 0f;
        } else if (t > 1f) {
            t = 1f;
        }
        return t * t * (3f - 2f * t);
    }
}
