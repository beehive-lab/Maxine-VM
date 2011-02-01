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

import java.util.*;

import com.sun.cri.bytecode.*;
import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.tele.*;
import com.sun.max.tele.MaxMachineCode.*;
import com.sun.max.tele.object.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.constant.*;

/**
 * Base class for views of disassembled target code for a single method in the VM.
 *
 * @author Mick Jordan
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public abstract class TargetCodeViewer extends CodeViewer {

    private final MaxMachineCode machineCode;
    private final InstructionMap instructionMap;
    private TeleConstantPool teleConstantPool;
    private ConstantPool localConstantPool;
    private final String[] rowToTagText;

    protected TargetCodeViewer(Inspection inspection, MethodInspector parent, MaxMachineCode machineCode) {
        super(inspection, parent);
        this.machineCode = machineCode;
        this.instructionMap = machineCode.instructionMap();
        final int targetInstructionCount = instructionMap.length();
        this.rowToTagText = new String[targetInstructionCount];
        rowToStackFrame = new MaxStackFrame[targetInstructionCount];

        teleConstantPool = null;
        localConstantPool = null;
        Arrays.fill(rowToTagText, "");
        if (machineCode instanceof MaxCompiledCode) {
            final MaxCompiledCode compiledCode = (MaxCompiledCode) machineCode;
            final TeleClassMethodActor teleClassMethodActor = compiledCode.getTeleClassMethodActor();
            if (teleClassMethodActor != null) {
                final TeleCodeAttribute teleCodeAttribute = teleClassMethodActor.getTeleCodeAttribute();
                if (teleCodeAttribute != null) {
                    teleConstantPool = teleCodeAttribute.getTeleConstantPool();
                    ClassMethodActor classMethodActor = teleClassMethodActor.classMethodActor();
                    localConstantPool = classMethodActor == null ? null : classMethodActor.codeAttribute().constantPool;
                    for (int index = 0; index < instructionMap.length(); index++) {
                        final int opcode = instructionMap.opcode(index);
                        if (instructionMap.isBytecodeBoundary(index) && opcode >= 0) {
                            rowToTagText[index] = instructionMap.bytecodeLocation(index).bytecodePosition + ": " + Bytecodes.nameOf(opcode);
                        } else {
                            rowToTagText[index] = "";
                        }
                    }
                } else {
                    // Must be a hand crafted stub
                }
            }
        }
        updateStackCache();
    }

    @Override
    public  MethodCodeKind codeKind() {
        return MethodCodeKind.TARGET_CODE;
    }

    @Override
    public String codeViewerKindName() {
        return "Target Code";
    }

    protected InstructionMap instructionMap() {
        return instructionMap;
    }

    /**
     * Rebuilds the cache of stack information if needed, based on the thread that is the current focus.
     * <br>
     * Identifies for each row (instruction) a stack frame (if any) that is related to the instruction.
     * In the case of the top frame, this would be the row (instruction) at the current IP.
     * In the case of other frames, this would be the row (instruction) that is the call return site.
     *
     */
    @Override
    protected void updateStackCache() {
        final MaxThread thread = focus().thread();
        if (thread == null) {
            return;
        }
        final List<MaxStackFrame> frames = thread.stack().frames();

        Arrays.fill(rowToStackFrame, null);

        // For very deep stacks (e.g. when debugging a metacircular related infinite recursion issue),
        // it's faster to loop over the frames and then only loop over the instructions for each
        // frame related to the target code represented by this viewer.
        final MaxMemoryRegion targetCodeRegion = machineCode().memoryRegion();
        for (MaxStackFrame frame : frames) {
            final MaxCodeLocation frameCodeLocation = frame.codeLocation();
            final MaxMachineCode machineCode = frame.compiledCode();
            if (frameCodeLocation != null && machineCode != null) {
                final boolean isFrameForThisCode =
                    frame instanceof MaxStackFrame.Compiled ?
                                    targetCodeRegion.overlaps(machineCode.memoryRegion()) :
                                        targetCodeRegion.contains(frameCodeLocation.address());
                if (isFrameForThisCode) {
                    for (int row = 0; row < instructionMap.length(); row++) {
                        if (instructionMap.instruction(row).address.equals(frameCodeLocation.address())) {
                            rowToStackFrame[row] = frame;
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * @return surrogate for the {@link TargetRoutine} in the VM for the method being viewed.
     */
    protected MaxMachineCode machineCode() {
        return machineCode;
    }

    /**
     * @return surrogate for the {@link ConstantPool} in the VM for the method being viewed.
     */
    protected final TeleConstantPool teleConstantPool() {
        return teleConstantPool;
    }

    /**
     * @return local {@link ConstantPool} for the class containing the method in the VM being viewed.
     */
    protected final ConstantPool localConstantPool() {
        return localConstantPool;
    }

    /**
     * Adapter for bytecode scanning that only knows the constant pool index argument of the last method invocation instruction scanned.
     */
    private static final class MethodRefIndexFinder extends BytecodeAdapter  {
        int methodRefIndex = -1;

        public MethodRefIndexFinder reset() {
            methodRefIndex = -1;
            return this;
        }

        @Override
        protected void invokestatic(int index) {
            methodRefIndex = index;
        }

        @Override
        protected void invokespecial(int index) {
            methodRefIndex = index;
        }

        @Override
        protected void invokevirtual(int index) {
            methodRefIndex = index;
        }

        @Override
        protected void invokeinterface(int index, int count) {
            methodRefIndex = index;
        }

        public int methodRefIndex() {
            return methodRefIndex;
        }
    };

    private final MethodRefIndexFinder methodRefIndexFinder = new MethodRefIndexFinder();

    /**
     * Does the instruction address have a target code breakpoint set in the VM.
     */
    protected MaxBreakpoint getTargetBreakpointAtRow(int row) {
        return vm().breakpointManager().findBreakpoint(instructionMap.instructionLocation(row));
    }

    protected final String rowToTagText(int row) {
        return rowToTagText[row];
    }

}
