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
package com.sun.max.vm.cps.eir;

import com.sun.max.lang.*;
import com.sun.max.vm.collect.*;

/**
 * @author Bernd Mathiske
 * @author Paul Caprioli
 */
public abstract class EirStop<EirInstructionVisitor_Type extends EirInstructionVisitor, EirTargetEmitter_Type extends EirTargetEmitter>
                      extends EirInstruction<EirInstructionVisitor_Type, EirTargetEmitter_Type> {

    private EirJavaFrameDescriptor javaFrameDescriptor;

    public final EirJavaFrameDescriptor javaFrameDescriptor() {
        return javaFrameDescriptor;
    }

    public void setEirJavaFrameDescriptor(EirJavaFrameDescriptor javaFrameDescriptor) {
        this.javaFrameDescriptor = javaFrameDescriptor;
    }

    public EirStop(EirBlock block) {
        super(block);
    }

    public void addFrameReferenceMap(WordWidth stackSlotWidth, ByteArrayBitMap map) {
        for (EirVariable variable : liveVariables()) {
            if (variable.kind().isReference) {
                EirLocation location = variable.location();
                if (location.category() == EirLocationCategory.STACK_SLOT) {
                    final EirStackSlot stackSlot = (EirStackSlot) location;
                    if (stackSlot.purpose != EirStackSlot.Purpose.PARAMETER) {
                        final int stackSlotBitIndex = stackSlot.offset / stackSlotWidth.numberOfBytes;
                        map.set(stackSlotBitIndex);
                    }
                }
            }
        }
    }

    @Override
    public void visitOperands(EirOperand.Procedure visitor) {
        EirJavaFrameDescriptor javaFrameDescriptor = this.javaFrameDescriptor;
        while (javaFrameDescriptor != null) {
            for (EirOperand operand : javaFrameDescriptor.locals) {
                visitor.run(operand);
            }
            javaFrameDescriptor = javaFrameDescriptor.parent();
        }
    }

}
