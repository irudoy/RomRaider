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

package com.romraider.logger.external.udp.io;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parses one telemetry datagram: the token <code>RRUDP1</code> followed by
 * whitespace separated <code>key=value</code> tokens with decimal values.
 */
public final class UdpDatagramParser {
    public static final String MAGIC = "RRUDP1";

    private UdpDatagramParser() {
    }

    /**
     * @return the finite values of the datagram by key, empty when the
     * datagram does not start with the format token
     */
    public static Map<String, Double> parse(String datagram) {
        final Map<String, Double> values = new LinkedHashMap<String, Double>();
        final String[] tokens = datagram.trim().split("\\s+");
        if (!MAGIC.equals(tokens[0])) return values;
        for (int i = 1; i < tokens.length; i++) {
            final int separator = tokens[i].indexOf('=');
            if (separator <= 0) continue;
            try {
                final double value =
                        Double.parseDouble(tokens[i].substring(separator + 1));
                if (Double.isNaN(value) || Double.isInfinite(value)) continue;
                values.put(tokens[i].substring(0, separator), value);
            } catch (NumberFormatException e) {
                // a malformed token does not invalidate the other values
            }
        }
        return values;
    }
}
