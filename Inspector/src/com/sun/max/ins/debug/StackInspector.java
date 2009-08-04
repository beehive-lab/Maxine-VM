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
import com.sun.max.ins.value.*;
import com.sun.max.ins.value.WordValueLabel.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.LocalVariableTable.*;
import com.sun.max.vm.collect.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jit.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.JavaStackFrameLayout.*;
import com.sun.max.vm.value.*;

/**
 * A singleton inspector that displays stack contents for the thread in the VM that is the current user focus.
 *
 * @author Doug Simon
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public class StackInspector extends Inspector {

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

    static class TruncatedStackFrame extends StackFrame {
        private StackFrame truncatedStackFrame;

        TruncatedStackFrame(StackFrame callee, StackFrame truncatedStackFrame) {
            super(callee, truncatedStackFrame.instructionPointer, truncatedStackFrame.framePointer, truncatedStackFrame.stackPointer);
            this.truncatedStackFrame = truncatedStackFrame;
        }

        StackFrame getTruncatedStackFrame() {
            return truncatedStackFrame;
        }

        @Override
        public TargetMethod targetMethod() {
            return truncatedStackFrame.targetMethod();
        }

        @Override
        public boolean isSameFrame(StackFrame stackFrame) {
            if (stackFrame instanceof TruncatedStackFrame) {
                final TruncatedStackFrame other = (TruncatedStackFrame) stackFrame;
                return truncatedStackFrame.isSameFrame(other.truncatedStackFrame);
            }
            return false;
        }
    }

    private final SaveSettingsListener saveSettingsListener = createGeometrySettingsClient(this, "stackInspector");

    private MaxThread thread = null;
    private InspectorPanel contentPane = null;
    private  DefaultListModel stackFrameListModel = null;
    private JList stackFrameList = null;
    private JSplitPane splitPane = null;
    private JPanel nativeFrame = null;

    private final FrameSelectionListener frameSelectionListener = new FrameSelectionListener();
    private final StackFrameListCellRenderer stackFrameListCellRenderer = new StackFrameListCellRenderer();

    private boolean stateChanged = true;  // conservative assessment of possible stack change
    private StackFramePanel<? extends StackFrame> selectedFrame;

    @Override
    public void threadStateChanged(MaxThread thread) {
        if (thread.equals(this.thread)) {
            stateChanged = thread.framesChanged();
        }
        super.threadStateChanged(thread);
    }

    @Override
    public void stackFrameFocusChanged(StackFrame oldStackFrame, MaxThread threadForStackFrame, StackFrame newStackFrame) {
        if (threadForStackFrame == thread) {
            final int oldIndex = stackFrameList.getSelectedIndex();
            for (int index = 0; index < stackFrameListModel.getSize(); index++) {
                final StackFrame stackFrame = (StackFrame) stackFrameListModel.get(index);
                if (stackFrame.isSameFrame(newStackFrame)) {
                    if (index != oldIndex) {
                        stackFrameList.setSelectedIndex(index);
                    }
                    break;
                }
            }
        }
    }

    private final class StackFrameListCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int modelIndex, boolean isSelected, boolean cellHasFocus) {
            final StackFrame stackFrame = (StackFrame) value;
            String name;
            String toolTip = null;
            Component component;
            if (stackFrame instanceof JavaStackFrame) {
                final JavaStackFrame javaStackFrame = (JavaStackFrame) stackFrame;
                final Address address = javaStackFrame.instructionPointer;
                final TeleTargetMethod teleTargetMethod = maxVM().makeTeleTargetMethod(address);
                if (teleTargetMethod != null) {
                    name = inspection().nameDisplay().veryShortName(teleTargetMethod);
                    toolTip = inspection().nameDisplay().longName(teleTargetMethod, address);
                    final TeleClassMethodActor teleClassMethodActor = teleTargetMethod.getTeleClassMethodActor();
                    if (teleClassMethodActor != null && teleClassMethodActor.isSubstituted()) {
                        name = name + inspection().nameDisplay().methodSubstitutionShortAnnotation(teleClassMethodActor);
                        toolTip = toolTip + inspection().nameDisplay().methodSubstitutionLongAnnotation(teleClassMethodActor);
                    }
                } else {
                    final MethodActor classMethodActor = javaStackFrame.targetMethod().classMethodActor();
                    name = classMethodActor.format("%h.%n");
                    toolTip = classMethodActor.format("%r %H.%n(%p)");
                }
                if (javaStackFrame instanceof AdapterStackFrame) {
                    name = "frame adapter [" + name + "]";
                    if (javaStackFrame.targetMethod().compilerScheme().equals(StackInspector.this.inspection().maxVM().vmConfiguration().jitScheme())) {
                        toolTip = "optimized-to-JIT frame adapter [ " + toolTip + "]";
                    } else {
                        toolTip = "JIT-to-optimized frame adapter [ " + toolTip + "]";
                    }
                }
            } else if (stackFrame instanceof TruncatedStackFrame) {
                name = "*select here to extend the display*";
            } else if (stackFrame instanceof TeleStackFrameWalker.ErrorStackFrame) {
                name = "*a stack walker error occurred*";
                toolTip = ((TeleStackFrameWalker.ErrorStackFrame) stackFrame).errorMessage();
            } else if (stackFrame  instanceof RuntimeStubStackFrame) {
                final RuntimeStubStackFrame runtimeStubStackFrame = (RuntimeStubStackFrame) stackFrame;
                final RuntimeStub runtimeStub = runtimeStubStackFrame.stub();
                name = runtimeStub.name();
                toolTip = name;
            } else {
                ProgramWarning.check(stackFrame instanceof NativeStackFrame, "Unhandled type of non-native stack frame: " + stackFrame.getClass().getName());
                final Pointer instructionPointer = stackFrame.instructionPointer;
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
            component.setFont(style().defaultCodeFont());
            if (modelIndex == 0) {
                component.setForeground(style().wordCallEntryPointColor());
            } else {
                component.setForeground(style().wordCallReturnPointColor());
            }
            return component;
        }
    }

    public StackInspector(Inspection inspection) {
        super(inspection);
        Trace.begin(1,  tracePrefix() + " initializing");
        createFrame(null);
        frame().menu().addSeparator();
        frame().menu().add(copyStackToClipboardAction);
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
        thread = inspection().focus().thread();
        contentPane = new InspectorPanel(inspection(), new BorderLayout());
        if (thread != null) {
            stackFrameListModel = new DefaultListModel();
            stackFrameList = new JList(stackFrameListModel);
            stackFrameList.setCellRenderer(stackFrameListCellRenderer);

            final JPanel header = new InspectorPanel(inspection(), new SpringLayout());
            header.add(new TextLabel(inspection(), "start: "));
            header.add(new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD, thread.stack().start(), contentPane));
            header.add(new TextLabel(inspection(), "size: "));
            header.add(new DataLabel.IntAsDecimal(inspection(), thread.stack().size().toInt()));
            SpringUtilities.makeCompactGrid(header, 2);
            contentPane.add(header, BorderLayout.NORTH);

            stackFrameList.setSelectionInterval(1, 0);
            stackFrameList.setVisibleRowCount(10);
            stackFrameList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            stackFrameList.setLayoutOrientation(JList.VERTICAL);
            stackFrameList.addKeyListener(stackFrameKeyTypedListener);
            stackFrameList.addMouseListener(new StackFrameMouseClickAdapter(inspection()));

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
        frame().setContentPane(contentPane);
        refreshView(true);

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
        if (thread != null) {
            title += inspection().nameDisplay().longNameWithState(thread);
        }
        return title;
    }

    @Override
    protected boolean refreshView(boolean force) {
        if (thread != null && thread.isLive()) {
            final Sequence<StackFrame> frames = thread.frames();
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
                final StackFrame newTopFrame = frames.first();
                if (selectedFrame != null && selectedFrame.stackFrame().isTopFrame() && selectedFrame.stackFrame().isSameFrame(newTopFrame)) {
                    selectedFrame.setStackFrame(newTopFrame);
                }
            }
            if (selectedFrame != null) {
                selectedFrame.refresh(force);
            }
        }
        super.refreshView(force);
        // The title displays thread state, so must be updated.
        updateFrameTitle();
        return true;
    }

    public void viewConfigurationChanged() {
        reconstructView();
    }


    @Override
    public void threadFocusSet(MaxThread oldThread, MaxThread thread) {
        reconstructView();
    }

    private int maxFramesDisplay = defaultMaxFramesDisplay;

    /**
     * Add frames to the stack model until {@link #maxFramesDisplay} reached.
     */
    private void addToModel(final Sequence<StackFrame> frames) {
        StackFrame parentStackFrame = null;
        for (StackFrame stackFrame : frames) {
            if (stackFrameListModel.size()  >= maxFramesDisplay) {
                stackFrameListModel.addElement(new TruncatedStackFrame(parentStackFrame, stackFrame));
                inspection().gui().informationMessage("stack depth of " + stackFrameListModel.size() + " exceeds " + maxFramesDisplay + ": truncated", "Stack Inspector");
                break;
            }
            stackFrameListModel.addElement(stackFrame);
            parentStackFrame = stackFrame;
        }
    }

    abstract static class StackFramePanel<StackFrame_Type extends StackFrame> extends InspectorPanel {

        protected StackFrame_Type stackFrame;

        public StackFramePanel(Inspection inspection, StackFrame_Type stackFrame) {
            super(inspection, new BorderLayout());
            this.stackFrame = stackFrame;
        }

        public final StackFrame_Type stackFrame() {
            return stackFrame;
        }

        public final void setStackFrame(StackFrame stackFrame) {
            final Class<StackFrame_Type> type = null;
            this.stackFrame = StaticLoophole.cast(type, stackFrame);
            refresh(true);
        }
    }

    private final class AdapterStackFramePanel extends StackFramePanel<AdapterStackFrame> {

        private final AppendableSequence<InspectorLabel> labels = new ArrayListSequence<InspectorLabel>(10);

        public AdapterStackFramePanel(Inspection inspection, AdapterStackFrame adapterStackFrame) {
            super(inspection, adapterStackFrame);
            final String frameClassName = adapterStackFrame.getClass().getSimpleName();
            final JPanel header = new InspectorPanel(inspection(), new SpringLayout());
            addLabel(header, new TextLabel(inspection(), "Frame size:", frameClassName));
            addLabel(header, new DataLabel.IntAsDecimal(inspection(), adapterStackFrame.layout.frameSize()));
            addLabel(header, new TextLabel(inspection(), "Frame pointer:", frameClassName));
            addLabel(header, new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD, adapterStackFrame.framePointer, this));
            addLabel(header, new TextLabel(inspection(), "Stack pointer:", frameClassName));
            addLabel(header, new DataLabel.AddressAsHex(inspection(), adapterStackFrame.stackPointer));
            addLabel(header, new TextLabel(inspection(), "Instruction pointer:", frameClassName));
            addLabel(header, new WordValueLabel(inspection(), ValueMode.INTEGER_REGISTER, adapterStackFrame.instructionPointer, this));
            SpringUtilities.makeCompactGrid(header, 2);

            add(header, BorderLayout.NORTH);
            add(new InspectorPanel(inspection()), BorderLayout.CENTER);
        }

        private void addLabel(JPanel panel, InspectorLabel label) {
            panel.add(label);
            labels.append(label);
        }

        @Override
        public void refresh(boolean force) {
            for (InspectorLabel label : labels) {
                label.refresh(force);
            }
        }

        @Override
        public void redisplay() {
            for (InspectorLabel label : labels) {
                label.redisplay();
            }
        }
    }

    private final class JavaStackFramePanel extends StackFramePanel<JavaStackFrame> {

        final WordValueLabel instructionPointerLabel;
        final Slots slots;
        final TextLabel[] slotLabels;
        final WordValueLabel[] slotValues;
        final TargetMethod targetMethod;
        final CodeAttribute codeAttribute;
        final JCheckBox showSlotAddresses;

        JavaStackFramePanel(Inspection inspection, JavaStackFrame javaStackFrame) {
            super(inspection, javaStackFrame);
            final String frameClassName = javaStackFrame.getClass().getSimpleName();
            final Address slotBase = javaStackFrame.slotBase();
            targetMethod = javaStackFrame.targetMethod();
            codeAttribute = (targetMethod.classMethodActor() == null) ? null : targetMethod.classMethodActor().codeAttribute();
            final int frameSize = javaStackFrame.layout.frameSize();

            final JPanel header = new InspectorPanel(inspection(), new SpringLayout());
            instructionPointerLabel = new WordValueLabel(inspection(), ValueMode.INTEGER_REGISTER, this) {
                @Override
                public Value fetchValue() {
                    return WordValue.from(stackFrame().instructionPointer);
                }
            };

            header.add(new TextLabel(inspection(), "Frame size:", frameClassName));
            header.add(new DataLabel.IntAsDecimal(inspection(), frameSize));

            final TextLabel framePointerLabel = new TextLabel(inspection(), "Frame pointer:", frameClassName);
            final TextLabel stackPointerLabel = new TextLabel(inspection(), "Stack pointer:", frameClassName);
            final Pointer framePointer = javaStackFrame.framePointer;
            final Pointer stackPointer = javaStackFrame.stackPointer;
            final StackBias bias = javaStackFrame.bias();

            header.add(framePointerLabel);
            header.add(new DataLabel.BiasedStackAddressAsHex(inspection(), framePointer, bias));
            header.add(stackPointerLabel);
            header.add(new DataLabel.BiasedStackAddressAsHex(inspection(), stackPointer, bias));
            header.add(new TextLabel(inspection(), "Instruction pointer:", frameClassName));
            header.add(instructionPointerLabel);


            SpringUtilities.makeCompactGrid(header, 2);

            final JPanel slotsPanel = new InspectorPanel(inspection(), new SpringLayout());
            slotsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            slots = javaStackFrame.layout.slots();
            slotLabels = new TextLabel[slots.length()];
            slotValues = new WordValueLabel[slots.length()];
            int slotIndex = 0;
            for (Slot slot : slots) {
                final int offset = slot.offset;
                final TextLabel slotLabel = new TextLabel(inspection(), slot.name + ":");
                slotsPanel.add(slotLabel);
                final WordValueLabel slotValue = new WordValueLabel(inspection(), WordValueLabel.ValueMode.INTEGER_REGISTER, this) {
                    @Override
                    public Value fetchValue() {
                        // TODO (mlvdv)  generalize this, and catch at {@link WordValueLabel}
                        try {
                            return new WordValue(maxVM().readWord(slotBase, offset));
                        } catch (DataIOError e) {
                            return VoidValue.VOID;
                        }
                    }
                };
                slotsPanel.add(slotValue);

                slotLabels[slotIndex] = slotLabel;
                slotValues[slotIndex] = slotValue;
                ++slotIndex;
            }

            SpringUtilities.makeCompactGrid(slotsPanel, 2);

            showSlotAddresses = new InspectorCheckBox(inspection(), "Slot addresses", null, false);
            final JPanel slotNameFormatPanel = new InspectorPanel(inspection(), new FlowLayout(FlowLayout.LEFT));
            slotNameFormatPanel.add(showSlotAddresses);
            showSlotAddresses.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    refresh(true);
                }
            });

            refresh(true);

            add(header, BorderLayout.NORTH);
            final JScrollPane slotsScrollPane = new InspectorScrollPane(inspection(), slotsPanel);
            add(slotsScrollPane, BorderLayout.CENTER);
            add(slotNameFormatPanel, BorderLayout.SOUTH);
        }

        @Override
        public void refresh(boolean force) {
            instructionPointerLabel.refresh(force);
            final boolean isTopFrame = stackFrame.isTopFrame();
            final Pointer instructionPointer = stackFrame.instructionPointer;
            final Pointer instructionPointerForStopPosition = isTopFrame ?  instructionPointer.plus(1) :  instructionPointer;
            int stopIndex = targetMethod.findClosestStopIndex(instructionPointerForStopPosition);
            if (stopIndex != -1 && isTopFrame) {
                final int stopPosition = targetMethod.stopPosition(stopIndex);
                final int targetCodePosition = targetMethod.targetCodePositionFor(instructionPointer);
                if (targetCodePosition != stopPosition) {
                    stopIndex = -1;
                }
            }

            // Update the color of the slot labels to denote if a reference map indicates they are holding object references:
            final ByteArrayBitMap referenceMap = stopIndex == -1 ? null : targetMethod.frameReferenceMapFor(stopIndex);
            for (int slotIndex = 0; slotIndex < slots.length(); ++slotIndex) {
                final Slot slot = slots.slot(slotIndex);
                final TextLabel slotLabel = slotLabels[slotIndex];
                updateSlotLabel(slot, slotLabel);
                slotValues[slotIndex].refresh(force);
                if (slot.referenceMapIndex != -1) {
                    if (referenceMap != null && referenceMap.isSet(slot.referenceMapIndex)) {
                        slotLabel.setForeground(style().wordValidObjectReferenceDataColor());
                    } else {
                        slotLabel.setForeground(style().textLabelColor());
                    }
                }
            }
        }

        /**
         * Updates the text of a given slot's label based on the check box for specifying use of slot addresses. Also,
         * the tool tip is updated to show the slot's Java source variable name if such a variable name exists.
         *
         * @param slot the slot to update
         * @param slotLabel the label for {@code slot}
         */
        private void updateSlotLabel(Slot slot, TextLabel slotLabel) {
            final String sourceVariableName = sourceVariableName(slot);
            final int offset = slot.offset;
            final String name = showSlotAddresses.isSelected() ? stackFrame.slotBase().plus(offset).toHexString() : slot.name;
            slotLabel.setText(name + ":");
            String otherInfo = "";
            final StackBias bias = stackFrame.bias();
            if (bias.isFramePointerBiased()) {
                final int biasedOffset = stackFrame.biasedOffset(offset);
                otherInfo = String.format("(%%fp %+d)", biasedOffset);
            }
            slotLabel.setToolTipText(String.format("%+d%s%s", offset, otherInfo, sourceVariableName == null ? "" : " [" + sourceVariableName + "]"));
        }

        /**
         * Gets the Java source variable name (if any) for a given slot.
         *
         * @param slot the slot for which the Java source variable name is being requested
         * @return the Java source name for {@code slot} or null if a name is not available
         */
        private String sourceVariableName(Slot slot) {
            if (targetMethod instanceof JitTargetMethod) {
                final JitTargetMethod jitTargetMethod = (JitTargetMethod) targetMethod;
                final JitStackFrameLayout jitLayout = (JitStackFrameLayout) stackFrame.layout;
                final int bytecodePosition = jitTargetMethod.bytecodePositionFor(stackFrame.instructionPointer);
                if (bytecodePosition != -1 && codeAttribute != null) {
                    for (int localVariableIndex = 0; localVariableIndex < codeAttribute.maxLocals(); ++localVariableIndex) {
                        final int localVariableOffset = jitLayout.localVariableOffset(localVariableIndex);
                        if (slot.offset == localVariableOffset) {
                            final Entry entry = codeAttribute.localVariableTable().findLocalVariable(localVariableIndex, bytecodePosition);
                            if (entry != null) {
                                return entry.name(codeAttribute.constantPool()).string;
                            }
                        }
                    }
                }
            }
            return null;
        }

    }

    private class StackFrameMouseClickAdapter extends InspectorMouseClickAdapter {

        StackFrameMouseClickAdapter(Inspection inspection) {
            super(inspection);
        }

        @Override
        public void procedure(final MouseEvent mouseEvent) {
            switch(MaxineInspector.mouseButtonWithModifiers(mouseEvent)) {
                case MouseEvent.BUTTON1: {
                    final int index = stackFrameList.getSelectedIndex();
                    final StackFrame stackFrame = (StackFrame) stackFrameListModel.get(index);
                    inspection().focus().setStackFrame(thread, stackFrame, true);
                }
            }
        }
    }

    // With each call, you get a bunch of events for which the selection index is -1, followed
    // by one more such event for which the selection index is 0.
    private class FrameSelectionListener implements ListSelectionListener {

        public void valueChanged(ListSelectionEvent listSelectionEvent) {
            if (listSelectionEvent.getValueIsAdjusting()) {
                return;
            }
            final int index = stackFrameList.getSelectedIndex();
            selectedFrame = null;
            final int dividerLocation = splitPane.getDividerLocation();

            final Component oldRightComponent = splitPane.getRightComponent();
            Component newRightComponent = oldRightComponent;

            if (index >= 0 && index < stackFrameListModel.getSize()) {
                final StackFrame stackFrame = (StackFrame) stackFrameListModel.get(index);
                if (stackFrame instanceof JavaStackFrame) {
                    if (stackFrame instanceof AdapterStackFrame) {
                        final AdapterStackFrame adapterStackFrame = (AdapterStackFrame) stackFrame;
                        selectedFrame = new AdapterStackFramePanel(inspection(), adapterStackFrame);
                    } else {
                        final JavaStackFrame javaStackFrame = (JavaStackFrame) stackFrame;
                        selectedFrame = new JavaStackFramePanel(inspection(), javaStackFrame);
                    }
                    newRightComponent = selectedFrame;
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
                    final StackFrame stackFrame = (StackFrame) stackFrameListModel.get(index);
                    if (stackFrame instanceof JitStackFrame) {
                        LocalsInspector.make(inspection(), thread, (JitStackFrame) stackFrame).highlight();
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
