/*
 * RomRaider Open-Source Tuning, Logging and Reflashing
 * Copyright (C) 2006-2026 RomRaider.com
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

package com.romraider.maps;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.Assert.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.BeforeClass;
import org.junit.Test;

public class TableViewTest {

    @BeforeClass
    public static void createSettingsFile() throws Exception {
        final Path settingsDirectory = Paths.get(
                System.getProperty("user.home"), ".RomRaider");
        Files.createDirectories(settingsDirectory);
        final Path settingsFile = settingsDirectory.resolve("settings.xml");
        if (!Files.exists(settingsFile)) {
            Files.writeString(settingsFile, "<settings/>", ISO_8859_1);
        }
    }

    @Test
    public void overlayLogTurnsOffOnAViewThatHidesItsCells() {
        final TableSwitch table = new TableSwitch();
        table.setName("Switch");
        final TableSwitchView view = new TableSwitchView(table);
        table.setTableView(view);
        view.populateTableVisual();

        // the toolbar clears its Overlay Log box when such a table is selected
        view.setOverlayLog(false);

        assertFalse(view.getOverlayLog());
    }
}
