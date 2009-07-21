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

import com.sun.c1x.*;
import com.sun.c1x.asm.*;
import com.sun.c1x.lir.*;
import com.sun.c1x.util.*;


/**
 * The <code>CodeStub</code> class definition. CodeStubs are little 'out-of-line'
 * pieces of code that usually handle slow cases of operations. All code stubs are
 * collected and code is emitted at the end of the method.
 *
 * @author Marcelo Cintra
 *
 */
public abstract class CodeStub {

    protected Label entry;            // label at the stub entry point
    protected Label continuation;     // label where stub continues, if any

    public CodeStub() {

    }

    /**
     * Asserts that the code stub has bounded labels.
     */
    public boolean assertNoUnboundLabels() {
        assert !entry.isUnbound() && !continuation.isUnbound() : "Code stub has an unbound label";
        return true;
    }

    /**
     * Returns the entry label for this code stub.
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
    public Label continuation() {
        return continuation;
    }

    /**
     *
     * @return <code>null</code> to mean there is no CodeEmit info at this level
     */
    public CodeEmitInfo info() {
        return null;
    }

    /**
     * Checks if this is an exception throw code stub.
     *
     * @return false
     */
    public boolean isExceptionThrowStub() {
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

    public void visit(LIRVisitState v) {
        if (C1XOptions.LIRTracePeephole && C1XOptions.Verbose) {
            TTY.print("no visitor for ");
            printName(TTY.out);
            TTY.println();
        }
    }

    public abstract void emitCode(LIRAssembler e);

    public abstract void printName(LogStream out);

    public String name() {
        // TODO Auto-generated method stub
        return null;
    }

}
