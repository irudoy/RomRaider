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

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;

import com.romraider.Settings;
import com.romraider.logger.external.core.ExternalDataItem;
import com.romraider.logger.external.udp.plugin.OpenRomTableValues;
import com.romraider.logger.external.udp.plugin.RomTableValues;
import com.romraider.logger.external.udp.plugin.UdpDataItem;
import com.romraider.logger.external.udp.plugin.UdpDataSource;
import com.romraider.logger.external.udp.plugin.UdpExpression;
import com.romraider.maps.Rom;
import com.romraider.maps.RomID;
import com.romraider.maps.Table1D;
import com.romraider.swing.JProgressPane;

public class UdpExpressionTest {
    private static final double EXACT = 0.0;
    private static final String REFERENCE_TABLE = "Load Reference";
    // a byte ratio in 1/128 steps of a 1/2048 ms value to a ROM word
    private static final String RATIO =
            "min(255, floor(round(fuel_ms*2048)*128/reference))*100/128";
    private static final int REFERENCE = 27226;

    /** ROM tables by name; a table may be added and removed between reads. */
    private static final class Tables implements RomTableValues {
        final Map<String, Cell> cells = new HashMap<String, Cell>();
        final List<String> watched = new ArrayList<String>();

        public void watch(String tableName) {
            watched.add(tableName);
        }

        public Cell firstCell(String tableName) {
            return cells.get(tableName);
        }
    }

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

    private static Tables tables() {
        final Tables tables = new Tables();
        tables.cells.put(REFERENCE_TABLE,
                new RomTableValues.Cell(REFERENCE, "stock.bin"));
        return tables;
    }

    private static Properties properties() {
        final Properties properties = new Properties();
        properties.setProperty("udp.port", "0");
        properties.setProperty("udp.item.1",
                "fuel_ms|Fuel Schedule|ms|0.000|0|20|2");
        properties.setProperty("udp.item.2",
                "load|Engine Load|%|0.00|0|200|20");
        properties.setProperty("udp.item.2.expression", RATIO);
        properties.setProperty("udp.item.2.table.reference", REFERENCE_TABLE);
        return properties;
    }

    private static UdpDataSource source(RomTableValues tables,
            Properties properties) {
        final UdpDataSource source = new UdpDataSource(tables);
        source.setProperties(properties);
        return source;
    }

    private static UdpDataItem item(UdpDataSource source, int index) {
        return (UdpDataItem) source.getDataItems().get(index);
    }

    @Test
    public void derivedItemCombinesADatagramItemWithARomTable() {
        final UdpDataSource source = source(tables(), properties());
        final UdpDataItem fuel = item(source, 0);
        final UdpDataItem load = item(source, 1);
        assertEquals(0.0, load.getData(), EXACT);
        fuel.setData(REFERENCE / 2048.0);
        assertEquals(100.0, load.getData(), EXACT);
        fuel.setData(5.81543);
        assertEquals(11910 * 128 / REFERENCE * 100 / 128.0, load.getData(),
                EXACT);
        assertEquals("43.75", load.getConvertors()[0].format(43.75));
    }

    @Test
    public void expressionMatchesIntegerArithmeticForEveryDatagramValue() {
        final UdpDataSource source = source(tables(), properties());
        final UdpDataItem fuel = item(source, 0);
        final UdpDataItem load = item(source, 1);
        for (int raw = 0; raw <= 0xFFFF; raw++) {
            // the sender prints seven significant digits
            fuel.setData(Double.parseDouble(
                    String.format(Locale.ROOT, "%.7g", raw / 2048.0)));
            final int ratio = Math.min(255, raw * 128 / REFERENCE);
            assertEquals("raw " + raw, ratio * 100 / 128.0, load.getData(),
                    EXACT);
        }
    }

    @Test
    public void derivedItemIsUnavailableUntilARomHoldsTheTable() {
        final Tables tables = new Tables();
        final UdpDataSource source = source(tables, properties());
        final UdpDataItem load = item(source, 1);
        item(source, 0).setData(REFERENCE / 2048.0);
        assertEquals(UdpExpression.UNAVAILABLE, load.getData(), EXACT);
        assertTrue(source.describe(), source.describe().contains(
                "reference: no open ROM has the table Load Reference"));

        tables.cells.put(REFERENCE_TABLE,
                new RomTableValues.Cell(REFERENCE, "stock.bin"));
        assertEquals(100.0, load.getData(), EXACT);
        assertTrue(source.describe(), source.describe().contains(
                "Engine Load = " + RATIO
                + "\n    reference = 27226: Load Reference, stock.bin"));

        tables.cells.put(REFERENCE_TABLE,
                new RomTableValues.Cell(REFERENCE / 2, "tuned.bin"));
        assertEquals(255 * 100 / 128.0, load.getData(), EXACT);
    }

    @Test
    public void valueThatIsNotFiniteIsUnavailable() {
        final Properties properties = properties();
        properties.setProperty("udp.item.2.expression", "fuel_ms/reference");
        final Tables tables = new Tables();
        tables.cells.put(REFERENCE_TABLE, new RomTableValues.Cell(0, "x.bin"));
        final UdpDataSource source = source(tables, properties);
        item(source, 0).setData(1.0);
        assertEquals(UdpExpression.UNAVAILABLE, item(source, 1).getData(),
                EXACT);
    }

