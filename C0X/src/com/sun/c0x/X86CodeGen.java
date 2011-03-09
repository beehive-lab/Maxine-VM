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
package com.sun.c0x;

import com.sun.c0x.C0XCompilation.Location;
import com.sun.c1x.asm.Label;
import com.sun.c1x.target.amd64.AMD64;
import com.sun.c1x.target.amd64.AMD64Assembler;
import com.sun.c1x.target.amd64.AMD64MacroAssembler;
import com.sun.c1x.util.Util;
import com.sun.cri.bytecode.BytecodeLookupSwitch;
import com.sun.cri.bytecode.BytecodeTableSwitch;
import com.sun.cri.bytecode.Bytecodes;
import com.sun.cri.ci.CiAddress;
import com.sun.cri.ci.CiKind;
import com.sun.cri.ci.CiRegister;
import com.sun.cri.ci.CiTarget;
import com.sun.cri.ri.RiField;
import com.sun.cri.ri.RiMethod;
import com.sun.cri.ri.RiType;

/**
 * The {@code X86CodeGen} class definition.
 *
 * @author Ben L. Titzer
 */
public class X86CodeGen extends CodeGen {

    // Checkstyle: stop

    final AMD64MacroAssembler asm;
    final boolean is64bit;
    int fakeRegisterNum = -1;

    public X86CodeGen(C0XCompilation compilation, CiTarget target) {
        super(compilation, target);
        asm = new AMD64MacroAssembler(null, compilation.registerConfig);
        is64bit = target.arch.is64bit();
    }

    @Override
    void genBreakpoint(int bci) {
        unimplemented(CiKind.Void);
    }

    @Override
    Location genNewMultiArray(RiType type, Location[] lengths) {
        return unimplemented(CiKind.Object);
    }

    @Override
    void genMonitorExit(Location object) {
        unimplemented(CiKind.Void);
    }

    @Override
    void genMonitorEnter(Location object) {
        unimplemented(CiKind.Void);
    }

    @Override
    Location genInstanceOf(RiType type, Location object) {
        return unimplemented(CiKind.Int);
    }

    @Override
    Location genCheckCast(RiType type, Location object) {
        return unimplemented(CiKind.Object);
    }

    @Override
    Location genArrayLength(Location object) {
        return unimplemented(CiKind.Int);
    }

    @Override
    Location genNewObjectArray(RiType type, Location length) {
        return unimplemented(CiKind.Object);
    }

    @Override
    Location genNewTypeArray(CiKind elemType, Location length) {
        return unimplemented(CiKind.Object);
    }

    @Override
    Location genNewInstance(RiType type) {
        return unimplemented(CiKind.Object);
    }

    @Override
    Location genInvokeInterface(RiMethod riMethod, Location[] args) {
        return unimplemented(riMethod.signature().returnKind());
    }

    @Override
    Location genInvokeStatic(RiMethod riMethod, Location[] args) {
        return unimplemented(riMethod.signature().returnKind());
    }

    @Override
    Location genInvokeSpecial(RiMethod riMethod, Location[] args) {
        return unimplemented(riMethod.signature().returnKind());
    }

    @Override
    Location genInvokeVirtual(RiMethod riMethod, Location[] args) {
        return unimplemented(riMethod.signature().returnKind());
    }

    @Override
    void genPutField(RiField field, Location object, Location value) {
        CiKind kind = field.kind();
        CiRegister objReg = allocSrc(object, CiKind.Object);
        CiRegister valReg = allocSrc(value, kind);
        if (field.isResolved()) {
            // the field is loaded, emit a single store instruction
            recordImplicitExceptionPoint(NullPointerException.class);
            // XXX: write barrier
            // XXX: store
            //emitStore(kind, new Address(objReg, field.offset()), valReg);
        } else {
            // TODO: call global stub to resolve the field and get its offset
            unimplemented(CiKind.Void);
        }
    }

