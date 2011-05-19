/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.ins.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;

import com.sun.max.*;
import com.sun.max.ins.*;
import com.sun.max.ins.util.*;

/**
 * An abstract class for a dialog that presents the user a list that can
 * be refined with a text field which is used as a pattern to filter the list.
 *
 * @author Doug Simon
 */
public abstract class FilteredListDialog<Type> extends InspectorDialog {

    protected final boolean multiSelection;
    protected final JTextField textField = new JTextField();
    protected final String actionName;
    protected final DefaultListModel listModel = new DefaultListModel();
    protected final JList list = new JList(EMPTY_LIST_MODEL);

    private List<Type> selectedObjects;

    /**
     * The value representing that no object was selected.
     */
    protected abstract Type noSelectedObject();

    /**
     * Gets the object selected by the user.
     *
     * @return the object selected by the user or the value returned by {@link #noSelectedObject()} if the dialog was canceled without a selection being made; if
     * multi-selection enabled, returns the first selection.
     */
    public Type selectedObject() {
        return (selectedObjects != null && selectedObjects.size() > 0) ? Utils.first(selectedObjects) :  noSelectedObject();
    }

    public List<Type> selectedObjects() {
        return selectedObjects;
    }

    /**
     * A subclass overwrites this method to convert a selected item from the list to the
     * {@linkplain #selectedObject() selected object}.
     *
     * @param listItem
     *                the item currently selected when the user pressed the "Select" button that closed the dialog
     */
    protected abstract Type convertSelectedItem(Object listItem);

    private final class SelectAction extends InspectorAction {
        private SelectAction() {
            super(inspection(), actionName);
        }

        @Override
        protected void procedure() {
            final int[] selectedIndices = list.getSelectedIndices();
            selectedObjects = new LinkedList<Type>();
            for (int i = 0; i < selectedIndices.length; i++) {
                selectedObjects.add(convertSelectedItem(listModel.get(selectedIndices[i])));
            }
            dispose();
        }
    }

    private final class CancelAction extends InspectorAction {
        private CancelAction() {
            super(inspection(), "Cancel");
        }

        @Override
        protected void procedure() {
            dispose();
        }
    }

    /**
     * Used as the place holder list model while the real list model is being notified. Using a place holder prevents
     * events being sent to the list for each modification to its (potentially large) model.
     */
    private static final ListModel EMPTY_LIST_MODEL = new AbstractListModel() {
        public int getSize() {
            return 0;
        }

        public Object getElementAt(int i) {
            return "No Data Model";
        }
    };

    private class TextListener implements DocumentListener {
        public void changedUpdate(DocumentEvent e) {
            // This should never be called
            InspectorError.unexpected();
        }

        public void insertUpdate(DocumentEvent event) {
            rebuildList();
        }

        public void removeUpdate(DocumentEvent e) {
            rebuildList();
        }
    }

    /**
     * Overwritten by subclasses to update the list model when the filter input field is updated.
     *
     * @param filterText the value of the filter input field
     */
    protected abstract void rebuildList(String filterText);

    /**
     * Rebuilds the list.
     */
    protected void rebuildList() {
        list.setModel(EMPTY_LIST_MODEL);
        listModel.clear();
        final String text = textField.getText();
        rebuildList(text);
        list.setModel(listModel);
        if (!listModel.isEmpty() && list.getSelectedIndex() == -1) {
            list.setSelectedIndex(0);
        }
    }

    protected FilteredListDialog(Inspection inspection, String title, String filterFieldLabelText, String actionName, boolean multiSelection) {
        super(inspection, title, true);
        this.multiSelection = multiSelection;
        if (actionName == null) {
            this.actionName = "Select";
        } else {
            this.actionName = actionName;
        }

        final JPanel dialogPanel = new InspectorPanel(inspection, new BorderLayout());
        final JPanel textPanel = new InspectorPanel(inspection);

        TextLabel filterFieldLabel = new TextLabel(inspection, filterFieldLabelText + ":");
        textPanel.add(filterFieldLabel);
        filterFieldLabel.setToolTipText(filterFieldLabelTooltip());
        textField.setPreferredSize(new Dimension(500, 30));
        textField.getDocument().addDocumentListener(new TextListener());
        textPanel.add(textField);
        dialogPanel.add(textPanel, BorderLayout.NORTH);

        // Allow the user to press DOWN to navigate forward out of the text box into the list (a la Eclipse)
        final Set<AWTKeyStroke> forwardTraversalKeys = new HashSet<AWTKeyStroke>(textField.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
        forwardTraversalKeys.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_DOWN, 0));
        textField.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, forwardTraversalKeys);

        list.setSelectedIndex(0);
        list.setVisibleRowCount(10);
        if (multiSelection) {
            list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        } else {
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        }
        list.setLayoutOrientation(JList.VERTICAL);
        final JScrollPane scrollPane = new InspectorScrollPane(inspection, list);
        scrollPane.setPreferredSize(new Dimension(550, 425));
        dialogPanel.add(scrollPane, BorderLayout.CENTER);

        final JPanel buttonPanel = new InspectorPanel(inspection);
        final JButton selectButton = new JButton(new SelectAction());
        final JButton cancelButton = new JButton(new CancelAction());
        buttonPanel.add(selectButton);
        buttonPanel.add(cancelButton);
        dialogPanel.add(buttonPanel, BorderLayout.SOUTH);

        // The select button is only enabled and focusable when the list has a selected item
        selectButton.setEnabled(false);
        selectButton.setFocusable(false);

        // Make pressing "Enter" equivalent to pressing the "Select" button.
        getRootPane().setDefaultButton(selectButton);

        // Make pressing "Escape" equivalent to pressing the "Cancel" button.
        getRootPane().registerKeyboardAction(cancelButton.getAction(), KeyStroke.getKeyStroke((char) KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        // Disable the "Select" button if there is no selected item in the list.
        list.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                final boolean hasSelection = list.getSelectedIndex() != -1;
                selectButton.setEnabled(hasSelection);
                selectButton.setFocusable(hasSelection);
            }
        });

        // Make double clicking an item in the list equivalent to pressing the "Select" button.
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    final int index = list.locationToIndex(e.getPoint());
                    if (index != -1) {
                        selectButton.doClick();
                    }
                }
            }
        });

        final FocusTraversalPolicy focusTraversalPolicy = new ExplicitFocusTraversalPolicy(textField, list, selectButton, cancelButton);
        setFocusTraversalPolicy(focusTraversalPolicy);

        setContentPane(dialogPanel);
        pack();
        inspection.gui().moveToMiddle(this);
    }

    protected String filterFieldLabelTooltip() {
        return null;
    }
}
