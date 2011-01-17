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
import com.sun.max.ins.InspectionSettings.SaveSettingsListener;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.gui.TableColumnVisibilityPreferences.TableColumnViewPreferenceListener;
import com.sun.max.ins.memory.*;
import com.sun.max.ins.util.*;
import com.sun.max.ins.value.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
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
public class StackInspector extends Inspector implements TableColumnViewPreferenceListener {

    // Set to null when inspector closed.
    private static StackInspector stackInspector;

    /**
     * Displays the (singleton) inspector, creating it if needed.
     */
    public static StackInspector make(Inspection inspection) {
        if (stackInspector == null) {
            stackInspector = new StackInspector(inspection);
        }
        return stackInspector;
    }

    private static final int DEFAULT_MAX_FRAMES_DISPLAY = 500;
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

    private static CompiledStackFrameViewPreferences viewPreferences;

    /**
     * A specially wrapped stack frame used to mark the last frame in a
     * truncated view of the stack.
     */
    private static final class TruncatedStackFrame implements MaxStackFrame {
        private final MaxStack stack;
        private final MaxStackFrame stackFrame;
        private final int position;
        final MaxCompiledCode compiledCode;

        TruncatedStackFrame(MaxStack stack, MaxStackFrame stackFrame, int position) {
            this.stack = stack;
            this.stackFrame = stackFrame;
            this.position = position;
            this.compiledCode = vm().codeCache().findCompiledCode(stackFrame.ip());
        }

        public MaxVM vm() {
            return stack.vm();
        }

        public String entityName() {
            return toString();
        }

        public String entityDescription() {
            return "A pseudo stack frame created to represent a large number of stack frames that couldn't be displayed";
        }

        public MaxCompiledCode compiledCode() {
            return compiledCode;
        }

        public MaxEntityMemoryRegion<MaxStackFrame> memoryRegion() {
            return null;
        }

        public boolean contains(Address address) {
            return false;
        }

        public MaxStack stack() {
            return stack;
        }

        public int position() {
            return position;
        }

        public boolean isTop() {
            return position == 0;
        }

        public Pointer ip() {
            return stackFrame.ip();
        }

        public Pointer sp() {
            return stackFrame.sp();
        }

        public Pointer fp() {
            return stackFrame.fp();
        }

        public MaxCodeLocation codeLocation() {
            return stackFrame.codeLocation();
        }

        public boolean isSameFrame(MaxStackFrame stackFrame) {
            if (stackFrame instanceof TruncatedStackFrame) {
                final TruncatedStackFrame otherFrame = (TruncatedStackFrame) stackFrame;
                return stackFrame.isSameFrame(otherFrame.stackFrame);
            }
            return false;
        }

        @Override
        public String toString() {
            return "<truncated frame>" + stackFrame;
        }
    }

    private final SaveSettingsListener saveSettingsListener = createGeometrySettingsClient(this, "stackInspectorGeometry");

    /**
     * Marks the last time in VM state history that we refreshed from the stack.
     */
    private MaxVMState lastUpdatedState = null;

    /**
     * Marks the last time in VM state history when the stack changed structurally.
     */
    private MaxVMState lastChangedState = null;

    private MaxStack stack = null;
    private InspectorPanel contentPane = null;
    private DefaultListModel stackFrameListModel = null;
    private JList stackFrameList = null;
    private JSplitPane splitPane = null;
    private JPanel nativeFrame = null;

    private final FrameSelectionListener frameSelectionListener = new FrameSelectionListener();
    private final StackFrameListCellRenderer stackFrameListCellRenderer = new StackFrameListCellRenderer(inspection());

    private CompiledStackFramePanel selectedFramePanel;

    private final class StackFrameListCellRenderer extends TargetCodeLabel implements ListCellRenderer {

        StackFrameListCellRenderer(Inspection inspection) {
            super(inspection, "");
            setOpaque(true);
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
            setText(Integer.toString(modelIndex) + ":  " + methodName);
            setWrappedToolTipText(toolTip);
            setFont(style().defaultFont());
            return this;
        }

    }

    /**
     * Responds to a new selection in the display of stack frames.
     * <br>
     * With each call, you get a bunch of events for which the selection index is -1, followed
     * by one more such event for which the selection index is 0.
     * <br>
     * When the new selection is legitimate, update all state related to the newly selected
     * frame.
     */
    private class FrameSelectionListener implements ListSelectionListener {

