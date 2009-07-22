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
 * The <code>DivByZeroStub</code> class definition.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 */
public class DivByZeroStub extends CodeStub {

    private CodeEmitInfo info;
    private int offset;

    public DivByZeroStub(CodeEmitInfo info) {
        this(-1, info);
    }

    public DivByZeroStub(int offset, CodeEmitInfo info) {
        this.info = info;
        this.offset = offset;
    }

    @Override
    public void emitCode(LIRAssembler e) {
        // TODO Not implemented yet
    }

    /**
     * Gets the CodeEmitInfo of this class.
     *
     * @return the info
     */
    @Override
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

    @Override
    public boolean isExceptionThrowStub() {
        return true;
    }

    public boolean isDivbyzeroStub() {
        return true;
    }

    @Override
    public void visit(LIRVisitState visitor) {
        visitor.doSlowCase(info);
    }

    @Override
    public void printName(LogStream out) {
        out.print("DivByZeroStub");
    }
}
