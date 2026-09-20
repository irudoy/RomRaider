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

package com.romraider.logger.external.udp.plugin;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

import com.romraider.editor.ecu.ECUEditor;
import com.romraider.editor.ecu.ECUEditorManager;
import com.romraider.maps.DataCell;
import com.romraider.maps.Rom;
import com.romraider.maps.Table;

/**
 * Table values of the ROM images open in the editor. The images belong to
 * the event dispatch thread, so they are read there at most twice a second,
 * and the Logger thread receives the values of the latest read. A value
 * follows the image in the editor, including edits that are not saved yet.
 */
public final class OpenRomTableValues implements RomTableValues {
    private static final long REFRESH_NANOS = TimeUnit.MILLISECONDS.toNanos(500);
    private final Set<String> tableNames = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean refreshing = new AtomicBoolean();
    private volatile Map<String, Cell> cells = emptyMap();
    private volatile long refreshed = System.nanoTime() - REFRESH_NANOS;

    public void watch(String tableName) {
        final boolean named = tableNames.add(tableName);
        if (!named && System.nanoTime() - refreshed < REFRESH_NANOS) return;
        if (!refreshing.compareAndSet(false, true)) return;
        final Runnable refresh = new Runnable() {
            public void run() {
                try {
                    cells = read(openRoms(), tableNames);
                } finally {
                    refreshed = System.nanoTime();
                    refreshing.set(false);
                }
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            refresh.run();
        } else {
            SwingUtilities.invokeLater(refresh);
        }
    }

    public Cell firstCell(String tableName) {
        watch(tableName);
        return cells.get(tableName);
    }

    /**
     * @param roms the open images, the preferred one first
     * @return the first cell of every named table an image holds
     */
    public static Map<String, Cell> read(List<Rom> roms,
            Collection<String> tableNames) {
        final Map<String, Cell> cells = new HashMap<String, Cell>();
        for (String tableName : tableNames) {
            for (Rom rom : roms) {
                final Cell cell = firstCell(rom, tableName);
                if (cell == null) continue;
                cells.put(tableName, cell);
                break;
            }
        }
        return cells;
    }

    private static Cell firstCell(Rom rom, String tableName) {
        try {
            final Table table = rom.getTableByName(tableName);
            if (table == null || table.isStaticDataTable()) return null;
            final DataCell[] data = table.getData();
            if (data == null || data.length == 0 || data[0] == null) return null;
            return new Cell(data[0].getBinValue(), rom.getFileName());
        } catch (RuntimeException e) {
            // an image that is being opened or closed has no table data
            return null;
        }
    }

    /** The image selected in the editor leads the other open images. */
    private static List<Rom> openRoms() {
        final ECUEditor editor = ECUEditorManager.getECUEditorWithoutCreation();
        if (editor == null) return emptyList();
        final List<Rom> roms = new ArrayList<Rom>();
        final Rom selected = editor.getLastSelectedRom();
        if (selected != null) roms.add(selected);
        for (Rom rom : editor.getImages()) {
            if (rom != selected) roms.add(rom);
        }
        return roms;
    }
}
