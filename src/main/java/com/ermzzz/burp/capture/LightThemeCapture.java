package com.ermzzz.burp.capture;

import burp.api.montoya.logging.Logging;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LightThemeCapture {

    public static void captureRegionToClipboard(Frame burpFrame, Rectangle region, Logging logging) {
        if (burpFrame == null || region == null) {
            return;
        }

        String originalLaf = UIManager.getLookAndFeel().getClass().getName();

        try {
            // switch a un L&F chiaro (Nimbus se disponibile, altrimenti Metal)
            String lightLaf = findLightLookAndFeelClassName(logging);

            List<Frame> burpFrames = findVisibleBurpFrames();
            runOnEdtAndWait(() -> {
                if (lightLaf != null && !lightLaf.equals(originalLaf)) {
                    UIManager.setLookAndFeel(lightLaf);
                }
                for (Frame f : burpFrames) {
                    SwingUtilities.updateComponentTreeUI(f);
                    f.invalidate();
                    f.validate();
                    f.repaint();
                }
            });

            Robot robot = new Robot();
            Image image = robot.createScreenCapture(region);

            ClipboardCapture.copyImageToClipboard(image, logging);

        } catch (Exception e) {
            logging.logToError("Light screenshot capture failed: " + e.getMessage());
        } finally {
            // ripristina L&F originale su tutti i frame Burp visibili
            try {
                List<Frame> burpFrames = findVisibleBurpFrames();
                runOnEdtAndWait(() -> {
                    if (originalLaf != null && !originalLaf.equals(UIManager.getLookAndFeel().getClass().getName())) {
                        UIManager.setLookAndFeel(originalLaf);
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

