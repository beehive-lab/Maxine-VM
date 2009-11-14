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
package com.sun.max.vm.compiler.builtin;

import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.*;

public abstract class AddressBuiltin extends Builtin {

    private AddressBuiltin() {
        super(Address.class);
    }

    @Override
    public final boolean hasSideEffects() {
        return false;
    }

    public static class LessEqual extends AddressBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitLessEqual(this, result, arguments);
        }
        public static final LessEqual BUILTIN = new LessEqual();
    }

    public static class LessThan extends AddressBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitLessThan(this, result, arguments);
        }
        public static final LessThan BUILTIN = new LessThan();
    }

    public static class GreaterEqual extends AddressBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitGreaterEqual(this, result, arguments);
        }
        public static final GreaterEqual BUILTIN = new GreaterEqual();
    }

    public static class GreaterThan extends AddressBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitGreaterThan(this, result, arguments);
        }
        public static final GreaterThan BUILTIN = new GreaterThan();
    }

    public static class DividedByAddress extends AddressBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitDividedByAddress(this, result, arguments);
        }
        public static final DividedByAddress BUILTIN = new DividedByAddress();

        @Override
        public int reasonsMayStop() {
            return Stoppable.DIVIDE_BY_ZERO_CHECK;
        }
    }

    public static class DividedByInt extends AddressBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitDividedByInt(this, result, arguments);
        }
        public static final DividedByInt BUILTIN = new DividedByInt();

        @Override
        public int reasonsMayStop() {
            return Stoppable.DIVIDE_BY_ZERO_CHECK;
        }
    }

    public static class RemainderByAddress extends AddressBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitRemainderByAddress(this, result, arguments);
        }
        public static final RemainderByAddress BUILTIN = new RemainderByAddress();

        @Override
        public int reasonsMayStop() {
            return Stoppable.DIVIDE_BY_ZERO_CHECK;
        }
    }

    public static class RemainderByInt extends AddressBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitRemainderByInt(this, result, arguments);
        }
        public static final RemainderByInt BUILTIN = new RemainderByInt();

        @Override
        public int reasonsMayStop() {
            return Stoppable.DIVIDE_BY_ZERO_CHECK;
        }
    }
}
