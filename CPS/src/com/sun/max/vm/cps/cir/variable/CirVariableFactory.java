/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.vm.cps.cir.variable;

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
