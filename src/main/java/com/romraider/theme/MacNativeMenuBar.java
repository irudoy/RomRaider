/*
 * RomRaider Open-Source Tuning, Logging and Reflashing
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 */

package com.romraider.theme;

import javax.swing.BorderFactory;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.UIManager;
import javax.swing.plaf.MenuBarUI;
import javax.swing.plaf.synth.SynthLookAndFeel;

/**
 * Moves only a top-level frame menu into the native macOS screen menu.
 *
 * Installing AquaMenuBarUI globally also affects JMenuBar instances inside
 * JInternalFrame map windows. Aqua cannot paint those bars while Nimbus is
 * active and aborts Swing repaint processing. Top-level frames opt in
 * explicitly so internal map menus retain their Nimbus delegate.
 */
public final class MacNativeMenuBar {
    private static final String AQUA_MENU_BAR_UI =
            "com.apple.laf.AquaMenuBarUI";

    private MacNativeMenuBar() {
        throw new UnsupportedOperationException();
    }

    public static void install(JMenuBar menuBar) {
        if (!System.getProperty("os.name", "").startsWith("Mac")
                || !isScreenMenuEnabled()) {
            return;
        }

        installAquaPainterFallbacks();
        if (AQUA_MENU_BAR_UI.equals(
                menuBar.getUI().getClass().getName())) {
            hideMnemonicUnderlines(menuBar);
            return;
        }

        try {
            Class<?> uiClass = Class.forName(AQUA_MENU_BAR_UI);
            menuBar.setUI((MenuBarUI) uiClass
                    .getDeclaredConstructor()
                    .newInstance());
            hideMnemonicUnderlines(menuBar);
        } catch (ReflectiveOperationException | LinkageError error) {
            System.err.println(
                    "Native macOS menu bar is unavailable: " + error);
        }
    }

    /**
     * Keeps a menu embedded in a map window inside the active Synth theme.
     *
     * The macOS screen-menu property assigns AquaMenuBarUI to every newly
     * created JMenuBar. That delegate is valid for top-level frame menus, but
     * it cannot paint a menu embedded in a dark Nimbus JInternalFrame.
     */
    public static void installInWindow(JMenuBar menuBar) {
        if (!System.getProperty("os.name", "").startsWith("Mac")
                || !(UIManager.getLookAndFeel()
                        instanceof SynthLookAndFeel)) {
            return;
        }

        menuBar.setUI((MenuBarUI) SynthLookAndFeel.createUI(menuBar));
    }

    private static void installAquaPainterFallbacks() {
        installPainterFallback("MenuBar.backgroundPainter");
        installPainterFallback("MenuBar.selectedBackgroundPainter");
        installPainterFallback("MenuItem.selectedBackgroundPainter");
    }

    private static void installPainterFallback(String key) {
        if (UIManager.getBorder(key) == null) {
            UIManager.put(key, BorderFactory.createEmptyBorder());
        }
    }

    private static void hideMnemonicUnderlines(JMenuBar menuBar) {
        for (int index = 0; index < menuBar.getMenuCount(); index++) {
            JMenu menu = menuBar.getMenu(index);
            if (menu != null) {
                menu.setDisplayedMnemonicIndex(-1);
            }
        }
    }

    private static boolean isScreenMenuEnabled() {
        return Boolean.getBoolean("apple.laf.useScreenMenuBar")
                || Boolean.getBoolean(
                        "com.apple.macos.useScreenMenuBar");
    }
}
