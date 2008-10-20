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
package com.sun.max.ins.method;

import java.io.*;

import javax.swing.*;

import com.sun.max.collect.*;
import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.io.*;
import com.sun.max.lang.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.debug.TeleBytecodeBreakpoint.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.MethodKey.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.type.*;

/**
 * Base class for Bytecode viewers.
 *
 * @author Michael Van De Vanter
 *
 */
public abstract class BytecodeViewer extends CodeViewer {

    @Override
    public MethodInspector.CodeKind codeKind() {
        return MethodInspector.CodeKind.BYTECODES;
    }

    @Override
    public String codeViewerKindName() {
        return "Bytecodes";
    }

    private final TeleClassMethodActor _teleClassMethodActor;

    /**
     * @return Local {@link TeleClassMethodActor} corresponding to the {@link TeleVM} method being viewed.
     */
    public TeleClassMethodActor teleClassMethodActor() {
        return _teleClassMethodActor;
    }

    private final MethodActorKey _methodActorKey;

    private final TeleTargetMethod _teleTargetMethod;

    /**
     * The compilation associated with this view, if exists.
     */
    protected TeleTargetMethod teleTargetMethod() {
        return _teleTargetMethod;
    }

    private final byte[] _methodBytes;

    private final TeleConstantPool _teleConstantPool;

    protected TeleConstantPool teleConstantPool() {
        return _teleConstantPool;
    }

    private final ConstantPool _constantPool;

    /**
     * Disassembled target code instructions from the associated compilation of the method, null if none associated.
     */
    private IndexedSequence<TargetCodeInstruction> _targetCodeInstructions = null;

    private boolean _haveTargetCodeAddresses = false;

    /**
     * True if a compiled version of the method is available and if we have a map between bytecode and target locations.
     */
    protected boolean haveTargetCodeAddresses() {
        return _haveTargetCodeAddresses;
    }

    private AppendableIndexedSequence<BytecodeInstruction> _bytecodeInstructions = null;

    protected AppendableIndexedSequence<BytecodeInstruction> bytecodeInstructions() {
        return _bytecodeInstructions;
    }


    /**
     * Base class for bytecode viewers. TargetCode is optional, since a method may not yet be compiled, but may appear
     * and change as method is compiled and recompiled.
     */
    protected BytecodeViewer(Inspection inspection, MethodInspector parent, TeleClassMethodActor teleClassMethodActor, TeleTargetMethod teleTargetMethod) {
        super(inspection, parent);
        _teleClassMethodActor = teleClassMethodActor;
        _teleTargetMethod = teleTargetMethod;
        _methodActorKey = new MethodActorKey(teleClassMethodActor.classMethodActor());
        final TeleCodeAttribute teleCodeAttribute = teleClassMethodActor.getTeleCodeAttribute();
        // In a substituted method, the constant pool for the bytecodes is the one from the origin of the substitution,
        // not the current holder of the method.
        _teleConstantPool = teleCodeAttribute.getTeleConstantPool();
        _constantPool = _teleConstantPool.getTeleHolder().classActor().constantPool();
        _methodBytes = teleCodeAttribute.readBytecodes();
        // ProgramWarning.check(Bytes.equals(_methodBytes, teleMethod.classMethodActor().codeAttribute().code()),
        // "inconsistent bytecode");
        final byte[] localMethodBytes = teleClassMethodActor.classMethodActor().codeAttribute().code();
        if (!Bytes.equals(_methodBytes, localMethodBytes)) {
            // We're occasionally seeing a violation of the invariant that the bytecodes in the {@link TeleVM} for a particular
            // method aren't the same as those associated with the same method in the Inspector.  So far, the
            // difference shows up as some extra bytecodes at the end.
            final ConstantPool constantPool = teleClassMethodActor.getTeleHolder().classActor().constantPool();
            System.out.println("BytecodeViewer bytecode comparison failure for: " + teleClassMethodActor.classMethodActor().toString());
            System.out.println("  Bytecodes from VM(" + _methodBytes.length + " bytes) =");
            System.out.println(BytecodePrinter.toString(constantPool, new BytecodeBlock(_methodBytes)));
            System.out.println("  Bytecodes loaded locally: " + localMethodBytes.length + " bytes) =");
            System.out.println(BytecodePrinter.toString(constantPool, new BytecodeBlock(localMethodBytes)));
            System.out.println();
        }
        buildView();
        _rowToStackFrameInfo = new StackFrameInfo[_bytecodeInstructions.length()];
    }

