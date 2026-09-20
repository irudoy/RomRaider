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

package com.romraider.logger.external.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.junit.Test;

import com.romraider.logger.external.udp.plugin.UdpDataSource;

public class GenericDataSourceManagerTest {
    private static final long TIMEOUT_MS = 5000L;

    private static UdpDataSource source(int port) {
        final Properties properties = new Properties();
        properties.setProperty("udp.port", String.valueOf(port));
        properties.setProperty("udp.item.1",
                "rpm|VQ Engine Speed|RPM|0|0|8000|1000");
        final UdpDataSource source = new UdpDataSource();
        source.setProperties(properties);
        return source;
    }

    /** The manager connects on a thread of its own. */
    private static int boundPort(UdpDataSource source) throws Exception {
        final long end = System.currentTimeMillis() + TIMEOUT_MS;
        while (source.getLocalPort() < 0 && System.currentTimeMillis() < end) {
            Thread.sleep(10L);
        }
        assertTrue("data source is connected", source.getLocalPort() > 0);
        return source.getLocalPort();
    }

    @Test
    public void oneDisconnectLeavesASourceHeldByOtherItemsConnected()
            throws Exception {
        final UdpDataSource source = source(0);
        final GenericDataSourceManager manager =
                new GenericDataSourceManager(source);
        manager.connect();
        manager.connect();
        manager.connect();
        final int port = boundPort(source);

        manager.disconnect();
        assertEquals(port, source.getLocalPort());

        manager.release();
        assertEquals(-1, source.getLocalPort());
    }

    @Test
    public void aReleasedPortIsFreeForTheSourceOfTheNextLogger()
            throws Exception {
        final UdpDataSource closed = source(0);
        final GenericDataSourceManager closedLogger =
                new GenericDataSourceManager(closed);
        closedLogger.connect();
        closedLogger.connect();
        final int port = boundPort(closed);
        closedLogger.release();

        final UdpDataSource opened = source(port);
        final GenericDataSourceManager openedLogger =
                new GenericDataSourceManager(opened);
        openedLogger.connect();
        try {
            assertEquals(port, boundPort(opened));
        } finally {
            openedLogger.release();
        }
    }

    @Test
    public void aReleasedSourceConnectsAgain() throws Exception {
        final UdpDataSource source = source(0);
        final GenericDataSourceManager manager =
                new GenericDataSourceManager(source);
        manager.release();
        assertEquals(-1, source.getLocalPort());

        manager.connect();
        manager.connect();
        boundPort(source);
        manager.release();
        assertEquals(-1, source.getLocalPort());

        manager.connect();
        try {
            boundPort(source);
        } finally {
            manager.release();
        }
    }
}
