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

import com.sun.c1x.asm.*;
import com.sun.c1x.debug.*;
import com.sun.c1x.lir.*;

/**
 * The <code>CodeStub</code> class definition. CodeStubs are little 'out-of-line'
 * pieces of code that usually handle slow cases of operations. All code stubs are
 * collected and code is emitted at the end of the method.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 */
public abstract class CodeStub {

    public LIRDebugInfo info;
    public final Label entry = new Label();            // label at the stub entry point
    public final Label continuation = new Label();     // label where stub continues, if any

    protected LIRInstruction instruction;

    private LIROperand[] operands;
    private LIROperand result;
    private LIRInstruction.OperandSlot resultSlot;

    protected int tempCount;
    protected int tempInputCount;

    public CodeStub(LIRDebugInfo info) {
        this(info, LIROperandFactory.IllegalLocation);
    }

    public CodeStub(LIRDebugInfo info, LIROperand result) {
        this.info = info;
        this.result = result;
    }

    protected void setOperands(int tempInputCount, int tempCount, LIROperand... operands) {
        this.tempCount = tempCount;
        this.tempInputCount = tempInputCount;
        this.operands = operands;
    }

    protected LIROperand operand(int index) {
        return instruction.stubOperand(index);
    }

    /**
     * Asserts that the code stub has bounded labels.
     */
    public boolean assertNoUnboundLabels() {
        assert !entry.isUnbound() && !continuation.isUnbound() : "Code stub has an unbound label";
        return true;
    }

    /**
     * Checks if this is an exception throw code stub.
     *
     * @return false
     */
    public boolean isExceptionThrowStub() {
        return false;
    }

    public abstract void accept(CodeStubVisitor visitor);

    public void printName(LogStream out) {
        out.print(name());
    }

    public String name() {
        return this.getClass().getSimpleName();
    }

    public LIROperand originalResult() {
        return result;
    }

    public LIROperand result() {
        return resultSlot.get(instruction);
    }

    public LIROperand[] operands() {
        return operands;
    }

    public int tempCount() {
        return tempCount;
    }

    public int tempInputCount() {
        return tempInputCount;
    }

    public void setResultSlot(LIRInstruction.OperandSlot resultSlot) {
        this.resultSlot = resultSlot;
    }

    public void setInstruction(LIRInstruction instruction) {
        this.instruction = instruction;

    }

}
