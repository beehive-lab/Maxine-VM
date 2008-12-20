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
import java.awt.datatransfer.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import com.sun.max.collect.*;
import com.sun.max.gui.*;
import com.sun.max.ins.*;
import com.sun.max.ins.debug.StackInspectorContainer.*;
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
 * An inspector for a specific thread's stack in the {@link TeleVM}.
 *
 * @author Doug Simon
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public class StackInspector extends UniqueInspector<StackInspector> {

    /**
     * Displays and highlights a StackInspector for a thread.
     * @return a possibly new inspector
     */
    public static StackInspector make(Inspection inspection, TeleNativeThread teleNativeThread) {
        final StackInspector stackInspector = StackInspectorContainer.makeInspector(inspection, teleNativeThread);
        stackInspector.highlight();
        return stackInspector;
    }

    private Inspection _inspection;
    private StackInspectorContainer _parent;
    private final TeleNativeThread _teleNativeThread;
    private final DefaultListModel _listModel = new DefaultListModel();
    private final JList _list = new JList(_listModel);
    private final FrameSelectionListener _frameSelectionListener = new FrameSelectionListener();
    private static final int DEFAULT_MAX_FRAMES_DISPLAY = 500;
    private static final String MAX_FRAMES_DISPLAY_PROPERTY = "inspector.max.stack.frames.display";
    private static int _defaultMaxFramesDisplay;
    private boolean _stateChanged = true;  // conservative assessment of possible stack change
    private SelectedFrame<? extends StackFrame> _selectedFrame;
    private JSplitPane _splitPane;
    private JPanel _nativeFrame;

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

    /**
     * @return the thread whose stack is being inspected.
     */
    TeleNativeThread teleNativeThread() {
        return _teleNativeThread;
    }

    @Override
    public String getTextForTitle() {
        return _inspection.nameDisplay().longName(_teleNativeThread);
    }

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
            moveToFront();
            final int oldIndex = _list.getSelectedIndex();
            for (int index = 0; index < _listModel.getSize(); index++) {
                final StackFrame stackFrame = (StackFrame) _listModel.get(index);
                if (stackFrame.isSameFrame(newStackFrame)) {
                    if (index != oldIndex) {
                        _list.setSelectedIndex(index);
                    }
                    break;
                }
            }
        }
    }

    public StackInspector(Inspection inspection, TeleNativeThread teleNativeThread, Residence residence, StackInspectorContainer parent) {
        super(inspection, residence, LongValue.from(teleNativeThread.id()));
        _inspection = inspection;
        _teleNativeThread = teleNativeThread;
        _parent = parent;

        _list.setCellRenderer(new DefaultListCellRenderer() {
            /**
             * @param list
             * @param value
             * @param modelIndex
             * @param isSelected
             * @param cellHasFocus
             * @return
             */
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int modelIndex, boolean isSelected, boolean cellHasFocus) {
                final StackFrame stackFrame = (StackFrame) value;
                String name;
                String toolTip = null;
                Component component;
                if (stackFrame instanceof JavaStackFrame) {
                    final JavaStackFrame javaStackFrame = (JavaStackFrame) stackFrame;
                    final Address address = javaStackFrame.instructionPointer();
                    final TeleTargetMethod teleTargetMethod = TeleTargetMethod.make(teleVM(), address);
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
                        if (javaStackFrame.targetMethod().compilerScheme().equals(StackInspector.this.inspection().teleVM().vmConfiguration().jitScheme())) {
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
                    final TeleNativeTargetRoutine teleNativeTargetRoutine = TeleNativeTargetRoutine.make(teleVM(), instructionPointer);
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
        });
        final InspectorMenu frameMenu = new InspectorMenu();
        frameMenu.add(_copyStackToClipboardAction);
        createFrame(frameMenu);
    }

    @Override
    public void createView(long epoch) {
        final JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        final JPanel header = new JPanel(new SpringLayout());
        header.setOpaque(true);
        header.setBackground(style().defaultBackgroundColor());
        header.add(new TextLabel(_inspection, "start: "));
        header.add(new DataLabel.AddressAsHex(_inspection, _teleNativeThread.stack().start()));
        header.add(new TextLabel(_inspection, "size: "));
        header.add(new DataLabel.IntAsDecimal(_inspection, _teleNativeThread.stack().size().toInt()));
        SpringUtilities.makeCompactGrid(header, 2);
        panel.add(header, BorderLayout.NORTH);

        _list.setSelectionInterval(1, 0);
        _list.setVisibleRowCount(10);
        _list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        _list.setLayoutOrientation(JList.VERTICAL);
        _list.addKeyListener(_stackFrameKeyTypedListener);
        _list.addMouseListener(new StackFrameMouseClickAdapter(inspection()));

        _list.addListSelectionListener(_frameSelectionListener);

        _nativeFrame = new JPanel();
        final JScrollPane listScrollPane = new JScrollPane(_list);
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

        panel.add(_splitPane, BorderLayout.CENTER);

        frame().setContentPane(panel);
        refreshView(epoch, true);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // System.err.println("setting divider location in stack inspector for " + inspection().inspectionThreadName(_teleNativeThread));
                // Try to place the split pane divider in the middle of the split pane's space initially
                _splitPane.setDividerLocation(0.5d);
            }
        });
    }

    @Override
    public void refreshView(long epoch, boolean force) {
        if ((isShowing() && _stateChanged) || force) {
            // It is worth checking if we are in the same method and the frame is the same, which case
            // the state has not really changed, which can be a significant optimization for deep stacks
            final Sequence<StackFrame> frames = _teleNativeThread.frames();
            assert !frames.isEmpty();
            final StackFrame topFrame = frames.first();
            if (_selectedFrame != null && _selectedFrame.stackFrame().isTopFrame() && _selectedFrame.stackFrame().isSameFrame(topFrame)) {
                _selectedFrame.setStackFrame(topFrame);
            }
            _listModel.clear();
            addToModel(frames);
            _stateChanged = false;
        }
    }

    public void viewConfigurationChanged(long epoch) {
        reconstructView();
    }

    private int _maxFramesDisplay = _defaultMaxFramesDisplay;

    /**
     * Add frames to the stack model until {@link #_maxFramesDisplay} reached.
     */
    private void addToModel(final Sequence<StackFrame> frames) {
        StackFrame parentStackFrame = null;
        for (StackFrame stackFrame : frames) {
            if (_listModel.size()  >= _maxFramesDisplay) {
                _listModel.addElement(new TruncatedStackFrame(parentStackFrame, stackFrame));
                inspection().informationMessage("stack depth exceeds " + _maxFramesDisplay + ": truncated", "Stack Inspector");
                break;
            }
            _listModel.addElement(stackFrame);
            parentStackFrame = stackFrame;
        }
    }

    abstract static class SelectedFrame<StackFrame_Type extends StackFrame> extends JPanel {

        protected StackFrame_Type _stackFrame;

        public SelectedFrame(StackFrame_Type stackFrame) {
            super(new BorderLayout());
            _stackFrame = stackFrame;
        }

        public StackFrame_Type stackFrame() {
            return _stackFrame;
        }

        public void setStackFrame(StackFrame stackFrame) {
            final Class<StackFrame_Type> type = null;
            _stackFrame = StaticLoophole.cast(type, stackFrame);
        }

        /**
         * Reads any state from the {@link TeleVM} that might change.
         */
        public void refresh(long epoch, boolean force) {
        }
    }

    class SelectedAdapterFrame extends SelectedFrame<AdapterStackFrame> {

        public SelectedAdapterFrame(AdapterStackFrame adapterStackFrame) {
            super(adapterStackFrame);
            final JPanel header = new JPanel(new SpringLayout());
            header.setOpaque(true);
            header.setBackground(style().defaultBackgroundColor());
            header.add(new TextLabel(_inspection, "Frame size:"));
            header.add(new DataLabel.IntAsDecimal(_inspection, adapterStackFrame.layout().frameSize()));
            header.add(new TextLabel(_inspection, "Frame pointer:"));
            header.add(new DataLabel.AddressAsHex(_inspection, adapterStackFrame.framePointer()));
            header.add(new TextLabel(_inspection, "Stack pointer:"));
            header.add(new DataLabel.AddressAsHex(_inspection, adapterStackFrame.stackPointer()));
            header.add(new TextLabel(_inspection, "Instruction pointer:"));
            header.add(new WordValueLabel(_inspection, ValueMode.INTEGER_REGISTER, adapterStackFrame.instructionPointer()));
            SpringUtilities.makeCompactGrid(header, 2);

            add(header, BorderLayout.NORTH);
            add(new JPanel(), BorderLayout.CENTER);
        }

        @Override
        public void refresh(long epoch, boolean force) {
        }
    }

    class SelectedJavaFrame extends SelectedFrame<JavaStackFrame> {

        final WordValueLabel _instructionPointerLabel;
        final Slots _slots;
        final TextLabel[] _slotLabels;
        final WordValueLabel[] _slotValues;
        final TargetMethod _targetMethod;
        final CodeAttribute _codeAttribute;
        final JCheckBox _showSlotAddresses;

        SelectedJavaFrame(JavaStackFrame javaStackFrame) {
            super(javaStackFrame);
            final Address slotBase = javaStackFrame.slotBase();
            _stackFrame = javaStackFrame;
            _targetMethod = javaStackFrame.targetMethod();
            _codeAttribute = _targetMethod.classMethodActor().codeAttribute();
            final int frameSize = javaStackFrame.layout().frameSize();

            final JPanel header = new JPanel(new SpringLayout());
            _instructionPointerLabel = new WordValueLabel(_inspection, ValueMode.INTEGER_REGISTER, javaStackFrame.instructionPointer());

            header.setOpaque(true);
            header.setBackground(style().defaultBackgroundColor());
            header.add(new TextLabel(_inspection, "Frame size:"));
            header.add(new DataLabel.IntAsDecimal(_inspection, frameSize));

            final TextLabel framePointerLabel = new TextLabel(_inspection, "Frame pointer:");
            final TextLabel stackPointerLabel = new TextLabel(_inspection, "Stack pointer:");
            final Pointer framePointer = javaStackFrame.framePointer();
            final Pointer stackPointer = javaStackFrame.stackPointer();
            final STACK_BIAS bias = javaStackFrame.bias();

            header.add(framePointerLabel);
            header.add(new DataLabel.BiasedStackAddressAsHex(_inspection, framePointer, bias));
            header.add(stackPointerLabel);
            header.add(new DataLabel.BiasedStackAddressAsHex(_inspection, stackPointer, bias));
            header.add(new TextLabel(_inspection, "Instruction pointer:"));
            header.add(_instructionPointerLabel);


            SpringUtilities.makeCompactGrid(header, 2);

            final JPanel slotsPanel = new JPanel(new SpringLayout());
            slotsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            slotsPanel.setOpaque(true);
            slotsPanel.setBackground(style().defaultBackgroundColor());

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
                        try {
                            return new WordValue(teleVM().teleProcess().dataAccess().readWord(slotBase, offset));
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

            _showSlotAddresses = new JCheckBox("Slot addresses");
            final JPanel slotNameFormatPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            slotNameFormatPanel.add(_showSlotAddresses);
            _showSlotAddresses.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    refresh(teleProcess().epoch(), true);
                }
            });

            refresh(teleProcess().epoch(), true);

            add(header, BorderLayout.NORTH);
            final JScrollPane slotsScrollPane = new JScrollPane(slotsPanel);
            add(slotsScrollPane, BorderLayout.CENTER);
            add(slotNameFormatPanel, BorderLayout.SOUTH);
        }

        @Override
        public void refresh(long epoch, boolean force) {
            final Pointer instructionPointer = _stackFrame.instructionPointer();
            _instructionPointerLabel.setValue(new WordValue(instructionPointer));
            Pointer instructionPointerForStopPosition = instructionPointer;
            final boolean isTopFrame = _stackFrame.isTopFrame();
            if (isTopFrame) {
                instructionPointerForStopPosition = instructionPointerForStopPosition.plus(1);
            }
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
                    final int index = _list.getSelectedIndex();
                    final StackFrame stackFrame = (StackFrame) _listModel.get(index);
                    inspection().focus().setStackFrame(teleNativeThread(), stackFrame, true);
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
            final int index = _list.getSelectedIndex();
            _selectedFrame = null;
            final int dividerLocation = _splitPane.getDividerLocation();

            final Component oldRightComponent = _splitPane.getRightComponent();
            Component newRightComponent = oldRightComponent;

            if (index >= 0 && index < _listModel.getSize()) {
                final StackFrame stackFrame = (StackFrame) _listModel.get(index);
                if (stackFrame instanceof JavaStackFrame) {
                    if (stackFrame.isAdapter()) {
                        final AdapterStackFrame adapterStackFrame = (AdapterStackFrame) stackFrame;
                        _selectedFrame = new SelectedAdapterFrame(adapterStackFrame);
                    } else {
                        final JavaStackFrame javaStackFrame = (JavaStackFrame) stackFrame;
                        _selectedFrame = new SelectedJavaFrame(javaStackFrame);
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
                final int index = _list.getSelectedIndex();
                if (index >= 0 && index < _listModel.getSize()) {
                    final StackFrame stackFrame = (StackFrame) _listModel.get(index);
                    if (stackFrame instanceof JitStackFrame) {
                        LocalsInspector.make(_inspection, _teleNativeThread, (JitStackFrame) stackFrame);
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
            final ListCellRenderer cellRenderer = _list.getCellRenderer();
            for (int index = 0; index < _listModel.getSize(); index++) {
                final Object elementAt = _listModel.getElementAt(index);
                final JLabel jLabel = (JLabel) cellRenderer.getListCellRendererComponent(_list, elementAt, index, false, false);
                result.append(jLabel.getText()).append("\n");
            }
            final Clipboard clipboard = inspection().getToolkit().getSystemClipboard();
            final StringSelection selection = new StringSelection(result.toString());
            clipboard.setContents(selection, selection);
        }
    }

    private final InspectorAction _copyStackToClipboardAction = new CopyStackToClipboardAction();

    @Override
    public void setSelected() {
        if (_parent != null) {
            _parent.setSelected(this);
        } else {
            super.moveToFront();
        }
    }

    @Override
    public boolean isSelected() {
        if (_parent != null) {
            return _parent.isSelected(this);
        }
        return false;
    }

    @Override
    public void moveToFront() {
        if (_parent != null) {
            _parent.setSelected(this);
        } else {
            super.moveToFront();
        }
    }

    @Override
    public synchronized void setResidence(Residence residence) {
        final Residence current = residence();
        super.setResidence(residence);
        if (current != residence) {
            if (residence == Residence.INTERNAL) {
                // coming back from EXTERNAL, need to redock
                if (_parent != null) {
                    _parent.add(this);
                }
                moveToFront();
            } else if (residence == Residence.EXTERNAL) {
                frame().setTitle("Stack " + getTextForTitle());
            }
        }
    }


}