    private void buildView() {
        int[] bytecodeToTargetCodePositionMap = null;
        if (_teleTargetMethod != null) {
            _targetCodeInstructions = _teleTargetMethod.getInstructions();
            bytecodeToTargetCodePositionMap = _teleTargetMethod.bytecodeToTargetCodePositionMap();
            // TODO (mlvdv) can only map bytecodes to JIT target code so far
            if (bytecodeToTargetCodePositionMap != null) {
                _haveTargetCodeAddresses = true;
            }
        }
        _bytecodeInstructions = new ArrayListSequence<BytecodeInstruction>(10);
        int currentBytecodeOffset = 0;
        int bytecodeRow = 0;
        int targetCodeRow = 0;
        Address targetCodeFirstAddress = Address.zero();
        while (currentBytecodeOffset < _methodBytes.length) {
            final OutputStream stream = new NullOutputStream();
            try {
                final InspectorBytecodePrinter bytecodePrinter = new InspectorBytecodePrinter(new PrintStream(stream), _constantPool);
                final BytecodeScanner bytecodeScanner = new BytecodeScanner(bytecodePrinter);
                final int nextBytecodeOffset = bytecodeScanner.scanInstruction(_methodBytes, currentBytecodeOffset);
                final byte[] instructionBytes = Bytes.getSection(_methodBytes, currentBytecodeOffset, nextBytecodeOffset);
                if (bytecodeToTargetCodePositionMap != null) {
                    while (_targetCodeInstructions.get(targetCodeRow).position() < bytecodeToTargetCodePositionMap[currentBytecodeOffset]) {
                        targetCodeRow++;
                    }
                    targetCodeFirstAddress = _targetCodeInstructions.get(targetCodeRow).address();
                }
                final BytecodeInstruction instruction = new BytecodeInstruction(bytecodeRow, currentBytecodeOffset, instructionBytes, bytecodePrinter.opcode(), bytecodePrinter.operand1(),
                                bytecodePrinter.operand2(), targetCodeRow, targetCodeFirstAddress);
                _bytecodeInstructions.append(instruction);
                bytecodeRow++;
                currentBytecodeOffset = nextBytecodeOffset;
            } catch (Throwable throwable) {
                throw new InspectorError("could not disassemble byte code", throwable);
            }
        }

    }

    protected BytecodeInstruction bytecodeInstructionAt(int row) {
        return bytecodeInstructions().get(row);
    }

    /**
     * Return the row containing the instruction starting at a bytecode position.
     */
    protected int positionToRow(int position) {
        for (BytecodeInstruction instruction : bytecodeInstructions()) {
            if (instruction.position() == position) {
                return instruction.row();
            }
        }
        return -1;
    }

    /**
     * Return the row containing the instruction at an address.
     */
    protected int addressToRow(Address instructionPointer) {
        if (_haveTargetCodeAddresses) {
            for (BytecodeInstruction instruction : bytecodeInstructions()) {
                int row = instruction.row();
                if (rowContainsAddress(row, instructionPointer)) {
                    return row;
                }
                row++;
            }
        }
        return -1;
    }

    /**
     * @return Whether the compiled code in the {@link TeleVM} for the bytecode at specified row contains the specified address.
     */
    protected boolean rowContainsAddress(int row, Address address) {
        if (_haveTargetCodeAddresses) {
            final BytecodeInstruction bytecodeInstruction = bytecodeInstructionAt(row);
            if (address.lessThan(bytecodeInstruction._targetCodeFirstAddress)) {
                // before the first byte location of the first target instruction for this bytecode
                return false;
            }
            if (row < (_bytecodeInstructions.length() - 1)) {
                // All but last bytecode instruction: see if before the first byte location of the first target instruction for the next bytecode
                return address.lessThan(bytecodeInstructionAt(row + 1)._targetCodeFirstAddress);
            }
            // Last bytecode instruction:  see if before the end of the target code
            final TargetCodeInstruction lastTargetCodeInstruction = _targetCodeInstructions.last();
            return address.lessThan(lastTargetCodeInstruction.address().plus(lastTargetCodeInstruction.length()));
        }
        return false;
    }

   /**
     * Rebuilds the cache of stack information if needed, based on the thread that is the current focus.
     * Identifies for each instruction in the method a stack frame (if any) whose instruction pointer is at the address of the instruction.
     */
    @Override
    protected void updateStackCache() {
        if (haveTargetCodeAddresses()) {
            final TeleNativeThread teleNativeThread = inspection().focus().thread();
            final Sequence<StackFrame> frames = teleNativeThread.frames();
            for (int row = 0; row < _bytecodeInstructions.length(); row++) {
                int stackPosition = 0;
                StackFrameInfo stackFrameInfo = null;
                for (StackFrame frame : frames) {
                    if (rowContainsAddress(row, frame.instructionPointer())) {
                        stackFrameInfo = new StackFrameInfo(frame, teleNativeThread, stackPosition);
                        break;
                    }
                    stackPosition++;
                }
                _rowToStackFrameInfo[row] = stackFrameInfo;
            }
        }
    }

    /**
     * Determines if the compiled code for the bytecode has a target breakpoint set at this location in the {@link TeleVM}, in
     * situations where we can map between locations.
     */
    protected Sequence<TeleTargetBreakpoint> getTargetBreakpointsAtRow(int row) {
        final AppendableSequence<TeleTargetBreakpoint> teleTargetBreakpoints = new LinkSequence<TeleTargetBreakpoint>();
        if (_haveTargetCodeAddresses) {
            for (TeleTargetBreakpoint teleTargetBreakpoint : teleVM().teleProcess().targetBreakpointFactory().breakpoints(true)) {
                if (rowContainsAddress(row, teleTargetBreakpoint.address())) {
                    teleTargetBreakpoints.append(teleTargetBreakpoint);
                }
            }
        }
        return teleTargetBreakpoints;
    }

