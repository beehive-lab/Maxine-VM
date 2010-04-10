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
package com.sun.cri.bytecode;

/**
 * A utility class that makes iterating over bytecodes and reading operands
 * simpler and less error prone. For example, it handles the {@link Bytecodes#WIDE} instruction
 * and wide variants of instructions internally.
 *
 * @author Ben L. Titzer
 */
public class BytecodeStream {

    final byte[] code;
    int opcode;
    int curBCI;
    int nextBCI;

    /**
     * Creates a new {@code BytecodeStream} for the specified bytecode.
     * @param code the array of bytes that contains the bytecode
     */
    public BytecodeStream(byte[] code) {
        this.code = code;
        setBCI(0);
    }

    /**
     * Advances to the next bytecode.
     */
    public void next() {
        setBCI(nextBCI);
    }

    /**
     * Gets the next bytecode index (no side-effects).
     * @return the next bytecode index
     */
    public int nextBCI() {
        return nextBCI;
    }

    /**
     * Gets the current bytecode index.
     * @return the current bytecode index
     */
    public int currentBCI() {
        return curBCI;
    }

    /**
     * Gets the bytecode index of the end of the code.
     * @return the index of the end of the code
     */
    public int endBCI() {
        return code.length;
    }

    /**
     * Gets the current opcode. This method will never return the
     * {@link Bytecodes#WIDE WIDE} opcode, but will instead
     * return the opcode that is modified by the {@code WIDE} opcode.
     * @return the current opcode; {@link Bytecodes#END} if at or beyond the end of the code
     */
    public int currentBC() {
        if (opcode == Bytecodes.WIDE) {
            return Bytes.beU1(code, curBCI + 1);
        } else {
            return opcode;
        }
    }

    /**
     * Reads the index of a local variable for one of the load or store instructions.
     * The WIDE modifier is handled internally.
     * @return the index of the local variable
     */
    public int readLocalIndex() {
        // read local variable index for load/store
        if (opcode == Bytecodes.WIDE) {
            return Bytes.beU2(code, curBCI + 2);
        }
        return Bytes.beU1(code, curBCI + 1);
    }

    /**
     * Read the delta for an {@link Bytecodes#IINC} bytecode.
     * @return the delta for the {@code IINC}
     */
    public int readIncrement() {
        // read the delta for the iinc bytecode
        if (opcode == Bytecodes.WIDE) {
            return Bytes.beS2(code, curBCI + 4);
        }
        return Bytes.beS1(code, curBCI + 2);
    }

    /**
     * Read the destination of a {@link Bytecodes#GOTO} or {@code IF} instructions.
     * @return the destination bytecode index
     */
    public int readBranchDest() {
        // reads the destination for a branch bytecode
        return curBCI + Bytes.beS2(code, curBCI + 1);
    }

    /**
     * Read the destination of a {@link Bytecodes#GOTO_W} or {@link Bytecodes#JSR_W} instructions.
     * @return the destination bytecode index
     */
    public int readFarBranchDest() {
        // reads the destination for a wide branch bytecode
        return curBCI + Bytes.beS4(code, curBCI + 2);
    }

    /**
     * Read a signed 4-byte integer from the bytecode stream at the specified bytecode index.
     * @param bci the bytecode index
     * @return the integer value
     */
    public int readInt(int bci) {
        // reads a 4-byte signed value
        return Bytes.beS4(code, bci);
    }

    /**
     * Reads an unsigned, 1-byte value from the bytecode stream at the specified bytecode index.
     * @param bci the bytecode index
     * @return the byte
     */
    public int readUByte(int bci) {
        return Bytes.beU1(code, bci);
    }

    /**
     * Reads a constant pool index for the current instruction.
     * @return the constant pool index
     */
    public char readCPI() {
        if (opcode == Bytecodes.LDC) {
            return (char) Bytes.beU1(code, curBCI + 1);
        }
        return (char) Bytes.beU2(code, curBCI + 1);
    }

    /**
     * Reads a signed, 1-byte value for the current instruction (e.g. BIPUSH).
     * @return the byte
     */
    public byte readByte() {
        return code[curBCI + 1];
    }

    /**
     * Reads a signed, 2-byte short for the current instruction (e.g. SIPUSH).
     * @return the short value
     */
    public short readShort() {
        return (short) Bytes.beS2(code, curBCI + 1);
    }

    /**
     * Sets the bytecode index to the specified value.
     * If {@code bci} is beyond the end of the array, {@link #currentBC} will return
     * {@link Bytecodes#END} and other methods may throw {@link ArrayIndexOutOfBoundsException}.
     * @param bci the new bytecode index
     */
    public void setBCI(int bci) {
        curBCI = bci;
        if (curBCI < code.length) {
            opcode = Bytes.beU1(code, bci);
            nextBCI = bci + Bytecodes.lengthOf(code, bci);
        } else {
            opcode = Bytecodes.END;
            nextBCI = curBCI;
        }
    }
}
