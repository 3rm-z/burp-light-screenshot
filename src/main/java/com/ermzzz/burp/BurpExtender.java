package com.ermzzz.burp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.menu.Menu;
import burp.api.montoya.ui.menu.MenuItem;

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

        Menu mainMenu = Menu.menu("Light Screenshot")
                .addItem(MenuItem.menuItem("Select region -> Clipboard", this::handleLightScreenshot));
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
                LightThemeCapture.captureRegionToClipboard(burpFrame, rectangle, api.logging());
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

