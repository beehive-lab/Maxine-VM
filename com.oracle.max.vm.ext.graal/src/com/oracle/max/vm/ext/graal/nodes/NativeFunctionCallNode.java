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
package com.oracle.max.vm.ext.graal.nodes;

import java.util.*;

import com.oracle.graal.amd64.*;
import com.oracle.graal.api.code.*;
import com.oracle.graal.api.meta.*;
import com.oracle.graal.compiler.gen.*;
import com.oracle.graal.lir.*;
import com.oracle.graal.lir.amd64.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.nodes.type.*;
import com.oracle.max.vm.ext.graal.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;

/**
 * Represents a call to a native function from within a native method stub.
 * Start off life as abstract node in the template created by the {@link NodeIntrinsic},
 * then becomes specific to the actual native method during lowering, via {@link #updateCall}.
 */
public final class NativeFunctionCallNode extends NativeFunctionAdapterNode implements DeoptimizingNode, LIRLowerable {

    /**
     * The instruction that produces the native function address for this native call.
     */
    @Input private ValueNode address;
    @Input private FrameState deoptState;

    private boolean specialized;

    public NativeFunctionCallNode(ValueNode address, ValueNode[] arguments, Kind returnKind, MethodActor nativeMethodActor) {
        super(StampFactory.forKind(returnKind.getStackKind()), arguments);
        this.nativeMethodActor = nativeMethodActor;
        this.address = address;
    }

    /**
     * Used by the {@link NodeIntrinsic} to create the dummy call in the template.
     */
    public NativeFunctionCallNode(ValueNode address, ValueNode handleBase, ValueNode jniEnv) {
        super(StampFactory.forKind(Kind.Object), new ValueNode[] {address, handleBase, jniEnv});
    }

    @NodeIntrinsic
    public static native Object nativeFunctionCall(Address function, Pointer handles, Pointer jniEnv);

    public void updateCall(ValueNode address, List<ValueNode> nativeArguments, Kind returnKind) {
        setStamp(StampFactory.forKind(returnKind.getStackKind()));
        this.address = address;
        arguments().clear();
        updateUsages(null, address);
        for (ValueNode arg : nativeArguments) {
            arguments().add(arg);
        }
        specialized = true;
    }

    public boolean specialized() {
        return specialized;
    }

    public ValueNode address() {
        return address;
    }

    @Override
    public void generate(LIRGeneratorTool gen) {
        LIRGenerator lir = (LIRGenerator) gen;
        LIRFrameState state = !canDeoptimize() ? null : lir.stateFor(getDeoptimizationState(), getDeoptimizationReason());
        Value resultOperand = lir.resultOperandFor(this.kind());
        Value callAddress = lir.operand(this.address());
        Kind[] paramKinds = new Kind[arguments().size()];
        for (int i = 0; i < arguments().size(); i++) {
            paramKinds[i] = arguments().get(i).kind();
        }
        CallingConvention cc = ((MaxRegisterConfig) MaxGraal.runtime().lookupRegisterConfig()).nativeCallingConvention(kind(), paramKinds);
        lir.frameMap.callsMethod(cc);


        Value[] argList = lir.visitInvokeArguments(cc, arguments());

        AllocatableValue targetAddress = AMD64.rax.asValue();
        lir.emitMove(targetAddress, callAddress);

        lir.append(new AMD64Call.IndirectCallOp(MaxResolvedJavaMethod.get(nativeMethodActor), resultOperand, argList, new Value[0], targetAddress, state));

        if (ValueUtil.isLegal(resultOperand)) {
            lir.setResult(this, lir.emitMove(resultOperand));
        }
    }

    @Override
    public boolean canDeoptimize() {
        return !nativeMethodActor.isCFunction();
    }

    @Override
    public FrameState getDeoptimizationState() {
        if (deoptState != null) {
            return deoptState;
        } else if (stateAfter() != null && canDeoptimize()) {
            FrameState stateDuring = stateAfter();
            if ((stateDuring.stackSize() > 0 && stateDuring.stackAt(stateDuring.stackSize() - 1) == this) || (stateDuring.stackSize() > 1 && stateDuring.stackAt(stateDuring.stackSize() - 2) == this)) {
                stateDuring = stateDuring.duplicateModified(stateDuring.bci, stateDuring.rethrowException(), this.kind());
            }
            setDeoptimizationState(stateDuring);
            return stateDuring;
        }
        return null;
    }

    @Override
    public void setDeoptimizationState(FrameState state) {
        updateUsages(deoptState, state);
        assert deoptState == null && canDeoptimize() : "shouldn't assign deoptState to " + this;
        deoptState = state;
    }

    @Override
    public DeoptimizationReason getDeoptimizationReason() {
        return null;
    }
}
