package com.ermzzz.burp.capture;

import burp.api.montoya.logging.Logging;

import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;

/**
 * Captures a screen region in dark theme and then applies a report-friendly
 * light document filter on pixels (without changing Burp Look&amp;Feel).
 * <p>
 * Switching global L&amp;F + {@code updateComponentTreeUI} across Burp can block EDT;
 * this approach keeps Burp responsive while producing report-ready images.
 */
public final class LightThemeCapture {

    /** Delay after removing glass pane to avoid capturing the selection border. */
    private static final int POST_OVERLAY_DELAY_MS = 220;
    /** Inset (px per side) to exclude border stroke + antialiasing from capture. */
    private static final int SELECTION_BORDER_INSET = 2;

    private LightThemeCapture() {
    }

    /**
     * Run from a worker thread (not EDT): {@link Robot}, pixel filtering and native clipboard
     * operations can block and would freeze UI if executed on EDT.
     */
    public static void captureRegionToClipboard(Frame burpFrame, Rectangle region, Logging logging) {
        captureRegionToClipboard(burpFrame, region, logging, true);
    }

    public static void captureRegionToClipboard(Frame burpFrame, Rectangle region, Logging logging, boolean applyFilter) {
        if (burpFrame == null || region == null) {
            return;
        }
        try {
            logging.logToOutput("Light screenshot: starting capture (frame=" + burpFrame.getTitle() + ", region=" + region + ")");
            Robot robot = new Robot();
            // Flush repaint after overlay removal; otherwise Robot can still capture the border.
            logging.logToOutput("Light screenshot: waiting for screen update (overlay)...");
            waitForScreenToUpdate(burpFrame, robot, logging);
            Rectangle captureRect = insetToSkipSelectionBorder(region);
            logging.logToOutput("Light screenshot: effective capture area (selection border excluded): " + captureRect);
            Image image = robot.createScreenCapture(captureRect);
            int w = image.getWidth(null);
            int h = image.getHeight(null);
            logging.logToOutput("Light screenshot: capture completed (" + w + "x" + h + ")");

            BufferedImage raw = toBufferedImage(image);
            if (applyFilter) {
                logging.logToOutput("Light screenshot: applying document-style filter (dark -> report/light)...");
                BufferedImage doc = DocumentLightFilter.toDocumentStyle(raw);
                ClipboardCapture.copyImageToClipboard(doc, logging);
            } else {
                logging.logToOutput("Light screenshot: original colors mode (no filter).");
                ClipboardCapture.copyImageToClipboard(raw, logging);
            }
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
            logging.logToOutput("Light screenshot: EDT wait interrupted, continuing.");
        } catch (InvocationTargetException e) {
            logging.logToOutput("Light screenshot: repaint flush: " + e.getCause());
        }
        robot.delay(POST_OVERLAY_DELAY_MS);
    }

    /**
     * Crops a few pixels inside the selected rectangle so the overlay border is not captured.
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
