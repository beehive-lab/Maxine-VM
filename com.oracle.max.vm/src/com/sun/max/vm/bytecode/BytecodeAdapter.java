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

/**
 * An adapter class that exists as a convenience for implementing a {@linkplain BytecodeVisitor bytecode visitor} that
 * only needs to process a subset of JVM instructions in a non-default way. The body of each method defined this class
 * simply returns.
 */
public class BytecodeAdapter extends BytecodeVisitor {

    public BytecodeAdapter() {
        super();
    }

    @Override
    protected void nop() {
    }

    @Override
    protected void aconst_null() {
    }

    @Override
    protected void iconst_m1() {
    }

    @Override
    protected void iconst_0() {
    }

    @Override
    protected void iconst_1() {
    }

    @Override
    protected void iconst_2() {
    }

    @Override
    protected void iconst_3() {
    }

    @Override
    protected void iconst_4() {
    }

    @Override
    protected void iconst_5() {
    }

    @Override
    protected void lconst_0() {
    }

    @Override
    protected void lconst_1() {
    }

    @Override
    protected void fconst_0() {
    }

    @Override
    protected void fconst_1() {
    }

    @Override
    protected void fconst_2() {
    }

    @Override
    protected void dconst_0() {
    }

    @Override
    protected void dconst_1() {
    }

    @Override
    protected void bipush(int operand) {
    }

    @Override
    protected void sipush(int operand) {
    }

    @Override
    protected void ldc(int index) {
    }

    @Override
    protected void ldc_w(int index) {
    }

    @Override
    protected void ldc2_w(int index) {
    }

    @Override
    protected void iload(int index) {
    }

    @Override
    protected void lload(int index) {
    }

    @Override
    protected void fload(int index) {
    }

    @Override
    protected void dload(int index) {
    }

    @Override
    protected void aload(int index) {
    }

    @Override
    protected void iload_0() {
    }

    @Override
    protected void iload_1() {
    }

    @Override
    protected void iload_2() {
    }

    @Override
    protected void iload_3() {
    }

    @Override
    protected void lload_0() {
    }

    @Override
    protected void lload_1() {
    }

    @Override
    protected void lload_2() {
    }

    @Override
    protected void lload_3() {
    }

    @Override
    protected void fload_0() {
    }

    @Override
    protected void fload_1() {
    }

    @Override
    protected void fload_2() {
    }

    @Override
    protected void fload_3() {
    }

    @Override
    protected void dload_0() {
    }

    @Override
    protected void dload_1() {
    }

    @Override
    protected void dload_2() {
    }

    @Override
    protected void dload_3() {
    }

    @Override
    protected void aload_0() {
    }

    @Override
    protected void aload_1() {
    }

    @Override
    protected void aload_2() {
    }

    @Override
    protected void aload_3() {
    }

    @Override
    protected void iaload() {
    }

    @Override
    protected void laload() {
    }

    @Override
    protected void faload() {
    }

    @Override
    protected void daload() {
    }

    @Override
    protected void aaload() {
    }

    @Override
    protected void baload() {
    }

    @Override
    protected void caload() {
    }

    @Override
    protected void saload() {
    }

    @Override
    protected void istore(int index) {
    }

    @Override
    protected void lstore(int index) {
    }

    @Override
    protected void fstore(int index) {
    }

    @Override
    protected void dstore(int index) {
    }

    @Override
    protected void astore(int index) {
    }

    @Override
    protected void istore_0() {
    }

    @Override
    protected void istore_1() {
    }

    @Override
    protected void istore_2() {
    }

    @Override
    protected void istore_3() {
    }

    @Override
    protected void lstore_0() {
    }

    @Override
    protected void lstore_1() {
    }

    @Override
    protected void lstore_2() {
    }

    @Override
    protected void lstore_3() {
    }

    @Override
    protected void fstore_0() {
    }

    @Override
    protected void fstore_1() {
    }

    @Override
    protected void fstore_2() {
    }

    @Override
    protected void fstore_3() {
    }

    @Override
    protected void dstore_0() {
    }

    @Override
    protected void dstore_1() {
    }

    @Override
    protected void dstore_2() {
    }

    @Override
    protected void dstore_3() {
    }

    @Override
    protected void astore_0() {
    }

    @Override
    protected void astore_1() {
    }

    @Override
    protected void astore_2() {
    }

    @Override
    protected void astore_3() {
    }

    @Override
    protected void iastore() {
    }

    @Override
    protected void lastore() {
    }

    @Override
    protected void fastore() {
    }

    @Override
    protected void dastore() {
    }

    @Override
    protected void aastore() {
    }

    @Override
    protected void bastore() {
    }

    @Override
    protected void castore() {
    }

    @Override
    protected void sastore() {
    }

    @Override
    protected void pop() {
    }

    @Override
    protected void pop2() {
    }

    @Override
    protected void dup() {
    }

    @Override
    protected void dup_x1() {
    }

    @Override
    protected void dup_x2() {
    }

    @Override
    protected void dup2() {
    }

    @Override
    protected void dup2_x1() {
    }

    @Override
    protected void dup2_x2() {
    }

    @Override
    protected void swap() {
    }

    @Override
    protected void iadd() {
    }

    @Override
    protected void ladd() {
    }

    @Override
    protected void fadd() {
    }

    @Override
    protected void dadd() {
    }

    @Override
    protected void isub() {
    }

    @Override
    protected void lsub() {
    }

