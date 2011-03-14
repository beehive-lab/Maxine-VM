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
package test.com.sun.max.vm.cps.bytecode.create;

/**
 * A {@code MillCode} assembles the byte codes of one Java(TM) method.
 * This class contains what can be seen as templates for byte code assembly
 * language statements.
 *
 * @see MillClass
 *
 * @author Bernd Mathiske
 * @version 1.0
 */
public class MillCode {

    final int numberOfLocals;
    final int numberOfMaxStackWords;

    /**
     * Create a new byte code sequence and set basic stack information.
     *
     * @param nParameters
     *            The number of parameters of the method to which this byte code
     *            will belong.
     * @param nLocals
     *            The number of locals that this byte code allocates on the
     *            stack.
     * @param nMaxStackWords
     *            The number of words on the operand stack that this byte code
     *            will (hopefully) never exceed.
     */
    public MillCode(int nLocals, int nMaxStackWords) {
        this.numberOfLocals = nLocals;
        this.numberOfMaxStackWords = nMaxStackWords;
    }

    private Operation operation = null;
    private int n = 0;

    /**
     * Determine the accumulated number of bytes in this code when generated.
     *
     * @return The accumulated number of bytes in this code when generated.
     */
    public int nBytes() {
        return n;
    }

    private void append(int code) {
        operation = new Operation((byte) code, operation);
        n++;
    }

    private void appendRef(int code, MillConstant constant) {
        operation = new Operation((byte) code, constant, operation);
        n += 3;
    }

    void assemble(byte[] b, int offset) {
        int index = offset + n;
        while (operation != null) {
            if (operation.constant != null) {
                b[--index] = MillWord.byte0(operation.constant.index);
                b[--index] = MillWord.byte1(operation.constant.index);
            }
            b[--index] = operation.code;
            operation = operation.next;
        }
    }

    /**
     * Get the stack index of a local variable.
     *
     * @param The
     *            logical index among locals of the local we are interested in.
     * @return The stack index of the local.
     */
    public byte local(int i) {
        if (i > numberOfLocals) {
            throw new IllegalArgumentException();
        }
        return (byte) i;
    }

    /**
     * Append a nop byte code.
     */
    public void nop() {
        append(0x00);
    }

    /**
     * Append an iconst byte code.
     *
     * @param x
     *            The operand of the iconst byte code.
     */
    public void iconst(int x) {
        if (-1 <= x && x <= 5) {
            append(0x03 + x);
        } else {
            bipush(x);
        }
    }

    /**
     * Append a bipush byte code.
     *
     * @param x
     *            The operand of the bipush byte code.
     */
    public void bipush(int x) {
        append(0x10);
        append(x);
    }

    /**
     * Append an ldc_w byte code.
     *
     * @param _x
     *            The operand of the bipush byte code.
     */
    public void ldc_w(MillIntConstant intConstant) {
        appendRef(0x13, intConstant);
    }

    public void iload(int index) {
        if (index <= 3) {
            append(0x1a + index);
        } else {
            append(0x15);
            append(index);
        }
    }

    public void wide_iload(int index) {
        append(0xc4);
        append(0x15);
        append(MillWord.byte1(index));
        append(MillWord.byte0(index));
    }

    public void lload(int i) {
        if (i <= 3) {
            append(0x1e + i);
        } else {
            append(0x16);
            append(i);
        }
    }

    public void wide_lload(int index) {
        append(0xc4);
        append(0x16);
        append(MillWord.byte1(index));
        append(MillWord.byte0(index));
    }

    public void fload(int i) {
        if (i <= 3) {
            append(0x22 + i);
        } else {
            append(0x17);
            append(i);
        }
    }

    public void wide_fload(int index) {
        append(0xc4);
        append(0x17);
        append(MillWord.byte1(index));
        append(MillWord.byte0(index));
    }

    public void dload(int i) {
        if (i <= 3) {
            append(0x26 + i);
        } else {
            append(0x18);
            append(i);
        }
    }

