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

import com.oracle.graal.api.code.*;
import com.oracle.graal.api.meta.*;
import com.oracle.graal.compiler.gen.*;
import com.oracle.graal.lir.*;
import com.oracle.graal.lir.amd64.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.extended.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.nodes.type.*;
import com.oracle.max.vm.ext.graal.*;

/**
 * Represents a call to a native function from within a native method stub.
 */
public final class NativeCallNode extends AbstractCallNode implements LIRLowerable {

    /**
     * The instruction that produces the native function address for this native call.
     */
    @Input private ValueNode address;

    /**
     * The native method for this native call.
     */
    public final ResolvedJavaMethod nativeMethod;

    public NativeCallNode(ValueNode address, ValueNode[] arguments, Kind returnKind, ResolvedJavaMethod nativeMethod) {
        super(StampFactory.forKind(returnKind.getStackKind()), arguments);
        this.nativeMethod = nativeMethod;
        this.address = address;
    }

    public ValueNode address() {
        return address;
    }

    @Override
    public void generate(LIRGeneratorTool gen) {
        LIRGenerator lir = (LIRGenerator) gen;
        FrameState stateDuring = stateAfter().duplicateModified(stateAfter().bci, false, kind());
        LIRFrameState info = lir.stateFor(stateDuring, null);
        Value resultOperand = lir.resultOperandFor(this.kind());
        Value callAddress = lir.operand(this.address());
        CallingConvention cc = CodeUtil.getCallingConvention(MaxGraal.runtime(), CallingConvention.Type.NativeCall, nativeMethod, false);
//        lir.compilation.frameMap().adjustOutgoingStackSize(cc, NativeCall);

        Value[] argList = lir.visitInvokeArguments(cc, arguments());

        // The callAddress is a possibly long-living virtual register used at the call site.  The register allocator does not like this
        // because all virtual registers must be spilled at a call site (we don't have callee-saved registers). Therefore, make a
        // short-lived temporary virtual register where the lifetime is guaranteed to end at the call site.
        // TODO(cwi) Revisit this when doing SSA-based register allocation, see if there is a way the register allocator can handle it.
        callAddress = lir.emitMove(callAddress);

//        argList.add(callAddress);

//        String target = this.nativeMethod.jniSymbol();
        lir.append(new AMD64Call.IndirectCallOp(nativeMethod, resultOperand, argList, null, callAddress, info));

        if (ValueUtil.isLegal(resultOperand)) {
            lir.setResult(this, lir.emitMove(resultOperand));
        }
    }

    @Override
    public LocationIdentity[] getLocationIdentities() {
        // TODO Auto-generated method stub
        return null;
    }
}
