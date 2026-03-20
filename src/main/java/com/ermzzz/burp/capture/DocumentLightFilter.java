package com.ermzzz.burp.capture;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Trasforma uno screenshot del tema scuro di Burp in uno stile “documento chiaro” per i report.
 * <p>
 * Dopo la mappatura inversa luminanza, applica uno <strong>stretch percentile</strong> sulla luminanza
 * (simile a Livelli in GIMP) per togliere il “velo grigio” quando i bianchi restano ammassati sotto 240.
 * I pixel quasi neutri (carta) non passano da HSB aggressivo, così non si fangono ulteriormente.
 */
public final class DocumentLightFilter {

    private static final float INK_L = 0.04f;
    private static final float PAPER_L = 0.997f;
    private static final float GAMMA = 0.78f;
    private static final float SATURATION_BOOST = 1.52f;
    private static final float VALUE_BOOST = 1.06f;
    private static final float LUM_FLOOR = 0.018f;
    /** ≥ PAPER_L / LUM_FLOOR */
    private static final float SCALE_CAP = 60f;

    /** Percentili luminanza per lo stretch (esclude outlier). */
    private static final float STRETCH_LOW_FRACT = 0.02f;
    private static final float STRETCH_HIGH_FRACT = 0.02f;
    /** Output luminanza dopo stretch: lascia un po’ di inchiostro sotto. */
    private static final int STRETCH_OUT_LOW = 12;
    private static final int STRETCH_OUT_HIGH = 255;

    private DocumentLightFilter() {
    }

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

                float rf = r / 255f;
                float gf = g / 255f;
                float bf = b / 255f;
                float ln = 0.2126f * rf + 0.7152f * gf + 0.0722f * bf;
                if (ln < LUM_FLOOR) {
                    ln = LUM_FLOOR;
                }

                float lOut = INK_L + (PAPER_L - INK_L) * (1f - (float) Math.pow(ln, GAMMA));

                float scale = lOut / ln;
                if (scale > SCALE_CAP) {
                    scale = SCALE_CAP;
                }

                float r2 = clamp01(rf * scale);
                float g2 = clamp01(gf * scale);
                float b2 = clamp01(bf * scale);

                int ri = Math.round(r2 * 255f);
                int gi = Math.round(g2 * 255f);
                int bi = Math.round(b2 * 255f);

                float lum = luminanceByte(ri, gi, bi) / 255f;
                float maxc = max3(r2, g2, b2);
                float minc = min3(r2, g2, b2);
                float chroma = maxc - minc;

                if (chroma < 0.07f && lum > 0.72f) {
                    // Carta / grigi chiari: niente HSB (evita tinte sporche), avvicina al bianco
                    float whiten = smoothstep(0.72f, 0.93f, lum) * 0.45f;
                    ri = blend255(ri, whiten);
                    gi = blend255(gi, whiten);
                    bi = blend255(bi, whiten);
                } else {
                    float[] hsb = Color.RGBtoHSB(ri, gi, bi, null);
                    float s = hsb[1];
                    float accent = smoothstep(0.07f, 0.38f, s);
                    float satMul = 1f + (SATURATION_BOOST - 1f) * (0.2f + 0.8f * accent);
                    hsb[1] = clamp01(s * satMul);
                    float valMul = 1f + (VALUE_BOOST - 1f) * (1f - 0.65f * accent);
                    hsb[2] = clamp01(hsb[2] * valMul);
                    int rgb = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]) & 0xFFFFFF;
                    out.setRGB(x, y, rgb);
                    continue;
                }
                int rgb = (ri << 16) | (gi << 8) | bi;
                out.setRGB(x, y, rgb);
            }
        }

        applyLuminanceHistogramStretch(out);
        return out;
    }

    /**
     * Allarga il range luminanza (2°–98° percentile → [STRETCH_OUT_LOW, STRETCH_OUT_HIGH]) mantenendo il rapporto RGB.
     */
    private static void applyLuminanceHistogramStretch(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int n = w * h;
        if (n < 64) {
            return;
        }

        int[] hist = new int[256];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = img.getRGB(x, y);
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                int L = luminanceByte(r, g, b);
                if (L < 0) {
                    L = 0;
                } else if (L > 255) {
                    L = 255;
                }
                hist[L]++;
            }
        }

        int lowCount = Math.max(1, Math.round(n * STRETCH_LOW_FRACT));
        int highCount = Math.max(1, Math.round(n * STRETCH_HIGH_FRACT));

        int lowBin = 0;
        int acc = 0;
        while (lowBin < 255 && acc < lowCount) {
            acc += hist[lowBin];
            lowBin++;
        }
        lowBin = Math.max(0, lowBin - 1);

        int highBin = 255;
        acc = 0;
        while (highBin > 0 && acc < highCount) {
            acc += hist[highBin];
            highBin--;
        }
        highBin = Math.min(255, highBin + 1);

        if (highBin <= lowBin + 8) {
            return;
        }

        float inScale = 1f / (highBin - lowBin);
        float outRange = STRETCH_OUT_HIGH - STRETCH_OUT_LOW;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = img.getRGB(x, y);
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                int L = luminanceByte(r, g, b);
                float t = (L - lowBin) * inScale;
                if (t < 0f) {
                    t = 0f;
                } else if (t > 1f) {
                    t = 1f;
                }
                float Lnew = STRETCH_OUT_LOW + t * outRange;
                float scale = Lnew / Math.max(L, 1f);
                int rn = clamp255(Math.round(r * scale));
                int gn = clamp255(Math.round(g * scale));
                int bn = clamp255(Math.round(b * scale));
                img.setRGB(x, y, (rn << 16) | (gn << 8) | bn);
            }
        }
    }

    private static int luminanceByte(int r, int g, int b) {
        return Math.round(0.2126f * r + 0.7152f * g + 0.0722f * b);
    }

    private static int blend255(int c, float towardWhite) {
        return clamp255(Math.round(c + (255 - c) * towardWhite));
    }

    private static int clamp255(int v) {
        if (v < 0) {
            return 0;
        }
        if (v > 255) {
            return 255;
        }
        return v;
    }

    private static float max3(float a, float b, float c) {
        return Math.max(a, Math.max(b, c));
    }

    private static float min3(float a, float b, float c) {
        return Math.min(a, Math.min(b, c));
    }

    private static float clamp01(float v) {
        if (v < 0f) {
            return 0f;
        }
        if (v > 1f) {
            return 1f;
        }
        return v;
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        if (edge1 <= edge0) {
            return x >= edge1 ? 1f : 0f;
        }
        float t = clamp01((x - edge0) / (edge1 - edge0));
        return t * t * (3f - 2f * t);
    }
}
