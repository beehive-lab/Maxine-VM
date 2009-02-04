/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.ins.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import com.sun.max.collect.*;
import com.sun.max.ins.*;
import com.sun.max.program.*;

/**
 * An abstract base class for a dialog that presents the user a list that can be refined with a text field that can be used to filter the list.
 *
 * @author Doug Simon
 */
public abstract class FilteredListDialog<Type> extends InspectorDialog {

    protected final boolean _multiSelection;
    protected final JTextField _textField = new JTextField();
    protected final String _actionName;
    protected final DefaultListModel _listModel = new DefaultListModel();
    protected final JList _list = new JList(EMPTY_LIST_MODEL);

    private AppendableSequence<Type> _selectedObjects;

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
        return (_selectedObjects != null && _selectedObjects.length() > 0) ? _selectedObjects.first() :  noSelectedObject();
    }

    public Sequence<Type> selectedObjects() {
        return _selectedObjects;
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
            super(inspection(), _actionName);
        }

        @Override
        protected void procedure() {
            final int[] selectedIndices = _list.getSelectedIndices();
            _selectedObjects = new LinkSequence<Type>();
            for (int i = 0; i < selectedIndices.length; i++) {
                _selectedObjects.append(convertSelectedItem(_listModel.get(selectedIndices[i])));
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
        @Override
        public int getSize() {
            return 0;
        }
        @Override
        public Object getElementAt(int i) {
            return "No Data Model";
        }
    };

    private class TextListener implements DocumentListener {
        public void changedUpdate(DocumentEvent e) {
            // This should never be called
            ProgramError.unexpected();
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
        _list.setModel(EMPTY_LIST_MODEL);
        _listModel.clear();
        final String text = _textField.getText();
        rebuildList(text);
        _list.setModel(_listModel);
        if (!_listModel.isEmpty() && _list.getSelectedIndex() == -1) {
            _list.setSelectedIndex(0);
        }
    }

    protected FilteredListDialog(Inspection inspection, String title, String filterFieldLabel, String actionName, boolean multiSelection) {
        super(inspection, title, true);
        _multiSelection = multiSelection;
        if (actionName == null) {
            _actionName = "Select";
        } else {
            _actionName = actionName;
        }

        final JPanel dialogPanel = new InspectorPanel(inspection, new BorderLayout());
        final JPanel textPanel = new InspectorPanel(inspection);

        textPanel.add(new TextLabel(inspection, filterFieldLabel + ":"));
        _textField.setFont(style().javaClassNameFont());
        _textField.setPreferredSize(new Dimension(500, 30));
        _textField.getDocument().addDocumentListener(new TextListener());
        textPanel.add(_textField);
        dialogPanel.add(textPanel, BorderLayout.NORTH);

        // Allow the user to press DOWN to navigate forward out of the text box into the list (a la Eclipse)
        final Set<AWTKeyStroke> forwardTraversalKeys = new HashSet<AWTKeyStroke>(_textField.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
        forwardTraversalKeys.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_DOWN, 0));
        _textField.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, forwardTraversalKeys);

        _list.setSelectedIndex(0);
        _list.setVisibleRowCount(10);
        if (multiSelection) {
            _list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        } else {
            _list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        }
        _list.setLayoutOrientation(JList.VERTICAL);
        final JScrollPane scrollPane = new InspectorScrollPane(inspection, _list);
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
        _list.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                final boolean hasSelection = _list.getSelectedIndex() != -1;
                selectButton.setEnabled(hasSelection);
                selectButton.setFocusable(hasSelection);
            }
        });

        // Make double clicking an item in the list equivalent to pressing the "Select" button.
        _list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    final int index = _list.locationToIndex(e.getPoint());
                    if (index != -1) {
                        selectButton.doClick();
                    }
                }
            }
        });

        final FocusTraversalPolicy focusTraversalPolicy = new ExplicitFocusTraversalPolicy(_textField, _list, selectButton, cancelButton);
        setFocusTraversalPolicy(focusTraversalPolicy);

        setContentPane(dialogPanel);
        pack();
        inspection.moveToMiddle(this);
    }
}
