/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.graal.compiler.gen;

import java.util.*;
import java.util.Map.Entry;

import com.oracle.max.graal.compiler.*;
import com.oracle.max.graal.compiler.lir.*;
import com.oracle.max.graal.extensions.*;
import com.oracle.max.graal.graph.*;
import com.oracle.max.graal.nodes.*;
import com.oracle.max.graal.nodes.virtual.*;
import com.sun.cri.ci.*;

public class DebugInfoBuilder {
    public final GraalCompilation compilation;

    public DebugInfoBuilder(GraalCompilation compilation) {
        this.compilation = compilation;
    }

    public LIRDebugInfo build(FrameState state, List<CiStackSlot> pointerSlots, LabelRef exceptionEdge) {
        return new LIRDebugInfo(computeFrame(state), pointerSlots, exceptionEdge);
    }


    private HashMap<VirtualObjectNode, CiVirtualObject> virtualObjects = new HashMap<VirtualObjectNode, CiVirtualObject>();

    private CiFrame computeFrame(FrameState topState) {
        if (compilation.placeholderState != null) {
            return null;
        }

        assert virtualObjects.size() == 0;
        CiFrame frame = computeFrameForState(topState);

        if (virtualObjects.size() != 0) {
            // collect all VirtualObjectField instances:
            IdentityHashMap<VirtualObjectNode, VirtualObjectFieldNode> objectStates = new IdentityHashMap<VirtualObjectNode, VirtualObjectFieldNode>();
            FrameState current = topState;
            do {
                for (Node n : current.virtualObjectMappings()) {
                    VirtualObjectFieldNode field = (VirtualObjectFieldNode) n;
                    // null states occur for objects with 0 fields
                    if (field != null && !objectStates.containsKey(field.object())) {
                        objectStates.put(field.object(), field);
                    }
                }
                current = current.outerFrameState();
            } while (current != null);
            // fill in the CiVirtualObject values:
            // during this process new CiVirtualObjects might be discovered, so repeat until no more changes occur.
            boolean changed;
            do {
                changed = false;
                IdentityHashMap<VirtualObjectNode, CiVirtualObject> virtualObjectsCopy = new IdentityHashMap<VirtualObjectNode, CiVirtualObject>(virtualObjects);
                for (Entry<VirtualObjectNode, CiVirtualObject> entry : virtualObjectsCopy.entrySet()) {
                    if (entry.getValue().values() == null) {
                        VirtualObjectNode vobj = entry.getKey();
                        if (vobj instanceof BoxedVirtualObjectNode) {
                            BoxedVirtualObjectNode boxedVirtualObjectNode = (BoxedVirtualObjectNode) vobj;
                            entry.getValue().setValues(new CiValue[]{toCiValue(boxedVirtualObjectNode.getUnboxedValue())});
                        } else {
                            CiValue[] values = new CiValue[vobj.fieldsCount()];
                            entry.getValue().setValues(values);
                            if (values.length > 0) {
                                changed = true;
                                ValueNode currentField = objectStates.get(vobj);
                                assert currentField != null;
                                do {
                                    if (currentField instanceof VirtualObjectFieldNode) {
                                        int index = ((VirtualObjectFieldNode) currentField).index();
                                        if (values[index] == null) {
                                            values[index] = toCiValue(((VirtualObjectFieldNode) currentField).input());
                                        }
                                        currentField = ((VirtualObjectFieldNode) currentField).lastState();
                                    } else {
                                        assert currentField instanceof PhiNode : currentField;
                                        currentField = ((PhiNode) currentField).valueAt(0);
                                    }
                                } while (currentField != null);
                            }
                        }
                    }
                }
            } while (changed);

            virtualObjects.clear();
        }
        return frame;
    }

    private CiFrame computeFrameForState(FrameState state) {
        CiValue[] values = new CiValue[state.valuesSize() + state.locksSize()];
        int valueIndex = 0;

        for (int i = 0; i < state.valuesSize(); i++) {
            values[valueIndex++] = toCiValue(state.valueAt(i));
        }

        for (int i = 0; i < state.locksSize(); i++) {
            ValueNode lock = state.lockAt(i);
            if (compilation.compiler.runtime.sizeOfBasicObjectLock() != 0) {
                if (!(lock instanceof VirtualObjectNode)) {
                    CiValue monitorAddress = new LIRDebugInfo.MonitorIndex(i);
                    values[valueIndex++] = monitorAddress;
                } else {
                    values[valueIndex++] = CiValue.IllegalValue;
                }
            } else {
                if (lock.isConstant() && compilation.compiler.runtime.asJavaClass(lock.asConstant()) != null) {
                    // lock on class for synchronized static method
                    values[valueIndex++] = lock.asConstant();
                } else {
                    values[valueIndex++] = toCiValue(lock);
                }
            }
        }
        CiFrame caller = null;
        if (state.outerFrameState() != null) {
            caller = computeFrameForState(state.outerFrameState());
        }
        CiFrame frame = new CiFrame(caller, state.method(), state.bci, state.rethrowException(), values, state.localsSize(), state.stackSize(), state.locksSize());
        if (GraalOptions.Extend) {
            frame = overrideFrame(frame);
        }
        return frame;
    }

    private CiValue toCiValue(ValueNode value) {
        if (value instanceof VirtualObjectNode) {
            VirtualObjectNode obj = (VirtualObjectNode) value;
            CiVirtualObject ciObj = virtualObjects.get(value);
            if (ciObj == null) {
                ciObj = CiVirtualObject.get(obj.type(), null, virtualObjects.size());
                virtualObjects.put(obj, ciObj);
            }
            return ciObj;

        } else if (value instanceof ConstantNode) {
            return ((ConstantNode) value).value;

        } else if (value != null) {
            CiValue operand = compilation.operand(value);
            assert operand != null && operand instanceof CiVariable || operand instanceof CiConstant;
            return operand;

        } else {
            // return a dummy value because real value not needed
            return CiValue.IllegalValue;
        }
    }


    public static ThreadLocal<ServiceLoader<FrameModifier>> frameModifierLoader = new ThreadLocal<ServiceLoader<FrameModifier>>();

    private CiFrame overrideFrame(CiFrame frame) {
        ServiceLoader<FrameModifier> serviceLoader = frameModifierLoader.get();
        if (serviceLoader == null) {
            serviceLoader = ServiceLoader.load(FrameModifier.class);
            frameModifierLoader.set(serviceLoader);
        }

        CiFrame result = frame;
        for (FrameModifier modifier : serviceLoader) {
            result = modifier.getFrame(compilation.compiler.runtime, result);
        }
        return result;
    }
}
