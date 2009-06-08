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
import java.awt.print.*;
import java.text.*;
import java.util.*;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.InspectionSettings.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.stack.*;

/**
 * An inspector combines an aggregation of {@link Prober}s in a frame.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 *
 * Implementation notes:  for historical reasons an {@link Inspector} has a Frame,
 * rather than being a specialized subclass of Frame.  This creates a number of
 * awkward interfaces, which may be cleaned up in the fullness of time. (mlvdv May '09)
 */
public abstract class Inspector extends AbstractInspectionHolder implements InspectionListener, ViewFocusListener {

    private static final int TRACE_VALUE = 2;

    private InspectorFrame _frame;

    /**
     * @return the window system frame in which the Inspector displays its view
     */
    public InspectorFrame frame() {
        return _frame;
    }

    /**
     * @return default geometry for this inspector, to be used if no prior settings; null if no default specified.
     */
    protected Rectangle defaultFrameBounds() {
        return null;
    }

    /**
     * Gets an object that is an adapter between the inspection's persistent {@linkplain Inspection#settings()}
     * and this inspector. If the object's {@link SaveSettingsListener#component()} , then the
     * size and location of this inspector are adjusted according to the settings as well as being
     * persisted any time this inspector is moved or resized.
     */
    protected SaveSettingsListener saveSettingsListener() {
        return null;
    }

    /**
     * Creates a settings client for this inspector that causes window geometry to be saved & restored.
     */
    protected static SaveSettingsListener createGeometrySettingsClient(final Inspector inspector, final String name) {
        return new AbstractSaveSettingsListener(name) {
            @Override
            public Component component() {
                return (Component) inspector.frame();
            }

            @Override
            public Rectangle defaultBounds() {
                return inspector.defaultFrameBounds();
            }

            public void saveSettings(SaveSettingsEvent saveSettingsEvent) {
            }
        };
    }

    /**
     * @return a short string suitable for appearing in the window frame of an inspector.
     * If this text is expected to change dynamically, a call to {@link #updateFrameTitle()}
     * will cause this to be called again and the result assigned to the frame.
     */
    public abstract String getTextForTitle();

    /**
     * @return the string currently appearing in the title of the Inspector's window frame
     */
    protected final String getCurrentTitle() {
        return frame().getTitle();
    }

    protected Inspector(Inspection inspection) {
        super(inspection);
    }

    /**
     * Populates the inspector's frame, already created, with components that make up the Inspector's view.
     */
    protected abstract void createView();

    protected final void updateFrameTitle() {
        frame().setTitle(getTextForTitle());
    }

    /**
     * Creates a frame for the inspector
     * calls {@link createView()} to populate it; adds the inspector to the update
     * listeners; makes it all visible.
     *
     * If this inspector has a {@linkplain #saveSettingsListener()}, then its size and location
     * is adjusted according to the {@linkplain Inspection#settings() inspection's settings}.
     *
     * @param menu  optional menu to replace the default frame menu
     */
    protected void createFrame(InspectorMenu menu) {
        _frame = new InternalInspectorFrame(this, menu);
        updateFrameTitle();
        createView();
        _frame.pack();
        gui().addInspector(this);
        inspection().addInspectionListener(this);
        inspection().focus().addListener(this);
        final SaveSettingsListener saveSettingsListener = saveSettingsListener();
        if (saveSettingsListener != null) {
            inspection().settings().addSaveSettingsListener(saveSettingsListener);
        }
    }

    /**
     * Reads, re-reads, and updates any state caches if needed from the VM.
     *
     * @param force suspend caching behavior; read state unconditionally.
     */
    protected void refreshView(boolean force) {
        _frame.refresh(force);
        _frame.invalidate();
        _frame.repaint();
    }

    /**
     * Rebuilds the "view" component of the inspector, much more
     * expensive than {@link refreshView()}, but necessary when the parameters or
     * configuration of the view changes enough to require creating a new one.
     */
    protected void reconstructView() {
        final Dimension size = _frame.getSize();
        createView();
        _frame.setPreferredSize(size);
        frame().pack();
    }

    /**
     * @return the visible table for inspectors with table-based views; null if none.
     */
    protected InspectorTable getTable() {
        return null;
    }

    public void vmStateChanged(boolean force) {
        refreshView(force);
    }

    public void threadSetChanged() {
    }

    public void threadStateChanged(TeleNativeThread teleNativeThread) {
    }

    public void breakpointSetChanged() {
    }

    public void vmProcessTerminated() {
    }

    public void codeLocationFocusSet(TeleCodeLocation teleCodeLocation, boolean interactiveForNative) {
    }

    public void threadFocusSet(TeleNativeThread oldTeleNativeThread, TeleNativeThread teleNativeThread) {
    }

