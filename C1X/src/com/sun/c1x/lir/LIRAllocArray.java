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

import com.sun.c1x.util.*;
import com.sun.c1x.value.*;

/**
 * The <code>LIRAllocArray</code> class definition.
 *
 * @author Marcelo Cintra
 *
 */
public class LIRAllocArray extends LIRInstruction {

    LIROperand klass;
    LIROperand len;
    LIROperand tmp1;
    LIROperand tmp2;
    LIROperand tmp3;
    LIROperand tmp4;
    BasicType type;
    CodeStub stub;

    /**
     * Creates a new LIRAllocArray instruction.
     *
     * @param klass
     * @param len
     * @param tmp1
     * @param tmp2
     * @param tmp3
     * @param tmp4
     * @param type
     * @param stub
     */
    public LIRAllocArray(LIROperand klass, LIROperand len, LIROperand result, LIROperand tmp1, LIROperand tmp2, LIROperand tmp3, LIROperand tmp4, BasicType type, CodeStub stub) {
        super(LIROpcode.AllocArray, result, null);
        this.klass = klass;
        this.len = len;
        this.tmp1 = tmp1;
        this.tmp2 = tmp2;
        this.tmp3 = tmp3;
        this.tmp4 = tmp4;
        this.type = type;
        this.stub = stub;
    }

    /**
     * Gets the klass member of this instruction.
     *
     * @return the klass
     */
    public LIROperand klass() {
        return klass;
    }

    /**
     * Gets the array length to be allocated by this instruction.
     * @return the array length to be allocated
     */
    public LIROperand length() {
        return len;
    }

    /**
     * Gets the result operand of this instruction.
     *
     * @return the result operand
     */
    public LIROperand obj() {
        return result();
    }

    /**
     * Gets the first temporary associated with this call instruction.
     *
     * @return the tmp1
     */
    public LIROperand tmp1() {
        return tmp1;
    }

    /**
     * Gets the second temporary associated with this call instruction.
     * TODO: what information does the tmp hold?
     *
     * @return the tmp2
     */
    public LIROperand tmp2() {
        return tmp2;
    }

    /**
     * Gets the third temporary associated with this call instruction.
     * TODO: what information does the tmp hold?
     *
     * @return the tmp3
     */
    public LIROperand tmp3() {
        return tmp3;
    }

    /**
     * Gets the fourth temporary associated with this call instruction.
     * TODO: what information does the tmp hold?
     *
     * @return the tmp4
     */
    public LIROperand tmp4() {
        return tmp4;
    }

    /**
     * Gets the basic type of this instruction.
     *
     * @return the type
     */
    public BasicType type() {
        return type;
    }

    /**
     * Gets the code stub for this instruction.
     *
     * @return the stub
     */
    public CodeStub stub() {
        return stub;
    }

    /**
     * Emits target assembly code for this instruction.
     *
     * @param masm the target assembler
     */
    @Override
    public void emitCode(LIRAssembler masm) {
        masm.emitAllocArray(this);
    }

    /**
     * Prints this instruction.
     *
     * @param out the output log stream
     */
    @Override
    public void printInstruction(LogStream out) {
        klass.print(out);
        out.print(" ");
        len.print(out);
        out.print(" ");
        obj().print(out);
        out.print(" ");
        result.print(out);
        out.print(" ");
        tmp1.print(out);
        out.print(" ");
        tmp2.print(out);
        out.print(" ");
        tmp3.print(out);
        out.print(" ");
        tmp4.print(out);
        out.print(" ");
        out.printf("[type:0x%x] ", type());
        out.printf("[label:0x%x]", stub.entry());
    }
}
