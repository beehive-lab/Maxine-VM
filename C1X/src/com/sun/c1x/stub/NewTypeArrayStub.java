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

import com.sun.c1x.debug.LogStream;
import com.sun.c1x.lir.CodeEmitInfo;
import com.sun.c1x.lir.LIRAssembler;
import com.sun.c1x.lir.LIROperand;
import com.sun.c1x.lir.LIRVisitState;


/**
 * The <code>NewTypeArrayStub</code> class definition.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 */
public class NewTypeArrayStub extends CodeStub {

    private LIROperand klassReg;
    private LIROperand length;
    private LIROperand result;
    private CodeEmitInfo info;

    /**
     * @param klassReg
     * @param length
     * @param result
     * @param info
     */
    public NewTypeArrayStub(LIROperand klassReg, LIROperand length, LIROperand result, CodeEmitInfo info) {
        super();
        this.klassReg = klassReg;
        this.length = length;
        this.result = result;
        this.info = info;
    }

    @Override
    public void emitCode(LIRAssembler e) {
        // TODO: Not implemented yet
    }

    @Override
    public CodeEmitInfo info() {
        return info;
    }

    @Override
    public void visit(LIRVisitState visitor) {
        visitor.doSlowCase(info);
        visitor.doInput(klassReg);
        visitor.doInput(length);
        assert result.isValid() : "must be valid";
        visitor.doOutput(result);
    }

    @Override
    public void printName(LogStream out) {
        out.print("NewTypeArrayStub");
    }
}
