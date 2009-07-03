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
package com.sun.c1x.lir;

import com.sun.c1x.ci.*;
import com.sun.c1x.util.*;
import com.sun.c1x.ci.CiType;

/**
 * The <code>LIRArrayCopy</code> class definition.
 *
 * @author Marcelo Cintra
 *
 */
public class LIRArrayCopy extends LIRInstruction {

    private ArrayCopyStub stub;
    private LIROperand src;
    private LIROperand srcPos;
    private LIROperand dst;
    private LIROperand dstPos;
    private LIROperand length;
    private LIROperand tmp;
    private CiType expectedType;
    private int arrayCopyFlags;

    public enum Flags {
        SrcNullCheck,
        DstNullCheck,
        SrcPosPositiveCheck,
        DstPositiveCheck,
        LengthPositiveCheck,
        SrcRangeCheck,
        DstRangeCheck,
        TypeCheck,
        AllFlags;

        public final int mask() {
            return 1 << ordinal();
        }
    }

    /**
     * Creates a new LIRArrayCopy instruction.
     *
     * @param src
     * @param srcPos
     * @param dst
     * @param dstPos
     * @param length
     * @param tmp
     */
    public LIRArrayCopy(LIROperand src, LIROperand srcPos, LIROperand dst, LIROperand dstPos, LIROperand length, LIROperand tmp, CiType expectedType, int arrayCopyFlags, CodeEmitInfo info) {
        super(LIROpcode.ArrayCopy, LIROperandFactory.illegalOperand, info);
        this.src = src;
        this.srcPos = srcPos;
        this.dst = dst;
        this.dstPos = dstPos;
        this.length = length;
        this.tmp = tmp;
        this.expectedType = expectedType;
        this.arrayCopyFlags = arrayCopyFlags;
        stub = new ArrayCopyStub(this);
    }

    /**
     * Gets the source of this class.
     *
     * @return the source
     */
    public LIROperand src() {
        return src;
    }

    /**
     * Gets the sourcePos of this class.
     *
     * @return the sourcePos
     */
    public LIROperand srcPos() {
        return srcPos;
    }

    /**
     * Gets the dest of this class.
     *
     * @return the dest
     */
    public LIROperand dst() {
        return dst;
    }

    /**
     * Gets the destPos of this class.
     *
     * @return the destPos
     */
    public LIROperand dstPos() {
        return dstPos;
    }

    /**
     * Gets the length of this class.
     *
     * @return the length
     */
    public LIROperand length() {
        return length;
    }

    /**
     * Gets the tmp of this class.
     *
     * @return the tmp
     */
    public LIROperand tmp() {
        return tmp;
    }

    /**
     * Gets the flags of this class.
     *
     * @return the flags
     */
    public int flags() {
        return arrayCopyFlags;
    }

    /**
     *
     * @return the expected type
     */
    public CiType expectedType() {
        return expectedType;
    }

    /**
     * Gets the stub of this class.
     *
     * @return the stub
     */
    public ArrayCopyStub stub() {
        return stub;
    }

    /**
     * Emits target assembly code for this instruction.
     *
     * @param masm the target assembler
     */
    @Override
    public void emitCode(LIRAssembler masm) {
        masm.emitArrayCopy(this);
    }

     /**
     * Prints this instruction.
     *
     * @param out the output stream
     */
    @Override
    public void printInstruction(LogStream out) {
        src().print(out);
        out.print(" ");
        srcPos().print(out);
        out.print(" ");
        dst().print(out);
        out.print(" ");
        dstPos().print(out);
        out.print(" ");
        length().print(out);
        out.print(" ");
        tmp().print(out);
        out.print(" ");
    }
}
