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

import static com.romraider.util.ThreadUtil.runAsDaemon;
import static java.util.Collections.unmodifiableList;
import static javax.swing.JOptionPane.INFORMATION_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static org.apache.log4j.Logger.getLogger;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.apache.log4j.Logger;

import com.romraider.logger.ecu.EcuLogger;
import com.romraider.logger.external.core.ExternalDataItem;
import com.romraider.logger.external.core.ExternalDataSource;
import com.romraider.logger.external.udp.io.UdpRunner;

/**
 * External data source fed by telemetry datagrams. The plugin file declares
 * the listening endpoint and the data items:
 * <pre>
 * datasource.class=com.romraider.logger.external.udp.plugin.UdpDataSource
 * udp.name=My Telemetry
 * udp.host=127.0.0.1
 * udp.port=47809
 * udp.item.1=rpm|Engine Speed|RPM|0|0|8000|1000
 * udp.item.2=ratio|Ratio|%|0.0|0|200|20
 * udp.item.2.expression=min(200, rpm*100/limit)
 * udp.item.2.table.limit=Rev Limit
 * </pre>
 * Items are ordered by their numeric suffix. The sender, not the Logger,
 * selects the endpoint, so the plugin file is its only source. An item with
 * an expression is derived from the datagram items and from tables of the
 * ROM open in the editor.
 */
public final class UdpDataSource implements ExternalDataSource {
    private static final Logger LOGGER = getLogger(UdpDataSource.class);
    private static final String ITEM_PREFIX = "udp.item.";
    private static final String EXPRESSION = "expression";
    private static final String TABLE = "table";
    public static final String DEFAULT_NAME = "UDP Telemetry";
    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 47809;
    private final Map<String, UdpDataItem> dataItems =
            new LinkedHashMap<String, UdpDataItem>();
    private final RomTableValues romTableValues;
    private String name = DEFAULT_NAME;
    private String host = DEFAULT_HOST;
    private int port = DEFAULT_PORT;
    private UdpRunner runner;

    public UdpDataSource() {
        this(new OpenRomTableValues());
    }

    public UdpDataSource(RomTableValues romTableValues) {
        this.romTableValues = romTableValues;
    }

    public String getId() {
        return getClass().getName();
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return "1.1";
    }

    public List<? extends ExternalDataItem> getDataItems() {
        return unmodifiableList(new ArrayList<UdpDataItem>(dataItems.values()));
    }

    public Action getMenuAction(final EcuLogger logger) {
        return new AbstractAction() {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent actionEvent) {
                showMessageDialog(logger, describe(), name,
                        INFORMATION_MESSAGE);
            }
        };
    }

    public void setPort(String port) {
        // the plugin file selects the endpoint
    }

    public String getPort() {
        return String.valueOf(port);
    }

    public void setProperties(Properties properties) {
        name = properties.getProperty("udp.name", DEFAULT_NAME).trim();
        host = properties.getProperty("udp.host", DEFAULT_HOST).trim();
        port = Integer.parseInt(properties.getProperty(
                "udp.port", String.valueOf(DEFAULT_PORT)).trim());
        if (port < 0 || port > 0xFFFF) {
            throw new IllegalArgumentException("udp.port out of range: " + port);
        }
        final Map<Integer, String> definitions = new TreeMap<Integer, String>();
        final Map<Integer, String> expressions = new TreeMap<Integer, String>();
        final Map<Integer, Map<String, String>> tables =
                new HashMap<Integer, Map<String, String>>();
        for (String property : properties.stringPropertyNames()) {
            if (!property.startsWith(ITEM_PREFIX)) continue;
            final String[] parts =
                    property.substring(ITEM_PREFIX.length()).split("\\.", 3);
            final Integer index = Integer.valueOf(parts[0]);
            final String value = properties.getProperty(property).trim();
            if (parts.length == 1) {
                definitions.put(index, value);
            } else if (parts.length == 2 && EXPRESSION.equals(parts[1])) {
                expressions.put(index, value);
            } else if (parts.length == 3 && TABLE.equals(parts[1])
                    && !parts[2].isEmpty()) {
                if (!tables.containsKey(index)) {
                    tables.put(index, new TreeMap<String, String>());
                }
                tables.get(index).put(parts[2], value);
            } else {
                throw new IllegalArgumentException(
                        "unknown property: " + property);
            }
        }
        for (Integer index : tables.keySet()) {
            if (!expressions.containsKey(index)) {
                throw new IllegalArgumentException("udp.item." + index
                        + " binds a table without an expression");
            }
        }
        dataItems.clear();
        final Map<Integer, UdpDataItem> indexed =
                new HashMap<Integer, UdpDataItem>();
        for (Map.Entry<Integer, String> definition : definitions.entrySet()) {
            final UdpDataItem dataItem = UdpDataItem.parse(definition.getValue());
            if (dataItems.put(dataItem.getKey(), dataItem) != null) {
                throw new IllegalArgumentException(
                        "duplicate udp.item key: " + dataItem.getKey());
            }
            indexed.put(definition.getKey(), dataItem);
        }
        // an expression reads datagram items only, so it cannot form a cycle
        final Map<String, UdpDataItem> inputs =
                new LinkedHashMap<String, UdpDataItem>();
        for (Map.Entry<Integer, UdpDataItem> dataItem : indexed.entrySet()) {
            if (!expressions.containsKey(dataItem.getKey())) {
                inputs.put(dataItem.getValue().getKey(), dataItem.getValue());
            }
        }
        for (Map.Entry<Integer, String> expression : expressions.entrySet()) {
            final UdpDataItem dataItem = indexed.get(expression.getKey());
            if (dataItem == null) {
                throw new IllegalArgumentException("udp.item."
                        + expression.getKey() + " has an expression and no"
                        + " definition");
            }
            final Map<String, String> bound = tables.get(expression.getKey());
            dataItem.setExpression(new UdpExpression(expression.getValue(),
                    inputs,
                    bound == null ? new TreeMap<String, String>() : bound,
                    romTableValues));
        }
    }

    /** @return the endpoint, the item count and the state of derived items */
    public String describe() {
        final StringBuilder text = new StringBuilder("Listening on UDP ")
                .append(host).append(":").append(port).append("\n")
                .append(dataItems.size())
                .append(" data items declared by the plugin file");
        for (UdpDataItem dataItem : dataItems.values()) {
            final UdpExpression expression = dataItem.getExpression();
            if (expression == null) continue;
            text.append("\n\n").append(dataItem.getName()).append(" = ")
                    .append(expression.describe());
        }
        return text.toString();
    }

    public synchronized void connect() {
        if (runner != null) return;
        for (UdpDataItem dataItem : dataItems.values()) {
            if (dataItem.getExpression() != null) {
                dataItem.getExpression().prime();
            }
        }
        try {
            runner = new UdpRunner(host, port, dataItems);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "cannot listen on UDP " + host + ":" + port, e);
        }
        LOGGER.info(name + ": listening on UDP " + host + ":"
                + runner.getLocalPort());
        runAsDaemon(runner);
    }

    public synchronized void disconnect() {
        if (runner == null) return;
        runner.stop();
        runner = null;
    }

    /** @return the bound port while connected, otherwise -1 */
    public synchronized int getLocalPort() {
        return runner == null ? -1 : runner.getLocalPort();
    }
}
