/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.graal.compiler.lir;

import java.util.*;

import com.sun.cri.ci.*;

/**
 * This class represents debugging and deoptimization information attached to a LIR instruction.
 */
public class LIRDebugInfo {
    public final CiFrame topFrame;
    private final List<CiStackSlot> pointerSlots;
    public final LabelRef exceptionEdge;
    private CiDebugInfo debugInfo;

    public LIRDebugInfo(CiFrame topFrame, List<CiStackSlot> pointerSlots, LabelRef exceptionEdge) {
        this.topFrame = topFrame;
        this.pointerSlots = pointerSlots;
        this.exceptionEdge = exceptionEdge;
    }

    public CiDebugInfo debugInfo() {
        assert debugInfo != null : "debug info not allocated yet";
        return debugInfo;
    }

    public boolean hasDebugInfo() {
        return debugInfo != null;
    }


    public static class MonitorIndex extends CiValue {
        public final int index;

        public MonitorIndex(int index) {
            super(CiKind.Illegal);
            this.index = index;
        }

        @Override
        public String name() {
            return "monitorIndex";
        }
        @Override
        public boolean equals(Object obj) {
            return obj instanceof MonitorIndex && ((MonitorIndex) obj).index == index;
        }
        @Override
        public boolean equalsIgnoringKind(CiValue other) {
            return equals(other);
        }
        @Override
        public int hashCode() {
            return index;
        }
    }

    public interface ValueProcedure {
        CiValue doValue(CiVariable value);
    }


    public void forEachLiveStateValue(ValueProcedure proc) {
        CiBitMap processedObjects = null;
        for (CiFrame cur = topFrame; cur != null; cur = cur.caller()) {
            processedObjects = processValues(cur.values, proc, processedObjects);
        }
    }

    private CiBitMap processValues(CiValue[] values, ValueProcedure proc, CiBitMap processedObjects) {
        for (int i = 0; i < values.length; i++) {
            CiValue value = values[i];
            if (value instanceof CiVariable) {
                CiValue newValue = proc.doValue((CiVariable) value);
                if (newValue != null) {
                    values[i] = newValue;
                }
            } else if (value instanceof CiVirtualObject) {
                CiVirtualObject obj = (CiVirtualObject) value;
                if (processedObjects == null) {
                    processedObjects = new CiBitMap();
                }
                processedObjects.grow(obj.id());
                if (!processedObjects.get(obj.id())) {
                    processedObjects.set(obj.id());
                    processedObjects = processValues(obj.values(), proc, processedObjects);
                }
            } else {
                // Nothing to do for these types.
                assert value == CiValue.IllegalValue || value instanceof CiConstant || value instanceof CiStackSlot || value instanceof MonitorIndex;
            }
        }
        return processedObjects;
    }


    public void initDebugInfo(LIRInstruction op, FrameMap frameMap) {
        CiBitMap frameRefMap = frameMap.initFrameRefMap();
        CiBitMap regRefMap = op.hasCall() ? null : new CiBitMap(frameMap.target.arch.registerReferenceMapBitCount);

        // Add locks that are in the designated frame area.
        for (CiFrame cur = topFrame; cur != null; cur = cur.caller()) {
            for (int i = 0; i < cur.numLocks; i++) {
                CiValue lock = cur.values[i + cur.numLocals + cur.numStack];
                if (lock instanceof MonitorIndex) {
                    int index = ((MonitorIndex) lock).index;
                    cur.values[i + cur.numLocals + cur.numStack] = frameMap.toMonitorBaseStackAddress(index);
                    setOop(frameMap.toMonitorObjectStackAddress(index), frameMap.target, frameRefMap, regRefMap);
                }
            }
        }

        // Add additional stack slots for outoing method parameters.
        if (pointerSlots != null) {
            for (CiStackSlot v : pointerSlots) {
                setOop(v, frameMap.target, frameRefMap, regRefMap);
            }
        }

        debugInfo = new CiDebugInfo(topFrame, regRefMap, frameRefMap);
    }

    public void setOop(CiValue location, CiTarget target, CiBitMap frameRefMap, CiBitMap regRefMap) {
        if (location.isAddress()) {
            CiAddress stackLocation = (CiAddress) location;
            assert stackLocation.index.isIllegal();
            if (stackLocation.base == CiRegister.Frame.asValue()) {
                int offset = stackLocation.displacement;
                assert offset % target.wordSize == 0 : "must be aligned";
                int stackMapIndex = offset / target.wordSize;
                setBit(frameRefMap, stackMapIndex);
            }
        } else if (location.isStackSlot()) {
            CiStackSlot stackSlot = (CiStackSlot) location;
            assert !stackSlot.inCallerFrame();
            assert target.spillSlotSize == target.wordSize;
            setBit(frameRefMap, stackSlot.index());
        } else {
            assert location.isRegister() : "objects can only be in a register";
            CiRegisterValue registerLocation = (CiRegisterValue) location;
            int reg = registerLocation.reg.number;
            assert reg >= 0 : "object cannot be in non-object register " + registerLocation.reg;
            assert reg < target.arch.registerReferenceMapBitCount;
            setBit(regRefMap, reg);
        }
    }

    private static void setBit(CiBitMap refMap, int bit) {
        assert !refMap.get(bit) : "Ref map entry " + bit + " is already set.";
        refMap.set(bit);
    }

    @Override
    public String toString() {
        return debugInfo != null ? debugInfo.toString() : topFrame.toString();
    }
}
