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
import com.sun.c1x.debug.*;
import com.sun.c1x.stub.*;
import com.sun.c1x.util.*;


/**
 * The <code>LIRTypeCheck</code> class definition.
 *
 * @author Marcelo Cintra
 *
 */
public class LIRTypeCheck extends LIRInstruction {

    LIROperand object;
    LIROperand array;
    private RiType klass;
    LIROperand tmp1;
    LIROperand tmp2;
    LIROperand tmp3;
    private boolean fastCheck;
    CodeEmitInfo infoForPatch;
    CodeEmitInfo infoForException;
    CodeStub stub;
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
        super(opcode, result, null);
        this.object = object;
        this.array = LIROperandFactory.IllegalOperand;
        this.klass = klass;
        this.tmp1 = tmp1;
        this.tmp2 = tmp2;
        this.tmp3 = tmp3;
        this.fastCheck = fastCheck;
        this.stub = stub;
        this.infoForPatch = infoForPatch;
        this.infoForException = infoForException;
        this.profiledMethod = profiledMethod;
        this.profiledBci = profiledBci;

        if (opcode == LIROpcode.CheckCast) {
            assert this.infoForException != null : "infoForException must not be null. CheckCast throws exceptions.";
        } else if (opcode == LIROpcode.InstanceOf) {
            assert this.infoForException == null : "infoForExeception must be null. Instanceof throws no exceptions.";
        } else {
            Util.shouldNotReachHere();
        }
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
        super(opcode, LIROperandFactory.IllegalOperand, null);
        this.object = object;
        this.klass = null;
        this.array = array;
        this.tmp1 = tmp1;
        this.tmp2 = tmp2;
        this.tmp3 = tmp3;
        this.fastCheck = false;
        this.stub = null;
        this.infoForPatch = null;
        this.infoForException = infoForException;
        this.profiledMethod = profiledMethod;
        this.profiledBci = profiledBci;

        if (opcode == LIROpcode.StoreCheck) {
            stub = new ArrayStoreExceptionStub(infoForException);
            assert infoForException != null : "infoForException must not be null. StoreCheck instrution throws exceptions.";
        } else {
            Util.shouldNotReachHere();
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
        assert code == LIROpcode.StoreCheck : "opcode is not valid.";
        return array;
    }

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
     * Gets the klass of this type check instruction.
     *
     * @return the klass
     */
    public RiType klass() {
        assert code() == LIROpcode.InstanceOf || code() == LIROpcode.CheckCast : "opcode is not valid.";
        return klass;
    }

    /**
     * Gets the fastCheck of this type check instruction.
     *
     * @return the fastCheck
     */
    public boolean isFastCheck() {
        assert code() == LIROpcode.InstanceOf || code() == LIROpcode.CheckCast : "opcode is not valid.";
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
    public RiMethod profiledMethod() {
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
        object().print(out);                  out.print(" ");
        if (code() == LIROpcode.StoreCheck) {
          array().print(out);
          out.print(" ");
        }
        if (code() != LIROpcode.StoreCheck) {
          out.print(klass().name());
          out.print(" ");
          if (isFastCheck()) {
              out.print("fastCheck ");
          }
        }
        tmp1().print(out);
        out.print(" ");
        tmp2().print(out);
        out.print(" ");
        tmp3().print(out);
        out.print(" ");
        result().print(out);
        out.print(" ");
        if (infoForException() != null) {
            out.printf(" [bci:%d]", infoForException().bci());
        }
    }
}

