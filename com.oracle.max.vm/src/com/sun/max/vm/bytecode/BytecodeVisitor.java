/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.bytecode;

import static com.sun.max.vm.classfile.ErrorContext.*;

import com.sun.cri.bytecode.*;
import com.sun.max.annotate.*;

/**
 * An abstract class that can be extended and paired with a {@link BytecodeScanner} to process
 * the instructions decoded from a JVM instruction stream.
 */
public abstract class BytecodeVisitor {

    private BytecodeScanner bytecodeScanner;

    void setBytecodeScanner(BytecodeScanner bytecodeScanner) {
        this.bytecodeScanner = bytecodeScanner;
    }

    @INLINE
    public final BytecodeScanner bytecodeScanner() {
        return bytecodeScanner;
    }

    @INLINE
    public final int currentBCI() {
        return bytecodeScanner.currentBCI();
    }

    @INLINE
    public final int currentOpcode() {
        return bytecodeScanner.currentOpcode();
    }

    /**
     * Determines if the instruction currently being visited has a {@linkplain Bytecodes#WIDE wide} prefix.
     */
    @INLINE
    public final boolean isCurrentOpcodeWidened() {
        return bytecodeScanner.isCurrentOpcodeWidened();
    }

    @INLINE
    public final int currentOpcodeBCI() {
        return bytecodeScanner.currentOpcodeBCI();
    }

    @INLINE
    protected final byte[] code() {
        return bytecodeScanner.bytecodeBlock().code();
    }

    /**
     * Gives the byte code visitor an opportunity to do something in the presence of its byte code scanner before the
     * first byte gets scanned.
     */
    protected void prologue() {
    }

    /**
     * Subclasses override this method if they need to do any processing after the
     * {@linkplain #bytecodeScanner() scanner} has decoded the {@linkplain #currentOpcode() opcode} of an instruction
     * but before it dispatches to the relevant bytecode specific method in this visitor.
     * <p>
     * The default implementation does nothing.
     */
    protected void opcodeDecoded() {
    }

    /**
     * Subclasses override this method if they need to do any processing after the
     * {@linkplain #bytecodeScanner() scanner} has decoded a complete instruction.
     * <p>
     * The default implementation does nothing.
     */
    protected void instructionDecoded() {
    }

