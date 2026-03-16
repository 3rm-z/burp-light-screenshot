package com.ermzzz.burp.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

public class RegionSelectorOverlay extends JWindow {

    private Point start;
    private Rectangle selection;
    private final Consumer<Rectangle> callback;

    private RegionSelectorOverlay(Window owner, Consumer<Rectangle> callback) {
        super(owner);
        this.callback = callback;
        setAlwaysOnTop(true);
        setBackground(new Color(0, 0, 0, 32));

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                start = e.getPoint();
                selection = new Rectangle(start);
                repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (start == null) {
                    return;
                }
                Point end = e.getPoint();
                int x = Math.min(start.x, end.x);
                int y = Math.min(start.y, end.y);
                int w = Math.abs(start.x - end.x);
                int h = Math.abs(start.y - end.y);
                selection.setBounds(x, y, w, h);
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                Rectangle result = null;
                if (selection != null && selection.width > 0 && selection.height > 0) {
                    // convert da coordinate overlay a coordinate schermo
                    Point locOnScreen = getLocationOnScreen();
                    result = new Rectangle(
                            locOnScreen.x + selection.x,
                            locOnScreen.y + selection.y,
                            selection.width,
                            selection.height
                    );
                }
                dispose();
                callback.accept(result);
            }
        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (selection != null) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(new Color(0, 0, 0, 80));
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setComposite(AlphaComposite.Clear);
            g2.fillRect(selection.x, selection.y, selection.width, selection.height);

            g2.setComposite(AlphaComposite.SrcOver);
            g2.setColor(Color.RED);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRect(selection.x, selection.y, selection.width, selection.height);

            g2.dispose();
        }
    }

    public static void selectRegion(Frame burpFrame, Consumer<Rectangle> callback) {
        EventQueue.invokeLater(() -> {
            Window owner = burpFrame;
            RegionSelectorOverlay overlay = new RegionSelectorOverlay(owner, callback);
            Rectangle bounds = burpFrame.getBounds();
            Point loc = burpFrame.getLocationOnScreen();
            overlay.setBounds(loc.x, loc.y, bounds.width, bounds.height);
            overlay.setVisible(true);
        });
    }
}

