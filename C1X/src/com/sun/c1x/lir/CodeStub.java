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

import com.sun.c1x.asm.*;
import com.sun.c1x.util.*;


/**
 * The <code>CodeStub</code> class definition.
 *
 * @author Marcelo Cintra
 *
 */
public abstract class CodeStub {

    protected Label entry;                                  // label at the stub entry point
    protected Label continuation;

    /**
     * Returns the entry for this Code stub.
     *
     * @return the entry
     */
    public Label entry() {
        return entry;
    }

    /**
     * Returns the continuation for this code stub.
     *
     * @return the continuation
     */
    public Label getContinuation() {
        return continuation;
    }

    /**
     * Asserts that the code stub has bounded labels.
     */
    public void assertNoUnboundLabels() {
        assert !entry.isUnbound() && !continuation.isUnbound() : "Code stub has an unbound label";
    }

    public abstract void emitCode(LIRAssembler e);

    public abstract void printName(LogStream out);

    public void visit(LIRVisitState v) {

    }

    /**
     * Checks if this is an exception throw code stub.
     *
     * @return false
     */
    public boolean isExeptionThrow() {
        return false;
    }

    /**
     * Checks if this is a range check code stub.
     *
     * @return false
     */
    public boolean isRangeCheck() {
        return false;
    }

    /**
     * Checks if this is a divide by zero code stub.
     *
     * @return false
     */
    public boolean isDivideByZero() {
        return false;
    }

    public CodeEmitInfo info() {
        // TODO Auto-generated method stub
        return null;
    }


}
