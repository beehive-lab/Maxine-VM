/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.method;

import java.io.*;
import java.util.*;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.util.*;
import com.sun.max.io.*;
import com.sun.max.lang.*;
import com.sun.max.tele.*;
import com.sun.max.tele.MaxMachineCode.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.actor.member.MethodKey.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.type.*;

/**
 * Base class for Bytecodes viewers.
 *
 * @author Michael Van De Vanter
 */
public abstract class BytecodeViewer extends CodeViewer {

    @Override
    public MethodCodeKind codeKind() {
        return MethodCodeKind.BYTECODES;
    }

    @Override
    public String codeViewerKindName() {
        return "Bytecodes";
    }

    private final TeleClassMethodActor teleClassMethodActor;

    /**
     * @return Local {@link TeleClassMethodActor} corresponding to the VM method being viewed.
     */
    public TeleClassMethodActor teleClassMethodActor() {
        return teleClassMethodActor;
    }

    /**
     * Abstract description of the method being viewed.
     */
    private final MethodKey methodKey;

    /**
     * @return abstract description of the method being viewed.
     */
    protected final MethodKey methodKey() {
        return methodKey;
    }

    private final MaxCompiledCode compiledCode;

    /**
     * The compilation associated with this view, if exists.
     */
    protected MaxCompiledCode compiledCode() {
        return compiledCode;
    }

    private final byte[] methodBytes;

    private final TeleConstantPool teleConstantPool;

    /**
     * @return local surrogate for the {@link ConstantPool} in the VM that is associated with this method.
     */
    protected TeleConstantPool teleConstantPool() {
        return teleConstantPool;
    }

    private final ConstantPool localConstantPool;

    /**
     * @return local {@link ConstantPool}, should be equivalent to the one in the VM that is associated with this method.
     */
    protected ConstantPool localConstantPool() {
        return localConstantPool;
    }

    /**
     * Disassembled target code instructions from the associated compilation of the method, null if none associated.
     */
    private List<TargetCodeInstruction> targetCodeInstructions = null;

    private boolean haveTargetCodeAddresses = false;

    /**
     * True if a compiled version of the method is available and if we have a map between bytecode and target locations.
     */
    protected boolean haveTargetCodeAddresses() {
        return haveTargetCodeAddresses;
    }

    private List<BytecodeInstruction> bytecodeInstructions = null;

    protected List<BytecodeInstruction> bytecodeInstructions() {
        return bytecodeInstructions;
    }

    /**
     * Base class for bytecode viewers. TargetCode is optional, since a method may not yet be compiled, but may appear
     * and change as method is compiled and recompiled.
     */
    protected BytecodeViewer(Inspection inspection, MethodInspector parent, TeleClassMethodActor teleClassMethodActor, MaxCompiledCode compiledCode) {
        super(inspection, parent);
        this.teleClassMethodActor = teleClassMethodActor;
        this.compiledCode = compiledCode;
        methodKey = new MethodActorKey(teleClassMethodActor.classMethodActor());
        final TeleCodeAttribute teleCodeAttribute = teleClassMethodActor.getTeleCodeAttribute();
        // Always use the {@link ConstantPool} taken from the {@link CodeAttribute}; in a substituted method, the
        // constant pool for the bytecodes is the one from the origin of the substitution, not the current holder of the method.
        teleConstantPool = teleCodeAttribute.getTeleConstantPool();
        localConstantPool = teleConstantPool.getTeleHolder().classActor().constantPool();
        methodBytes = teleCodeAttribute.readBytecodes();
        buildView();
        rowToStackFrame = new MaxStackFrame[bytecodeInstructions.size()];
    }

