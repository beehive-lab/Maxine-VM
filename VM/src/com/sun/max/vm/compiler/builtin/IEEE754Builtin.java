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
/*VCSID=f5fce50b-2808-4360-8abb-e8c0ddb1212c*/
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

        @BUILTIN(builtinClass = ConvertFloatToInt.class)
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

        @BUILTIN(builtinClass = ConvertFloatToLong.class)
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

        @BUILTIN(builtinClass = ConvertDoubleToInt.class)
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

        @BUILTIN(builtinClass = ConvertDoubleToLong.class)
        public static long convertDoubleToLong(double doubleValue) {
            assert doubleValue < MAX_DOUBLE_VALUE : "IEEE754 convertDoubleToLong not implemented for value " + doubleValue;
            return (long) doubleValue;
        }

        public static final ConvertDoubleToLong BUILTIN = new ConvertDoubleToLong();
    }

}
