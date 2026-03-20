package com.ermzzz.burp.capture;

import burp.api.montoya.logging.Logging;

import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;

/**
 * Cattura una regione dello schermo mentre lavori in dark theme, poi applica un filtro
 * “stile documento” chiaro sui pixel (senza toccare il Look&amp;Feel di Burp).
 * <p>
 * Il cambio globale di L&amp;F + {@code updateComponentTreeUI} su tutta Burp può bloccare l'EDT;
 * questo approccio mantiene Burp stabile e produce comunque immagini più adatte ai report.
 */
public final class LightThemeCapture {

    /** Pausa dopo aver tolto il glass pane: altrimenti X11/compositor cattura ancora il bordo rosso. */
    private static final int POST_OVERLAY_DELAY_MS = 220;
    /** Ritaglio interno (px per lato) per escludere stroke rosso + antialias dal mirino. */
    private static final int SELECTION_BORDER_INSET = 2;

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
            // Flush repaint dopo rimozione overlay (senza pausa il Robot vede ancora il bordo rosso).
            logging.logToOutput("Light screenshot: attesa aggiornamento schermo (overlay)...");
            waitForScreenToUpdate(burpFrame, robot, logging);
            Rectangle captureRect = insetToSkipSelectionBorder(region);
            logging.logToOutput("Light screenshot: area cattura effettiva (bordo selezione escluso): " + captureRect);
            Image image = robot.createScreenCapture(captureRect);
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

    private static void waitForScreenToUpdate(Frame burpFrame, Robot robot, Logging logging) {
        try {
            SwingUtilities.invokeAndWait(() -> {
                burpFrame.invalidate();
                burpFrame.validate();
                burpFrame.repaint();
                if (burpFrame instanceof javax.swing.JFrame jf) {
                    jf.getRootPane().repaint();
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logging.logToOutput("Light screenshot: attesa EDT interrotta, proseguo comunque.");
        } catch (InvocationTargetException e) {
            logging.logToOutput("Light screenshot: repaint flush: " + e.getCause());
        }
        robot.delay(POST_OVERLAY_DELAY_MS);
    }

    /**
     * Ritaglia qualche pixel dentro il rettangolo scelto così non finisce nello shot il contorno rosso dell’overlay.
     */
    private static Rectangle insetToSkipSelectionBorder(Rectangle region) {
        int inset = SELECTION_BORDER_INSET;
        int w = region.width - 2 * inset;
        int h = region.height - 2 * inset;
        if (w < 16 || h < 16) {
            return new Rectangle(region);
        }
        return new Rectangle(region.x + inset, region.y + inset, w, h);
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
