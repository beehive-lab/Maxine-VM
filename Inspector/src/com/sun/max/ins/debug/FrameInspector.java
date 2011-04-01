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

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.InspectionSettings.SaveSettingsListener;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.gui.TableColumnVisibilityPreferences.TableColumnViewPreferenceListener;
import com.sun.max.ins.util.*;
import com.sun.max.ins.view.*;
import com.sun.max.ins.view.InspectionViews.ViewKind;
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
public final class FrameInspector extends Inspector implements TableColumnViewPreferenceListener {

    private static final int TRACE_VALUE = 1;
    private static final ViewKind VIEW_KIND = ViewKind.FRAME;
    private static final String SHORT_NAME = "Frame";
    private static final String LONG_NAME = "Frame Inspector";
    private static final String GEOMETRY_SETTINGS_KEY = "frameInspectorGeometry";

    private static final class FrameViewManager extends AbstractSingletonViewManager<FrameInspector> {

        protected FrameViewManager(Inspection inspection) {
            super(inspection, VIEW_KIND, SHORT_NAME, LONG_NAME);
        }

        public boolean isSupported() {
            return true;
        }

        public boolean isEnabled() {
            return inspection().hasProcess() && focus().hasThread();
        }

        public FrameInspector activateView(Inspection inspection) {
            if (inspector == null) {
                inspector = new FrameInspector(inspection);
            }
            return inspector;
        }
    }

    // Will be non-null before any instances created.
    private static FrameViewManager viewManager = null;

    public static ViewManager makeViewManager(Inspection inspection) {
        if (viewManager == null) {
            viewManager = new FrameViewManager(inspection);
        }
        return viewManager;
    }

    // This is a singleton viewer, so only use a single level of view preferences.
    private static CompiledStackFrameViewPreferences viewPreferences;

    private final SaveSettingsListener saveSettingsListener = createGeometrySettingsListener(this, GEOMETRY_SETTINGS_KEY);

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

    private MaxStackFrame stackFrame;
    private final InspectorPanel nullFramePanel;
    private final InspectorPanel truncatedFramePanel;
    private CompiledStackFramePanel compiledStackFramePanel = null;


    private final InspectorAction copyStackFrameToClipboardAction = new CopyStackFrameToClipboardAction();


    public FrameInspector(Inspection inspection) {
        super(inspection, VIEW_KIND);
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
        memoryMenu.add(actions().activateSingletonView(ViewKind.ALLOCATIONS));

        frame.makeMenu(MenuKind.VIEW_MENU).add(defaultMenuItems(MenuKind.VIEW_MENU));

        forceRefresh();
        Trace.end(1,  tracePrefix() + " initializing");
    }

    @Override
    protected SaveSettingsListener saveSettingsListener() {
        return saveSettingsListener;
    }

    @Override
    public String getTextForTitle() {
        final StringBuilder sb = new StringBuilder(viewManager.shortName() + ": ");
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
    public void frameFocusChanged(MaxStackFrame oldStackFrame, MaxStackFrame newStackFrame) {
        // The focus mechanism will suppress calls where the stack frame is identical to the previous frame.
        if (newStackFrame != null && newStackFrame.isSameFrame(stackFrame)) {
            // The frame object is different, but it represents the same frame; this typically happens
            // when there has been a single step that only affects the top frame.
            this.stackFrame = newStackFrame;
            if (compiledStackFramePanel != null) {
                compiledStackFramePanel.setStackFrame(stackFrame);
            }
            forceRefresh();
        } else {
            // Entirely new frame; start over
            reconstructView();
        }
    }

    @Override
    public void addressFocusChanged(Address oldAddress, Address newAddress) {
        forceRefresh();
    }

    @Override
    public void watchpointSetChanged() {
        if (vm().state().processState() == STOPPED) {
            forceRefresh();
        }
    }

    @Override
    public InspectorAction getViewOptionsAction() {
        return new InspectorAction(inspection(), "View Options") {
            @Override
            public void procedure() {
                new TableColumnVisibilityPreferences.ColumnPreferencesDialog<CompiledStackFrameColumnKind>(inspection(), viewManager.shortName() + " View Options", viewPreferences);
            }
        };
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
        viewPreferences.removeListener(this);
        super.inspectorClosing();
    }


    @Override
    public void vmProcessTerminated() {
        reconstructView();
    }
}
