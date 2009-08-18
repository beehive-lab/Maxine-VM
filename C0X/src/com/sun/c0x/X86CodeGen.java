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

import com.sun.c1x.ci.*;
import com.sun.c1x.value.BasicType;
import com.sun.c1x.bytecode.BytecodeTableSwitch;
import com.sun.c1x.bytecode.BytecodeLookupSwitch;
import com.sun.c1x.bytecode.Bytecodes;
import com.sun.c1x.target.Target;
import com.sun.c1x.target.Register;
import com.sun.c1x.target.x86.X86MacroAssembler;
import com.sun.c1x.util.Util;
import com.sun.c0x.C0XCompilation.Location;

/**
 * The <code>X86CodeGen</code> class definition.
 *
 * @author Ben L. Titzer
 */
public class X86CodeGen extends CodeGen {

    final X86MacroAssembler asm;

    public X86CodeGen(C0XCompilation compilation, Target target) {
        super(compilation, target);
        // TODO: make macro assembler compiler independent
        asm = new X86MacroAssembler(null);
    }

    @Override
    void genBreakpoint(int bci) {
        unimplemented(BasicType.Void);
    }

    @Override
    Location genNewMultiArray(RiType type, Location[] lengths) {
        return unimplemented(BasicType.Object);
    }

    @Override
    Location genExtendedBytecode(RiBytecodeExtension.Bytecode extcode, Location[] args) {
        return unimplemented(extcode.signatureType().returnBasicType());
    }

    @Override
    void genMonitorExit(Location object) {
        unimplemented(BasicType.Void);
    }

    @Override
    void genMonitorEnter(Location object) {
        unimplemented(BasicType.Void);
    }

    @Override
    Location genInstanceOf(RiType type, Location object) {
        return unimplemented(BasicType.Int);
    }

    @Override
    Location genCheckCast(RiType type, Location object) {
        return unimplemented(BasicType.Object);
    }

    @Override
    Location genArrayLength(Location object) {
        return unimplemented(BasicType.Int);
    }

    @Override
    Location genNewObjectArray(RiType type, Location length) {
        return unimplemented(BasicType.Object);
    }

    @Override
    Location genNewTypeArray(BasicType elemType, Location length) {
        return unimplemented(BasicType.Object);
    }

    @Override
    Location genNewInstance(RiType type) {
        return unimplemented(BasicType.Object);
    }

    @Override
    Location genInvokeInterface(RiMethod riMethod, Location[] args) {
        return unimplemented(riMethod.signatureType().returnBasicType());
    }

    @Override
    Location genInvokeStatic(RiMethod riMethod, Location[] args) {
        return unimplemented(riMethod.signatureType().returnBasicType());
    }

    @Override
    Location genInvokeSpecial(RiMethod riMethod, Location[] args) {
        return unimplemented(riMethod.signatureType().returnBasicType());
    }

    @Override
    Location genInvokeVirtual(RiMethod riMethod, Location[] args) {
        return unimplemented(riMethod.signatureType().returnBasicType());
    }

    @Override
    void genPutField(RiField riField, Location object, Location value) {
        unimplemented(BasicType.Void);
    }

    @Override
    Location genGetField(RiField riField, Location object) {
        return unimplemented(riField.basicType());
    }

    @Override
    void getPutStatic(RiField riField, Location value) {
        unimplemented(BasicType.Void);
    }

    @Override
    Location genGetStatic(RiField riField) {
        return unimplemented(riField.basicType());
    }

    @Override
    void genThrow(Location thrown) {
        unimplemented(BasicType.Void);
    }

    @Override
    void genReturn(Location value) {
        unimplemented(BasicType.Void);
    }

    @Override
    void genTableswitch(BytecodeTableSwitch bytecodeTableSwitch, Location key) {
        unimplemented(BasicType.Void);
    }

    @Override
    void genLookupswitch(BytecodeLookupSwitch bytecodeLookupSwitch, Location key) {
        unimplemented(BasicType.Void);
    }

    @Override
    void genRet(Location r) {
        unimplemented(BasicType.Void);
    }

    @Override
    Location genJsr(int bci, int targetBCI) {
        return unimplemented(BasicType.Word);
    }

    @Override
    void genGoto(int bci, int targetBCI) {
        unimplemented(BasicType.Void);
    }

    @Override
    void genIfNull(C0XCompilation.Condition cond, Location obj, int nextBCI, int targetBCI) {
        unimplemented(BasicType.Void);
    }

    @Override
    void genIfSame(C0XCompilation.Condition cond, Location x, Location y, int nextBCI, int targetBCI) {
        unimplemented(BasicType.Void);
    }

    @Override
    void genIfZero(C0XCompilation.Condition cond, Location val, int nextBCI, int targetBCI) {
        unimplemented(BasicType.Void);
    }

    @Override
    Location genIncrement(Location l) {
        return unimplemented(BasicType.Int);
    }

