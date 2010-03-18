/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.c0x;

import com.sun.c0x.C0XCompilation.Location;
import com.sun.c1x.bytecode.BytecodeLookupSwitch;
import com.sun.c1x.bytecode.BytecodeTableSwitch;
import com.sun.c1x.ci.CiKind;
import com.sun.c1x.ci.CiTarget;
import com.sun.c1x.ri.RiBytecodeExtension;
import com.sun.c1x.ri.RiField;
import com.sun.c1x.ri.RiMethod;
import com.sun.c1x.ri.RiRuntime;
import com.sun.c1x.ri.RiType;

/**
 * The {@code CodeGen} class definition.
 *
 * @author Ben L. Titzer
 */
public abstract class CodeGen {
    protected final RiRuntime runtime;
    protected final CiTarget target;
    protected final RiMethod method;

    public CodeGen(C0XCompilation compilation, CiTarget target) {
        this.runtime = compilation.runtime;
        this.target = compilation.target;
        this.method = compilation.method;
    }

    abstract void genBreakpoint(int bci);

    abstract Location genNewMultiArray(RiType type, Location[] lengths);

    abstract Location genExtendedBytecode(RiBytecodeExtension.Bytecode extcode, Location[] args);

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

    abstract void genReturn(CiKind basicType, Location value);

    abstract void genTableswitch(BytecodeTableSwitch bytecodeTableSwitch, Location key);

    abstract void genLookupswitch(BytecodeLookupSwitch bytecodeLookupSwitch, Location key);

    abstract void genRet(Location r);

    abstract Location genJsr(int bci, int targetBCI);

    abstract void genGoto(int bci, int targetBCI);

    abstract void genIfNull(C0XCompilation.Condition cond, Location obj, int nextBCI, int targetBCI);

    abstract void genIfSame(C0XCompilation.Condition cond, Location x, Location y, int nextBCI, int targetBCI);

    abstract void genIfZero(C0XCompilation.Condition cond, Location val, int nextBCI, int targetBCI);

    abstract Location genIncrement(Location l);

    abstract Location genCompareOp(CiKind basicType, int opcode, Location x, Location y);

    abstract Location genConvert(int opcode, CiKind from, CiKind to, Location value);

    abstract Location genArrayLoad(CiKind basicType, Location array, Location index);

    abstract void genArrayStore(CiKind basicType, Location array, Location index, Location value);

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
