/*
 * RomRaider Open-Source Tuning, Logging and Reflashing
 * Copyright (C) 2006-2015 RomRaider.com
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

import com.romraider.logger.ecu.definition.plugin.PluginFilenameFilter;
import com.romraider.logger.ecu.exception.ConfigurationException;
import com.romraider.logger.ecu.exception.PluginNotInstalledException;

import static com.romraider.util.ParamChecker.isNullOrEmpty;
import org.apache.log4j.Logger;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public final class ExternalDataSourceLoaderImpl implements ExternalDataSourceLoader {
    private static final Logger LOGGER = Logger.getLogger(ExternalDataSourceLoaderImpl.class);
    private static final String PLUGINS_DIR = "plugins";
    private final List<File> pluginsDirs;
    private List<ExternalDataSource> externalDataSources = new ArrayList<ExternalDataSource>();

    public ExternalDataSourceLoaderImpl() {
        this(defaultPluginsDirs());
    }

    ExternalDataSourceLoaderImpl(List<File> pluginsDirs) {
        this.pluginsDirs = pluginsDirs;
    }

    /**
     * The installation folder holds the plugin files RomRaider ships. The
     * per-user folder beside the settings holds plugin files of other
     * programs, so they stay in place when the installation is replaced.
     */
    static List<File> defaultPluginsDirs() {
        return Arrays.asList(
                new File(".", PLUGINS_DIR),
                new File(new File(System.getProperty("user.home"), ".RomRaider"), PLUGINS_DIR));
    }

    /** A later folder replaces a plugin file of the same name from an earlier one. */
    static List<File> pluginPropertyFiles(List<File> pluginsDirs) {
        Map<String, File> filesByName = new LinkedHashMap<String, File>();
        for (File pluginsDir : pluginsDirs) {
            if (!pluginsDir.isDirectory()) continue;
            File[] files = pluginsDir.listFiles(new PluginFilenameFilter());
            if (files == null) continue;
            Arrays.sort(files);
            for (File file : files) {
                filesByName.remove(file.getName());
                filesByName.put(file.getName(), file);
            }
        }
        return new ArrayList<File>(filesByName.values());
    }

    public void loadExternalDataSources(Map<String, String> loggerPluginPorts) {
        try {
            for (File pluginPropertyFile : pluginPropertyFiles(pluginsDirs)) {
                Properties pluginProps = new Properties();
                FileInputStream inputStream = new FileInputStream(pluginPropertyFile);
                try {
                    pluginProps.load(inputStream);
                    String datasourceClassName = pluginProps.getProperty("datasource.class");
                    if (!isNullOrEmpty(datasourceClassName)) {
                        try {
                            Class<?> dataSourceClass = getClass().getClassLoader().loadClass(datasourceClassName);
                            if (dataSourceClass != null && ExternalDataSource.class.isAssignableFrom(dataSourceClass)) {
                                ExternalDataSource dataSource = dataSource(dataSourceClass, loggerPluginPorts, pluginProps);
                                ExternalDataSource managedDataSource = new GenericDataSourceManager(dataSource);
                                externalDataSources.add(managedDataSource);
                                LOGGER.info("Plugin loaded: " + dataSource.getName() + " v" + dataSource.getVersion());
                            }
                        }
                        catch (PluginNotInstalledException e) {
                            LOGGER.warn(e.getMessage());
                        }
                        catch (Throwable t) {
                            LOGGER.error("Error loading external datasource: " + datasourceClassName + ", specified in: "
                                    + pluginPropertyFile.getAbsolutePath(), t);
                        }
                    }
                } finally {
                    inputStream.close();
                }
            }
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }
    }

    private ExternalDataSource dataSource(
            Class<?> dataSourceClass,
            Map<String, String> loggerPluginPorts,
            Properties pluginProps) throws Exception {

        ExternalDataSource dataSource = (ExternalDataSource) dataSourceClass.newInstance();
        if (loggerPluginPorts != null) {
            String port = loggerPluginPorts.get(dataSource.getId());
            if (port != null && port.trim().length() > 0) dataSource.setPort(port);
        }
        dataSource.setProperties(pluginProps);
        return dataSource;
    }

    public List<ExternalDataSource> getExternalDataSources() {
        return externalDataSources;
    }
}
