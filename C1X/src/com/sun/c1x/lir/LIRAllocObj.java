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

import com.sun.c1x.debug.*;
import com.sun.c1x.stub.*;


/**
 * The <code>LIRAllocObj</code> class definition.
 *
 * @author Marcelo Cintra
 *
 */
public class LIRAllocObj extends LIRInstruction {

    private int     hdrSize;
    private int     objSize;
    private boolean initCheck;


    /**
     * Constructs a new LIRAllocObj instruction.
     *
     * @param klass
     * @param result
     * @param tmp1
     * @param tmp2
     * @param tmp3
     * @param tmp4
     * @param headerSize
     * @param objSize
     * @param stub
     * @param initCheck
     */
    public LIRAllocObj(LIROperand klass, LIROperand result, LIROperand tmp1, LIROperand tmp2, LIROperand tmp3, LIROperand tmp4,
                    int hdrSize, int objSize, boolean initCheck, CodeStub stub) {
        super(LIROpcode.AllocObject, result, null, false, stub, 0, 4, klass, tmp1, tmp2, tmp3, tmp4);
        this.hdrSize = hdrSize;
        this.objSize = objSize;
        this.initCheck = initCheck;
    }

    /**
     * @return the operand
     */
    public LIROperand klass() {
        return operand(0);
    }

    public LIROperand obj() {
        return result();
    }
    /**
     * @return the tmp1
     */
    public LIROperand tmp1() {
        return operand(1);
    }

    /**
     * @return the tmp2
     */
    public LIROperand tmp2() {
        return operand(2);
    }

    /**
     * @return the tmp3
     */
    public LIROperand tmp3() {
        return operand(3);
    }

    /**
     * @return the tmp4
     */
    public LIROperand tmp4() {
        return operand(4);
    }

    /**
     * @return the hdrSize
     */
    public int headerSize() {
        return hdrSize;
    }

    /**
     * @return the objSize
     */
    public int obectSize() {
        return objSize;
    }

     /**
     * @return the initCheck
     */
    public boolean isInitCheck() {
        return initCheck;
    }

    /**
     * Emits code for this instruction.
     *
     */
    @Override
    public void emitCode(LIRAssembler masm) {
        masm.emitAllocObj(this);
        masm.emitCodeStub(stub);
    }

     /**
     * Prints this instruction.
     *
     * @param out the outputstream
     */
    @Override
    public void printInstruction(LogStream out) {
        super.printInstruction(out);
        out.printf("[hdr:%d]", headerSize());
        out.print(" ");
        out.printf("[obj:%d]", headerSize());
        out.print(" ");
        out.printf("[lbl:0x%x]", stub().entry);
    }
}