    @Test
    public void functionsCoverIntegerArithmetic() {
        final Map<String, UdpDataItem> none = Collections.emptyMap();
        final Map<String, String> unbound = Collections.emptyMap();
        assertEquals(2.0 + 3.0 + 1.0 + 2.0, new UdpExpression(
                "floor(2.7) + round(2.5) + min(1, 2) + max(1, 2)",
                none, unbound, new Tables()).value(), EXACT);
        assertEquals(-3.0, new UdpExpression(
                "floor(-2.5)", none, unbound, new Tables()).value(), EXACT);
        assertEquals(10.0 + 3.0 + 4.0, new UdpExpression(
                "if(3 > 2, 10, 20) + 7 % 4 + abs(-4)",
                none, unbound, new Tables()).value(), EXACT);
    }

    @Test
    public void connectNamesTheRomTablesBeforeSampling() {
        final Tables tables = tables();
        final UdpDataSource source = source(tables, properties());
        source.connect();
        try {
            assertEquals(Arrays.asList(REFERENCE_TABLE), tables.watched);
        } finally {
            source.disconnect();
        }
    }

    @Test
    public void datagramDoesNotReplaceADerivedValue() throws Exception {
        final UdpDataSource source = source(tables(), properties());
        source.connect();
        try {
            final byte[] payload = "RRUDP1 load=7 fuel_ms=13.29395"
                    .getBytes(StandardCharsets.US_ASCII);
            final DatagramSocket socket = new DatagramSocket();
            try {
                socket.send(new DatagramPacket(payload, payload.length,
                        InetAddress.getByName("127.0.0.1"),
                        source.getLocalPort()));
            } finally {
                socket.close();
            }
            final ExternalDataItem fuel = source.getDataItems().get(0);
            final long deadline = System.currentTimeMillis() + 5000L;
            while (fuel.getData() == 0.0
                    && System.currentTimeMillis() < deadline) {
                Thread.sleep(5L);
            }
            assertEquals(13.29395, fuel.getData(), EXACT);
            assertEquals(100.0, source.getDataItems().get(1).getData(), EXACT);
        } finally {
            source.disconnect();
        }
    }

    @Test
    public void derivedItemDescribesItsOrigin() {
        final UdpDataSource source = source(tables(), properties());
        assertEquals("Fuel Schedule (UDP key fuel_ms)",
                item(source, 0).getDescription());
        assertEquals("Engine Load (derived from UDP telemetry)",
                item(source, 1).getDescription());
        assertNull(item(source, 0).getExpression());
        assertNotNull(item(source, 1).getExpression());
    }

    @Test(expected = IllegalArgumentException.class)
    public void expressionReadsDatagramItemsOnly() {
        final Properties properties = properties();
        properties.setProperty("udp.item.3",
                "double|Double Load|%|0.0|0|400|40");
        properties.setProperty("udp.item.3.expression", "load*2");
        source(tables(), properties);
    }

    @Test(expected = IllegalArgumentException.class)
    public void malformedExpressionRejectsThePluginFile() {
        final Properties properties = properties();
        properties.setProperty("udp.item.2.expression", "min(255, fuel_ms");
        source(tables(), properties);
    }

    @Test(expected = IllegalArgumentException.class)
    public void tableVariableMustNotRepeatAnItemKey() {
        final Properties properties = properties();
        properties.setProperty("udp.item.2.table.fuel_ms", REFERENCE_TABLE);
        source(tables(), properties);
    }

    @Test(expected = IllegalArgumentException.class)
    public void tableBindingRequiresAnExpression() {
        final Properties properties = properties();
        properties.setProperty("udp.item.1.table.reference", REFERENCE_TABLE);
        source(tables(), properties);
    }

    @Test(expected = IllegalArgumentException.class)
    public void expressionRequiresAnItemDefinition() {
        final Properties properties = properties();
        properties.setProperty("udp.item.9.expression", "fuel_ms");
        source(tables(), properties);
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknownItemPropertyIsRejected() {
        final Properties properties = properties();
        properties.setProperty("udp.item.2.formula", "fuel_ms");
        source(tables(), properties);
    }

    @Test
    public void openImagesAreReadInOrderOfPreference() {
        final Rom selected = rom("selected.bin", "Other Table", 1);
        final Rom stock = rom("stock.bin", REFERENCE_TABLE, REFERENCE);
        final Rom tuned = rom("tuned.bin", REFERENCE_TABLE, 30000);

        final Map<String, RomTableValues.Cell> cells = OpenRomTableValues.read(
                Arrays.asList(selected, stock, tuned),
                Arrays.asList(REFERENCE_TABLE, "Other Table", "Absent"));

        assertEquals(2, cells.size());
        assertEquals(REFERENCE, cells.get(REFERENCE_TABLE).getStoredValue(),
                EXACT);
        assertEquals("stock.bin", cells.get(REFERENCE_TABLE).getRomFileName());
        assertEquals("selected.bin", cells.get("Other Table").getRomFileName());
        assertFalse(cells.containsKey("Absent"));
        assertTrue(OpenRomTableValues.read(
                Collections.<Rom>emptyList(),
                Arrays.asList(REFERENCE_TABLE)).isEmpty());
    }

    private static Rom rom(String fileName, String tableName, int word) {
        final Rom rom = new Rom(new RomID());
        rom.setFileName(fileName);
        final Table1D table = new Table1D();
        table.setName(tableName);
        table.setCategory("Test");
        table.setStorageAddress(0);
        table.setStorageType(2);
        table.setEndian(Settings.Endian.BIG);
        table.setDataSize(1);
        rom.addTableByName(table);
        rom.populateTables(
                new byte[] {(byte) (word >> 8), (byte) word},
                new JProgressPane());
        return rom;
    }
}
