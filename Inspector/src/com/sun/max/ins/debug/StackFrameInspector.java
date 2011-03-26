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
package com.sun.max.ins.debug;

import static com.sun.max.tele.MaxProcessState.*;

import java.awt.*;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.InspectionSettings.SaveSettingsListener;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.gui.TableColumnVisibilityPreferences.TableColumnViewPreferenceListener;
import com.sun.max.ins.util.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;

/**
 * A singleton inspector that displays the contents of the stack frame in
 * the VM that is the current user focus.
 *
 * @author Doug Simon
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public final class StackFrameInspector extends Inspector implements TableColumnViewPreferenceListener {

    private static final int TRACE_VALUE = 1;

    // Set to null when inspector closed.
    private static StackFrameInspector stackFrameInspector;

    // This is a singleton viewer, so only use a single level of view preferences.
    private static CompiledStackFrameViewPreferences viewPreferences;


    /**
     * Displays the (singleton) inspector, creating it if needed.
     */
    public static StackFrameInspector make(Inspection inspection) {
        if (stackFrameInspector == null) {
            stackFrameInspector = new StackFrameInspector(inspection);
        }
        return stackFrameInspector;
    }

    private final class CopyStackFrameToClipboardAction extends InspectorAction {

        private CopyStackFrameToClipboardAction() {
            super(inspection(), "Copy stack frame to clipboard");
        }

        @Override
        public void procedure() {
            // (mlvdv)  This is pretty awkward, but has the virtue that it reproduces exactly what's displayed.  Could be improved.
            final StringBuilder result = new StringBuilder(100);
            // TODO  maybe just rely on the generic print command based on the table
            gui().postToClipboard(result.toString());
        }
    }

    private final SaveSettingsListener saveSettingsListener = createGeometrySettingsListener(this, "stackFrameInspectorGeometry");

    private MaxStackFrame stackFrame;
    private final InspectorPanel nullFramePanel;
    private final InspectorPanel truncatedFramePanel;
    private CompiledStackFramePanel compiledStackFramePanel = null;


    private final InspectorAction copyStackFrameToClipboardAction = new CopyStackFrameToClipboardAction();


    public StackFrameInspector(Inspection inspection) {
        super(inspection);
        Trace.begin(1,  tracePrefix() + " initializing");

        viewPreferences = CompiledStackFrameViewPreferences.globalPreferences(inspection);
        viewPreferences.addListener(this);

        nullFramePanel = new InspectorPanel(inspection);
        final JTextArea nullFrameTextArea = new JTextArea(inspection.nameDisplay().unavailableDataLongText());
        nullFrameTextArea.setEditable(false);
        nullFramePanel.add(nullFrameTextArea);

        truncatedFramePanel = new InspectorPanel(inspection);
        final JTextArea truncatedFrameTextArea = new JTextArea("<truncated frame>");
        truncatedFrameTextArea.setEditable(false);
        truncatedFramePanel.add(truncatedFrameTextArea);

        final InspectorFrame frame = createFrame(true);

        frame.makeMenu(MenuKind.DEFAULT_MENU).add(defaultMenuItems(MenuKind.DEFAULT_MENU));

        final InspectorMenu editMenu = frame.makeMenu(MenuKind.EDIT_MENU);
        editMenu.add(copyStackFrameToClipboardAction);

        final InspectorMenu memoryMenu = frame.makeMenu(MenuKind.MEMORY_MENU);
        memoryMenu.add(actions().inspectSelectedStackFrameMemory("Inspect memory for frame"));
        memoryMenu.add(actions().inspectSelectedThreadStackMemory("Inspect memory for stack"));
        memoryMenu.add(defaultMenuItems(MenuKind.MEMORY_MENU));
        final JMenuItem viewMemoryRegionsMenuItem = new JMenuItem(actions().viewMemoryRegions());
        viewMemoryRegionsMenuItem.setText("View Memory Regions");
        memoryMenu.add(viewMemoryRegionsMenuItem);

        frame.makeMenu(MenuKind.VIEW_MENU).add(defaultMenuItems(MenuKind.VIEW_MENU));

        forceRefresh();
        Trace.end(1,  tracePrefix() + " initializing");
    }

    @Override
    protected Rectangle defaultGeometry() {
        return inspection().geometry().stackFrameDefaultFrameGeometry();
    }

    @Override
    protected SaveSettingsListener saveSettingsListener() {
        return saveSettingsListener;
    }

    @Override
    public String getTextForTitle() {
        final StringBuilder sb = new StringBuilder("Frame: ");
        if (stackFrame == null) {
            sb.append(inspection().nameDisplay().unavailableDataShortText());
        } else {
            if (stackFrame instanceof MaxStackFrame.Compiled) {
                final MaxCompiledCode compiledCode = stackFrame.compiledCode();
                sb.append(inspection().nameDisplay().veryShortName(compiledCode));
            } else if (stackFrame instanceof MaxStackFrame.Native) {
                sb.append("<native>");
            }
            sb.append(" in ").append(inspection().nameDisplay().longNameWithState(stackFrame.stack().thread()));
        }
        return sb.toString();
    }

    @Override
    protected void createView() {
        stackFrame = inspection().focus().stackFrame();
        if (stackFrame instanceof MaxStackFrame.Compiled) {
            final MaxStackFrame.Compiled compiledStackFrame = (MaxStackFrame.Compiled) stackFrame;
            compiledStackFramePanel = new DefaultCompiledStackFramePanel(inspection(), compiledStackFrame, viewPreferences);
            setContentPane(compiledStackFramePanel);
        } else if (stackFrame instanceof MaxStackFrame.Native) {
            final InspectorPanel nativeFramePanel = new InspectorPanel(inspection());
            final JTextArea nativeFrameTextArea = new JTextArea(stackFrame.entityName());
            nativeFrameTextArea.setEditable(false);
            nativeFramePanel.add(nativeFrameTextArea);
            setContentPane(nativeFramePanel);
            compiledStackFramePanel = null;
        } else if (stackFrame instanceof MaxStackFrame.Error) {
            final MaxStackFrame.Error errorFrame = (MaxStackFrame.Error) stackFrame;
            final String message = "Stack walking error:\n" + errorFrame.errorMessage();
            final InspectorPanel errorFramePanel = new InspectorPanel(inspection());
            final JTextArea errorFrameTextArea = new JTextArea(message);
            errorFrameTextArea.setEditable(false);
            errorFramePanel.add(errorFrameTextArea);
            setContentPane(errorFramePanel);
            compiledStackFramePanel = null;
        } else if (stackFrame instanceof TruncatedStackFrame) {
            setContentPane(truncatedFramePanel);
            compiledStackFramePanel = null;
        } else if (stackFrame == null) {
            setContentPane(nullFramePanel);
            compiledStackFramePanel = null;
        } else {
            InspectorError.unexpected(tracePrefix() + " unknown type of stack frame");
        }
        setTitle();
    }

    @Override
    protected void refreshState(boolean force) {
        if (compiledStackFramePanel != null) {
            compiledStackFramePanel.refresh(force);
        }
        // Title displays thread state, so must be updated
        setTitle();
    }

    @Override
    public void vmProcessTerminated() {
        reconstructView();
    }

    @Override
    public void stackFrameFocusChanged(MaxStackFrame oldStackFrame, MaxStackFrame newStackFrame) {
        // The focus mechanism will suppress calls where the stack frame is either equal or
        // the "same as" the previous frame, the latter of which happens when the frame is on the
        // top and gets represented by a different object.
        reconstructView();
    }

    @Override
    public void addressFocusChanged(Address oldAddress, Address newAddress) {
        forceRefresh();
    }

    @Override
    public InspectorAction getViewOptionsAction() {
        return new InspectorAction(inspection(), "View Options") {
            @Override
            public void procedure() {
                new TableColumnVisibilityPreferences.ColumnPreferencesDialog<CompiledStackFrameColumnKind>(inspection(), "Stack Frame View Options", viewPreferences);
            }
        };
    }

    @Override
    public void watchpointSetChanged() {
        if (vm().state().processState() == STOPPED) {
            forceRefresh();
        }
    }

    public void viewConfigurationChanged() {
        reconstructView();
    }

    public void tableColumnViewPreferencesChanged() {
        reconstructView();
    }

    @Override
    public void inspectorClosing() {
        Trace.line(1, tracePrefix() + " closing");
        stackFrameInspector = null;
        viewPreferences.removeListener(this);
        super.inspectorClosing();
    }
}
