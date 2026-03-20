package com.ermzzz.burp.capture;

import burp.api.montoya.logging.Logging;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ClipboardCapture {

    private static Transferable lastTransferable;

    /**
     * Scrive PNG, copia su clipboard (AWT su EDT + tool nativi Linux sul thread chiamante) e logga il path.
     * <p>
     * Chiamare da un <strong>thread di lavoro</strong>, non dall’EDT: così {@code waitFor()} di xclip/wl-copy
     * non blocca l’interfaccia di Burp.
     */
    public static void copyImageToClipboard(Image image, Logging logging) {
        if (image == null) {
            return;
        }
        try {
            BufferedImage buffered = toBufferedImage(image);

            // 1) Sempre PNG su disco (serve per xclip/wl-copy e fallback utente)
            Path saved = saveTempPng(buffered);
            if (saved != null) {
                logging.logToOutput("Light screenshot: PNG salvato: " + saved.toAbsolutePath());
            }

            // 2) AWT: molte JVM si aspettano setContents sull’EDT (breve, non blocca come xclip)
            setAwtClipboardContents(buffered, logging);

            // 3) Linux per ultimo: xclip/wl-copy da file (può attendere — deve restare fuori EDT)
            if (isLinux() && saved != null) {
                boolean nativeOk = tryLinuxClipboardFromFile(saved, logging);
                if (!nativeOk) {
                    logging.logToOutput("Light screenshot: xclip/wl-copy non riuscito: resta solo AWT + file PNG.");
                }
            }
        } catch (Exception e) {
            logging.logToError("Light screenshot: copia in clipboard fallita: " + e.getMessage());
        }
    }

    private static void setAwtClipboardContents(BufferedImage buffered, Logging logging) {
        Runnable task = () -> {
            try {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                Transferable t = new MultiFlavorImageTransferable(buffered);
                lastTransferable = t;
                ClipboardOwner owner = (cb, contents) -> { };
                clipboard.setContents(t, owner);
                logging.logToOutput("Light screenshot: system clipboard (AWT) aggiornata.");
            } catch (Exception e) {
                logging.logToError("Light screenshot: AWT clipboard: " + e.getMessage());
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
            return;
        }
        try {
            SwingUtilities.invokeAndWait(task);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logging.logToError("Light screenshot: AWT clipboard interrotta.");
        } catch (InvocationTargetException e) {
            Throwable c = e.getCause();
            logging.logToError("Light screenshot: AWT clipboard: " + (c != null ? c.getMessage() : e.getMessage()));
        }
    }

    private static boolean isLinux() {
        String os = System.getProperty("os.name", "");
        return os.toLowerCase().contains("linux");
    }

    /**
     * @return true se almeno un metodo nativo ha avuto exit 0
     */
    private static boolean tryLinuxClipboardFromFile(Path pngFile, Logging logging) {
        String wayland = System.getenv("WAYLAND_DISPLAY");
        if (wayland != null && !wayland.isEmpty()) {
            return tryWlCopyFromFile(pngFile, logging);
        }
        return tryXclipFromFile(pngFile, logging);
    }

    private static boolean tryWlCopyFromFile(Path pngFile, Logging logging) {
        String[] bins = {"wl-copy", "/usr/bin/wl-copy", "/bin/wl-copy"};
        for (String bin : bins) {
            try {
                ProcessBuilder pb = new ProcessBuilder(bin, "--type", "image/png");
                pb.redirectInput(pngFile.toFile());
                pb.redirectErrorStream(true);
                Process p = pb.start();
                String out = readStream(p.getInputStream());
                int code = p.waitFor();
                if (code == 0) {
                    logging.logToOutput("Light screenshot: clipboard immagine via wl-copy (Wayland).");
                    return true;
                }
                if (!out.isBlank()) {
                    logging.logToOutput("Light screenshot: wl-copy [" + bin + "] exit " + code + ": " + out.trim());
                }
            } catch (IOException e) {
                logging.logToOutput("Light screenshot: wl-copy non eseguibile (" + bin + "): " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logging.logToError("Light screenshot: wl-copy interrotto.");
                return false;
            }
        }
        return false;
    }

    private static boolean tryXclipFromFile(Path pngFile, Logging logging) {
        String[] bins = {"/usr/bin/xclip", "/bin/xclip", "xclip"};
        for (String bin : bins) {
            try {
                ProcessBuilder pb = new ProcessBuilder(
                        bin, "-selection", "clipboard", "-t", "image/png");
                pb.redirectInput(pngFile.toFile());
                pb.redirectErrorStream(true);
                Process p = pb.start();
                String out = readStream(p.getInputStream());
                int code = p.waitFor();
                if (code == 0) {
                    logging.logToOutput("Light screenshot: clipboard immagine via xclip (X11).");
                    return true;
                }
                if (!out.isBlank()) {
                    logging.logToOutput("Light screenshot: xclip [" + bin + "] exit " + code + ": " + out.trim());
                }
            } catch (IOException e) {
                logging.logToOutput("Light screenshot: xclip non eseguibile (" + bin + "): " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logging.logToError("Light screenshot: xclip interrotto.");
                return false;
            }
        }
        return false;
    }

    private static String readStream(InputStream is) throws IOException {
        byte[] b = is.readAllBytes();
        return new String(b, StandardCharsets.UTF_8);
    }

    private static Path saveTempPng(BufferedImage buffered) {
        try {
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path p = Files.createTempFile("burp-light-screenshot-", "-" + ts + ".png");
            try (OutputStream os = Files.newOutputStream(p)) {
                ImageIO.write(buffered, "png", os);
            }
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