    @Override
    Location genGetField(RiField field, Location object) {
        CiKind kind = field.kind();
        CiRegister objReg = allocSrc(object, CiKind.Object);
        if (field.isResolved()) {
            // the field is loaded, emit a single load instruction
            CiRegister valReg = allocDst(kind);
            recordImplicitExceptionPoint(NullPointerException.class);
            // emitLoad(kind, valReg, new Address(objReg, field.offset()));
            return location(valReg);
        } else {
            // TODO: call global stub to resolve the field and get its offset
            return unimplemented(kind);
        }
    }

    @Override
    void getPutStatic(RiField field, Location value) {
        CiKind kind = field.kind();
        CiRegister valReg = allocSrc(value, kind);
        if (field.isResolved() && field.holder().isInitialized()) {
            // TODO: convert CiConstant to object
            Location l = genObjectConstant(field.holder().getEncoding(RiType.Representation.StaticFields));
            CiRegister objReg = allocSrc(l, CiKind.Object);
            // XXX: write barrier

            // XXX: store
            //emitStore(kind, new Address(objReg, field.offset()), valReg);
        } else {
            // TODO: call global stub to resolve the field and get its offset
            unimplemented(CiKind.Void);
        }
    }

    @Override
    Location genGetStatic(RiField field) {
        CiKind kind = field.kind();
        CiRegister valReg = allocDst(kind);
        if (field.isResolved() && field.holder().isInitialized()) {
            // TODO: convert CiConstant to object
            Location l = genObjectConstant(field.holder().getEncoding(RiType.Representation.StaticFields));
            CiRegister objReg = allocSrc(l, CiKind.Object);
            //emitLoad(kind, valReg, new Address(objReg, field.offset()));
            return location(valReg);
        } else {
            // TODO: call global stub to resolve the field and get its offset
            return unimplemented(kind);
        }
    }

    @Override
    void genThrow(Location thrown) {
        unimplemented(CiKind.Void);
    }

    @Override
    void genReturn(CiKind kind, Location value) {
        allocDst(compilation.registerConfig.getReturnRegister(kind), kind);
        // TODO: adjust stack pointer
        asm.ret(0);
    }

    @Override
    void genTableswitch(BytecodeTableSwitch bytecodeTableSwitch, Location key) {
        unimplemented(CiKind.Void);
    }

    @Override
    void genLookupswitch(BytecodeLookupSwitch bytecodeLookupSwitch, Location key) {
        unimplemented(CiKind.Void);
    }

    @Override
    void genRet(Location r) {
        CiRegister dst = allocSrc(r, CiKind.Word);
        asm.jmp(dst);
    }

    @Override
    Location genJsr(int bci, int targetBCI) {
        return unimplemented(CiKind.Word);
    }

    @Override
    void genGoto(int bci, int targetBCI) {
        unimplemented(CiKind.Void);
    }

    @Override
    void genIfNull(C0XCompilation.Condition cond, Location obj, int nextBCI, int targetBCI) {
        unimplemented(CiKind.Void);
    }

    @Override
    void genIfSame(C0XCompilation.Condition cond, Location x, Location y, int nextBCI, int targetBCI) {
        unimplemented(CiKind.Void);
    }

    @Override
    void genIfZero(C0XCompilation.Condition cond, Location val, int nextBCI, int targetBCI) {
        unimplemented(CiKind.Void);
    }

    @Override
    Location genIncrement(Location l) {
        CiRegister dst = allocSrcDst(l, CiKind.Int);
        asm.incl(dst);
        return location(dst);
    }

    @Override
    Location genCompareOp(CiKind kind, int opcode, Location x, Location y) {
        return unimplemented(CiKind.Int);
    }

