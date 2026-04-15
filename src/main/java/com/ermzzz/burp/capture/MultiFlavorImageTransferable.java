package com.ermzzz.burp.capture;

import javax.imageio.ImageIO;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Clipboard Transferable exposing both {@link DataFlavor#imageFlavor} and PNG (MIME image/png).
 * Useful on Linux/X11 where some applications read only one of them.
 */
public class MultiFlavorImageTransferable implements Transferable {

    private static final DataFlavor[] FLAVORS;

    static {
        DataFlavor png;
        try {
            png = new DataFlavor("image/png", "PNG");
        } catch (Exception e) {
            png = null;
        }
        if (png != null) {
            FLAVORS = new DataFlavor[]{DataFlavor.imageFlavor, png};
        } else {
            FLAVORS = new DataFlavor[]{DataFlavor.imageFlavor};
        }
    }

    private final BufferedImage image;
    private byte[] pngBytes;

    public MultiFlavorImageTransferable(BufferedImage image) {
        this.image = image;
    }

    private byte[] pngBytes() throws IOException {
        if (pngBytes == null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            pngBytes = baos.toByteArray();
        }
        return pngBytes;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return FLAVORS.clone();
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        for (DataFlavor f : FLAVORS) {
            if (f.equals(flavor)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (DataFlavor.imageFlavor.equals(flavor)) {
            return image;
        }
        if (FLAVORS.length > 1 && FLAVORS[1].equals(flavor)) {
            return new ByteArrayInputStream(pngBytes());
        }
        throw new UnsupportedFlavorException(flavor);
    }
}
