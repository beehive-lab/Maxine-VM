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

import com.sun.c1x.ci.*;
import com.sun.c1x.util.*;


/**
 * The <code>LIRTypeCheck</code> class definition.
 *
 * @author Marcelo Cintra
 *
 */
public class LIRTypeCheck extends LIRInstruction {

    private LIROperand object;
    private LIROperand array;
    //private ciKlass klass; TODO need to port class ciKlass
    private LIROperand tmp1;
    private LIROperand tmp2;
    private LIROperand tmp3;
    private boolean fastCheck;
    CodeEmitInfo infoForPatch;
    CodeEmitInfo infoForException;
    CodeStub stub;
    // Helpers for Tier1UpdateMethodData
    CiMethod profiledMethod;
    int profiledBci;

    /**
     * Constructs a new TypeCheck instruction.
     *
     * @param object
     * @param result
     * @param array
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
    public LIRTypeCheck(LIROpcode opcode, LIROperand result, LIROperand object, /*ciKlass klass,*/ LIROperand tmp1, LIROperand tmp2, LIROperand tmp3, boolean fastCheck, CodeEmitInfo infoForPatch,
                        CodeEmitInfo infoForException, CodeStub stub, CiMethod profiledMethod, int profiledBci) {
        super(opcode, result, null);
        this.object = object;
        this.array = LIROperandFactory.illegalOperand;
        //this.klass = klass;
        this.tmp1 = tmp1;
        this.tmp2 = tmp2;
        this.tmp3 = tmp3;
        this.fastCheck = fastCheck;
        this.infoForPatch = infoForPatch;
        this.infoForException = infoForException;
        this.stub = stub;
        this.profiledMethod = profiledMethod;
        this.profiledBci = profiledBci;

        if (opcode == LIROpcode.CheckCast) {
            assert this.infoForException != null : "infoForExeception must not be null. CheckCast throws exceptions.";
        } else if (opcode == LIROpcode.InstanceOf) {
            assert this.infoForException == null : "infoForExeception must be null. Instanceof throws no exceptions.";
        } else {
            assert false : "Ilegal opcode for LIRTypeCheck instruction.";
        }
    }

    /**
     * Constructs a new TypeCheck instruction.
     *
     * @param object
     * @param array
     * @param klass
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
    public LIRTypeCheck(LIROpcode opcode, LIROperand object, LIROperand array, /*ciKlass klass,*/ LIROperand tmp1, LIROperand tmp2, LIROperand tmp3,
                        CodeEmitInfo infoForException, CodeStub stub, CiMethod profiledMethod, int profiledBci) {
        super(opcode, LIROperandFactory.illegalOperand, null);
        this.object = object;
        //this.klass = null;
        this.array = array;
        this.tmp1 = tmp1;
        this.tmp2 = tmp2;
        this.tmp3 = tmp3;
        this.fastCheck = false;
        this.infoForPatch = null;
        this.infoForException = infoForException;
        this.stub = null;
        this.profiledMethod = profiledMethod;
        this.profiledBci = profiledBci;

        if (opcode == LIROpcode.StoreCheck) {
            stub = new ArrayStoreExceptionStub(infoForException);
            assert infoForException != null : "infoForException must not be null. StoreCheck instrution throws exceptions.";
        }
    }

    /**
     * Gets the object of this class.
     *
     * @return the object
     */
    public LIROperand object() {
        return object;
    }

    /**
     * Gets the array of this type check instruction.
     *
     * @return the array
     */
    public LIROperand array() {
        assert opcode == LIROpcode.StoreCheck : "opcode is not valid.";
        return array;
    }

    /**
     * Gets the klass of this type check instruction.
     *
     * @return the klass
     */
    //public ciKlass klass() {
    //    assert opcode == LIROpcode.InstanceOf || opcode == LIROpcode.CheckCast, "opcode is not valid.";
    //    return klass;
    //}

    /**
     * Gets the tmp1 of this type check instruction.
     *
     * @return the tmp1
     */
    public LIROperand tmp1() {
        return tmp1;
    }

    /**
     * Gets the tmp2 of this type check instruction.
     *
     * @return the tmp2
     */
    public LIROperand tmp2() {
        return tmp2;
    }

    /**
     * Gets the tmp3 of this type check instruction.
     *
     * @return the tmp3
     */
    public LIROperand tmp3() {
        return tmp3;
    }

    /**
     * Gets the fastCheck of this type check instruction.
     *
     * @return the fastCheck
     */
    public boolean isFastCheck() {
        return fastCheck;
    }

    /**
     * Gets the infoForPatch of this type check instruction.
     *
     * @return the infoForPatch
     */
    public CodeEmitInfo infoForPatch() {
        return infoForPatch;
    }

    /**
     * Gets the infoForException of this type check instruction.
     *
     * @return the infoForException
     */
    public CodeEmitInfo infoForException() {
        return infoForException;
    }

    /**
     * Gets the stub of this type check instruction.
     *
     * @return the stub
     */
    public CodeStub stub() {
        return stub;
    }

    /**
     * Gets the profiledMethod of this type check.
     *
     * @return the profiledMethod
     */
    public CiMethod profiledMethod() {
        return profiledMethod;
    }

    /**
     * Gets the profiledBci of this type check instruction.
     *
     * @return the profiledBci
     */
    public int profiledBci() {
        return profiledBci;
    }

    /**
     * Emits target assembly code for this instruction.
     *
     * @param masm the target assembler
     */
    @Override
    public void emitCode(LIRAssembler masm) {
        // TODO Auto-generated method stub

    }

    /**
     * Prints this instruction.
     *
     * @param out the output stream
     */
    @Override
    public void printInstruction(LogStream out) {

    }
}

