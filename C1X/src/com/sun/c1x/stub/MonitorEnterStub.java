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
 * The <code>MonitorEnterStub</code> class definition.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 */
public class MonitorEnterStub extends MonitorAccessStub {

    /**
     * Creates a new instance of <code>MonitorEnterStub</code>.
     *
     * @param objReg the operand with the object reference
     * @param lockReg the lock register
     * @param info the debug information for this code stub
     */
    public MonitorEnterStub(LIROperand objReg, LIROperand lockReg, CodeEmitInfo info) {
        super(objReg, lockReg, info);
    }

    @Override
    public void accept(CodeStubVisitor visitor) {
        visitor.visitMonitorEnterStub(this);
    }

    @Override
    public void visit(LIRVisitState visitor) {
        visitor.doInput(objReg);
        visitor.doInput(lockReg);
        visitor.doSlowCase(info);
    }
}
