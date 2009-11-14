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
import com.sun.c1x.asm.Address;
import com.sun.c1x.asm.Label;
import com.sun.c1x.bytecode.BytecodeLookupSwitch;
import com.sun.c1x.bytecode.BytecodeTableSwitch;
import com.sun.c1x.bytecode.Bytecodes;
import com.sun.c1x.ci.CiKind;
import com.sun.c1x.ci.CiRegister;
import com.sun.c1x.ci.CiTarget;
import com.sun.c1x.ri.RiBytecodeExtension;
import com.sun.c1x.ri.RiField;
import com.sun.c1x.ri.RiMethod;
import com.sun.c1x.ri.RiType;
import com.sun.c1x.target.x86.X86;
import com.sun.c1x.target.x86.X86Assembler;
import com.sun.c1x.target.x86.X86MacroAssembler;
import com.sun.c1x.util.Util;

/**
 * The <code>X86CodeGen</code> class definition.
 *
 * @author Ben L. Titzer
 */
public class X86CodeGen extends CodeGen {

    private static boolean GenBoundsCheck = false;

    final X86MacroAssembler asm;
    final boolean is64bit;
    int fakeRegisterNum = -1;

    public X86CodeGen(C0XCompilation compilation, CiTarget target) {
        super(compilation, target);
        asm = new X86MacroAssembler(null, target, -1);
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
    Location genExtendedBytecode(RiBytecodeExtension.Bytecode extcode, Location[] args) {
        return unimplemented(extcode.signatureType().returnBasicType());
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
        CiRegister objReg = allocSrc(object, CiKind.Object);
        CiRegister lenReg = allocDst(CiKind.Int);
        recordImplicitExceptionPoint(NullPointerException.class);
        asm.movl(lenReg, new Address(objReg, runtime.arrayLengthOffsetInBytes()));
        return location(lenReg);
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
    void genPutField(RiField field, Location object, Location value) {
        CiKind basicType = field.kind();
        CiRegister objReg = allocSrc(object, CiKind.Object);
        CiRegister valReg = allocSrc(value, basicType);
        if (field.isLoaded()) {
            // the field is loaded, emit a single store instruction
            recordImplicitExceptionPoint(NullPointerException.class);
            // XXX: write barrier
            emitStore(basicType, new Address(objReg, field.offset()), valReg);
        } else {
            // TODO: call global stub to resolve the field and get its offset
            unimplemented(CiKind.Void);
        }
    }

    @Override
    Location genGetField(RiField field, Location object) {
        CiKind basicType = field.kind();
        CiRegister objReg = allocSrc(object, CiKind.Object);
        if (field.isLoaded()) {
            // the field is loaded, emit a single load instruction
            CiRegister valReg = allocDst(basicType);
            recordImplicitExceptionPoint(NullPointerException.class);
            emitLoad(basicType, valReg, new Address(objReg, field.offset()));
            return location(valReg);
        } else {
            // TODO: call global stub to resolve the field and get its offset
            return unimplemented(basicType);
        }
    }

    @Override
    void getPutStatic(RiField field, Location value) {
        CiKind basicType = field.kind();
        CiRegister valReg = allocSrc(value, basicType);
        if (field.isLoaded() && field.holder().isInitialized()) {
            // TODO: convert CiConstant to object
            Location l = genObjectConstant(field.holder().getEncoding(RiType.Representation.StaticFields));
            CiRegister objReg = allocSrc(l, CiKind.Object);
            // XXX: write barrier
            emitStore(basicType, new Address(objReg, field.offset()), valReg);
        } else {
            // TODO: call global stub to resolve the field and get its offset
            unimplemented(CiKind.Void);
        }
    }

    @Override
    Location genGetStatic(RiField field) {
        CiKind basicType = field.kind();
        CiRegister valReg = allocDst(basicType);
        if (field.isLoaded() && field.holder().isInitialized()) {
            // TODO: convert CiConstant to object
            Location l = genObjectConstant(field.holder().getEncoding(RiType.Representation.StaticFields));
            CiRegister objReg = allocSrc(l, CiKind.Object);
            emitLoad(basicType, valReg, new Address(objReg, field.offset()));
            return location(valReg);
        } else {
            // TODO: call global stub to resolve the field and get its offset
            return unimplemented(basicType);
        }
    }

    @Override
    void genThrow(Location thrown) {
        unimplemented(CiKind.Void);
    }

    @Override
    void genReturn(CiKind basicType, Location value) {
        allocDst(target.config.getReturnRegister(basicType), basicType);
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
    Location genCompareOp(CiKind basicType, int opcode, Location x, Location y) {
        return unimplemented(CiKind.Int);
    }

    @Override
    Location genConvert(int opcode, CiKind from, CiKind to, Location value) {
        CiRegister src = allocSrc(value, from);
        CiRegister dst = allocDst(to);
        switch (opcode) {
            case Bytecodes.I2B:
                asm.movsbl(dst, src);
                return location(dst);
            case Bytecodes.I2S:
                asm.movswl(dst, src);
                return location(dst);
            case Bytecodes.I2C:
                asm.movzwl(dst, src);
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
                asm.jcc(X86Assembler.Condition.notEqual, endLabel);
                recordGlobalStubCallPoint();
// TODO                asm.callGlobalStub(GlobalStub.f2i, dst, src);
                asm.bind(endLabel);
                return location(dst);
            }
            case Bytecodes.F2L: {
                Label endLabel = new Label();
            CiRegister rscratch1 = target.scratchRegister;
                asm.cvttss2siq(dst, src);
                asm.mov64(rscratch1, Long.MIN_VALUE);
                asm.cmpq(dst, rscratch1);
                asm.jcc(X86Assembler.Condition.notEqual, endLabel);
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
                asm.jcc(X86Assembler.Condition.notEqual, endLabel);
// TODO                asm.callGlobalStub(GlobalStub.d2i, dst, src);
                asm.bind(endLabel);
                return location(dst);
            }
            case Bytecodes.D2L: {
                Label endLabel = new Label();
            CiRegister rscratch1 = target.scratchRegister;
                asm.cvttsd2siq(dst, src);
                asm.mov64(rscratch1, Long.MIN_VALUE);
                asm.cmpq(dst, rscratch1);
                asm.jcc(X86Assembler.Condition.notEqual, endLabel);
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
    Location genArrayLoad(CiKind basicType, Location array, Location index) {
        int arrayElemSize = target.sizeInBytes(basicType);
        int arrayBaseOffset = runtime.firstArrayElementOffset(basicType);
        CiRegister objReg = allocSrc(array, CiKind.Object);
        CiRegister indReg = allocSrc(index, CiKind.Int);
        genBoundsCheck(objReg, indReg);
        CiRegister dst = allocDst(basicType);
        Address elemAddress = new Address(objReg, indReg, Address.ScaleFactor.fromInt(arrayElemSize), arrayBaseOffset);
        emitLoad(basicType, dst, elemAddress);
        return location(dst);
    }

    @Override
    void genArrayStore(CiKind basicType, Location array, Location index, Location value) {
        int arrayElemSize = target.sizeInBytes(basicType);
        int arrayBaseOffset = runtime.firstArrayElementOffset(basicType);
        CiRegister objReg = allocSrc(array, CiKind.Object);
        CiRegister indReg = allocSrc(index, CiKind.Int);
        CiRegister valReg = allocSrc(value, basicType);
        genBoundsCheck(objReg, indReg);
        Address elemAddress = new Address(objReg, indReg, Address.ScaleFactor.fromInt(arrayElemSize), arrayBaseOffset);
        emitStore(basicType, elemAddress, valReg);
    }

    private void genBoundsCheck(CiRegister objReg, CiRegister indReg) {
        if (GenBoundsCheck) {
            // TODO: finish bounds check
            CiRegister lenReg = allocTmp(CiKind.Int);
            recordImplicitExceptionPoint(NullPointerException.class);
            asm.movl(lenReg, new Address(objReg, runtime.arrayLengthOffsetInBytes()));
            asm.cmpl(indReg, lenReg);
        }
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
                CiRegister dst = allocSrcDst(x, X86.rax, CiKind.Int);
                CiRegister edx = allocTmp(X86.rdx, CiKind.Int);
                asm.xorl(edx, edx);
                recordImplicitExceptionPoint(ArithmeticException.class);
                asm.idivl(allocSrc(y, CiKind.Int));
                return location(dst);
            }
            case Bytecodes.IREM: {
                allocSrc(x, X86.rax, CiKind.Int);
                CiRegister edx = allocDst(X86.rdx, CiKind.Int);
                asm.xorl(edx, edx);
                recordImplicitExceptionPoint(ArithmeticException.class);
                asm.idivl(allocSrc(y, CiKind.Int));
                return location(edx);
            }
            case Bytecodes.ISHL: {
                allocSrc(y, X86.rcx, CiKind.Int);
                CiRegister dst = allocSrcDst(x, CiKind.Int);
                asm.shll(dst);
                return location(dst);
            }
            case Bytecodes.ISHR: {
                allocSrc(y, X86.rcx, CiKind.Int);
                CiRegister dst = allocSrcDst(x, CiKind.Int);
                asm.sarl(dst);
                return location(dst);
            }
            case Bytecodes.IUSHR: {
                allocSrc(y, X86.rcx, CiKind.Int);
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
                CiRegister dst = allocSrcDst(x, X86.rax, CiKind.Long);
                CiRegister edx = allocTmp(X86.rdx, CiKind.Long);
                asm.xorq(edx, edx);
                recordImplicitExceptionPoint(ArithmeticException.class);
                asm.idivq(allocSrc(y, CiKind.Long));
                return location(dst);
            }
            case Bytecodes.LREM: {
                allocSrc(x, X86.rax, CiKind.Long);
                CiRegister edx = allocDst(X86.rdx, CiKind.Long);
                asm.xorq(edx, edx);
                recordImplicitExceptionPoint(ArithmeticException.class);
                asm.idivq(allocSrc(y, CiKind.Long));
                return location(edx);
            }
            case Bytecodes.LSHL: {
                allocSrc(y, X86.rcx, CiKind.Int);
                CiRegister dst = allocSrcDst(x, CiKind.Long);
                asm.shlq(dst);
                return location(dst);
            }
            case Bytecodes.LSHR: {
                allocSrc(y, X86.rcx, CiKind.Int);
                CiRegister dst = allocSrcDst(x, CiKind.Long);
                asm.sarq(dst);
                return location(dst);
            }
            case Bytecodes.LUSHR: {
                allocSrc(y, X86.rcx, CiKind.Int);
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
            asm.mov64(dst, val);
        }
        return location(dst);
    }

    private void emitLoad(CiKind basicType, CiRegister dst, Address elemAddress) {
        switch (basicType) {
            case Byte:
            case Boolean:
                asm.movsbl(dst, elemAddress);
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
                if (target.referenceSize == 4) {
                    asm.movl(dst, elemAddress);
                } else {
                    asm.movq(dst, elemAddress);
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

    private void emitStore(CiKind basicType, Address elemAddress, CiRegister valReg) {
        switch (basicType) {
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
                if (target.referenceSize == 4) {
                    asm.movl(elemAddress, valReg);
                } else {
                    asm.movq(elemAddress, valReg);
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

    Location unimplemented(CiKind basicType) {
        return new C0XCompilation.Register(fakeRegisterNum--, basicType);
    }

    Location unimplemented(CiKind basicType, String msg) {
        return new C0XCompilation.Register(fakeRegisterNum--, basicType);
    }

    CiRegister allocDst(CiKind basicType) {
        return defaultRegister(basicType);
    }

    CiRegister allocDst(CiRegister r, CiKind basicType) {
        return r;
    }

    CiRegister allocSrc(Location l, CiKind basicType) {
        return defaultRegister(basicType);
    }

    CiRegister allocSrc(Location l, CiRegister r, CiKind basicType) {
        return r;
    }

    CiRegister allocSrcDst(Location l, CiKind basicType) {
        return defaultRegister(basicType);
    }

    CiRegister allocSrcDst(Location l, CiRegister r, CiKind basicType) {
        return r;
    }

    CiRegister allocTmp(CiKind basicType) {
        return defaultRegister(basicType);
    }

    CiRegister allocTmp(CiRegister r, CiKind basicType) {
        return r;
    }

    private CiRegister defaultRegister(CiKind basicType) {
        return basicType == CiKind.Float || basicType == CiKind.Double ? X86.xmm0 : X86.rax;
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
