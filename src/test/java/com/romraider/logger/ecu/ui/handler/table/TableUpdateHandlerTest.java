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

package com.romraider.logger.ecu.ui.handler.table;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import javax.swing.SwingUtilities;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import com.romraider.Settings;
import com.romraider.editor.ecu.ECUEditorManager;
import com.romraider.logger.ecu.comms.query.ResponseImpl;
import com.romraider.logger.ecu.definition.ExternalDataImpl;
import com.romraider.logger.ecu.definition.LoggerData;
import com.romraider.logger.external.udp.plugin.UdpDataItem;
import com.romraider.logger.external.udp.plugin.UdpDataSource;
import com.romraider.maps.Rom;
import com.romraider.maps.RomID;
import com.romraider.maps.Table;
import com.romraider.maps.Table1D;
import com.romraider.maps.Table1DView;
import com.romraider.maps.Table2D;
import com.romraider.maps.Table2DView;
import com.romraider.maps.Table3D;
import com.romraider.maps.Table3DView;
import com.romraider.swing.JProgressPane;

public class TableUpdateHandlerTest {
    private static final String SPEED = "X_VQ_Engine_Speed";
    private static final String LOAD = "X_VQ_Engine_Load";
    // speed axis 10, 20, 30; load axis 40, 50; six data cells
    private static final byte[] IMAGE = {10, 20, 30, 40, 50, 1, 2, 3, 4, 5, 6};
    private Table registered;

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

