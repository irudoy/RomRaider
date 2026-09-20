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

import static org.apache.log4j.Logger.getLogger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.log4j.Logger;

import com.romraider.logger.external.core.Stoppable;
import com.romraider.logger.external.udp.plugin.UdpDataItem;

/**
 * Receives telemetry datagrams and stores their values in the data items.
 * The socket is bound by the constructor so that a bind failure reaches the
 * caller instead of the receive thread.
 */
public final class UdpRunner implements Stoppable {
    private static final Logger LOGGER = getLogger(UdpRunner.class);
    private static final int MAX_DATAGRAM = 8192;
    private final Map<String, UdpDataItem> dataItems;
    private final DatagramSocket socket;
    private volatile boolean stop;

    public UdpRunner(String host, int port,
            Map<String, UdpDataItem> dataItems) throws IOException {
        this.dataItems = dataItems;
        this.socket = new DatagramSocket(
                new InetSocketAddress(InetAddress.getByName(host), port));
    }

    public int getLocalPort() {
        return socket.getLocalPort();
    }

    public void run() {
        final byte[] buffer = new byte[MAX_DATAGRAM];
        final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        try {
            while (!stop) {
                packet.setLength(buffer.length);
                socket.receive(packet);
                update(new String(packet.getData(), packet.getOffset(),
                        packet.getLength(), StandardCharsets.US_ASCII));
            }
        } catch (SocketException e) {
            if (!stop) LOGGER.error("UDP telemetry socket error", e);
        } catch (IOException e) {
            LOGGER.error("UDP telemetry receive error", e);
        } finally {
            socket.close();
        }
    }

    public void stop() {
        stop = true;
        socket.close();
    }

    private void update(String datagram) {
        for (Map.Entry<String, Double> value
                : UdpDatagramParser.parse(datagram).entrySet()) {
            final UdpDataItem dataItem = dataItems.get(value.getKey());
            if (dataItem != null) dataItem.setData(value.getValue());
        }
    }
}
