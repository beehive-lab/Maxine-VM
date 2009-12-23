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
package com.sun.max.vm.compiler.cps.cir.builtin;

import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.cps.cir.*;
import com.sun.max.vm.compiler.cps.cir.optimize.*;

/**
 * @author Bernd Mathiske
 */
public final class CirShiftBuiltin extends CirStrengthReducible {

    private final int width;
    private final boolean minusOneLeftOperandIsReducible;

    protected CirShiftBuiltin(Builtin builtin, int width, boolean minusOneLeftOperandIsReducible) {
        super(builtin);
        this.width = width;
        this.minusOneLeftOperandIsReducible = minusOneLeftOperandIsReducible;
    }

    @Override
    public boolean isReducible(CirOptimizer cirOptimizer, CirValue[] arguments) {
        if (arguments[0].isScalarConstant()) {
            if (arguments[0].value().isZero()) {
                return true;
            }
            if (minusOneLeftOperandIsReducible && arguments[0].value().isAllOnes()) {
                return true;
            }
        }
        if (arguments[1].isScalarConstant()) {
            final int shift = arguments[1].value().toInt();
            return (shift % width) == 0;
        }
        return false;
    }

    @Override
    public CirCall reduce(CirOptimizer cirOptimizer, CirValue... arguments) {
        final CirValue number = arguments[0];
        final CirValue normalContinuation = arguments[2];
        return new CirCall(normalContinuation, number);
    }
}
