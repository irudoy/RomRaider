/*
 * RomRaider Open-Source Tuning, Logging and Reflashing
 * Copyright (C) 2006-2016 RomRaider.com
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

import static com.romraider.util.ParamChecker.isNullOrEmpty;
import static java.util.Collections.synchronizedMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import com.romraider.logger.ecu.comms.query.Response;
import com.romraider.logger.ecu.definition.LoggerData;
import com.romraider.logger.ecu.ui.handler.DataUpdateHandler;
import com.romraider.maps.Table;
import com.romraider.maps.Table2D;
import com.romraider.maps.Table3D;
import com.romraider.maps.TableView;

public final class TableUpdateHandler implements DataUpdateHandler {
    private static final Logger LOGGER = Logger.getLogger(TableUpdateHandler.class);
    private static final TableUpdateHandler INSTANCE = new TableUpdateHandler();
    private final Map<String, List<Table>> tableMap = synchronizedMap(new HashMap<String, List<Table>>());
    // the latest live value of each view that waits for the event dispatch thread
    private final Map<TableView, String> pending = new LinkedHashMap<TableView, String>();
    private final Set<String> reported = new HashSet<String>();

    private TableUpdateHandler() {
        tableMap.clear();
    }

    @Override
    public void registerData(LoggerData loggerData) {
    }

    @Override
    public void handleDataUpdate(Response response) {
    	if(!tableMap.isEmpty()) {
	        for (LoggerData loggerData : response.getData()) {
	        	synchronized(tableMap) {
		            List<Table> tables = tableMap.get(loggerData.getId());
		            if (tables != null && !tables.isEmpty()) {
		                String formattedValue = loggerData.getSelectedConvertor().format(response.getDataValue(loggerData));
		                for(ListIterator<Table> item = tables.listIterator(); item.hasNext();) {
		                	TableView v = item.next().getTableView();
		                	if(v!= null) post(v, formattedValue);
		                }
		            }
		        }
	    	}
	    }
    }

    // Table cells are Swing components that share one number format, so a
    // live value is shown on the event dispatch thread. A view that is still
    // waiting for its turn shows the latest value only.
    private void post(TableView view, String value) {
        final boolean idle;
        synchronized (pending) {
            idle = pending.isEmpty();
            pending.put(view, value);
        }
        if (idle) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    show();
                }
            });
        }
    }

    private void show() {
        final Map<TableView, String> values;
        synchronized (pending) {
            values = new LinkedHashMap<TableView, String>(pending);
            pending.clear();
        }
        for (Map.Entry<TableView, String> value : values.entrySet()) {
            final TableView view = value.getKey();
            final Table table = view.getTable();
            // a table closed since the value arrived has no view to update
            if (table == null || table.getTableView() != view) continue;
            try {
                view.highlightLiveData(value.getValue());
            } catch (RuntimeException e) {
                final String failure = e.getClass().getName();
                if (reported.add(failure)) {
                    LOGGER.error("Live data highlight error, reported once", e);
                }
            }
        }
    }

    @Override
    public void deregisterData(LoggerData loggerData) {
    }

    @Override
    public void cleanUp() {
        // tables are registered by the editor and outlive a Logger session
    }

    @Override
    public void reset() {
    }

    public void registerTable(Table table) {
        String logParam = table.getLogParam();
        if (!isNullOrEmpty(logParam)) {
            if (!tableMap.containsKey(logParam)) {
                tableMap.put(logParam, new ArrayList<Table>());
            }
            tableMap.get(logParam).add(table);
        }
        registerAxes(table);
    }

    public void deregisterTable(Table table) {
    	if(table == null) return;
    	
        String logParam = table.getLogParam();
        if (tableMap.containsKey(logParam)) {
            List<Table> tables = tableMap.get(logParam);
            tables.remove(table);
            if (tables.isEmpty()) {
                tableMap.remove(logParam);
            }
        }
        deregisterAxes(table);
    }

    public static TableUpdateHandler getInstance() {
        return INSTANCE;
    }

    private void registerAxes(Table table) {
        if (table instanceof Table2D) {
            registerTable(((Table2D) table).getAxis());
        }
        if (table instanceof Table3D) {
            registerTable(((Table3D) table).getXAxis());
            registerTable(((Table3D) table).getYAxis());
        }
    }

    private void deregisterAxes(Table table) {
        if (table instanceof Table2D) {
            deregisterTable(((Table2D) table).getAxis());
        }
        if (table instanceof Table3D) {
            deregisterTable(((Table3D) table).getXAxis());
            deregisterTable(((Table3D) table).getYAxis());
        }
    }

}
