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

import com.sun.max.collect.*;
import com.sun.max.gui.*;
import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.value.*;
import com.sun.max.lang.*;
import com.sun.max.tele.debug.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.LocalVariableTable.*;
import com.sun.max.vm.jit.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.value.*;


public class LocalsInspector extends UniqueInspector<LocalsInspector> implements ItemListener {

    /**
     * Display and highlight an inspector for stack frame locals.
     */
    public static LocalsInspector make(Inspection inspection, TeleNativeThread teleNativeThread, JitStackFrame jitStackFrame) {
        // CLEANUP: probably want to hide the use of calleeFramePointer in framePointer() ?
        final Pointer localsBasePointer = jitStackFrame.localsPointer(0);
        final UniqueInspector.Key<LocalsInspector> key = UniqueInspector.Key.create(LocalsInspector.class, localsBasePointer.toLong());
        LocalsInspector localsInspector = UniqueInspector.find(inspection, key);
        if (localsInspector == null) {
            localsInspector = new LocalsInspector(inspection, teleNativeThread, jitStackFrame);
        }
        localsInspector.highlight();
        return localsInspector;
    }

    private final DataAccess _dataAccess;
    private final TeleNativeThread _teleNativeThread;
    private final JitStackFrame _jitStackFrame;


    private final JPanel _localsPanel;
    private final JPanel _stackPanel;
    private final WordValueLabel[] _locals;
    private final WordValueLabel[] _stack;
    private int _stackDepth;

    /**
     * Control whether the entire operand stack is displayed or just the effective stack (i.e., all the slot up to
     * the current top of stack).
     */
    private boolean _showAll;

    public LocalsInspector(Inspection inspection, TeleNativeThread teleNativeThread, JitStackFrame jitStackFrame) {
        super(inspection, LongValue.from(jitStackFrame.framePointer().toLong()));
        assert jitStackFrame.targetMethod().compilerScheme() == teleVM().vmConfiguration().jitScheme();
        _dataAccess = teleVM().dataAccess();
        _teleNativeThread = teleNativeThread;
        _jitStackFrame = jitStackFrame;
        final ClassMethodActor classMethodActor = jitStackFrame.targetMethod().classMethodActor();
        _locals = new WordValueLabel[classMethodActor.codeAttribute().maxLocals()];
        _stack =  new WordValueLabel[classMethodActor.codeAttribute().maxStack()];
        _localsPanel = new InspectorPanel(inspection, new SpringLayout());
        _stackPanel = new InspectorPanel(inspection, new SpringLayout());
        _stackPanel.setBackground(InspectorStyle.SunYellow2);
        _stackDepth = 0;
        _showAll = true;
        createFrame(null);
    }

    public void itemStateChanged(ItemEvent e) {
        _showAll = e.getStateChange() == ItemEvent.SELECTED;
        final int tos = topOfStack(_stackDepth);
        if (_showAll) {
            for (int stackSlotIndex = tos; stackSlotIndex  < _stack.length; stackSlotIndex++) {
                final Word stackItem = readStackSlot(stackSlotIndex);
                final WordValueLabel label = _stack[stackSlotIndex];
                label.setValue(new WordValue(stackItem));
            }
        } else {
            for (int stackSlotIndex = tos; stackSlotIndex  < _stack.length; stackSlotIndex++) {
                final WordValueLabel label = _stack[stackSlotIndex];
                label.setValue(WordValue.ZERO);
            }
        }
    }

    @Override
    public void refreshView(long epoch, boolean force) {
        if (isShowing() || force) {
            // First, refresh stack frame information.
            Pointer stackPointer = null;
            final Sequence<StackFrame> frames = _teleNativeThread.frames();
            for (StackFrame stackFrame : frames) {
                if (stackFrame instanceof JitStackFrame) {
                    final JitStackFrame jitStackFrame = (JitStackFrame) stackFrame;
                    if (_jitStackFrame.isSameFrame(jitStackFrame)) {
                        stackPointer = jitStackFrame.stackPointer();
                        break;
                    }
                }
            }
            if (stackPointer == null) {
                // stack frame is inactive, remove it.
                frame().dispose();
                return;
            }

            for (int  localVarIndex = 0; localVarIndex < _locals.length; localVarIndex++) {
                final Word localVar = readlocalVariable(localVarIndex);
                _locals[localVarIndex].setValue(new WordValue(localVar));
            }

            // Update top of stack now (do it once the tos label has been cleared)
            final int stackDepth = _jitStackFrame.operandStackDepth();
            final int tos = topOfStack(stackDepth);
            if (tos != topOfStack(_stackDepth)) {
                clearTosLabel();
                _stackDepth = stackDepth;
                setTosLabel();
                if (!_showAll) {
                    for (int stackSlotIndex = tos; stackSlotIndex  < _stack.length; stackSlotIndex++) {
                        final WordValueLabel label = _stack[stackSlotIndex];
                        label.setValue(WordValue.ZERO);
                    }
                }
            }
            final int end = _showAll ? _stack.length : tos;
            for (int stackSlotIndex = 0; stackSlotIndex <  end; stackSlotIndex++) {
                final Word stackItem = readStackSlot(stackSlotIndex);
                final WordValueLabel label = _stack[stackSlotIndex];
                label.setValue(new WordValue(stackItem));
            }
            super.refreshView(epoch, force);
        }
    }

    public void viewConfigurationChanged(long epoch) {
        for (InspectorLabel label : _locals) {
            label.redisplay();
        }
        for (InspectorLabel label : _stack) {
            label.redisplay();
        }
        // TODO (mlvdv) redisplay this
    }

    class LocalIndex extends JLabel {
        public LocalIndex(int index) {
            super(Strings.padLengthWithSpaces(4, Integer.toString(index)));
        }
    }

