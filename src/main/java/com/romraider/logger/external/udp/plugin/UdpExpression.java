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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.nfunk.jep.JEP;

/**
 * Value of a derived data item. The expression reads the latest values of
 * the datagram items by their keys and the first cells of ROM tables by the
 * variables the plugin file binds to table names. A table variable holds the
 * value as the ROM stores it, so it does not depend on the scaling selected
 * in the editor.
 */
public final class UdpExpression {
    /** Value of an expression whose ROM table is not open. */
    public static final double UNAVAILABLE = 0.0;
    private final String expression;
    private final Map<String, UdpDataItem> items;
    private final Map<String, String> tables;
    private final RomTableValues romTableValues;
    private final Map<String, RomTableValues.Cell> cells =
            new HashMap<String, RomTableValues.Cell>();
    private final JEP parser = new JEP();

    /**
     * @param items datagram items by key
     * @param tables ROM table names by variable
     * @throws IllegalArgumentException when the expression does not parse
     */
    public UdpExpression(String expression, Map<String, UdpDataItem> items,
            Map<String, String> tables, RomTableValues romTableValues) {
        this.expression = expression;
        this.items = new LinkedHashMap<String, UdpDataItem>(items);
        this.tables = new LinkedHashMap<String, String>(tables);
        this.romTableValues = romTableValues;
        parser.addStandardFunctions();
        UdpFunctions.addTo(parser);
        for (String key : this.items.keySet()) {
            parser.addVariable(key, 0.0);
        }
        for (String variable : this.tables.keySet()) {
            if (this.items.containsKey(variable)) {
                throw new IllegalArgumentException(
                        "table variable repeats an item key: " + variable);
            }
            parser.addVariable(variable, 0.0);
        }
        parser.parseExpression(expression);
        if (parser.hasError()) {
            throw new IllegalArgumentException("expression '" + expression
                    + "': " + parser.getErrorInfo().trim());
        }
    }

    /** Names the ROM tables before the Logger starts sampling. */
    public void prime() {
        for (String tableName : tables.values()) {
            romTableValues.watch(tableName);
        }
    }

    /** @return the expression value, or {@link #UNAVAILABLE} */
    public synchronized double value() {
        boolean complete = true;
        for (Map.Entry<String, String> table : tables.entrySet()) {
            final RomTableValues.Cell cell =
                    romTableValues.firstCell(table.getValue());
            cells.put(table.getKey(), cell);
            if (cell == null) {
                complete = false;
            } else {
                parser.setVarValue(table.getKey(),
                        Double.valueOf(cell.getStoredValue()));
            }
        }
        if (!complete) return UNAVAILABLE;
        for (Map.Entry<String, UdpDataItem> item : items.entrySet()) {
            parser.setVarValue(item.getKey(),
                    Double.valueOf(item.getValue().getData()));
        }
        final double value = parser.getValue();
        return Double.isNaN(value) || Double.isInfinite(value)
                ? UNAVAILABLE : value;
    }

    /** @return the expression and the present source of every table variable */
    public synchronized String describe() {
        value();
        final StringBuilder text = new StringBuilder(expression);
        for (Map.Entry<String, String> table : tables.entrySet()) {
            final RomTableValues.Cell cell = cells.get(table.getKey());
            text.append("\n    ").append(table.getKey());
            if (cell == null) {
                text.append(": no open ROM has the table ")
                        .append(table.getValue());
            } else {
                text.append(" = ").append(number(cell.getStoredValue()))
                        .append(": ").append(table.getValue())
                        .append(", ").append(cell.getRomFileName());
            }
        }
        return text.toString();
    }

    private static String number(double value) {
        return value == Math.rint(value) && Math.abs(value) < 1e15
                ? String.valueOf((long) value) : String.valueOf(value);
    }
}
