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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ExternalDataSourceLoaderImplTest {
    private static final String UDP_CLASS =
            "com.romraider.logger.external.udp.plugin.UdpDataSource";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static File write(File directory, String name, String sourceName)
            throws IOException {
        final File file = new File(directory, name);
        final OutputStream stream = new FileOutputStream(file);
        try {
            stream.write(("datasource.class=" + UDP_CLASS + "\n"
                    + "udp.name=" + sourceName + "\n"
                    + "udp.port=0\n"
                    + "udp.item.1=rpm|Engine Speed|RPM|0|0|8000|1000\n")
                    .getBytes(StandardCharsets.ISO_8859_1));
        } finally {
            stream.close();
        }
        return file;
    }

    private static List<String> names(ExternalDataSourceLoaderImpl loader) {
        final List<String> names = new ArrayList<String>();
        for (ExternalDataSource source : loader.getExternalDataSources()) {
            names.add(source.getName());
        }
        return names;
    }

    @Test
    public void defaultFoldersAreTheInstallationAndThePerUserFolder() {
        final List<File> folders =
                ExternalDataSourceLoaderImpl.defaultPluginsDirs();
        assertEquals(2, folders.size());
        assertEquals(new File(".", "plugins"), folders.get(0));
        assertEquals(
                new File(new File(System.getProperty("user.home"),
                        ".RomRaider"), "plugins"),
                folders.get(1));
    }

    @Test
    public void pluginFilesOfEveryFolderAreLoaded() throws IOException {
        final File installation = folder.newFolder("installation");
        final File user = folder.newFolder("user");
        write(installation, "shipped.plugin", "Shipped");
        write(installation, "notes.txt", "Ignored");
        write(user, "telemetry.plugin", "Telemetry");

        final ExternalDataSourceLoaderImpl loader =
                new ExternalDataSourceLoaderImpl(
                        Arrays.asList(installation, user));
        loader.loadExternalDataSources(null);

        assertEquals(Arrays.asList("Shipped", "Telemetry"), names(loader));
    }

    @Test
    public void aPerUserFileReplacesAnInstallationFileOfTheSameName()
            throws IOException {
        final File installation = folder.newFolder("installation");
        final File user = folder.newFolder("user");
        write(installation, "a.plugin", "First");
        write(installation, "telemetry.plugin", "Installation");
        final File replacement = write(user, "telemetry.plugin", "User");

        final List<File> files =
                ExternalDataSourceLoaderImpl.pluginPropertyFiles(
                        Arrays.asList(installation, user));

        assertEquals(2, files.size());
        assertEquals("a.plugin", files.get(0).getName());
        assertEquals(replacement, files.get(1));
    }

    @Test
    public void aMissingFolderIsSkipped() throws IOException {
        final File user = folder.newFolder("user");
        write(user, "telemetry.plugin", "Telemetry");
        final File missing = new File(folder.getRoot(), "missing");
        final File notAFolder = write(user, "file.plugin", "File");

        final List<File> files =
                ExternalDataSourceLoaderImpl.pluginPropertyFiles(
                        Arrays.asList(missing, notAFolder, user));

        assertEquals(2, files.size());
        assertTrue(ExternalDataSourceLoaderImpl.pluginPropertyFiles(
                Arrays.asList(missing)).isEmpty());
    }
}
