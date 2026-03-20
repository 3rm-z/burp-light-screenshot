package com.ermzzz.burp.capture;

import java.awt.image.BufferedImage;

/**
 * Trasforma uno screenshot del tema scuro di Burp in uno stile “documento chiaro” per i report.
 * <p>
 * Il vecchio filtro trattava <strong>tutti</strong> i pixel a bassa luminanza come sfondo e li
 * schiariva verso il bianco: anche testo e controlli grigi venivano cancellati (grigio illeggibile).
 * <p>
 * Qui si usa una <strong>mappatura inversa della luminanza</strong> (scuro → carta chiara,
 * chiaro → inchiostro scuro) mantenendo il rapporto tra R, G e B così restano leggibili colori
 * d’accento e anti-aliasing.
 */
public final class DocumentLightFilter {

    /** Luminanza output per pixel che erano “inchiostro” (testo chiaro sul dark). */
    private static final float INK_L = 0.07f;
    /** Luminanza output per pixel che erano sfondo scuro. */
    private static final float PAPER_L = 0.97f;
    /**
     * Curva sulla luminanza in ingresso [0–1]: &lt; 1 stringe la zona centrale (più contrasto
     * tra sfondo e contenuto).
     */
    private static final float GAMMA = 0.58f;
    /** Evita scale esplosive sul rumore/cerniere nere. */
    private static final float LUM_FLOOR = 0.018f;
    /** Limite scala RGB per pixel rumorosissimi. */
    private static final float SCALE_CAP = 32f;

    private DocumentLightFilter() {
    }

    /**
     * Applica trasformazione stile documento chiaro pixel per pixel.
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

                float rf = r / 255f;
                float gf = g / 255f;
                float bf = b / 255f;
                // Luminanza percettiva su sRGB (stesso ordine di grandezza del vecchio filtro)
                float ln = 0.2126f * rf + 0.7152f * gf + 0.0722f * bf;
                if (ln < LUM_FLOOR) {
                    ln = LUM_FLOOR;
                }

                // Scuro (sfondo) → carta chiara; chiaro (testo/UI) → inchiostro scuro
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
                int rgb = (ri << 16) | (gi << 8) | bi;
                out.setRGB(x, y, rgb);
            }
        }
        return out;
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
}
