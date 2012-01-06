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
package com.sun.max.ins.method;

import java.io.*;
import java.util.*;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.debug.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.util.*;
import com.sun.max.io.*;
import com.sun.max.lang.*;
import com.sun.max.tele.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.actor.member.MethodKey.MethodActorKey;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.type.*;

/**
 * Base class for Bytecodes viewers.
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

    private final MaxCompilation compilation;

    /**
     * The compilation associated with this view, if exists.
     */
    protected MaxCompilation compilation() {
        return compilation;
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
     * Disassembled machine code instructions from the associated compilation of the method, null if none associated.
     */
    private List<TargetCodeInstruction> machineCodeInstructions = null;

    private boolean haveMachineCodeAddresses = false;

    /**
     * True if a compiled version of the method is available and if we have a map between bytecode and machine code locations.
     */
    protected boolean haveMachineCodeAddresses() {
        return haveMachineCodeAddresses;
    }

    private List<BytecodeInstruction> bytecodeInstructions = null;

    protected List<BytecodeInstruction> bytecodeInstructions() {
        return bytecodeInstructions;
    }

    /**
     * Base class for bytecode viewers. Machine code is optional, since a method may not yet be compiled, but may appear
     * and change as method is compiled and recompiled.
     */
    protected BytecodeViewer(Inspection inspection, MethodView parent, TeleClassMethodActor teleClassMethodActor, MaxCompilation compilation) {
        super(inspection, parent);
        this.teleClassMethodActor = teleClassMethodActor;
        this.compilation = compilation;
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
        int[] bciToMachineCodePositionMap = null;
        MaxMachineCodeInfo machineCodeInfo = null;
        if (compilation != null) {
            machineCodeInfo = compilation.getMachineCodeInfo();
            bciToMachineCodePositionMap = machineCodeInfo.bciToMachineCodePositionMap();
            // TODO (mlvdv) can only map bytecodes to JIT machine code so far
            if (bciToMachineCodePositionMap != null) {
                haveMachineCodeAddresses = true;
            }
        }
        bytecodeInstructions = new ArrayList<BytecodeInstruction>(10);
        int bci = 0;
        int bytecodeRow = 0;
        int machineCodeRow = 0;
        Address machineCodeFirstAddress = Address.zero();
        while (bci < methodBytes.length) {
            final OutputStream stream = new NullOutputStream();
            try {
                final InspectorBytecodePrinter bytecodePrinter = new InspectorBytecodePrinter(new PrintStream(stream), localConstantPool);
                final BytecodeScanner bytecodeScanner = new BytecodeScanner(bytecodePrinter);
                final int nextBCI = bytecodeScanner.scanInstruction(methodBytes, bci);
                final byte[] instructionBytes = Bytes.getSection(methodBytes, bci, nextBCI);
                if (haveMachineCodeAddresses) {
                    while (machineCodeInfo.instruction(machineCodeRow).position < bciToMachineCodePositionMap[bci]) {
                        machineCodeRow++;
                    }
                    machineCodeFirstAddress = machineCodeInfo.instruction(machineCodeRow).address;
                }
                final BytecodeInstruction instruction = new BytecodeInstruction(bytecodeRow, bci, instructionBytes, bytecodePrinter.opcode(), bytecodePrinter.operand1(),
                                bytecodePrinter.operand2(), machineCodeRow, machineCodeFirstAddress);
                bytecodeInstructions.add(instruction);
                bytecodeRow++;
                bci = nextBCI;
            } catch (Throwable throwable) {
                InspectorError.unexpected("could not disassemble bytecode", throwable);
            }
        }
    }

    /**
     * @return Whether the compiled code in the VM for the bytecode at specified row contains the specified address.
     */
    protected boolean rowContainsAddress(int row, Address address) {
        if (haveMachineCodeAddresses) {
            final BytecodeInstruction bytecodeInstruction = bytecodeInstructions.get(row);
            if (address.lessThan(bytecodeInstruction.machineCodeFirstAddress)) {
                // before the first byte location of the first machine instruction for this bytecode
                return false;
            }
            if (row < (bytecodeInstructions.size() - 1)) {
                // All but last bytecode instruction: see if before the first byte location of the first machine code instruction for the next bytecode
                return address.lessThan(bytecodeInstructions.get(row + 1).machineCodeFirstAddress);
            }
            // Last bytecode instruction:  see if before the end of the machine code
            final MaxMachineCodeInfo machineCodeInfo = compilation.getMachineCodeInfo();
            final TargetCodeInstruction lastMachineCodeInstruction = machineCodeInfo.instruction(machineCodeInfo.length() - 1);
            return address.lessThan(lastMachineCodeInstruction.address.plus(lastMachineCodeInstruction.bytes.length));
        }
        return false;
    }

   /**
     * Rebuilds the cache of stack information if needed, based on the thread that is the current focus.
     * Identifies for each instruction in the method a stack frame (if any) whose instruction pointer is at the address of the instruction.
     */
    @Override
    protected void updateStackCache() {
        if (haveMachineCodeAddresses()) {
            Arrays.fill(rowToStackFrame, null);
            final MaxThread thread = focus().thread();
            if (thread != null) {
                for (int row = 0; row < bytecodeInstructions.size(); row++) {
                    for (MaxStackFrame frame : thread.stack().frames(StackView.DEFAULT_MAX_FRAMES_DISPLAY)) {
                        if (frame.codeLocation() != null && rowContainsAddress(row, frame.codeLocation().address())) {
                            rowToStackFrame[row] = frame;
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Determines if the compiled code for the bytecode has a machine code breakpoint
     * set at this location in the VM, in
     * situations where we can map between locations.
     */
    protected List<MaxBreakpoint> getMachineCodeBreakpointsAtRow(int row) {
        final List<MaxBreakpoint> breakpoints = new LinkedList<MaxBreakpoint>();
        if (haveMachineCodeAddresses) {
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
                if (methodKey.equals(breakpointLocation.methodKey()) &&  bytecodeInstructions().get(row).bci == breakpointLocation.bci()) {
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

    /**
     * A representation of a bytecode instruction, suitable for displaying
     * different aspects of it.
     *
     */
    protected class BytecodeInstruction {

        /**
         * bytecode index: offset from method beginning of the first byte of this instruction.
         */
        public final int bci;


        /**
         * the bytes constituting this instruction.
         */
        public final byte[] instructionBytes;

        /**
         * index of this instruction in the method.
         */
        public final int row;

        /**
         * index of the first machine code instruction implementing this bytecode (Jit only for now).
         */
        public final int machineCodeRow;

        /**
         * address of the first byte in the machine code instructions implementing this bytecode (Jit only for now).
         */
        public final Address machineCodeFirstAddress;

        /**
         * the code of the operation for this instruction.
         */
        public final int opcode;

        // * Either a rendering component or an index into the constant pool if a reference kind. */
        /**
         * The first operand of this instruction:
         * <ul>
         * <li>if the operand is a reference kind, returns an {@link Integer} index into the {@link ConstantPool}</li>
         * <li>if the operand is not a reference kind, returns a {@link BytecodeOperandLabel} that can render the operand.
         * </ul>
         */
        public final Object operand1;

        /**
         * The second operand of this instruction:
         * <ul>
         * <li>if the operand is a reference kind, returns an {@link Integer} index into the {@link ConstantPool}</li>
         * <li>if the operand is not a reference kind, returns a {@link BytecodeOperandLabel} that can render the operand.
         * </ul>
         */
        public final Object operand2;

        BytecodeInstruction(int bytecodeRow, int bci, byte[] bytes, int opcode, Object operand1, Object operand2, int machineCodeRow, Address machineCodeFirstAddress) {
            this.row = bytecodeRow;
            this.bci = bci;
            this.instructionBytes = bytes;
            this.opcode = opcode;
            this.operand1 = operand1;
            this.operand2 = operand2;
            this.machineCodeRow = machineCodeRow;
            this.machineCodeFirstAddress = machineCodeFirstAddress;
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
