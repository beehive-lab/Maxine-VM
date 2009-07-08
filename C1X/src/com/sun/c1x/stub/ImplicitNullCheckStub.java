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
import com.sun.c1x.util.*;


/**
 * The <code>ImplicitNullCheckStub</code> class definition.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 */
public class ImplicitNullCheckStub {

    private CodeEmitInfo info;
    private int offset;

    /**
     * Creates a new instance of <code>ImplicitNullCheckStub</code>.
     *
     * @param offset the offset for this code stub
     * @param info the debug information associated to this code stub
     */
    public ImplicitNullCheckStub(int offset, CodeEmitInfo info) {
        this.offset = offset;
        this.info = info;
    }

    public CodeEmitInfo info() {
        return info;
    }

    /**
     * Gets the offset of this class.
     *
     * @return the offset
     */
    public int offset() {
        return offset;
    }

    public boolean isExceptionThrowStub() {
        return true;
    }

    public void visit(LIRVisitState visitor) {
        visitor.doSlowCase(info);
    }

    public void printName(LogStream out) {
        out.print("ImplicitNullCheckStub");
    }
}
