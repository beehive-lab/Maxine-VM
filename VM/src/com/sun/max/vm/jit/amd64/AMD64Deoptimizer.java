/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.jit.amd64;

import com.sun.max.vm.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jit.*;

/**
 * Deoptimization of AMD64 stack frames into JIT format.
 *
 * @author Bernd Mathiske
 */
public final class AMD64Deoptimizer extends Deoptimizer {

    private AMD64Deoptimizer() {
        super();
    }

    public static Deoptimizer deoptimizer() {
        return Deoptimizer.deoptimizer();
    }

    static {
        new AMD64Deoptimizer();
    }

    @Override
    public int directCallSize() {
        return 5;
    }

    private static boolean isRexPrefix(byte opcode) {
        return (opcode & 0xf0) == 0x40;
    }

    @Override
    public int indirectCallSize(byte firstInstructionByte) {
        return isRexPrefix(firstInstructionByte) ? 4 : 3;
    }

    private static byte[] illegalInstruction = {(byte) 0x27}; // DAA: illegal in 64-bit mode

    @Override
    public byte[] illegalInstruction() {
        return illegalInstruction;
    }

    public static final TargetLocation.IntegerRegister INTEGER_RETURN_REGISTER = new TargetLocation.IntegerRegister(VMConfiguration.hostOrTarget().targetABIsScheme().optimizedJavaABI().integerReturn().value());

    @Override
    public TargetLocation.IntegerRegister referenceReturnRegister() {
        return INTEGER_RETURN_REGISTER;
    }

    public static final TargetLocation.FloatingPointRegister FLOATING_POINT_RETURN_REGISTER = new TargetLocation.FloatingPointRegister(VMConfiguration.hostOrTarget().targetABIsScheme().optimizedJavaABI().floatingPointReturn().value());

    @Override
    protected AMD64Deoptimization createDeoptimization() {
        return new AMD64Deoptimization();
    }

}
