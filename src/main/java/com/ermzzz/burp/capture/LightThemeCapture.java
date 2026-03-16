package com.ermzzz.burp.capture;

import burp.api.montoya.logging.Logging;

import javax.swing.*;
import java.awt.*;

public class LightThemeCapture {

    public static void captureRegionToClipboard(Frame burpFrame, Rectangle region, Logging logging) {
        if (burpFrame == null || region == null) {
            return;
        }

        String originalLaf = UIManager.getLookAndFeel().getClass().getName();

        try {
            // switch a un L&F chiaro (Nimbus se disponibile, altrimenti Metal)
            String lightLaf = findLightLookAndFeelClassName(logging);
            if (lightLaf != null && !lightLaf.equals(originalLaf)) {
                UIManager.setLookAndFeel(lightLaf);
                SwingUtilities.updateComponentTreeUI(burpFrame);
            }

            // Assicura che la UI sia ridipinta
            burpFrame.invalidate();
            burpFrame.validate();
            burpFrame.repaint();

            // piccola sincronizzazione EDT
            try {
                EventQueue.invokeAndWait(() -> {
                });
            } catch (Exception ignored) {
            }

            Robot robot = new Robot();
            Image image = robot.createScreenCapture(region);

            ClipboardCapture.copyImageToClipboard(image, logging);

        } catch (Exception e) {
            logging.logToError("Light screenshot capture failed: " + e.getMessage());
        } finally {
            // ripristina L&F originale
            try {
                if (originalLaf != null && !originalLaf.equals(UIManager.getLookAndFeel().getClass().getName())) {
                    UIManager.setLookAndFeel(originalLaf);
                    SwingUtilities.updateComponentTreeUI(burpFrame);
                    burpFrame.invalidate();
                    burpFrame.validate();
                    burpFrame.repaint();
                }
            } catch (Exception e) {
                logging.logToError("Failed to restore original LookAndFeel: " + e.getMessage());
            }
        }
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

