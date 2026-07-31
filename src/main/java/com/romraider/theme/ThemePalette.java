/*
 * RomRaider Open-Source Tuning, Logging and Reflashing
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 */

package com.romraider.theme;

import java.awt.Color;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;

/**
 * Resolves application-specific surfaces that are not standard Swing roles.
 */
public final class ThemePalette {
    private static final String DARK_LOOK_AND_FEEL_ID =
            "RomRaiderHDDark";
    private static final Color DARK_DESKTOP_FALLBACK =
            new Color(31, 38, 42);

    private ThemePalette() {
        throw new UnsupportedOperationException();
    }

    /**
     * Keeps the editor desktop distinct from map panels in both variants.
     */
    public static Color editorDesktopBackground() {
        LookAndFeel lookAndFeel = UIManager.getLookAndFeel();
        if (lookAndFeel != null
                && DARK_LOOK_AND_FEEL_ID.equals(lookAndFeel.getID())) {
            Color background =
                    UIManager.getColor("DesktopPane.background");
            return background == null
                    ? DARK_DESKTOP_FALLBACK
                    : background;
        }
        return Color.WHITE;
    }
}