    public void wide_dload(int index) {
        append(0xc4);
        append(0x18);
        append(MillWord.byte1(index));
        append(MillWord.byte0(index));
    }

    public void aload(int i) {
        if (i <= 3) {
            append(0x2a + i);
        } else {
            append(0x19);
            append(i);
        }
    }

    public void wide_aload(int index) {
        append(0xc4);
        append(0x19);
        append(MillWord.byte1(index));
        append(MillWord.byte0(index));
    }

    public void istore(int i) {
        if (i <= 3) {
            append(0x3b + i);
        } else {
            append(0x36);
            append(i);
        }
    }

    public void wide_istore(int index) {
        append(0xc4);
        append(0x36);
        append(MillWord.byte1(index));
        append(MillWord.byte0(index));
    }

    public void lstore(int i) {
        if (i <= 3) {
            append(0x3f + i);
        } else {
            append(0x37);
            append(i);
        }
    }

    public void wide_lstore(int index) {
        append(0xc4);
        append(0x37);
        append(MillWord.byte1(index));
        append(MillWord.byte0(index));
    }

    public void wide_fstore(int index) {
        append(0xc4);
        append(0x38);
        append(MillWord.byte1(index));
        append(MillWord.byte0(index));
    }

    public void dstore(int i) {
        if (i <= 3) {
            append(0x47 + i);
        } else {
            append(0x39);
            append(i);
        }
    }

    public void wide_dstore(int index) {
        append(0xc4);
        append(0x39);
        append(MillWord.byte1(index));
        append(MillWord.byte0(index));
    }

    public void astore(int i) {
        if (i <= 3) {
            append(0x4b + i);
        } else {
            append(0x3a);
            append(i);
        }
    }

    public void wide_astore(int index) {
        append(0xc4);
        append(0x3a);
        append(MillWord.byte1(index));
        append(MillWord.byte0(index));
    }

    /**
     * Append an aastore byte code.
     */
    public void aastore() {
        append(0x53);
    }

    /**
     * Append a pop byte code.
     */
    public void pop() {
        append(0x57);
    }

    public void pop2() {
        append(0x58);
    }

    public void dup() {
        append(0x59);
    }

    public void dup_x1() {
        append(0x5a);
    }

    public void dup_x2() {
        append(0x5b);
    }

    public void dup2() {
        append(0x5c);
    }

    public void dup2_x1() {
        append(0x5d);
    }

    public void dup2_x2() {
        append(0x5e);
    }

    public void swap() {
        append(0x5f);
    }

    public void iadd() {
        append(0x60);
    }

    public void isub() {
        append(0x64);
    }

    public void lsub() {
        append(0x65);
    }

    public void fsub() {
        append(0x66);
    }

    public void dsub() {
        append(0x67);
    }

    public void imul() {
        append(0x68);
    }

    public void lmul() {
        append(0x69);
    }

    public void dmul() {
        append(0x6b);
    }

    public void idiv() {
        append(0x6c);
    }

    public void ldiv() {
        append(0x6d);
    }

    public void wide_iinc(int index, int addend) {
        append(0xc4);
        append(0x84);
        append(MillWord.byte1(index));
        append(MillWord.byte0(index));
        append(MillWord.byte1(addend));
        append(MillWord.byte0(addend));
    }

    public void i2l() {
        append(0x85);
    }

    public void f2d() {
        append(0x8d);
    }

    public void lcmp() {
        append(0x94);
    }

    public void fcmpl() {
        append(0x95);
    }

    public void fcmpg() {
        append(0x96);
    }

    public void dcmpl() {
        append(0x97);
    }

    public void dcmpg() {
        append(0x98);
    }

    public void goto_(int offset) {
        final short shortOffset = (short) offset;
        assert shortOffset == offset;
        append(0xa7);
        append(MillWord.byte1(offset));
        append(MillWord.byte0(offset));
    }