    @BeforeClass
    public static void tolerateTheMissingEditorWindow() {
        // an axis view moves its live cell before it reports the value to the
        // toolbar of the editor window, which a headless run cannot create
        Thread.setDefaultUncaughtExceptionHandler(
                new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread thread, Throwable e) {
                        if (!editorWindowIsMissing(e)) e.printStackTrace();
                    }
                });
    }

    @After
    public void deregister() {
        if (registered != null) {
            TableUpdateHandler.getInstance().deregisterTable(registered);
        }
    }

    @Test
    public void aViewOfATableWithTwoAxesIsReachedThroughItsAxisTables() {
        final Table3D table = surface();
        final Table3DView view = new Table3DView(table);
        table.setTableView(view);
        assertSame(view, table.getTableView());
        assertSame(view.getXAxis(), table.getXAxis().getTableView());
        assertSame(view.getYAxis(), table.getYAxis().getTableView());

        final Table3DView reopened = new Table3DView(table);
        table.setTableView(reopened);
        assertSame(reopened.getXAxis(), table.getXAxis().getTableView());
        assertSame(reopened.getYAxis(), table.getYAxis().getTableView());

        table.setTableView(null);
        assertNull(table.getXAxis().getTableView());
        assertNull(table.getYAxis().getTableView());
    }

    @Test
    public void aViewOfATableWithOneAxisIsReachedThroughItsAxisTable() {
        final Table2D table = new Table2D();
        table.setName("Curve");
        table.setAxis(axis("Speed", SPEED, 0, 3));
        final Table2DView view = new Table2DView(table);
        table.setTableView(view);
        assertSame(view.getAxis(), table.getAxis().getTableView());

        table.setTableView(null);
        assertNull(table.getAxis().getTableView());
    }

    @Test
    public void loggerDataMovesTheLiveCellOfAnOpenTable() throws Exception {
        final Table3D table = populated(surface());
        final Table3DView view = new Table3DView(table);
        table.setTableView(view);
        view.populateTableVisual();
        view.setOverlayLog(true);
        TableUpdateHandler.getInstance().registerTable(table);
        registered = table;

        update(SPEED, 27.0);
        update(LOAD, 46.0);

        assertEquals(2, view.getXAxis().getLiveDataIndex());
        assertEquals(1, view.getYAxis().getLiveDataIndex());
    }

    @Test
    public void tablesStayRegisteredWhenTheLoggerCloses() throws Exception {
        final Table3D table = populated(surface());
        final Table3DView view = new Table3DView(table);
        table.setTableView(view);
        view.populateTableVisual();
        view.setOverlayLog(true);
        TableUpdateHandler.getInstance().registerTable(table);
        registered = table;

        TableUpdateHandler.getInstance().cleanUp();
        update(SPEED, 21.0);

        assertEquals(1, view.getXAxis().getLiveDataIndex());
    }

    @Test
    public void aLiveValueReachesAViewOnTheEventDispatchThread()
            throws Exception {
        final RecordingView view = recordingView();

        update(SPEED, 27.0);

        assertEquals(Arrays.asList("27.00"), view.values);
        assertEquals(Arrays.asList(true), view.onEventDispatchThread);
    }

    @Test
    public void aBusyViewShowsTheLatestLiveValueOnly() throws Exception {
        final RecordingView view = recordingView();
        final CountDownLatch busy = occupyTheEventDispatchThread();

        send(SPEED, 11.0);
        send(SPEED, 21.0);
        send(SPEED, 29.0);
        busy.countDown();
        settle();

        assertEquals(Arrays.asList("29.00"), view.values);
    }

    @Test
    public void aViewUnboundBeforeItsTurnIsLeftAlone() throws Exception {
        final RecordingView view = recordingView();
        final CountDownLatch busy = occupyTheEventDispatchThread();

        send(SPEED, 27.0);
        view.getTable().setTableView(null);
        busy.countDown();
        settle();

        assertTrue(view.values.isEmpty());
    }

    private RecordingView recordingView() {
        final Table1D table = axis("Speed", SPEED, 0, 3);
        final RecordingView view = new RecordingView(table);
        table.setTableView(view);
        TableUpdateHandler.getInstance().registerTable(table);
        registered = table;
        return view;
    }

    private static CountDownLatch occupyTheEventDispatchThread() {
        final CountDownLatch busy = new CountDownLatch(1);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    busy.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        return busy;
    }

    private static void update(String id, double value) throws Exception {
        send(id, value);
        settle();
    }

    private static void send(String id, double value) {
        final ResponseImpl response = new ResponseImpl();
        response.setDataValue(loggerData(id), value);
        TableUpdateHandler.getInstance().handleDataUpdate(response);
    }

    private static void settle() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    private static boolean editorWindowIsMissing(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            for (StackTraceElement frame : t.getStackTrace()) {
                if (ECUEditorManager.class.getName().equals(
                        frame.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static final class RecordingView extends Table1DView {
        private static final long serialVersionUID = 1L;
        final List<String> values = new CopyOnWriteArrayList<String>();
        final List<Boolean> onEventDispatchThread =
                new CopyOnWriteArrayList<Boolean>();

        RecordingView(Table1D table) {
            super(table, Table1DType.NO_AXIS);
        }

        @Override
        public void highlightLiveData(String liveVal) {
            values.add(liveVal);
            onEventDispatchThread.add(SwingUtilities.isEventDispatchThread());
        }
    }

    private static LoggerData loggerData(String id) {
        final String name = id.substring(2).replace('_', ' ');
        final UdpDataItem item =
                UdpDataItem.parse("key|" + name + "|unit|0.00|0|100|10");
        final LoggerData data = new ExternalDataImpl(item, new UdpDataSource());
        assertEquals(id, data.getId());
        return data;
    }

    private static Table3D surface() {
        final Table3D table = new Table3D();
        table.setName("Surface");
        table.setCategory("Test");
        table.setStorageAddress(5);
        table.setStorageType(1);
        table.setEndian(Settings.Endian.BIG);
        table.setXAxis(axis("Speed", SPEED, 0, 3));
        table.setYAxis(axis("Load", LOAD, 3, 2));
        table.setSizeX(3);
        table.setSizeY(2);
        return table;
    }

    private static Table1D axis(String name, String logParam, int address,
            int size) {
        final Table1D axis = new Table1D();
        axis.setName(name);
        axis.setLogParam(logParam);
        axis.setStorageAddress(address);
        axis.setStorageType(1);
        axis.setEndian(Settings.Endian.BIG);
        axis.setDataSize(size);
        return axis;
    }

    private static Table3D populated(Table3D table) {
        final Rom rom = new Rom(new RomID());
        rom.setFileName("image.bin");
        rom.addTableByName(table);
        rom.populateTables(IMAGE, new JProgressPane());
        // the editor registers a table again when it opens the frame
        TableUpdateHandler.getInstance().deregisterTable(table);
        return table;
    }
}
