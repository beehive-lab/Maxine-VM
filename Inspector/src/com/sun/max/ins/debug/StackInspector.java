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
    private static StackInspector _stackInspector;

    /**
     * Displays the (singleton) inspector, creating it if needed.
     */
    public static StackInspector make(Inspection inspection) {
        if (_stackInspector == null) {
            _stackInspector = new StackInspector(inspection);
        }
        return _stackInspector;
    }

    private static final int DEFAULT_MAX_FRAMES_DISPLAY = 500;
    private static final String MAX_FRAMES_DISPLAY_PROPERTY = "inspector.max.stack.frames.display";
    private static int _defaultMaxFramesDisplay;
    static {
        final String value = System.getProperty(MAX_FRAMES_DISPLAY_PROPERTY);
        if (value != null) {
            try {
                _defaultMaxFramesDisplay = Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                ProgramError.unexpected(MAX_FRAMES_DISPLAY_PROPERTY + " value " +  value + " not an integer");
            }
        } else {
            _defaultMaxFramesDisplay = DEFAULT_MAX_FRAMES_DISPLAY;
        }
    }

    static class TruncatedStackFrame extends StackFrame {
        private StackFrame _truncatedStackFrame;

        TruncatedStackFrame(StackFrame callee, StackFrame truncatedStackFrame) {
            super(callee, truncatedStackFrame.instructionPointer(), truncatedStackFrame.framePointer(), truncatedStackFrame.stackPointer());
            _truncatedStackFrame = truncatedStackFrame;
        }

        StackFrame getTruncatedStackFrame() {
            return _truncatedStackFrame;
        }

        @Override
        public TargetMethod targetMethod() {
            return _truncatedStackFrame.targetMethod();
        }

        @Override
        public boolean isJavaStackFrame() {
            return false;
        }

        @Override
        public boolean isSameFrame(StackFrame stackFrame) {
            if (stackFrame instanceof TruncatedStackFrame) {
                final TruncatedStackFrame other = (TruncatedStackFrame) stackFrame;
                return _truncatedStackFrame.isSameFrame(other._truncatedStackFrame);
            }
            return false;
        }
    }

    private final SaveSettingsListener _saveSettingsListener = createGeometrySettingsClient(this, "stackInspector");

    private TeleNativeThread _teleNativeThread = null;
    private InspectorPanel _contentPane = null;
    private  DefaultListModel _stackFrameListModel = null;
    private JList _stackFrameList = null;
    private JSplitPane _splitPane = null;
    private JPanel _nativeFrame = null;

    private final FrameSelectionListener _frameSelectionListener = new FrameSelectionListener();
    private final StackFrameListCellRenderer _stackFrameListCellRenderer = new StackFrameListCellRenderer();

    private boolean _stateChanged = true;  // conservative assessment of possible stack change
    private StackFramePanel<? extends StackFrame> _selectedFrame;

    @Override
    public void threadStateChanged(TeleNativeThread teleNativeThread) {
        if (teleNativeThread.equals(_teleNativeThread)) {
            _stateChanged = teleNativeThread.framesChanged();
        }
        super.threadStateChanged(teleNativeThread);
    }

    @Override
    public void stackFrameFocusChanged(StackFrame oldStackFrame, TeleNativeThread threadForStackFrame, StackFrame newStackFrame) {
        if (threadForStackFrame == _teleNativeThread) {
            final int oldIndex = _stackFrameList.getSelectedIndex();
            for (int index = 0; index < _stackFrameListModel.getSize(); index++) {
                final StackFrame stackFrame = (StackFrame) _stackFrameListModel.get(index);
                if (stackFrame.isSameFrame(newStackFrame)) {
                    if (index != oldIndex) {
                        _stackFrameList.setSelectedIndex(index);
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
                final Address address = javaStackFrame.instructionPointer();
                final TeleTargetMethod teleTargetMethod = maxVM().makeTeleTargetMethod(address);
                if (teleTargetMethod != null) {
                    name = inspection().nameDisplay().veryShortName(teleTargetMethod);
                    toolTip = inspection().nameDisplay().longName(teleTargetMethod, address);
                    final TeleClassMethodActor teleClassMethodActor = teleTargetMethod.getTeleClassMethodActor();
                    if (teleClassMethodActor.isSubstituted()) {
                        name = name + inspection().nameDisplay().methodSubstitutionShortAnnotation(teleClassMethodActor);
                        toolTip = toolTip + inspection().nameDisplay().methodSubstitutionLongAnnotation(teleClassMethodActor);
                    }
                } else {
                    final MethodActor classMethodActor = javaStackFrame.targetMethod().classMethodActor();
                    name = classMethodActor.format("%h.%n");
                    toolTip = classMethodActor.format("%r %H.%n(%p)");
                }
                if (javaStackFrame.isAdapter()) {
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
                final Pointer instructionPointer = stackFrame.instructionPointer();
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
        frame().menu().add(_copyStackToClipboardAction);
        refreshView(inspection.maxVM().epoch(), true);
        Trace.end(1,  tracePrefix() + " initializing");
    }

    @Override
    protected Rectangle defaultFrameBounds() {
        return inspection().geometry().stackFrameDefaultBounds();
    }

    @Override
    protected SaveSettingsListener saveSettingsListener() {
        return _saveSettingsListener;
    }

    @Override
    public void createView(long epoch) {
        _teleNativeThread = inspection().focus().thread();

        if (_teleNativeThread == null) {
            _contentPane = null;
        } else {
            _contentPane = new InspectorPanel(inspection(), new BorderLayout());

            _stackFrameListModel = new DefaultListModel();
            _stackFrameList = new JList(_stackFrameListModel);
            _stackFrameList.setCellRenderer(_stackFrameListCellRenderer);

            final JPanel header = new InspectorPanel(inspection(), new SpringLayout());
            header.add(new TextLabel(inspection(), "start: "));
            header.add(new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD, _teleNativeThread.stack().start()));
            header.add(new TextLabel(inspection(), "size: "));
            header.add(new DataLabel.IntAsDecimal(inspection(), _teleNativeThread.stack().size().toInt()));
            SpringUtilities.makeCompactGrid(header, 2);
            _contentPane.add(header, BorderLayout.NORTH);

            _stackFrameList.setSelectionInterval(1, 0);
            _stackFrameList.setVisibleRowCount(10);
            _stackFrameList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            _stackFrameList.setLayoutOrientation(JList.VERTICAL);
            _stackFrameList.addKeyListener(_stackFrameKeyTypedListener);
            _stackFrameList.addMouseListener(new StackFrameMouseClickAdapter(inspection()));

            _stackFrameList.addListSelectionListener(_frameSelectionListener);

            _nativeFrame = new InspectorPanel(inspection());
            final JScrollPane listScrollPane = new InspectorScrollPane(inspection(), _stackFrameList);
            _splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT) {
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
            _splitPane.setOneTouchExpandable(true);
            _splitPane.setLeftComponent(listScrollPane);
            _splitPane.setRightComponent(_nativeFrame);

            _contentPane.add(_splitPane, BorderLayout.CENTER);
        }
        frame().setContentPane(_contentPane);
        refreshView(maxVM().epoch(), true);

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                // System.err.println("setting divider location in stack inspector for " + inspection().inspectionThreadName(_teleNativeThread));
                // Try to place the split pane divider in the middle of the split pane's space initially
                _splitPane.setDividerLocation(0.5d);
            }
        });
    }

    @Override
    public String getTextForTitle() {
        String title = "Stack: ";
        if (_teleNativeThread != null) {
            title += inspection().nameDisplay().longNameWithState(_teleNativeThread);
        }
        return title;
    }

    @Override
    protected void refreshView(long epoch, boolean force) {
        final Sequence<StackFrame> frames = _teleNativeThread.frames();
        assert !frames.isEmpty();
        if (_stateChanged || force) {
            _stackFrameListModel.clear();
            addToModel(frames);
            _stateChanged = false;
        } else {
            // The stack is structurally unchanged with respect to methods,
            // so avoid a complete redisplay for performance reasons.
            // However, the object representing the top frame may be different,
            // in which case the state of the old frame object is out of date.
            final StackFrame newTopFrame = frames.first();
            if (_selectedFrame != null && _selectedFrame.stackFrame().isTopFrame() && _selectedFrame.stackFrame().isSameFrame(newTopFrame)) {
                _selectedFrame.setStackFrame(newTopFrame);
            }
        }
        if (_selectedFrame != null) {
            _selectedFrame.refresh(epoch, force);
        }
        super.refreshView(epoch, force);
        // The title displays thread state, so must be updated.
        updateFrameTitle();
    }

    public void viewConfigurationChanged(long epoch) {
        reconstructView();
    }


    @Override
    public void threadFocusSet(TeleNativeThread oldTeleNativeThread, TeleNativeThread teleNativThread) {
        reconstructView();
    }

    private int _maxFramesDisplay = _defaultMaxFramesDisplay;

    /**
     * Add frames to the stack model until {@link #_maxFramesDisplay} reached.
     */
    private void addToModel(final Sequence<StackFrame> frames) {
        StackFrame parentStackFrame = null;
        for (StackFrame stackFrame : frames) {
            if (_stackFrameListModel.size()  >= _maxFramesDisplay) {
                _stackFrameListModel.addElement(new TruncatedStackFrame(parentStackFrame, stackFrame));
                inspection().gui().informationMessage("stack depth of " + _stackFrameListModel.size() + " exceeds " + _maxFramesDisplay + ": truncated", "Stack Inspector");
                break;
            }
            _stackFrameListModel.addElement(stackFrame);
            parentStackFrame = stackFrame;
        }
    }

    abstract static class StackFramePanel<StackFrame_Type extends StackFrame> extends InspectorPanel {

        protected StackFrame_Type _stackFrame;

        public StackFramePanel(Inspection inspection, StackFrame_Type stackFrame) {
            super(inspection, new BorderLayout());
            _stackFrame = stackFrame;
        }

        public final StackFrame_Type stackFrame() {
            return _stackFrame;
        }

        public final void setStackFrame(StackFrame stackFrame) {
            final Class<StackFrame_Type> type = null;
            _stackFrame = StaticLoophole.cast(type, stackFrame);
            refresh(maxVM().epoch(), true);
        }
    }

    private final class AdapterStackFramePanel extends StackFramePanel<AdapterStackFrame> {

        private final AppendableSequence<InspectorLabel> _labels = new ArrayListSequence<InspectorLabel>(10);

        public AdapterStackFramePanel(Inspection inspection, AdapterStackFrame adapterStackFrame) {
            super(inspection, adapterStackFrame);
            final String frameClassName = adapterStackFrame.getClass().getSimpleName();
            final JPanel header = new InspectorPanel(inspection(), new SpringLayout());
            addLabel(header, new TextLabel(inspection(), "Frame size:", frameClassName));
            addLabel(header, new DataLabel.IntAsDecimal(inspection(), adapterStackFrame.layout().frameSize()));
            addLabel(header, new TextLabel(inspection(), "Frame pointer:", frameClassName));
            addLabel(header, new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD, adapterStackFrame.framePointer()));
            addLabel(header, new TextLabel(inspection(), "Stack pointer:", frameClassName));
            addLabel(header, new DataLabel.AddressAsHex(inspection(), adapterStackFrame.stackPointer()));
            addLabel(header, new TextLabel(inspection(), "Instruction pointer:", frameClassName));
            addLabel(header, new WordValueLabel(inspection(), ValueMode.INTEGER_REGISTER, adapterStackFrame.instructionPointer()));
            SpringUtilities.makeCompactGrid(header, 2);

            add(header, BorderLayout.NORTH);
            add(new InspectorPanel(inspection()), BorderLayout.CENTER);
        }

        private void addLabel(JPanel panel, InspectorLabel label) {
            panel.add(label);
            _labels.append(label);
        }

        @Override
        public void refresh(long epoch, boolean force) {
            for (InspectorLabel label : _labels) {
                label.refresh(epoch, force);
            }
        }

        @Override
        public void redisplay() {
            for (InspectorLabel label : _labels) {
                label.redisplay();
            }
        }
    }

    private final class JavaStackFramePanel extends StackFramePanel<JavaStackFrame> {

        final WordValueLabel _instructionPointerLabel;
        final Slots _slots;
        final TextLabel[] _slotLabels;
        final WordValueLabel[] _slotValues;
        final TargetMethod _targetMethod;
        final CodeAttribute _codeAttribute;
        final JCheckBox _showSlotAddresses;

        JavaStackFramePanel(Inspection inspection, JavaStackFrame javaStackFrame) {
            super(inspection, javaStackFrame);
            final String frameClassName = javaStackFrame.getClass().getSimpleName();
            final Address slotBase = javaStackFrame.slotBase();
            _targetMethod = javaStackFrame.targetMethod();
            _codeAttribute = _targetMethod.classMethodActor().codeAttribute();
            final int frameSize = javaStackFrame.layout().frameSize();

            final JPanel header = new InspectorPanel(inspection(), new SpringLayout());
            _instructionPointerLabel = new WordValueLabel(inspection(), ValueMode.INTEGER_REGISTER) {
                @Override
                public Value fetchValue() {
                    return WordValue.from(stackFrame().instructionPointer());
                }
            };

            header.add(new TextLabel(inspection(), "Frame size:", frameClassName));
            header.add(new DataLabel.IntAsDecimal(inspection(), frameSize));

            final TextLabel framePointerLabel = new TextLabel(inspection(), "Frame pointer:", frameClassName);
            final TextLabel stackPointerLabel = new TextLabel(inspection(), "Stack pointer:", frameClassName);
            final Pointer framePointer = javaStackFrame.framePointer();
            final Pointer stackPointer = javaStackFrame.stackPointer();
            final STACK_BIAS bias = javaStackFrame.bias();

            header.add(framePointerLabel);
            header.add(new DataLabel.BiasedStackAddressAsHex(inspection(), framePointer, bias));
            header.add(stackPointerLabel);
            header.add(new DataLabel.BiasedStackAddressAsHex(inspection(), stackPointer, bias));
            header.add(new TextLabel(inspection(), "Instruction pointer:", frameClassName));
            header.add(_instructionPointerLabel);


            SpringUtilities.makeCompactGrid(header, 2);

            final JPanel slotsPanel = new InspectorPanel(inspection(), new SpringLayout());
            slotsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            _slots = javaStackFrame.layout().slots();
            _slotLabels = new TextLabel[_slots.length()];
            _slotValues = new WordValueLabel[_slots.length()];
            int slotIndex = 0;
            for (Slot slot : _slots) {
                final int offset = slot.offset();
                final TextLabel slotLabel = new TextLabel(inspection(), slot.name() + ":");
                slotsPanel.add(slotLabel);
                final WordValueLabel slotValue = new WordValueLabel(inspection(), WordValueLabel.ValueMode.INTEGER_REGISTER) {
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

                _slotLabels[slotIndex] = slotLabel;
                _slotValues[slotIndex] = slotValue;
                ++slotIndex;
            }

            SpringUtilities.makeCompactGrid(slotsPanel, 2);

            _showSlotAddresses = new InspectorCheckBox(inspection(), "Slot addresses", null, false);
            final JPanel slotNameFormatPanel = new InspectorPanel(inspection(), new FlowLayout(FlowLayout.LEFT));
            slotNameFormatPanel.add(_showSlotAddresses);
            _showSlotAddresses.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    refresh(maxVM().epoch(), true);
                }
            });

            refresh(maxVM().epoch(), true);

            add(header, BorderLayout.NORTH);
            final JScrollPane slotsScrollPane = new InspectorScrollPane(inspection(), slotsPanel);
            add(slotsScrollPane, BorderLayout.CENTER);
            add(slotNameFormatPanel, BorderLayout.SOUTH);
        }

        @Override
        public void refresh(long epoch, boolean force) {
            _instructionPointerLabel.refresh(epoch, force);
            final boolean isTopFrame = _stackFrame.isTopFrame();
            final Pointer instructionPointer = _stackFrame.instructionPointer();
            final Pointer instructionPointerForStopPosition = isTopFrame ?  instructionPointer.plus(1) :  instructionPointer;
            int stopIndex = _targetMethod.findClosestStopIndex(instructionPointerForStopPosition);
            if (stopIndex != -1 && isTopFrame) {
                final int stopPosition = _targetMethod.stopPosition(stopIndex);
                final int targetCodePosition = _targetMethod.targetCodePositionFor(instructionPointer);
                if (targetCodePosition != stopPosition) {
                    stopIndex = -1;
                }
            }

            // Update the color of the slot labels to denote if a reference map indicates they are holding object references:
            final ByteArrayBitMap referenceMap = stopIndex == -1 ? null : _targetMethod.frameReferenceMapFor(stopIndex);
            for (int slotIndex = 0; slotIndex < _slots.length(); ++slotIndex) {
                final Slot slot = _slots.slot(slotIndex);
                final TextLabel slotLabel = _slotLabels[slotIndex];
                updateSlotLabel(slot, slotLabel);
                _slotValues[slotIndex].refresh(epoch, force);
                if (slot.referenceMapIndex() != -1) {
                    if (referenceMap != null && referenceMap.isSet(slot.referenceMapIndex())) {
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
            final int offset = slot.offset();
            final String name = _showSlotAddresses.isSelected() ? _stackFrame.slotBase().plus(offset).toHexString() : slot.name();
            slotLabel.setText(name + ":");
            String otherInfo = "";
            final STACK_BIAS bias = _stackFrame.bias();
            if (bias.isFramePointerBiased()) {
                final int biasedOffset = _stackFrame.biasedOffset(offset);
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
            if (_targetMethod instanceof JitTargetMethod) {
                final JitTargetMethod jitTargetMethod = (JitTargetMethod) _targetMethod;
                final JitStackFrameLayout jitLayout = (JitStackFrameLayout) _stackFrame.layout();
                final int bytecodePosition = jitTargetMethod.bytecodePositionFor(_stackFrame.instructionPointer());
                if (bytecodePosition != -1) {
                    for (int localVariableIndex = 0; localVariableIndex < _codeAttribute.maxLocals(); ++localVariableIndex) {
                        final int localVariableOffset = jitLayout.localVariableOffset(localVariableIndex);
                        if (slot.offset() == localVariableOffset) {
                            final Entry entry = _codeAttribute.localVariableTable().findLocalVariable(localVariableIndex, bytecodePosition);
                            if (entry != null) {
                                return entry.name(_codeAttribute.constantPool()).string();
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
                    final int index = _stackFrameList.getSelectedIndex();
                    final StackFrame stackFrame = (StackFrame) _stackFrameListModel.get(index);
                    inspection().focus().setStackFrame(_teleNativeThread, stackFrame, true);
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
            final int index = _stackFrameList.getSelectedIndex();
            _selectedFrame = null;
            final int dividerLocation = _splitPane.getDividerLocation();

            final Component oldRightComponent = _splitPane.getRightComponent();
            Component newRightComponent = oldRightComponent;

            if (index >= 0 && index < _stackFrameListModel.getSize()) {
                final StackFrame stackFrame = (StackFrame) _stackFrameListModel.get(index);
                if (stackFrame instanceof JavaStackFrame) {
                    if (stackFrame.isAdapter()) {
                        final AdapterStackFrame adapterStackFrame = (AdapterStackFrame) stackFrame;
                        _selectedFrame = new AdapterStackFramePanel(inspection(), adapterStackFrame);
                    } else {
                        final JavaStackFrame javaStackFrame = (JavaStackFrame) stackFrame;
                        _selectedFrame = new JavaStackFramePanel(inspection(), javaStackFrame);
                    }
                    newRightComponent = _selectedFrame;
                } else if (stackFrame instanceof TruncatedStackFrame) {
                    _maxFramesDisplay *= 2;
                    _stateChanged = true;
                    refreshView(true);
                } else {
                    newRightComponent = _nativeFrame;
                }
            }
            if (oldRightComponent != newRightComponent) {
                _splitPane.setRightComponent(newRightComponent);
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        _splitPane.setDividerLocation(dividerLocation);
                    }
                });
            }
        }
    }

    /**
     * Watch for shift key being released to display the selected activation's stack frame.
     */
    private final KeyListener _stackFrameKeyTypedListener = new KeyListener() {
        public final void keyTyped(KeyEvent keyEvent) {
        }
        public final void keyPressed(KeyEvent keyEvent) {
        }

        public final void keyReleased(KeyEvent keyEvent) {
            if (keyEvent.getKeyCode() == KeyEvent.VK_SHIFT) {
                final int index = _stackFrameList.getSelectedIndex();
                if (index >= 0 && index < _stackFrameListModel.getSize()) {
                    final StackFrame stackFrame = (StackFrame) _stackFrameListModel.get(index);
                    if (stackFrame instanceof JitStackFrame) {
                        LocalsInspector.make(inspection(), _teleNativeThread, (JitStackFrame) stackFrame).highlight();
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
            final ListCellRenderer cellRenderer = _stackFrameList.getCellRenderer();
            for (int index = 0; index < _stackFrameListModel.getSize(); index++) {
                final Object elementAt = _stackFrameListModel.getElementAt(index);
                final JLabel jLabel = (JLabel) cellRenderer.getListCellRendererComponent(_stackFrameList, elementAt, index, false, false);
                result.append(jLabel.getText()).append("\n");
            }
            gui().postToClipboard(result.toString());
        }
    }

    private final InspectorAction _copyStackToClipboardAction = new CopyStackToClipboardAction();


    @Override
    public void inspectorClosing() {
        Trace.line(1, tracePrefix() + " closing");
        _stackInspector = null;
        super.inspectorClosing();
    }


}
