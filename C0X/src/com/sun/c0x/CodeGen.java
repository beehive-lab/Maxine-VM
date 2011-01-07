/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.c0x;

import com.sun.c0x.C0XCompilation.Location;
import com.sun.cri.bytecode.BytecodeLookupSwitch;
import com.sun.cri.bytecode.BytecodeTableSwitch;
import com.sun.cri.ci.CiKind;
import com.sun.cri.ci.CiTarget;
import com.sun.cri.ri.RiField;
import com.sun.cri.ri.RiMethod;
import com.sun.cri.ri.RiRuntime;
import com.sun.cri.ri.RiType;

/**
 * The {@code CodeGen} class definition.
 *
 * @author Ben L. Titzer
 */
public abstract class CodeGen {
    protected final RiRuntime runtime;
    protected final CiTarget target;
    protected final RiMethod method;
    protected final C0XCompilation compilation;

    public CodeGen(C0XCompilation compilation, CiTarget target) {
        this.runtime = compilation.runtime;
        this.target = compilation.target;
        this.method = compilation.method;
        this.compilation = compilation;
    }

    abstract void genBreakpoint(int bci);

    abstract Location genNewMultiArray(RiType type, Location[] lengths);

    abstract void genMonitorExit(Location object);

    abstract void genMonitorEnter(Location object);

    abstract Location genInstanceOf(RiType type, Location object);

    abstract Location genCheckCast(RiType type, Location object);

    abstract Location genArrayLength(Location object);

    abstract Location genNewObjectArray(RiType type, Location length);

    abstract Location genNewTypeArray(CiKind elemType, Location length);

    abstract Location genNewInstance(RiType type);

    abstract Location genInvokeInterface(RiMethod riMethod, Location[] args);

    abstract Location genInvokeStatic(RiMethod riMethod, Location[] args);

    abstract Location genInvokeSpecial(RiMethod riMethod, Location[] args);

    abstract Location genInvokeVirtual(RiMethod riMethod, Location[] args);

    abstract void genPutField(RiField riField, Location object, Location value);

    abstract Location genGetField(RiField riField, Location object);

    abstract void getPutStatic(RiField riField, Location value);

    abstract Location genGetStatic(RiField riField);

    abstract void genThrow(Location thrown);

    abstract void genReturn(CiKind kind, Location value);

    abstract void genTableswitch(BytecodeTableSwitch bytecodeTableSwitch, Location key);

    abstract void genLookupswitch(BytecodeLookupSwitch bytecodeLookupSwitch, Location key);

    abstract void genRet(Location r);

    abstract Location genJsr(int bci, int targetBCI);

    abstract void genGoto(int bci, int targetBCI);

    abstract void genIfNull(C0XCompilation.Condition cond, Location obj, int nextBCI, int targetBCI);

    abstract void genIfSame(C0XCompilation.Condition cond, Location x, Location y, int nextBCI, int targetBCI);

    abstract void genIfZero(C0XCompilation.Condition cond, Location val, int nextBCI, int targetBCI);

    abstract Location genIncrement(Location l);

    abstract Location genCompareOp(CiKind kind, int opcode, Location x, Location y);

    abstract Location genConvert(int opcode, CiKind from, CiKind to, Location value);

    abstract Location genArrayLoad(CiKind kind, Location array, Location index);

    abstract void genArrayStore(CiKind kind, Location array, Location index, Location value);

    abstract Location genIntOp2(int opcode, Location x, Location y);

    abstract Location genLongOp2(int opcode, Location x, Location y);

    abstract Location genFloatOp2(int opcode, Location x, Location y);

    abstract Location genDoubleOp2(int opcode, Location x, Location y);

    abstract Location genIntNeg(int opcode, Location x);

    abstract Location genLongNeg(int opcode, Location x);

    abstract Location genFloatNeg(int opcode, Location x);

    abstract Location genDoubleNeg(int opcode, Location x);

    abstract Location genResolveClass(RiType type);

    abstract Location genObjectConstant(Object aClass);

    abstract Location genIntConstant(int val);

    abstract Location genDoubleConstant(double val);

    abstract Location genFloatConstant(float val);

    abstract Location genLongConstant(long val);
}
