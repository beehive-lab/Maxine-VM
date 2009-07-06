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


/**
 * The <code>LIRAllocObj</code> class definition.
 *
 * @author Marcelo Cintra
 *
 */
public class LIRAllocObj extends LIROp1 {

    private LIROperand tmp1;
    private LIROperand tmp2;
    private LIROperand tmp3;
    private LIROperand tmp4;
    private int headerSize;
    private int objectSize;
    private CodeStub stub;
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
                    int headerSize, int objSize, CodeStub stub, boolean initCheck) {
        super(LIROpcode.AllocObject, klass, result);
        this.tmp1 = tmp1;
        this.tmp2 = tmp2;
        this.tmp3 = tmp3;
        this.tmp4 = tmp4;
        this.headerSize = headerSize;
        this.objectSize = objSize;
        this.stub = stub;
        this.initCheck = initCheck;
    }

    /**
     * @return the tmp1
     */
    public LIROperand tmp1() {
        return tmp1;
    }

    /**
     * @return the tmp2
     */
    public LIROperand tmp2() {
        return tmp2;
    }

    /**
     * @return the tmp3
     */
    public LIROperand tmp3() {
        return tmp3;
    }

    /**
     * @return the tmp4
     */
    public LIROperand tmp4() {
        return tmp4;
    }

    /**
     * @return the hdrSize
     */
    public int headerSize() {
        return headerSize;
    }

    /**
     * @return the objSize
     */
    public int obectSize() {
        return objectSize;
    }

    /**
     * @return the stub
     */
    public CodeStub stub() {
        return stub;
    }

    /**
     * @return the initCheck
     */
    public boolean isInitCheck() {
        return initCheck;
    }
}
