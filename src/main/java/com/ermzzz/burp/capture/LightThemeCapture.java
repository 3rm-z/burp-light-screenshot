package com.ermzzz.burp.capture;

import burp.api.montoya.logging.Logging;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Cattura una regione dello schermo mentre lavori in dark theme, poi applica un filtro
 * “stile documento” chiaro sui pixel (senza toccare il Look&amp;Feel di Burp).
 * <p>
 * Il cambio globale di L&amp;F + {@code updateComponentTreeUI} su tutta Burp può bloccare l'EDT;
 * questo approccio mantiene Burp stabile e produce comunque immagini più adatte ai report.
 */
public final class LightThemeCapture {

    private LightThemeCapture() {
    }

    /**
     * Esegui da un thread di lavoro (non EDT): {@link Robot}, filtro pixel e clipboard nativa
     * possono bloccare a lungo; sull’EDT congelerebbero l’interfaccia.
     */
    public static void captureRegionToClipboard(Frame burpFrame, Rectangle region, Logging logging) {
        if (burpFrame == null || region == null) {
            return;
        }
        try {
            logging.logToOutput("Light screenshot: avvio capture (frame=" + burpFrame.getTitle() + ", region=" + region + ")");
            Robot robot = new Robot();
            logging.logToOutput("Light screenshot: Robot creato, cattura schermo...");
            Image image = robot.createScreenCapture(region);
            int w = image.getWidth(null);
            int h = image.getHeight(null);
            logging.logToOutput("Light screenshot: cattura completata (" + w + "x" + h + ")");

            BufferedImage raw = toBufferedImage(image);
            logging.logToOutput("Light screenshot: applico filtro stile documento (dark -> report chiaro)...");
            BufferedImage doc = DocumentLightFilter.toDocumentStyle(raw);
            ClipboardCapture.copyImageToClipboard(doc, logging);
        } catch (Throwable t) {
            logging.logToError("Light screenshot capture failed: " + t);
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            logging.logToError(sw.toString());
        }
    }

    private static BufferedImage toBufferedImage(Image image) {
        if (image instanceof BufferedImage bi) {
            return bi;
        }
        int w = image.getWidth(null);
        int h = image.getHeight(null);
        BufferedImage buffered = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = buffered.createGraphics();
        g2.drawImage(image, 0, 0, null);
        g2.dispose();
        return buffered;
    }
}
