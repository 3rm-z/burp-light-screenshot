package com.ermzzz.burp.capture;

import burp.api.montoya.logging.Logging;

import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LightThemeCapture {

    public static void captureRegionToClipboard(Frame burpFrame, Rectangle region, Logging logging) {
        if (burpFrame == null || region == null) {
            return;
        }

        LookAndFeel originalLookAndFeel = UIManager.getLookAndFeel();

        try {
            logging.logToOutput("Light screenshot: avvio capture (region=" + region + ")");
            // switch a un L&F chiaro (Nimbus se disponibile, altrimenti Metal)
            String lightLaf = findLightLookAndFeelClassName(logging);

            List<Frame> burpFrames = findVisibleBurpFrames();
            runOnEdtAndWait(() -> {
                if (lightLaf != null && (originalLookAndFeel == null || !lightLaf.equals(originalLookAndFeel.getClass().getName()))) {
                    try {
                        UIManager.setLookAndFeel(lightLaf);
                    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
                        logging.logToError("Light screenshot: setLookAndFeel light failed: " + ex.getMessage());
                    }
                }
                for (Frame f : burpFrames) {
                    SwingUtilities.updateComponentTreeUI(f);
                    f.invalidate();
                    f.validate();
                    f.repaint();
                }
            });

            Robot robot = new Robot();
            logging.logToOutput("Light screenshot: Robot creato, cattura schermo...");
            Image image = robot.createScreenCapture(region);
            logging.logToOutput("Light screenshot: cattura completata (" + image.getWidth(null) + "x" + image.getHeight(null) + ")");

            // Siamo già sull'EDT (invokeLater da BurpExtender): copia diretta
            ClipboardCapture.copyImageToClipboard(image, logging);

        } catch (Throwable t) {
            logging.logToError("Light screenshot capture failed: " + t);
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            logging.logToError(sw.toString());
        } finally {
            // ripristina L&F originale su tutti i frame Burp visibili
            try {
                List<Frame> burpFrames = findVisibleBurpFrames();
                runOnEdtAndWait(() -> {
                    if (originalLookAndFeel != null && UIManager.getLookAndFeel() != originalLookAndFeel) {
                        try {
                            UIManager.setLookAndFeel(originalLookAndFeel);
                        } catch (Exception ex) {
                            logging.logToError("Light screenshot: restore LookAndFeel failed: " + ex.getMessage());
                        }
                    }
                    for (Frame f : burpFrames) {
                        SwingUtilities.updateComponentTreeUI(f);
                        f.invalidate();
                        f.validate();
                        f.repaint();
                    }
                });
            } catch (Exception e) {
                logging.logToError("Failed to restore original LookAndFeel: " + e.getMessage());
            }
        }
    }

    private static void runOnEdtAndWait(Runnable r) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
            return;
        }
        EventQueue.invokeAndWait(r);
    }

    private static List<Frame> findVisibleBurpFrames() {
        Frame[] frames = Frame.getFrames();
        if (frames == null) {
            return Arrays.asList();
        }
        List<Frame> out = new ArrayList<>();
        for (Frame f : frames) {
            try {
                if (f == null) continue;
                if (!f.isVisible()) continue;
                if (f.getTitle() == null) continue;
                String title = f.getTitle().toLowerCase();
                if (title.contains("burp")) {
                    out.add(f);
                }
            } catch (Exception ignored) {
            }
        }
        if (out.isEmpty()) {
            // fallback al frame passato (anche se non matcha titolo)
            Frame[] frames2 = Frame.getFrames();
            for (Frame f : frames2) {
                if (f != null && f.isVisible()) {
                    out.add(f);
                }
            }
        }
        return out;
    }

    private static String findLightLookAndFeelClassName(Logging logging) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                String name = info.getName().toLowerCase();
                if (name.contains("nimbus")) {
                    return info.getClassName();
                }
            }
        } catch (Exception e) {
            logging.logToError("Error while searching Nimbus L&F: " + e.getMessage());
        }

        // fallback a Metal
        return "javax.swing.plaf.metal.MetalLookAndFeel";
    }
}

