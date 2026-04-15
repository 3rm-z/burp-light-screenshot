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
     * Writes PNG, copies to clipboard (AWT on EDT + Linux native tools on caller thread), and logs the path.
     * <p>
     * Run on a <strong>worker thread</strong>, not EDT, so xclip/wl-copy {@code waitFor()} calls
     * do not block Burp UI.
     */
    public static void copyImageToClipboard(Image image, Logging logging) {
        if (image == null) {
            return;
        }
        try {
            BufferedImage buffered = toBufferedImage(image);

            // 1) Always persist PNG to disk (used by xclip/wl-copy and user fallback).
            Path saved = saveTempPng(buffered);
            if (saved != null) {
                logging.logToOutput("Light screenshot: PNG saved: " + saved.toAbsolutePath());
            }

            if (!isLinux()) {
                setAwtClipboardContents(buffered, logging);
            }

            if (isWindows() && saved != null) {
                tryWindowsClipboardFromPngFile(saved, logging);
            }

            if (isLinux() && saved != null) {
                linuxSleepAfterPngWrite();
                boolean nativeOk = tryLinuxClipboardFromFile(saved, logging);
                if (!nativeOk) {
                    logging.logToOutput("Light screenshot: xclip/wl-copy failed; trying AWT fallback.");
                    setAwtClipboardContents(buffered, logging);
                    logging.logToOutput("Light screenshot: PNG path above can be used as manual fallback.");
                }
            }
        } catch (Exception e) {
            logging.logToError("Light screenshot: clipboard copy failed: " + e.getMessage());
        }
    }

    private static void setAwtClipboardContents(BufferedImage buffered, Logging logging) {
        if (SwingUtilities.isEventDispatchThread()) {
            doAwtClipboardSet(buffered, logging);
            return;
        }
        // On Windows this often works from worker threads; invokeAndWait may fail or stall in some setups.
        if (isWindows()) {
            try {
                doAwtClipboardSet(buffered, logging);
                return;
            } catch (Exception e) {
                logging.logToOutput("Light screenshot: AWT from worker failed, retrying on EDT: " + e.getMessage());
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
            logging.logToError("Light screenshot: AWT clipboard interrupted.");
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
        logging.logToOutput("Light screenshot: system clipboard (AWT) updated.");
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    /**
     * Sets image clipboard via PowerShell + WinForms (STA), useful when AWT clipboard does not paste.
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
                logging.logToOutput("Light screenshot: image clipboard set via PowerShell (WinForms).");
            } else {
                logging.logToOutput("Light screenshot: PowerShell clipboard exit " + code
                        + (out.isBlank() ? "" : (": " + out.trim())));
            }
        } catch (IOException e) {
            logging.logToOutput("Light screenshot: PowerShell clipboard not executable: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logging.logToError("Light screenshot: PowerShell clipboard interrupted.");
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
        return v == null || v.isEmpty() ? "(not set)" : v;
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
     * Prefer xclip on X11 even if WAYLAND_DISPLAY is set (XWayland / hybrid sessions).
     *
     * @return true if {@code clipboard} selection was set successfully
     */
    private static boolean tryLinuxClipboardFromFile(Path pngFile, Logging logging) {
        logLinuxClipboardEnv(logging);
        final byte[] pngData;
        try {
            pngData = Files.readAllBytes(pngFile);
        } catch (IOException e) {
            logging.logToOutput("Light screenshot: failed reading PNG for clipboard: " + e.getMessage());
            return false;
        }
        if (pngData.length == 0) {
            return false;
        }

        String display = System.getenv("DISPLAY");
        String wayland = System.getenv("WAYLAND_DISPLAY");

        boolean ok = false;
        if (display != null && !display.isEmpty()) {
            ok = tryXclipFromPngBytes(pngData, logging);
        }
        if (!ok && wayland != null && !wayland.isEmpty()) {
            ok = tryWlCopyFromPngBytes(pngData, logging);
        }
        if (!ok) {
            ok = tryXclipFromPngBytes(pngData, logging);
        }
        return ok;
    }

    private static boolean tryWlCopyFromPngBytes(byte[] pngData, Logging logging) {
        String[] bins = {"wl-copy", "/usr/bin/wl-copy", "/bin/wl-copy"};
        for (String bin : bins) {
            try {
                ProcessBuilder pb = new ProcessBuilder(bin, "--type", "image/png");
                applyLinuxX11Env(pb);
                pb.redirectErrorStream(true);
                Process p = pb.start();
                writeAllAndClose(p.getOutputStream(), pngData);
                int code = p.waitFor();
                String out = readStream(p.getInputStream());
                if (code == 0) {
                    logging.logToOutput("Light screenshot: image clipboard set via wl-copy (Wayland).");
                    return true;
                }
                if (!out.isBlank()) {
                    logging.logToOutput("Light screenshot: wl-copy [" + bin + "] exit " + code + ": " + out.trim());
                }
            } catch (IOException e) {
                logging.logToOutput("Light screenshot: wl-copy not executable (" + bin + "): " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logging.logToError("Light screenshot: wl-copy interrupted.");
                return false;
            }
        }
        return false;
    }

    /**
     * Reads PNG once in memory; each xclip process receives bytes on stdin (avoids
     * {@code redirectInput(File)} issues on repeated invocations / primary).
     */
    private static boolean tryXclipFromPngBytes(byte[] pngData, Logging logging) {
        String[] bins = {"/usr/bin/xclip", "/bin/xclip", "xclip"};
        boolean clipboardOk = false;
        for (String bin : bins) {
            if (runXclip(bin, pngData, "clipboard", logging)) {
                logging.logToOutput("Light screenshot: image clipboard set via xclip (selection=clipboard) [" + bin + "].");
                clipboardOk = true;
                break;
            }
        }
        if (clipboardOk) {
            for (String bin : bins) {
                if (runXclip(bin, pngData, "primary", logging)) {
                    logging.logToOutput("Light screenshot: xclip also set selection=primary (middle-click) [" + bin + "].");
                    break;
                }
            }
        }
        return clipboardOk;
    }

    private static void writeAllAndClose(OutputStream os, byte[] data) throws IOException {
        try (OutputStream out = os) {
            out.write(data);
            out.flush();
        }
    }

    private static boolean runXclip(String bin, byte[] pngData, String selection, Logging logging) {
        if (runXclipVariant(bin, pngData, selection, logging, false)) {
            return true;
        }
        if (runXclipVariant(bin, pngData, selection, logging, true)) {
            return true;
        }
        return runXclipVariantOrder2(bin, pngData, selection, logging);
    }

    private static boolean runXclipVariant(String bin, byte[] pngData, String selection, Logging logging, boolean dashI) {
        try {
            ProcessBuilder pb = dashI
                    ? new ProcessBuilder(bin, "-i", "-selection", selection, "-t", "image/png")
                    : new ProcessBuilder(bin, "-selection", selection, "-t", "image/png");
            applyLinuxX11Env(pb);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            writeAllAndClose(p.getOutputStream(), pngData);
            int code = p.waitFor();
            String out = readStream(p.getInputStream());
            if (code == 0) {
                return true;
            }
            logXclipFail(bin, selection, code, out, logging, dashI ? "with -i" : "without -i");
        } catch (IOException e) {
            logging.logToOutput("Light screenshot: xclip [" + bin + "] " + selection + ": " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logging.logToError("Light screenshot: xclip interrupted.");
        }
        return false;
    }

    private static boolean runXclipVariantOrder2(String bin, byte[] pngData, String selection, Logging logging) {
        try {
            ProcessBuilder pb = new ProcessBuilder(bin, "-t", "image/png", "-selection", selection);
            applyLinuxX11Env(pb);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            writeAllAndClose(p.getOutputStream(), pngData);
            int code = p.waitFor();
            String out = readStream(p.getInputStream());
            if (code == 0) {
                return true;
            }
            logXclipFail(bin, selection, code, out, logging, "order with -t first");
        } catch (IOException e) {
            logging.logToOutput("Light screenshot: xclip order2 [" + bin + "]: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logging.logToError("Light screenshot: xclip interrupted.");
        }
        return false;
    }

    private static void logXclipFail(String bin, String selection, int code, String out, Logging logging, String variant) {
        if (!out.isBlank()) {
            logging.logToOutput("Light screenshot: xclip [" + bin + "] " + variant + " selection=" + selection + " exit " + code + ": " + out.trim());
        } else {
            logging.logToOutput("Light screenshot: xclip [" + bin + "] " + variant + " selection=" + selection + " exit " + code);
        }
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