    @Override
    Location genCompareOp(BasicType basicType, int opcode, Location x, Location y) {
        return unimplemented(BasicType.Int);
    }

    @Override
    Location genConvert(int opcode, BasicType from, BasicType to, Location value) {
        return unimplemented(to);
    }

    @Override
    Location genArrayLoad(BasicType basicType, Location array, Location index) {
        return unimplemented(basicType);
    }

    @Override
    void genArrayStore(BasicType basicType, Location array, Location index, Location value) {
        unimplemented(BasicType.Void);
    }

    @Override
    Location genIntOp2(int opcode, Location x, Location y) {
        switch (opcode) {
            case Bytecodes.IADD: {
                Register dst = intRegisterSrcDst(x);
                asm.addl(dst, intRegister(y));
                return location(dst);
            }
            case Bytecodes.ISUB: {
                Register dst = intRegisterSrcDst(x);
                asm.subl(dst, intRegister(y));
                return location(dst);
            }
            case Bytecodes.IMUL: {
                Register dst = intRegisterSrcDst(x);
                asm.imull(dst, intRegister(x));
                return location(dst);
            }
            case Bytecodes.IDIV: {
                return unimplemented(BasicType.Int, "has to be done with global stub");
            }
            case Bytecodes.IREM: {
                return unimplemented(BasicType.Int, "has to be done with global stub");
            }
            case Bytecodes.ISHL: {
                Register dst = intRegisterSrcDst(x);
                Register shift = intRegister(y);
                asm.shldl(dst, shift);
                return location(dst);
            }
            case Bytecodes.ISHR: {
                return unimplemented(BasicType.Int, "two operand form of sarl needed");
            }
            case Bytecodes.IUSHR: {
                Register dst = intRegisterSrcDst(x);
                Register shift = intRegister(y);
                asm.shrdl(dst, shift); // TODO: need two operand form of shrl, or use ecx?
                return location(dst);
            }
            case Bytecodes.IAND: {
                Register dst = intRegisterSrcDst(x);
                asm.andl(dst, intRegister(y));
                return location(dst);
            }
            case Bytecodes.IOR: {
                Register dst = intRegisterSrcDst(x);
                asm.orl(dst, intRegister(y));
                return location(dst);
            }
            case Bytecodes.IXOR: {
                Register dst = intRegisterSrcDst(x);
                asm.xorl(dst, intRegister(y));
                return location(dst);
            }
        }
        throw Util.shouldNotReachHere();
    }

    @Override
    Location genLongOp2(int opcode, Location x, Location y) {
        switch (opcode) {

        }
        throw Util.shouldNotReachHere();
    }

    @Override
    Location genFloatOp2(int opcode, Location x, Location y) {
        switch (opcode) {

        }
        throw Util.shouldNotReachHere();
    }

    @Override
    Location genDoubleOp2(int opcode, Location x, Location y) {
        switch (opcode) {

        }
        throw Util.shouldNotReachHere();
    }

    @Override
    Location genIntNeg(int opcode, Location x) {
        Register dst = intRegisterSrcDst(x);
        asm.negl(dst);
        return location(dst);
    }

    @Override
    Location genLongNeg(int opcode, Location x) {
        Register dst = intRegisterSrcDst(x);
        asm.negq(dst); // TODO: only works on 64-bit
        return location(dst);
    }

    @Override
    Location genFloatNeg(int opcode, Location x) {
        return unimplemented(BasicType.Float);
    }

    @Override
    Location genDoubleNeg(int opcode, Location x) {
        return unimplemented(BasicType.Double);
    }

    @Override
    Location genResolveClass(RiType type) {
        return unimplemented(BasicType.Object);
    }

    @Override
    Location genObjectConstant(Object aClass) {
        return unimplemented(BasicType.Object);
    }

    @Override
    Location genIntConstant(int val) {
        return unimplemented(BasicType.Int);
    }

    @Override
    Location genDoubleConstant(double val) {
        return unimplemented(BasicType.Double);
    }

    @Override
    Location genFloatConstant(float val) {
        return unimplemented(BasicType.Float);
    }

    @Override
    Location genLongConstant(long val) {
        return unimplemented(BasicType.Long);
    }

    Location unimplemented(BasicType basicType) {
        return null;
    }

    Location unimplemented(BasicType basicType, String msg) {
        return null;
    }

    Register intRegister(Location l) {
        return null;
    }

    Register intRegisterDestroy(Location l) {
        return null;
    }

    Register eaxDestroy() {
        return null;
    }

    Register ecxDestroy() {
        return null;
    }

    Register edxDestroy() {
        return null;
    }

    Register intRegisterSrcDst(Location l) {
        return null;
    }

    Register longRegister(Location l) {
        return null;
    }

    Register longRegisterSrcDst(Location l) {
        return null;
    }

    Register floatRegister(Location l) {
        return null;
    }

    Register doubleRegister(Location l) {
        return null;
    }

    Location location(Register r) {
        return null;
    }
}