    @Override
    Location genConvert(int opcode, CiKind from, CiKind to, Location value) {
        CiRegister src = allocSrc(value, from);
        CiRegister dst = allocDst(to);
        switch (opcode) {
            case Bytecodes.I2B:
                asm.movsxb(dst, src);
                return location(dst);
            case Bytecodes.I2S:
                asm.movsxw(dst, src);
                return location(dst);
            case Bytecodes.I2C:
                asm.movzxl(dst, src);
                return location(dst);
            case Bytecodes.I2L:
                assert is64bit;
                asm.movslq(dst, src);
                return location(dst);
            case Bytecodes.I2F:
                asm.cvtsi2ssl(dst, src);
                return location(dst);
            case Bytecodes.I2D:
                asm.cvtsi2sdl(dst, src);
                return location(dst);
            case Bytecodes.L2I:
                assert is64bit;
                asm.movl(dst, src); // TODO: is this correct?
                return location(dst);
            case Bytecodes.L2F:
                assert is64bit;
                asm.cvtsi2ssq(dst, src);
                return location(dst);
            case Bytecodes.L2D:
                assert is64bit;
                asm.cvtsi2sdq(dst, src);
                return location(dst);
            case Bytecodes.F2I: {
                Label endLabel = new Label();
                asm.cvttss2sil(dst, src);
                asm.cmp32(dst, Integer.MIN_VALUE);
                asm.jcc(AMD64Assembler.ConditionFlag.notEqual, endLabel);
                recordGlobalStubCallPoint();
// TODO                asm.callGlobalStub(GlobalStub.f2i, dst, src);
                asm.bind(endLabel);
                return location(dst);
            }
            case Bytecodes.F2L: {
                Label endLabel = new Label();
                CiRegister rscratch1 = compilation.registerConfig.getScratchRegister();
                asm.cvttss2siq(dst, src);
                asm.movq(rscratch1, Long.MIN_VALUE);
                asm.cmpq(dst, rscratch1);
                asm.jcc(AMD64Assembler.ConditionFlag.notEqual, endLabel);
// TODO                asm.callGlobalStub(GlobalStub.f2i, dst, src);
                asm.bind(endLabel);
                return location(dst);
            }
            case Bytecodes.F2D:
                asm.cvtss2sd(dst, src);
                return location(dst);
            case Bytecodes.D2I: {
                Label endLabel = new Label();
                asm.cvttsd2sil(dst, src);
                asm.cmp32(dst, Integer.MIN_VALUE);
                asm.jcc(AMD64Assembler.ConditionFlag.notEqual, endLabel);
// TODO                asm.callGlobalStub(GlobalStub.d2i, dst, src);
                asm.bind(endLabel);
                return location(dst);
            }
            case Bytecodes.D2L: {
                Label endLabel = new Label();
                CiRegister rscratch1 = compilation.registerConfig.getScratchRegister();
                asm.cvttsd2siq(dst, src);
                asm.movq(rscratch1, Long.MIN_VALUE);
                asm.cmpq(dst, rscratch1);
                asm.jcc(AMD64Assembler.ConditionFlag.notEqual, endLabel);
// TODO                asm.callGlobalStub(GlobalStub.d2i, dst, src);
                asm.bind(endLabel);
                return location(dst);
            }
            case Bytecodes.D2F:
                asm.cvtsd2ss(dst, src);
                return location(dst);
        }
        throw Util.shouldNotReachHere();
    }

    @Override
    Location genArrayLoad(CiKind kind, Location array, Location index) {
        throw Util.unimplemented();
    }

    @Override
    void genArrayStore(CiKind kind, Location array, Location index, Location value) {
        throw Util.unimplemented();
    }