    private void buildView() {
        int[] bytecodeToTargetCodePositionMap = null;
        InstructionMap instructionMap = null;
        if (compiledCode != null) {
            instructionMap = compiledCode.instructionMap();
            bytecodeToTargetCodePositionMap = instructionMap.bytecodeToTargetCodePositionMap();
            // TODO (mlvdv) can only map bytecodes to JIT target code so far
            if (bytecodeToTargetCodePositionMap != null) {
                haveTargetCodeAddresses = true;
            }
        }
        bytecodeInstructions = new ArrayList<BytecodeInstruction>(10);
        int currentBytecodeOffset = 0;
        int bytecodeRow = 0;
        int targetCodeRow = 0;
        Address targetCodeFirstAddress = Address.zero();
        while (currentBytecodeOffset < methodBytes.length) {
            final OutputStream stream = new NullOutputStream();
            try {
                final InspectorBytecodePrinter bytecodePrinter = new InspectorBytecodePrinter(new PrintStream(stream), localConstantPool);
                final BytecodeScanner bytecodeScanner = new BytecodeScanner(bytecodePrinter);
                final int nextBytecodeOffset = bytecodeScanner.scanInstruction(methodBytes, currentBytecodeOffset);
                final byte[] instructionBytes = Bytes.getSection(methodBytes, currentBytecodeOffset, nextBytecodeOffset);
                if (haveTargetCodeAddresses) {
                    while (instructionMap.instruction(targetCodeRow).position < bytecodeToTargetCodePositionMap[currentBytecodeOffset]) {
                        targetCodeRow++;
                    }
                    targetCodeFirstAddress = instructionMap.instruction(targetCodeRow).address;
                }
                final BytecodeInstruction instruction = new BytecodeInstruction(bytecodeRow, currentBytecodeOffset, instructionBytes, bytecodePrinter.opcode(), bytecodePrinter.operand1(),
                                bytecodePrinter.operand2(), targetCodeRow, targetCodeFirstAddress);
                bytecodeInstructions.add(instruction);
                bytecodeRow++;
                currentBytecodeOffset = nextBytecodeOffset;
            } catch (Throwable throwable) {
                InspectorError.unexpected("could not disassemble bytecode", throwable);
            }
        }
    }

    /**
     * @return Whether the compiled code in the VM for the bytecode at specified row contains the specified address.
     */
    protected boolean rowContainsAddress(int row, Address address) {
        if (haveTargetCodeAddresses) {
            final BytecodeInstruction bytecodeInstruction = bytecodeInstructions.get(row);
            if (address.lessThan(bytecodeInstruction.targetCodeFirstAddress)) {
                // before the first byte location of the first target instruction for this bytecode
                return false;
            }
            if (row < (bytecodeInstructions.size() - 1)) {
                // All but last bytecode instruction: see if before the first byte location of the first target instruction for the next bytecode
                return address.lessThan(bytecodeInstructions.get(row + 1).targetCodeFirstAddress);
            }
            // Last bytecode instruction:  see if before the end of the target code
            final InstructionMap instructionMap = compiledCode.instructionMap();
            final TargetCodeInstruction lastTargetCodeInstruction = instructionMap.instruction(instructionMap.length() - 1);
            return address.lessThan(lastTargetCodeInstruction.address.plus(lastTargetCodeInstruction.bytes.length));
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
            Arrays.fill(rowToStackFrame, null);
            for (int row = 0; row < bytecodeInstructions.size(); row++) {
                for (MaxStackFrame frame : focus().thread().stack().frames()) {
                    if (rowContainsAddress(row, frame.codeLocation().address())) {
                        rowToStackFrame[row] = frame;
                        break;
                    }
                }
            }
        }
    }

    /**
     * Determines if the compiled code for the bytecode has a target breakpoint set at this location in the VM, in
     * situations where we can map between locations.
     */
    protected List<MaxBreakpoint> getTargetBreakpointsAtRow(int row) {
        final List<MaxBreakpoint> breakpoints = new LinkedList<MaxBreakpoint>();
        if (haveTargetCodeAddresses) {
            for (MaxBreakpoint breakpoint : vm().breakpointManager().breakpoints()) {
                if (!breakpoint.isBytecodeBreakpoint() && rowContainsAddress(row, breakpoint.codeLocation().address())) {
                    breakpoints.add(breakpoint);
                }
            }
        }
        return breakpoints;
    }

    /**
     * @return the bytecode breakpoint, if any, set at the bytecode being displayed in the row.
     */
    protected MaxBreakpoint getBytecodeBreakpointAtRow(int row) {
        for (MaxBreakpoint breakpoint : vm().breakpointManager().breakpoints()) {
            if (breakpoint.isBytecodeBreakpoint()) {
                final MaxCodeLocation breakpointLocation = breakpoint.codeLocation();
                // the direction of key comparison is significant
                if (methodKey.equals(breakpointLocation.methodKey()) &&  bytecodeInstructions().get(row).position() == breakpointLocation.bytecodePosition()) {
                    return breakpoint;
                }
            }
        }
        return null;
    }

