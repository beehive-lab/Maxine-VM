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
package com.sun.c1x.stub;

import com.sun.c1x.lir.*;


/**
 * The <code>ArrayCopyStub</code> class represents a code stub for array copy.
 *
 * @author Marcelo Cintra
 *
 */
public class ArrayCopyStub extends CodeStub {

    private LIRArrayCopy arrayCopy;

    /**
     * Creates a new ArrayCopyStub.
     *
     * @param arrayCopy the LIR operation representing the array copy
     */
    public ArrayCopyStub(LIRArrayCopy arrayCopy) {
        super(null);
        this.arrayCopy = arrayCopy;
    }

    public LIROperand source() {
        return arrayCopy.src();
    }

    public LIROperand sourcePos() {
        return arrayCopy.srcPos();
    }

    public LIROperand dest() {
        return arrayCopy.dst();
    }

    public LIROperand destPos() {
        return arrayCopy.dstPos();
    }

    public LIROperand length() {
        return arrayCopy.length();
    }

    public LIROperand tmp() {
        return arrayCopy.tmp();
    }

    @Override
    public void accept(CodeStubVisitor visitor) {
        visitor.visitArrayCopyStub(this);
    }

    @Override
    public void visit(LIRVisitState visitor) {
        // don't pass in the code emit info since it's processed in the fast path
        visitor.doSlowCase();
    }
}
