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

package com.romraider.logger.ecu.comms.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Test;

import com.romraider.logger.ecu.comms.query.Response;
import com.romraider.logger.ecu.comms.query.ResponseImpl;
import com.romraider.logger.ecu.definition.LoggerData;
import com.romraider.logger.ecu.ui.handler.DataUpdateHandler;

public class AsyncDataUpdateHandlerTest {
    private AsyncDataUpdateHandler updater;

    @After
    public void stop() throws Exception {
        if (updater != null) {
            updater.stopUpdater();
            updater.join(2000);
        }
    }

    @Test
    public void aFailingHandlerLeavesTheOthersAndTheNextResponsesRunning()
            throws Exception {
        final Counting failing = new Counting(true);
        final Counting healthy = new Counting(false);
        updater = new AsyncDataUpdateHandler(
                new DataUpdateHandler[] {failing, healthy});
        updater.start();

        updater.addResponse(new ResponseImpl());
        updater.addResponse(new ResponseImpl());
        waitFor(healthy, 2);

        assertEquals(2, failing.updates.get());
        assertEquals(2, healthy.updates.get());
        assertTrue(updater.isAlive());
    }

    private static void waitFor(Counting handler, int updates)
            throws InterruptedException {
        final long deadline = System.currentTimeMillis() + 2000;
        while (handler.updates.get() < updates
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
    }

    private static final class Counting implements DataUpdateHandler {
        final AtomicInteger updates = new AtomicInteger();
        private final boolean fails;

        Counting(boolean fails) {
            this.fails = fails;
        }

        @Override
        public void registerData(LoggerData loggerData) {
        }

        @Override
        public void handleDataUpdate(Response response) {
            updates.incrementAndGet();
            if (fails) throw new IllegalStateException("handler failure");
        }

        @Override
        public void deregisterData(LoggerData loggerData) {
        }

        @Override
        public void cleanUp() {
        }

        @Override
        public void reset() {
        }
    }
}
