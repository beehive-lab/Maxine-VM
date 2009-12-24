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
package com.sun.max.vm.cps.eir.sparc;

import com.sun.max.lang.*;
import com.sun.max.vm.cps.eir.*;

/**
 * @author Bernd Mathiske
 * @author Laurent Daynes
 */
public abstract class SPARCEirOperation extends EirInstruction<SPARCEirInstructionVisitor, SPARCEirTargetEmitter> implements SPARCEirInstruction {

    public static boolean isSimm13(int value) {
        return Ints.numberOfEffectiveSignedBits(value) <= 13;
    }

    public static boolean isSimm13(long value) {
        return Longs.numberOfEffectiveSignedBits(value) <= 13;
    }

    public static boolean isSimm11(int value) {
        return Ints.numberOfEffectiveSignedBits(value) <= 11;
    }

    public static boolean isSimm11(long value) {
        return Longs.numberOfEffectiveSignedBits(value) <= 11;
    }

    public static SPARCEirRegister.GeneralPurpose toGeneralRegister(EirLocation location) {
        return (SPARCEirRegister.GeneralPurpose) location;
    }

    public static SPARCEirRegister.FloatingPoint toFloatingPointRegister(EirLocation location) {
        return (SPARCEirRegister.FloatingPoint) location;
    }

    protected SPARCEirOperation(EirBlock block) {
        super(block);
    }

}
