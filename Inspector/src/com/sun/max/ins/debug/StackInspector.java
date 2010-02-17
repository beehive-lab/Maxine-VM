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
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import com.sun.max.collect.*;
import com.sun.max.gui.*;
import com.sun.max.ins.*;
import com.sun.max.ins.InspectionSettings.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.gui.TableColumnVisibilityPreferences.*;
import com.sun.max.ins.value.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;

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
                ProgramError.unexpected(MAX_FRAMES_DISPLAY_PROPERTY + " value " +  value + " not an integer");
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

        TruncatedStackFrame(MaxStack stack, MaxStackFrame stackFrame, int position) {
            this.stack = stack;
            this.stackFrame = stackFrame;
            this.position = position;
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

        public TargetMethod targetMethod() {
            return stackFrame.targetMethod();
        }

        public boolean isSameFrame(MaxStackFrame stackFrame) {
            if (stackFrame instanceof TruncatedStackFrame) {
                final TruncatedStackFrame otherFrame = (TruncatedStackFrame) stackFrame;
                return stackFrame.isSameFrame(otherFrame.stackFrame);
            }
            return false;
        }


        public String description() {
            return toString();
        }

        @Override
        public String toString() {
            return "<truncated frame>" + stackFrame;
        }
    }

    private final SaveSettingsListener saveSettingsListener = createGeometrySettingsClient(this, "stackInspectorGeometry");

    private MaxStack stack = null;
    private InspectorPanel contentPane = null;
    private  DefaultListModel stackFrameListModel = null;
    private JList stackFrameList = null;
    private JSplitPane splitPane = null;
    private JPanel nativeFrame = null;

    private final FrameSelectionListener frameSelectionListener = new FrameSelectionListener();
    private final StackFrameListCellRenderer stackFrameListCellRenderer = new StackFrameListCellRenderer();

    private boolean stateChanged = true;  // conservative assessment of possible stack change
    private CompiledStackFramePanel selectedFramePanel;

    private final class StackFrameListCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int modelIndex, boolean isSelected, boolean cellHasFocus) {
            final MaxStackFrame stackFrame = (MaxStackFrame) value;
            String name;
            String toolTip = null;
            Component component;
            if (stackFrame instanceof MaxStackFrame.Compiled) {
                final TeleTargetMethod teleTargetMethod = maxVM().makeTeleTargetMethod(stackFrame.targetMethod().codeStart());
                name = inspection().nameDisplay().veryShortName(teleTargetMethod);
                toolTip = inspection().nameDisplay().longName(teleTargetMethod, stackFrame.ip());
                final TeleClassMethodActor teleClassMethodActor = teleTargetMethod.getTeleClassMethodActor();
                if (teleClassMethodActor != null && teleClassMethodActor.isSubstituted()) {
                    name = name + inspection().nameDisplay().methodSubstitutionShortAnnotation(teleClassMethodActor);
                    toolTip = toolTip + inspection().nameDisplay().methodSubstitutionLongAnnotation(teleClassMethodActor);
                }
            } else if (stackFrame instanceof TruncatedStackFrame) {
                name = "*select here to extend the display*";
            } else if (stackFrame instanceof MaxStackFrame.Error) {
                name = "*a stack walker error occurred*";
                final MaxStackFrame.Error errorStackFrame = (MaxStackFrame.Error) stackFrame;
                toolTip = errorStackFrame.errorMessage();
            } else {
                ProgramWarning.check(stackFrame instanceof MaxStackFrame.Native, "Unhandled type of non-native stack frame: " + stackFrame.getClass().getName());
                final Pointer instructionPointer = stackFrame.ip();
                final TeleNativeTargetRoutine teleNativeTargetRoutine = maxVM().findTeleTargetRoutine(TeleNativeTargetRoutine.class, instructionPointer);
                if (teleNativeTargetRoutine != null) {
                    // native that we know something about
                    name = inspection().nameDisplay().shortName(teleNativeTargetRoutine);
                    toolTip = inspection().nameDisplay().longName(teleNativeTargetRoutine);
                } else {
                    name = "nativeMethod:0x" + instructionPointer.toHexString();
                    toolTip = "nativeMethod";
                }
            }
            toolTip = "Stack " + modelIndex + ":  " + toolTip;
            setToolTipText(toolTip);
            component = super.getListCellRendererComponent(list, name, modelIndex, isSelected, cellHasFocus);
            if (modelIndex == 0) {
                component.setForeground(style().wordCallEntryPointColor());
            } else {
                component.setForeground(style().wordCallReturnPointColor());
            }
            return component;
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

            // TODO (mlvdv) Create appropriate stack frame viewers for all the kinds of frames we might find.
            if (index >= 0 && index < stackFrameListModel.getSize()) {
                final MaxStackFrame stackFrame = (MaxStackFrame) stackFrameListModel.get(index);
                // New stack frame selection; set the global focus.
                inspection().focus().setStackFrame(stackFrame, false);
                if (stackFrame instanceof MaxStackFrame.Compiled) {
                    final MaxStackFrame.Compiled compiledStackFrame = (MaxStackFrame.Compiled) stackFrame;
                    selectedFramePanel = new DefaultCompiledStackFramePanel<MaxStackFrame.Compiled>(inspection(), compiledStackFrame, viewPreferences);
                    newRightComponent = selectedFramePanel;
                } else if (stackFrame instanceof TruncatedStackFrame) {
                    maxFramesDisplay *= 2;
                    stateChanged = true;
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
        final MaxThread thread = inspection().focus().thread();
        contentPane = new InspectorPanel(inspection(), new BorderLayout());
        if (thread != null) {
            stack = thread.stack();
            stackFrameListModel = new DefaultListModel();
            stackFrameList = new JList(stackFrameListModel);
            stackFrameList.setCellRenderer(stackFrameListCellRenderer);

            final JPanel header = new InspectorPanel(inspection(), new SpringLayout());
            header.add(new TextLabel(inspection(), "start: "));
            header.add(new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD, stack.memoryRegion().start(), contentPane));
            header.add(new TextLabel(inspection(), "size: "));
            header.add(new DataLabel.IntAsDecimal(inspection(), stack.memoryRegion().size().toInt()));
            SpringUtilities.makeCompactGrid(header, 2);
            contentPane.add(header, BorderLayout.NORTH);

            stackFrameList.setSelectionInterval(1, 0);
            stackFrameList.setVisibleRowCount(10);
            stackFrameList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            stackFrameList.setLayoutOrientation(JList.VERTICAL);
            stackFrameList.addKeyListener(stackFrameKeyTypedListener);
            stackFrameList.addMouseListener(new InspectorMouseClickAdapter(inspection()) {

                @Override
                public void procedure(final MouseEvent mouseEvent) {
                    switch(Inspection.mouseButtonWithModifiers(mouseEvent)) {
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
                // TODO (mlvdv) view options
                //new SimpleDialog(inspection(), globalPreferences(inspection()).getPanel(), "Stack Inspector view options", true);
                new TableColumnVisibilityPreferences.ColumnPreferencesDialog<CompiledStackFrameColumnKind>(inspection(), "Stack Frame Options", viewPreferences);
            }
        };
    }

    private String javaStackFrameName(MaxStackFrame.Compiled javaStackFrame) {
        final Address address = javaStackFrame.ip();
        final TeleTargetMethod teleTargetMethod = maxVM().makeTeleTargetMethod(address);
        String name;
        if (teleTargetMethod != null) {
            name = inspection().nameDisplay().veryShortName(teleTargetMethod);
            final TeleClassMethodActor teleClassMethodActor = teleTargetMethod.getTeleClassMethodActor();
            if (teleClassMethodActor != null && teleClassMethodActor.isSubstituted()) {
                name = name + inspection().nameDisplay().methodSubstitutionShortAnnotation(teleClassMethodActor);
            }
        } else {
            final MethodActor classMethodActor = javaStackFrame.targetMethod().classMethodActor();
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
            final MemoryRegion memoryRegion = new FixedMemoryRegion(stackPointer, Size.fromInt(frameSize), "");
            final String frameName = javaStackFrameName(javaStackFrame);
            menu.add(actions().inspectRegionMemoryWords(memoryRegion, "stack frame for " + frameName, "Inspect memory for frame" + frameName));
        }
        if (stackFrame instanceof MaxStackFrame.Native) {
            final Pointer instructionPointer = stackFrame.ip();
            final TeleNativeTargetRoutine teleNativeTargetRoutine = maxVM().findTeleTargetRoutine(TeleNativeTargetRoutine.class, instructionPointer);
            if (teleNativeTargetRoutine == null) {
                menu.add(new InspectorAction(inspection(), "Open native code dialog...") {
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
        if (stack != null && stack.thread() != null && stack.thread().isLive()) {
            final Sequence<MaxStackFrame> frames = stack.frames();
            assert !frames.isEmpty();
            if (stateChanged || force) {
                stackFrameListModel.clear();
                addToModel(frames);
                stateChanged = false;
            } else {
                // The stack is structurally unchanged with respect to methods,
                // so avoid a complete redisplay for performance reasons.
                // However, the object representing the top frame may be different,
                // in which case the state of the old frame object is out of date.
                final MaxStackFrame newTopFrame = frames.first();
                if (selectedFramePanel != null && selectedFramePanel.stackFrame().isTop() && selectedFramePanel.stackFrame().isSameFrame(newTopFrame)) {
                    selectedFramePanel.setStackFrame(newTopFrame);
                }
            }
            if (selectedFramePanel != null) {
                selectedFramePanel.refresh(force);
            }
            final InspectorPanel panel = (InspectorPanel) splitPane.getRightComponent();
            panel.refresh(true);
        }
        super.refreshView(force);
        // The title displays thread state, so must be updated.
        setTitle();
    }

    @Override
    public void threadStateChanged(MaxThread thread) {
        if (stack != null && thread.equals(this.stack.thread())) {
            stateChanged = thread.framesChanged();
        }
        super.threadStateChanged(thread);
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
        if (maxVMState().processState() == ProcessState.STOPPED) {
            // TODO (mlvdv) workaround - not completely thread safe
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
    private void addToModel(final Sequence<MaxStackFrame> frames) {
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

    /**
     * Watch for shift key being released to display the selected activation's stack frame.
     */
    private final KeyListener stackFrameKeyTypedListener = new KeyListener() {
        public final void keyTyped(KeyEvent keyEvent) {
        }
        public final void keyPressed(KeyEvent keyEvent) {
        }

        public final void keyReleased(KeyEvent keyEvent) {
            if (keyEvent.getKeyCode() == KeyEvent.VK_SHIFT) {
                final int index = stackFrameList.getSelectedIndex();
                if (index >= 0 && index < stackFrameListModel.getSize()) {
                    final MaxStackFrame stackFrame = (MaxStackFrame) stackFrameListModel.get(index);
                    if (stackFrame instanceof MaxStackFrame.Jit) {
                        LocalsInspector.make(inspection(), stack.thread(), (MaxStackFrame.Jit) stackFrame).highlight();
                    }
                }
            }
        }
    };

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
