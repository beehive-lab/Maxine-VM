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

import com.sun.c1x.debug.*;
import com.sun.c1x.ri.*;
import com.sun.c1x.stub.*;


/**
 * The <code>LIRTypeCheck</code> class definition.
 *
 * @author Marcelo Cintra
 *
 */
public class LIRTypeCheck extends LIRInstruction {

    private RiType klass;
    private boolean fastCheck;


    // Helpers for Tier1UpdateMethodData
    RiMethod profiledMethod;
    int profiledBci;

    /**
     * Constructs a new TypeCheck instruction.
     *
     * @param opcode
     * @param result
     * @param object
     * @param tmp1
     * @param tmp2
     * @param tmp3
     * @param fastCheck
     * @param infoForPatch
     * @param infoForException
     * @param stub
     * @param profiledMethod
     * @param profiledBci
     */
    public LIRTypeCheck(LIROpcode opcode, LIROperand result, LIROperand object, RiType klass, LIROperand tmp1, LIROperand tmp2, LIROperand tmp3, boolean fastCheck, CodeEmitInfo infoForException,
                        CodeEmitInfo infoForPatch, CodeStub stub, RiMethod profiledMethod, int profiledBci) {
        super(opcode, result, infoForException, false, stub, 1, 2, object, LIROperandFactory.IllegalLocation, tmp1, tmp2, tmp3);

        assert opcode == LIROpcode.CheckCast || opcode == LIROpcode.InstanceOf;
        this.klass = klass;
        this.fastCheck = fastCheck;
        this.profiledMethod = profiledMethod;
        this.profiledBci = profiledBci;
    }

    /**
     * Constructs a new TypeCheck instruction.
     *
     * @param opcode
     * @param object
     * @param array
     * @param tmp1
     * @param tmp2
     * @param tmp3
     * @param infoForException
     * @param profiledMethod
     * @param profiledBci
     */
    public LIRTypeCheck(LIROpcode opcode, LIROperand object, LIROperand array,
                        LIROperand tmp1, LIROperand tmp2, LIROperand tmp3,
                        CodeEmitInfo infoForException, RiMethod profiledMethod, int profiledBci) {
        super(opcode, LIROperandFactory.IllegalLocation, infoForException, false, new ArrayStoreExceptionStub(infoForException), 0, 3, object, array, tmp1, tmp2, tmp3);
        this.klass = null;
        this.fastCheck = false;
        this.profiledMethod = profiledMethod;
        this.profiledBci = profiledBci;
        assert opcode == LIROpcode.StoreCheck;
        assert infoForException != null : "infoForException must not be null. StoreCheck instrution throws exceptions.";
    }

    /**
     * Gets the object of this class.
     *
     * @return the object
     */
    public LIROperand object() {
        return operand(0);
    }

    /**
     * Gets the array of this type check instruction.
     *
     * @return the array
     */
    public LIROperand array() {
        assert code == LIROpcode.StoreCheck : "opcode is not valid.";
        return operand(1);
    }

    /**
     * Gets the tmp1 of this type check instruction.
     *
     * @return the tmp1
     */
    public LIROperand tmp1() {
        return operand(2);
    }

    /**
     * Gets the tmp2 of this type check instruction.
     *
     * @return the tmp2
     */
    public LIROperand tmp2() {
        return operand(3);
    }

    /**
     * Gets the tmp3 of this type check instruction.
     *
     * @return the tmp3
     */
    public LIROperand tmp3() {
        return operand(4);
    }

    /**
     * Gets the klass of this type check instruction.
     *
     * @return the klass
     */
    public RiType klass() {
        assert code == LIROpcode.InstanceOf || code == LIROpcode.CheckCast : "opcode is not valid.";
        return klass;
    }

    /**
     * Gets the fastCheck of this type check instruction.
     *
     * @return the fastCheck
     */
    public boolean isFastCheck() {
        assert code == LIROpcode.InstanceOf || code == LIROpcode.CheckCast : "opcode is not valid.";
        return fastCheck;
    }

    /**
     * Gets the profiledMethod of this type check.
     *
     * @return the profiledMethod
     */
    public RiMethod profiledMethod() {
        return profiledMethod;
    }

    /**
     * Emits target assembly code for this instruction.
     *
     * @param masm the target assembler
     */
    @Override
    public void emitCode(LIRAssembler masm) {
        masm.emitTypeCheck(this);
        if (stub != null) {
            masm.emitCodeStub(stub);
        }
    }

    /**
     * Prints this instruction.
     *
     * @param out the output stream
     */
    @Override
    public void printInstruction(LogStream out) {
        if (code != LIROpcode.StoreCheck) {
          out.print(klass().name());
          out.print(" ");
          if (isFastCheck()) {
              out.print("fastCheck ");
          }
        }
        super.printInstruction(out);
        if (info != null) {
            out.printf(" [bci:%d]", info.bci());
        }
    }
}

