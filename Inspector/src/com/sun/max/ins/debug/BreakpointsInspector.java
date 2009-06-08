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

/**
 * Singleton inspector that displays information about all kinds of breakpoints that might be set in the VM.
 * Wrappers with extra information about each breakpoint are kept in a model.
 *
 * @author Mick Jordan
 * @author Michael Van De Vanter
 */
public final class BreakpointsInspector extends Inspector implements TableColumnViewPreferenceListener {

    // Set to null when inspector closed.
    private static BreakpointsInspector _breakpointsInspector;

    /**
     * Displays the (singleton) breakpoints inspector.
     * @return  The breakpoints inspector, possibly newly created.
     */
    public static BreakpointsInspector make(Inspection inspection) {
        if (_breakpointsInspector == null) {
            _breakpointsInspector = new BreakpointsInspector(inspection);
        }
        return _breakpointsInspector;
    }

    private final SaveSettingsListener _saveSettingsListener = createGeometrySettingsClient(this, "breakpointsInspector");

    // This is a singleton viewer, so only use a single level of view preferences.
    private final BreakpointsViewPreferences _viewPreferences;

    private BreakpointsTable _table;

    private BreakpointsInspector(Inspection inspection) {
        super(inspection);
        Trace.begin(1,  tracePrefix() + " initializing");
        _viewPreferences = BreakpointsViewPreferences.globalPreferences(inspection());
        _viewPreferences.addListener(this);
        createFrame(null);
        frame().add(new BreakpointFrameMenuItems());
        Trace.end(1,  tracePrefix() + " initializing");
    }

    @Override
    protected Rectangle defaultFrameBounds() {
        return inspection().geometry().breakpointsFrameDefaultBounds();
    }

    @Override
    protected void createView() {
        if (_table != null) {
            focus().removeListener(_table);
        }
        _table = new BreakpointsTable(inspection(), _viewPreferences);
        focus().addListener(_table);
        final JScrollPane scrollPane = new InspectorScrollPane(inspection(), _table);
        frame().setContentPane(scrollPane);
    }

    @Override
    protected SaveSettingsListener saveSettingsListener() {
        return _saveSettingsListener;
    }

    @Override
    protected InspectorTable getTable() {
        return _table;
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
                new TableColumnVisibilityPreferences.Dialog<BreakpointsColumnKind>(inspection(), "Breakpoints View Options", _viewPreferences);
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
            menu.add(inspection().actions().removeBreakpoint());
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
    protected void refreshView(boolean force) {
        _table.refresh(force);
        super.refreshView(force);
    }

    public void viewConfigurationChanged() {
        reconstructView();
    }

    @Override
    public void breakpointSetChanged() {
        refreshView(true);
    }

    public void tableColumnViewPreferencesChanged() {
        reconstructView();
    }

    @Override
    public void inspectorClosing() {
        Trace.line(1, tracePrefix() + " closing");
        _breakpointsInspector = null;
        _viewPreferences.removeListener(this);
        focus().removeListener(_table);
        super.inspectorClosing();
    }

}
