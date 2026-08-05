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

package com.romraider.swing;

import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import com.romraider.editor.ecu.ECUEditor;
import com.romraider.editor.ecu.ECUEditorManager;
import com.romraider.maps.Rom;
import com.romraider.util.SettingsManager;

public class RomTree extends JTree implements MouseListener {

    private static final long serialVersionUID = 1630446543383498886L;
    private transient TreePath pressedPath;
    private transient boolean mousePressCaptured;

    public RomTree(DefaultMutableTreeNode input) {
        this(input, SettingsManager.getSettings().getTableClickCount());
    }

    RomTree(DefaultMutableTreeNode input, int tableClickCount) {
        super(input);
        setRootVisible(false);
        setRowHeight(0);
        addMouseListener(this);
        setCellRenderer(new RomCellRenderer());
        setFont(new Font("Tahoma", Font.PLAIN, 11));
        setToggleClickCount(tableClickCount);

        // key binding actions
        Action tableSelectAction = new AbstractAction() {
            private static final long serialVersionUID = -6007532264821746092L;

            @Override
            public void actionPerformed(ActionEvent e) {
                Object selectedRow = getLastSelectedPathComponent();
                /* if nothing is selected */
                if (selectedRow == null) {
                    return;
                }

                if (selectedRow instanceof TableTreeNode) {
                    showTable((TableTreeNode)selectedRow);
                }
                setLastSelectedRom(selectedRow);
            }
        };

        this.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter");
        this.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "space");
        this.getActionMap().put("enter", tableSelectAction);
        this.getActionMap().put("space", tableSelectAction);
    }

    public ECUEditor getEditor() {
        return ECUEditorManager.getECUEditor();
    }

    public void insertRom(Rom rom) {
        insertRootNode(rom);
    }

    void insertRootNode(DefaultMutableTreeNode node) {
        DefaultMutableTreeNode root = getRootNode();
        getTreeModel().insertNodeInto(node, root, root.getChildCount());
    }

    public boolean containsRom(Rom rom) {
        return rom != null && getRootNode().getIndex(rom) >= 0;
    }

    public Rom removeRom(Rom rom) {
        return (Rom) removeRootNode(rom);
    }

    DefaultMutableTreeNode removeRootNode(DefaultMutableTreeNode node) {
        DefaultMutableTreeNode root = getRootNode();
        int removedIndex = root.getIndex(node);
        if (removedIndex < 0) {
            return null;
        }

        clearSelection();
        getTreeModel().removeNodeFromParent(node);
        if (root.getChildCount() == 0) {
            return null;
        }

        int nextIndex = Math.min(removedIndex, root.getChildCount() - 1);
        DefaultMutableTreeNode nextNode =
                (DefaultMutableTreeNode) root.getChildAt(nextIndex);
        TreePath nextPath = new TreePath(nextNode.getPath());
        setSelectionPath(nextPath);
        scrollPathToVisible(nextPath);
        return nextNode;
    }

    private DefaultMutableTreeNode getRootNode() {
        return (DefaultMutableTreeNode) getTreeModel().getRoot();
    }

    private DefaultTreeModel getTreeModel() {
        return (DefaultTreeModel) getModel();
    }

    @Override
    protected void processMouseEvent(MouseEvent e) {
        if (e.getID() == MouseEvent.MOUSE_PRESSED) {
            mousePressCaptured = true;
            pressedPath = SwingUtilities.isLeftMouseButton(e)
                    ? getPathForRowAt(e.getY()) : null;
        }

        try {
            super.processMouseEvent(e);
            if (e.getID() == MouseEvent.MOUSE_PRESSED) {
                handlePressedTable(e);
            }
        } finally {
            if (e.getID() == MouseEvent.MOUSE_CLICKED) {
                pressedPath = null;
                mousePressCaptured = false;
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        TreePath treePath = mousePressCaptured ? pressedPath : null;
        if (treePath == null) {
            return; // this happens if we click in the empty area
        }

        setSelectionPath(treePath);
        Object selectedRow = treePath.getLastPathComponent();
        /* if nothing is selected */
        if (selectedRow == null) {
            return;
        }

        if (selectedRow instanceof TableTreeNode) {
            return;
        }

        setLastSelectedRom(selectedRow);
    }

    private void handlePressedTable(MouseEvent e) {
        if (!SwingUtilities.isLeftMouseButton(e) || pressedPath == null) {
            return;
        }

        Object selectedRow = pressedPath.getLastPathComponent();
        if (selectedRow instanceof TableTreeNode
                && getRomNode((TableTreeNode) selectedRow) != null) {
            setSelectionPath(pressedPath);
            setLastSelectedRom(selectedRow);
            if (isConfiguredTableClick(e)) {
                showTable((TableTreeNode) selectedRow);
            }
        }
    }

    private TreePath getPathForRowAt(int y) {
        int row = getClosestRowForLocation(0, y);
        if (row < 0) {
            return null;
        }

        Rectangle bounds = getRowBounds(row);
        if (bounds == null || y < bounds.y || y >= bounds.y + bounds.height) {
            return null;
        }
        return getPathForRow(row);
    }

    private boolean isConfiguredTableClick(MouseEvent e) {
        int clickCount = Math.max(
                1, SettingsManager.getSettings().getTableClickCount());
        return SwingUtilities.isLeftMouseButton(e)
                && e.getClickCount() >= clickCount;
    }

    protected void showTable(TableTreeNode selectedRow) {
        getEditor().displayTable(selectedRow);
    }

    protected void setLastSelectedRom(Object selectedNode) {
        if (selectedNode == null || selectedNode instanceof RomTreeRootNode) {
            return;
        }

        Rom romNode = getRomNode(selectedNode);
        if (romNode == null) {
            return;
        }
        ECUEditor editor = getEditor();
        if (editor.getLastSelectedRom() != romNode) {
            editor.setLastSelectedRom(romNode);
            editor.refreshUI();
        }
    }

    public static Rom getRomNode(Object currentNode){
        if (currentNode == null) {
            return null;
        } else if(currentNode instanceof Rom) {
            return (Rom)currentNode;
        } else if(currentNode instanceof TableTreeNode) {
            return getRomNode(((TableTreeNode)currentNode).getParent());
        } else if(currentNode instanceof CategoryTreeNode) {
            return getRomNode(((CategoryTreeNode)currentNode).getParent());
        } else {
            return null;
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

}