    /**
     * @return the bytecode breakpoint, if any, set at the bytecode being displayed in the row.
     */
    protected TeleBytecodeBreakpoint getBytecodeBreakpointAtRow(int row) {
        for (TeleBytecodeBreakpoint teleBytecodeBreakpoint : teleVM().bytecodeBreakpointFactory().breakpoints()) {
            final Key key = teleBytecodeBreakpoint.key();
            // the direction of key comparison is significant
            if (_methodActorKey.equals(key) &&  bytecodeInstructions().get(row).position() == key.position()) {
                return teleBytecodeBreakpoint;
            }
        }
        return null;
    }

    protected final String rowToTagText(int row) {
        // Unimplemented
        return "";
    }

    protected class BytecodeInstruction {

        int _position;

        /** AsPosition of first byte of bytecode instruction. */
        int position() {
            return _position;
        }

        /** bytes constituting this bytecode instruction. */
        byte[] _instructionBytes;

        private int _row;

        /** index of this bytecode instruction in the method. */
        int row() {
            return _row;
        }

        /** index of the first target code instruction implementing this bytecode (Jit only for now). */
        int _targetCodeRow;

        private Address _targetCodeFirstAddress;

        /** address of the first byte in the target code instructions implementing this bytecode (Jit only for now. */
        Address targetCodeFirstAddress() {
            return _targetCodeFirstAddress;
        }

        Bytecode _opcode;

        // * Either a rendering component or an index into the constant pool if a reference kind. */
        Object _operand1;
        Object _operand2;

        BytecodeInstruction(int bytecodeRow, int position, byte[] bytes, Bytecode opcode, Object operand1, Object operand2, int targetCodeRow, Address targetCodeFirstAddress) {
            _row = bytecodeRow;
            _position = position;
            _instructionBytes = bytes;
            _opcode = opcode;
            _operand1 = operand1;
            _operand2 = operand2;
            _targetCodeRow = targetCodeRow;
            _targetCodeFirstAddress = targetCodeFirstAddress;
        }
    }

    private final class InspectorBytecodePrinter extends BytecodePrinter {

        public InspectorBytecodePrinter(PrintStream stream, ConstantPool constantPool) {
            super(new PrintWriter(stream), constantPool, "", "", 0);
        }

        @Override
        protected void prolog() {
        }

        private Bytecode _opcode;

        @Override
        protected void printOpcode() {
            _opcode = currentOpcode();
        }

        public Bytecode opcode() {
            return _opcode;
        }

        private Object _operand1 = new BytecodeOperandLabel(inspection(), "");

        public Object operand1() {
            return _operand1;
        }

        private JComponent _operand2 = new BytecodeOperandLabel(inspection(), "");

        public JComponent operand2() {
            return _operand2;
        }

        @Override
        protected void printImmediate(int immediate) {
            _operand1 = new BytecodeOperandLabel(inspection(), immediate);
        }

        @Override
        protected void printKind(Kind kind) {
            _operand1 = new BytecodeOperandLabel(inspection(), kind.name().toString());
        }

        @Override
        protected void printConstant(final int index) {
            _operand1 = new Integer(index);
        }

        @Override
        public void iinc(int index, int addend) {
            printOpcode();
            _operand1 = new BytecodeOperandLabel(inspection(), index);
            _operand2 = new BytecodeOperandLabel(inspection(), addend);
        }

        @Override
        public void multianewarray(int index, int nDimensions) {
            printOpcode();
            printConstant(index);
            _operand2 = new BytecodeOperandLabel(inspection(), nDimensions);
        }

        @Override
        public void tableswitch(int defaultOffset, int lowMatch, int highMatch, int numberOfCases) {
            printOpcode();
            _operand1 = new BytecodeOperandLabel(inspection(), defaultOffset + ", [" + lowMatch + " - " + highMatch + "] -> ");
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i != numberOfCases; ++i) {
                sb.append(getBytecodeScanner().readSwitchOffset());
                if (i != numberOfCases - 1) {
                    sb.append(' ');
                }
            }
            _operand2 = new BytecodeOperandLabel(inspection(), sb.toString());
        }

        @Override
        public void lookupswitch(int defaultOffset, int numberOfCases) {
            printOpcode();
            printImmediate(defaultOffset);
            String s = "";
            String separator = ", ";
            for (int i = 0; i < numberOfCases; i++) {
                s += separator + getBytecodeScanner().readSwitchCase() + "->" + getBytecodeScanner().readSwitchOffset();
                separator = " ";
            }
            _operand2 = new BytecodeOperandLabel(inspection(), s);
        }
    }

}
