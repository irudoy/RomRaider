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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.junit.Test;

public class RomTreeTest {

    @Test
    public void romChangesNotifyTreeAndSelectAdjacentRom() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                RomTreeRootNode root = new RomTreeRootNode("root");
                RomTree tree = new RomTree(root, 1);
                DefaultTreeModel model =
                        (DefaultTreeModel) tree.getModel();
                AtomicInteger inserted = new AtomicInteger();
                AtomicInteger removed = new AtomicInteger();
                model.addTreeModelListener(new TreeModelListener() {
                    @Override
                    public void treeNodesChanged(TreeModelEvent event) {
                    }

                    @Override
                    public void treeNodesInserted(TreeModelEvent event) {
                        inserted.incrementAndGet();
                    }

                    @Override
                    public void treeNodesRemoved(TreeModelEvent event) {
                        removed.incrementAndGet();
                    }

                    @Override
                    public void treeStructureChanged(TreeModelEvent event) {
                    }
                });

                DefaultMutableTreeNode first =
                        new DefaultMutableTreeNode("first");
                DefaultMutableTreeNode second =
                        new DefaultMutableTreeNode("second");
                DefaultMutableTreeNode third =
                        new DefaultMutableTreeNode("third");
                tree.insertRootNode(first);
                tree.insertRootNode(second);
                tree.insertRootNode(third);
                tree.expandPath(new TreePath(root.getPath()));

                assertEquals(3, inserted.get());
                assertEquals(3, root.getChildCount());
                assertEquals(3, tree.getRowCount());

                tree.setSelectionPath(new TreePath(second.getPath()));
                assertSame(third, tree.removeRootNode(second));
                assertEquals(1, removed.get());
                assertEquals(2, tree.getRowCount());
                assertSame(third, tree.getLastSelectedPathComponent());
                assertNull(second.getParent());

                assertSame(first, tree.removeRootNode(third));
                assertEquals(2, removed.get());
                assertSame(first, tree.getLastSelectedPathComponent());

                assertNull(tree.removeRootNode(first));
                assertEquals(3, removed.get());
                assertEquals(0, tree.getRowCount());
                assertNull(tree.getLastSelectedPathComponent());
            }
        });
    }
}
