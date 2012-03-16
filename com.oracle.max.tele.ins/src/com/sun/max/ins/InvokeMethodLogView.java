/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins;

import java.awt.event.*;
import java.awt.print.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

import com.sun.max.ins.gui.*;
import com.sun.max.ins.view.*;
import com.sun.max.ins.view.InspectionViews.ViewKind;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;

/**
 * A view that gives viewing and editing access to a notepad.
 * <p>
 * The view is currently a singleton, but this might be generalized
 * if multiple notepads are supported in the future.  Some of the code
 * is written to anticipate this.
 * @see InspectorInvokeMethodLog
 * @see InvokeMethodLogManager

 * TODO This was cloned from {@link NotePadView} and should be refactored.
*/
public final class InvokeMethodLogView extends AbstractView<InvokeMethodLogView> {

    private static final int TRACE_VALUE = 1;
    private static final ViewKind VIEW_KIND = ViewKind.INVOKE_METHOD_LOG;
    private static final String SHORT_NAME = "InvokeMethodLog";
    private static final String LONG_NAME = "InvokeMethodLog View";
    private static final String GEOMETRY_SETTINGS_KEY = "notepadViewGeometry";

    // A compiled regular expression pattern that matches hex numbers (with or without prefix) and ordinary
    // integers as well.
    // TODO (mlvdv) fix pattern failure at end of contents (if no newline)
    private static final Pattern hexNumberPattern = Pattern.compile("[0-9a-fA-F]+[^a-zA-Z0-9]");

    public static final class InvokeMethodLogViewManager extends AbstractSingletonViewManager<InvokeMethodLogView> {

        private final InvokeMethodLogManager notepadManager;

        protected InvokeMethodLogViewManager(Inspection inspection) {
            super(inspection, VIEW_KIND, SHORT_NAME, LONG_NAME);
            notepadManager = new InvokeMethodLogManager(inspection);
        }

        @Override
        protected InvokeMethodLogView createView(Inspection inspection) {
            invokeMethodLogView = new InvokeMethodLogView(inspection, notepadManager.getInvokeMethodLog());
            return invokeMethodLogView;
        }

    }

    // Will be non-null before any instances created.
    private static InvokeMethodLogViewManager viewManager = null;

    public static InvokeMethodLogViewManager makeViewManager(Inspection inspection) {
        if (viewManager == null) {
            viewManager = new InvokeMethodLogViewManager(inspection);
        }
        return viewManager;
    }

    private final InspectorInvokeMethodLog invokeMethodLog;
    private final JTextArea textArea;
    private final JPopupMenu popupMenu;
    private final Action copyAction;
    private final Action cutAction;
    private final Action pasteAction;
    private final InspectorAction selectAllAction;
    private final InspectorAction clearAction;
    private final InspectorAction writeToFileAction;
    private final InspectorAction invokeMethodLogPrintAction;
    private final InspectSelectedAddressMemoryAction inspectSelectedAddressMemoryAction;
    private final InspectSelectedAddressRegionAction inspectSelectedAddressRegionAction;
    private final InspectSelectedAddressObjectAction inspectSelectedAddressObjectAction;

    // invariant:  always non-null
    private String selectedText = "";

    private static InvokeMethodLogView invokeMethodLogView;

    public static InvokeMethodLogView getInvokeMethodLogView() {
        return invokeMethodLogView;
    }

