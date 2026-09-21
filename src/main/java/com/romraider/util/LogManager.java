/*
 * RomRaider Open-Source Tuning, Logging and Reflashing
 * Copyright (C) 2006-2012 RomRaider.com
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.romraider.util;

import static org.apache.log4j.PropertyConfigurator.configureAndWatch;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import org.apache.log4j.Logger;

public final class LogManager {

    /**
     * Set to true by launchers that start the JVM without a console, such as
     * the Windows installer shortcuts running javaw.exe, so that the console
     * output of the application still reaches romraider_sout.log.
     */
    public static final String CONSOLE_LOG_PROPERTY = "romraider.consoleLog";

    private static final String CONSOLE_LOG_FILE = "romraider_sout.log";

    private LogManager() {
        throw new UnsupportedOperationException();
    }

    public static void initDebugLogging() {
        // The log4j console appender binds to System.out while it is
        // configured, so the console is redirected first.
        if (Boolean.getBoolean(CONSOLE_LOG_PROPERTY)) {
            redirectConsole();
        }
        configureAndWatch("lib/log4j.properties");
        Thread.setDefaultUncaughtExceptionHandler((thread, error) ->
                Logger.getLogger(LogManager.class).error(
                        "Uncaught exception in thread " + thread.getName(),
                        error));
    }

    private static void redirectConsole() {
        File directory = new File(System.getProperty("user.home"), ".RomRaider");
        if (!directory.isDirectory() && !directory.mkdirs()) {
            return;
        }
        try {
            PrintStream console = new PrintStream(new FileOutputStream(
                    new File(directory, CONSOLE_LOG_FILE), true), true);
            System.setOut(console);
            System.setErr(console);
        } catch (FileNotFoundException e) {
            System.err.println("Unable to write " + CONSOLE_LOG_FILE + ": " + e);
        }
    }
}
