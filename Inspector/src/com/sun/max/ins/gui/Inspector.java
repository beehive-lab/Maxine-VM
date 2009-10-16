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
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.stack.*;

/**
 * <p>
 * An inspector combines an aggregation of {@link Prober}s in a displayed frame.</p>
 * <p>
 * <b>Event Notification</b>:
 * This abstract class ensures that very Inspector listens for {@linkplain InspectionListener Inspection Events}
 * as well as {@linkplain ViewFocusListener Focus Events}.  Any Inspector implementation
 * that wishes to receive such notifications must do so by overriding the appropriate
 * methods in interfaces {@link InspectionListener} and {@link ViewFocusListener},
 * for which empty methods are provided in this abstract class.</p>
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 *
 */
public abstract class Inspector extends AbstractInspectionHolder implements InspectionListener, ViewFocusListener {

    private static final int TRACE_VALUE = 2;

    private static final ImageIcon FRAME_ICON = InspectorImageIcon.createDownTriangle(16, 16);
    public static final String DEFAULT_INSPECTOR_MENU = "Default";
    public static final String VIEW_INSPECTOR_MENU = "View";
    public static final String METHOD_INSPECTOR_MENU = "Method";

    private InspectorFrameInterface frame;
    //private InspectorInternalFrame frame;

    protected Inspector(Inspection inspection) {
        super(inspection);
    }

    /**
     * Populates the inspector's frame, already created, with components that make up the Inspector's view.
     */
    protected abstract void createView();

    public InspectorMenu createDefaultMenu() {
        final InspectorMenu menu = new InspectorMenu(Inspector.DEFAULT_INSPECTOR_MENU);
        menu.add(getViewOptionsAction());
        menu.add(getRefreshAction());
        menu.addSeparator();
        menu.add(getCloseAction());
        menu.add(getCloseOtherInspectorsAction());
        menu.addSeparator();
        menu.add(getPrintAction());
        menu.setText(null);
        menu.setIcon(FRAME_ICON);
        return menu;
    }

    protected InspectorMenu getMenu(String menuName) {
        return frame.getMenu(menuName);
    }

    /**
     * @return the component in which the Inspector displays its view.
     */
    public final JComponent getJComponent() {
        return frame.getJComponent();
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
        return new AbstractSaveSettingsListener(name, inspector) {

            @Override
            public Rectangle defaultBounds() {
                return inspector.defaultFrameBounds();
            }

            public void saveSettings(SaveSettingsEvent saveSettingsEvent) {
            }
        };
    }

    protected void setWarning() {
        //highlight();
    }

    /**
     * @return the string currently appearing in the title of the Inspector's window frame
     */
    public final String getTitle() {
        return frame.getTitle();
    }

    /**
     * Gets from subclasses the currently appropriate title for this inspector's display frame.
     *
     * @return a short string suitable for appearing in the window frame of an inspector.
     * If this text is expected to change dynamically, a call to {@link #setTitle()}
     * will cause this to be called again and the result assigned to the frame.
     */
    protected abstract String getTextForTitle();


    /**
     * Sets the display frame title for this inspector to the string provided by
     * the abstract method {@link #getTextForTitle()}.
     */
    protected final void setTitle() {
        setTitle(null);
    }

    /**
     * Sets the display frame title for this inspector.
     *
     * @param title a string to display.  If null, uses the string provided by
     * the abstract method {@link #getTextForTitle()}.
     */
    protected final void setTitle(String title) {
        frame.setTitle(title == null ? getTextForTitle() : title);
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
        frame = new InspectorInternalFrame(this, menu);
        setTitle();
        createView();
        frame.pack();
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
    protected boolean refreshView(boolean force) {
        frame.refresh(force);
        frame.invalidate();
        frame.repaint();
        return true;
    }

    /**
     * Rebuilds the "view" component of the inspector, much more
     * expensive than {@link refreshView()}, but necessary when the parameters or
     * configuration of the view changes enough to require creating a new one.
     */
    protected void reconstructView() {
        final Dimension size = frame.getSize();
        createView();
        frame.setPreferredSize(size);
        frame.pack();
    }

    /**
     * @return the visible table for inspectors with table-based views; null if none.
     */
    protected InspectorTable getTable() {
        return null;
    }

    public JRootPane getRootPane() {
        return frame.getRootPane();
    }

    public void setContentPane(Container contentPane) {
        frame.setContentPane(contentPane);
    }

    public Container getContentPane() {
        return frame.getContentPane();
    }

    public void setLayeredPane(JLayeredPane layeredPane) {
        frame.setLayeredPane(layeredPane);
    }

    public JLayeredPane getLayeredPane() {
        return frame.getLayeredPane();
    }

    public void setGlassPane(Component glassPane) {
        frame.setGlassPane(glassPane);
    }

    public Component getGlassPane() {
        return frame.getGlassPane();
    }

    protected void moveToFront() {
        frame.moveToFront();
    }

    protected boolean isSelected() {
        return frame.isSelected();
    }

    protected void setSelected() {
        frame.setSelected();
    }

    public void pack() {
        frame.pack();
    }

    public void flash() {
        frame.flash(style().frameBorderFlashColor());
    }

    /**
     * Calls this inspector to the users attention:  move to front, select, and flash.
     */
    public void highlight() {
        frame.moveToFront();
        setSelected();
        flash();
    }

    /**
     * If not already visible and selected, calls this inspector to the users attention:  move to front, select, and flash.
     */
    protected void highlightIfNotVisible() {
        frame.moveToFront();
        if (!isSelected()) {
            setSelected();
            frame.flash(style().frameBorderFlashColor());
        }
    }

    /**
     * Explicitly closes a particular Inspector, but
     * many are closed implicitly by a window system
     * event on the frame.
     */
    public void dispose() {
        frame.dispose();
    }

    public void replaceFrameCloseAction(InspectorAction action) {
        frame.replaceFrameCloseAction(action);
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

    public void vmStateChanged(boolean force) {
        refreshView(force);
    }

    public void threadStateChanged(MaxThread thread) {
    }

    public void breakpointStateChanged() {
    }

    public void watchpointSetChanged() {
    }

    public void vmProcessTerminated() {
    }

    public void codeLocationFocusSet(TeleCodeLocation teleCodeLocation, boolean interactiveForNative) {
    }

    public void threadFocusSet(MaxThread oldMaxThread, MaxThread maxeThread) {
    }

    public void stackFrameFocusChanged(StackFrame oldStackFrame, MaxThread threadForStackFrame, StackFrame stackFrame) {
    }

    public void addressFocusChanged(Address oldAddress, Address address) {
    }

    public void memoryRegionFocusChanged(MemoryRegion oldMemoryRegion, MemoryRegion memoryRegion) {
    }

    public void breakpointFocusSet(TeleBreakpoint oldTeleBreakpoint, TeleBreakpoint teleBreakpoint) {
    }

    public void watchpointFocusSet(MaxWatchpoint oldWatchpoint, MaxWatchpoint watchpoint) {
    }

    public void heapObjectFocusChanged(TeleObject oldTeleObject, TeleObject teleObject) {
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
                dispose();
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
