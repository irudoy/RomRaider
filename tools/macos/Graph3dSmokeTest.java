/*
 * RomRaider Open-Source Tuning, Logging and Reflashing
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 */

package com.romraider.build;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.UIManager;

import com.ecm.graphics.Graph3dFrameManager;
import com.ecm.graphics.Graph3dJPanel;
import com.romraider.Settings;
import com.romraider.maps.Table2D;
import com.romraider.maps.Table2DView;
import com.romraider.swing.TableFrame;
import com.romraider.theme.DarkNimbusLookAndFeel;
import com.romraider.theme.MacNativeMenuBar;
import com.romraider.theme.RomRaiderBootstrap;
import com.romraider.util.SettingsManager;

/**
 * Opens the same native Java3D window used by the RomRaider table toolbar.
 */
public final class Graph3dSmokeTest {
    private Graph3dSmokeTest() {
        throw new UnsupportedOperationException();
    }

    public static void main(String[] arguments) {
        try {
            RomRaiderBootstrap.prepareMacLookAndFeel();
            UIManager.setLookAndFeel(new DarkNimbusLookAndFeel());
            verifyNativeScreenMenu();
            verifyInWindowMenu();
            verifyRealMapMenu();

            Vector<float[]> values = new Vector<float[]>();
            values.add(new float[] {1.0f, 2.0f, 3.0f});
            values.add(new float[] {2.0f, 3.0f, 4.0f});
            values.add(new float[] {3.0f, 4.0f, 5.0f});
            double[] xValues = {1000.0, 2000.0, 3000.0};
            double[] yValues = {0.5, 1.0, 1.5};

            EventQueue.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    Graph3dFrameManager.openGraph3dFrame(
                            values,
                            1.0,
                            5.0,
                            xValues,
                            yValues,
                            "RPM",
                            "Value",
                            "Load",
                            "RomRaiderHD 3D smoke test");
                }
            });

            Field panelField = Graph3dFrameManager.class.getDeclaredField(
                    "graph3dJPanel");
            panelField.setAccessible(true);
            Graph3dJPanel panel = (Graph3dJPanel) panelField.get(null);
            String canvasType = panel.getClass()
                    .getDeclaredField("canvas3d")
                    .getType()
                    .getName();
            if (!"org.jogamp.java3d.Canvas3D".equals(canvasType)) {
                throw new AssertionError(
                        "Unexpected Java3D canvas type: " + canvasType);
            }

            Field frameField = Graph3dFrameManager.class.getDeclaredField(
                    "graph3dJFrame");
            frameField.setAccessible(true);
            final Frame frame = (Frame) frameField.get(null);
            if (frame == null || !frame.isDisplayable() || !frame.isVisible()) {
                throw new AssertionError(
                        "RomRaider 3D window did not become visible");
            }
            EventQueue.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    Graph3dFrameManager.closeGraph3dFrame();
                    frame.dispose();
                }
            });
            panel.shutdownCleanup();
            System.out.println("RomRaiderHD Java3D smoke test passed");
            System.exit(0);
        } catch (Throwable error) {
            error.printStackTrace();
            System.exit(1);
        }
    }

    private static void verifyNativeScreenMenu() throws Exception {
        EventQueue.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame("Native menu smoke test");
                JMenuBar menuBar = new JMenuBar();
                JMenu fileMenu = new JMenu("File");
                fileMenu.setMnemonic('F');
                menuBar.add(fileMenu);
                menuBar.add(new JMenu("Edit"));
                frame.setJMenuBar(menuBar);
                MacNativeMenuBar.install(menuBar);
                frame.setSize(400, 240);
                frame.setVisible(true);
                menuBar.getPreferredSize();

                String uiClass = menuBar.getUI().getClass().getName();
                if (!"com.apple.laf.AquaMenuBarUI".equals(uiClass)) {
                    throw new AssertionError(
                            "Unexpected menu UI delegate: " + uiClass);
                }
                if (fileMenu.getDisplayedMnemonicIndex() != -1) {
                    throw new AssertionError(
                            "Native menu retains a mnemonic underline");
                }
                String nativeMenuClass = frame.getMenuBar() == null
                        ? "null"
                        : frame.getMenuBar().getClass().getName();
                if (!"com.apple.laf.ScreenMenuBar".equals(
                        nativeMenuClass)) {
                    throw new AssertionError(
                            "Swing menu did not move to the macOS screen menu: "
                                    + nativeMenuClass);
                }
                BufferedImage image = new BufferedImage(
                        400, 24, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = image.createGraphics();
                menuBar.getUI().paint(graphics, menuBar);
                graphics.dispose();
                frame.dispose();
            }
        });
    }

    private static void verifyInWindowMenu() throws Exception {
        EventQueue.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                JInternalFrame frame = new JInternalFrame(
                        "Map menu smoke test", true, true, true, true);
                JMenuBar menuBar = new JMenuBar();
                menuBar.add(new JMenu("Table"));
                menuBar.add(new JMenu("Edit"));
                frame.setJMenuBar(menuBar);
                MacNativeMenuBar.installInWindow(menuBar);
                frame.setSize(400, 240);
                frame.setVisible(true);

                String uiClass = menuBar.getUI().getClass().getName();
                if ("com.apple.laf.AquaMenuBarUI".equals(uiClass)) {
                    throw new AssertionError(
                            "Aqua menu UI leaked into a map window");
                }

                BufferedImage image = new BufferedImage(
                        400, 240, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = image.createGraphics();
                frame.paint(graphics);
                graphics.dispose();
                frame.dispose();
            }
        });
    }

    private static void verifyRealMapMenu() throws Exception {
        useMinimalSettings();
        Table2D table = new Table2D();
        table.setName("Map menu smoke test");
        Table2DView view = new Table2DView(table);

        EventQueue.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                TableFrame frame = new TableFrame(
                        "Map menu smoke test", view);
                frame.setSize(400, 240);
                frame.setVisible(true);
                String uiClass =
                        frame.getJMenuBar().getUI().getClass().getName();
                if ("com.apple.laf.AquaMenuBarUI".equals(uiClass)) {
                    throw new AssertionError(
                            "Aqua menu UI leaked into a real map frame");
                }

                BufferedImage image = new BufferedImage(
                        400, 240, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = image.createGraphics();
                frame.paint(graphics);
                graphics.dispose();
                frame.dispose();
            }
        });
    }

    private static void useMinimalSettings() throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Object unsafe = unsafeField.get(null);
        Method allocateInstance =
                unsafeClass.getMethod("allocateInstance", Class.class);
        Settings settings =
                (Settings) allocateInstance.invoke(unsafe, Settings.class);
        settings.setCellSize(new Dimension(42, 18));

        Field field = SettingsManager.class.getDeclaredField("settings");
        field.setAccessible(true);
        field.set(null, settings);
    }
}
