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

package com.romraider.logger.external.udp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

import com.romraider.logger.ecu.definition.EcuDataConvertor;
import com.romraider.logger.ecu.definition.ExternalDataImpl;
import com.romraider.logger.external.core.ExternalDataItem;
import com.romraider.logger.external.udp.io.UdpDatagramParser;
import com.romraider.logger.external.udp.plugin.UdpDataItem;
import com.romraider.logger.external.udp.plugin.UdpDataSource;

public class UdpDataSourceTest {
    private static final double EXACT = 0.0;

    private static Properties properties(String port) {
        final Properties properties = new Properties();
        properties.setProperty("udp.name", "VQ CAN Telemetry");
        properties.setProperty("udp.host", "127.0.0.1");
        properties.setProperty("udp.port", port);
        properties.setProperty("udp.item.10",
                "coolant_c|VQ Coolant Temperature|C|0|-40|140|20");
        properties.setProperty("udp.item.1",
                "rpm|VQ Engine Speed|RPM|0|0|8000|1000");
        properties.setProperty("udp.item.2",
                "load|VQ Engine Load|%|0.00|0|200|20");
        return properties;
    }

    @Test
    public void parserReturnsFiniteValuesOfAVersionedDatagram() {
        final Map<String, Double> values = UdpDatagramParser.parse(
                "RRUDP1 rpm=2512.5 load=43.75\tbad token=1 =2 x= nan=NaN"
                + " inf=Infinity neg=-1e-3\n");
        assertEquals(4, values.size());
        assertEquals(2512.5, values.get("rpm"), EXACT);
        assertEquals(43.75, values.get("load"), EXACT);
        assertEquals(1.0, values.get("token"), EXACT);
        assertEquals(-0.001, values.get("neg"), EXACT);
    }

    @Test
    public void parserRejectsAnotherFormatToken() {
        assertTrue(UdpDatagramParser.parse("RRUDP2 rpm=1").isEmpty());
        assertTrue(UdpDatagramParser.parse("rpm=1").isEmpty());
        assertTrue(UdpDatagramParser.parse("").isEmpty());
    }

    @Test
    public void itemDefinitionCarriesPresentationOfOneConvertor() {
        final UdpDataItem item =
                UdpDataItem.parse("load|VQ Engine Load|%|0.00|0|200|20");
        assertEquals("load", item.getKey());
        assertEquals("VQ Engine Load", item.getName());
        final EcuDataConvertor[] convertors = item.getConvertors();
        assertEquals(1, convertors.length);
        assertEquals("%", convertors[0].getUnits());
        assertEquals(200.0, convertors[0].getGaugeMinMax().max, EXACT);
        item.setData(43.75);
        assertEquals(43.75, convertors[0].convert(null), EXACT);
        assertEquals("43.75", convertors[0].format(43.75));
    }

    @Test(expected = IllegalArgumentException.class)
    public void itemDefinitionRequiresSevenFields() {
        UdpDataItem.parse("rpm|VQ Engine Speed|RPM|0");
    }

    @Test(expected = IllegalArgumentException.class)
    public void itemDefinitionRequiresAKey() {
        UdpDataItem.parse("|VQ Engine Speed|RPM|0|0|8000|1000");
    }

    @Test
    public void pluginFileOrdersItemsByNumericSuffix() {
        final UdpDataSource source = new UdpDataSource();
        source.setProperties(properties("47809"));
        assertEquals("VQ CAN Telemetry", source.getName());
        assertEquals("47809", source.getPort());
        final List<? extends ExternalDataItem> items = source.getDataItems();
        assertEquals(3, items.size());
        assertEquals("VQ Engine Speed", items.get(0).getName());
        assertEquals("VQ Engine Load", items.get(1).getName());
        assertEquals("VQ Coolant Temperature", items.get(2).getName());
    }

    @Test
    public void loggerIdIsTheValueATableLogparamNames() {
        final UdpDataSource source = new UdpDataSource();
        source.setProperties(properties("47809"));
        assertEquals("X_VQ_Engine_Speed",
                new ExternalDataImpl(source.getDataItems().get(0), source)
                        .getId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void duplicateKeysAreRejected() {
        final Properties properties = properties("47809");
        properties.setProperty("udp.item.3",
                "rpm|Second Engine Speed|RPM|0|0|8000|1000");
        new UdpDataSource().setProperties(properties);
    }

    @Test
    public void persistedLoggerPortDoesNotReplaceThePluginFileEndpoint() {
        final UdpDataSource source = new UdpDataSource();
        source.setProperties(properties("47809"));
        source.setPort("COM3");
        assertEquals("47809", source.getPort());
    }

    @Test
    public void datagramsUpdateDeclaredItemsAndKeepMissingValues()
            throws Exception {
        final UdpDataSource source = new UdpDataSource();
        source.setProperties(properties("0"));
        source.connect();
        try {
            final List<? extends ExternalDataItem> items = source.getDataItems();
            send(source.getLocalPort(),
                    "RRUDP1 rpm=2512.5 load=43.75 unknown=7");
            await(items.get(1), 43.75);
            assertEquals(2512.5, items.get(0).getData(), EXACT);
            assertEquals(0.0, items.get(2).getData(), EXACT);

            send(source.getLocalPort(), "not telemetry");
            send(source.getLocalPort(), "RRUDP1 load=50 coolant_c=90");
            await(items.get(2), 90.0);
            assertEquals(2512.5, items.get(0).getData(), EXACT);
            assertEquals(50.0, items.get(1).getData(), EXACT);
        } finally {
            source.disconnect();
        }
        assertEquals(-1, source.getLocalPort());
    }

    private static void send(int port, String text) throws Exception {
        final byte[] payload = text.getBytes(StandardCharsets.US_ASCII);
        final DatagramSocket socket = new DatagramSocket();
        try {
            socket.send(new DatagramPacket(payload, payload.length,
                    InetAddress.getByName("127.0.0.1"), port));
        } finally {
            socket.close();
        }
    }

    private static void await(ExternalDataItem item, double expected)
            throws InterruptedException {
        final long deadline = System.currentTimeMillis() + 5000L;
        while (item.getData() != expected
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(5L);
        }
        assertEquals(expected, item.getData(), EXACT);
    }
}
