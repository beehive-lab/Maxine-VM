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
package com.oracle.max.graal.compiler.target.amd64;

import com.oracle.max.asm.*;
import com.oracle.max.asm.target.amd64.*;
import com.oracle.max.graal.compiler.*;
import com.oracle.max.graal.compiler.asm.*;
import com.oracle.max.graal.compiler.lir.*;
import com.oracle.max.graal.compiler.util.*;
import com.sun.cri.ci.*;

/**
 * This class implements the x86-specific code generation for LIR.
 */
public final class AMD64LIRAssembler extends LIRAssembler {

    final CiTarget target;
    final AMD64MacroAssembler masm;

    public AMD64LIRAssembler(GraalCompilation compilation, TargetMethodAssembler tasm) {
        super(compilation, tasm);
        masm = (AMD64MacroAssembler) asm;
        target = compilation.compiler.target;
    }


    protected CiRegister asIntReg(CiValue value) {
        assert value.kind == CiKind.Int || value.kind == CiKind.Jsr;
        return asRegister(value);
    }

    protected CiRegister asLongReg(CiValue value) {
        assert value.kind == CiKind.Long;
        return asRegister(value);
    }

    protected CiRegister asObjectReg(CiValue value) {
        assert value.kind == CiKind.Object;
        return asRegister(value);
    }

    protected CiRegister asFloatReg(CiValue value) {
        assert value.kind == CiKind.Float;
        return asRegister(value);
    }

    protected CiRegister asDoubleReg(CiValue value) {
        assert value.kind == CiKind.Double;
        return asRegister(value);
    }

    protected CiRegister asRegister(CiValue value) {
        return value.asRegister();
    }

    /**
     * Returns the integer value of any constants that can be represented by a 32-bit integer value,
     * including long constants that fit into the 32-bit range.
     */
    protected int asIntConst(CiValue value) {
        assert (value.kind.stackKind() == CiKind.Int || value.kind == CiKind.Jsr || value.kind == CiKind.Long) && value.isConstant();
        long c = ((CiConstant) value).asLong();
        if (!(NumUtil.isInt(c))) {
            throw Util.shouldNotReachHere();
        }
        return (int) c;
    }

    /**
     * Returns the address of a float constant that is embedded as a data references into the code.
     */
    protected CiAddress asFloatConstRef(CiValue value) {
        assert value.kind == CiKind.Float && value.isConstant();
        return tasm.recordDataReferenceInCode((CiConstant) value);
    }

    /**
     * Returns the address of a double constant that is embedded as a data references into the code.
     */
    protected CiAddress asDoubleConstRef(CiValue value) {
        assert value.kind == CiKind.Double && value.isConstant();
        return tasm.recordDataReferenceInCode((CiConstant) value);
    }

    protected CiAddress asAddress(CiValue value) {
        if (value.isStackSlot()) {
            return compilation.frameMap().toStackAddress((CiStackSlot) value);
        }
        return (CiAddress) value;
    }
}
