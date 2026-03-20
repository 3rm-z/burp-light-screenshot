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
import java.util.Map;

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

            // Su Linux/X11 l’AWT spesso non incolla in altre app e può “rubare” ownership alla clipboard:
            // usiamo solo xclip/wl-copy (vedi tryLinuxClipboardFromFile).
            if (!isLinux()) {
                setAwtClipboardContents(buffered, logging);
            } else {
                logging.logToOutput("Light screenshot: Linux — salto clipboard AWT, uso xclip/wl-copy.");
            }

            if (isWindows() && saved != null) {
                tryWindowsClipboardFromPngFile(saved, logging);
            }

            if (isLinux() && saved != null) {
                linuxSleepAfterPngWrite();
                boolean nativeOk = tryLinuxClipboardFromFile(saved, logging);
                if (!nativeOk) {
                    logging.logToOutput("Light screenshot: xclip/wl-copy non riuscito. File PNG salvato sopra; "
                            + "nota: molte VM sincronizzano verso Windows solo testo, non immagini.");
                }
            }
        } catch (Exception e) {
            logging.logToError("Light screenshot: copia in clipboard fallita: " + e.getMessage());
        }
    }

    private static void setAwtClipboardContents(BufferedImage buffered, Logging logging) {
        if (SwingUtilities.isEventDispatchThread()) {
            doAwtClipboardSet(buffered, logging);
            return;
        }
        // Su Windows Burp spesso accetta setContents dal worker; invokeAndWait può fallire o andare in stallo in alcuni setup.
        if (isWindows()) {
            try {
                doAwtClipboardSet(buffered, logging);
                return;
            } catch (Exception e) {
                logging.logToOutput("Light screenshot: AWT da worker fallita, provo su EDT: " + e.getMessage());
            }
        }
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    doAwtClipboardSet(buffered, logging);
                } catch (Exception e) {
                    logging.logToError("Light screenshot: AWT clipboard (EDT): " + e.getMessage());
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logging.logToError("Light screenshot: AWT clipboard interrotta.");
        } catch (InvocationTargetException e) {
            Throwable c = e.getCause();
            logging.logToError("Light screenshot: AWT clipboard (EDT): " + (c != null ? c.getMessage() : e.getMessage()));
        }
    }

    private static void doAwtClipboardSet(BufferedImage buffered, Logging logging) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable t = new MultiFlavorImageTransferable(buffered);
        lastTransferable = t;
        ClipboardOwner owner = (cb, contents) -> { };
        clipboard.setContents(t, owner);
        logging.logToOutput("Light screenshot: system clipboard (AWT) aggiornata.");
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    /**
     * Clipboard immagine tramite PowerShell + WinForms (STA). Utile quando l’AWT clipboard con Burp non incolla.
     */
    private static void tryWindowsClipboardFromPngFile(Path pngFile, Logging logging) {
        String path = pngFile.toAbsolutePath().toString().replace("'", "''");
        String command = String.format(
                "Add-Type -AssemblyName System.Windows.Forms; "
                        + "Add-Type -AssemblyName System.Drawing; "
                        + "$i = [System.Drawing.Image]::FromFile('%s'); "
                        + "[System.Windows.Forms.Clipboard]::SetImage($i); "
                        + "$i.Dispose()",
                path);
        String[] cmd = {"powershell.exe", "-NoProfile", "-Sta", "-Command", command};
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String out = readStream(p.getInputStream());
            int code = p.waitFor();
            if (code == 0) {
                logging.logToOutput("Light screenshot: clipboard immagine via PowerShell (WinForms).");
            } else {
                logging.logToOutput("Light screenshot: PowerShell clipboard exit " + code
                        + (out.isBlank() ? "" : (": " + out.trim())));
            }
        } catch (IOException e) {
            logging.logToOutput("Light screenshot: PowerShell clipboard non eseguibile: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logging.logToError("Light screenshot: PowerShell clipboard interrotto.");
        }
    }

    private static boolean isLinux() {
        String os = System.getProperty("os.name", "");
        return os.toLowerCase().contains("linux");
    }

    private static void linuxSleepAfterPngWrite() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void logLinuxClipboardEnv(Logging logging) {
        logging.logToOutput("Light screenshot: DISPLAY=" + envOrDash("DISPLAY")
                + " WAYLAND_DISPLAY=" + envOrDash("WAYLAND_DISPLAY")
                + " XDG_SESSION_TYPE=" + envOrDash("XDG_SESSION_TYPE"));
    }

    private static String envOrDash(String name) {
        String v = System.getenv(name);
        return v == null || v.isEmpty() ? "(non impostato)" : v;
    }

    private static void applyLinuxX11Env(ProcessBuilder pb) {
        Map<String, String> env = pb.environment();
        copyEnvIfPresent(env, "DISPLAY");
        copyEnvIfPresent(env, "XAUTHORITY");
    }

    private static void copyEnvIfPresent(Map<String, String> env, String key) {
        String v = System.getenv(key);
        if (v != null && !v.isEmpty()) {
            env.put(key, v);
        }
    }

    /**
     * Su i3/X11 conviene xclip anche se WAYLAND_DISPLAY è settato (XWayland / sessioni ibride).
     *
     * @return true se la selection {@code clipboard} è stata impostata con successo
     */
    private static boolean tryLinuxClipboardFromFile(Path pngFile, Logging logging) {
        logLinuxClipboardEnv(logging);
        String display = System.getenv("DISPLAY");
        String wayland = System.getenv("WAYLAND_DISPLAY");

        boolean ok = false;
        if (display != null && !display.isEmpty()) {
            ok = tryXclipFromFile(pngFile, logging);
        }
        if (!ok && wayland != null && !wayland.isEmpty()) {
            ok = tryWlCopyFromFile(pngFile, logging);
        }
        if (!ok) {
            ok = tryXclipFromFile(pngFile, logging);
        }
        return ok;
    }

    private static boolean tryWlCopyFromFile(Path pngFile, Logging logging) {
        String[] bins = {"wl-copy", "/usr/bin/wl-copy", "/bin/wl-copy"};
        for (String bin : bins) {
            try {
                ProcessBuilder pb = new ProcessBuilder(bin, "--type", "image/png");
                pb.redirectInput(pngFile.toFile());
                applyLinuxX11Env(pb);
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

    /**
     * {@code xclip -i} legge da stdin; alcune build si aspettano {@code -i} esplicito.
     * Copia anche su {@code primary} (incolla col tasto centrale su X11).
     */
    private static boolean tryXclipFromFile(Path pngFile, Logging logging) {
        String[] bins = {"/usr/bin/xclip", "/bin/xclip", "xclip"};
        boolean clipboardOk = false;
        for (String bin : bins) {
            if (runXclip(bin, pngFile, "clipboard", logging)) {
                logging.logToOutput("Light screenshot: clipboard immagine via xclip (selection=clipboard) [" + bin + "].");
                clipboardOk = true;
                break;
            }
        }
        if (clipboardOk) {
            for (String bin : bins) {
                if (runXclip(bin, pngFile, "primary", logging)) {
                    logging.logToOutput("Light screenshot: xclip anche selection=primary (middle-click) [" + bin + "].");
                    break;
                }
            }
        }
        return clipboardOk;
    }

    private static boolean runXclip(String bin, Path pngFile, String selection, Logging logging) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    bin,
                    "-i",
                    "-selection", selection,
                    "-t", "image/png");
            pb.redirectInput(pngFile.toFile());
            applyLinuxX11Env(pb);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String out = readStream(p.getInputStream());
            int code = p.waitFor();
            if (code == 0) {
                return true;
            }
            if (!out.isBlank()) {
                logging.logToOutput("Light screenshot: xclip [" + bin + "] selection=" + selection + " exit " + code + ": " + out.trim());
            } else if (code != 0) {
                logging.logToOutput("Light screenshot: xclip [" + bin + "] selection=" + selection + " exit " + code);
            }
        } catch (IOException e) {
            logging.logToOutput("Light screenshot: xclip [" + bin + "] selection=" + selection + ": " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logging.logToError("Light screenshot: xclip interrotto.");
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
