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
package com.sun.max.tele.object;

import java.util.*;

import com.sun.max.tele.*;
import com.sun.max.tele.method.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.LocalVariableTable.Entry;
import com.sun.max.vm.cps.jit.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.stack.CompiledStackFrameLayout.Slots;
import com.sun.max.vm.stack.*;

/**
 * Canonical surrogate for a Jit compilation of a Java {@link ClassMethod} in the VM.
 *
 * @author Michael Van De Vanter
 */
public class TeleJitTargetMethod extends TeleCPSTargetMethod {

    TeleJitTargetMethod(TeleVM teleVM, Reference jitTargetMethodReference) {
        super(teleVM, jitTargetMethodReference);
    }

    @Override
    public int[] getBytecodeToTargetCodePositionMap() {
        JitTargetMethod jitTargetMethod = (JitTargetMethod) targetMethod();
        return jitTargetMethod.bytecodeToTargetCodePositionMap();
    }

    @Override
    protected DeepCopier newDeepCopier() {
        return new ReducedDeepCopier().omit(vm().teleFields().JitTargetMethod_referenceMapEditor.fieldActor());
    }

    @Override
    public BytecodeLocation[] getPositionToBytecodeLocationMap() {
        BytecodeLocation[] bytecodeLocations = new BytecodeLocation[getCodeLength()];
        final int[] bytecodeToTargetCodePositionMap = getBytecodeToTargetCodePositionMap();
        if (bytecodeToTargetCodePositionMap == null) {
            return super.getPositionToBytecodeLocationMap();
        }
        final List<TargetCodeInstruction> instructions = getInstructions();
        int bytecodeIndex = 0; // position cursor in the original bytecode stream, used if we have a bytecode-> machine code map
        for (int index = 0; index < instructions.size(); index++) {
            final TargetCodeInstruction instruction = instructions.get(index);
            // offset in bytes of this machine code instruction from beginning
            final int position = instruction.position;
            final int bytecodePosition = bytecodeIndex;
            // To check if we're crossing a bytecode boundary in the JITed code,
            ///compare the offset of the instruction at the current row with the offset recorded by the JIT
            // for the start of bytecode template.
            if (bytecodePosition < bytecodeToTargetCodePositionMap.length &&
                            position == bytecodeToTargetCodePositionMap[bytecodePosition]) {
                // This is the start of the machine code block implementing the next bytecode
                bytecodeLocations[position] = targetMethod().getBytecodeLocationFor(instruction.address.asPointer(), false);
                do {
                    ++bytecodeIndex;
                } while (bytecodeIndex < bytecodeToTargetCodePositionMap.length &&
                                bytecodeToTargetCodePositionMap[bytecodeIndex] == 0);
            }
        }
        return bytecodeLocations;
    }

    /**
     * Gets the name of the source variable corresponding to a stack slot, if any.
     *
     * @param slotIndex a stack slot
     * @return the Java source name for the frame slot, null if not available.
     */
    @Override
    public String sourceVariableName(MaxStackFrame.Compiled javaStackFrame, int slotIndex) {
        final JitTargetMethod jitTargetMethod = (JitTargetMethod) targetMethod();
        final JitStackFrameLayout jitLayout = (JitStackFrameLayout) javaStackFrame.layout();
        final Slots slots = javaStackFrame.layout().slots();
        final int bytecodePosition = jitTargetMethod.bytecodePositionFor(javaStackFrame.ip());
        if (bytecodePosition < 0) {
            return null;
        }
        final ClassMethodActor classMethodActor = jitTargetMethod.classMethodActor();
        if (classMethodActor == null) {
            return null;
        }
        CodeAttribute codeAttribute = classMethodActor.codeAttribute();
        if (codeAttribute == null) {
            return null;
        }
        for (int localVariableIndex = 0; localVariableIndex < codeAttribute.maxLocals; ++localVariableIndex) {
            final int localVariableOffset = jitLayout.localVariableOffset(localVariableIndex);
            if (slots.slot(slotIndex).offset == localVariableOffset) {
                final Entry entry = codeAttribute.localVariableTable().findLocalVariable(localVariableIndex, bytecodePosition);
                if (entry != null) {
                    return entry.name(codeAttribute.constantPool).string;
                }
            }
        }
        return null;
    }
}