    public void goto_w(int offset) {
        append(0xc8);
        append(MillWord.byte3(offset));
        append(MillWord.byte2(offset));
        append(MillWord.byte1(offset));
        append(MillWord.byte0(offset));
    }

    /**
     * Append an areturn byte code.
     */
    public void areturn() {
        append(0xb0);
    }

    /**
     * Append a return byte code. This code is slightly renamed here to avoid
     * conflict with the {@code return} keyword in the Java(TM)
     * Programming Language.
     */
    public void vreturn() { // 'return'
        append(0xb1);
    }

    /**
     * Append an ireturn byte code.
     */
    public void ireturn() {
        append(0xac);
    }

    /**
     * Append an lreturn byte code.
     */
    public void lreturn() {
        append(0xad);
    }

    /**
     * Append an freturn byte code.
     */
    public void freturn() {
        append(0xae);
    }

    /**
     * Append a dreturn byte code.
     */
    public void dreturn() {
        append(0xaf);
    }

    /**
     * Append a getfield byte code.
     *
     * @param fieldRef
     *            The operand of the getfield byte code: a field reference
     *            constant.
     *
     * @see MillFieldRefConstant
     * @see MillClass
     */
    public void getfield(MillFieldRefConstant fieldRef) {
        appendRef(0xb4, fieldRef);
    }

    /**
     * Append a putfield byte code.
     *
     * @param fieldRef
     *            The operand of the putfield byte code: a field reference
     *            constant.
     *
     * @see MillFieldRefConstant
     * @see MillClass
     */
    public void putfield(MillFieldRefConstant fieldRef) {
        appendRef(0xb5, fieldRef);
    }

    /**
     * Append an invokespecial byte code.
     *
     * @param methodRef
     *            The operand of the invokespecial byte code: a method reference
     *            constant.
     *
     * @see MillMethodRefConstant
     * @see MillClass
     */
    public void invokespecial(MillMethodRefConstant methodRef) {
        appendRef(0xb7, methodRef);
    }

    /**
     * Append an invokestatic byte code.
     *
     * @param methodRef
     *            The operand of the invokestatic byte code: a method reference
     *            constant.
     *
     * @see MillMethodRefConstant
     * @see MillClass
     */
    public void invokestatic(MillMethodRefConstant methodRef) {
        appendRef(0xb8, methodRef);
    }

    public void monitorenter() {
        append(0xc2);
    }

    public void monitorexit() {
        append(0xc3);
    }

    /**
     * Append a {@code new} byte code. This code is slightly renamed here
     * to avoid conflict with the {@code new} keyword in the Java(TM)
     * Programming Language.
     *
     * @param classRef
     *            The operand of the newObject byte code: a class reference
     *            constant.
     *
     * @see MillClassConstant
     * @see MillClass
     */
    public void newObject(MillClassConstant classRef) {
        appendRef(0xbb, classRef);
    }

    /**
     * Append an anewarray byte code.
     *
     * @param classRef
     *            The operand of the anewarray byte code: a class reference
     *            constant.
     *
     * @see MillClassConstant
     * @see MillClass
     */
    public void anewarray(MillClassConstant classRef) {
        appendRef(0xbd, classRef);
    }

    /**
     * Append an athrow byte code.
     */
    public void athrow() {
        append(0xbf);
    }

    /**
     * Append a checkcast byte code.
     *
     * @param classRef The operand of the checkcast byte code:
     *                 a class reference constant.
     *
     * @see MillClassConstant
     * @see MillClass
     */
    public void checkcast(MillClassConstant classRef) {
        appendRef(0xc0, classRef);
    }

}

class Operation {

    final byte code;
    final MillConstant constant;
    final Operation next;

    Operation(byte code, Operation next) {
        this.code = code;
        this.constant = null;
        this.next = next;
    }

    Operation(byte code, MillConstant constant, Operation next) {
        this.code = code;
        this.constant = constant;
        this.next = next;
    }

}
