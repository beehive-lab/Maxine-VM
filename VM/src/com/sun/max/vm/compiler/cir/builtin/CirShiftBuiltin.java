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
/*VCSID=6d28c8da-a557-44d5-9781-29453604a92b*/
package com.sun.max.vm.compiler.cir.builtin;

import com.sun.max.lang.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.optimize.*;

/**
 * @author Bernd Mathiske
 */
public abstract class CirShiftBuiltin extends CirStrengthReducible {

    private final int _width;

    protected CirShiftBuiltin(Builtin builtin, int width) {
        super(builtin);
        _width = width;
    }

    @Override
    public boolean isReducible(CirOptimizer cirOptimizer, CirValue[] arguments) {
        if (arguments[0].isScalarConstant()) {
            final int number = arguments[0].value().toInt();
            if (number == 0) {
                return true;
            }
        }
        if (arguments[1].isScalarConstant()) {
            final int shift = arguments[1].value().toInt();
            return (shift % _width) == 0;
        }
        return false;
    }

    @Override
    public CirCall reduce(CirOptimizer cirOptimizer, CirValue... arguments) {
        final CirValue number = arguments[0];
        final CirValue normalContinuation = arguments[2];
        return new CirCall(normalContinuation, number);
    }

    public static final class IntShiftedLeft extends CirShiftBuiltin {
        public IntShiftedLeft() {
            super(JavaBuiltin.IntShiftedLeft.BUILTIN, Ints.WIDTH);
        }
    }

    public static final class LongShiftedLeft extends CirShiftBuiltin {
        public LongShiftedLeft() {
            super(JavaBuiltin.LongShiftedLeft.BUILTIN, Longs.WIDTH);
        }
    }

    public static final class IntUnsignedShiftedRight extends CirShiftBuiltin {
        public IntUnsignedShiftedRight() {
            super(JavaBuiltin.IntUnsignedShiftedRight.BUILTIN, Ints.WIDTH);
        }
    }

    public static final class LongUnsignedShiftedRight extends CirShiftBuiltin {
        public LongUnsignedShiftedRight() {
            super(JavaBuiltin.LongUnsignedShiftedRight.BUILTIN, Longs.WIDTH);
        }
    }

    public static final class IntSignedShiftedRight extends CirShiftBuiltin {
        public IntSignedShiftedRight() {
            super(JavaBuiltin.IntSignedShiftedRight.BUILTIN, Ints.WIDTH);
        }

        @Override
        public boolean isReducible(CirOptimizer cirOptimizer, CirValue[] arguments) {
            if (arguments[0].isScalarConstant() && arguments[0].value().toInt() == -1) {
                return true;
            }
            return super.isReducible(cirOptimizer, arguments);
        }
    }

    public static final class LongSignedShiftedRight extends CirShiftBuiltin {
        public LongSignedShiftedRight() {
            super(JavaBuiltin.LongSignedShiftedRight.BUILTIN, Longs.WIDTH);
        }

        @Override
        public boolean isReducible(CirOptimizer cirOptimizer, CirValue[] arguments) {
            if (arguments[0].isScalarConstant() && arguments[0].value().toLong() == -1L) {
                return true;
            }
            return super.isReducible(cirOptimizer, arguments);
        }
    }
}
