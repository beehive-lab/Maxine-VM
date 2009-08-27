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
import com.sun.c1x.ci.RiBytecodeExtension;
import com.sun.c1x.ci.RiField;
import com.sun.c1x.ci.RiMethod;
import com.sun.c1x.ci.RiType;
import com.sun.c1x.target.Register;
import com.sun.c1x.target.Target;
import com.sun.c1x.target.x86.X86;
import com.sun.c1x.target.x86.X86Assembler;
import com.sun.c1x.target.x86.X86FrameMap;
import com.sun.c1x.target.x86.X86MacroAssembler;
import com.sun.c1x.util.Util;
import com.sun.c1x.value.BasicType;


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

    public X86CodeGen(C0XCompilation compilation, Target target) {
        super(compilation, target);
        asm = new X86MacroAssembler(null, target);
        is64bit = target.arch.is64bit();
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
        Register objReg = allocSrc(object, BasicType.Object);
        Register lenReg = allocDst(BasicType.Int);
        recordImplicitExceptionPoint(NullPointerException.class);
        asm.movl(lenReg, new Address(objReg, runtime.arrayLengthOffsetInBytes()));
        return location(lenReg);
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
    void genPutField(RiField field, Location object, Location value) {
        BasicType basicType = field.basicType();
        Register objReg = allocSrc(object, BasicType.Object);
        Register valReg = allocSrc(value, basicType);
        if (field.isLoaded()) {
            // the field is loaded, emit a single store instruction
            recordImplicitExceptionPoint(NullPointerException.class);
            // XXX: write barrier
            emitStore(basicType, new Address(objReg, field.offset()), valReg);
        } else {
            // TODO: call global stub to resolve the field and get its offset
            unimplemented(BasicType.Void);
        }
    }

    @Override
    Location genGetField(RiField field, Location object) {
        BasicType basicType = field.basicType();
        Register objReg = allocSrc(object, BasicType.Object);
        if (field.isLoaded()) {
            // the field is loaded, emit a single load instruction
            Register valReg = allocDst(basicType);
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
        BasicType basicType = field.basicType();
        Register valReg = allocSrc(value, basicType);
        if (field.isLoaded() && field.holder().isInitialized()) {
            // TODO: convert CiConstant to object
            Location l = genObjectConstant(field.holder().getEncoding(RiType.Representation.StaticFields));
            Register objReg = allocSrc(l, BasicType.Object);
            // XXX: write barrier
            emitStore(basicType, new Address(objReg, field.offset()), valReg);
        } else {
            // TODO: call global stub to resolve the field and get its offset
            unimplemented(BasicType.Void);
        }
    }

    @Override
    Location genGetStatic(RiField field) {
        BasicType basicType = field.basicType();
        Register valReg = allocDst(basicType);
        if (field.isLoaded() && field.holder().isInitialized()) {
            // TODO: convert CiConstant to object
            Location l = genObjectConstant(field.holder().getEncoding(RiType.Representation.StaticFields));
            Register objReg = allocSrc(l, BasicType.Object);
            emitLoad(basicType, valReg, new Address(objReg, field.offset()));
            return location(valReg);
        } else {
            // TODO: call global stub to resolve the field and get its offset
            return unimplemented(basicType);
        }
    }

    @Override
    void genThrow(Location thrown) {
        unimplemented(BasicType.Void);
    }

    @Override
    void genReturn(BasicType basicType, Location value) {
        allocDst(runtime.returnRegister(basicType), basicType);
        // TODO: adjust stack pointer
        asm.ret(0);
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
        Register dst = allocSrc(r, BasicType.Word);
        asm.jmp(dst);
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
        Register dst = allocSrcDst(l, BasicType.Int);
        asm.incl(dst);
        return location(dst);
    }

    @Override
    Location genCompareOp(BasicType basicType, int opcode, Location x, Location y) {
        return unimplemented(BasicType.Int);
    }

    @Override
    Location genConvert(int opcode, BasicType from, BasicType to, Location value) {
        Register src = allocSrc(value, from);
        Register dst = allocDst(to);
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
                Register rscratch1 = X86FrameMap.rscratch1(target.arch);
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
                Register rscratch1 = X86FrameMap.rscratch1(target.arch);
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
    Location genArrayLoad(BasicType basicType, Location array, Location index) {
        int arrayElemSize = target.sizeInBytes(basicType);
        int arrayBaseOffset = runtime.firstArrayElementOffsetInBytes(basicType);
        Register objReg = allocSrc(array, BasicType.Object);
        Register indReg = allocSrc(index, BasicType.Int);
        genBoundsCheck(objReg, indReg);
        Register dst = allocDst(basicType);
        Address elemAddress = new Address(objReg, indReg, Address.ScaleFactor.fromInt(arrayElemSize), arrayBaseOffset);
        emitLoad(basicType, dst, elemAddress);
        return location(dst);
    }

    @Override
    void genArrayStore(BasicType basicType, Location array, Location index, Location value) {
        int arrayElemSize = target.sizeInBytes(basicType);
        int arrayBaseOffset = runtime.firstArrayElementOffsetInBytes(basicType);
        Register objReg = allocSrc(array, BasicType.Object);
        Register indReg = allocSrc(index, BasicType.Int);
        Register valReg = allocSrc(value, basicType);
        genBoundsCheck(objReg, indReg);
        Address elemAddress = new Address(objReg, indReg, Address.ScaleFactor.fromInt(arrayElemSize), arrayBaseOffset);
        emitStore(basicType, elemAddress, valReg);
    }

    private void genBoundsCheck(Register objReg, Register indReg) {
        if (GenBoundsCheck) {
            // TODO: finish bounds check
            Register lenReg = allocTmp(BasicType.Int);
            recordImplicitExceptionPoint(NullPointerException.class);
            asm.movl(lenReg, new Address(objReg, runtime.arrayLengthOffsetInBytes()));
            asm.cmpl(indReg, lenReg);
        }
    }

    @Override
    Location genIntOp2(int opcode, Location x, Location y) {
        switch (opcode) {
            case Bytecodes.IADD: {
                Register dst = allocSrcDst(x, BasicType.Int);
                asm.addl(dst, allocSrc(y, BasicType.Int));
                return location(dst);
            }
            case Bytecodes.ISUB: {
                Register dst = allocSrcDst(x, BasicType.Int);
                asm.subl(dst, allocSrc(y, BasicType.Int));
                return location(dst);
            }
            case Bytecodes.IMUL: {
                Register dst = allocSrcDst(x, BasicType.Int);
                asm.imull(dst, allocSrc(y, BasicType.Int));
                return location(dst);
            }
            case Bytecodes.IDIV: {
                Register dst = allocSrcDst(x, X86.rax, BasicType.Int);
                Register edx = allocTmp(X86.rdx, BasicType.Int);
                asm.xorl(edx, edx);
                recordImplicitExceptionPoint(ArithmeticException.class);
                asm.idivl(allocSrc(y, BasicType.Int));
                return location(dst);
            }
            case Bytecodes.IREM: {
                allocSrc(x, X86.rax, BasicType.Int);
                Register edx = allocDst(X86.rdx, BasicType.Int);
                asm.xorl(edx, edx);
                recordImplicitExceptionPoint(ArithmeticException.class);
                asm.idivl(allocSrc(y, BasicType.Int));
                return location(edx);
            }
            case Bytecodes.ISHL: {
                allocSrc(y, X86.rcx, BasicType.Int);
                Register dst = allocSrcDst(x, BasicType.Int);
                asm.shll(dst);
                return location(dst);
            }
            case Bytecodes.ISHR: {
                allocSrc(y, X86.rcx, BasicType.Int);
                Register dst = allocSrcDst(x, BasicType.Int);
                asm.sarl(dst);
                return location(dst);
            }
            case Bytecodes.IUSHR: {
                allocSrc(y, X86.rcx, BasicType.Int);
                Register dst = allocSrcDst(x, BasicType.Int);
                asm.shrl(dst);
                return location(dst);
            }
            case Bytecodes.IAND: {
                Register dst = allocSrcDst(x, BasicType.Int);
                asm.andl(dst, allocSrc(y, BasicType.Int));
                return location(dst);
            }
            case Bytecodes.IOR: {
                Register dst = allocSrcDst(x, BasicType.Int);
                asm.orl(dst, allocSrc(y, BasicType.Int));
                return location(dst);
            }
            case Bytecodes.IXOR: {
                Register dst = allocSrcDst(x, BasicType.Int);
                asm.xorl(dst, allocSrc(y, BasicType.Int));
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
                Register dst = allocSrcDst(x, BasicType.Int);
                asm.addq(dst, allocSrc(y, BasicType.Int));
                return location(dst);
            }
            case Bytecodes.LSUB: {
                Register dst = allocSrcDst(x, BasicType.Int);
                asm.subq(dst, allocSrc(y, BasicType.Int));
                return location(dst);
            }
            case Bytecodes.LMUL: {
                Register dst = allocSrcDst(x, BasicType.Int);
                asm.imulq(dst, allocSrc(y, BasicType.Int));
                return location(dst);
            }
            case Bytecodes.LDIV: {
                Register dst = allocSrcDst(x, X86.rax, BasicType.Long);
                Register edx = allocTmp(X86.rdx, BasicType.Long);
                asm.xorq(edx, edx);
                recordImplicitExceptionPoint(ArithmeticException.class);
                asm.idivq(allocSrc(y, BasicType.Long));
                return location(dst);
            }
            case Bytecodes.LREM: {
                allocSrc(x, X86.rax, BasicType.Long);
                Register edx = allocDst(X86.rdx, BasicType.Long);
                asm.xorq(edx, edx);
                recordImplicitExceptionPoint(ArithmeticException.class);
                asm.idivq(allocSrc(y, BasicType.Long));
                return location(edx);
            }
            case Bytecodes.LSHL: {
                allocSrc(y, X86.rcx, BasicType.Int);
                Register dst = allocSrcDst(x, BasicType.Long);
                asm.shlq(dst);
                return location(dst);
            }
            case Bytecodes.LSHR: {
                allocSrc(y, X86.rcx, BasicType.Int);
                Register dst = allocSrcDst(x, BasicType.Long);
                asm.sarq(dst);
                return location(dst);
            }
            case Bytecodes.LUSHR: {
                allocSrc(y, X86.rcx, BasicType.Int);
                Register dst = allocSrcDst(x, BasicType.Long);
                asm.shrq(dst);
                return location(dst);
            }
            case Bytecodes.LAND: {
                Register dst = allocSrcDst(x, BasicType.Long);
                asm.andq(dst, allocSrc(y, BasicType.Long));
                return location(dst);
            }
            case Bytecodes.LOR : {
                Register dst = allocSrcDst(x, BasicType.Long);
                asm.orq(dst, allocSrc(y, BasicType.Long));
                return location(dst);
            }
            case Bytecodes.LXOR: {
                Register dst = allocSrcDst(x, BasicType.Long);
                asm.xorq(dst, allocSrc(y, BasicType.Long));
                return location(dst);
            }
        }
        throw Util.shouldNotReachHere();
    }

    @Override
    Location genFloatOp2(int opcode, Location x, Location y) {
        switch (opcode) {
            case Bytecodes.FADD: {
                Register dst = allocSrcDst(x, BasicType.Float);
                asm.addss(dst, allocSrc(y, BasicType.Float));
                return location(dst);
            }
            case Bytecodes.FSUB: {
                Register dst = allocSrcDst(x, BasicType.Float);
                asm.subss(dst, allocSrc(y, BasicType.Float));
                return location(dst);
            }
            case Bytecodes.FMUL: {
                Register dst = allocSrcDst(x, BasicType.Float);
                asm.mulss(dst, allocSrc(y, BasicType.Float));
                return location(dst);
            }
            case Bytecodes.FDIV: {
                Register dst = allocSrcDst(x, BasicType.Float);
                asm.divss(dst, allocSrc(y, BasicType.Float));
                return location(dst);
            }
            case Bytecodes.FREM:
                // TODO: this has to be done with a runtime call or global stub
                return unimplemented(BasicType.Float);
        }
        throw Util.shouldNotReachHere();
    }

    @Override
    Location genDoubleOp2(int opcode, Location x, Location y) {
        switch (opcode) {
            case Bytecodes.DADD: {
                Register dst = allocSrcDst(x, BasicType.Double);
                asm.addsd(dst, allocSrc(y, BasicType.Double));
                return location(dst);
            }
            case Bytecodes.DSUB: {
                Register dst = allocSrcDst(x, BasicType.Double);
                asm.subsd(dst, allocSrc(y, BasicType.Double));
                return location(dst);
            }
            case Bytecodes.DMUL: {
                Register dst = allocSrcDst(x, BasicType.Double);
                asm.mulsd(dst, allocSrc(y, BasicType.Double));
                return location(dst);
            }
            case Bytecodes.DDIV: {
                Register dst = allocSrcDst(x, BasicType.Double);
                asm.divsd(dst, allocSrc(y, BasicType.Double));
                return location(dst);
            }
            case Bytecodes.DREM:
                // TODO: this has to be done with a runtime call or global stub
                return unimplemented(BasicType.Double);
        }
        throw Util.shouldNotReachHere();
    }

    @Override
    Location genIntNeg(int opcode, Location x) {
        Register dst = allocSrcDst(x, BasicType.Int);
        asm.negl(dst);
        return location(dst);
    }

    @Override
    Location genLongNeg(int opcode, Location x) {
        Register dst = allocSrcDst(x, BasicType.Long);
        if (is64bit) {
            asm.negq(dst);
        } else {
            return unimplemented(BasicType.Long);
        }
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
        Register dst = allocDst(BasicType.Int);
        if (val == 0) {
            asm.xorl(dst, dst);
        } else {
            asm.movl(dst, val);
        }
        return location(dst);
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
        Register dst = allocDst(BasicType.Long);
        assert is64bit;
        if (val == 0) {
            asm.xorq(dst, dst);
        } else {
            asm.mov64(dst, val);
        }
        return location(dst);
    }

    private void emitLoad(BasicType basicType, Register dst, Address elemAddress) {
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

    private void emitStore(BasicType basicType, Address elemAddress, Register valReg) {
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

    Location unimplemented(BasicType basicType) {
        return new C0XCompilation.Register(fakeRegisterNum--, basicType);
    }

    Location unimplemented(BasicType basicType, String msg) {
        return new C0XCompilation.Register(fakeRegisterNum--, basicType);
    }

    Register allocDst(BasicType basicType) {
        return defaultRegister(basicType);
    }

    Register allocDst(Register r, BasicType basicType) {
        return r;
    }

    Register allocSrc(Location l, BasicType basicType) {
        return defaultRegister(basicType);
    }

    Register allocSrc(Location l, Register r, BasicType basicType) {
        return r;
    }

    Register allocSrcDst(Location l, BasicType basicType) {
        return defaultRegister(basicType);
    }

    Register allocSrcDst(Location l, Register r, BasicType basicType) {
        return r;
    }

    Register allocTmp(BasicType basicType) {
        return defaultRegister(basicType);
    }

    Register allocTmp(Register r, BasicType basicType) {
        return r;
    }

    private Register defaultRegister(BasicType basicType) {
        return basicType == BasicType.Float || basicType == BasicType.Double ? X86.xmm0 : X86.rax;
    }

    Location location(Register r) {
        return new C0XCompilation.Register(r.number, null);
    }

    void recordImplicitExceptionPoint(Class<?> eClass) {
        // TODO: record register state for implicit exception point
    }

    void recordGlobalStubCallPoint() {

    }

}
