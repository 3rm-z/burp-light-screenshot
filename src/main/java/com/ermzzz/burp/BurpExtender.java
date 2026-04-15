package com.ermzzz.burp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.Registration;

import com.ermzzz.burp.config.SelectionAppearance;
import com.ermzzz.burp.ui.RegionSelectorOverlay;
import com.ermzzz.burp.capture.LightThemeCapture;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Optional;

public class BurpExtender implements BurpExtension {

    private MontoyaApi api;
    private Registration menuRegistration;
    private volatile boolean applyFilter = true;

    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;

        api.extension().setName("Light Screenshot Helper");

        api.logging().logToOutput("Light Screenshot Helper loaded");
        if (System.getProperty(SelectionAppearance.PROPERTY_SELECTION_COLOR) != null) {
            api.logging().logToOutput("Light Screenshot: bordo selezione = "
                    + SelectionAppearance.selectionBorderColor() + " (-D" + SelectionAppearance.PROPERTY_SELECTION_COLOR + ")");
        }

        JMenu menu = buildSwingMenu();
        menuRegistration = api.userInterface().menuBar().registerMenu(menu);
    }

    private JMenu buildSwingMenu() {
        JMenu root = new JMenu("Light Screenshot");

        JMenuItem takeScreenshot = new JMenuItem("Take screenshot -> Clipboard");
        takeScreenshot.addActionListener(e -> handleLightScreenshot(applyFilter));
        root.add(takeScreenshot);

        JMenu colorModeMenu = new JMenu("Color mode");
        ButtonGroup colorModeGroup = new ButtonGroup();
        JRadioButtonMenuItem reportLight = new JRadioButtonMenuItem("Report / chiaro", true);
        reportLight.addActionListener(e -> {
            applyFilter = true;
            api.logging().logToOutput("Light screenshot: color mode = report / chiaro");
        });
        JRadioButtonMenuItem original = new JRadioButtonMenuItem("Original colors", false);
        original.addActionListener(e -> {
            applyFilter = false;
            api.logging().logToOutput("Light screenshot: color mode = original colors");
        });
        colorModeGroup.add(reportLight);
        colorModeGroup.add(original);
        colorModeMenu.add(reportLight);
        colorModeMenu.add(original);
        root.add(colorModeMenu);

        JMenu borderColorMenu = new JMenu("Border color");
        ButtonGroup borderGroup = new ButtonGroup();
        for (SelectionAppearance.Preset p : SelectionAppearance.PRESETS) {
            boolean selected = SelectionAppearance.selectionBorderColor().equals(p.color());
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(p.label(), selected);
            item.addActionListener(e -> {
                SelectionAppearance.setSelectionBorderColor(p.color());
                api.logging().logToOutput("Light screenshot: bordo selezione impostato a " + p.label());
            });
            borderGroup.add(item);
            borderColorMenu.add(item);
        }
        JMenuItem reset = new JMenuItem("Reset");
        reset.addActionListener(e -> {
            SelectionAppearance.resetToPropertyOrDefault();
            api.logging().logToOutput("Light screenshot: bordo selezione ripristinato (property/default).");
            if (menuRegistration != null) {
                menuRegistration.deregister();
            }
            menuRegistration = api.userInterface().menuBar().registerMenu(buildSwingMenu());
        });
        borderColorMenu.addSeparator();
        borderColorMenu.add(reset);
        root.add(borderColorMenu);

        return root;
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

