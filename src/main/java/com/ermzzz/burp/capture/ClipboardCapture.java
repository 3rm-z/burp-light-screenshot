package com.ermzzz.burp.capture;

import burp.api.montoya.logging.Logging;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;

public class ClipboardCapture {

    public static void copyImageToClipboard(Image image, Logging logging) {
        if (image == null) {
            return;
        }
        try {
            BufferedImage buffered;
            if (image instanceof BufferedImage bi) {
                buffered = bi;
            } else {
                buffered = new BufferedImage(
                        image.getWidth(null),
                        image.getHeight(null),
                        BufferedImage.TYPE_INT_ARGB
                );
                Graphics2D g2 = buffered.createGraphics();
                g2.drawImage(image, 0, 0, null);
                g2.dispose();
            }

            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable t = new ImageSelection(buffered);
            clipboard.setContents(t, null);
            logging.logToOutput("Light screenshot: immagine copiata in clipboard.");
        } catch (Exception e) {
            logging.logToError("Light screenshot: copia in clipboard fallita: " + e.getMessage());
        }
    }
}

