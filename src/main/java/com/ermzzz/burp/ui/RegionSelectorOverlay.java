package com.ermzzz.burp.ui;

import com.ermzzz.burp.config.SelectionAppearance;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Region selection overlay using {@link JRootPane#setGlassPane(Component)}
 * on the main Burp frame. Avoids {@link JWindow}, which can remain opaque/black
 * on some Linux/X11 setups and hide the whole window.
 */
public final class RegionSelectorOverlay {

    private RegionSelectorOverlay() {
    }

    public static void selectRegion(Frame burpFrame, Consumer<Rectangle> callback) {
        EventQueue.invokeLater(() -> {
            if (!(burpFrame instanceof JFrame)) {
                callback.accept(null);
                return;
            }
            JFrame jf = (JFrame) burpFrame;
            JRootPane root = jf.getRootPane();
            Component previousGlass = root.getGlassPane();

            AtomicBoolean finished = new AtomicBoolean(false);

            JPanel glass = new JPanel(null) {
                private Point start;
                private final Rectangle selection = new Rectangle();

                {
                    setOpaque(false);
                    setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                    setFocusable(true);

                    MouseAdapter ma = new MouseAdapter() {
                        @Override
                        public void mousePressed(MouseEvent e) {
                            start = e.getPoint();
                            selection.setBounds(start.x, start.y, 0, 0);
                            repaint();
                        }

                        @Override
                        public void mouseDragged(MouseEvent e) {
                            if (start == null) {
                                return;
                            }
                            int x = Math.min(start.x, e.getX());
                            int y = Math.min(start.y, e.getY());
                            int w = Math.abs(e.getX() - start.x);
                            int h = Math.abs(e.getY() - start.y);
                            selection.setBounds(x, y, w, h);
                            repaint();
                        }

                        @Override
                        public void mouseReleased(MouseEvent e) {
                            Rectangle screenRect = null;
                            if (selection.width > 0 && selection.height > 0) {
                                Point loc = getLocationOnScreen();
                                screenRect = new Rectangle(
                                        loc.x + selection.x,
                                        loc.y + selection.y,
                                        selection.width,
                                        selection.height
                                );
                            }
                            finish(root, previousGlass, callback, screenRect, finished);
                        }
                    };
                    addMouseListener(ma);
                    addMouseMotionListener(ma);

                    addKeyListener(new KeyAdapter() {
                        @Override
                        public void keyPressed(KeyEvent e) {
                            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                                finish(root, previousGlass, callback, null, finished);
                            }
                        }
                    });
                }

                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    if (selection.width > 0 && selection.height > 0) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        try {
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g2.setColor(SelectionAppearance.selectionBorderColor());
                            g2.setStroke(new BasicStroke(2f));
                            g2.drawRect(selection.x, selection.y, selection.width - 1, selection.height - 1);
                        } finally {
                            g2.dispose();
                        }
                    }
                }
            };
            glass.setOpaque(false);
            root.setGlassPane(glass);
            glass.setVisible(true);
            glass.requestFocusInWindow();
            root.revalidate();
            root.repaint();
        });
    }

    /**
     * Restores the original glass pane and invokes callback. Must run on EDT.
     */
    private static void finish(JRootPane root, Component previousGlass, Consumer<Rectangle> callback,
                               Rectangle screenRect, AtomicBoolean finished) {
        if (!finished.compareAndSet(false, true)) {
            return;
        }
        try {
            root.setGlassPane(previousGlass);
            previousGlass.setVisible(false);
            root.revalidate();
            root.repaint();
        } finally {
            callback.accept(screenRect);
        }
    }
}
