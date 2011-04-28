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

import java.awt.*;
import java.awt.event.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;

import com.sun.max.gui.*;
import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.util.*;
import com.sun.max.ins.value.*;
import com.sun.max.ins.view.*;
import com.sun.max.ins.view.InspectionViews.ViewKind;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;

/**
 * A singleton inspector that displays stack contents for the thread in the VM that is the current user focus.
 *
 * @author Doug Simon
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public final class StackInspector extends Inspector {
    private static final int TRACE_VALUE = 1;
    private static final ViewKind VIEW_KIND = ViewKind.STACK;
    private static final String SHORT_NAME = "Stack";
    private static final String LONG_NAME = "Stack Inspector";
    private static final String GEOMETRY_SETTINGS_KEY = "stackInspectorGeometry";

    private static final int DEFAULT_MAX_FRAMES_DISPLAY = 200;
    private static final String MAX_FRAMES_DISPLAY_PROPERTY = "inspector.max.stack.frames.display";
    private static int defaultMaxFramesDisplay;
    static {
        final String value = System.getProperty(MAX_FRAMES_DISPLAY_PROPERTY);
        if (value != null) {
            try {
                defaultMaxFramesDisplay = Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                InspectorError.unexpected(MAX_FRAMES_DISPLAY_PROPERTY + " value " +  value + " not an integer");
            }
        } else {
            defaultMaxFramesDisplay = DEFAULT_MAX_FRAMES_DISPLAY;
        }
    }

    public static final class StackViewManager extends AbstractSingletonViewManager<StackInspector> {

        protected StackViewManager(Inspection inspection) {
            super(inspection, VIEW_KIND, SHORT_NAME, LONG_NAME);
        }

        public boolean isSupported() {
            return true;
        }

        public boolean isEnabled() {
            return true;
        }

        @Override
        protected StackInspector createView(Inspection inspection) {
            return new StackInspector(inspection);
        }

    }

    // Will be non-null before any instances created.
    private static StackViewManager viewManager = null;

    public static StackViewManager makeViewManager(Inspection inspection) {
        if (viewManager == null) {
            viewManager = new StackViewManager(inspection);
        }
        return viewManager;
    }

    private final class StackFrameListCellRenderer extends MachineCodeLabel implements ListCellRenderer {

        StackFrameListCellRenderer(Inspection inspection) {
            super(inspection, "");
        }

        public Component getListCellRendererComponent(JList list, Object value, int modelIndex, boolean isSelected, boolean cellHasFocus) {
            final MaxStackFrame stackFrame = (MaxStackFrame) value;
            String methodName = "";
            String toolTip = null;
            if (stackFrame instanceof MaxStackFrame.Compiled) {
                final MaxCompiledCode compiledCode = stackFrame.compiledCode();
                methodName += inspection().nameDisplay().veryShortName(compiledCode);
                toolTip = htmlify(inspection().nameDisplay().longName(compiledCode, stackFrame.ip()));
                if (compiledCode != null) {

                    try {
                        vm().acquireLegacyVMAccess();
                        try {
                            final TeleClassMethodActor teleClassMethodActor = compiledCode.getTeleClassMethodActor();
                            if (teleClassMethodActor != null && teleClassMethodActor.isSubstituted()) {
                                methodName += inspection().nameDisplay().methodSubstitutionShortAnnotation(teleClassMethodActor);
                                try {
                                    toolTip += inspection().nameDisplay().methodSubstitutionLongAnnotation(teleClassMethodActor);
                                } catch (Exception e) {
                                    // There's corner cases where we can't obtain detailed information for the tool tip (e.g., the method we're trying to get the substitution info about
                                    //  is being constructed. Instead of propagating the exception, just use a default tool tip. [Laurent].
                                    toolTip += inspection().nameDisplay().unavailableDataLongText();
                                }
                            }
                        } finally {
                            vm().releaseLegacyVMAccess();
                        }
                    } catch (MaxVMBusyException e) {
                        methodName += inspection().nameDisplay().unavailableDataShortText();
                        toolTip = inspection().nameDisplay().unavailableDataLongText();
                    }
                }
            } else if (stackFrame instanceof TruncatedStackFrame) {
                methodName += "*select here to extend the display*";
            } else if (stackFrame instanceof MaxStackFrame.Error) {
                methodName += "*a stack walker error occurred*";
                final MaxStackFrame.Error errorStackFrame = (MaxStackFrame.Error) stackFrame;
                toolTip = errorStackFrame.errorMessage();
            } else {
                InspectorWarning.check(stackFrame instanceof MaxStackFrame.Native, "Unhandled type of non-native stack frame: " + stackFrame.getClass().getName());
                final Pointer instructionPointer = stackFrame.ip();
                final MaxExternalCode externalCode = vm().codeCache().findExternalCode(instructionPointer);
                if (externalCode != null) {
                    // native that we know something about
                    methodName += inspection().nameDisplay().shortName(externalCode);
                    toolTip = inspection().nameDisplay().longName(externalCode);
                } else {
                    methodName += "nativeMethod:" + instructionPointer.to0xHexString();
                    toolTip = "nativeMethod";
                }
            }
            if (modelIndex == 0) {
                setToolTipPrefix("IP in frame " + modelIndex + " points at:<br>");
                setForeground(style().wordCallEntryPointColor());
            } else {
                setToolTipPrefix("call return in frame " + modelIndex + " points at:<br>");
                setForeground(style().wordCallReturnPointColor());
            }
            setWrappedText(Integer.toString(modelIndex) + ":  " + htmlify(methodName));
            setWrappedToolTipText(toolTip);
            setFont(style().defaultFont());
            setBackground(isSelected ? stackFrameList.getSelectionBackground() : stackFrameList.getBackground());
            return this;
        }

    }

    /**
     * Listens for mouse events over the stack frame list so that the right button
     * will bring up a contextual menu.
     */
    private final MouseListener frameMouseListener = new InspectorMouseClickAdapter(inspection()) {

        @Override
        public void procedure(final MouseEvent mouseEvent) {
            switch(inspection().gui().getButton(mouseEvent)) {
                case MouseEvent.BUTTON3:
                    int index = stackFrameList.locationToIndex(mouseEvent.getPoint());
                    if (index >= 0 && index < stackFrameList.getModel().getSize()) {
                        getPopupMenu(index, mouseEvent).show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                    }
                    break;
            }
        }
    };

    /**
     * Listens for change of selection in the list of stack frames and passes this along
     * to the global focus setting.
     */
    private final ListSelectionListener frameSelectionListener = new ListSelectionListener() {

        public void valueChanged(ListSelectionEvent listSelectionEvent) {
            if (!listSelectionEvent.getValueIsAdjusting()) {
                final int index = stackFrameList.getSelectedIndex();
                if (index >= 0 && index < stackFrameListModel.getSize()) {
                    MaxStackFrame stackFrame = (MaxStackFrame) stackFrameListModel.get(index);
                    // New stack frame selection; set the global focus.
                    inspection().focus().setStackFrame(stackFrame, false);
                    if (stackFrame instanceof TruncatedStackFrame) {
                        // A user selection of the pseudo frame that marks the end
                        // of a partial (truncated) stack list has the effect of
                        // doubling the number of frames so that you can see more.
                        maxFramesDisplay *= 2;
                        lastChangedState = vm().state();
                        // Reconstruct the frame display with the new, extended cap
                        forceRefresh();
                        // Finally, reset the focus on the actual stack, frame, not the
                        // special frame that was just replaced after the maximum increased.
                        stackFrame = (MaxStackFrame) stackFrameListModel.get(index);
                        focus().setStackFrame(stackFrame, true);
                    }
                }
            }
        }
    };

    private final StackFrameListCellRenderer stackFrameListCellRenderer = new StackFrameListCellRenderer(inspection());
    private final InspectorAction copyStackToClipboardAction = new CopyStackToClipboardAction();

    private MaxStack stack = null;

    /**
     * Marks the most recent time in VM state history that we refreshed from the stack.
     */
    private MaxVMState lastUpdatedState = null;

    /**
     * Marks the most recent time in VM state history when the stack was observed to have changed structurally.
     */
    private MaxVMState lastChangedState = null;

    private InspectorPanel contentPane = null;
    private DefaultListModel stackFrameListModel = null;
    private JList stackFrameList = null;

    /**
     * The maximum number of frames to be displayed at any given time, to defend against extremely large
     * stacks.  The user can click in the final pseudo-frame in the display when it has been limited
     * this way, and this will cause the number displayed to grow.
     */
    private int maxFramesDisplay = defaultMaxFramesDisplay;

    public StackInspector(Inspection inspection) {
        super(inspection, VIEW_KIND, GEOMETRY_SETTINGS_KEY);
        Trace.begin(TRACE_VALUE,  tracePrefix() + " initializing");
        final InspectorFrame frame = createFrame(true);

        frame.makeMenu(MenuKind.DEFAULT_MENU).add(defaultMenuItems(MenuKind.DEFAULT_MENU));

        final InspectorMenu editMenu = frame.makeMenu(MenuKind.EDIT_MENU);
        editMenu.add(copyStackToClipboardAction);

        final InspectorMenu memoryMenu = frame.makeMenu(MenuKind.MEMORY_MENU);
        memoryMenu.add(actions().inspectSelectedThreadStackMemory("Inspect memory for stack"));
        memoryMenu.add(defaultMenuItems(MenuKind.MEMORY_MENU));
        memoryMenu.add(views().activateSingletonViewAction(ViewKind.ALLOCATIONS));

        frame.makeMenu(MenuKind.VIEW_MENU).add(defaultMenuItems(MenuKind.VIEW_MENU));

        forceRefresh();
        Trace.end(TRACE_VALUE,  tracePrefix() + " initializing");
    }

    @Override
    public String getTextForTitle() {
        String title = viewManager.shortName() + ": ";
        if (!inspection().hasProcess()) {
            title += inspection().nameDisplay().noProcessShortText();
        } else if (stack != null && stack.thread() != null) {
            title += inspection().nameDisplay().longNameWithState(stack.thread());
        }
        return title;
    }

    @Override
    public void createView() {
        lastUpdatedState = null;
        lastChangedState = null;
        final MaxThread thread = inspection().focus().thread();
        contentPane = new InspectorPanel(inspection(), new BorderLayout());
        if (thread != null) {
            stack = thread.stack();
            lastUpdatedState = stack.lastUpdated();
            lastChangedState = stack.lastChanged();
            assert stack != null;
            stackFrameListModel = new DefaultListModel();
            stackFrameList = new JList(stackFrameListModel);
            stackFrameList.setCellRenderer(stackFrameListCellRenderer);

            final JPanel header = new InspectorPanel(inspection(), new SpringLayout());
            final TextLabel stackStartLabel = new TextLabel(inspection(), "start: ");
            stackStartLabel.setToolTipText("Stack memory start location");
            header.add(stackStartLabel);
            final WordValueLabel stackStartValueLabel = new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD, stack.memoryRegion().start(), contentPane);
            stackStartValueLabel.setToolTipPrefix("Stack memory start @ ");
            header.add(stackStartValueLabel);
            final TextLabel stackSizeLabel = new TextLabel(inspection(), "size: ");
            stackSizeLabel.setToolTipText("Stack size");
            header.add(stackSizeLabel);
            final DataLabel.LongAsDecimal stackSizeValueLabel = new DataLabel.LongAsDecimal(inspection());
            stackSizeValueLabel.setToolTipPrefix("Stack size ");
            stackSizeValueLabel.setValue(stack.memoryRegion().nBytes());
            header.add(stackSizeValueLabel);
            SpringUtilities.makeCompactGrid(header, 2);
            contentPane.add(header, BorderLayout.NORTH);

            stackFrameList.setSelectionInterval(1, 0);
            stackFrameList.setVisibleRowCount(10);
            stackFrameList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            stackFrameList.setLayoutOrientation(JList.VERTICAL);
            stackFrameList.addMouseListener(frameMouseListener);
            stackFrameList.addListSelectionListener(frameSelectionListener);

            final JScrollPane listScrollPane = new InspectorScrollPane(inspection(), stackFrameList);
            contentPane.add(listScrollPane, BorderLayout.CENTER);
        }
        setContentPane(contentPane);
        forceRefresh();
        // TODO (mlvdv) try to set frame selection to match global focus at creation; doesn't display.
        frameFocusChanged(null, inspection().focus().stackFrame());
    }

    @Override
    protected void refreshState(boolean force) {
        if (stack != null && stack.thread() != null && stack.thread().isLive()) {
            if (force || stack.lastUpdated() == null || vm().state().newerThan(lastUpdatedState)) {
                final List<MaxStackFrame> frames = stack.frames();
                if (!frames.isEmpty()) {
                    if (force || stack.lastChanged().newerThan(this.lastChangedState)) {
                        stackFrameListModel.clear();
                        int position = stackFrameListModel.size();
                        for (MaxStackFrame stackFrame : frames) {
                            if (position  >= maxFramesDisplay) {
                                stackFrameListModel.addElement(new TruncatedStackFrame(stackFrame.stack(), stackFrame,  position));
                                inspection().gui().informationMessage("stack depth of " + stackFrameListModel.size() + " exceeds " + maxFramesDisplay + ": truncated", "Stack Inspector");
                                break;
                            }
                            stackFrameListModel.addElement(stackFrame);
                            position++;
                        }
                        this.lastChangedState = stack.lastChanged();
                    } else {
                        // The stack is structurally unchanged with respect to methods,
                        // which typically happens after a single step.
                        // Avoid a complete redisplay for performance reasons.
                        // However, the object representing the top frame may be different,
                        // in which case the state of the old frame object is out of date.
                        final MaxStackFrame newTopFrame = frames.get(0);
                        stackFrameListModel.set(0, newTopFrame);
                    }
                    lastUpdatedState = stack.lastUpdated();
                }
            }
        }
        // The title displays thread state, so must be updated.
        setTitle();
    }

    @Override
    public void threadFocusSet(MaxThread oldThread, MaxThread thread) {
        reconstructView();
    }

    @Override
    public void frameFocusChanged(MaxStackFrame oldStackFrame, MaxStackFrame newStackFrame) {
        if (stackFrameList != null) {
            if (newStackFrame == null || newStackFrame.stack().thread() != this.stack.thread()) {
                stackFrameList.clearSelection();
            } else {
                final int oldIndex = stackFrameList.getSelectedIndex();
                for (int index = 0; index < stackFrameListModel.getSize(); index++) {
                    final MaxStackFrame stackFrame = (MaxStackFrame) stackFrameListModel.get(index);
                    if (stackFrame.isSameFrame(newStackFrame)) {
                        if (index != oldIndex) {
                            stackFrameList.setSelectedIndex(index);
                            stackFrameList.ensureIndexIsVisible(index);
                        }
                        break;
                    }
                }
            }
        }
    }

    public void viewConfigurationChanged() {
        reconstructView();
    }

    @Override
    public void vmProcessTerminated() {
        reconstructView();
    }

    @Override
    public void inspectorClosing() {
        Trace.line(TRACE_VALUE, tracePrefix() + " closing");
        super.inspectorClosing();
    }

    private String javaStackFrameName(MaxStackFrame.Compiled javaStackFrame) {
        final Address address = javaStackFrame.ip();
        final MaxCompiledCode compiledCode = vm().codeCache().findCompiledCode(address);
        String name;
        if (compiledCode != null) {
            name = inspection().nameDisplay().veryShortName(compiledCode);
            final TeleClassMethodActor teleClassMethodActor = compiledCode.getTeleClassMethodActor();
            if (teleClassMethodActor != null && teleClassMethodActor.isSubstituted()) {
                name = name + inspection().nameDisplay().methodSubstitutionShortAnnotation(teleClassMethodActor);
            }
        } else {
            final MethodActor classMethodActor = javaStackFrame.compiledCode().classMethodActor();
            name = classMethodActor.format("%h.%n");
        }
        return name;
    }

    private InspectorPopupMenu getPopupMenu(int row, MouseEvent mouseEvent) {
        final MaxStackFrame stackFrame = (MaxStackFrame) stackFrameListModel.get(row);
        final InspectorPopupMenu menu = new InspectorPopupMenu("Stack Frame");

        menu.add(new InspectorAction(inspection(), "Select frame (Left-Button)") {
            @Override
            protected void procedure() {
                inspection().focus().setStackFrame(stackFrame, false);
            }
        });
        if (stackFrame instanceof MaxStackFrame.Compiled) {
            final MaxStackFrame.Compiled javaStackFrame = (MaxStackFrame.Compiled) stackFrame;
            final String frameName = javaStackFrameName(javaStackFrame);
            menu.add(actions().inspectStackFrameMemory(javaStackFrame, "Inspect memory for frame" + frameName));
            menu.add(new InspectorAction(inspection(), "View frame " + frameName) {
                @Override
                protected void procedure() {
                    inspection().focus().setStackFrame(stackFrame, false);
                    views().activateSingletonView(ViewKind.STACK_FRAME).highlight();
                }
            });
        }
        if (stackFrame instanceof MaxStackFrame.Native) {
            final Pointer instructionPointer = stackFrame.ip();
            final MaxExternalCode externalCode = vm().codeCache().findExternalCode(instructionPointer);
            if (externalCode == null) {
                menu.add(new InspectorAction(inspection(), "Open external code dialog...") {
                    @Override
                    protected void procedure() {
                        MaxCodeLocation codeLocation = stackFrame.codeLocation();
                        if (codeLocation == null) {
                            gui().errorMessage("Stack frame has no code location");
                        } else {
                            focus().setCodeLocation(codeLocation, true);
                        }
                    }
                });
            }
        }
        return menu;
    }

    private final class CopyStackToClipboardAction extends InspectorAction {

        private CopyStackToClipboardAction() {
            super(inspection(), "Copy stack list to clipboard");
        }

        @Override
        public void procedure() {
            // (mlvdv)  This is pretty awkward, but has the virtue that it reproduces exactly what's displayed.  Could be improved.
            final StringBuilder result = new StringBuilder(100);
            final ListCellRenderer cellRenderer = stackFrameList.getCellRenderer();
            for (int index = 0; index < stackFrameListModel.getSize(); index++) {
                final Object elementAt = stackFrameListModel.getElementAt(index);
                final JLabel jLabel = (JLabel) cellRenderer.getListCellRendererComponent(stackFrameList, elementAt, index, false, false);
                result.append(jLabel.getText()).append("\n");
            }
            gui().postToClipboard(result.toString());
        }
    }

}
