/*
 * * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.ins;

import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.util.*;
import java.util.regex.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

import com.sun.max.ins.InspectionSettings.*;
import com.sun.max.ins.gui.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;

/**
 * An inspector that gives viewing and editing access to a notepad.
 * <br>
 * The inspector is currently a singleton, but this might be generalized
 * if multiple notepads are supported in the future.
 *
 * @author Michael Van De Vanter
 * @see InspectorNotepad
 * @see NotepadManager
 */
public final class NotepadInspector extends Inspector {

    // A compiled regular expression pattern that matches hex numbers (with or without prefix) and ordinary
    // integers as well.
    // TODO (mlvdv) fix pattern failure at end of contents (if no newline)
    private static final Pattern hexNumberPattern = Pattern.compile("[0-9a-fA-F]+[^a-zA-Z0-9]");

    // Set to null when inspector closed.
    private static NotepadInspector notepadInspector;
    /**
     * Display the (singleton) Notepad inspector.
     * @param notepad the notepad, assumed to be a singleton for the time being
     */
    public static NotepadInspector make(Inspection inspection, InspectorNotepad notepad) {
        if (notepadInspector == null) {
            notepadInspector = new NotepadInspector(inspection, notepad);
        }
        return notepadInspector;
    }

    // TODO (mlvdv)  only geometry settings saved now, but might need view options if add features such as highlighting
    private final SaveSettingsListener saveSettingsListener = createGeometrySettingsClient(this, "notepadInspectorGeometry");
    private final InspectorNotepad notepad;
    private final JTextArea textArea;
    private final JPopupMenu popupMenu;
    private final Action copyAction;
    private final Action cutAction;
    private final Action pasteAction;
    private final NotepadPrintAction notepadPrintAction;
    private final InspectSelectedAddressMemoryAction inspectSelectedAddressMemoryAction;
    private final InspectSelectedAddressRegionAction inspectSelectedAddressRegionAction;
    private final InspectSelectedAddressObjectAction inspectSelectedAddressObjectAction;

    // invariant:  always non-null
    private String selectedText = "";

    private NotepadInspector(Inspection inspection, InspectorNotepad notepad) {
        super(inspection);
        Trace.begin(1,  tracePrefix() + " initializing");
        this.notepad = notepad;

        textArea = new JTextArea(notepad.getContents());

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

        // Add handlers for various user events
        textArea.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(final MouseEvent mouseEvent) {
                switch(Inspection.mouseButtonWithModifiers(mouseEvent)) {
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
                System.out.println("insert");
                updateHighlighting();
            }

            public void removeUpdate(DocumentEvent e) {
                System.out.println("remove");
                updateHighlighting();
            }
        });

        notepadPrintAction = new NotepadPrintAction(inspection);
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

        final InspectorFrame frame = createFrame(true);

        frame.makeMenu(MenuKind.DEFAULT_MENU).add(defaultMenuItems(MenuKind.DEFAULT_MENU));
        final InspectorMenu editMenu = frame.makeMenu(MenuKind.EDIT_MENU);
        editMenu.add(copyAction);
        editMenu.add(cutAction);
        editMenu.add(pasteAction);
        final InspectorMenu memoryMenu = frame.makeMenu(MenuKind.MEMORY_MENU);
        memoryMenu.add(inspectSelectedAddressMemoryAction);
        memoryMenu.add(inspectSelectedAddressRegionAction);
        memoryMenu.add(defaultMenuItems(MenuKind.MEMORY_MENU));
        final InspectorMenu objectMenu = frame.makeMenu(MenuKind.OBJECT_MENU);
        objectMenu.add(inspectSelectedAddressObjectAction);
        objectMenu.add(defaultMenuItems(MenuKind.OBJECT_MENU));
        final JMenuItem viewMemoryRegionsMenuItem = new JMenuItem(actions().viewMemoryRegions());
        viewMemoryRegionsMenuItem.setText("View Memory Regions");
        memoryMenu.add(viewMemoryRegionsMenuItem);
        frame.makeMenu(MenuKind.VIEW_MENU).add(defaultMenuItems(MenuKind.VIEW_MENU));


        Trace.end(1,  tracePrefix() + " initializing");
    }

    @Override
    protected SaveSettingsListener saveSettingsListener() {
        return saveSettingsListener;
    }

    @Override
    public String getTextForTitle() {
        return "Notepad: " + notepad.getName();
    }

    @Override
    protected Rectangle defaultFrameBounds() {
        return inspection().geometry().notepadFrameDefaultBounds();
    }

    @Override
    protected void createView() {
        setContentPane(new InspectorScrollPane(inspection(), textArea));
        setDisplayStyle(textArea);
        updateHighlighting();
    }

    @Override
    protected void refreshView(boolean force) {
        save();
        super.refreshView(force);
    }

    @Override
    public InspectorAction getPrintAction() {
        return notepadPrintAction;
    }

    public void viewConfigurationChanged() {
        save();
        setDisplayStyle(textArea);
        reconstructView();
    }

    @Override
    public void inspectorClosing() {
        Trace.line(1, tracePrefix() + " closing");
        save();
        notepadInspector = null;
        super.inspectorClosing();
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
        textArea.setFont(style().defaultFont());
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
        notepad.setContents(textArea.getText());
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

    /**
     * An action that brings up a memory inspector on a specified memory location.
     */
    private final class InspectSelectedAddressMemoryAction extends InspectorAction {

        private Address address;

        public InspectSelectedAddressMemoryAction(Inspection inspection) {
            super(inspection, "Inspect memory at selected address");
            setEnabled(false);
        }

        @Override
        protected void procedure() {
            actions().inspectMemoryWords(address).perform();
            focus().setAddress(address);
        }

        public void setSelectedAddress(Address address) {
            this.address = address;
            setEnabled(address != null && !address.isZero());
        }
    }

    /**
     * An action that brings up a memory region inspector on the VM
     * memory region, if any, that includes a specified address.
     *
     */
    private final class InspectSelectedAddressRegionAction extends InspectorAction {

        private Address address;
        private MaxMemoryRegion memoryRegion;

        public InspectSelectedAddressRegionAction(Inspection inspection) {
            super(inspection, "Inspect region containing selected address");
            setEnabled(false);
        }

        @Override
        protected void procedure() {
            actions().inspectMemoryWords(memoryRegion).perform();
            focus().setAddress(address);
        }

        public void setSelectedAddress(Address address) {
            this.address = address;
            this.memoryRegion = vm().findMemoryRegion(address);
            setEnabled(memoryRegion != null);
        }
    }

    /**
     * An action that brings up an object inspector on the VM object, if any,
     * that includes a specified address.
     */
    private final class InspectSelectedAddressObjectAction extends InspectorAction {

        private Address address;
        private TeleObject object;

        public InspectSelectedAddressObjectAction(Inspection inspection) {
            super(inspection, "Inspect object at selected origin");
            setEnabled(false);
        }

        @Override
        protected void procedure() {
            actions().inspectObject(object, null).perform();
            focus().setAddress(address);
        }

        public void setSelectedAddress(Address address) {
            this.address = address;
            this.object = vm().heap().findObjectAt(address);
            setEnabled(object != null);
        }
    }

    /**
     * Action that produces a dialog for printing the contents, if any, of
     * the notepad.
     */
    private final class NotepadPrintAction extends InspectorAction {

        public NotepadPrintAction(Inspection inspection) {
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