    public void stackFrameFocusChanged(StackFrame oldStackFrame, TeleNativeThread threadForStackFrame, StackFrame stackFrame) {
    }

    public void addressFocusChanged(Address oldAddress, Address address) {
    }

    public void memoryRegionFocusChanged(MemoryRegion oldMemoryRegion, MemoryRegion memoryRegion) {
    }

    public void breakpointFocusSet(TeleBreakpoint oldTeleBreakpoint, TeleBreakpoint teleBreakpoint) {
    }

    public void heapObjectFocusChanged(TeleObject oldTeleObject, TeleObject teleObject) {
    }

    /**
     * @return whether the inspector's view can be seen on the screen.
     */
    protected final boolean isShowing() {
        return _frame.isShowing();
    }

    protected void moveToFront() {
        _frame.moveToFront();
    }

    protected boolean isSelected() {
        return _frame.isSelected();
    }

    protected void setSelected() {
        _frame.setSelected();
    }

    /**
     * Calls this inspector to the users attention:  move to front, select, and flash.
     */
    public void highlight() {
        moveToFront();
        setSelected();
        _frame.flash(style().frameBorderFlashColor());
    }

    /**
     * If not already visible and selected, calls this inspector to the users attention:  move to front, select, and flash.
     */
    protected void highlightIfNotVisible() {
        moveToFront();
        if (!isSelected()) {
            setSelected();
            _frame.flash(style().frameBorderFlashColor());
        }
    }

    /**
     * Explicitly closes a particular Inspector, but
     * many are closed implicitly by a window system
     * event on the frame.
     */
    public void dispose() {
        _frame.dispose();
    }

    /**
     * Receives notification that the the frame has acquired focus in the window system.
     */
    protected void inspectorGetsWindowFocus() {
    }

    /**
     * Receives notification that the the frame has acquired focus in the window system.
     */
    protected void inspectorLosesWindowFocus() {
    }

    /**
     * Receives notification that the window system is closing this inspector.
     */
    protected void inspectorClosing() {
        inspection().removeInspectionListener(this);
        inspection().focus().removeListener(this);
        final SaveSettingsListener saveSettingsListener = saveSettingsListener();
        if (saveSettingsListener != null) {
            inspection().settings().removeSaveSettingsListener(saveSettingsListener);
        }
    }

    /**
     * @return an action that will present a dialog that enables selection of view options;
     * returns a disabled dummy action if not overridden.
     */
    public InspectorAction getViewOptionsAction() {
        final InspectorAction dummyViewOptionsAction = new InspectorAction(inspection(), "View Options") {
            @Override
            protected void procedure() {
            }
        };
        dummyViewOptionsAction.setEnabled(false);
        return dummyViewOptionsAction;
    }

    /**
     * @return an action that will refresh any state from the VM.
     */
    public InspectorAction getRefreshAction() {
        return new InspectorAction(inspection(), "Refresh") {
            @Override
            protected void procedure() {
                Trace.line(TRACE_VALUE, "Refreshing view: " + Inspector.this);
                refreshView(true);
            }
        };
    }

    /**
     * @return an action that will close this inspector
     */
    public InspectorAction getCloseAction() {
        return new InspectorAction(inspection(), "Close") {
            @Override
            protected void procedure() {
                frame().dispose();
            }
        };
    }

    public InspectorAction getCloseOtherInspectorsAction() {
        final Predicate<Inspector> predicate = new Predicate<Inspector>() {
            public boolean evaluate(Inspector inspector) {
                return inspector != Inspector.this;
            }
        };
        return actions().closeViews(predicate, "Close Other Inspectors");
    }

    /**
     * @return the default print action for table-based views; depends on an overridden
     * {@link #getTable()} method to provide the table.
     */
    protected final InspectorAction getDefaultPrintAction() {
        return new InspectorAction(inspection(), "Print") {
            @Override
            public void procedure() {
                final MessageFormat footer = new MessageFormat("Maxine: " + getTextForTitle() + "  Printed: " + new Date() + " -- Page: {0, number, integer}");
                try {
                    final InspectorTable inspectorTable = getTable();
                    assert inspectorTable != null;
                    inspectorTable.print(JTable.PrintMode.FIT_WIDTH, null, footer);
                } catch (PrinterException printerException) {
                    gui().errorMessage("Print failed: " + printerException.getMessage());
                }
            }
        };
    }

    /**
     * @return an action that will present a print dialog for printing the contents of the view;
     * returns a disabled dummy action if not overridden.
     */
    public InspectorAction getPrintAction() {
        final InspectorAction dummyPrintAction = new InspectorAction(inspection(), "Print") {
            @Override
            protected void procedure() {
            }
        };
        dummyPrintAction.setEnabled(false);
        return dummyPrintAction;
    }


    @Override
    public String toString() {
        return this.getClass().getSimpleName() + ":  " + getTextForTitle();
    }

}