        public void valueChanged(ListSelectionEvent listSelectionEvent) {
            if (listSelectionEvent.getValueIsAdjusting()) {
                return;
            }
            final int index = stackFrameList.getSelectedIndex();
            selectedFramePanel = null;
            final int dividerLocation = splitPane.getDividerLocation();

            final Component oldRightComponent = splitPane.getRightComponent();
            Component newRightComponent = oldRightComponent;
            if (index >= 0 && index < stackFrameListModel.getSize()) {
                final MaxStackFrame stackFrame = (MaxStackFrame) stackFrameListModel.get(index);
                // New stack frame selection; set the global focus.
                inspection().focus().setStackFrame(stackFrame, false);
                if (stackFrame instanceof MaxStackFrame.Compiled) {
                    final MaxStackFrame.Compiled compiledStackFrame = (MaxStackFrame.Compiled) stackFrame;
                    selectedFramePanel = new DefaultCompiledStackFramePanel(inspection(), compiledStackFrame, viewPreferences);
                    newRightComponent = selectedFramePanel;
                } else if (stackFrame instanceof TruncatedStackFrame) {
                    maxFramesDisplay *= 2;
                    lastChangedState = vm().state();
                    refreshView(true);
                } else {
                    newRightComponent = nativeFrame;
                }
            }
            if (oldRightComponent != newRightComponent) {
                splitPane.setRightComponent(newRightComponent);
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        splitPane.setDividerLocation(dividerLocation);
                    }
                });
            }
        }
    }

    public StackInspector(Inspection inspection) {
        super(inspection);
        Trace.begin(1,  tracePrefix() + " initializing");

        viewPreferences = CompiledStackFrameViewPreferences.globalPreferences(inspection);
        viewPreferences.addListener(this);

        final InspectorFrame frame = createFrame(true);

        frame.makeMenu(MenuKind.DEFAULT_MENU).add(defaultMenuItems(MenuKind.DEFAULT_MENU));

        final InspectorMenu editMenu = frame.makeMenu(MenuKind.EDIT_MENU);
        editMenu.add(copyStackToClipboardAction);

        final InspectorMenu memoryMenu = frame.makeMenu(MenuKind.MEMORY_MENU);
        memoryMenu.add(actions().inspectSelectedThreadMemoryWords("Inspect memory for thread"));
        memoryMenu.add(defaultMenuItems(MenuKind.MEMORY_MENU));
        final JMenuItem viewMemoryRegionsMenuItem = new JMenuItem(actions().viewMemoryRegions());
        viewMemoryRegionsMenuItem.setText("View Memory Regions");
        memoryMenu.add(viewMemoryRegionsMenuItem);

        frame.makeMenu(MenuKind.VIEW_MENU).add(defaultMenuItems(MenuKind.VIEW_MENU));

        refreshView(true);
        Trace.end(1,  tracePrefix() + " initializing");
    }

    @Override
    protected Rectangle defaultFrameBounds() {
        return inspection().geometry().stackFrameDefaultBounds();
    }

    @Override
    protected SaveSettingsListener saveSettingsListener() {
        return saveSettingsListener;
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
            final DataLabel.IntAsDecimal stackSizeValueLabel = new DataLabel.IntAsDecimal(inspection());
            stackSizeValueLabel.setToolTipPrefix("Stack size ");
            stackSizeValueLabel.setValue(stack.memoryRegion().size().toInt());
            header.add(stackSizeValueLabel);
            SpringUtilities.makeCompactGrid(header, 2);
            contentPane.add(header, BorderLayout.NORTH);

            stackFrameList.setSelectionInterval(1, 0);
            stackFrameList.setVisibleRowCount(10);
            stackFrameList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            stackFrameList.setLayoutOrientation(JList.VERTICAL);
            stackFrameList.addMouseListener(new InspectorMouseClickAdapter(inspection()) {

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
            });

            stackFrameList.addListSelectionListener(frameSelectionListener);

            nativeFrame = new InspectorPanel(inspection());
            final JScrollPane listScrollPane = new InspectorScrollPane(inspection(), stackFrameList);
            splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT) {
                @Override
                public void setLeftComponent(Component component) {
                    super.setLeftComponent(setMinimumSizeToZero(component));
                }

                @Override
                public void setRightComponent(Component component) {
                    super.setRightComponent(setMinimumSizeToZero(component));
                }

                // Enable the user to completely hide either the stack or selected frame panel
                private Component setMinimumSizeToZero(Component component) {
                    if (component != null) {
                        component.setMinimumSize(new Dimension(0, 0));
                    }
                    return component;
                }
            };
            splitPane.setOneTouchExpandable(true);
            splitPane.setLeftComponent(listScrollPane);
            splitPane.setRightComponent(nativeFrame);

            contentPane.add(splitPane, BorderLayout.CENTER);
        }
        setContentPane(contentPane);
        refreshView(true);
        // TODO (mlvdv) try to set frame selection to match global focus; doesn't work.
        updateFocusSelection(inspection().focus().stackFrame());

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // System.err.println("setting divider location in stack inspector for " + inspection().inspectionThreadName(_thread));
                // Try to place the split pane divider in the middle of the split pane's space initially
                splitPane.setDividerLocation(0.5d);
            }
        });
    }

    @Override
    public String getTextForTitle() {
        String title = "Stack: ";
        if (stack != null && stack.thread() != null) {
            title += inspection().nameDisplay().longNameWithState(stack.thread());
        }
        return title;
    }

    @Override
    public InspectorAction getViewOptionsAction() {
        return new InspectorAction(inspection(), "View Options") {
            @Override
            public void procedure() {
                //
                //new SimpleDialog(inspection(), globalPreferences(inspection()).getPanel(), "Stack Inspector view options", true);
                new TableColumnVisibilityPreferences.ColumnPreferencesDialog<CompiledStackFrameColumnKind>(inspection(), "Stack Frame Options", viewPreferences);
            }
        };
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
            final int frameSize = javaStackFrame.layout().frameSize();
            final Pointer stackPointer = javaStackFrame.sp();
            final MaxMemoryRegion memoryRegion = new InspectorMemoryRegion(vm(), "", stackPointer, Size.fromInt(frameSize));
            final String frameName = javaStackFrameName(javaStackFrame);
            menu.add(actions().inspectRegionMemoryWords(memoryRegion, "stack frame for " + frameName, "Inspect memory for frame" + frameName));
        }
        if (stackFrame instanceof MaxStackFrame.Native) {
            final Pointer instructionPointer = stackFrame.ip();
            final MaxExternalCode externalCode = vm().codeCache().findExternalCode(instructionPointer);
            if (externalCode == null) {
                menu.add(new InspectorAction(inspection(), "Open external code dialog...") {
                    @Override
                    protected void procedure() {
                        focus().setCodeLocation(stackFrame.codeLocation(), true);
                    }
                });
            }
        }

        return menu;
    }

    @Override
    protected void refreshView(boolean force) {
        InspectorError.check(stack != null);
        if (stack.thread() != null && stack.thread().isLive()) {
            if (force || stack.lastUpdated() == null || vm().state().newerThan(lastUpdatedState)) {
                final List<MaxStackFrame> frames = stack.frames();
                assert !frames.isEmpty();
                if (force || stack.lastChanged().newerThan(this.lastChangedState)) {
                    stackFrameListModel.clear();
                    addToModel(frames);
                    this.lastChangedState = stack.lastChanged();
                } else {
                    // The stack is structurally unchanged with respect to methods,
                    // which typically happens after a single step.
                    // Avoid a complete redisplay for performance reasons.
                    // However, the object representing the top frame may be different,
                    // in which case the state of the old frame object is out of date.
                    final MaxStackFrame newTopFrame = frames.get(0);
                    stackFrameListModel.set(0, newTopFrame);
                    if (selectedFramePanel != null && selectedFramePanel.stackFrame().isTop() && selectedFramePanel.stackFrame().isSameFrame(newTopFrame)) {
                        // If the top frame is selected, update the panel displaying its contents
                        selectedFramePanel.setStackFrame(newTopFrame);
                    }
                }
                lastUpdatedState = stack.lastUpdated();
                if (selectedFramePanel != null) {
                    selectedFramePanel.refresh(force);
                }
                final InspectorPanel panel = (InspectorPanel) splitPane.getRightComponent();
                panel.refresh(true);
            }
        }
        super.refreshView(force);
        // The title displays thread state, so must be updated.
        setTitle();
    }

    @Override
    public void stackFrameFocusChanged(MaxStackFrame oldStackFrame, MaxStackFrame newStackFrame) {
        if (newStackFrame.stack().thread() == this.stack.thread()) {
            updateFocusSelection(newStackFrame);
        }
    }

    /**
     * Updates list selection state to agree with focus on a particular stack frame.
     */
    private void updateFocusSelection(MaxStackFrame newStackFrame) {
        if (newStackFrame != null) {
            final int oldIndex = stackFrameList.getSelectedIndex();
            for (int index = 0; index < stackFrameListModel.getSize(); index++) {
                final MaxStackFrame stackFrame = (MaxStackFrame) stackFrameListModel.get(index);
                if (stackFrame.isSameFrame(newStackFrame)) {
                    // The frame is in the list; we may or may not have to update the current selection.
                    if (index != oldIndex) {
                        stackFrameList.setSelectedIndex(index);
                    }
                    return;
                }
            }
        }
        stackFrameList.clearSelection();
    }

    @Override
    public void codeLocationFocusSet(MaxCodeLocation codeLocation, boolean interactiveForNative) {
        if (selectedFramePanel != null) {
            // TODO (mlvdv)  This call is a no-op at present.  What should happen?
            selectedFramePanel.instructionPointerFocusChanged(codeLocation.address().asPointer());
        }
    }

    @Override
    public void threadFocusSet(MaxThread oldThread, MaxThread thread) {
        reconstructView();
    }

    @Override
    public void watchpointSetChanged() {
        if (vm().state().processState() == ProcessState.STOPPED) {
            refreshView(true);
        }
    }

    public void viewConfigurationChanged() {
        reconstructView();
    }

    private int maxFramesDisplay = defaultMaxFramesDisplay;

    /**
     * Add frames to the stack model until {@link #maxFramesDisplay} reached.
     */
    private void addToModel(final List<MaxStackFrame> frames) {
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

    private final InspectorAction copyStackToClipboardAction = new CopyStackToClipboardAction();

    public void tableColumnViewPreferencesChanged() {
        reconstructView();
    }

    @Override
    public void inspectorClosing() {
        Trace.line(1, tracePrefix() + " closing");
        stackInspector = null;
        super.inspectorClosing();
    }

    @Override
    public void vmProcessTerminated() {
        reconstructView();
    }

}
