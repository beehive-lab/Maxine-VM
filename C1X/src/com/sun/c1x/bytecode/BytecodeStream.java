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
package com.sun.c1x.bytecode;

import com.sun.c1x.util.Bytes;

/**
 * The <code>CiBytecodeStream</code> class implements a utility that makes iterating over
 * bytecodes and reading operands simpler and less error prone. For example, it handles
 * the WIDE instruction and wide variants of instructions internally.
 *
 * @author Ben L. Titzer
 */
public class BytecodeStream {

    final byte[] _code;
    int _opcode;
    int _curBCI;
    int _nextBCI;

    /**
     * Creates a new <code>BytecodeStream</code> for the specified bytecode
     * @param code the array of bytes that contains the bytecode
     */
    public BytecodeStream(byte[] code) {
        _code = code;
        setBCI(0);
    }

    /**
     * Advances to the next bytecode and returns its opcode.
     * @return the opcode of the current bytecode
     */
    public int next() {
        final int opcode = currentBC();
        setBCI(_nextBCI);
		return opcode;
    }

    /**
     * Gets the next bytecode index (no side-effects).
     * @return the next bytecode index
     */
    public int nextBCI() {
        return _nextBCI;
    }

    /**
     * Gets the current bytecode index.
     * @return the current bytecode index
     */
    public int currentBCI() {
        return _curBCI;
    }

    /**
     * Gets the current opcode. This method will never return the
     * {@link com.sun.c1x.bytecode.Bytecodes#WIDE WIDE} opcode, but will instead
     * return the opcode that is modified by the WIDE opcode.
     * @return the current opcode; {@link Bytecodes#END} if at or beyond the end of the code
     */
    public int currentBC() {
        if (_opcode == Bytecodes.WIDE) {
            return Bytes.beU1(_code, _curBCI + 1);
        } else {
            return _opcode;
        }
    }

    /**
     * Reads the index of a local variable for one of the load or store instructions.
     * The WIDE modifier is handled internally.
     * @return the index of the local variable
     */
    public int readLocalIndex() {
        // read local variable index for load/store
        if (_opcode == Bytecodes.WIDE) {
            return Bytes.beU2(_code, _curBCI + 2);
        }
        return Bytes.beU1(_code, _curBCI + 1);
    }

    /**
     * Read the delta for an IINC bytecode.
     * @return the delta for the IINC
     */
    public int readIncrement() {
        // read the delta for the iinc bytecode
        if (_opcode == Bytecodes.WIDE) {
            return Bytes.beS2(_code, _curBCI + 4);
        }
        return Bytes.beS1(_code, _curBCI + 2);
    }

    /**
     * Read the destination of a GOTO or IF instructions.
     * @return the destination bytecode index
     */
    public int readBranchDest() {
        // reads the destination for a branch bytecode
        return _curBCI + Bytes.beS2(_code, _curBCI + 1);
    }

    /**
     * Read the destination of a GOTO_W or JSR_W instructions.
     * @return the destination bytecode index
     */
    public int readFarBranchDest() {
        // reads the destination for a wide branch bytecode
        return _curBCI + Bytes.beS4(_code, _curBCI + 2);
    }

    /**
     * Read a signed 4-byte integer from the bytecode stream at the specified bytecode index.
     * @param bci the bytecode index
     * @return the integer value
     */
    public int readInt(int bci) {
        // reads a 4-byte signed value
        return Bytes.beS4(_code, bci);
    }

    /**
     * Reads an unsigned, 1-byte value from the bytecode stream at the specified bytecode index.
     * @param bci the bytecode index
     * @return the byte
     */
    public int readUByte(int bci) {
        // reads a 1-byte unsigned value
        return Bytes.beU1(_code, _curBCI + 1);
    }

    /**
     * Reads a constant pool index for the current instruction.
     * @return the constant pool index
     */
    public char readCPI() {
        if (_opcode == Bytecodes.LDC_W || _opcode == Bytecodes.LDC2_W) {
            return (char) Bytes.beU2(_code, _curBCI + 1);
        }
        return (char) Bytes.beU1(_code, _curBCI + 1);
    }

    /**
     * Reads a signed, 1-byte value for the current instruction (e.g. BIPUSH).
     * @return the byte
     */
    public byte readByte() {
        return _code[_curBCI + 1];
    }

    /**
     * Reads a signed, 2-byte short for the current instruction (e.g. SIPUSH).
     * @return the short value
     */
    public short readShort() {
        return (short) Bytes.beS2(_code, _curBCI + 1);
    }

    /**
     * Sets the bytecode index to the specified value.
     * @param bci the new bytecode index
     */
    public void setBCI(int bci) {
        _curBCI = bci;
        if (_curBCI < _code.length) {
            _opcode = Bytes.beU1(_code, bci);
            _nextBCI = bci + Bytecodes.length(_code, bci);
        } else {
            _opcode = Bytecodes.END;
            _nextBCI = _curBCI;
        }
    }
    
    public boolean hasMore() {
    	return _opcode != Bytecodes.END;
    }
}
