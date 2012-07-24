/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.max.ins.debug.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;

/**
 * Base class for views of disassembled machine code for a single method in the VM.
 */
public abstract class MachineCodeViewer extends CodeViewer {

    private final MaxMachineCodeRoutine machineCode;
    private TeleConstantPool teleConstantPool;
    private ConstantPool localConstantPool;
    private String[] rowToTagText;

    protected MachineCodeViewer(Inspection inspection, MethodView parent, MaxMachineCodeRoutine machineCode) {
        super(inspection, parent);
        this.machineCode = machineCode;
        updateMachineCodeInfo();
    }

    /**
     * Updates all information derived from the machine code.
     */
    private void updateMachineCodeInfo() {
        final MaxMachineCodeInfo machineCodeInfo = this.machineCode.getMachineCodeInfo();
        final int machineInstructionCount = machineCodeInfo.length();
        this.rowToTagText = new String[machineInstructionCount];
        rowToStackFrame = new MaxStackFrame[machineInstructionCount];

        teleConstantPool = null;
        localConstantPool = null;
        Arrays.fill(rowToTagText, "");
        if (this.machineCode instanceof MaxCompilation) {
            final MaxCompilation compilation = (MaxCompilation) this.machineCode;
            final TeleClassMethodActor teleClassMethodActor = compilation.getTeleClassMethodActor();
            if (teleClassMethodActor != null) {
                final TeleCodeAttribute teleCodeAttribute = teleClassMethodActor.getTeleCodeAttribute();
                if (teleCodeAttribute != null) {
                    teleConstantPool = teleCodeAttribute.getTeleConstantPool();
                    ClassMethodActor classMethodActor = teleClassMethodActor.classMethodActor();
                    localConstantPool = classMethodActor == null ? null : classMethodActor.codeAttribute().cp;
                    for (int index = 0; index < machineCodeInfo.length(); index++) {
                        final int opcode = machineCodeInfo.opcode(index);
                        if (machineCodeInfo.isBytecodeBoundary(index) && opcode >= 0) {
                            if (opcode == Integer.MAX_VALUE) {
                                rowToTagText[index] = "<epilogue>";
                            } else {
                                rowToTagText[index] = machineCodeInfo.debugInfoAt(index).codePos.bci + ": " + Bytecodes.nameOf(opcode);
                            }
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
        return MethodCodeKind.MACHINE_CODE;
    }

    @Override
    public String codeViewerKindName() {
        return "Machine Code";
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
        final List<MaxStackFrame> frames = thread.stack().frames(StackView.DEFAULT_MAX_FRAMES_DISPLAY);

        Arrays.fill(rowToStackFrame, null);

        // For very deep stacks (e.g. when debugging stack overflow handling),
        // it's faster to loop over the frames and then only loop over the instructions for each
        // frame related to the machine code represented by this viewer.
        final MaxMemoryRegion machineCodeRegion = machineCode().memoryRegion();
        for (MaxStackFrame frame : frames) {
            final MaxCodeLocation frameCodeLocation = frame.codeLocation();
            final MaxMachineCodeRoutine machineCode = frame.machineCode();
            if (frameCodeLocation != null && machineCode != null) {
                final boolean isFrameForThisCode =
                    frame instanceof MaxStackFrame.Compiled ?
                                    machineCodeRegion.overlaps(machineCode.memoryRegion()) :
                                        machineCodeRegion.contains(frameCodeLocation.address());
                if (isFrameForThisCode) {
                    final MaxMachineCodeInfo machineCodeInfo = machineCode.getMachineCodeInfo();
                    for (int row = 0; row < machineCodeInfo.length(); row++) {
                        if (machineCodeInfo.instruction(row).address.equals(frameCodeLocation.address())) {
                            rowToStackFrame[row] = frame;
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * @return surrogate for the machine in the VM for the method being viewed.
     */
    protected MaxMachineCodeRoutine machineCode() {
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
     * Does the instruction address have a machine code breakpoint set in the VM.
     */
    protected MaxBreakpoint getMachineCodeBreakpointAtRow(int row) {
        return vm().breakpointManager().findBreakpoint(machineCode.getMachineCodeInfo().instructionLocation(row));
    }

    protected final String rowToTagText(int row) {
        return rowToTagText[row];
    }

}
