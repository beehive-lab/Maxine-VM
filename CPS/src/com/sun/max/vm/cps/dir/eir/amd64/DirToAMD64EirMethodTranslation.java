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
package com.sun.max.vm.cps.dir.eir.amd64;

import com.sun.max.vm.cps.b.c.d.e.amd64.*;
import com.sun.max.vm.cps.dir.*;
import com.sun.max.vm.cps.dir.eir.*;
import com.sun.max.vm.cps.eir.*;
import com.sun.max.vm.cps.eir.amd64.*;
import com.sun.max.vm.cps.eir.amd64.AMD64EirInstruction.CALL;
import com.sun.max.vm.cps.eir.amd64.AMD64EirInstruction.JMP;
import com.sun.max.vm.cps.eir.amd64.AMD64EirInstruction.RET;
import com.sun.max.vm.cps.eir.amd64.AMD64EirInstruction.RUNTIME_CALL;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
public final class DirToAMD64EirMethodTranslation extends DirToEirMethodTranslation {

    public DirToAMD64EirMethodTranslation(EirGenerator eirGenerator, EirMethod eirMethod, DirMethod dirMethod) {
        super(eirGenerator, eirMethod, dirMethod);
    }

    @Override
    protected DirToEirInstructionTranslation createInstructionTranslation(EirBlock eirBlock) {
        return new DirToAMD64EirInstructionTranslation(this, eirBlock);
    }

    @Override
    protected EirPrologue createPrologue(EirBlock eirBlock) {
        return new AMD64EirPrologue(eirBlock, eirMethod(),
                                    calleeSavedEirVariables, calleeSavedEirRegisters,
                                    isCalleeSavedParameter,
                                    eirParameters, parameterEirLocations);
    }

    @Override
    protected EirEpilogue createEpilogue(EirBlock eirBlock) {
        return new AMD64EirEpilogue(eirBlock, eirMethod(),
                                    calleeSavedEirVariables, calleeSavedEirRegisters,
                                    resultEirLocation());
    }

    @Override
    protected EirInstruction createJump(EirBlock eirBlock, EirBlock toBlock) {
        return new JMP(eirBlock, toBlock);
    }

    @Override
    protected EirInstruction createReturn(EirBlock eirBlock) {
        return new RET(eirBlock);
    }

    @Override
    public EirCall createCall(EirBlock eirBlock, EirABI abi, EirValue result, EirLocation resultLocation,
                              EirValue function, EirValue[] arguments, EirLocation[] argumentLocations, boolean isNativeFunctionCall) {
        return new CALL(eirBlock, abi, result, resultLocation, function, arguments, argumentLocations, isNativeFunctionCall, this);
    }

    @Override
    public EirCall createRuntimeCall(EirBlock eirBlock, EirABI abi, EirValue result, EirLocation resultLocation,
                                      EirValue function, EirValue[] arguments, EirLocation[] argumentLocations) {
        return new RUNTIME_CALL(eirBlock, abi, result, resultLocation, function, arguments, argumentLocations, this);
    }

    @Override
    public EirInstruction createAssignment(EirBlock eirBlock, Kind kind, EirValue destination, EirValue source) {
        final EirInstruction<?, ?> assignment = new AMD64EirAssignment(eirBlock, kind, destination, source);
        return assignment;
    }

    @Override
    public EirInfopoint createInfopoint(EirBlock eirBlock, int opcode, EirValue destination) {
        return new AMD64EirInfopoint(eirBlock, opcode, destination);
    }
}