    private static final int LABEL_WIDTH = 4;
    private static final Dimension _iconSize = new Dimension(16, 12);
    private static final Icon _ARROW = IconFactory.createRightArrow(_iconSize);

    private Word readStackSlot(int stackSlotIndex) {
        return _dataAccess.readWord(_jitStackFrame.operandStackPointer(stackSlotIndex));
    }

    private Word readlocalVariable(int localVariableIndex) {
        return _dataAccess.readWord(_jitStackFrame.localsPointer(localVariableIndex));
    }

    private JLabel getTosLabel(int stackSize) {
        return  (JLabel) _stackPanel.getComponent(stackSize * 2);
    }

    private void setTosLabel() {
        final JLabel tosLabel = getTosLabel(_stackDepth);
        tosLabel.setForeground(InspectorStyle.SunOrange2);
    }

    private void clearTosLabel() {
        final JLabel tosLabel = getTosLabel(_stackDepth);
        tosLabel.setForeground(InspectorStyle.SunYellow2);
    }

    public int topOfStack(int stackDepth) {
        return stackDepth < _stack.length ? stackDepth : _stack.length;
    }

    private JLabel createStackPointerLabel(int stackSlotIndex) {
        final JLabel stackPointer = createStackPointerLabel();
        stackPointer.setToolTipText(readStackSlot(stackSlotIndex).toHexString());
        return stackPointer;
    }
    private JLabel createStackPointerLabel() {
        final JLabel stackPointer = new JLabel(_ARROW);
        stackPointer.setForeground(InspectorStyle.SunYellow2);
        return stackPointer;
    }

    private void initPanelView() {
        final JitTargetMethod targetMethod = _jitStackFrame.targetMethod();
        final Word callEntryPoint = targetMethod.codeStart();
        final WordValueLabel header = new WordValueLabel(inspection(), WordValueLabel.ValueMode.CALL_ENTRY_POINT, callEntryPoint);
        // header.setToolTipText(_jitStackFrame.targetMethod().name());
        _localsPanel.add(new Space(4));
        _localsPanel.add(header);
        final ClassMethodActor classMethodActor = targetMethod.classMethodActor();
        final CodeAttribute codeAttribute = classMethodActor.codeAttribute();
        final LocalVariableTable localVariableTable = codeAttribute.localVariableTable();
        boolean addReceiverToolTip = !classMethodActor.isStatic();

        for (int  localVarIndex =  0; localVarIndex < _locals.length; localVarIndex++) {
            final Word localVar = readlocalVariable(localVarIndex);
            final WordValueLabel label = new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD, localVar);
            JLabel indexLabel = new LocalIndex(localVarIndex);
            final int bytecodePosition = targetMethod.bytecodePositionFor(_jitStackFrame.instructionPointer());
            if (bytecodePosition != -1) {
                final Entry entry = localVariableTable.findLocalVariable(localVarIndex, bytecodePosition);
                if (entry != null) {
                    indexLabel = new JLabel(entry.name(codeAttribute.constantPool()).string());
                }
            }
            if (addReceiverToolTip) {
                indexLabel.setToolTipText("this");
                addReceiverToolTip = false;
            }
            _locals[localVarIndex] = label;
            _localsPanel.add(indexLabel);
            _localsPanel.add(label);
        }
        // Create operand stack part of the panel.
        _stackPanel.add(createStackPointerLabel());
        _stackPanel.add(new Space(LABEL_WIDTH));

        _stackDepth = _jitStackFrame.operandStackDepth();
        // In some rare case, the computed top of stack may be larger than the operand stack size.
        // This happens when the operand stack is full and is calling a method, in that case the stack pointer
        // of the current frame includes extra space for the RIP. The following adjust the top of stack.
        final int tos = _showAll ? _stack.length : topOfStack(_stackDepth);
        for (int stackSlotIndex = 0; stackSlotIndex <  tos; stackSlotIndex++) {
            final Word stackItem = readStackSlot(stackSlotIndex);
            final WordValueLabel label = new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD, stackItem);
            label.setBackground(InspectorStyle.SunYellow2);
            _stack[stackSlotIndex] = label;
            _stackPanel.add(createStackPointerLabel(stackSlotIndex));
            _stackPanel.add(label);
        }
        for (int stackSlotIndex = tos; stackSlotIndex  < _stack.length; stackSlotIndex++) {
            final WordValueLabel label = new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD, Word.zero());
            label.setBackground(InspectorStyle.SunYellow2);
            _stack[stackSlotIndex] = label;
            _stackPanel.add(createStackPointerLabel(stackSlotIndex));
            _stackPanel.add(label);
        }
        setTosLabel();

        SpringUtilities.makeCompactGrid(_localsPanel, _locals.length + 1, 2, 0, 0, 5, 1);
        SpringUtilities.makeCompactGrid(_stackPanel, _stack.length + 1, 2, 0, 0, 5, 1);
    }

    @Override
    public String getTextForTitle() {
        return "StackFrame @ " + _jitStackFrame.framePointer().toHexString();
    }

    @Override
    protected void createView(long epoch) {
        initPanelView();
        final JPanel mainPanel = new InspectorPanel(inspection());
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        final JCheckBox showAll = new InspectorCheckBox(inspection(), "show all", null, true);
        showAll.setMnemonic(KeyEvent.VK_C);
        showAll.addItemListener(this);
        showAll.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(showAll);
        mainPanel.add(_localsPanel);
        mainPanel.add(_stackPanel);
        final JScrollPane scrollPane = new InspectorScrollPane(inspection(), mainPanel);
        frame().setContentPane(scrollPane);
    }

}
