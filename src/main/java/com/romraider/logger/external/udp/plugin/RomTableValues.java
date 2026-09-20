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

/**
 * Source of the ROM table values a derived data item reads.
 */
public interface RomTableValues {

    /** Stored value of one table cell and the ROM image it comes from. */
    final class Cell {
        private final double storedValue;
        private final String romFileName;

        public Cell(double storedValue, String romFileName) {
            this.storedValue = storedValue;
            this.romFileName = romFileName;
        }

        /** @return the value as the ROM stores it, before the table scaling */
        public double getStoredValue() {
            return storedValue;
        }

        public String getRomFileName() {
            return romFileName;
        }
    }

    /**
     * Names a table before its first read, so that the value is ready when
     * the Logger starts sampling.
     */
    void watch(String tableName);

    /** @return the first cell of the table, or null when no open ROM has it */
    Cell firstCell(String tableName);
}
