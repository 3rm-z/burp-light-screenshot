package com.ermzzz.burp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.menu.Menu;
import burp.api.montoya.ui.menu.MenuItem;
import burp.api.montoya.ui.menu.BasicMenuItem;

import com.ermzzz.burp.config.SelectionAppearance;
import com.ermzzz.burp.ui.RegionSelectorOverlay;
import com.ermzzz.burp.capture.LightThemeCapture;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class BurpExtender implements BurpExtension {

    private MontoyaApi api;

    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;

        api.extension().setName("Light Screenshot Helper");

        api.logging().logToOutput("Light Screenshot Helper loaded");
        if (System.getProperty(SelectionAppearance.PROPERTY_SELECTION_COLOR) != null) {
            api.logging().logToOutput("Light Screenshot: bordo selezione = "
                    + SelectionAppearance.selectionBorderColor() + " (-D" + SelectionAppearance.PROPERTY_SELECTION_COLOR + ")");
        }

        BasicMenuItem filtered = BasicMenuItem
                .basicMenuItem("Select region -> Clipboard (report / chiaro)")
                .withAction(() -> handleLightScreenshot(true));
        BasicMenuItem original = BasicMenuItem
                .basicMenuItem("Select region -> Clipboard (original colors)")
                .withAction(() -> handleLightScreenshot(false));
        MenuItem[] borderItems = createBorderColorItems();
        Menu mainMenu = Menu.menu("Light Screenshot")
                .withMenuItems(combineMenuItems(filtered, original, borderItems));

        api.userInterface().menuBar().registerMenu(mainMenu);
    }

    private MenuItem[] createBorderColorItems() {
        List<MenuItem> items = new ArrayList<>();
        for (SelectionAppearance.Preset p : SelectionAppearance.PRESETS) {
            items.add(BasicMenuItem.basicMenuItem("[Border] " + p.label()).withAction(() -> {
                SelectionAppearance.setSelectionBorderColor(p.color());
                api.logging().logToOutput("Light screenshot: bordo selezione impostato a " + p.label());
            }));
        }
        items.add(BasicMenuItem.basicMenuItem("[Border] Reset to JVM property/default").withAction(() -> {
            SelectionAppearance.resetToPropertyOrDefault();
            api.logging().logToOutput("Light screenshot: bordo selezione ripristinato (property/default).");
        }));
        return items.toArray(new MenuItem[0]);
    }

    private MenuItem[] combineMenuItems(MenuItem first, MenuItem second, MenuItem[] others) {
        MenuItem[] all = new MenuItem[2 + others.length];
        all[0] = first;
        all[1] = second;
        System.arraycopy(others, 0, all, 2, others.length);
        return all;
    }

    private void handleLightScreenshot(boolean applyFilter) {
        EventQueue.invokeLater(() -> {
            Frame burpFrame = locateBurpFrame();
            if (burpFrame == null) {
                JOptionPane.showMessageDialog(null,
                        "Impossibile trovare la finestra principale di Burp.",
                        "Light Screenshot Helper",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!(burpFrame instanceof JFrame)) {
                JOptionPane.showMessageDialog(burpFrame,
                        "La finestra di Burp non è un JFrame: la selezione regione (glass pane) non è disponibile.",
                        "Light Screenshot Helper",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            RegionSelectorOverlay.selectRegion(burpFrame, rectangle -> {
                if (rectangle == null || rectangle.width <= 0 || rectangle.height <= 0) {
                    api.logging().logToOutput("Light screenshot: selezione annullata o vuota.");
                    return;
                }
                api.logging().logToOutput("Light screenshot: regione selezionata " + rectangle);
                // Robot + filtro + xclip/wl-copy NON vanno sull’EDT: bloccherebbero Burp e il repaint del glass pane.
                new Thread(() -> LightThemeCapture.captureRegionToClipboard(burpFrame, rectangle, api.logging(), applyFilter),
                        "burp-light-screenshot").start();
            });
        });
    }

    private Frame locateBurpFrame() {
        Frame[] frames = Frame.getFrames();
        Optional<Frame> visible = Arrays.stream(frames)
                .filter(f -> f.isVisible()
                        && (f instanceof JFrame)
                        && (f.getTitle() != null && f.getTitle().toLowerCase().contains("burp")))
                .findFirst();
        if (visible.isPresent()) {
            return visible.get();
        }

        // fallback: frame attivo
        for (Frame f : frames) {
            if (f.isActive()) {
                return f;
            }
        }
        return null;
    }
}

