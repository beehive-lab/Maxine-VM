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
package com.sun.max.vm.compiler.cps.cir.variable;

import com.sun.max.vm.type.*;

/**
 * Creates variables with naming and serial numbers that facilitate reading CIR listings.
 *
 * @author Bernd Mathiske
 */
public class CirVariableFactory {

    private int serial;

    public CirVariableFactory(int serial) {
        this.serial = serial;
    }

    public CirVariableFactory() {
        serial = 0;
    }

    private int makeSerial() {
        if (serial >= 0) {
            serial++;
            return serial;
        }
        serial--;
        return serial;
    }

    private final CirVariable normalContinuationParameter = new CirNormalContinuationParameter(makeSerial());

    public CirVariable normalContinuationParameter() {
        return normalContinuationParameter;
    }

    private final CirVariable exceptionContinuationParameter = new CirExceptionContinuationParameter(makeSerial());

    public CirVariable exceptionContinuationParameter() {
        return exceptionContinuationParameter;
    }

    public CirVariable createMethodParameter(Kind kind, int index) {
        return new CirMethodParameter(makeSerial(), kind, index);
    }

    public CirVariable createLocalVariable(Kind kind, int localIndex) {
        return new CirLocalVariable(makeSerial(), kind, localIndex);
    }

    public CirVariable createStackVariable(Kind kind, int stackSlotIndex) {
        return new CirStackVariable(makeSerial(), kind, stackSlotIndex);
    }

    public CirVariable createTemporary(Kind kind) {
        return new CirTemporaryVariable(makeSerial(), kind);
    }

    public CirVariable createFresh(CirVariable variable) {
        return variable.createFresh(makeSerial());
    }

    public CirExceptionContinuationParameter createFreshExceptionContinuationParameter() {
        return new CirExceptionContinuationParameter(makeSerial());
    }
    public CirNormalContinuationParameter createFreshNormalContinuationParameter() {
        return new CirNormalContinuationParameter(makeSerial());
    }
}
