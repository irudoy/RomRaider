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

import com.romraider.logger.ecu.definition.EcuDataConvertor;
import com.romraider.logger.ecu.definition.ExternalDataConvertorImpl;
import com.romraider.logger.ecu.ui.handler.dash.GaugeMinMax;
import com.romraider.logger.external.core.ExternalDataItem;

/**
 * One value of the telemetry datagram. The sender delivers engineering
 * units, so the single convertor passes the value through. An item with an
 * expression is derived: it takes its value from the expression, and the
 * datagram does not carry its key.
 */
public final class UdpDataItem implements ExternalDataItem {
    private static final int FIELD_COUNT = 7;
    private final String key;
    private final String name;
    private final EcuDataConvertor[] convertors;
    private volatile double data;
    private volatile UdpExpression expression;

    public UdpDataItem(String key, String name, String units, String format,
            GaugeMinMax gaugeMinMax) {
        this.key = key;
        this.name = name;
        this.convertors = new EcuDataConvertor[] {
            new ExternalDataConvertorImpl(this, units, "x", format, gaugeMinMax)
        };
    }

    /**
     * @param definition <code>key|name|units|format|min|max|step</code>
     * @throws IllegalArgumentException when the definition is malformed
     */
    public static UdpDataItem parse(String definition) {
        final String[] fields = definition.split("\\|", -1);
        if (fields.length != FIELD_COUNT) {
            throw new IllegalArgumentException(
                    "expected key|name|units|format|min|max|step: "
                    + definition);
        }
        final String key = fields[0].trim();
        final String name = fields[1].trim();
        if (key.isEmpty() || name.isEmpty()) {
            throw new IllegalArgumentException(
                    "key and name are required: " + definition);
        }
        return new UdpDataItem(key, name, fields[2].trim(), fields[3].trim(),
                new GaugeMinMax(
                        Double.parseDouble(fields[4].trim()),
                        Double.parseDouble(fields[5].trim()),
                        Double.parseDouble(fields[6].trim())));
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return expression == null
                ? name + " (UDP key " + key + ")"
                : name + " (derived from UDP telemetry)";
    }

    public double getData() {
        final UdpExpression expression = this.expression;
        return expression == null ? data : expression.value();
    }

    public UdpExpression getExpression() {
        return expression;
    }

    public void setExpression(UdpExpression expression) {
        this.expression = expression;
    }

    public void setData(double data) {
        this.data = data;
    }

    public EcuDataConvertor[] getConvertors() {
        return convertors;
    }
}
