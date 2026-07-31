/*
 * RomRaider Open-Source Tuning, Logging and Reflashing
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 */

package com.romraider.theme;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

/**
 * Verifies and records the Retina scale managed by the macOS AWT toolkit.
 */
public final class MacHiDpiBootstrap {
    private static final String SCALE_PROPERTY =
            "romraider.hidpi.scales";
    private static final int MINIMUM_JAVA_VERSION = 17;

    private static boolean initialized;

    private MacHiDpiBootstrap() {
        throw new UnsupportedOperationException();
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        if (runtimeMajorVersion() < MINIMUM_JAVA_VERSION) {
            throw new IllegalStateException(
                    "RomRaiderHD requires Java "
                            + MINIMUM_JAVA_VERSION + " or newer");
        }

        GraphicsEnvironment environment =
                GraphicsEnvironment.getLocalGraphicsEnvironment();
        StringBuilder scales = new StringBuilder();
        for (GraphicsDevice device : environment.getScreenDevices()) {
            if (scales.length() > 0) {
                scales.append(',');
            }
            double scale = device.getDefaultConfiguration()
                    .getDefaultTransform().getScaleX();
            scales.append(device.getIDstring())
                    .append('=')
                    .append((int) Math.round(scale));
        }
        System.setProperty(SCALE_PROPERTY, scales.toString());
        initialized = true;
    }

    private static int runtimeMajorVersion() {
        String version = System.getProperty(
                "java.specification.version", "0");
        if (version.startsWith("1.")) {
            version = version.substring(2);
        }
        int separator = version.indexOf('.');
        if (separator >= 0) {
            version = version.substring(0, separator);
        }
        try {
            return Integer.parseInt(version);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }
}