    @Override
    Location genIntOp2(int opcode, Location x, Location y) {
        switch (opcode) {
            case Bytecodes.IADD: {
                CiRegister dst = allocSrcDst(x, CiKind.Int);
                asm.addl(dst, allocSrc(y, CiKind.Int));
                return location(dst);
            }
            case Bytecodes.ISUB: {
                CiRegister dst = allocSrcDst(x, CiKind.Int);
                asm.subl(dst, allocSrc(y, CiKind.Int));
                return location(dst);
            }
            case Bytecodes.IMUL: {
                CiRegister dst = allocSrcDst(x, CiKind.Int);
                asm.imull(dst, allocSrc(y, CiKind.Int));
                return location(dst);
            }
            case Bytecodes.IDIV: {
                CiRegister dst = allocSrcDst(x, AMD64.rax, CiKind.Int);
                CiRegister edx = allocTmp(AMD64.rdx, CiKind.Int);
                asm.xorl(edx, edx);
                recordImplicitExceptionPoint(ArithmeticException.class);
                asm.idivl(allocSrc(y, CiKind.Int));
                return location(dst);
            }
            case Bytecodes.IREM: {
                allocSrc(x, AMD64.rax, CiKind.Int);
                CiRegister edx = allocDst(AMD64.rdx, CiKind.Int);
                asm.xorl(edx, edx);
                recordImplicitExceptionPoint(ArithmeticException.class);
                asm.idivl(allocSrc(y, CiKind.Int));
                return location(edx);
            }
            case Bytecodes.ISHL: {
                allocSrc(y, AMD64.rcx, CiKind.Int);
                CiRegister dst = allocSrcDst(x, CiKind.Int);
                asm.shll(dst);
                return location(dst);
            }
            case Bytecodes.ISHR: {
                allocSrc(y, AMD64.rcx, CiKind.Int);
                CiRegister dst = allocSrcDst(x, CiKind.Int);
                asm.sarl(dst);
                return location(dst);
            }
            case Bytecodes.IUSHR: {
                allocSrc(y, AMD64.rcx, CiKind.Int);
                CiRegister dst = allocSrcDst(x, CiKind.Int);
                asm.shrl(dst);
                return location(dst);
            }
            case Bytecodes.IAND: {
                CiRegister dst = allocSrcDst(x, CiKind.Int);
                asm.andl(dst, allocSrc(y, CiKind.Int));
                return location(dst);
            }
            case Bytecodes.IOR: {
                CiRegister dst = allocSrcDst(x, CiKind.Int);
                asm.orl(dst, allocSrc(y, CiKind.Int));
                return location(dst);
            }
            case Bytecodes.IXOR: {
                CiRegister dst = allocSrcDst(x, CiKind.Int);
                asm.xorl(dst, allocSrc(y, CiKind.Int));
                return location(dst);
            }
        }
        throw Util.shouldNotReachHere();
    }

    @Override
    Location genLongOp2(int opcode, Location x, Location y) {
        assert is64bit : "32-bit mode not supported yet";
        switch (opcode) {
            case Bytecodes.LADD: {
                CiRegister dst = allocSrcDst(x, CiKind.Int);
                asm.addq(dst, allocSrc(y, CiKind.Int));
                return location(dst);
            }
            case Bytecodes.LSUB: {
                CiRegister dst = allocSrcDst(x, CiKind.Int);
                asm.subq(dst, allocSrc(y, CiKind.Int));
                return location(dst);
            }
            case Bytecodes.LMUL: {
                CiRegister dst = allocSrcDst(x, CiKind.Int);
                asm.imulq(dst, allocSrc(y, CiKind.Int));
                return location(dst);
            }
            case Bytecodes.LDIV: {
                CiRegister dst = allocSrcDst(x, AMD64.rax, CiKind.Long);
                CiRegister edx = allocTmp(AMD64.rdx, CiKind.Long);
                asm.xorq(edx, edx);
                recordImplicitExceptionPoint(ArithmeticException.class);
                asm.idivq(allocSrc(y, CiKind.Long));
                return location(dst);
            }
            case Bytecodes.LREM: {
                allocSrc(x, AMD64.rax, CiKind.Long);
                CiRegister edx = allocDst(AMD64.rdx, CiKind.Long);
                asm.xorq(edx, edx);
                recordImplicitExceptionPoint(ArithmeticException.class);
                asm.idivq(allocSrc(y, CiKind.Long));
                return location(edx);
            }
            case Bytecodes.LSHL: {
                allocSrc(y, AMD64.rcx, CiKind.Int);
                CiRegister dst = allocSrcDst(x, CiKind.Long);
                asm.shlq(dst);
                return location(dst);
            }
            case Bytecodes.LSHR: {
                allocSrc(y, AMD64.rcx, CiKind.Int);
                CiRegister dst = allocSrcDst(x, CiKind.Long);
                asm.sarq(dst);
                return location(dst);
            }
            case Bytecodes.LUSHR: {
                allocSrc(y, AMD64.rcx, CiKind.Int);
                CiRegister dst = allocSrcDst(x, CiKind.Long);
                asm.shrq(dst);
                return location(dst);
            }
            case Bytecodes.LAND: {
                CiRegister dst = allocSrcDst(x, CiKind.Long);
                asm.andq(dst, allocSrc(y, CiKind.Long));
                return location(dst);
            }
            case Bytecodes.LOR : {
                CiRegister dst = allocSrcDst(x, CiKind.Long);
                asm.orq(dst, allocSrc(y, CiKind.Long));
                return location(dst);
            }
            case Bytecodes.LXOR: {
                CiRegister dst = allocSrcDst(x, CiKind.Long);
                asm.xorq(dst, allocSrc(y, CiKind.Long));
                return location(dst);
            }
        }
        throw Util.shouldNotReachHere();
    }