    // Note that we're treating the view as a singleton for now, so we're using
    // the default mechanism for saving geometry.  May need more view options
    // when we add functionality.
    private InvokeMethodLogView(Inspection inspection, InspectorInvokeMethodLog invokeMethodLog) {
        super(inspection, VIEW_KIND, GEOMETRY_SETTINGS_KEY);
        Trace.begin(1,  tracePrefix() + " initializing");
        this.invokeMethodLog = invokeMethodLog;

        textArea = new JTextArea(invokeMethodLog.getContents());

        // Get the standard editing actions for the text area and wrap
        // them in actions with different names
        final Action[] actions = textArea.getActions();
        final Hashtable<String, Action> actionLookup = new Hashtable<String, Action>();
        for (Action action : actions) {
            final String name = (String) action.getValue(Action.NAME);
            actionLookup.put(name, action);
        }
        copyAction = new AbstractAction("Copy (Ctrl-c)") {
            final Action action = actionLookup.get(DefaultEditorKit.copyAction);

            @Override
            public void actionPerformed(ActionEvent e) {
                action.actionPerformed(e);
            }
        };
        cutAction = new AbstractAction("Cut (Ctrl-x)") {
            final Action action = actionLookup.get(DefaultEditorKit.cutAction);

            @Override
            public void actionPerformed(ActionEvent e) {
                action.actionPerformed(e);
            }
        };
        pasteAction = new AbstractAction("Paste (Ctrl-v)") {
            final Action action = actionLookup.get(DefaultEditorKit.pasteAction);

            @Override
            public void actionPerformed(ActionEvent e) {
                action.actionPerformed(e);
            }
        };
        selectAllAction = new InvokeMethodLogSelectAllAction(inspection);
        clearAction = new InvokeMethodLogClearAction(inspection);

        // Add handlers for various user events
        textArea.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(final MouseEvent mouseEvent) {
                switch(inspection().gui().getButton(mouseEvent)) {
                    case MouseEvent.BUTTON1:
                        break;
                    case MouseEvent.BUTTON2:
                        break;
                    case MouseEvent.BUTTON3:
                        popupMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                        break;
                }
            }
        });
        textArea.addCaretListener(new CaretListener() {

            public void caretUpdate(CaretEvent e) {
                updateSelection(textArea.getSelectedText());
            }
        });
        textArea.addFocusListener(new FocusListener() {

            public void focusGained(FocusEvent e) {
            }

            public void focusLost(FocusEvent e) {
                save();
            }
        });
        textArea.getDocument().addDocumentListener(new DocumentListener() {

            public void changedUpdate(DocumentEvent e) {
            }

            public void insertUpdate(DocumentEvent e) {
                updateHighlighting();
            }

            public void removeUpdate(DocumentEvent e) {
                updateHighlighting();
            }
        });

        invokeMethodLogPrintAction = new InvokeMethodLogPrintAction(inspection);
        writeToFileAction = new WriteToFileAction(inspection);
        inspectSelectedAddressMemoryAction = new InspectSelectedAddressMemoryAction(inspection);
        inspectSelectedAddressRegionAction = new InspectSelectedAddressRegionAction(inspection);
        inspectSelectedAddressObjectAction = new InspectSelectedAddressObjectAction(inspection);

        popupMenu = new JPopupMenu();
        popupMenu.add(inspectSelectedAddressMemoryAction);
        popupMenu.add(inspectSelectedAddressRegionAction);
        popupMenu.add(inspectSelectedAddressObjectAction);
        popupMenu.addSeparator();
        popupMenu.add(copyAction);
        popupMenu.add(cutAction);
        popupMenu.add(pasteAction);
        popupMenu.add(selectAllAction);
        popupMenu.add(clearAction);

        final InspectorFrame frame = createFrame(true);

        frame.makeMenu(MenuKind.DEFAULT_MENU).add(defaultMenuItems(MenuKind.DEFAULT_MENU));
        final InspectorMenu editMenu = frame.makeMenu(MenuKind.EDIT_MENU);
        editMenu.add(copyAction);
        editMenu.add(cutAction);
        editMenu.add(pasteAction);
        editMenu.add(selectAllAction);
        editMenu.add(clearAction);
        editMenu.add(writeToFileAction);
        final InspectorMenu memoryMenu = frame.makeMenu(MenuKind.MEMORY_MENU);
        memoryMenu.add(inspectSelectedAddressMemoryAction);
        memoryMenu.add(inspectSelectedAddressRegionAction);
        memoryMenu.add(defaultMenuItems(MenuKind.MEMORY_MENU));
        final InspectorMenu objectMenu = frame.makeMenu(MenuKind.OBJECT_MENU);
        objectMenu.add(inspectSelectedAddressObjectAction);
        objectMenu.add(defaultMenuItems(MenuKind.OBJECT_MENU));
        memoryMenu.add(views().activateSingletonViewAction(ViewKind.ALLOCATIONS));
        frame.makeMenu(MenuKind.VIEW_MENU).add(defaultMenuItems(MenuKind.VIEW_MENU));

        Trace.end(1,  tracePrefix() + " initializing");
    }

    @Override
    public String getTextForTitle() {
        return viewManager.shortName() +  ": " + invokeMethodLog.getName();
    }

    @Override
    protected void createViewContent() {
        setContentPane(new InspectorScrollPane(inspection(), textArea));
        setDisplayStyle(textArea);
        updateHighlighting();
    }

    @Override
    protected void refreshState(boolean force) {
        save();
    }

    @Override
    public InspectorAction getPrintAction() {
        return invokeMethodLogPrintAction;
    }

    @Override
    public void viewConfigurationChanged() {
        save();
        setDisplayStyle(textArea);
        reconstructView();
    }

    @Override
    public void viewClosing() {
        save();
        super.viewClosing();
    }

    @Override
    public void inspectionEnding() {
        save();
        super.inspectionEnding();
    }

    /**
     * Apply the current display style settings to the editing pane.
     * @param textArea the text editing pane for notepad contents
     */
    private void setDisplayStyle(JTextArea textArea) {
        textArea.setFont(preference().style().defaultFont());
    }

    /**
     * Apply visual styles to parts of the editing area that are
     * recognized, for example as VM memory addresses.
     */
    private void updateHighlighting() {
        // TODO (mlvdv) use address matcher to drive some display features;
        // will require using a JEditorPane/JTextPane instead of the JTextArea.
//        final String text = textArea.getText();
//        final Matcher matcher = hexNumberPattern.matcher(text);
//        while (matcher.find()) {
//            System.out.println("match=(" + matcher.start() + "," + matcher.end() + ")");
//        }

    }

    /**
     * Writes the current contents of the editor back to the persistent notepad.
     */
    private void save() {
        invokeMethodLog.setContents(textArea.getText());
    }

    /**
     * Responds to a change in the user's selection.
     *
     * @param selection the text currently selected, null if none.
     */
    private void updateSelection(String selection) {
        final String newSelectedText = selection == null ? "" : selection;
        if (!selectedText.equals(newSelectedText)) {
            selectedText = newSelectedText;
            final Address selectedAddress = getSelectionAsAddress();
            inspectSelectedAddressMemoryAction.setSelectedAddress(selectedAddress);
            inspectSelectedAddressRegionAction.setSelectedAddress(selectedAddress);
            inspectSelectedAddressObjectAction.setSelectedAddress(selectedAddress);
        }
    }

    /**
     * Attempts to convert the currently selected text into a memory address, assuming
     * that it is represented in hex and is optionally prefaced by "0x".
     * @return the memory address specified (in hex); null
     * if selection cannot be understood as a hex address.
     */
    private Address getSelectionAsAddress() {
        String selectedText = textArea.getSelectedText();
        Address selectedAddress = Address.zero();
        if (selectedText != null) {
            selectedText = selectedText.trim();
            if (selectedText.length() > 2 && selectedText.substring(0, 2).equalsIgnoreCase("0x")) {
                selectedText = selectedText.substring(2);
            }
            try {
                selectedAddress = Address.parse(selectedText, 16);
            } catch (NumberFormatException e) {
                // Can't interpret as an address; allow null to be returned.
            }
        }
        return selectedAddress;
    }

    public void appendText(String text) {
        textArea.append(text + "\n");
    }

    /**
     * An action that brings up a memory view on a specified memory location.
     */
    private final class InspectSelectedAddressMemoryAction extends InspectorAction {

        private Address address;

        public InspectSelectedAddressMemoryAction(Inspection inspection) {
            super(inspection, "View memory at selected address");
            setEnabled(false);
        }

        @Override
        protected void procedure() {
            views().memory().makeView(address).highlight();
            focus().setAddress(address);
        }

        public void setSelectedAddress(Address address) {
            this.address = address;
            setEnabled(address != null && address.isNotZero());
        }
    }

    /**
     * An action that brings up a memory region view on the VM
     * memory region, if any, that includes a specified address.
     *
     */
    private final class InspectSelectedAddressRegionAction extends InspectorAction {

        private Address address;
        private MaxMemoryRegion memoryRegion;

        public InspectSelectedAddressRegionAction(Inspection inspection) {
            super(inspection, "View region containing selected address");
            setEnabled(false);
        }

        @Override
        protected void procedure() {
            views().memory().makeView(memoryRegion, null).highlight();
            focus().setAddress(address);
        }

        public void setSelectedAddress(Address address) {
            this.address = address;
            this.memoryRegion = vm().state().findMemoryRegion(address);
            setEnabled(memoryRegion != null);
        }
    }

    /**
     * An action that brings up an object view on the VM object, if any,
     * that includes a specified address.
     */
    private final class InspectSelectedAddressObjectAction extends InspectorAction {

        private Address address;
        private TeleObject object;

        public InspectSelectedAddressObjectAction(Inspection inspection) {
            super(inspection, "View object at selected origin");
            setEnabled(false);
        }

        @Override
        protected void procedure() {
            views().objects().makeView(object);
            focus().setAddress(address);
        }

        public void setSelectedAddress(Address address) {
            this.address = address;
            this.object = vm().objects().findObjectAt(address);
            setEnabled(object != null);
        }
    }

    /**
     * Action that removes the current contents of the notepad.
     */
    private final class InvokeMethodLogSelectAllAction extends InspectorAction {

        public InvokeMethodLogSelectAllAction(Inspection inspection) {
            super(inspection, "Select all");
        }

        @Override
        protected void procedure() {
            textArea.selectAll();
        }
    }


    /**
     * Action that removes the current contents of the notepad.
     */
    private final class InvokeMethodLogClearAction extends InspectorAction {

        public InvokeMethodLogClearAction(Inspection inspection) {
            super(inspection, "Clear all");
        }

        @Override
        protected void procedure() {
            textArea.setText("");
        }
    }
    /**
     * Action:  produces a dialog for writing notepad contents to an interactively specified file.
     */
    final class WriteToFileAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Write contents to a file...";

        WriteToFileAction(Inspection inspection) {
            super(inspection, DEFAULT_TITLE);
        }

        @Override
        protected void procedure() {
            //Create a file chooser
            final JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
            fileChooser.setDialogTitle("Write notepad contents to file:");
            final int returnVal = fileChooser.showSaveDialog(gui().frame());
            if (returnVal != JFileChooser.APPROVE_OPTION) {
                return;
            }
            final File file = fileChooser.getSelectedFile();
            if (file.exists() && !gui().yesNoDialog("File " + file + "exists.  Overwrite?\n")) {
                return;
            }
            try {
                final PrintStream printStream = new PrintStream(new FileOutputStream(file, false));
                printStream.print(textArea.getText());
            } catch (FileNotFoundException fileNotFoundException) {
                gui().errorMessage("Unable to open " + file + " for writing:" + fileNotFoundException);
            }
        }
    }

    /**
     * Action that produces a dialog for printing the contents, if any, of
     * the notepad.
     */
    private final class InvokeMethodLogPrintAction extends InspectorAction {

        public InvokeMethodLogPrintAction(Inspection inspection) {
            super(inspection, "Print notepad contents");
        }

        @Override
        protected void procedure() {
            if (textArea.getText().length() == 0) {
                gui().errorMessage("notepad is empty");
            } else {
                try {
                    textArea.print();
                } catch (PrinterException printerException) {
                    inspection().gui().errorMessage("notepad print failed: " + printerException.getMessage());
                    printerException.printStackTrace();
                }
            }
        }
    }
}
