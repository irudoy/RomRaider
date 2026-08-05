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

package com.romraider.swing;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Component;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.junit.BeforeClass;
import org.junit.Test;

import com.romraider.Settings;
import com.romraider.maps.Rom;
import com.romraider.maps.RomID;
import com.romraider.maps.Table1D;
import com.romraider.util.SettingsManager;

public class RomFilterPanelTest {

    @BeforeClass
    public static void createSettingsFile() throws Exception {
        Path settingsDirectory = Paths.get(
                System.getProperty("user.home"), ".RomRaider");
        Files.createDirectories(settingsDirectory);
        Path settingsFile = settingsDirectory.resolve("settings.xml");
        if (!Files.exists(settingsFile)) {
            Files.writeString(settingsFile, "<settings/>", ISO_8859_1);
        }
    }

    @Test
    public void userLevelRefreshNotifiesModelAndPreservesTreeState()
            throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                Settings settings = SettingsManager.getSettings();
                int originalUserLevel = settings.getUserLevel();
                boolean originalDisplayHighTables =
                        settings.isDisplayHighTables();
                try {
                    settings.setDisplayHighTables(false);
                    settings.setUserLevel(1);

                    RomTreeRootNode root = new RomTreeRootNode("root");
                    RomTree tree = new RomTree(root, 1);
                    new JScrollPane(tree);
                    RomFilterPanel filterPanel =
                            new RomFilterPanel(root, tree);
                    Rom rom = createRom();
                    rom.refreshDisplayedTables();
                    tree.insertRom(rom);

                    TreePath rootPath = new TreePath(root.getPath());
                    TreePath romPath = new TreePath(rom.getPath());
                    tree.expandPath(rootPath);
                    tree.expandPath(romPath);
                    DefaultMutableTreeNode basicCategory =
                            findChild(rom, "Basic");
                    assertNotNull(basicCategory);
                    tree.expandPath(new TreePath(basicCategory.getPath()));

                    AtomicInteger structureChanges = new AtomicInteger();
                    DefaultTreeModel model =
                            (DefaultTreeModel) tree.getModel();
                    model.addTreeModelListener(new TreeModelListener() {
                        @Override
                        public void treeNodesChanged(TreeModelEvent event) {
                        }

                        @Override
                        public void treeNodesInserted(TreeModelEvent event) {
                        }

                        @Override
                        public void treeNodesRemoved(TreeModelEvent event) {
                        }

                        @Override
                        public void treeStructureChanged(
                                TreeModelEvent event) {
                            structureChanges.incrementAndGet();
                        }
                    });

                    settings.setUserLevel(5);
                    filterPanel.refreshDisplayedTables();

                    assertEquals(1, structureChanges.get());
                    assertNotNull(findChild(rom, "Basic"));
                    assertNotNull(findChild(rom, "Advanced"));
                    assertTrue(tree.isExpanded(new TreePath(rom.getPath())));
                    DefaultMutableTreeNode refreshedBasic =
                            findChild(rom, "Basic");
                    assertTrue(tree.isExpanded(
                            new TreePath(refreshedBasic.getPath())));
                } finally {
                    settings.setUserLevel(originalUserLevel);
                    settings.setDisplayHighTables(
                            originalDisplayHighTables);
                }
            }
        });
    }

    @Test
    public void userLevelRefreshReappliesActiveFilter() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                Settings settings = SettingsManager.getSettings();
                int originalUserLevel = settings.getUserLevel();
                boolean originalDisplayHighTables =
                        settings.isDisplayHighTables();
                try {
                    settings.setDisplayHighTables(false);
                    settings.setUserLevel(5);

                    RomTreeRootNode root = new RomTreeRootNode("root");
                    RomTree tree = new RomTree(root, 1);
                    new JScrollPane(tree);
                    RomFilterPanel filterPanel =
                            new RomFilterPanel(root, tree);
                    Rom rom = createRom();
                    rom.refreshDisplayedTables();
                    tree.insertRom(rom);
                    tree.expandPath(new TreePath(root.getPath()));
                    tree.expandPath(new TreePath(rom.getPath()));

                    JTextField filterField = findFilterField(filterPanel);
                    filterField.setText("Advanced map");
                    assertEquals(1, rom.getChildCount());
                    assertNotNull(findChild(rom, "Advanced"));

                    settings.setUserLevel(1);
                    filterPanel.refreshDisplayedTables();

                    assertEquals(1, rom.getChildCount());
                    assertFalse(rom.getChildAt(0)
                            instanceof CategoryTreeNode);
                    assertTrue(tree.isExpanded(
                            new TreePath(rom.getPath())));
                } finally {
                    settings.setUserLevel(originalUserLevel);
                    settings.setDisplayHighTables(
                            originalDisplayHighTables);
                }
            }
        });
    }

    private static Rom createRom() {
        Rom rom = new Rom(new RomID());
        rom.addTableByName(createTable(
                "Basic map", "Basic", 1));
        rom.addTableByName(createTable(
                "Advanced map", "Advanced", 5));
        return rom;
    }

    private static Table1D createTable(
            String name, String category, int userLevel) {
        Table1D table = new Table1D();
        table.setName(name);
        table.setCategory(category);
        table.setUserLevel(userLevel);
        return table;
    }

    private static DefaultMutableTreeNode findChild(
            DefaultMutableTreeNode parent, String name) {
        for (int index = 0; index < parent.getChildCount(); index++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode)
                    parent.getChildAt(index);
            if (name.equals(child.toString())) {
                return child;
            }
        }
        return null;
    }

    private static JTextField findFilterField(
            RomFilterPanel filterPanel) {
        for (Component component : filterPanel.getComponents()) {
            if (component instanceof JTextField) {
                return (JTextField) component;
            }
        }
        throw new AssertionError("Filter text field not found");
    }
}
