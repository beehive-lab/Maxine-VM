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
