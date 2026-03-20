package com.ermzzz.burp.capture;

import burp.api.montoya.logging.Logging;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ClipboardCapture {

    /** Mantiene vivo il Transferable finché la clipboard non lo sostituisce (X11). */
    private static Transferable lastTransferable;

    /**
     * Copia l'immagine in clipboard con più strategie (AWT + opzionale xclip su Linux) e salva PNG in /tmp.
     */
    public static void copyImageToClipboard(Image image, Logging logging) {
        if (image == null) {
            return;
        }
        try {
            BufferedImage buffered = toBufferedImage(image);

            // 1) AWT system clipboard + owner (evita GC / problemi X11)
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable t = new MultiFlavorImageTransferable(buffered);
            lastTransferable = t;
            // Owner non-null: su alcuni X11 server evita che il Transferable venga scartato prima del primo incolla.
            ClipboardOwner owner = (cb, contents) -> { /* ownership persa: normale */ };
            clipboard.setContents(t, owner);
            logging.logToOutput("Light screenshot: immagine registrata su system clipboard (AWT).");

            // 2) Linux: AWT spesso non espone PNG ad altre app; pipe nativa (Wayland o X11)
            if (isLinux()) {
                tryLinuxNativeClipboardPipe(buffered, logging);
            }

            // 3) Fallback sempre utile: file su disco
            Path saved = saveTempPng(buffered);
            if (saved != null) {
                logging.logToOutput("Light screenshot: PNG salvato (fallback): " + saved.toAbsolutePath());
            }
        } catch (Exception e) {
            logging.logToError("Light screenshot: copia in clipboard fallita: " + e.getMessage());
        }
    }

    private static boolean isLinux() {
        String os = System.getProperty("os.name", "");
        return os.toLowerCase().contains("linux");
    }

    private static void tryLinuxNativeClipboardPipe(BufferedImage buffered, Logging logging) {
        String wayland = System.getenv("WAYLAND_DISPLAY");
        if (wayland != null && !wayland.isEmpty()) {
            tryWlCopyPng(buffered, logging);
        } else {
            tryXclipPng(buffered, logging);
        }
    }

    private static void tryWlCopyPng(BufferedImage buffered, Logging logging) {
        try {
            ProcessBuilder pb = new ProcessBuilder("wl-copy", "--type", "image/png");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (OutputStream out = p.getOutputStream()) {
                ImageIO.write(buffered, "png", out);
            }
            int code = p.waitFor();
            if (code == 0) {
                logging.logToOutput("Light screenshot: clipboard aggiornata via wl-copy (Wayland, image/png).");
            } else {
                logging.logToOutput("Light screenshot: wl-copy terminato con codice " + code + ".");
            }
        } catch (IOException e) {
            logging.logToOutput("Light screenshot: wl-copy non disponibile: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logging.logToError("Light screenshot: wl-copy interrotto.");
        }
    }

    private static void tryXclipPng(BufferedImage buffered, Logging logging) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "xclip", "-selection", "clipboard", "-t", "image/png", "-i");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (OutputStream out = p.getOutputStream()) {
                ImageIO.write(buffered, "png", out);
            }
            int code = p.waitFor();
            if (code == 0) {
                logging.logToOutput("Light screenshot: clipboard aggiornata anche via xclip (image/png).");
            } else {
                logging.logToOutput("Light screenshot: xclip terminato con codice " + code + " (installa xclip se vuoi clipboard PNG su Linux).");
            }
        } catch (IOException e) {
            logging.logToOutput("Light screenshot: xclip non disponibile o fallito: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logging.logToError("Light screenshot: xclip interrotto.");
        }
    }

    private static Path saveTempPng(BufferedImage buffered) {
        try {
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path p = Files.createTempFile("burp-light-screenshot-", "-" + ts + ".png");
            ImageIO.write(buffered, "png", p.toFile());
            return p;
        } catch (IOException e) {
            return null;
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
