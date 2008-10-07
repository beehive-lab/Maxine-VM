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
/*VCSID=86d67a15-11ec-4d85-9837-af5d3060d138*/
package com.sun.max.vm.compiler.cir.variable;

import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.type.*;

/**
 * Creates variables with naming and serial numbers that facilitate reading CIR listings.
 *
 * @author Bernd Mathiske
 */
public class CirVariableFactory {

    private int _serial;

    public CirVariableFactory(int serial) {
        _serial = serial;
    }

    public CirVariableFactory() {
        _serial = 0;
    }

    private int makeSerial() {
        if (_serial >= 0) {
            _serial++;
            return _serial;
        }
        _serial--;
        return _serial;
    }

    private final CirVariable _normalContinuationParameter = new CirNormalContinuationParameter(makeSerial());

    public CirVariable normalContinuationParameter() {
        return _normalContinuationParameter;
    }

    private final CirVariable _exceptionContinuationParameter = new CirExceptionContinuationParameter(makeSerial());

    public CirVariable exceptionContinuationParameter() {
        return _exceptionContinuationParameter;
    }

    public CirVariable createMethodParameter(Kind kind, int index, BytecodeLocation location) {
        return new CirMethodParameter(makeSerial(), kind, index, location);
    }

    public CirVariable createLocalVariable(Kind kind, int localIndex, BytecodeLocation location) {
        return new CirLocalVariable(makeSerial(), kind, localIndex, location);
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