    @Override
    Location genFloatOp2(int opcode, Location x, Location y) {
        switch (opcode) {
            case Bytecodes.FADD: {
                CiRegister dst = allocSrcDst(x, CiKind.Float);
                asm.addss(dst, allocSrc(y, CiKind.Float));
                return location(dst);
            }
            case Bytecodes.FSUB: {
                CiRegister dst = allocSrcDst(x, CiKind.Float);
                asm.subss(dst, allocSrc(y, CiKind.Float));
                return location(dst);
            }
            case Bytecodes.FMUL: {
                CiRegister dst = allocSrcDst(x, CiKind.Float);
                asm.mulss(dst, allocSrc(y, CiKind.Float));
                return location(dst);
            }
            case Bytecodes.FDIV: {
                CiRegister dst = allocSrcDst(x, CiKind.Float);
                asm.divss(dst, allocSrc(y, CiKind.Float));
                return location(dst);
            }
            case Bytecodes.FREM:
                // TODO: this has to be done with a runtime call or global stub
                return unimplemented(CiKind.Float);
        }
        throw Util.shouldNotReachHere();
    }

    @Override
    Location genDoubleOp2(int opcode, Location x, Location y) {
        switch (opcode) {
            case Bytecodes.DADD: {
                CiRegister dst = allocSrcDst(x, CiKind.Double);
                asm.addsd(dst, allocSrc(y, CiKind.Double));
                return location(dst);
            }
            case Bytecodes.DSUB: {
                CiRegister dst = allocSrcDst(x, CiKind.Double);
                asm.subsd(dst, allocSrc(y, CiKind.Double));
                return location(dst);
            }
            case Bytecodes.DMUL: {
                CiRegister dst = allocSrcDst(x, CiKind.Double);
                asm.mulsd(dst, allocSrc(y, CiKind.Double));
                return location(dst);
            }
            case Bytecodes.DDIV: {
                CiRegister dst = allocSrcDst(x, CiKind.Double);
                asm.divsd(dst, allocSrc(y, CiKind.Double));
                return location(dst);
            }
            case Bytecodes.DREM:
                // TODO: this has to be done with a runtime call or global stub
                return unimplemented(CiKind.Double);
        }
        throw Util.shouldNotReachHere();
    }

    @Override
    Location genIntNeg(int opcode, Location x) {
        CiRegister dst = allocSrcDst(x, CiKind.Int);
        asm.negl(dst);
        return location(dst);
    }

    @Override
    Location genLongNeg(int opcode, Location x) {
        CiRegister dst = allocSrcDst(x, CiKind.Long);
        if (is64bit) {
            asm.negq(dst);
        } else {
            return unimplemented(CiKind.Long);
        }
        return location(dst);
    }

    @Override
    Location genFloatNeg(int opcode, Location x) {
        return unimplemented(CiKind.Float);
    }

    @Override
    Location genDoubleNeg(int opcode, Location x) {
        return unimplemented(CiKind.Double);
    }

    @Override
    Location genResolveClass(RiType type) {
        return unimplemented(CiKind.Object);
    }

    @Override
    Location genObjectConstant(Object aClass) {
        return unimplemented(CiKind.Object);
    }