    protected abstract void nop();
    protected abstract void aconst_null();
    protected abstract void iconst_m1();
    protected abstract void iconst_0();
    protected abstract void iconst_1();
    protected abstract void iconst_2();
    protected abstract void iconst_3();
    protected abstract void iconst_4();
    protected abstract void iconst_5();
    protected abstract void lconst_0();
    protected abstract void lconst_1();
    protected abstract void fconst_0();
    protected abstract void fconst_1();
    protected abstract void fconst_2();
    protected abstract void dconst_0();
    protected abstract void dconst_1();
    protected abstract void bipush(int operand);
    protected abstract void sipush(int operand);
    protected abstract void ldc(int index);
    protected abstract void ldc_w(int index);
    protected abstract void ldc2_w(int index);
    protected abstract void iload(int index);
    protected abstract void lload(int index);
    protected abstract void fload(int index);
    protected abstract void dload(int index);
    protected abstract void aload(int index);
    protected abstract void iload_0();
    protected abstract void iload_1();
    protected abstract void iload_2();
    protected abstract void iload_3();
    protected abstract void lload_0();
    protected abstract void lload_1();
    protected abstract void lload_2();
    protected abstract void lload_3();
    protected abstract void fload_0();
    protected abstract void fload_1();
    protected abstract void fload_2();
    protected abstract void fload_3();
    protected abstract void dload_0();
    protected abstract void dload_1();
    protected abstract void dload_2();
    protected abstract void dload_3();
    protected abstract void aload_0();
    protected abstract void aload_1();
    protected abstract void aload_2();
    protected abstract void aload_3();
    protected abstract void iaload();
    protected abstract void laload();
    protected abstract void faload();
    protected abstract void daload();
    protected abstract void aaload();
    protected abstract void baload();
    protected abstract void caload();
    protected abstract void saload();
    protected abstract void istore(int index);
    protected abstract void lstore(int index);
    protected abstract void fstore(int index);
    protected abstract void dstore(int index);
    protected abstract void astore(int index);
    protected abstract void istore_0();
    protected abstract void istore_1();
    protected abstract void istore_2();
    protected abstract void istore_3();
    protected abstract void lstore_0();
    protected abstract void lstore_1();
    protected abstract void lstore_2();
    protected abstract void lstore_3();
    protected abstract void fstore_0();
    protected abstract void fstore_1();
    protected abstract void fstore_2();
    protected abstract void fstore_3();
    protected abstract void dstore_0();
    protected abstract void dstore_1();
    protected abstract void dstore_2();
    protected abstract void dstore_3();
    protected abstract void astore_0();
    protected abstract void astore_1();
    protected abstract void astore_2();
    protected abstract void astore_3();
    protected abstract void iastore();
    protected abstract void lastore();
    protected abstract void fastore();
    protected abstract void dastore();
    protected abstract void aastore();
    protected abstract void bastore();
    protected abstract void castore();
    protected abstract void sastore();
    protected abstract void pop();
    protected abstract void pop2();
    protected abstract void dup();
    protected abstract void dup_x1();
    protected abstract void dup_x2();
    protected abstract void dup2();
    protected abstract void dup2_x1();
    protected abstract void dup2_x2();
    protected abstract void swap();
    protected abstract void iadd();
    protected abstract void ladd();
    protected abstract void fadd();
    protected abstract void dadd();
    protected abstract void isub();
    protected abstract void lsub();
    protected abstract void fsub();
    protected abstract void dsub();
    protected abstract void imul();
    protected abstract void lmul();
    protected abstract void fmul();
    protected abstract void dmul();
    protected abstract void idiv();
    protected abstract void ldiv();
    protected abstract void fdiv();
    protected abstract void ddiv();
    protected abstract void irem();
    protected abstract void lrem();
    protected abstract void frem();
    protected abstract void drem();
    protected abstract void ineg();
    protected abstract void lneg();
    protected abstract void fneg();
    protected abstract void dneg();
    protected abstract void ishl();
    protected abstract void lshl();
    protected abstract void ishr();
    protected abstract void lshr();
    protected abstract void iushr();
    protected abstract void lushr();
    protected abstract void iand();
    protected abstract void land();
    protected abstract void ior();
    protected abstract void lor();
    protected abstract void ixor();
    protected abstract void lxor();
    protected abstract void iinc(int index, int addend);
    protected abstract void i2l();
    protected abstract void i2f();
    protected abstract void i2d();
    protected abstract void l2i();
    protected abstract void l2f();
    protected abstract void l2d();
    protected abstract void f2i();
    protected abstract void f2l();
    protected abstract void f2d();
    protected abstract void d2i();
    protected abstract void d2l();
    protected abstract void d2f();
    protected abstract void i2b();
    protected abstract void i2c();
    protected abstract void i2s();
    protected abstract void lcmp();
    protected abstract void fcmpl();
    protected abstract void fcmpg();
    protected abstract void dcmpl();
    protected abstract void dcmpg();
    protected abstract void ifeq(int offset);
    protected abstract void ifne(int offset);
    protected abstract void iflt(int offset);
    protected abstract void ifge(int offset);
    protected abstract void ifgt(int offset);
    protected abstract void ifle(int offset);
    protected abstract void if_icmpeq(int offset);
    protected abstract void if_icmpne(int offset);
    protected abstract void if_icmplt(int offset);
    protected abstract void if_icmpge(int offset);
    protected abstract void if_icmpgt(int offset);
    protected abstract void if_icmple(int offset);
    protected abstract void if_acmpeq(int offset);
    protected abstract void if_acmpne(int offset);
    protected abstract void goto_(int offset);
    protected abstract void jsr(int offset);
    protected abstract void ret(int index);
    protected abstract void tableswitch(int defaultOffset, int lowMatch, int highMatch, int numberOfCases);
    protected abstract void lookupswitch(int defaultOffset, int numberOfCases);
    protected abstract void ireturn();
    protected abstract void lreturn();
    protected abstract void freturn();
    protected abstract void dreturn();
    protected abstract void areturn();
    protected abstract void vreturn();
    protected abstract void getstatic(int index);
    protected abstract void putstatic(int index);
    protected abstract void getfield(int index);
    protected abstract void putfield(int index);
    protected abstract void invokevirtual(int index);
    protected abstract void invokespecial(int index);
    protected abstract void invokestatic(int index);
    protected abstract void invokeinterface(int index, int count);
    protected abstract void new_(int index);
    protected abstract void newarray(int tag);
    protected abstract void anewarray(int index);
    protected abstract void arraylength();
    protected abstract void athrow();
    protected abstract void checkcast(int index);
    protected abstract void instanceof_(int index);
    protected abstract void monitorenter();
    protected abstract void monitorexit();
    protected abstract void wide();
    protected abstract void multianewarray(int index, int nDimensions);
    protected abstract void ifnull(int offset);
    protected abstract void ifnonnull(int offset);
    protected abstract void goto_w(int offset);
    protected abstract void jsr_w(int offset);
    protected abstract void breakpoint();

    /**
     * @see Bytecodes#JNICALL
     */
    protected abstract void jnicall(int nativeFunctionDescriptorIndex);

    /**
     * Parses an {@linkplain Bytecodes#isStandard(int) extended} bytecode instruction.
     *
     * @param opcode the opcode of the extended bytecode instruction
     * @param isWide specifies if the WIDE prefix was parsed before the opcode
     * @return {@code true} if this method ensures that the complete instruction has been parsed from the stream;
     *         otherwise the caller is responsible for skipping any unparsed bytes of the instruction.
     */
    protected boolean extension(int opcode, boolean isWide) {
        int length = Bytecodes.lengthOf(opcode);
        assert length != 0;
        bytecodeScanner.skipBytes(length - 1);
        return true;
    }

    protected void unknown(int opcode) {
        throw verifyError("Unsupported bytecode: " + opcode);
    }
}