    protected final String rowToTagText(int row) {
        // Unimplemented
        return "";
    }

    protected class BytecodeInstruction {

        int position;

        /** AsPosition of first byte of bytecode instruction. */
        int position() {
            return position;
        }

        /** bytes constituting this bytecode instruction. */
        byte[] instructionBytes;

        private int row;

        /** index of this bytecode instruction in the method. */
        int row() {
            return row;
        }

        /** index of the first target code instruction implementing this bytecode (Jit only for now). */
        int targetCodeRow;

        private Address targetCodeFirstAddress;

        /** address of the first byte in the target code instructions implementing this bytecode (Jit only for now. */
        Address targetCodeFirstAddress() {
            return targetCodeFirstAddress;
        }

        int opcode;

        // * Either a rendering component or an index into the constant pool if a reference kind. */
        Object operand1;
        Object operand2;

        BytecodeInstruction(int bytecodeRow, int position, byte[] bytes, int opcode, Object operand1, Object operand2, int targetCodeRow, Address targetCodeFirstAddress) {
            this.row = bytecodeRow;
            this.position = position;
            this.instructionBytes = bytes;
            this.opcode = opcode;
            this.operand1 = operand1;
            this.operand2 = operand2;
            this.targetCodeRow = targetCodeRow;
            this.targetCodeFirstAddress = targetCodeFirstAddress;
        }
    }

    private final class InspectorBytecodePrinter extends BytecodePrinter {

        public InspectorBytecodePrinter(PrintStream stream, ConstantPool constantPool) {
            super(new PrintWriter(stream), constantPool, "", "", 0);
        }

        @Override
        protected void prolog() {
        }

        private int opcode;

        @Override
        protected void printOpcode() {
            opcode = currentOpcode();
        }

        public int opcode() {
            return opcode;
        }

        private Object operand1 = new BytecodeOperandLabel(inspection(), "");

        public Object operand1() {
            return operand1;
        }

        private JComponent operand2 = new BytecodeOperandLabel(inspection(), "");

        public JComponent operand2() {
            return operand2;
        }

        @Override
        protected void printImmediate(int immediate) {
            operand1 = new BytecodeOperandLabel(inspection(), immediate);
        }

        @Override
        protected void printKind(Kind kind) {
            operand1 = new BytecodeOperandLabel(inspection(), kind.name.toString());
        }

        @Override
        protected void printConstant(final int index) {
            operand1 = new Integer(index);
        }

        @Override
        public void iinc(int index, int addend) {
            printOpcode();
            operand1 = new BytecodeOperandLabel(inspection(), index);
            operand2 = new BytecodeOperandLabel(inspection(), addend);
        }

        @Override
        public void multianewarray(int index, int nDimensions) {
            printOpcode();
            printConstant(index);
            operand2 = new BytecodeOperandLabel(inspection(), nDimensions);
        }

        @Override
        public void tableswitch(int defaultOffset, int lowMatch, int highMatch, int numberOfCases) {
            printOpcode();
            operand1 = new BytecodeOperandLabel(inspection(), defaultOffset + ", [" + lowMatch + " - " + highMatch + "] -> ");
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i != numberOfCases; ++i) {
                sb.append(bytecodeScanner().readSwitchOffset());
                if (i != numberOfCases - 1) {
                    sb.append(' ');
                }
            }
            operand2 = new BytecodeOperandLabel(inspection(), sb.toString());
        }

        @Override
        public void lookupswitch(int defaultOffset, int numberOfCases) {
            printOpcode();
            printImmediate(defaultOffset);
            String s = "";
            String separator = ", ";
            for (int i = 0; i < numberOfCases; i++) {
                s += separator + bytecodeScanner().readSwitchCase() + "->" + bytecodeScanner().readSwitchOffset();
                separator = " ";
            }
            operand2 = new BytecodeOperandLabel(inspection(), s);
        }
    }

}
