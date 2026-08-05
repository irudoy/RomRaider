/*
 * RomRaider Open-Source Tuning, Logging and Reflashing
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 */

package com.romraider.theme;

import java.awt.Desktop;
import java.awt.desktop.QuitResponse;

import javax.swing.JInternalFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.romraider.ECUExec;
import com.romraider.editor.ecu.ECUEditor;
import com.romraider.editor.ecu.ECUEditorManager;
import com.romraider.logger.ecu.EcuLogger;

/**
 * Applies macOS runtime integration before RomRaider creates an AWT window.
 */
public final class RomRaiderBootstrap {
    static final String AQUA_INTERNAL_FRAMES_PROPERTY =
            "romraider.macos.aquaInternalFrames";
    private static final String AQUA_LOOK_AND_FEEL =
            "com.apple.laf.AquaLookAndFeel";

    private RomRaiderBootstrap() {
        throw new UnsupportedOperationException();
    }

    public static void main(String[] arguments) {
        MacHiDpiBootstrap.initialize();
        prepareMacLookAndFeel();
        installMacQuitHandler();
        ECUExec.main(arguments);
    }

    private static void installMacQuitHandler() {
        if (!System.getProperty("os.name", "").startsWith("Mac")
                || !Desktop.isDesktopSupported()) {
            return;
        }

        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.APP_QUIT_HANDLER)) {
            return;
        }

        desktop.setQuitHandler((event, response) -> {
            Runnable quitTask = () -> quitApplication(response);
            if (SwingUtilities.isEventDispatchThread()) {
                quitTask.run();
            } else {
                SwingUtilities.invokeLater(quitTask);
            }
        });
    }

    private static void quitApplication(QuitResponse response) {
        try {
            ECUEditor editor =
                    ECUEditorManager.getECUEditorWithoutCreation();
            EcuLogger logger = EcuLogger.getEcuLoggerWithoutCreation();

            if (editor != null) {
                editor.handleExit();
            }
            if (logger != null) {
                logger.handleExit();
            }
            response.performQuit();
        } catch (RuntimeException error) {
            System.err.println(
                    "Unable to save application state before quitting: "
                            + error);
            response.cancelQuit();
        }
    }

    /**
     * Loads Aqua before a custom system look and feel replaces it.
     *
     * This keeps the native macOS internal-frame delegate available after
     * dark Nimbus becomes the application look and feel.
     */
    public static void prepareMacLookAndFeel() {
        boolean aquaInternalFramesAvailable = false;
        if (System.getProperty("os.name", "").startsWith("Mac")) {
            try {
                UIManager.setLookAndFeel(AQUA_LOOK_AND_FEEL);
                JInternalFrame probe = new JInternalFrame();
                aquaInternalFramesAvailable =
                        "com.apple.laf.AquaInternalFrameUI".equals(
                                probe.getUI().getClass().getName());
            } catch (Exception | LinkageError error) {
                System.err.println(
                        "Aqua internal frames are unavailable: " + error);
            }
        }
        System.setProperty(
                AQUA_INTERNAL_FRAMES_PROPERTY,
                Boolean.toString(aquaInternalFramesAvailable));
    }
}