    @Override
    Location genIntConstant(int val) {
        CiRegister dst = allocDst(CiKind.Int);
        if (val == 0) {
            asm.xorl(dst, dst);
        } else {
            asm.movl(dst, val);
        }
        return location(dst);
    }

    @Override
    Location genDoubleConstant(double val) {
        return unimplemented(CiKind.Double);
    }

    @Override
    Location genFloatConstant(float val) {
        return unimplemented(CiKind.Float);
    }

    @Override
    Location genLongConstant(long val) {
        CiRegister dst = allocDst(CiKind.Long);
        assert is64bit;
        if (val == 0) {
            asm.xorq(dst, dst);
        } else {
            asm.movq(dst, val);
        }
        return location(dst);
    }

    void emitLoad(CiKind kind, CiRegister dst, CiAddress elemAddress) {
        switch (kind) {
            case Byte:
            case Boolean:
                asm.movsxb(dst, elemAddress);
                break;
            case Short:
                asm.movswl(dst, elemAddress);
                break;
            case Char:
                asm.movw(dst, elemAddress);
                break;
            case Int:
                asm.movl(dst, elemAddress);
                break;
            case Long:
                asm.movq(dst, elemAddress);
                break;
            case Float:
                asm.movss(dst, elemAddress);
                break;
            case Double:
                asm.movsd(dst, elemAddress);
                break;
            case Object:
                if (is64bit) {
                    asm.movq(dst, elemAddress);
                } else {
                    asm.movl(dst, elemAddress);
                }
                break;
            case Word:
                if (is64bit) {
                    asm.movq(dst, elemAddress);
                } else {
                    asm.movl(dst, elemAddress);
                }
                break;
        }
    }

    void emitStore(CiKind kind, CiAddress elemAddress, CiRegister valReg) {
        switch (kind) {
            case Byte:
            case Boolean:
                asm.movb(elemAddress, valReg);
                break;
            case Short:
                asm.movw(elemAddress, valReg);
                break;
            case Char:
                asm.movw(elemAddress, valReg);
                break;
            case Int:
                asm.movl(elemAddress, valReg);
                break;
            case Long:
                asm.movq(elemAddress, valReg);
                break;
            case Float:
                asm.movss(elemAddress, valReg);
                break;
            case Double:
                asm.movsd(elemAddress, valReg);
                break;
            case Object:
                if (is64bit) {
                    asm.movq(elemAddress, valReg);
                } else {
                    asm.movl(elemAddress, valReg);
                }
                break;
            case Word:
                if (is64bit) {
                    asm.movq(elemAddress, valReg);
                } else {
                    asm.movl(elemAddress, valReg);
                }
                break;
        }
    }

    Location unimplemented(CiKind kind) {
        return new C0XCompilation.Register(fakeRegisterNum--, kind);
    }

    Location unimplemented(CiKind kind, String msg) {
        return new C0XCompilation.Register(fakeRegisterNum--, kind);
    }

    CiRegister allocDst(CiKind kind) {
        return defaultRegister(kind);
    }

    CiRegister allocDst(CiRegister r, CiKind kind) {
        return r;
    }

    CiRegister allocSrc(Location l, CiKind kind) {
        return defaultRegister(kind);
    }

    CiRegister allocSrc(Location l, CiRegister r, CiKind kind) {
        return r;
    }

    CiRegister allocSrcDst(Location l, CiKind kind) {
        return defaultRegister(kind);
    }

    CiRegister allocSrcDst(Location l, CiRegister r, CiKind kind) {
        return r;
    }

    CiRegister allocTmp(CiKind kind) {
        return defaultRegister(kind);
    }

    CiRegister allocTmp(CiRegister r, CiKind kind) {
        return r;
    }

    private CiRegister defaultRegister(CiKind kind) {
        return kind == CiKind.Float || kind == CiKind.Double ? AMD64.xmm0 : AMD64.rax;
    }

    Location location(CiRegister r) {
        return new C0XCompilation.Register(r.number, null);
    }

    void recordImplicitExceptionPoint(Class<?> eClass) {
        // TODO: record register state for implicit exception point
    }

    void recordGlobalStubCallPoint() {

    }
}
