/*
 * RomRaider Open-Source Tuning, Logging and Reflashing
 * Copyright (C) 2006-2025 RomRaider.com
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

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.ResourceBundle;

import com.romraider.maps.Rom;
import com.romraider.util.ResourceUtil;

public class RomFilterPanel extends JPanel {

	private static final long serialVersionUID = 1L;

    private static final ResourceBundle rb = new ResourceUtil().getBundle(
    		RomFilterPanel.class.getName());

    private final DefaultMutableTreeNode imageRoot;
    private final RomTree imageList;
    private final JTextField filterField = new JTextField(20);
	
    public RomFilterPanel(
            DefaultMutableTreeNode imageRoot, RomTree imageList) {
        super(new BorderLayout());
        this.imageRoot = imageRoot;
        this.imageList = imageList;

        JLabel label = new JLabel(rb.getString("LBLFILTER"));
        label.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        filterField.setToolTipText(rb.getString("LBLTOOLTIP"));

        add(label, BorderLayout.WEST);
        add(filterField, BorderLayout.CENTER);

        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { filter(); }
            @Override
            public void removeUpdate(DocumentEvent e) { filter(); }
            @Override
            public void changedUpdate(DocumentEvent e) { filter(); }

            private void filter() {
                refreshDisplayedTables();
            }
        });
    }

    /**
     * Rebuilds the visible table nodes using the current filter and notifies
     * the Swing tree model about every changed ROM node.
     */
    public void refreshDisplayedTables() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException(
                    "ROM table tree must be refreshed on the event dispatch thread");
        }

        String filterText = filterField.getText().trim();
        boolean filtering = !filterText.isEmpty();
        DefaultTreeModel model = (DefaultTreeModel) imageList.getModel();

        final Enumeration<?> children = imageRoot.children();
        while (children.hasMoreElements()) {
            Object child = children.nextElement();
            if (!(child instanceof Rom)) {
                continue;
            }

            Rom rom = (Rom) child;
            ExpandedPaths expandedPaths = captureExpandedPaths(rom);
            List<TreePath> filterPaths =
                    rom.refreshDisplayedTables(filterText);
            model.reload(rom);

            if (filtering) {
                imageList.expandPath(new TreePath(rom.getPath()));
                for (TreePath path : filterPaths) {
                    imageList.expandPath(path);
                }
            } else {
                restoreExpandedPaths(rom, expandedPaths);
            }
        }

        imageList.repaint();
    }

    private ExpandedPaths captureExpandedPaths(Rom rom) {
        TreePath romPath = new TreePath(rom.getPath());
        ExpandedPaths result = new ExpandedPaths(
                imageList.isExpanded(romPath));
        Enumeration<TreePath> paths =
                imageList.getExpandedDescendants(romPath);
        if (paths == null) {
            return result;
        }

        while (paths.hasMoreElements()) {
            Object[] components = paths.nextElement().getPath();
            List<String> categoryPath = new ArrayList<String>();
            boolean afterRom = false;
            for (Object component : components) {
                if (afterRom && component instanceof CategoryTreeNode) {
                    categoryPath.add(component.toString());
                }
                if (component == rom) {
                    afterRom = true;
                }
            }
            if (!categoryPath.isEmpty()) {
                result.categoryPaths.add(categoryPath);
            }
        }
        return result;
    }

    private void restoreExpandedPaths(Rom rom, ExpandedPaths expandedPaths) {
        if (!expandedPaths.romExpanded) {
            return;
        }

        imageList.expandPath(new TreePath(rom.getPath()));
        for (List<String> categoryPath : expandedPaths.categoryPaths) {
            DefaultMutableTreeNode current = rom;
            for (String category : categoryPath) {
                current = findCategory(current, category);
                if (current == null) {
                    break;
                }
                imageList.expandPath(new TreePath(current.getPath()));
            }
        }
    }

    private DefaultMutableTreeNode findCategory(
            DefaultMutableTreeNode parent, String category) {
        for (int index = 0; index < parent.getChildCount(); index++) {
            Object child = parent.getChildAt(index);
            if (child instanceof CategoryTreeNode
                    && child.toString().equalsIgnoreCase(category)) {
                return (DefaultMutableTreeNode) child;
            }
        }
        return null;
    }

    private static final class ExpandedPaths {
        private final boolean romExpanded;
        private final List<List<String>> categoryPaths =
                new ArrayList<List<String>>();

        private ExpandedPaths(boolean romExpanded) {
            this.romExpanded = romExpanded;
        }
    }
}