    @Override
    protected void fsub() {
    }

    @Override
    protected void dsub() {
    }

    @Override
    protected void imul() {
    }

    @Override
    protected void lmul() {
    }

    @Override
    protected void fmul() {
    }

    @Override
    protected void dmul() {
    }

    @Override
    protected void idiv() {
    }

    @Override
    protected void ldiv() {
    }

    @Override
    protected void fdiv() {
    }

    @Override
    protected void ddiv() {
    }

    @Override
    protected void irem() {
    }

    @Override
    protected void lrem() {
    }

    @Override
    protected void frem() {
    }

    @Override
    protected void drem() {
    }

    @Override
    protected void ineg() {
    }

    @Override
    protected void lneg() {
    }

    @Override
    protected void fneg() {
    }

    @Override
    protected void dneg() {
    }

    @Override
    protected void ishl() {
    }

    @Override
    protected void lshl() {
    }

    @Override
    protected void ishr() {
    }

    @Override
    protected void lshr() {
    }

    @Override
    protected void iushr() {
    }

    @Override
    protected void lushr() {
    }

    @Override
    protected void iand() {
    }

    @Override
    protected void land() {
    }

    @Override
    protected void ior() {
    }

    @Override
    protected void lor() {
    }

    @Override
    protected void ixor() {
    }

    @Override
    protected void lxor() {
    }

    @Override
    protected void iinc(int index, int addend) {
    }

    @Override
    protected void i2l() {
    }

    @Override
    protected void i2f() {
    }

    @Override
    protected void i2d() {
    }

    @Override
    protected void l2i() {
    }

    @Override
    protected void l2f() {
    }

    @Override
    protected void l2d() {
    }

    @Override
    protected void f2i() {
    }

    @Override
    protected void f2l() {
    }

    @Override
    protected void f2d() {
    }

    @Override
    protected void d2i() {
    }

    @Override
    protected void d2l() {
    }

    @Override
    protected void d2f() {
    }

    @Override
    protected void i2b() {
    }

    @Override
    protected void i2c() {
    }

    @Override
    protected void i2s() {
    }

    @Override
    protected void lcmp() {
    }

    @Override
    protected void fcmpl() {
    }

    @Override
    protected void fcmpg() {
    }

    @Override
    protected void dcmpl() {
    }

    @Override
    protected void dcmpg() {
    }

    @Override
    protected void ifeq(int offset) {
    }

    @Override
    protected void ifne(int offset) {
    }

    @Override
    protected void iflt(int offset) {
    }

    @Override
    protected void ifge(int offset) {
    }

    @Override
    protected void ifgt(int offset) {
    }

    @Override
    protected void ifle(int offset) {
    }

    @Override
    protected void if_icmpeq(int offset) {
    }

    @Override
    protected void if_icmpne(int offset) {
    }

    @Override
    protected void if_icmplt(int offset) {
    }

    @Override
    protected void if_icmpge(int offset) {
    }

    @Override
    protected void if_icmpgt(int offset) {
    }

    @Override
    protected void if_icmple(int offset) {
    }

    @Override
    protected void if_acmpeq(int offset) {
    }

    @Override
    protected void if_acmpne(int offset) {
    }

    @Override
    protected void goto_(int offset) {
    }

    @Override
    protected void jsr(int offset) {
    }

    @Override
    protected void ret(int index) {
    }

    @Override
    protected void tableswitch(int defaultOffset, int lowMatch, int highMatch, int numberOfCases) {
        bytecodeScanner().skipBytes(numberOfCases * 4);
    }

    @Override
    protected void lookupswitch(int defaultOffset, int numberOfCases) {
        bytecodeScanner().skipBytes(numberOfCases * 8);
    }

    @Override
    protected void ireturn() {
    }

    @Override
    protected void lreturn() {
    }

    @Override
    protected void freturn() {
    }

    @Override
    protected void dreturn() {
    }

    @Override
    protected void areturn() {
    }

    @Override
    protected void vreturn() {
    }

    @Override
    protected void getstatic(int index) {
    }

    @Override
    protected void putstatic(int index) {
    }

    @Override
    protected void getfield(int index) {
    }

    @Override
    protected void putfield(int index) {
    }

    @Override
    protected void invokevirtual(int index) {
    }

    @Override
    protected void invokespecial(int index) {
    }

    @Override
    protected void invokestatic(int index) {
    }

    @Override
    protected void invokeinterface(int index, int count) {
    }

    @Override
    protected void new_(int index) {
    }

    @Override
    protected void newarray(int tag) {
    }

    @Override
    protected void anewarray(int index) {
    }

    @Override
    protected void arraylength() {
    }

    @Override
    protected void athrow() {
    }

    @Override
    protected void checkcast(int index) {
    }

    @Override
    protected void instanceof_(int index) {
    }

    @Override
    protected void monitorenter() {
    }

    @Override
    protected void monitorexit() {
    }

    @Override
    protected void wide() {
    }

    @Override
    protected void multianewarray(int index, int nDimensions) {
    }

    @Override
    protected void ifnull(int offset) {
    }

    @Override
    protected void ifnonnull(int offset) {
    }

    @Override
    protected void goto_w(int offset) {
    }

    @Override
    protected void jsr_w(int offset) {
    }

    @Override
    protected void breakpoint() {
    }

    @Override
    protected void jnicall(int nativeFunctionDescriptorIndex) {
    }

}
