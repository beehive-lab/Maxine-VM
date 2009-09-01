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
package com.sun.max.ins.debug;

import java.awt.*;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.InspectionSettings.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.gui.TableColumnVisibilityPreferences.*;
import com.sun.max.program.*;
import com.sun.max.tele.debug.*;

/**
 * Singleton inspector that displays information about all kinds of breakpoints that might be set in the VM.
 * Wrappers with extra information about each breakpoint are kept in a model.
 *
 * @author Mick Jordan
 * @author Michael Van De Vanter
 */
public final class BreakpointsInspector extends Inspector implements TableColumnViewPreferenceListener {

    private static final int TRACE_VALUE = 1;

    // Set to null when inspector closed.
    private static BreakpointsInspector breakpointsInspector;

    /**
     * Displays the (singleton) breakpoints inspector.
     * @return  The breakpoints inspector, possibly newly created.
     */
    public static BreakpointsInspector make(Inspection inspection) {
        if (breakpointsInspector == null) {
            breakpointsInspector = new BreakpointsInspector(inspection);
        }
        return breakpointsInspector;
    }

    private final SaveSettingsListener saveSettingsListener = createGeometrySettingsClient(this, "breakpointsInspector");

    // This is a singleton viewer, so only use a single level of view preferences.
    private final BreakpointsViewPreferences viewPreferences;

    private BreakpointsTable table;

    private BreakpointsInspector(Inspection inspection) {
        super(inspection);
        Trace.begin(1,  tracePrefix() + " initializing");
        viewPreferences = BreakpointsViewPreferences.globalPreferences(inspection());
        viewPreferences.addListener(this);
        createFrame(null);
        getMenu(DEFAULT_INSPECTOR_MENU).add(new BreakpointFrameMenuItems());
        Trace.end(1,  tracePrefix() + " initializing");
    }

    @Override
    protected Rectangle defaultFrameBounds() {
        return inspection().geometry().breakpointsFrameDefaultBounds();
    }

    @Override
    protected void createView() {
        table = new BreakpointsTable(inspection(), viewPreferences);
        final JScrollPane scrollPane = new InspectorScrollPane(inspection(), table);
        frame().setContentPane(scrollPane);
    }

    @Override
    protected SaveSettingsListener saveSettingsListener() {
        return saveSettingsListener;
    }

    @Override
    protected InspectorTable getTable() {
        return table;
    }

    @Override
    public String getTextForTitle() {
        return "Breakpoints";
    }

    @Override
    public InspectorAction getViewOptionsAction() {
        return new InspectorAction(inspection(), "View Options") {
            @Override
            public void procedure() {
                new TableColumnVisibilityPreferences.ColumnPreferencesDialog<BreakpointsColumnKind>(inspection(), "Breakpoints View Options", viewPreferences);
            }
        };
    }

    @Override
    public InspectorAction getPrintAction() {
        return getDefaultPrintAction();
    }

    /**
     * Menu items not dependent on mouse location, suitable for the frame.
     */
    private final class BreakpointFrameMenuItems implements InspectorMenuItems {

        public void addTo(InspectorMenu menu) {
            final JMenu methodEntryBreakpoints = new JMenu("Break at Method Entry");
            methodEntryBreakpoints.add(inspection().actions().setTargetCodeBreakpointAtMethodEntriesByName());
            methodEntryBreakpoints.add(inspection().actions().setBytecodeBreakpointAtMethodEntryByName());
            methodEntryBreakpoints.add(inspection().actions().setBytecodeBreakpointAtMethodEntryByKey());
            menu.add(methodEntryBreakpoints);
            menu.add(inspection().actions().setTargetCodeBreakpointAtObjectInitializer());
            menu.addSeparator();
            menu.add(inspection().actions().removeSelectedBreakpoint());
            menu.add(inspection().actions().removeAllTargetCodeBreakpoints());
            menu.add(inspection().actions().removeAllBytecodeBreakpoints());
        }

        public Inspection inspection() {
            return BreakpointsInspector.this.inspection();
        }

        public void refresh(boolean force) {
        }

        public void redisplay() {
        }
    }

    @Override
    protected boolean refreshView(boolean force) {
        table.refresh(force);
        super.refreshView(force);
        return true;
    }

    public void viewConfigurationChanged() {
        reconstructView();
    }

    @Override
    public void breakpointStateChanged() {
        refreshView(true);
    }

    @Override
    public  void breakpointFocusSet(TeleBreakpoint oldTeleBreakpoint, TeleBreakpoint teleBreakpoint) {
        if (table != null) {
            table.updateFocusSelection();
        }
    }

    public void tableColumnViewPreferencesChanged() {
        reconstructView();
    }

    @Override
    public void inspectorClosing() {
        Trace.line(TRACE_VALUE, tracePrefix() + " closing");
        breakpointsInspector = null;
        viewPreferences.removeListener(this);
        super.inspectorClosing();
    }

}
