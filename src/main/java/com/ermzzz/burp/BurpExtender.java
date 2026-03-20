package com.ermzzz.burp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.menu.Menu;
import burp.api.montoya.ui.menu.MenuItem;
import burp.api.montoya.ui.menu.BasicMenuItem;

import com.ermzzz.burp.ui.RegionSelectorOverlay;
import com.ermzzz.burp.capture.LightThemeCapture;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Optional;

public class BurpExtender implements BurpExtension {

    private MontoyaApi api;

    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;

        api.extension().setName("Light Screenshot Helper");

        api.logging().logToOutput("Light Screenshot Helper loaded");

        BasicMenuItem item = BasicMenuItem.basicMenuItem("Select region -> Clipboard")
                .withAction(this::handleLightScreenshot);

        Menu mainMenu = Menu.menu("Light Screenshot")
                .withMenuItems(item);

        api.userInterface().menuBar().registerMenu(mainMenu);
    }

    private void handleLightScreenshot() {
        EventQueue.invokeLater(() -> {
            Frame burpFrame = locateBurpFrame();
            if (burpFrame == null) {
                JOptionPane.showMessageDialog(null,
                        "Impossibile trovare la finestra principale di Burp.",
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
                // Non blocchiamo l'EDT: l'overlay deve sparire subito, mentre lo screenshot/cambio tema può richiedere tempo.
                Thread t = new Thread(() ->
                        LightThemeCapture.captureRegionToClipboard(burpFrame, rectangle, api.logging()),
                        "light-theme-capture");
                t.setDaemon(true);
                t.start();
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

