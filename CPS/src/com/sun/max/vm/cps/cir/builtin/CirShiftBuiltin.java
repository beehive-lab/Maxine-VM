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
package com.sun.max.vm.cps.cir.builtin;

import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.optimize.*;

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
