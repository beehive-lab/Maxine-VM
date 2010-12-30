/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.annotate.*;
import com.sun.max.vm.compiler.snippet.*;

/**
 * This class defines the builtins that perform conversion from IEEE single and
 * double precision floating point numbers to ints and longs.  The JVM f2i, f2l,
 * d2i, and d2l instructions cannot be converted directly to use IEEE-semantics
 * that's implemented by the majority of hardware.  So, we implement the bytecodes
 * as snippets (e.g., {@link Snippet.ConvertFloatToInt}) that expresses the jvm
 * semantics in terms of the IEEE semantics.  The interpretation of these builtins
 * is not complete, they can only handle the subset of the floating point numbers
 * that can be passed by the expansion of the snippets, and raises an exception if
 * the number is out of the supported range.
 *
 * @author Aziz Ghuloum
 *
 */
public abstract class IEEE754Builtin extends Builtin {

    protected IEEE754Builtin() {
        super(null);
    }

    public static class ConvertFloatToInt extends IEEE754Builtin {
        private static final float MAX_FLOAT_VALUE =  -1 >>> 1;

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 1;
            visitor.visitConvertFloatToInt(this, result, arguments);
        }

        @BUILTIN(value = ConvertFloatToInt.class)
        public static int convertFloatToInt(float floatValue) {
            assert floatValue < MAX_FLOAT_VALUE : "IEEE754 convertFloatToInt not implemented for value " + floatValue;
            return (int) floatValue;
        }

        public static final ConvertFloatToInt BUILTIN = new ConvertFloatToInt();
    }

    public static class ConvertFloatToLong extends IEEE754Builtin {
        private static final float MAX_FLOAT_VALUE = ((long) -1) >>> 1;
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 1;
            visitor.visitConvertFloatToLong(this, result, arguments);
        }

        @BUILTIN(value = ConvertFloatToLong.class)
        public static long convertFloatToLong(float floatValue) {
            assert floatValue < MAX_FLOAT_VALUE : "IEEE754 convertFloatToLong not implemented for value " + floatValue;
            return (long) floatValue;
        }

        public static final ConvertFloatToLong BUILTIN = new ConvertFloatToLong();
    }

    public static class ConvertDoubleToInt extends IEEE754Builtin {
        private static final double MAX_DOUBLE_VALUE = -1 >>> 1;
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 1;
            visitor.visitConvertDoubleToInt(this, result, arguments);
        }

        @BUILTIN(value = ConvertDoubleToInt.class)
        public static int convertDoubleToInt(double doubleValue) {
            assert doubleValue < MAX_DOUBLE_VALUE : "IEEE754 convertDoubleToInt not implemented for value " + doubleValue;
            return (int) doubleValue;
        }

        public static final ConvertDoubleToInt BUILTIN = new ConvertDoubleToInt();
    }

    public static class ConvertDoubleToLong extends IEEE754Builtin {
        private static final double MAX_DOUBLE_VALUE = ((long) -1) >>> 1;
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 1;
            visitor.visitConvertDoubleToLong(this, result, arguments);
        }

        @BUILTIN(value = ConvertDoubleToLong.class)
        public static long convertDoubleToLong(double doubleValue) {
            assert doubleValue < MAX_DOUBLE_VALUE : "IEEE754 convertDoubleToLong not implemented for value " + doubleValue;
            return (long) doubleValue;
        }

        public static final ConvertDoubleToLong BUILTIN = new ConvertDoubleToLong();
    }

}
