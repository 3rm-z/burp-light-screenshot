package com.ermzzz.burp.capture;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Avvicina i colori a un light theme tipo FlatLaf / Burp (senza cambiare il L&amp;F in runtime).
 */
public final class FlatLafApproxFilter {

    /** Arancio pulsante / GET più acceso (#FF780A). */
    private static final int ORANGE_R = 255;
    private static final int ORANGE_G = 102;
    private static final int ORANGE_B = 0;

    /** Burp AI — indigo più chiaro (#5C6BC0). */
    private static final int AI_R = 150;
    private static final int AI_G = 160;
    private static final int AI_B = 240;

    /** Verde status più vivo (#43A047). */
    private static final int GREEN_R = 67;
    private static final int GREEN_G = 160;
    private static final int GREEN_B = 71;

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
                    a = smoothstep(0.84f, 0.97f, Br) * (1f - S / 0.11f) * 0.72f;
                    tr = 255;
                    tg = 255;
                    tb = 255;
                } else if (H >= 0.54f && H <= 0.64f && S >= 0.08f && S <= 0.28f && Br > 0.80f) {
                    // Nomi header HTTP in dark — azzurro chiarissimo #D1E8F9 → blu documento
                    a = smoothstep(0.08f, 0.28f, S) * 0.82f;
                    tr = 0;
                    tg = 85;
                    tb = 170;
                } else if (S < 0.14f && Br > 0.68f && Br < 0.86f) {
                    a = (1f - S / 0.14f) * 0.42f;
                    tr = 232;
                    tg = 232;
                    tb = 232;
                } else if (S < 0.12f && Br > 0.78f && Br <= 0.92f) {
                    a = (1f - S / 0.12f) * 0.35f;
                    tr = 245;
                    tg = 245;
                    tb = 245;
                } else if (isRedHue(H) && S > 0.09f && Br > 0.18f) {
                    a = smoothstep(0.09f, 0.38f, S) * 0.58f;
                    tr = 229;
                    tg = 57;
                    tb = 53;
                } else if (H >= 0.70f && H <= 0.86f && S > 0.11f) {
                    a = smoothstep(0.11f, 0.36f, S) * 0.52f;
                    tr = 142;
                    tg = 36;
                    tb = 170;
                } else if (H >= 0.50f && H <= 0.70f && S > 0.10f && Br > 0.18f) {
                    a = smoothstep(0.10f, 0.38f, S) * 0.18f;
                    tr = 25;
                    tg = 118;
                    tb = 210;
                } else if (H >= 0.28f && H <= 0.62f && S > 0.04f) {
                    // Header teal anche se poco saturi (dopo filtro finiscono grigi)
                    a = smoothstep(0.04f, 0.28f, S) * 0.45f;
                    tr = 0;
                    tg = 107;
                    tb = 107;
                } else if (H >= 0.18f && H <= 0.44f && S > 0.10f) {
                    a = smoothstep(0.10f, 0.36f, S) * 0.58f;
                    tr = GREEN_R;
                    tg = GREEN_G;
                    tb = GREEN_B;
                } else if (H >= 0.04f && H <= 0.12f && S > 0.04f) {
                    // Cookie / valori arancioni
                    a = smoothstep(0.04f, 0.2f, S) * 0.60f;
                    tr = ORANGE_R; 
                    tg = ORANGE_G; 
                    tb = ORANGE_B;
                } else if (H >= 0.58f && H <= 0.75f && S > 0.10f && Br < 0.38f) {
                    a = smoothstep(0.10f, 0.28f, S) * 0.38f;
                    tr = 26;
                    tg = 26;
                    tb = 46;
                } else if (H >= 0.58f && H <= 0.80f && S > 0.14f && Br > 0.15f && Br < 0.62f) {
                    a = smoothstep(0.14f, 0.38f, S) * 0.58f;
                    tr = AI_R;
                    tg = AI_G;
                    tb = AI_B;
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
