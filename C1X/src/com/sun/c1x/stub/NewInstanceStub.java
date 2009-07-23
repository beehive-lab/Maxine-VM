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

import com.sun.c1x.ci.*;
import com.sun.c1x.lir.*;
import com.sun.c1x.debug.LogStream;


/**
 * The <code>NewInstanceStub</code> class definition.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 */
public class NewInstanceStub extends CodeStub {

    private CiType klass;
    private LIROperand klassReg;
    private LIROperand result;
    private CodeEmitInfo info;
    private CiRuntimeCall stubId;

//        public NewInstanceStub(LIROperand klassReg, LIROperand dst, CiType klass, CodeEmitInfo info, CiRuntimeCall stubId) {
//           // TODO Auto-generated constructor stub
//       }

    /**
     * @param klass
     * @param klassReg
     * @param result
     * @param info
     * @param stubId
     */
    public NewInstanceStub(LIROperand klassReg, LIROperand result, CiType klass, CodeEmitInfo info, CiRuntimeCall stubId) {
        super();
        this.klass = klass;
        this.klassReg = klassReg;
        this.result = result;
        this.info = info;
        this.stubId = stubId;
    }

    @Override
    public void emitCode(LIRAssembler e) {

    }

    @Override
    public CodeEmitInfo info() {
        return info;
    }

    /**
     * Gets the klass of this class.
     *
     * @return the klass
     */
    public CiType getKlass() {
        return klass;
    }

    /**
     * Gets the stubId of this class.
     *
     * @return the stubId
     */
    public CiRuntimeCall getStubId() {
        return stubId;
    }

    @Override
    public void visit(LIRVisitState visitor) {
        visitor.doSlowCase(info);
        visitor.doInput(klassReg);
        visitor.doOutput(result);
    }

    @Override
    public void printName(LogStream out) {
        out.print("NewInstanceStub");
    }
}
